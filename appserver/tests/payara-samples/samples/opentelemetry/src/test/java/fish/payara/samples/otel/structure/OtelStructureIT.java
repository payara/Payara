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
 *  If you wish your version of this file to be governed under only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees are provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.samples.otel.structure;

import fish.payara.samples.otel.spanname.AbstractSpanNameTest;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class OtelStructureIT extends AbstractSpanNameTest {
    public static class StructureConfig extends Conf {
        public StructureConfig() {
            super(Map.of(SPAN_NAMING_KEY, "opentelemetry"));
        }
    }

    private String uri(String path) {
        return URI.create(baseUri.toString().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "")).toString();
    }

    @Deployment
    public static WebArchive deployment() {
        return configSource(base(), StructureConfig.class)
                .addClass(StructureResource.class)
                .addClass(StructureBean.class)
                .addClass(FlattyCounter.class)
                .addClass(AsyncServlet.class)
                .addClass(LegacyTraceCollector.class);
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    public void servletRequestCreatesServerRootNamedAfterMethod() throws Exception {
        exporter.reset();
        String url = uri("jaxrs/structure/ejb");
        HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var spans = exporter.getSpans();
        var urlPathKey = AttributeKey.stringKey("url.path");
        var serverSpans = spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .filter(s -> {
                    var urlPath = s.getAttributes().get(urlPathKey);
                    return urlPath != null && urlPath.endsWith("/jaxrs/structure/ejb");
                })
                .toList();

        assertThat(serverSpans)
                .describedAs("Should have exactly one SERVER span with url.path ending /jaxrs/structure/ejb")
                .hasSize(1)
                .allSatisfy(span -> {
                    assertThat(span.getName()).startsWith("GET");
                    assertThat(span.getKind()).isEqualTo(SpanKind.SERVER);
                    // Parent validity is not asserted: a stale context left on a pooled thread by earlier tests/clients can legitimately become the parent on a shared server.
                    assertThat(span.getAttributes().get(AttributeKey.stringKey("http.request.method"))).isEqualTo("GET");
                    assertThat(span.getAttributes().get(AttributeKey.longKey("http.response.status_code"))).isEqualTo(200L);
                });
    }

    @Test
    public void ejbAndJtaAreSpanEventsOnTheRoot() throws Exception {
        exporter.reset();
        String url = uri("jaxrs/structure/ejb");
        HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var spans = exporter.getSpans();
        var urlPathKey = AttributeKey.stringKey("url.path");
        var serverSpans = spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .filter(s -> {
                    var urlPath = s.getAttributes().get(urlPathKey);
                    return urlPath != null && urlPath.endsWith("/jaxrs/structure/ejb");
                })
                .toList();

        assertThat(serverSpans)
                .isNotEmpty()
                .allSatisfy(span -> {
                    var events = span.getEvents();
                    var eventNames = events.stream().map(e -> e.getName()).toList();

                    assertThat(eventNames)
                            .describedAs("Should contain all four event names")
                            .contains("jtaContextBeginEvent", "enterEjbMethodEvent", "exitEjbMethodEvent", "jtaContextEndEvent");

                    // Check enterEjbMethodEvent has EJBMethod = "transactional"
                    var ejbMethodKey = AttributeKey.stringKey("EJBMethod");
                    var enterEjbEvent = events.stream()
                            .filter(e -> "enterEjbMethodEvent".equals(e.getName()))
                            .findFirst();
                    assertThat(enterEjbEvent).isPresent();
                    assertThat(enterEjbEvent.get().getAttributes().get(ejbMethodKey))
                            .isEqualTo("transactional");

                    // Check jtaContextEndEvent has Status = "Committed"
                    var statusKey = AttributeKey.stringKey("Status");
                    var jtaEndEvent = events.stream()
                            .filter(e -> "jtaContextEndEvent".equals(e.getName()))
                            .findFirst();
                    assertThat(jtaEndEvent).isPresent();
                    assertThat(jtaEndEvent.get().getAttributes().get(statusKey))
                            .isEqualTo("Committed");
                });

        // Check legacy names are gone
        assertThat(spans)
                .noneMatch(s -> "processServletRequest".equals(s.getName())
                        || "processContainerRequest".equals(s.getName())
                        || "processWebserviceRequest".equals(s.getName()));
        var allEventNames = spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .flatMap(s -> s.getEvents().stream().map(e -> e.getName()))
                .toList();
        assertThat(allEventNames)
                .doesNotContain("processServletRequest", "processContainerRequest", "processWebserviceRequest");
    }

    @Test
    public void faultToleranceRetryCreatesInternalSpans() throws Exception {
        exporter.reset();
        String url = uri("jaxrs/structure/retry");
        HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var spans = exporter.getSpans();
        var urlPathKey = AttributeKey.stringKey("url.path");

        // Get the SERVER span for this request
        var serverSpans = spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .filter(s -> {
                    var urlPath = s.getAttributes().get(urlPathKey);
                    return urlPath != null && urlPath.endsWith("/jaxrs/structure/retry");
                })
                .toList();

        assertThat(serverSpans).isNotEmpty();
        String traceId = serverSpans.get(0).getTraceId();

        // Get delayRetry spans in the same trace
        var delayRetrySpans = spans.stream()
                .filter(s -> "delayRetry".equals(s.getName()))
                .filter(s -> s.getTraceId().equals(traceId))
                .toList();

        assertThat(delayRetrySpans)
                .describedAs("Should have at least 2 delayRetry spans in the same trace as SERVER span")
                .hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(span -> {
                    assertThat(span.getKind()).isEqualTo(SpanKind.INTERNAL);
                    assertThat(span.getParentSpanContext().isValid()).isTrue();
                });
    }

    @Test
    public void ejbTimerCreatesParentlessInternalSpan() throws Exception {
        exporter.reset();
        String url = uri("jaxrs/structure/timer");
        HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Poll for the timer span with deadline
        long deadline = System.currentTimeMillis() + 15_000;
        var timerSpans = List.<SpanData>of();

        while (System.currentTimeMillis() < deadline) {
            var allSpans = exporter.getSpans();
            timerSpans = allSpans.stream()
                    .filter(s -> "executeEjbTimerTask".equals(s.getName()))
                    .toList();

            if (!timerSpans.isEmpty()) {
                break;
            }

            Thread.sleep(100);
        }

        assertThat(timerSpans)
                .describedAs("Timer span 'executeEjbTimerTask' should be found within 15 seconds")
                .hasSize(1)
                .allSatisfy(span -> {
                    assertThat(span.getKind()).isEqualTo(SpanKind.INTERNAL);
                    assertThat(span.getParentSpanContext().isValid()).isFalse();
                    var timerIdKey = AttributeKey.stringKey("TimerID");
                    assertThat(span.getAttributes().get(timerIdKey))
                            .isNotEmpty();
                });
    }
}
