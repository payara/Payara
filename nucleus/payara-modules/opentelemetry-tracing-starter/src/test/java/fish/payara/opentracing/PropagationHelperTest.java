/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropagationHelperTest {

    private OpenTelemetrySdk sdk;
    private Tracer tracer;
    private TestSpanProcessor spanProcessor;

    @Before
    public void setUp() {
        spanProcessor = new TestSpanProcessor();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build();
        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        tracer = sdk.getTracer("test-tracer");
    }

    @After
    public void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    @Test
    public void testStatusSetBeforeEndOnSuccess() {
        Span span = tracer.spanBuilder("test-success").startSpan();
        PropagationHelper helper = PropagationHelper.start(span, Context.current());

        helper.end();
        helper.close();

        assertEquals(1, spanProcessor.endedSpans.size());
        SpanData data = spanProcessor.endedSpans.get(0);
        
        // Assert StatusCode remains UNSET on standard success (OTel Spec default)
        assertEquals(StatusCode.UNSET, data.getStatus().getStatusCode());
    }

    @Test
    public void testExistingStatusNotOverwrittenByEnd() {
        Span span = tracer.spanBuilder("test-preserve").startSpan();
        span.setStatus(StatusCode.ERROR, "Custom error");
        PropagationHelper helper = PropagationHelper.start(span, Context.current());

        helper.end();
        helper.close();

        assertEquals(1, spanProcessor.endedSpans.size());
        SpanData data = spanProcessor.endedSpans.get(0);
        
        // Assert the custom status is preserved and not overwritten to UNSET/OK
        assertEquals(StatusCode.ERROR, data.getStatus().getStatusCode());
        assertEquals("Custom error", data.getStatus().getDescription());
    }

    @Test
    public void testSpanEndedExactlyOnceOnError() {
        Span span = tracer.spanBuilder("test-error").startSpan();
        PropagationHelper helper = PropagationHelper.start(span, Context.current());

        helper.end(new RuntimeException("Test exception"));
        helper.close();

        // Verify that the span is ended exactly once (no leak)
        assertEquals(1, spanProcessor.endedSpans.size());
        SpanData data = spanProcessor.endedSpans.get(0);
        
        // Verify correct error status and recorded exception
        assertEquals(StatusCode.ERROR, data.getStatus().getStatusCode());
        assertEquals("Test exception", data.getStatus().getDescription());
        assertEquals(1, data.getEvents().size());
        assertEquals("exception", data.getEvents().get(0).getName());
    }

    private static class TestSpanProcessor implements SpanProcessor {
        final List<SpanData> endedSpans = new ArrayList<>();

        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {}

        @Override
        public boolean isStartRequired() { return false; }

        @Override
        public void onEnd(ReadableSpan span) {
            endedSpans.add(span.toSpanData());
        }

        @Override
        public boolean isEndRequired() { return true; }
    }
}
