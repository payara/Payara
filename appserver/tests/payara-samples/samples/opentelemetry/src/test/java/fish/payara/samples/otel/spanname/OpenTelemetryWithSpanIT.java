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
package fish.payara.samples.otel.spanname;

import fish.payara.samples.otel.annotation.SpanBean;
import fish.payara.samples.otel.annotation.WithSpanResource;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.ExceptionAttributes;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class OpenTelemetryWithSpanIT extends AbstractSpanNameTest {

    private static final Logger LOG = Logger.getLogger(OpenTelemetryWithSpanIT.class.getName());

    private static final String SPAN_NAME = "definedName";

    public static class SpanConfig extends Conf {
        public SpanConfig() {
            super(Map.of(SPAN_NAMING_KEY, "opentelemetry"));
        }
    }

    @Inject
    SpanBean spanBean;

    @Before
    public void setup() {
        exporter.reset();
    }

    @Deployment
    public static WebArchive deployment() {
        return configSource(base(), SpanConfig.class)
                .addClass(SpanBean.class)
                .addClass(WithSpanResource.class);
    }

    @Test
    public void testWithSpanShouldCreateSpan() {
        spanBean.span();
        var spans = exporter.getSpans();
        var expected = "fish.payara.samples.otel.annotation.SpanBean.span";
        assertEquals(1, spans.size());
        assertThat(spans).describedAs("Expecting span name of " + expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
    }

    @Test
    public void testWithSpanValueShouldCreateSpan() {
        spanBean.spanName();
        var spans = exporter.getSpans();
        var expected = SPAN_NAME;
        assertEquals(1, spans.size());
        assertThat(spans).describedAs("Expecting span name of "+expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
    }

    @Test
    public void testWithSpanKindShouldCreateSpan() {
        spanBean.spanKind();
        var spans = exporter.getSpans();
        var expected = SpanKind.SERVER;
        assertEquals(1, spans.size());
        assertThat(spans).describedAs("Expecting span kind of " + expected).anySatisfy(span -> assertThat(span.getKind()).isEqualTo(expected));
    }

    @Test
    public void testWithSpanAttributeShouldCreateSpan() {
        spanBean.spanArgs("stringAttr", true, 2, "noAttributeName", "noName");
        var spans = exporter.getSpans();
        assertEquals(1, spans.size());
        spans.stream().forEachOrdered(span -> {
            Attributes attrs = span.getAttributes();
            // Verify the three user-defined @SpanAttribute values are present.
            // Note: Payara's ThreadStampingProcessor also stamps thread.id and thread.name on
            // every span, so attrs.size() > 3 is expected and intentional.
            assertEquals("stringAttr", attrs.get(AttributeKey.stringKey("customStringAttribute")));
            assertEquals(true, attrs.get(AttributeKey.booleanKey("customBooleanAttribute")));
            assertEquals(Long.valueOf(2), attrs.get(AttributeKey.longKey("customIntegerAttribute")));
        });
    }

    @Test
    public void spanChild() {
        spanBean.spanChild();
        var spans = exporter.getSpans();
        assertEquals(2, spans.size());
        assertEquals("SpanChildBean.spanChild", spans.get(0).getName());
        assertEquals("fish.payara.samples.otel.annotation.SpanBean.spanChild", spans.get(1).getName());
        assertEquals(spans.get(0).getParentSpanId(), spans.get(1).getSpanId());
    }

    @Test
    public void testWithSpanWithinJaxrs() {
        var response = target(null, "withSpan", "span").request().get();
        assertEquals(200, response.getStatus());
        var spans = exporter.getSpans();
        var expected = "GET " + baseUri.getPath() + "jaxrs/withSpan/span";
        assertEquals("Expecting 3 span exists, which are first generated by @WithSpan of SpanBean, second and third generated by manually instrumented"
                , 3, spans.size());
        assertThat(spans).describedAs("Expecting span name of " + expected + " manually instrumented").anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
    }

    @Test
    public void testJaxRsWithSpanDirectAnnotated() {
        var response = target(null, "withSpan", "spanDirectAnnotated").request().get();
        assertEquals(200, response.getStatus());
        var spans = exporter.getSpans();
        var spanNames = spans.stream().map(s -> s.getName() + " (" + s.getKind() + ")").collect(java.util.stream.Collectors.toList());
        assertEquals("Spans found: " + spanNames, 2, spans.size());
        var expected = "GET " + baseUri.getPath() + "jaxrs/withSpan/spanDirectAnnotated";
        assertThat(spans).describedAs("Expecting span name of "+expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
    }

    @Test
    public void testWithSpanAsync() throws Exception {
        // Wait synchronously for the async completion so getSpans() runs on the test
        // thread after the span is ended — not inside a whenComplete lambda that would
        // race with subsequent tests and steal their spans from the exporter.
        String result = spanBean.asyncSpan().toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertEquals("OK", result);
        var spans = exporter.getSpans();
        assertEquals("Expecting 1 span generated by @WithSpan", 1, spans.size());
        assertEquals("fish.payara.samples.otel.annotation.SpanBean.asyncSpan", spans.get(0).getName());
    }

    @Test
    public void testWithSpanAsyncException() throws Exception {
        try {
            spanBean.asyncExceptionSpan().toCompletableFuture().get(10, TimeUnit.SECONDS);
        } catch (Exception expected) {
            // expected — the stage completes exceptionally
        }
        var spans = exporter.getSpans();
        assertEquals("Expecting 1 span generated by @WithSpan", 1, spans.size());
        var span = spans.get(0);
        assertEquals("fish.payara.samples.otel.annotation.SpanBean.asyncExceptionSpan", span.getName());
        // Span must be marked as ERROR status (not the legacy ad-hoc "error"=true attribute)
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        // EXCEPTION_TYPE must carry the actual exception class, not the generic "java.lang.Throwable"
        assertThat(span.getAttributes().get(ExceptionAttributes.EXCEPTION_TYPE))
                .isEqualTo(IllegalArgumentException.class.getName());
    }

    @Test
    public void testJaxrsWithSpanAnnotatedWithAttributes() {
        var response = target(null, "withSpan", "spanAnnotatedWithAttributes")
                .queryParam("q", "queryValue").request().get();
        assertEquals(200, response.getStatus());
        var spans = exporter.getSpans();
        assertEquals("Expecting have 2 spans generated by @WithSpan which are SERVER and CLIENT"
                , 2, spans.size());
        var expected = "GET " + baseUri.getPath() + "jaxrs/withSpan/spanAnnotatedWithAttributes";
        assertThat(spans).describedAs("Expecting span name of " + expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
        assertThat(spans).describedAs("Expecting specified attribute is exists").anySatisfy(span ->
                assertThat(span.getAttributes().get(AttributeKey.stringKey("query"))).isEqualTo("queryValue"));
    }
}
