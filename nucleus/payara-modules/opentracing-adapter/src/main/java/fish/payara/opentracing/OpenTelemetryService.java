/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.opentracing;

import fish.payara.telemetry.service.PayaraTelemetryBootstrapFactoryServiceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

/**
 * Manages per-application OpenTelemetry SDK instances as well as export to
 * Payara Request Tracing Service.
 */
@Service(name = "opentelemetry-service")
public class OpenTelemetryService implements EventListener {

    public static final String INSTRUMENTATION_SCOPE_NAME = "payara";

    // The tracer instances
    private static final Map<String, OpenTelemetryAppInfo> appTelemetries = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(OpenTelemetryService.class.getName());

    private PayaraTelemetryBootstrapFactoryServiceImpl payaraTelemetryBootstrapFactoryServiceImpl;

    @Inject
    Events events;

    @Inject
    ServiceLocator locator;

    @Inject
    InvocationManager invocationManager;

    @Inject
    ApplicationRegistry applicationRegistry;

    public Tracer getCurrentTracer() {
        String appName = initializeCurrentApplication();
        return getTracer(appName).orElseThrow(() -> currentAppNotInitializedException(appName));
    }

    public void initializeCurrentApplication(Map<String, String> otelProps) {
        initializeApplication(currentApplication(), otelProps);
    }

    /**
     * Required when application registers CDI-based components.
     */
    public void shutdownCurrentApplication() {
        shutdown(currentApplication());
    }

    public OpenTelemetry getCurrentSdk() {
        var appName = initializeCurrentApplication();
        return getSdk(appName).orElseThrow(() -> currentAppNotInitializedException(appName));
    }

    @PostConstruct
    void postConstruct() {
        if (events != null) {
            events.register(this);
        } else {
            logger.log(Level.WARNING, "OpenTelemetry service not registered to Payara Events: "
                    + "The Tracer for an application won't be removed upon undeployment");
        }
        payaraTelemetryBootstrapFactoryServiceImpl = Globals.getDefaultBaseServiceLocator().getService(PayaraTelemetryBootstrapFactoryServiceImpl.class);
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        if(openTelemetry == null) {
            GlobalOpenTelemetry.set(new GlobalTelemetry());
        }
    }

    @PreDestroy
    void stopAll() {
        appTelemetries.values().forEach(OpenTelemetryAppInfo::shutdown);
    }

