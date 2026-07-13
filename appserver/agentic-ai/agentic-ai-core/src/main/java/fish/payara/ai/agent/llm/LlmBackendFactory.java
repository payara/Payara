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

import org.eclipse.microprofile.config.Config;

/**
 * Selects and configures the {@link LlmBackend} from MicroProfile Config.
 * <p>
 * Provider selection and configuration are implementation-specific in Jakarta
 * Agentic AI 1.0; this factory is the Payara mechanism. All keys live under the
 * {@code payara.agentic.llm.} prefix:
 * <ul>
 *   <li>{@code provider} &mdash; {@code none} (default &rarr; {@link NoOpLlmBackend}),
 *       {@code ollama}, {@code anthropic}</li>
 *   <li>{@code model} &mdash; provider model name (Ollama default {@code gemma},
 *       Anthropic default {@code claude-opus-4-8})</li>
 *   <li>{@code ollama.base-url} &mdash; default {@code http://localhost:11434}</li>
 *   <li>{@code anthropic.base-url} &mdash; default {@code https://api.anthropic.com}</li>
 *   <li>{@code anthropic.api-key} &mdash; or the {@code ANTHROPIC_API_KEY} env var</li>
 *   <li>{@code max-tokens} &mdash; Anthropic response cap (default {@code 4096})</li>
 *   <li>{@code system} &mdash; optional system prompt used as the Anthropic cache prefix</li>
 * </ul>
 * Unknown providers fall back to the no-op backend so the runtime always
 * resolves a {@code LargeLanguageModel} without ambiguity.
 */
public final class LlmBackendFactory {

    static final String PREFIX = "payara.agentic.llm.";

    static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    static final String DEFAULT_OLLAMA_MODEL = "gemma";
    static final String DEFAULT_ANTHROPIC_BASE_URL = "https://api.anthropic.com";
    static final String DEFAULT_ANTHROPIC_MODEL = "claude-opus-4-8";
    static final int DEFAULT_ANTHROPIC_MAX_TOKENS = 4096;

    private LlmBackendFactory() {
    }

    public static LlmBackend create(Config config) {
        String provider = config.getOptionalValue(PREFIX + "provider", String.class)
                .orElse("none").trim().toLowerCase();
        return switch (provider) {
            case "ollama" -> new OllamaLlmBackend(
                    config.getOptionalValue(PREFIX + "ollama.base-url", String.class)
                            .orElse(DEFAULT_OLLAMA_BASE_URL),
                    config.getOptionalValue(PREFIX + "model", String.class)
                            .orElse(DEFAULT_OLLAMA_MODEL));
            case "anthropic", "claude" -> new AnthropicLlmBackend(
                    config.getOptionalValue(PREFIX + "anthropic.base-url", String.class)
                            .orElse(DEFAULT_ANTHROPIC_BASE_URL),
                    resolveAnthropicKey(config),
                    config.getOptionalValue(PREFIX + "model", String.class)
                            .orElse(DEFAULT_ANTHROPIC_MODEL),
                    config.getOptionalValue(PREFIX + "max-tokens", Integer.class)
                            .orElse(DEFAULT_ANTHROPIC_MAX_TOKENS),
                    config.getOptionalValue(PREFIX + "system", String.class)
                            .orElse(null));
            default -> new NoOpLlmBackend();
        };
    }

    private static String resolveAnthropicKey(Config config) {
        return config.getOptionalValue(PREFIX + "anthropic.api-key", String.class)
                .or(() -> config.getOptionalValue("ANTHROPIC_API_KEY", String.class))
                .orElseThrow(() -> new IllegalStateException(
                        "Anthropic provider selected but no API key found; set "
                                + PREFIX + "anthropic.api-key or the ANTHROPIC_API_KEY environment variable"));
    }
}
