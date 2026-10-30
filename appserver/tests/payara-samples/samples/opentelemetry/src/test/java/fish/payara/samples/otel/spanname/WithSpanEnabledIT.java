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
package fish.payara.samples.otel.spanname;

import fish.payara.samples.otel.annotation.SpanBean;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class WithSpanEnabledIT extends AbstractSpanNameTest {

    private static final Logger LOG = Logger.getLogger(WithSpanEnabledIT.class.getName());

    /**
     * Configuration that disables the disabledSpan() method via the WithSpan/enabled property
     */
    public static class DisabledSpanConfig extends Conf {
        public DisabledSpanConfig() {
            super(Map.of(
                    SPAN_NAMING_KEY, "opentelemetry",
                    "fish.payara.samples.otel.annotation.SpanBean/disabledSpan/WithSpan/enabled", "false"
            ));
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
        return configSource(base(), DisabledSpanConfig.class)
                .addClass(SpanBean.class)
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.assertj:assertj-core").withTransitivity().asFile());
    }

    @Test
    public void testDisabledSpanCreatesNoSpan() {
        LOG.log(Level.INFO, "Testing disabledSpan with enabled=false config");
        spanBean.disabledSpan();

        var spans = exporter.getSpans();
        assertEquals("disabledSpan should not create a span when WithSpan/enabled=false", 0, spans.size());
    }

    @Test
    public void testEnabledSpanCreatesSpan() {
        LOG.log(Level.INFO, "Testing span method (no disable config)");
        spanBean.span();

        var spans = exporter.getSpans();
        assertEquals("span method should create exactly 1 span", 1, spans.size());
        assertEquals("Span name should match method name", "fish.payara.samples.otel.annotation.SpanBean.span", spans.get(0).getName());
    }

    @Test
    public void testDisabledSpanMultipleCalls() {
        LOG.log(Level.INFO, "Testing multiple calls to disabledSpan");
        spanBean.disabledSpan();
        spanBean.disabledSpan();
        spanBean.disabledSpan();

        var spans = exporter.getSpans();
        assertEquals("disabledSpan should not create spans even with multiple calls", 0, spans.size());
    }
}
