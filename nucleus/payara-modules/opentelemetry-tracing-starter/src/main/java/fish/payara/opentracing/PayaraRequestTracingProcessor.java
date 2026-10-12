/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.opentracing;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.notification.requesttracing.RequestTraceSpanLog;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Maps OpenTelemetry spans to Payara Tracing Spans.
 * The models are quite different, OTEL one being much more precise. In order to keep Payara Tracing running, we feed it
 * with following data:
 * <ul>
 *     <li>Sampled root spans, and propagated spans are created as root spans in tracing service.</li>
 *     <li>Payara spans are created with parent reference to OTEL spans</li>
 *     <li>non-root spans are traced as "request spans", which should make them included in current trace</li>
 *     <li>A root span that ends on a different thread than it started on (async servlet) is reported as a root-only trace on the ending thread; children recorded on the starting thread are not included.</li>
 * </ul>
 */
class PayaraRequestTracingProcessor implements SpanProcessor {
    private final RequestTracingService requestTracingService;

    private static final class RootState {
        final RequestTraceSpan span;
        final Thread startThread;

        RootState(RequestTraceSpan span, Thread startThread) {
            this.span = span;
            this.startThread = startThread;
        }
    }

    private ConcurrentMap<ReadableSpan, RootState> rootInProgressSpans = new ConcurrentHashMap<>();

    public PayaraRequestTracingProcessor(RequestTracingService requestTracingService) {
        this.requestTracingService = requestTracingService;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        if (!span.getSpanContext().isSampled() || !requestTracingService.isRequestTracingEnabled()) {
            return;
        }
        var parent = span.getParentSpanContext();
        if (!parent.isValid() || parent.isRemote()) {
            // The trace store is thread-bound: drop a trace left open on this thread (e.g. its root ended on another thread).
            requestTracingService.discardTrace();
            var payaraSpan = createSpan(span, parent.isValid() ? EventType.PROPAGATED_TRACE : EventType.TRACE_START);
            rootInProgressSpans.put(span, new RootState(payaraSpan, Thread.currentThread()));
            requestTracingService.startTrace(payaraSpan);
        }
    }

    private static RequestTraceSpan createSpan(ReadableSpan span, EventType type) {
        return new RequestTraceSpan(type, span.getName(), parseTraceId(span.getSpanContext().getTraceId()),
                parseTraceId(span.getSpanContext().getSpanId()), RequestTraceSpan.SpanContextRelationshipType.ChildOf);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (!span.getSpanContext().isSampled()) {
            return;
        }
        RootState root = rootInProgressSpans.remove(span);
        if (!requestTracingService.isRequestTracingEnabled()) {
            return;
        }
        var data = span.toSpanData();
        RequestTraceSpan payaraSpan = root != null ? root.span : createTraceSpan(data);
        fill(data, payaraSpan);
        long endMillis = TimeUnit.NANOSECONDS.toMillis(data.getEndEpochNanos());
        if (root != null && root.startThread != Thread.currentThread()) {
            // Root finished on another thread (async): the trace opened on the start thread cannot be closed from here,
            // so report the root on this thread. Children recorded on the start thread are not included.
            if (requestTracingService.startTrace(payaraSpan) == null) {
                return;
            }
        }
        requestTracingService.traceSpan(payaraSpan, endMillis);
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    private RequestTraceSpan fill(SpanData data, RequestTraceSpan result) {
        result.setStartInstant(Instant.ofEpochSecond(
                data.getStartEpochNanos() / NANOS_PER_SECOND,
                data.getStartEpochNanos() % NANOS_PER_SECOND));
        result.setTraceEndTime(Instant.ofEpochSecond(data.getStartEpochNanos() / NANOS_PER_SECOND,
                data.getStartEpochNanos() % NANOS_PER_SECOND));
        result.setSpanDuration(data.getEndEpochNanos() - data.getStartEpochNanos());
        data.getAttributes().forEach((key, value) -> result.addSpanTag(key.getKey(), value.toString()));
        data.getEvents().forEach(ev -> result.addSpanLog(convert(ev)));
        // exporter has no access to baggage, that goes to propagators only.
        return result;
    }

    private RequestTraceSpan createTraceSpan(SpanData data) {
        RequestTraceSpan result = new RequestTraceSpan(EventType.PROPAGATED_TRACE, data.getName(), parseTraceId(data),
                parseTraceId(data.getSpanId()), RequestTraceSpan.SpanContextRelationshipType.ChildOf);
        return result;
    }

    private RequestTraceSpanLog convert(EventData ev) {
        var result = new RequestTraceSpanLog(TimeUnit.NANOSECONDS.toMillis(ev.getEpochNanos()), ev.getName());
        ev.getAttributes().forEach((k, v) -> result.addLogEntry(k.getKey(), String.valueOf(v)));
        return result;
    }

    private UUID parseTraceId(SpanData data) {
        return parseTraceId(data.getTraceId());
    }

    static UUID parseTraceId(String traceId) {
        if (traceId.length() >= 32) {
            // Long.parseLong does not like unsigned data
            // we trim everything beyond 32nd byte
            long high = parseUnsignedHex(traceId, 0, 0);
            long low = parseUnsignedHex(traceId, 16, 0);
            return new UUID(high, low);
        } else {
            long high = 0;
            if (traceId.length() > 16) {
                high = parseUnsignedHex(traceId, 0, 32 - traceId.length());
            }
            long low = parseUnsignedHex(traceId, Math.max(traceId.length() - 16, 0), Math.max(16 - traceId.length(), 0));
            return new UUID(high, low);
        }
    }

    static long parseUnsignedHex(CharSequence input, int start, int padding) {
        long result = 0;
        for (int i = start, shift = 60 - 4 * padding; shift >= 0; i++, shift -= 4) {
            long digit = Character.digit(input.charAt(i), 16);
            result |= digit << shift;
        }
        return result;
    }
}
