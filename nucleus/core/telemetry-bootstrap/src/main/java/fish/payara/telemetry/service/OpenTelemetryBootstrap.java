/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.telemetry.service;


import com.sun.appserv.server.util.Version;
import fish.payara.nucleus.microprofile.config.ServerConfigProvider;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.common.util.Constants;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.Rank;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.InitRunLevel;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

import static fish.payara.telemetry.service.PayaraTelemetryConstants.OTEL_ENV_PREFIX;
import static fish.payara.telemetry.service.PayaraTelemetryConstants.OTEL_LOGS_EXPORTER;
import static fish.payara.telemetry.service.PayaraTelemetryConstants.OTEL_METRICS_EXPORTER;
import static fish.payara.telemetry.service.PayaraTelemetryConstants.OTEL_PROPERTY_PREFIX;
import static fish.payara.telemetry.service.PayaraTelemetryConstants.OTEL_SDK_DISABLED;
import static fish.payara.telemetry.service.PayaraTelemetryConstants.OTEL_TRACES_EXPORTER;

@Service
@RunLevel(InitRunLevel.VAL)
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
public class OpenTelemetryBootstrap {
    @Inject
    ServerConfigProvider serverConfigProvider;

    @Inject
    ServerEnvironment serverEnvironment;

    private OpenTelemetrySdk runtimeSdk = null;
    
    private static final OpenTelemetry noopInstance = OpenTelemetry.noop();

    private final DelegatedSdk delegate = new DelegatedSdk(noopInstance);

    private boolean runtimeSdkDisabled;
    private JvmMetrics jvmMetrics;

    @Inject
    IterableProvider<AutoConfigurationCustomizerProvider> serverCustomizations;

    @Inject
    IterableProvider<AutoConfigureListener> sdkListeners;

    @PostConstruct
    void init() {
        GlobalOpenTelemetry.set(delegate);
        runtimeSdkDisabled = checkRuntimeTelemetryDisabled();
        if (!runtimeSdkDisabled) {
            createTelemetryRuntimeInstance();
        }
    }

    @PreDestroy
    void shutdown() {
        if (jvmMetrics != null) {
            jvmMetrics.close();
        }
        if (runtimeSdk != null) {
            runtimeSdk.close();
        }
    }

    /**
     * Reports the state of server-wide ("runtime" per MP Telemetry Spec) telemetry.
     * <p>
     * The logic may seem slightly inverted, but user needs to explicitly set {@code otel.sdk.disabled} to {@code false}
     * in order to enable runtime telemetry.
     * </p>
     * @return {@code true} if runtime telemetry is disabled (the default); {@code false} if it is enabled.
     */
    public boolean isRuntimeTelemetryDisabled() {
        return runtimeSdkDisabled;
    }

    /**
     * Return server-wide (aka runtime) SDK if available
     * @return configured OpenTelemetry SDK instance or {@code null} if runtime telemetry is not enabled.
     */
    public OpenTelemetrySdk getRuntimeSdk() {
        return runtimeSdk;
    }

    public void setGlobalDelegate(OpenTelemetry globalDelegate) {
        if (runtimeSdk != null) {
            throw new IllegalStateException("Runtime Telemetry is configured, other telemetry implementation is not anticipated.");
        }
        delegate.setDelegate(globalDelegate);
    }

    public OpenTelemetrySdk buildApplicationSdk(String applicationName, Config configProperties, Consumer<AutoConfigurationCustomizer> customizer) {
        return initializeSdk(collectOtelProperties(configProperties), b -> {
                b.addResourceCustomizer((resource, config) -> addApplicationNameAsServiceName(resource, applicationName));
                if (customizer != null) {
                    customizer.accept(b);
                }
        });
    }

    private void createTelemetryRuntimeInstance() {
        runtimeSdk = initializeSdk(collectOtelProperties(serverConfig()),
                b -> b.addResourceCustomizer(this::addEnvironmentAsServiceName));
        delegate.setDelegate(runtimeSdk);

        bootstrapSignals();
    }

    private void bootstrapSignals() {
        Meter meter = runtimeSdk.getMeterProvider().get("payara-runtime");
        jvmMetrics = new JvmMetrics(meter);
    }


    private OpenTelemetrySdk initializeSdk(Map<String, String> props, Consumer<AutoConfigurationCustomizer> customizer) {
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .addPropertiesSupplier(() -> props)
                .addPropertiesCustomizer(this::requireExplicitExporters)
                .addResourceCustomizer(this::addDefaultResourceAttributes)
                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                .disableShutdownHook();
        TreeSet<AutoConfigurationCustomizerProvider> customizers = new TreeSet<>(Comparator.comparingInt(AutoConfigurationCustomizerProvider::order));
        serverCustomizations.forEach(customizers::add);
        customizers.forEach(c -> c.customize(builder));
        if (customizer != null) {
            customizer.accept(builder);
        }
        var sdk = builder.build();
        notifySdkCreated(sdk);
        return sdk.getOpenTelemetrySdk();
    }

