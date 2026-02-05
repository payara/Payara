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

import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI producer for LargeLanguageModel instances.
 * <p>
 * Supports multiple LLM providers configured via MicroProfile Config:
 * <ul>
 *     <li>{@code jakarta.ai.llm.provider} - The LLM provider: openai, anthropic, ollama, azure, gemini, mistral (default: "openai")</li>
 *     <li>{@code jakarta.ai.llm.api-key} - The API key for the LLM service (not required for Ollama)</li>
 *     <li>{@code jakarta.ai.llm.model} - The model to use (e.g., "gpt-4", "claude-3-sonnet", "llama2")</li>
 *     <li>{@code jakarta.ai.llm.endpoint} - Custom API endpoint URL (optional, uses provider defaults)</li>
 *     <li>{@code jakarta.ai.llm.timeout} - Request timeout in seconds (default: 30)</li>
 * </ul>
 * <p>
 * Supported providers and their default models:
 * <table>
 *     <tr><th>Provider</th><th>Default Model</th><th>Requires API Key</th></tr>
 *     <tr><td>openai</td><td>gpt-4</td><td>Yes</td></tr>
 *     <tr><td>anthropic</td><td>claude-3-sonnet-20240229</td><td>Yes</td></tr>
 *     <tr><td>ollama</td><td>llama2</td><td>No (local)</td></tr>
 *     <tr><td>azure</td><td>gpt-4</td><td>Yes (+ endpoint required)</td></tr>
 *     <tr><td>gemini</td><td>gemini-pro</td><td>Yes</td></tr>
 *     <tr><td>mistral</td><td>mistral-medium</td><td>Yes</td></tr>
 * </table>
 *
 * @author Luis Neto
 */
@ApplicationScoped
public class LargeLanguageModelProducer {

    private static final Logger logger = Logger.getLogger(LargeLanguageModelProducer.class.getName());

    @Inject
    @ConfigProperty(name = "jakarta.ai.llm.provider", defaultValue = "openai")
    private String provider;

    @Inject
    @ConfigProperty(name = "jakarta.ai.llm.api-key")
    private Optional<String> apiKey;

    @Inject
    @ConfigProperty(name = "jakarta.ai.llm.model")
    private Optional<String> model;

    @Inject
    @ConfigProperty(name = "jakarta.ai.llm.endpoint")
    private Optional<String> endpoint;

    @Inject
    @ConfigProperty(name = "jakarta.ai.llm.timeout", defaultValue = "30")
    private int timeout;

    /**
     * Produces a LargeLanguageModel instance based on configuration.
     *
     * @param injectionPoint the CDI injection point
     * @return a configured LargeLanguageModel instance
     */
    @Produces
    @Default
    @ApplicationScoped
    public LargeLanguageModel produceLargeLanguageModel(InjectionPoint injectionPoint) {
        LLMProvider llmProvider = LLMProvider.fromConfigName(provider);

        logger.log(Level.INFO, "Producing LargeLanguageModel: provider={0}, model={1}",
                new Object[]{llmProvider.getConfigName(), model.orElse("(default)")});

        LLMConfiguration config = new LLMConfiguration(
                provider,
                apiKey.orElse(null),
                model.orElse(null),
                endpoint.orElse(null),
                timeout
        );

        return createLargeLanguageModel(llmProvider, config);
    }

    /**
     * Creates a LargeLanguageModel based on the provider and configuration.
     *
     * @param llmProvider the LLM provider
     * @param config      the LLM configuration
     * @return the configured LargeLanguageModel
     */
    private LargeLanguageModel createLargeLanguageModel(LLMProvider llmProvider, LLMConfiguration config) {
        // Ollama doesn't require an API key (local)
        if (llmProvider == LLMProvider.OLLAMA) {
            logger.log(Level.INFO, "Using Ollama for local LLM inference");
            return new OllamaLargeLanguageModel(config);
        }

        // All other providers require an API key
        if (config.apiKey() == null || config.apiKey().isEmpty()) {
            logger.log(Level.WARNING,
                    "No API key configured for LLM provider ''{0}''. Set jakarta.ai.llm.api-key property. " +
                            "Using mock implementation for development.",
                    llmProvider.getConfigName());
            return new MockLargeLanguageModel();
        }

        // Select implementation based on provider
        return switch (llmProvider) {
            case ANTHROPIC -> {
                logger.log(Level.INFO, "Using Anthropic Claude API");
                yield new AnthropicLargeLanguageModel(config);
            }
            case OPENAI, AZURE, OPENAI_COMPATIBLE -> {
                logger.log(Level.INFO, "Using OpenAI-compatible API: {0}", llmProvider.getConfigName());
                yield new ConfigurableLargeLanguageModel(config);
            }
            case GEMINI -> {
                logger.log(Level.INFO, "Using Google Gemini API");
                yield new GeminiLargeLanguageModel(config);
            }
            case MISTRAL -> {
                logger.log(Level.INFO, "Using Mistral AI API");
                // Mistral uses OpenAI-compatible format
                yield new ConfigurableLargeLanguageModel(config);
            }
            default -> {
                logger.log(Level.INFO, "Using default OpenAI-compatible implementation");
                yield new ConfigurableLargeLanguageModel(config);
            }
        };
    }

    /**
     * Configuration holder for LLM settings.
     */
    public record LLMConfiguration(
            String provider,
            String apiKey,
            String model,
            String endpoint,
            int timeout
    ) {}
}
