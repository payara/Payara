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
 * {@link LlmBackend} backed by the Anthropic (Claude) Messages API over raw
 * {@link HttpClient} + JSON-B (no SDK, to avoid OSGi dependency conflicts).
 * <p>
 * When a system prompt is present it is sent as a single text block carrying a
 * {@code cache_control} breakpoint, so the stable prefix is reused across the
 * phases of a workflow (prompt caching). Claude only caches prefixes at or above
 * its minimum size (~4096 tokens on Opus); shorter prompts simply will not cache
 * &mdash; this is silent, not an error. The per-call system prompt (if any) takes
 * precedence over the configured default.
 * <p>
 * The request is non-streaming, which is appropriate for the modest
 * {@code max_tokens} used by agent phases; very large {@code max_tokens} would
 * need streaming to avoid HTTP timeouts.
 */
public class AnthropicLlmBackend implements LlmBackend {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Jsonb jsonb = JsonbBuilder.create();

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final String defaultSystemPrompt;

    public AnthropicLlmBackend(String baseUrl, String apiKey, String model,
                               int maxTokens, String defaultSystemPrompt) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    @Override
    public String chat(String systemPrompt, List<Turn> conversation) {
        String system = (systemPrompt != null && !systemPrompt.isBlank())
                ? systemPrompt : defaultSystemPrompt;
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                .timeout(Duration.ofSeconds(120))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonb.toJson(buildBody(system, conversation))))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
           if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new LLMException("Anthropic HTTP " + response.statusCode() + ": " + response.body());
            }
            return jsonb.fromJson(response.body(), AnthropicResponse.class).firstText();
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Anthropic backend error", e);
        }
    }

    private Map<String, Object> buildBody(String system, List<Turn> conversation) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        if (system != null && !system.isBlank()) {
            body.put("system", List.of(Map.of(
                    "type", "text",
                    "text", system,
                    "cache_control", Map.of("type", "ephemeral"))));
        }
        List<Map<String, String>> messages = new ArrayList<>();
        for (Turn turn : conversation) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        body.put("messages", messages);
        return body;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Maps the Messages API response: {@code {"content":[{"type":"text","text":...}]}}. */
    public static final class AnthropicResponse {
        public List<Block> content;

        String firstText() {
            if (content != null) {
                for (Block block : content) {
                    if ("text".equals(block.type) && block.text != null) {
                        return block.text;
                    }
                }
            }
            return "";
        }
    }

    public static final class Block {
        public String type;
        public String text;
    }
}