    private void notifySdkCreated(AutoConfiguredOpenTelemetrySdk sdk) {
        sdkListeners.forEach(l -> l.afterAutoConfigure(sdk.getOpenTelemetrySdk()));
    }

    /** Require explicit specification of exporters for all signals; default to "none" if unset. */
    private Map<String, String> requireExplicitExporters(ConfigProperties configProperties) {
        Map<String, String> defaults = new HashMap<>();
        if (configProperties.getString(OTEL_LOGS_EXPORTER) == null) {
            defaults.put(OTEL_LOGS_EXPORTER, "none");
        }
        if (configProperties.getString(OTEL_TRACES_EXPORTER) == null) {
            defaults.put(OTEL_TRACES_EXPORTER, "none");
        }
        if (configProperties.getString(OTEL_METRICS_EXPORTER) == null) {
            defaults.put(OTEL_METRICS_EXPORTER, "none");
        }
        return defaults;
    }

    private boolean checkRuntimeTelemetryDisabled() {
        return serverConfig().getOptionalValue(OTEL_SDK_DISABLED, Boolean.class).orElse(true);
    }

    private Config serverConfig() {
        return serverConfigProvider.getConfig();
    }

    private Map<String, String> collectOtelProperties(Config config) {
        if (config == null) {
            return Map.of();
        }
        Map<String, String> props = new HashMap<>();
        for (var property : config.getPropertyNames()) {
            if (!property.startsWith(OTEL_PROPERTY_PREFIX) && !property.startsWith(OTEL_ENV_PREFIX)) {
                continue;
            }
            props.put(normalizePropertyName(property), config.getValue(property, String.class));
        }
        return props;
    }

    private String normalizePropertyName(String property) {
        return property.replace('_', '.').toLowerCase();
    }

    private Resource addDefaultResourceAttributes(Resource resource, ConfigProperties configProperties) {
        ResourceBuilder defaultResource = createDefaultResource();
        // we only override attributes that are not already auto-discovered
        defaultResource.removeIf(a -> resource.getAttribute(a) != null);
        return resource.merge(defaultResource.build());
    }

    private ResourceBuilder createDefaultResource() {
        var platform = ManagementFactory.getRuntimeMXBean();
        return Resource.builder()
                .put("payara.server.domain", serverEnvironment.getDomainName())
                .put("payara.server.instance", serverEnvironment.getInstanceName())
                .put("payara.server.runtime_type", serverEnvironment.getRuntimeType().name().toLowerCase())
                .put("payara.version", Version.getVersion())
                // we may consider static instance id stored in server config later
                .put("service.instance.id", UUID.randomUUID().toString())
                .put("jvm.vm.name", platform.getVmName())
                .put("jvm.vm.vendor", platform.getVmVendor())
                .put("jvm.vm.version", platform.getVmVersion());
    }

    private Resource addEnvironmentAsServiceName(Resource resource, ConfigProperties configProperties) {
        if (missingOtelService(resource)) {
            return resource.toBuilder().put("service.name", serverEnvironment.getDomainName() + "-" + serverEnvironment.getInstanceName()).build();
        }
        return resource;
    }

    private Resource addApplicationNameAsServiceName(Resource resource, String applicationName) {
        if (missingOtelService(resource)) {
            return resource.toBuilder().put("service.name", applicationName).build();
        }
        return resource;
    }

    private static boolean missingOtelService(Resource resource) {
        String serviceName = resource.getAttribute(AttributeKey.stringKey("service.name"));
        return serviceName == null || "unknown_service:java".equals(serviceName);
    }

    static class DelegatedSdk implements OpenTelemetry {

        private OpenTelemetry delegate;

        private DelegatedSdk(OpenTelemetry delegate) {
            this.delegate = delegate;
        }

        private void setDelegate(OpenTelemetry delegate) {
            this.delegate = delegate;
        }

        @Override
        public TracerProvider getTracerProvider() {
            return delegate.getTracerProvider();
        }

        @Override
        public Tracer getTracer(String instrumentationScopeName) {
            return delegate.getTracer(instrumentationScopeName);
        }

        @Override
        public Tracer getTracer(String instrumentationScopeName, String instrumentationScopeVersion) {
            return delegate.getTracer(instrumentationScopeName, instrumentationScopeVersion);
        }

        @Override
        public TracerBuilder tracerBuilder(String instrumentationScopeName) {
            return delegate.tracerBuilder(instrumentationScopeName);
        }

        @Override
        public MeterProvider getMeterProvider() {
            return delegate.getMeterProvider();
        }

        @Override
        public Meter getMeter(String instrumentationScopeName) {
            return delegate.getMeter(instrumentationScopeName);
        }

        @Override
        public MeterBuilder meterBuilder(String instrumentationScopeName) {
            return delegate.meterBuilder(instrumentationScopeName);
        }

        @Override
        public LoggerProvider getLogsBridge() {
            return delegate.getLogsBridge();
        }

        @Override
        public ContextPropagators getPropagators() {
            return delegate.getPropagators();
        }
    }
}
