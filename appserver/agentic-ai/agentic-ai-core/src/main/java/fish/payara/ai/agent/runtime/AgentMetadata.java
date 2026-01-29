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
package fish.payara.ai.agent.runtime;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Metadata holder for an @Agent annotated class.
 * Extracts and caches information about workflow methods annotated with
 * @Trigger, @Decision, @Action, @Outcome, and @HandleException.
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
public class AgentMetadata {

    private final Class<?> agentClass;
    private final String agentName;
    private final String agentDescription;
    private final List<Method> triggerMethods;
    private final List<Method> decisionMethods;
    private final List<Method> actionMethods;
    private final List<Method> outcomeMethods;
    private final List<Method> exceptionHandlerMethods;

    private AgentMetadata(Class<?> agentClass, String agentName, String agentDescription,
                          List<Method> triggerMethods, List<Method> decisionMethods,
                          List<Method> actionMethods, List<Method> outcomeMethods,
                          List<Method> exceptionHandlerMethods) {
        this.agentClass = agentClass;
        this.agentName = agentName;
        this.agentDescription = agentDescription;
        this.triggerMethods = Collections.unmodifiableList(triggerMethods);
        this.decisionMethods = Collections.unmodifiableList(decisionMethods);
        this.actionMethods = Collections.unmodifiableList(actionMethods);
        this.outcomeMethods = Collections.unmodifiableList(outcomeMethods);
        this.exceptionHandlerMethods = Collections.unmodifiableList(exceptionHandlerMethods);
    }

    /**
     * Creates AgentMetadata by introspecting the given class.
     *
     * @param agentClass the class annotated with @Agent
     * @return the extracted metadata
     */
    public static AgentMetadata fromClass(Class<?> agentClass) {
        Agent agentAnnotation = agentClass.getAnnotation(Agent.class);
        if (agentAnnotation == null) {
            throw new IllegalArgumentException("Class " + agentClass.getName() + " is not annotated with @Agent");
        }

        String name = agentAnnotation.name();
        if (name == null || name.isEmpty()) {
            // Use class name in camelCase as default
            name = toCamelCase(agentClass.getSimpleName());
        }

        String description = agentAnnotation.description();

        List<Method> triggers = new ArrayList<>();
        List<Method> decisions = new ArrayList<>();
        List<Method> actions = new ArrayList<>();
        List<Method> outcomes = new ArrayList<>();
        List<Method> exceptionHandlers = new ArrayList<>();

        // Scan all methods including inherited ones
        for (Method method : agentClass.getMethods()) {
            if (method.isAnnotationPresent(Trigger.class)) {
                triggers.add(method);
            }
            if (method.isAnnotationPresent(Decision.class)) {
                decisions.add(method);
            }
            if (method.isAnnotationPresent(Action.class)) {
                actions.add(method);
            }
            if (method.isAnnotationPresent(Outcome.class)) {
                outcomes.add(method);
            }
            if (method.isAnnotationPresent(HandleException.class)) {
                exceptionHandlers.add(method);
            }
        }

        // Also scan declared methods for private methods
        for (Method method : agentClass.getDeclaredMethods()) {
            if (!method.canAccess(null) || java.lang.reflect.Modifier.isPrivate(method.getModifiers())) {
                if (method.isAnnotationPresent(Trigger.class) && !triggers.contains(method)) {
                    method.setAccessible(true);
                    triggers.add(method);
                }
                if (method.isAnnotationPresent(Decision.class) && !decisions.contains(method)) {
                    method.setAccessible(true);
                    decisions.add(method);
                }
                if (method.isAnnotationPresent(Action.class) && !actions.contains(method)) {
                    method.setAccessible(true);
                    actions.add(method);
                }
                if (method.isAnnotationPresent(Outcome.class) && !outcomes.contains(method)) {
                    method.setAccessible(true);
                    outcomes.add(method);
                }
                if (method.isAnnotationPresent(HandleException.class) && !exceptionHandlers.contains(method)) {
                    method.setAccessible(true);
                    exceptionHandlers.add(method);
                }
            }
        }

        // Sort actions by method name for consistent ordering
        actions.sort(Comparator.comparing(Method::getName));

        return new AgentMetadata(agentClass, name, description, triggers, decisions, actions, outcomes, exceptionHandlers);
    }

    private static String toCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    public Class<?> getAgentClass() {
        return agentClass;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentDescription() {
        return agentDescription;
    }

    public List<Method> getTriggerMethods() {
        return triggerMethods;
    }

    public List<Method> getDecisionMethods() {
        return decisionMethods;
    }

    public List<Method> getActionMethods() {
        return actionMethods;
    }

    public List<Method> getOutcomeMethods() {
        return outcomeMethods;
    }

    public List<Method> getExceptionHandlerMethods() {
        return exceptionHandlerMethods;
    }

    /**
     * Checks if this agent has a valid workflow structure.
     *
     * @return true if at least one trigger method exists
     */
    public boolean hasValidWorkflow() {
        return !triggerMethods.isEmpty();
    }

    @Override
    public String toString() {
        return "AgentMetadata{" +
                "agentName='" + agentName + '\'' +
                ", triggers=" + triggerMethods.size() +
                ", decisions=" + decisionMethods.size() +
                ", actions=" + actionMethods.size() +
                ", outcomes=" + outcomeMethods.size() +
                ", exceptionHandlers=" + exceptionHandlerMethods.size() +
                '}';
    }
}
