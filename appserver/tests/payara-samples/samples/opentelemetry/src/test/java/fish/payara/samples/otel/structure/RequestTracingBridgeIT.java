/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file is subject to the terms of either the GNU
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

import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.samples.CliCommands;
import fish.payara.samples.otel.spanname.AbstractSpanNameTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class RequestTracingBridgeIT extends AbstractSpanNameTest {
    private static final Logger LOG = Logger.getLogger(RequestTracingBridgeIT.class.getName());

    private String uri(String path) {
        return URI.create(baseUri.toString().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "")).toString();
    }

    public static class BridgeConfig extends Conf {
        public BridgeConfig() {
            super(Map.of(SPAN_NAMING_KEY, "opentelemetry"));
        }
    }

    @Deployment
    public static WebArchive deployment() {
        return configSource(base(), BridgeConfig.class)
                .addClass(StructureResource.class)
                .addClass(StructureBean.class)
                .addClass(FlattyCounter.class)
                .addClass(AsyncServlet.class)
                .addClass(LegacyTraceCollector.class);
    }

    @Inject
    private LegacyTraceCollector collector;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeClass
    public static void enableRequestTracing() throws Exception {
        CliCommands.payaraGlassFish(
                "set-requesttracing-configuration",
                "--thresholdValue=1",
                "--thresholdUnit=NANOSECONDS",
                "--enabled=true",
                "--target=server-config",
                "--dynamic=true"
        );

        CliCommands.payaraGlassFish(
                "notification-cdieventbus-configure",
                "--loopBack=true",
                "--dynamic=true",
                "--enabled=true",
                "--hazelcastEnabled=true"
        );
    }

    @AfterClass
    public static void disableRequestTracing() {
        CliCommands.payaraGlassFish(
                "set-requesttracing-configuration",
                "--enabled=false",
                "--dynamic=true"
        );

        CliCommands.payaraGlassFish(
                "notification-cdieventbus-configure",
                "--enabled=false",
                "--dynamic=true"
        );
    }

    @Before
    public void clearCollector() {
        collector.clear();
    }

    @After
    public void logTestEnd() {
        LOG.log(Level.INFO, "Test finished");
    }

    @Test
    public void syncRequestYieldsOneCompleteTraceWithRootFirst() throws Exception {
        String url = uri("jaxrs/structure/ejb");
        HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        RequestTrace trace = waitForTrace("/jaxrs/structure/ejb", 10_000);
        assertThat(trace)
                .describedAs("Should have published a trace for /jaxrs/structure/ejb")
                .isNotNull();
        assertThat(trace.getTraceSpans())
                .describedAs("Trace should have at least one span")
                .isNotEmpty();

        var firstSpan = trace.getTraceSpans().getFirst();
        assertThat(firstSpan.getEventName())
                .describedAs("Root span event name should start with GET")
                .startsWith("GET");
        assertThat(firstSpan.getEventType().toString())
                .isEqualTo("TRACE_START");
        assertThat(trace.getElapsedTime())
                .describedAs("Trace should have elapsed time > 0")
                .isGreaterThan(0L);
        assertThat(trace.isCompleted())
                .describedAs("Trace should be completed")
                .isTrue();
    }

    @Test
    public void asyncRequestIsPublishedAndLaterRequestsAreUnaffected() throws Exception {
        // First async request
        String asyncUrl = uri("structure-async");
        HttpRequest asyncRequest = HttpRequest.newBuilder(new URI(asyncUrl)).GET().build();
        HttpResponse<String> asyncResponse = httpClient.send(asyncRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(asyncResponse.statusCode()).isEqualTo(200);

        RequestTrace asyncTrace = waitForTrace("/structure-async", 10_000);
        assertThat(asyncTrace)
                .describedAs("Async request should be published")
                .isNotNull();

        collector.clear();

        // Then 3 sequential sync requests
        for (int i = 0; i < 3; i++) {
            String url = uri("jaxrs/structure/ejb");
            HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);

            RequestTrace syncTrace = waitForTrace("/jaxrs/structure/ejb", 10_000);
            assertThat(syncTrace)
                    .describedAs("Request " + i + " should be published")
                    .isNotNull();

            var rootSpan = syncTrace.getTraceSpans().getFirst();
            assertThat(rootSpan.getEventName()).startsWith("GET");
            assertThat(rootSpan.getEventType().toString()).isEqualTo("TRACE_START");
            assertThat(syncTrace.isCompleted()).isTrue();

            collector.clear();
        }
    }

    private RequestTrace waitForTrace(String urlPathSuffix, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        RequestTrace foundTrace = null;
        while (System.currentTimeMillis() < deadline) {
            List<RequestTrace> traces = collector.traces();
            for (RequestTrace trace : traces) {
                if (!trace.getTraceSpans().isEmpty()) {
                    var rootSpan = trace.getTraceSpans().getFirst();
                    if (rootSpan.getEventName().contains("ArquillianServletRunner")) {
                        continue;
                    }
                    String urlPath = rootSpan.getSpanTags().get("url.path");
                    if (urlPath != null && urlPath.endsWith(urlPathSuffix)) {
                        foundTrace = trace;
                        break;
                    }
                }
            }
            if (foundTrace != null) {
                break;
            }
            Thread.sleep(100);
        }

        if (foundTrace == null) {
            return null;
        }

        // Wait settle time for asynchronous event bus to complete
        Thread.sleep(500);

        // Verify exactly one trace matches the suffix
        List<RequestTrace> allTraces = collector.traces();
        var matchingTraces = allTraces.stream()
            .filter(trace -> !trace.getTraceSpans().isEmpty())
            .filter(trace -> {
                var rootSpan = trace.getTraceSpans().getFirst();
                if (rootSpan.getEventName().contains("ArquillianServletRunner")) {
                    return false;
                }
                String urlPath = rootSpan.getSpanTags().get("url.path");
                return urlPath != null && urlPath.endsWith(urlPathSuffix);
            })
            .toList();

        if (matchingTraces.size() != 1) {
            StringBuilder details = new StringBuilder();
            for (RequestTrace trace : matchingTraces) {
                var rootSpan = trace.getTraceSpans().getFirst();
                details.append("\n  - Root: ").append(rootSpan.getEventName())
                       .append(", TraceID: ").append(trace.getTraceId())
                       .append(", url.path: ").append(rootSpan.getSpanTags().get("url.path"));
            }
            throw new AssertionError("Expected exactly one trace with url.path suffix '" + urlPathSuffix +
                "' but found " + matchingTraces.size() + ":" + details);
        }

        return foundTrace;
    }
}
