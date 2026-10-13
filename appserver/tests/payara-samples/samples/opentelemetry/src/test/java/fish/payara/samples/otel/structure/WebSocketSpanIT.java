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
 *  enclosed by brackets [] replaced with your own identifying information:
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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(Arquillian.class)
public class WebSocketSpanIT extends AbstractSpanNameTest {
    public static class WebSocketConfig extends Conf {
        public WebSocketConfig() {
            super(Map.of(SPAN_NAMING_KEY, "opentelemetry"));
        }
    }

    private String wsUri(String path) {
        String httpUri = baseUri.toString().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
        return "ws" + httpUri.substring(4); // http -> ws
    }

    @Deployment
    public static WebArchive deployment() {
        return configSource(base(), WebSocketConfig.class)
                .addClass(EchoEndpoint.class);
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    public void websocketMessageCreatesInternalSpan() throws Exception {
        exporter.reset();

        String wsUrl = wsUri("structure-ws");
        CompletableFuture<String> receivedMessage = new CompletableFuture<>();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                receivedMessage.complete(data.toString());
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                receivedMessage.completeExceptionally(error);
            }
        };

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), listener)
                .join();

        webSocket.sendText("ping", true);
        String response = receivedMessage.get();
        assertThat(response).isEqualTo("ping");

        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();

        // Poll for processWebsocketRequest span
        long deadline = System.currentTimeMillis() + 15_000;
        List<SpanData> accumulatedSpans = new ArrayList<>();
        SpanData wsSpan = null;

        while (System.currentTimeMillis() < deadline) {
            List<SpanData> spans = exporter.getSpans();
            accumulatedSpans.addAll(spans);
            wsSpan = accumulatedSpans.stream()
                    .filter(s -> "processWebsocketRequest".equals(s.getName()))
                    .findFirst()
                    .orElse(null);

            if (wsSpan != null) {
                break;
            }

            Thread.sleep(100);
        }

        assertThat(wsSpan)
                .describedAs("Should find span named 'processWebsocketRequest'")
                .isNotNull();
        assertThat(wsSpan.getKind()).isEqualTo(SpanKind.INTERNAL);
        assertThat(wsSpan.getParentSpanContext().isValid())
                .describedAs("processWebsocketRequest span should have invalid parent (root)")
                .isFalse();

        String wsTraceId = wsSpan.getTraceId();
        List<String> serverTraceIds = accumulatedSpans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .map(SpanData::getTraceId)
                .toList();

        assertThat(wsTraceId)
                .describedAs("WebSocket span traceId should differ from any SERVER span traceId")
                .isNotIn(serverTraceIds.toArray(new String[0]));
    }
}
