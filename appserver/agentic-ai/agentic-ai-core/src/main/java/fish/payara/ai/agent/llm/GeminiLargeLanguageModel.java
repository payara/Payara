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
 * LargeLanguageModel implementation for Google Gemini API.
 * <p>
 * Supports Gemini Pro and Gemini Ultra models.
 * <p>
 * Configuration:
 * <ul>
 *     <li>jakarta.ai.llm.provider=gemini</li>
 *     <li>jakarta.ai.llm.api-key=your-google-api-key</li>
 *     <li>jakarta.ai.llm.model=gemini-pro (or gemini-1.5-pro, gemini-ultra)</li>
 * </ul>
 *
 * @author Luis Neto
 */
public class GeminiLargeLanguageModel implements LargeLanguageModel {

    private static final Logger logger = Logger.getLogger(GeminiLargeLanguageModel.class.getName());

    private static final String DEFAULT_MODEL = "gemini-pro";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final LLMConfiguration config;
    private final HttpClient httpClient;
    private final Jsonb jsonb;

    public GeminiLargeLanguageModel(LLMConfiguration config) {
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
            String model = config.model() != null ? config.model() : DEFAULT_MODEL;
            String endpoint = buildEndpoint(model);
            String requestBody = buildRequestBody(prompt, systemPrompt);

            logger.log(Level.FINE, "Sending request to Gemini API: {0}", endpoint);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(config.timeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.log(Level.WARNING, "Gemini API error: {0} - {1}",
                        new Object[]{response.statusCode(), response.body()});
                throw new LLMException("Gemini API error: " + response.statusCode());
            }

            return extractResponseContent(response.body());

        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error communicating with Gemini API", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LLMException("Failed to communicate with Gemini API", e);
        }
    }

    private String buildEndpoint(String model) {
        if (config.endpoint() != null) {
            return config.endpoint();
        }
        // Gemini API endpoint format: /v1beta/models/{model}:generateContent?key={api_key}
        return String.format("%s/%s:generateContent?key=%s", API_BASE_URL, model, config.apiKey());
    }

    private String buildRequestBody(String prompt, String systemPrompt) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"contents\": [");

        // System instruction (if provided)
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            json.append("{\"role\": \"user\", \"parts\": [{\"text\": \"System instruction: ")
                .append(escapeJson(systemPrompt)).append("\"}]},");
            json.append("{\"role\": \"model\", \"parts\": [{\"text\": \"Understood. I will follow these instructions.\"}]},");
        }

        // User prompt
        json.append("{\"role\": \"user\", \"parts\": [{\"text\": \"")
            .append(escapeJson(prompt)).append("\"}]}");

        json.append("],");
        json.append("\"generationConfig\": {");
        json.append("\"temperature\": 0.7,");
        json.append("\"maxOutputTokens\": 4096");
        json.append("}");
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
        // Gemini response format:
        // {"candidates": [{"content": {"parts": [{"text": "..."}], "role": "model"}, ...}], ...}
        try {
            int textStart = responseBody.indexOf("\"text\":");
            if (textStart == -1) {
                return responseBody;
            }

            textStart = responseBody.indexOf("\"", textStart + 7) + 1;
            int textEnd = findClosingQuote(responseBody, textStart);

            if (textEnd == -1) {
                return responseBody;
            }

            return unescapeJson(responseBody.substring(textStart, textEnd));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing Gemini response", e);
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
