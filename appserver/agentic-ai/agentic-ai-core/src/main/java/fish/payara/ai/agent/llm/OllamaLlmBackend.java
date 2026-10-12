/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.ai.agent.llm;

import jakarta.ai.agent.LLMException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link LlmBackend} backed by a local <a href="https://ollama.com">Ollama</a>
 * server (e.g. {@code gemma}, {@code llama3}) through its {@code /api/chat}
 * endpoint. No API key is required.
 * <p>
 * Ollama has no prefix prompt-cache concept, so the {@code systemPrompt} is
 * simply prepended as a {@code system} message when present.
 */
public class OllamaLlmBackend implements LlmBackend {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Jsonb jsonb = JsonbBuilder.create();

    private final String baseUrl;
    private final String model;

    public OllamaLlmBackend(String baseUrl, String model) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.model = model;
    }

    @Override
    public String chat(String systemPrompt, List<Turn> conversation) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(120))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        jsonb.toJson(buildBody(systemPrompt, conversation))))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new LLMException("Ollama HTTP " + response.statusCode() + ": " + response.body());
            }
            return jsonb.fromJson(response.body(), OllamaResponse.class).text();
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Ollama backend error", e);
        }
    }

    private Map<String, Object> buildBody(String systemPrompt, List<Turn> conversation) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (Turn turn : conversation) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        return body;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Maps the {@code /api/chat} non-streaming response: {@code {"message":{"content":...}}}. */
    public static final class OllamaResponse {
        public Message message;

        String text() {
            return message == null || message.content == null ? "" : message.content;
        }
    }

    public static final class Message {
        public String role;
        public String content;
    }
}
