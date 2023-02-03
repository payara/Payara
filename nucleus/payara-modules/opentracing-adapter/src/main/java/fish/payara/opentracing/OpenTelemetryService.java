/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

/**
 * Service class for the OpenTracing integration.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "opentelemetry-service")
public class OpenTelemetryService implements EventListener {

    public static final String INSTRUMENTATION_SCOPE_NAME = "payara";

    // The tracer instances
    private static final Map<String, OpenTelemetryAppInfo> appTelemetries = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(OpenTelemetryService.class.getName());

    private static long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    @Inject
    Events events;

    @Inject
    PayaraExecutorService executorService;

    @Inject
    private RequestTracingService requestTracingService;

    @PostConstruct
    void postConstruct() {
        if (events != null) {
            events.register(this);
        } else {
            logger.log(Level.WARNING, "OpenTracing service not registered to Payara Events: "
                    + "The Tracer for an application won't be removed upon undeployment");
        }
    }

    @Override
    public void event(Event<?> event) {
        // Listen for application unloaded events (happens during undeployment), so that we remove the tracer instance
        // registered to that application (if there is one)
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            OpenTelemetryAppInfo otel = appTelemetries.remove(info.getName());
            if (otel != null) {
                otel.shutdown();
            }
        }
    }

    /**
     * Return existing tracer for a given application.
     * <p>Because Telemetry SDKs are configurable per app, the tracer need to be explicitly created for an application</p>
     *
     * @param applicationName
     * @return
     */
    public Optional<Tracer> getTracer(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::tracer);
    }

    private <T> Optional<T> get(String applicationName, Function<OpenTelemetryAppInfo, T> getter) {
        if (applicationName == null) {
            return Optional.empty();
        }
        OpenTelemetryAppInfo appInfo = appTelemetries.get(applicationName);
        if (appInfo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getter.apply(appInfo));
    }

    /**
     * Return Meter builder for given application
     *
     * @param applicationName
     * @return
     */
    public Optional<Meter> getMeter(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::meter);
    }

    /**
     * Return logger for given application.
     *
     * @param applicationName
     * @return
     */
    public Optional<io.opentelemetry.api.logs.Logger> getLogger(String applicationName) {
        return get(applicationName, OpenTelemetryAppInfo::logger);
    }

    public OpenTelemetrySdk initializeApplication(String applicationName, Map<String, String> configProperties) {
        Supplier<Map<String, String>> supplier = configProperties == null ? () -> Map.of() : () -> configProperties;

        AutoConfiguredOpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.builder()
                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())

                .addSpanExporterCustomizer((exporter, c) -> {
                    if (isPayaraTracingEnabled()) {
                        return SpanExporter.composite(exporter, new PayaraRequestTracingForwarder());
                    } else {
                        return exporter;
                    }
                })
                .addPropertiesSupplier(supplier)
                .addResourceCustomizer((resource, config) -> {
                    if (resource.getAttributes().get(ResourceAttributes.SERVICE_NAME) == null) {
                        // there are few more attributes to add later to identify domain, payara version, etc.
                        return resource.toBuilder().put(ResourceAttributes.SERVICE_NAME, applicationName).build();
                    } else {
                        return resource;
                    }
                })
                .build();
        OpenTelemetryAppInfo previous = appTelemetries.put(applicationName, new OpenTelemetryAppInfo(sdk.getOpenTelemetrySdk()));
        if (previous != null) {
            previous.shutdown();
        }
        ;
        return sdk.getOpenTelemetrySdk();
    }

    /**
     * Pass-through method that checks if Request Tracing is enabled.
     *
     * @return True if the Request Tracing Service is enabled
     */
    public boolean isPayaraTracingEnabled() {
        return requestTracingService.isRequestTracingEnabled();
    }

    static class OpenTelemetryAppInfo {

        private final OpenTelemetrySdk sdk;

        private Tracer tracer;

        private Meter meter;

        private io.opentelemetry.api.logs.Logger logger;

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

        void shutdown() {
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

    class PayaraRequestTracingForwarder implements SpanExporter {

        @Override
        public CompletableResultCode export(Collection<SpanData> collection) {
            CompletableResultCode result = new CompletableResultCode();
            executorService.submit(() -> {
                try {
                    collection.forEach(this::convert);
                    result.succeed();
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Failed to export OpenTelemetry span to Payara Request Tracing", e);
                    result.fail();
                }
            });
            return result;
        }


        RequestTraceSpan convert(SpanData data) {
            RequestTraceSpan result = new RequestTraceSpan(EventType.PROPAGATED_TRACE, data.getName());
            result.getSpanContext().setTraceId(parseTraceId(data));
            // we cannot set spanId
            result.setStartInstant(Instant.ofEpochSecond(data.getStartEpochNanos() / NANOS_PER_SECOND, data.getStartEpochNanos() % NANOS_PER_SECOND));
            result.setSpanDuration(data.getEndEpochNanos() - data.getStartEpochNanos());
            data.getAttributes().forEach((key, value) -> result.addSpanTag(key.getKey(), value.toString()));
            // exporter has no access to baggage, that goes to propagators only.
            return result;
        }

        private UUID parseTraceId(SpanData data) {
            long high = Long.parseLong(data.getTraceId().substring(0, 16), 16);
            long low = Long.parseLong(data.getTraceId().substring(16), 16);
            return new UUID(high, low);
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }

}
