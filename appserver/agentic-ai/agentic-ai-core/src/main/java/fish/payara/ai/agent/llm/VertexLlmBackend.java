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

import java.io.IOException;
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
 * {@link LlmBackend} backed by Claude on Google Cloud Vertex AI.
 * <p>
 * The request/response body format is identical to the Anthropic Messages API;
 * the differences are the endpoint URL and {@code Authorization: Bearer} auth
 * instead of {@code x-api-key}.
 * <p>
 * Token resolution order:
 * <ol>
 *   <li>{@code GOOGLE_ACCESS_TOKEN} environment variable — explicit override,
 *       useful when {@code gcloud} is not on PATH.</li>
 *   <li>{@code gcloud auth application-default print-access-token} subprocess —
 *       honours Application Default Credentials (ADC) as configured by
 *       {@code gcloud auth application-default login} or workload identity.</li>
 * </ol>
 */
public class VertexLlmBackend implements LlmBackend {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Jsonb jsonb = JsonbBuilder.create();

    private final String projectId;
    private final String region;
    private final String model;
    private final int maxTokens;
    private final String defaultSystemPrompt;

    public VertexLlmBackend(String projectId, String region, String model,
                            int maxTokens, String defaultSystemPrompt) {
        this.projectId = projectId;
        this.region = region;
        this.model = model;
        this.maxTokens = maxTokens;
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    @Override
    public String chat(String systemPrompt, List<Turn> conversation) {
        String system = (systemPrompt != null && !systemPrompt.isBlank())
                ? systemPrompt : defaultSystemPrompt;
        String token = resolveAccessToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(buildEndpoint()))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + token)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonb.toJson(buildBody(system, conversation))))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new LLMException("Vertex AI HTTP " + response.statusCode() + ": " + response.body());
            }
            return jsonb.fromJson(response.body(), AnthropicLlmBackend.AnthropicResponse.class).firstText();
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Vertex AI backend error", e);
        }
    }

    private String buildEndpoint() {
        // The "global" pseudo-region uses the root host without a region prefix.
        String host = "global".equalsIgnoreCase(region)
                ? "https://aiplatform.googleapis.com"
                : "https://" + region + "-aiplatform.googleapis.com";
        return host + "/v1/projects/" + projectId + "/locations/" + region
                + "/publishers/anthropic/models/" + model + ":rawPredict";
    }

    private String resolveAccessToken() {
        String envToken = System.getenv("GOOGLE_ACCESS_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.trim();
        }
        try {
            ProcessBuilder pb = IS_WINDOWS
                    ? new ProcessBuilder("cmd", "/c", "gcloud", "auth",
                            "application-default", "print-access-token")
                    : new ProcessBuilder("gcloud", "auth",
                            "application-default", "print-access-token");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exit = process.waitFor();
            if (exit == 0 && !output.isBlank()) {
                return output;
            }
            throw new LLMException("gcloud exited " + exit + ": " + output);
        } catch (LLMException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new LLMException(
                    "Cannot obtain Google access token: gcloud not found on PATH. "
                    + "Set the GOOGLE_ACCESS_TOKEN environment variable to a valid "
                    + "Bearer token, or ensure gcloud is available on PATH.", e);
        }
    }

    private Map<String, Object> buildBody(String system, List<Turn> conversation) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("anthropic_version", "vertex-2023-10-16");
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
}
