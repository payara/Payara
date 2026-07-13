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

import java.util.List;

/**
 * Internal SPI that adapts the Jakarta Agentic AI {@code LargeLanguageModel}
 * facade to a concrete provider (Ollama, Anthropic, ...).
 * <p>
 * This type is <strong>not</strong> part of the specification &mdash; it is a
 * Payara implementation detail, reachable from application code only through
 * {@code LargeLanguageModel.unwrap(LlmBackend.class)}.
 * <p>
 * Conversation turns are role-tagged so the provider receives a correctly
 * alternating user/assistant history. This is required both by multi-turn
 * provider APIs (which reject consecutive same-role messages) and by the
 * specification's requirement that conversational state be maintained per
 * workflow context across {@code query} calls.
 */
public interface LlmBackend {

    /**
     * A single conversation turn.
     *
     * @param role    {@code "user"} or {@code "assistant"}
     * @param content the turn text
     */
    record Turn(String role, String content) {}

    /**
     * Sends the conversation to the provider and returns the assistant reply.
     *
     * @param systemPrompt a stable system instruction the provider may also use
     *                     as a prompt-cache prefix, or {@code null} when none is
     *                     set (the facade has no system-prompt setter yet)
     * @param conversation role-tagged turns, oldest first, ending with the
     *                     current user turn
     * @return the assistant's textual reply (never {@code null})
     */
    String chat(String systemPrompt, List<Turn> conversation);
}
