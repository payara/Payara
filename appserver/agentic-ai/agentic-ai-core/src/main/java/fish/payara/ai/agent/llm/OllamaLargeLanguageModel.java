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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LargeLanguageModel implementation for Ollama (local LLM server).
 * <p>
 * Ollama allows running LLMs locally without external API calls.
 * Supports models like Llama 2, Mistral, CodeLlama, etc.
 * <p>
 * Configuration:
 * <ul>
 *     <li>jakarta.ai.llm.provider=ollama</li>
 *     <li>jakarta.ai.llm.model=llama2 (or mistral, codellama, etc.)</li>
 *     <li>jakarta.ai.llm.endpoint=http://localhost:11434/api/chat (optional, this is default)</li>
 * </ul>
 * <p>
 * Note: Ollama does not require an API key.
 *
 * @author Luis Neto
 */
public class OllamaLargeLanguageModel implements LargeLanguageModel {

    private static final Logger logger = Logger.getLogger(OllamaLargeLanguageModel.class.getName());

    private static final String DEFAULT_MODEL = "llama2";

    private final LLMConfiguration config;
    private final HttpClient httpClient;
    private final Jsonb jsonb;

    public OllamaLargeLanguageModel(LLMConfiguration config) {
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
            String endpoint = config.endpoint() != null ? config.endpoint() : LLMProvider.OLLAMA.getDefaultEndpoint();
            String model = config.model() != null ? config.model() : DEFAULT_MODEL;
            String requestBody = buildRequestBody(prompt, systemPrompt, model);

            logger.log(Level.FINE, "Sending request to Ollama: {0}", endpoint);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(config.timeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.log(Level.WARNING, "Ollama API error: {0} - {1}",
                        new Object[]{response.statusCode(), response.body()});
                throw new LLMException("Ollama API error: " + response.statusCode());
            }

            return extractResponseContent(response.body());

        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error communicating with Ollama. Is Ollama running?", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LLMException("Failed to communicate with Ollama. Ensure Ollama is running on " +
                    (config.endpoint() != null ? config.endpoint() : LLMProvider.OLLAMA.getDefaultEndpoint()), e);
        }
    }

    private String buildRequestBody(String prompt, String systemPrompt, String model) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\": \"").append(escapeJson(model)).append("\",");
        json.append("\"stream\": false,");
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
        // Ollama response format: {"message": {"role": "assistant", "content": "..."}, ...}
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
            logger.log(Level.WARNING, "Error parsing Ollama response", e);
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

    @SuppressWarnings("unchecked")
    private <T> T parseResponse(String response, Class<T> resultType) {
        if (resultType == String.class) {
            return (T) response;
        }

        if (resultType == Boolean.class || resultType == boolean.class) {
            String lower = response.toLowerCase().trim();
            return (T) Boolean.valueOf(
                    lower.contains("yes") || lower.contains("true") ||
                            lower.equals("1") || lower.contains("affirmative"));
        }

        if (resultType == Integer.class || resultType == int.class) {
            try {
                return (T) Integer.valueOf(response.trim());
            } catch (NumberFormatException e) {
                String numbers = response.replaceAll("[^0-9-]", "");
                return (T) Integer.valueOf(numbers.isEmpty() ? "0" : numbers);
            }
        }

        try {
            return jsonb.fromJson(response, resultType);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not parse response to type " + resultType.getName(), e);
            throw new LLMException("Failed to parse LLM response to " + resultType.getName(), e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
