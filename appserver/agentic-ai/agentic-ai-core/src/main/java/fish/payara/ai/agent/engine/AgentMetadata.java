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
package fish.payara.ai.agent.engine;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Immutable, validated description of an {@code @Agent} produced at deploy time.
 * <p>
 * Captures everything the {@link WorkflowEngine} needs to run a workflow: the
 * trigger method and its CDI event type, the {@code @Decision}/{@code @Action}
 * phases already in execution order, the optional {@code @Outcome} method, and
 * the {@code @HandleException} methods ordered most-specific-first.
 */
public class AgentMetadata {
    private final Class<?> agentClass;
    private final String agentName;
    private final Method triggerMethod;
    private final Class<?> triggerEventType;      // CDI event type that starts the workflow
    private final List<PhaseMethod> sortedPhases; // @Decision + @Action in execution order
    private final Method outcomeMethod;           // null when the agent has no @Outcome
    private final List<Method> exceptionHandlers; // @HandleException, most-specific first

    public AgentMetadata(Class<?> agentClass, String agentName, Method triggerMethod,
                         Class<?> triggerEventType, List<PhaseMethod> sortedPhases,
                         Method outcomeMethod, List<Method> exceptionHandlers) {
        this.agentClass = agentClass;
        this.agentName = agentName;
        this.triggerMethod = triggerMethod;
        this.triggerEventType = triggerEventType;
        this.sortedPhases = List.copyOf(sortedPhases);
        this.outcomeMethod = outcomeMethod;
        this.exceptionHandlers = List.copyOf(exceptionHandlers);
    }

    public Class<?> getAgentClass() {
        return agentClass;
    }

    public String getAgentName() {
        return agentName;
    }

    public Method getTriggerMethod() {
        return triggerMethod;
    }

    public Class<?> getTriggerEventType() {
        return triggerEventType;
    }

    public List<PhaseMethod> getSortedPhases() {
        return sortedPhases;
    }

    public Method getOutcomeMethod() {
        return outcomeMethod;
    }

    public List<Method> getExceptionHandlers() {
        return exceptionHandlers;
    }
}