    @Override
    public void event(Event<?> event) {
        // Listen for application unloaded events (happens during undeployment), so that we remove the tracer instance
        // registered to that application (if there is one)
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            shutdown(info.getName());
        }
    }

    private void shutdown(String appName) {
        var appInfo = appTelemetries.remove(appName);
        if (appInfo != null) {
            appInfo.shutdown();
        }
    }

    private String initializeCurrentApplication() {
        var appName = currentApplication();
        if (appName == null) {
            throw new IllegalStateException("No application code is executing. Cannot determine current application scope");
        }
        // if no application-level initialization took place, initialize with defaults
        ensureAppInitialized(appName, null);
        return appName;
    }

    /**
     * Return existing tracer for a given application.
     * <p>Because Telemetry SDKs are configurable per app, the tracer need to be explicitly created for an application</p>
     *
     * @param applicationName
     * @return
     */
    private Optional<Tracer> getTracer(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::tracer);
    }

    private static IllegalStateException currentAppNotInitializedException(String appName) {
        return new IllegalStateException("Application " + appName + " should have initialized, but it didn't");
    }

    private String currentApplication() {
        final ComponentInvocation invocation = invocationManager.getCurrentInvocation();
        if (invocation == null) {
            // In CDI context we might have app environment
            var appEnv = invocationManager.peekAppEnvironment();
            if (appEnv != null) {
                return appEnv.getName();
            }
            return null;
        }
        String appName = invocation.getAppName();
        if (appName == null) {
            appName = invocation.getModuleName();

            if (appName == null) {
                appName = invocation.getComponentId();

                // If we've found a component name, check if there's an application registered with the same name
                if (appName != null) {
                    // If it's not directly in the registry, it's possible due to how the componentId is constructed
                    if (applicationRegistry.get(appName) == null) {
                        String[] componentIds = appName.split("_/");

                        // The application name should be the first component
                        appName = componentIds[0];
                    }
                }
            }
        }
        return appName;
    }

    /**
     * Initialize OpenTelemtry components for an application if they do not exist yet.
     *
     * @param appName
     * @param properties
     */
    void ensureAppInitialized(String appName, Map<String, String> properties) {
        appTelemetries.computeIfAbsent(appName, (k) -> new OpenTelemetryAppInfo(createSdk(appName, properties)));
    }

    private <T> Optional<T> get(String applicationName, Function<OpenTelemetryAppInfo, T> getter) {
        if (applicationName == null) {
            return Optional.empty();
        }
        OpenTelemetryAppInfo appInfo = null;
        if (payaraTelemetryBootstrapFactoryServiceImpl.getAvailableRuntimeReference().isPresent()) {
            appInfo = new OpenTelemetryAppInfo(payaraTelemetryBootstrapFactoryServiceImpl.getAvailableRuntimeReference().get());
        } else {
            appInfo = appTelemetries.get(applicationName);
            if (appInfo == null) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(getter.apply(appInfo));
    }

    /**
     * Set up SDK to be used by application. The SDK instance will be configured using auto-configuration using contents of provided
     * Map, system properties and environment variables (in this precedence) if
     * <ul>
     *     <li>Request tracing is enabled, additionally passing spans onto Payara Reuest Tracing; or</li>
     *     <li>Config properties contain {@code otel.sdk.disabled=false}; or</li>
     *     <li>System properties contain that property, or environment contains key {@code OTEL_SDK_DISABLED=false}</li>
     * </ul>
     * Otherwise a Noop instance is configured.
     * <p>
     * SDK instances are local to the app, global tracing is not installed.
     *
     * @param applicationName
     * @param configProperties
     * @return
     * @link
     * <a href="https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md">Autoconfigure documentation</a>
     */
    private OpenTelemetrySdk createSdk(String applicationName, Map<String, String> configProperties) {


        if (isOtelEnabled(configProperties) || isPayaraTracingEnabled()) {
            var props = new HashMap<>(configProperties != null ? configProperties : Map.of());
            addDefault(props, "otel.service.name", applicationName);
            addDefault(props, "otel.metrics.exporter", "none");
            try {
                return AutoConfiguredOpenTelemetrySdk.builder()
                        .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                        .disableShutdownHook()
                        .addTracerProviderCustomizer((builder, config) -> {
                          if (isPayaraTracingEnabled()) {
                              return builder.addSpanProcessor(new PayaraRequestTracingProcessor(locator.getService(RequestTracingService.class)));
                          } else {
                              return builder;
                          }
                        })
                        .addPropertiesSupplier(() -> props)
                        .build().getOpenTelemetrySdk();
            } catch (ConfigurationException ce) {
                logger.log(Level.SEVERE, "Failed to configure OpenTelemetry for " + applicationName + " using classlaoder "
                        + Thread.currentThread().getContextClassLoader() +" will revert to no-op", ce);
                // Do not prevent application from working when things go awry in telemetry config
                return OpenTelemetrySdk.builder().build();
            }
        } else {
            // noop
            if (payaraTelemetryBootstrapFactoryServiceImpl.getAvailableNoopReference().isPresent()) {
                return payaraTelemetryBootstrapFactoryServiceImpl.getAvailableNoopReference().get();
            } 
        }
        //noop
        return OpenTelemetrySdk.builder().build();
    }

    private boolean isOtelEnabled(Map<String, String> configProperties) {
        boolean result = configProperties != null && !configProperties.isEmpty();
        if (!result) {
            result = "false".equalsIgnoreCase(System.getProperty("otel.sdk.disabled", "true"));
        }
        if (!result) {
            result = "false".equalsIgnoreCase(System.getenv("OTEL_SDK_DISABLED"));
        }
        return result;
    }

    /**
     * Very carefully ask whether Payara Request Tracing service is enabled.
     * The method might be invoked way sooner than it is appropriate time for request tracing to start,
     * or in environments where it is not supported (App Client).
     *
     * @return True if the Request Tracing Service is enabled
     */
    private boolean isPayaraTracingEnabled() {
        ServiceHandle<RequestTracingService> handle = locator.getServiceHandle(RequestTracingService.class);
        return handle != null && handle.isActive() && handle.getService().isRequestTracingEnabled();
    }

    private void addDefault(Map<String, String> props, String key, String value) {
        if (props.containsKey(key)) {
            return;
        }
        if (System.getProperty(key) != null) {
            return;
        }
        if (System.getenv(key.toUpperCase().replace('.', '_')) != null) {
            return;
        }
        props.put(key, value);
    }

    /**
     * Return Meter builder for given application
     *
     * @param applicationName
     * @return
     */
    private Optional<Meter> getMeter(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::meter);
    }

    /**
     * Return logger for given application.
     *
     * @param applicationName
     * @return
     */
    private Optional<io.opentelemetry.api.logs.Logger> getLogger(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::logger);
    }

    /**
     * Create new OpenTelemetry components for application. Shutdown previous ones if such already existed.
     *
     * @param applicationName
     * @param configProperties
     * @return
     */
    private OpenTelemetrySdk initializeApplication(String applicationName, Map<String, String> configProperties) {
        OpenTelemetrySdk sdk = createSdk(applicationName, configProperties);
        OpenTelemetryAppInfo previous = appTelemetries.put(applicationName, new OpenTelemetryAppInfo(sdk));
        if (previous != null) {
            previous.shutdown();
        }
        return sdk;
    }


    Optional<OpenTelemetrySdk> getSdk(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::sdk);
    }

    public Optional<OpenTelemetrySdk> getSdkDependency(String applicationName, Runnable shutdownListener) {
        return get(applicationName, appInfo -> {
            appInfo.addShutdownListener(shutdownListener);
            return appInfo.sdk();
        });

    }

    public boolean isEnabled() {
        String application = currentApplication();
        return application != null && appTelemetries.containsKey(application) || isPayaraTracingEnabled();
    }

    static class OpenTelemetryAppInfo {

        private final OpenTelemetrySdk sdk;

        private Tracer tracer;

        private Meter meter;

        private io.opentelemetry.api.logs.Logger logger;

        private List<Runnable> shutdownListeners;

        OpenTelemetryAppInfo(OpenTelemetrySdk sdk) {
            this.sdk = sdk;
        }

        Tracer tracer() {
            if (this.tracer == null) {
                this.tracer = sdk.getTracerProvider().get(INSTRUMENTATION_SCOPE_NAME);
            }
            return this.tracer;
        }

        Meter meter() {
            if (this.meter == null) {
                this.meter = sdk.getMeterProvider().get(INSTRUMENTATION_SCOPE_NAME);
            }
            return this.meter;
        }

        io.opentelemetry.api.logs.Logger logger() {
            if (this.logger == null) {
                this.logger = sdk.getSdkLoggerProvider().get(INSTRUMENTATION_SCOPE_NAME);
            }
            return this.logger;
        }

        OpenTelemetrySdk sdk() {
            return sdk;
        }

        synchronized void addShutdownListener(Runnable listener) {
            if (shutdownListeners == null) {
                // we don't expect many listeners
                shutdownListeners = new ArrayList<>(2);
            }
            shutdownListeners.add(listener);
        }

        synchronized void shutdown() {
            if (shutdownListeners != null) {
                shutdownListeners.forEach(Runnable::run);
            }
            // we need to shut it down properly. SDK providers offer both async shutdown meter as well as implement
            // Closeable, where shutdown is invoked in sync fashion. Let's be optimistic and start with async shutdown
            SdkTracerProvider tracerProvider = this.sdk.getSdkTracerProvider();
            if (tracerProvider != null) {
                tracerProvider.shutdown();
            }
            SdkMeterProvider meterProvider = this.sdk.getSdkMeterProvider();
            if (meterProvider != null) {
                meterProvider.shutdown();
            }
            SdkLoggerProvider logProvider = this.sdk.getSdkLoggerProvider();
            if (logProvider != null) {
                logProvider.shutdown();
            }
        }
    }

    class GlobalTelemetry implements OpenTelemetry {

        @Override
        public TracerProvider getTracerProvider() {
            return get(currentApplication(), appInfo -> appInfo.sdk().getTracerProvider()).orElse(TracerProvider.noop());
        }

        @Override
        public ContextPropagators getPropagators() {
            return get(currentApplication(), appInfo -> appInfo.sdk().getPropagators()).orElse(ContextPropagators.noop());
        }
    }

}
