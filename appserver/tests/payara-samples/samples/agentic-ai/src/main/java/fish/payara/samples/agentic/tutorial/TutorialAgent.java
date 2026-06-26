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
package fish.payara.samples.agentic.tutorial;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Generates and refines an HTML tutorial that explains a web form, field by
 * field, using the configured LLM. Exercises the four specification phases and
 * supports a chat refinement loop: when the request carries the current HTML,
 * the {@code @Action} revises it instead of regenerating from scratch.
 */
@Agent(name = "TutorialAgent", description = "Generates and refines an HTML tutorial explaining a web form.")
public class TutorialAgent {

    private static final Logger LOGGER = Logger.getLogger(TutorialAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    TutorialStore store;

    @Trigger
    void onRequest(TutorialRequest request) {
        boolean refine = request.currentHtml() != null && !request.currentHtml().isBlank();
        LOGGER.info("[TRIGGER] tutorial request (" + (refine ? "refine" : "generate") + ")");
    }

    @Decision
    Result hasFields(TutorialRequest request) {
        boolean proceed = request.formSpec() != null && !request.formSpec().fields().isEmpty();
        LOGGER.info("[DECISION] proceed=" + proceed);
        return new Result(proceed, request);
    }

    @Action
    void render(TutorialRequest request) {
        String html;
        if (request.currentHtml() == null || request.currentHtml().isBlank()) {
            LOGGER.info("[ACTION] generating tutorial from the form spec...");
            html = model.query(
                    "Generate the tutorial HTML fragment for this form: {}", request.formSpec());
        } else {
            LOGGER.info("[ACTION] refining tutorial: " + request.instruction());
            html = model.query(
                    "Current tutorial HTML fragment:\n{}\n\n"
                            + "Apply this change requested by the developer: {}\n\n"
                            + "Return the full updated HTML fragment.",
                    request.currentHtml(), request.instruction());
        }
        store.put(stripCodeFences(html));
    }

    @Outcome
    void complete(TutorialRequest request) {
        LOGGER.info("[OUTCOME] tutorial ready (" + store.get().length() + " chars)");
    }

    /** LLMs sometimes wrap output in ```html ... ``` despite instructions; strip it. */
    private static String stripCodeFences(String html) {
        if (html == null) {
            return "";
        }
        String trimmed = html.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.strip();
    }
}
