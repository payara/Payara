package fish.payara.opentracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.HashMap;

/**
 * Carries a raw W3C propagation carrier, operation name, span kind, and
 * attributes across a boundary where the OTel SDK or invocation context is not
 * yet available — for example, between a CORBA PortableInterceptor callback
 * (no invocation context) and the EJB container {@code preInvoke} (invocation
 * established, correct per-app SDK available).
 * <p>
 * Context extraction is deliberately deferred until
 * {@link #applyDeferredContext()} so that the correct per-application propagator
 * is used.  The same object is reused to hold the {@link PropagationHelper} once
 * the span has been started.
 */
final class DeferredContext {
    private static final ThreadLocal<DeferredContext> local = new ThreadLocal<>();

    static DeferredContext get() {
        return local.get();
    }

    static void set(HashMap<String, String> carrier, String operation, SpanKind spanKind, Attributes attributes) {
        local.set(new DeferredContext(carrier, operation, spanKind, attributes));
    }

    public static void remove() {
        local.remove();
    }


    static final TextMapGetter<HashMap<String, String>> CARRIER_GETTER =
            new TextMapGetter<HashMap<String, String>>() {
                @Override public Iterable<String> keys(HashMap<String, String> c) { return c.keySet(); }
                @Override public String get(HashMap<String, String> c, String key) { return c.get(key); }
            };
    private final HashMap<String, String> carrier;
    private final String operation;
    private final SpanKind spanKind;
    private final Attributes attributes;
    private PropagationHelper spanHelper;

    DeferredContext(HashMap<String, String> carrier, String operation,
                    SpanKind spanKind, Attributes attributes) {
        this.carrier = carrier;
        this.operation = operation;
        this.spanKind = spanKind;
        this.attributes = attributes;
    }

    void apply(ContextPropagators propagators, Tracer currentTracer, Context current) {
        Context parentContext = propagators.getTextMapPropagator()
                .extract(current, carrier, CARRIER_GETTER);
        var span = currentTracer
                .spanBuilder(operation)
                .setParent(parentContext)
                .setSpanKind(spanKind)
                .setAllAttributes(attributes)
                .startSpan();
        this.spanHelper = PropagationHelper.start(span, parentContext);
    }

    void end(Throwable error) {
        if (spanHelper != null) {
            spanHelper.end(error);
            spanHelper.close();
        }
    }
}
