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
package fish.payara.samples.agentic.quickstart;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import java.util.logging.Logger;

/**
 * Minimal Jakarta Agentic AI agent: answers a question with the configured LLM
 * backend. Exercises all four specification phases &mdash; {@code @Trigger},
 * {@code @Decision}, {@code @Action}, {@code @Outcome} &mdash; and logs each so
 * the workflow is visible in {@code server.log}.
 * <p>
 * Default scope is {@code @WorkflowScoped} (applied by the runtime extension).
 */
@Agent(name = "QuestionAgent", description = "Answers a question using the configured LLM backend.")
public class QuestionAgent {

    private static final Logger LOGGER = Logger.getLogger(QuestionAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    AnswerStore answers;

    @Trigger
    void onQuestion(@Valid Question question) {
        LOGGER.info("[TRIGGER] question received: " + question.text());
    }

    @Decision
    Result hasContent(Question question) {
        boolean proceed = question.text() != null && !question.text().isBlank();
        LOGGER.info("[DECISION] proceed=" + proceed);
        return new Result(proceed, question);
    }

    @Action
    void generate(Question question) {
        LOGGER.info("[ACTION] querying LLM...");
        String answer = model.query("Answer concisely in one short paragraph: {}", question.text());
        answers.put(question.text(), answer);
        LOGGER.info("[ACTION] answer: " + answer);
    }

    @Outcome
    void complete(Question question) {
        LOGGER.info("[OUTCOME] workflow complete for: " + question.text());
    }
}
