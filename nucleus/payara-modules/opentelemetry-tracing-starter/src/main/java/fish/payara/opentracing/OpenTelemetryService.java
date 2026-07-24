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

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.telemetry.service.OpenTelemetryBootstrap;
import fish.payara.telemetry.service.PayaraTelemetryConstants;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages per-application OpenTelemetry SDK instances as well as export to
 * Payara Request Tracing Service.
 */
@Service(name = "opentelemetry-service")
@RunLevel(StartupRunLevel.VAL)
public class OpenTelemetryService implements EventListener {

    public static final String INSTRUMENTATION_SCOPE_NAME = "fish.payara.telemetry";

    private static final Logger logger = Logger.getLogger(OpenTelemetryService.class.getName());

    /***
     * Application Otel SDKs
     */
    private final Map<String, OpenTelemetrySdkHandle> appTelemetries = new ConcurrentHashMap<>();

    @Inject
    OpenTelemetryBootstrap openTelemetryBootstrap;

    @Inject
    Events events;

    @Inject
    ServiceLocator locator;

    @Inject
    InvocationManager invocationManager;

    @Inject
    ApplicationRegistry applicationRegistry;

    private OpenTelemetrySdkHandle runtimeHandle;

    private AppTelemetry appSpecificTelemetry;

    private static final OpenTelemetry NOOP = OpenTelemetry.noop();
    private static final Meter NOOP_METER = NOOP.getMeter("noop");
    private static final Tracer NOOP_TRACER = NOOP.getTracer("noop");

    /**
     * Check if OpenTelemetry SDK is active in current application context.
     * @return true if OpenTelemetry is enabled on runtime level or in currently invoking application.
     */
    public boolean isEnabled() {
        if (runtimeHandle != null) {
            return true;
        }
        String application = currentApplication();
        return application != null && appTelemetries.containsKey(application);
    }

    public Tracer getCurrentTracer() {
        return getCurrent(OpenTelemetrySdkHandle::tracer, NOOP_TRACER);
    }
    
    public Meter getCurrentMeter() {
        return getCurrent(OpenTelemetrySdkHandle::meter, NOOP_METER);
    }

    public OpenTelemetry getCurrentSdk() {
        return getCurrent(OpenTelemetrySdkHandle::sdk, NOOP);
    }

    /**
     * Returns the cached per-SDK {@code http.server.request.duration} histogram.
     * Must be called while the application invocation is active (i.e. inside
     * {@code StandardWrapper.service()}) so that app-mode resolution via
     * {@code InvocationManager} succeeds. The returned instance is safe to cache in
     * a request attribute and use later on any thread.
     *
     * @throws IllegalStateException if called in app-mode with no active invocation
     */
    public DoubleHistogram getRequestDurationHistogram() {
        var current = getCurrent(OpenTelemetrySdkHandle::requestDurationHistogram, null);
        if (current == null) {
            throw new IllegalStateException("OpenTelemetry is not initialized, but OTEL metric write was requested");
        }
        return current;
    }

    private <T> T getCurrent(Function<OpenTelemetrySdkHandle, T> getter, T fallback) {
        OpenTelemetrySdkHandle handle = runtimeHandle;
        if  (handle == null) {
            var appName = requiredApplicationName();
            handle = appTelemetries.get(appName);
        }
        if (handle == null) {
            return fallback;
        }
        T result = getter.apply(handle);
        if (result == null) {
            return fallback;
        }
        return result;
    }

    public void initializeCurrentApplication(Config appConfig) {
        if (runtimeHandle != null) {
            logger.log(Level.WARNING, "Runtime-level opentelemetry is configured, application configuration will be ignored");
            return;
        }
        initializeApplication(currentApplication(), appConfig);
        // runtime telemetry is disabled and we provably have app telemetry enabled. We'll route GlobalTelemetry over to us.
        if (appSpecificTelemetry == null) {
            appSpecificTelemetry = new AppTelemetry();
            openTelemetryBootstrap.setGlobalDelegate(appSpecificTelemetry);
        }
    }

    /**
     * Required when application registers CDI-based components.
     */
    public void shutdownCurrentApplication() {
        shutdown(currentApplication());
    }


    /**
     * Stores a raw W3C propagation carrier, operation name, span kind, and
     * span attributes for deferred span creation.  The carrier must NOT be
     * extracted here — the correct SDK may not be available yet.
     * Must be followed by {@link #applyDeferredContext()} once the application
     * invocation context is on the stack.
     *
     * @param carrier   raw W3C propagation headers (may be empty if no context was propagated)
     * @param operation logical operation/method name, used as the span name
     * @param spanKind  span kind ({@link SpanKind#SERVER} or {@link SpanKind#CLIENT})
     * @param attributes additional span attributes supplied by the caller
     */
    public void collectDeferredContext(HashMap<String, String> carrier, String operation,
                                       SpanKind spanKind, Attributes attributes) {
        DeferredContext.set(carrier, operation, spanKind, attributes);
    }

    /**
     * Extracts the OTel context from the stored carrier using the correct
     * per-application propagator (the application invocation must be on the stack),
     * then creates and activates a span with the kind and attributes provided to
     * {@link #collectDeferredContext}.
     *
     * @return {@code true} if a span was started and the caller is responsible for
     *         calling {@link #endDeferredSpan(Throwable)} when the operation completes;
     *         {@code false} if nothing was done (no deferred context, or OTel not
     *         enabled for the current application)
     */
    public boolean applyDeferredContext() {
        DeferredContext ctx = DeferredContext.get();
        if (ctx == null) {
            return false;
        }
        if (!isEnabled()) {
            DeferredContext.remove();
            return false;
        }
        // Use getCurrentSdk() rather than GlobalOpenTelemetry.get() so that both
        // extraction and span creation use the same per-app SDK instance consistently.
        ctx.apply(getCurrentSdk().getPropagators(), getCurrentTracer(), Context.current());
        return true;
    }

