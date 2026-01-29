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

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mock implementation of LargeLanguageModel for development and testing.
 * <p>
 * This implementation returns predictable responses and logs all queries.
 * It's used when no API key is configured.
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
public class MockLargeLanguageModel implements LargeLanguageModel {

    private static final Logger logger = Logger.getLogger(MockLargeLanguageModel.class.getName());

    @Override
    public String query(String prompt) {
        logger.log(Level.INFO, "[MOCK LLM] Query: {0}", truncate(prompt, 100));
        return generateMockResponse(prompt);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType) {
        logger.log(Level.INFO, "[MOCK LLM] Query with type {0}: {1}",
                new Object[]{resultType.getSimpleName(), truncate(prompt, 100)});

        String response = generateMockResponse(prompt);
        return convertToType(response, resultType);
    }

    @Override
    public String query(String prompt, Object... inputs) {
        logger.log(Level.INFO, "[MOCK LLM] Query with {0} inputs: {1}",
                new Object[]{inputs.length, truncate(prompt, 100)});
        return generateMockResponse(prompt);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType, Object... inputs) {
        logger.log(Level.INFO, "[MOCK LLM] Query with type {0} and {1} inputs: {2}",
                new Object[]{resultType.getSimpleName(), inputs.length, truncate(prompt, 100)});

        String response = generateMockResponse(prompt);
        return convertToType(response, resultType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> implClass) {
        if (implClass.isAssignableFrom(getClass())) {
            return (T) this;
        }
        throw new IllegalArgumentException("MockLargeLanguageModel cannot be unwrapped to " + implClass.getName());
    }

    private String generateMockResponse(String prompt) {
        // Generate contextual mock responses based on prompt content
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("fraud") || lowerPrompt.contains("suspicious")) {
            return "Based on the analysis, this transaction appears to be legitimate. No fraud indicators detected.";
        }

        if (lowerPrompt.contains("yes or no") || lowerPrompt.contains("true or false")) {
            return "yes";
        }

        if (lowerPrompt.contains("analyze") || lowerPrompt.contains("analysis")) {
            return "Analysis complete. The data shows normal patterns with no anomalies detected.";
        }

        if (lowerPrompt.contains("decision") || lowerPrompt.contains("should")) {
            return "Based on the provided context, I recommend proceeding with the proposed action.";
        }

        if (lowerPrompt.contains("summary") || lowerPrompt.contains("summarize")) {
            return "Summary: The workflow has been processed successfully with all steps completed.";
        }

        if (lowerPrompt.contains("json") || lowerPrompt.contains("format")) {
            return "{\"status\": \"success\", \"message\": \"Mock response generated\"}";
        }

        // Default response
        return "Mock LLM Response: Processed query successfully. " +
                "Configure jakarta.ai.llm.api-key for actual LLM integration.";
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(String response, Class<T> resultType) {
        if (resultType == String.class) {
            return (T) response;
        }

        if (resultType == Boolean.class || resultType == boolean.class) {
            String lower = response.toLowerCase();
            return (T) Boolean.valueOf(
                    lower.contains("yes") || lower.contains("true") ||
                            lower.contains("proceed") || lower.contains("success"));
        }

        if (resultType == Integer.class || resultType == int.class) {
            return (T) Integer.valueOf(42); // Mock number
        }

        if (resultType == Long.class || resultType == long.class) {
            return (T) Long.valueOf(42L);
        }

        if (resultType == Double.class || resultType == double.class) {
            return (T) Double.valueOf(0.95);
        }

        // For other types, try to create a basic instance
        try {
            return resultType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot create mock instance of {0}", resultType.getName());
            return null;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
