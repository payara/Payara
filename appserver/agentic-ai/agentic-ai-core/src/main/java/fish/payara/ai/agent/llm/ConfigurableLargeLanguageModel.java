/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.ai.agent.llm;

import fish.payara.ai.agent.llm.LargeLanguageModelProducer.LLMConfiguration;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.LLMException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Configurable implementation of LargeLanguageModel that supports multiple providers.
 * <p>
 * Currently supports:
 * <ul>
 *     <li>OpenAI API compatible endpoints</li>
 *     <li>Can be extended to support other providers</li>
 * </ul>
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
public class ConfigurableLargeLanguageModel implements LargeLanguageModel {

    private static final Logger logger = Logger.getLogger(ConfigurableLargeLanguageModel.class.getName());

    private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final LLMConfiguration config;
    private final HttpClient httpClient;
    private final Jsonb jsonb;

    public ConfigurableLargeLanguageModel(LLMConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeout()))
                .build();
        this.jsonb = JsonbBuilder.create();
    }

    @Override
    public String query(String prompt) {
        return executeQuery(prompt, null);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType) {
        String response = executeQuery(prompt, null);
        return parseResponse(response, resultType);
    }

    @Override
    public String query(String prompt, Object... inputs) {
        String fullPrompt = buildPromptWithInputs(prompt, inputs);
        return executeQuery(fullPrompt, null);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType, Object... inputs) {
        String fullPrompt = buildPromptWithInputs(prompt, inputs);
        String response = executeQuery(fullPrompt, null);
        return parseResponse(response, resultType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> implClass) {
        if (implClass.isAssignableFrom(getClass())) {
            return (T) this;
        }
        if (implClass.isAssignableFrom(HttpClient.class)) {
            return (T) httpClient;
        }
        if (implClass.isAssignableFrom(LLMConfiguration.class)) {
            return (T) config;
        }
        throw new IllegalArgumentException("Cannot unwrap to " + implClass.getName());
    }

    private String executeQuery(String prompt, String systemPrompt) {
        try {
            String endpoint = config.endpoint() != null ? config.endpoint() : getDefaultEndpoint();
            String requestBody = buildRequestBody(prompt, systemPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .timeout(Duration.ofSeconds(config.timeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.log(Level.WARNING, "LLM API error: {0} - {1}",
                        new Object[]{response.statusCode(), response.body()});
                throw new LLMException("LLM API error: " + response.statusCode());
            }

            return extractResponseContent(response.body());

        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error communicating with LLM API", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LLMException("Failed to communicate with LLM API", e);
        }
    }

    private String getDefaultEndpoint() {
        return switch (config.provider().toLowerCase()) {
            case "openai" -> OPENAI_ENDPOINT;
            case "azure" -> config.endpoint(); // Azure requires custom endpoint
            default -> OPENAI_ENDPOINT; // Default to OpenAI-compatible
        };
    }

    private String buildRequestBody(String prompt, String systemPrompt) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\": \"").append(escapeJson(config.model())).append("\",");
        json.append("\"messages\": [");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            json.append("{\"role\": \"system\", \"content\": \"").append(escapeJson(systemPrompt)).append("\"},");
        }

        json.append("{\"role\": \"user\", \"content\": \"").append(escapeJson(prompt)).append("\"}");
        json.append("]");
        json.append("}");

        return json.toString();
    }

    private String buildPromptWithInputs(String prompt, Object[] inputs) {
        if (inputs == null || inputs.length == 0) {
            return prompt;
        }

        StringBuilder fullPrompt = new StringBuilder(prompt);
        fullPrompt.append("\n\nContext:\n");

        for (Object input : inputs) {
            if (input != null) {
                try {
                    String inputJson = jsonb.toJson(input);
                    fullPrompt.append(inputJson).append("\n");
                } catch (Exception e) {
                    fullPrompt.append(input.toString()).append("\n");
                }
            }
        }

        return fullPrompt.toString();
    }

    private String extractResponseContent(String responseBody) {
        // Simple JSON parsing for OpenAI-compatible response format
        // Response format: {"choices": [{"message": {"content": "..."}}]}
        try {
            int contentStart = responseBody.indexOf("\"content\":");
            if (contentStart == -1) {
                return responseBody;
            }

            contentStart = responseBody.indexOf("\"", contentStart + 10) + 1;
            int contentEnd = findClosingQuote(responseBody, contentStart);

            if (contentEnd == -1) {
                return responseBody;
            }

            return unescapeJson(responseBody.substring(contentStart, contentEnd));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing LLM response", e);
            return responseBody;
        }
    }

    private int findClosingQuote(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '"' && str.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return -1;
    }

    private <T> T parseResponse(String response, Class<T> resultType) {
        if (resultType == String.class) {
            @SuppressWarnings("unchecked")
            T result = (T) response;
            return result;
        }

        if (resultType == Boolean.class || resultType == boolean.class) {
            String lower = response.toLowerCase().trim();
            @SuppressWarnings("unchecked")
            T result = (T) Boolean.valueOf(
                    lower.contains("yes") || lower.contains("true") ||
                            lower.equals("1") || lower.contains("affirmative"));
            return result;
        }

        if (resultType == Integer.class || resultType == int.class) {
            try {
                @SuppressWarnings("unchecked")
                T result = (T) Integer.valueOf(response.trim());
                return result;
            } catch (NumberFormatException e) {
                // Try to extract number from response
                String numbers = response.replaceAll("[^0-9-]", "");
                @SuppressWarnings("unchecked")
                T result = (T) Integer.valueOf(numbers.isEmpty() ? "0" : numbers);
                return result;
            }
        }

        // Try JSON deserialization for complex types
        try {
            return jsonb.fromJson(response, resultType);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not parse response to type " + resultType.getName(), e);
            throw new LLMException("Failed to parse LLM response to " + resultType.getName(), e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