    /**
     * Ends the span previously started by {@link #applyDeferredContext()}.
     *
     * @param error the exception that caused the failure, or {@code null} on success
     */
    public void endDeferredSpan(Throwable error) {
        DeferredContext ctx = DeferredContext.get();
        DeferredContext.remove();
        if (ctx != null) {
            ctx.end(error);
        }
    }

    @PostConstruct
    void postConstruct() {
        if (events != null) {
            events.register(this);
        } else {
            logger.log(Level.WARNING, "OpenTelemetry service not registered to Payara Events: "
                    + "The Tracer for an application won't be removed upon undeployment");
        }
        OpenTelemetrySdk runtimeSdk = openTelemetryBootstrap.getRuntimeSdk();
        if (runtimeSdk != null) {
            this.runtimeHandle = new OpenTelemetrySdkHandle(runtimeSdk);
        }
    }

    @PreDestroy
    void stopAll() {
        appTelemetries.values().forEach(OpenTelemetrySdkHandle::shutdown);
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


    @Deprecated
    public DoubleHistogram createMetricsHistogram(OpenTelemetry instance) {
        return instance.getMeterProvider().get(INSTRUMENTATION_SCOPE_NAME)
                .histogramBuilder(PayaraTelemetryConstants.HTTP_SERVER_REQUEST_DURATION_NAME)
                .setUnit(PayaraTelemetryConstants.OTEL_SECONDS_UNIT)
                .setDescription(PayaraTelemetryConstants.HTTP_SERVER_REQUEST_DURATION_DESC)
                .setExplicitBucketBoundariesAdvice(PayaraTelemetryConstants.BUCKET_BOUNDARIES_LIST).build();
    }

    private String requiredApplicationName() {
        var appName = currentApplication();
        if (appName == null) {
            throw new IllegalStateException("No application code is executing. Cannot determine current application scope");
        }
        return appName;
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
    private OpenTelemetrySdk createSdk(String applicationName, Config configProperties) {
        if (isOtelEnabled(configProperties) || isPayaraTracingEnabled()) {
            try {
                return openTelemetryBootstrap.buildApplicationSdk(applicationName, configProperties, null);
            } catch (ConfigurationException ce) {
                logger.log(Level.SEVERE, "Failed to configure OpenTelemetry for " + applicationName + " using classlaoder "
                        + Thread.currentThread().getContextClassLoader() +" will revert to no-op", ce);
                // Do not prevent application from working when things go awry in telemetry config
            }
        }
        //noop
        return OpenTelemetrySdk.builder().build();
    }

    private boolean isOtelEnabled(Config configProperties) {
        return configProperties != null && !configProperties.getOptionalValue(PayaraTelemetryConstants.OTEL_SDK_DISABLED, Boolean.class).orElse(true);
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

    /**
     * Create new OpenTelemetry components for application. Shutdown previous ones if such already existed.
     *
     * @param applicationName
     * @param config application's MP Config instance
     * @return
     */
    private void initializeApplication(String applicationName, Config config) {
        OpenTelemetrySdk sdk = createSdk(applicationName, config);
        OpenTelemetrySdkHandle previous = appTelemetries.put(applicationName, new OpenTelemetrySdkHandle(sdk));
        if (previous != null) {
            previous.shutdown();
        }
    }

    @Deprecated
    public String getCurrentApplicationName() {
        return currentApplication();
    }

    static class OpenTelemetrySdkHandle {

        private final OpenTelemetrySdk sdk;

        private Tracer tracer;

        private Meter meter;

        private io.opentelemetry.api.logs.Logger logger;

        private DoubleHistogram requestDurationHistogram;

        OpenTelemetrySdkHandle(OpenTelemetrySdk sdk) {
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

        /**
         * Returns a cached {@link DoubleHistogram} for {@code http.server.request.duration}.
         * Built once per SDK instance; reused on every request. Never call
         * {@code createMetricsHistogram} per-request — that is the bug this replaces.
         */
        DoubleHistogram requestDurationHistogram() {
            if (this.requestDurationHistogram == null) {
                this.requestDurationHistogram = meter()
                        .histogramBuilder(PayaraTelemetryConstants.HTTP_SERVER_REQUEST_DURATION_NAME)
                        .setUnit(PayaraTelemetryConstants.OTEL_SECONDS_UNIT)
                        .setDescription(PayaraTelemetryConstants.HTTP_SERVER_REQUEST_DURATION_DESC)
                        .setExplicitBucketBoundariesAdvice(PayaraTelemetryConstants.BUCKET_BOUNDARIES_LIST)
                        .build();
            }
            return this.requestDurationHistogram;
        }

        OpenTelemetrySdk sdk() {
            return sdk;
        }

        synchronized void shutdown() {
            sdk.close();
        }
    }

    class AppTelemetry implements OpenTelemetry {

        @Override
        public TracerProvider getTracerProvider() {
            return getCurrentSdk().getTracerProvider();
        }

        @Override
        public ContextPropagators getPropagators() {
            return getCurrentSdk().getPropagators();
        }

        @Override
        public MeterProvider getMeterProvider() {
            return getCurrentSdk().getMeterProvider();
        }

        @Override
        public LoggerProvider getLogsBridge() {
            return getCurrentSdk().getLogsBridge();
        }
    }

}
