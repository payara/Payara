/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

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
        assertThat(spans).describedAs("Expecting span name of "+expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
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
            // as per spec, only add if only SpanAttribute value is specified
            assertEquals(3, attrs.size());
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
        assertEquals("fish.payara.samples.otel.annotation.SpanBean.SpanChildBean.spanChild", spans.get(0).getName());
        assertEquals("fish.payara.samples.otel.annotation.SpanBean.spanChild", spans.get(1).getName());
        assertEquals(spans.get(0).getParentSpanId(), spans.get(1).getSpanId());
    }

    @Test
    public void testWithSpanWithinJaxrs() {
        var response = target(null, "withSpan", "span").request().get();
        assertEquals(200, response.getStatus());
        var spans = exporter.getSpans();
        var expected = "fish.payara.samples.otel.annotation.SpanBean.span";
        assertEquals("Expecting 3 span exists, which are 1 from @WithSpan of SpanBean, 2 and 3 from JAXRS server and client"
                , 3, spans.size());
        assertThat(spans).describedAs("Expecting span name of "+expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
    }

    @Test
    public void testWithSpanDirectJaxrs() {
        var response = target(null, "withSpan", "spanDirectAnnotated").request().get();
        assertEquals(200, response.getStatus());
        var spans = exporter.getSpans();
        assertEquals("Expecting have 2 spans from JAXRS which are SERVER and CLIENT kind without having span started by @WithSpan"
                , 2, spans.size());
        var expected = "fish.payara.samples.otel.annotation.WithSpanResource.getSpanAnnotatedOnDirectMethod";
        assertThat(spans).describedAs("Expecting span name of "+expected).anySatisfy(span -> assertThat(span.getName()).isEqualTo(expected));
    }
}
