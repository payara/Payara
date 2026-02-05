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

/**
 * Supported LLM providers.
 *
 * @author Luis Neto
 */
public enum LLMProvider {

    /**
     * OpenAI API (ChatGPT, GPT-4, etc.)
     * Default endpoint: https://api.openai.com/v1/chat/completions
     */
    OPENAI("openai", "https://api.openai.com/v1/chat/completions"),

    /**
     * Anthropic API (Claude models)
     * Default endpoint: https://api.anthropic.com/v1/messages
     */
    ANTHROPIC("anthropic", "https://api.anthropic.com/v1/messages"),

    /**
     * Azure OpenAI Service
     * Endpoint must be configured per deployment
     */
    AZURE("azure", null),

    /**
     * Ollama - Local LLM server
     * Default endpoint: http://localhost:11434/api/chat
     */
    OLLAMA("ollama", "http://localhost:11434/api/chat"),

    /**
     * Google Gemini API
     * Default endpoint: https://generativelanguage.googleapis.com/v1beta/models
     */
    GEMINI("gemini", "https://generativelanguage.googleapis.com/v1beta/models"),

    /**
     * Mistral AI API
     * Default endpoint: https://api.mistral.ai/v1/chat/completions
     */
    MISTRAL("mistral", "https://api.mistral.ai/v1/chat/completions"),

    /**
     * Generic OpenAI-compatible API
     * Endpoint must be configured
     */
    OPENAI_COMPATIBLE("openai-compatible", null);

    private final String configName;
    private final String defaultEndpoint;

    LLMProvider(String configName, String defaultEndpoint) {
        this.configName = configName;
        this.defaultEndpoint = defaultEndpoint;
    }

    public String getConfigName() {
        return configName;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    /**
     * Finds a provider by its configuration name (case-insensitive).
     *
     * @param name the configuration name
     * @return the provider, or OPENAI_COMPATIBLE if not found
     */
    public static LLMProvider fromConfigName(String name) {
        if (name == null || name.isEmpty()) {
            return OPENAI;
        }
        String lowerName = name.toLowerCase().trim();
        for (LLMProvider provider : values()) {
            if (provider.configName.equals(lowerName)) {
                return provider;
            }
        }
        // Fallback to OpenAI-compatible for unknown providers
        return OPENAI_COMPATIBLE;
    }
}
