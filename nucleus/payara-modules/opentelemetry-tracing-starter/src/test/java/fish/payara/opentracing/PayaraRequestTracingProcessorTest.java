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
 *  enclosed by brackets [] replaced with your identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees da above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.opentracing;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class PayaraRequestTracingProcessorTest {

    private Tracer tracer(RequestTracingService rt) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(new PayaraRequestTracingProcessor(rt))
                .build().get("test");
    }

    private Tracer tracer(RequestTracingService rt, Sampler sampler) {
        return SdkTracerProvider.builder()
                .setSampler(sampler)
                .addSpanProcessor(new PayaraRequestTracingProcessor(rt))
                .build().get("test");
    }

    @Test
    public void localRootSpanStartsAndEndsTrace() {
        RequestTracingService rt = mock(RequestTracingService.class);
        Span root = tracer(rt).spanBuilder("GET").startSpan();
        ArgumentCaptor<RequestTraceSpan> startCaptor = ArgumentCaptor.forClass(RequestTraceSpan.class);
        verify(rt).startTrace(startCaptor.capture());
        RequestTraceSpan capturedRoot = startCaptor.getValue();
        root.end();
        ArgumentCaptor<RequestTraceSpan> endCaptor = ArgumentCaptor.forClass(RequestTraceSpan.class);
        verify(rt).traceSpan(endCaptor.capture(), anyLong());
        assertEquals("Same root span should be passed to traceSpan", capturedRoot, endCaptor.getValue());
    }

    @Test
    public void childSpanIsTracedNotStarted() {
        RequestTracingService rt = mock(RequestTracingService.class);
        Tracer t = tracer(rt);
        Span root = t.spanBuilder("GET").startSpan();
        Span child = t.spanBuilder("child").setParent(Context.root().with(root)).startSpan();
        child.end();
        root.end();
        verify(rt, times(1)).startTrace(any(RequestTraceSpan.class));
        ArgumentCaptor<RequestTraceSpan> traceSpanCaptor = ArgumentCaptor.forClass(RequestTraceSpan.class);
        verify(rt, times(2)).traceSpan(traceSpanCaptor.capture(), anyLong());
        // Verify that one of the traced spans is the child
        boolean childFound = traceSpanCaptor.getAllValues().stream()
                .anyMatch(span -> "child".equals(span.getEventName()));
        assertEquals("Child span should be traced", true, childFound);
    }

    @Test
    public void remoteParentSpanStartsPropagatedTrace() {
        RequestTracingService rt = mock(RequestTracingService.class);
        // Build a remote parent context
        SpanContext remoteCtx = SpanContext.createFromRemoteParent(
                "12345678901234567890123456789012",
                "1234567890123456",
                TraceFlags.getSampled(),
                TraceState.getDefault());
        Span root = tracer(rt).spanBuilder("remote-child")
                .setParent(Context.root().with(Span.wrap(remoteCtx)))
                .startSpan();
        ArgumentCaptor<RequestTraceSpan> startCaptor = ArgumentCaptor.forClass(RequestTraceSpan.class);
        verify(rt).startTrace(startCaptor.capture());
        RequestTraceSpan capturedSpan = startCaptor.getValue();
        assertEquals("Remote parent span should be PROPAGATED_TRACE", EventType.PROPAGATED_TRACE, capturedSpan.getEventType());
        root.end();
        verify(rt).traceSpan(same(capturedSpan), anyLong());
    }

    @Test
    public void unsampledSpanCausesNoServiceCalls() {
        RequestTracingService rt = mock(RequestTracingService.class);
        Span root = tracer(rt, Sampler.alwaysOff()).spanBuilder("unsampled").startSpan();
        root.end();
        verifyNoInteractions(rt);
    }
}
