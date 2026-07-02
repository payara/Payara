package fish.payara.opentracing;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

/**
 * Bridge OTEL traces to Payara Request Service if the service is enabled at time of Sdk creation
 */
@Service
class RequestTracingBridgeProvider implements AutoConfigurationCustomizerProvider {
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    RequestTracingServiceConfiguration requestTracingServiceConfiguration;

    @Inject
    IterableProvider<RequestTracingService> requestTracingServiceHandle;

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        if (Boolean.parseBoolean(requestTracingServiceConfiguration.getEnabled())) {
            autoConfiguration.addTracerProviderCustomizer((builder, config) ->
                builder.addSpanProcessor(new LazyRequestTracingProcessor())
            );
        }
    }

    class LazyRequestTracingProcessor implements SpanProcessor {
        private SpanProcessor delegate;


        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
            if (delegate == null) {
                return;
            }
            delegate.onStart(parentContext, span);
        }

        @Override
        public boolean isStartRequired() {
            if (delegate == null) {
                return reevaluate();
            }
            return false;
        }

        private boolean reevaluate() {
            if (!requestTracingServiceHandle.getHandle().isActive()) {
                return false;
            }
            delegate = new PayaraRequestTracingProcessor(requestTracingServiceHandle.get());
            return true;
        }

        @Override
        public void onEnd(ReadableSpan span) {
            if (delegate == null) {
                return;
            }
            delegate.onEnd(span);
        }

        @Override
        public boolean isEndRequired() {
            if (delegate == null) {
                return false;
            }
            return delegate.isEndRequired();
        }

        @Override
        public CompletableResultCode shutdown() {
            if (delegate != null) {
                return delegate.shutdown();
            }
            return SpanProcessor.super.shutdown();
        }

        @Override
        public CompletableResultCode forceFlush() {
            if (delegate != null) {
                return delegate.forceFlush();
            }
            return SpanProcessor.super.forceFlush();
        }
    }
}
