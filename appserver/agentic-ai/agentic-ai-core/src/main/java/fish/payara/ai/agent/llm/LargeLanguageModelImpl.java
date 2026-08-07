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
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LargeLanguageModelImpl implements LargeLanguageModel {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\}");
    private final Jsonb jsonb = JsonbBuilder.create();
    private final LlmBackend backend;
    private final List<LlmBackend.Turn> conversation = new ArrayList<>();

    public LargeLanguageModelImpl(LlmBackend backend) {
        this.backend = backend;
    }

    @Override
    public String query(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        return callBackend(prompt);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        if (resultType == null) {
            throw new IllegalArgumentException("resultType must not be null");
        }
        return deserialize(callBackend(prompt), resultType);
    }

    @Override
    public String query(String prompt, Object... parameters) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        return callBackend(buildPrompt(prompt, parameters));
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType, Object... parameters) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        if (resultType == null) {
            throw new IllegalArgumentException("resultType must not be null");
        }
        return deserialize(callBackend(buildPrompt(prompt, parameters)), resultType);
    }

    @Override
    public <T> T unwrap(Class<T> implClass) {
        if (implClass == null) {
            throw new IllegalArgumentException("implClass must not be null");
        }
        if (implClass.isInstance(backend)) {
            return implClass.cast(backend);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + implClass.getName());
    }

    private String buildPrompt(String prompt, Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return prompt;
        }
        long placeholderCount = PLACEHOLDER.matcher(prompt).results().count();
        if (placeholderCount == 0) {
            if (parameters.length > 1) {
                throw new IllegalArgumentException(
                        "Prompt has no {} placeholders but " + parameters.length + " parameters were supplied");
            }
            return prompt + "\n" + serialize(parameters[0]);
        }
        if (placeholderCount != parameters.length) {
            throw new IllegalArgumentException(
                    "Prompt has " + placeholderCount + " {} placeholders but "
                            + parameters.length + " parameters were supplied");
        }
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PLACEHOLDER.matcher(prompt);
        int idx = 0;
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(serialize(parameters[idx++])));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String serialize(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return jsonb.toJson(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize parameter to JSON: " + obj, e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return jsonb.fromJson(json, type);
        } catch (Exception e) {
            throw new LLMException("Failed to deserialize LLM response to " + type.getName(), e);
        }
    }

    private String callBackend(String prompt) {
        LlmBackend.Turn userTurn = new LlmBackend.Turn("user", prompt);
        conversation.add(userTurn);
        try {
            String response = backend.chat(null, List.copyOf(conversation));
            conversation.add(new LlmBackend.Turn("assistant", response == null ? "" : response));
            return response;
        } catch (LLMException e) {
            conversation.remove(userTurn);
            throw e;
        } catch (Exception e) {
            conversation.remove(userTurn);
            throw new LLMException("LLM backend error", e);
        }
    }
}
