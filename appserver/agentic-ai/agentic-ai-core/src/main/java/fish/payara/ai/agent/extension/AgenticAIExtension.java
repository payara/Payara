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
package fish.payara.ai.agent.extension;

import fish.payara.ai.agent.cdi.WorkflowScopeContext;
import fish.payara.ai.agent.cdi.WorkflowScopeManager;
import fish.payara.ai.agent.engine.AgentMetadata;
import fish.payara.ai.agent.engine.PhaseMethod;
import fish.payara.ai.agent.engine.PhaseType;
import fish.payara.ai.agent.engine.WorkflowEngine;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CDI portable extension that wires Jakarta Agentic AI agents into the runtime.
 * <p>
 * During bean discovery it finds {@code @Agent} classes, applies the default
 * {@code @WorkflowScoped} scope when none is declared, and rewires their
 * {@code @Trigger} method (see {@link #processAgent}). After bean discovery it
 * registers the {@code @WorkflowScoped} context and one synthetic observer per
 * agent so that firing the trigger event drives the whole workflow through the
 * {@link WorkflowEngine}.
 */
public class AgenticAIExtension implements Extension {

    private final List<Class<?>> agentClasses = new ArrayList<>();

    /**
     * Discovers and prepares each {@code @Agent} type during bean discovery.
     * <p>
     * If the agent declares no scope, {@code @WorkflowScoped} is added as the
     * default. The {@code @Observes} annotation is then removed from the
     * {@code @Trigger} method so the CDI container does not invoke it directly;
     * the engine's synthetic observer becomes the single entry point, which
     * keeps the workflow context active across the whole run and avoids invoking
     * the trigger twice.
     *
     * @param processAnnotatedType the type being processed during bean discovery
     * @param <X>                  the annotated type's Java type
     */
    <X> void processAgent(@Observes ProcessAnnotatedType<X> processAnnotatedType) {
        var annotatedType = processAnnotatedType.getAnnotatedType();
        if (!annotatedType.isAnnotationPresent(Agent.class)) {
            return;
        }

        boolean hasScope = annotatedType.isAnnotationPresent(WorkflowScoped.class)
                || annotatedType.isAnnotationPresent(ApplicationScoped.class);
        var xAnnotatedTypeConfigurator = processAnnotatedType.configureAnnotatedType();
        if (!hasScope) {
            xAnnotatedTypeConfigurator.add(WorkflowScoped.Literal.INSTANCE);
        }

        xAnnotatedTypeConfigurator
                .filterMethods(m -> m.isAnnotationPresent(Trigger.class))
                .forEach(m -> m.params()
                        .forEach(p -> p.remove(a -> a.annotationType().equals(Observes.class))));

        agentClasses.add(annotatedType.getJavaClass());
    }

    /**
     * Registers the {@code @WorkflowScoped} context and a synthetic observer for
     * every discovered agent.
     * <p>
     * Each agent's observer listens for its {@code @Trigger} event type and, when
     * the event fires, runs the complete workflow through the
     * {@link WorkflowEngine}. Agents whose trigger declares no event type are
     * skipped (reserved for future programmatic triggering).
     */
    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        WorkflowScopeContext workflowScopeContext = new WorkflowScopeContext();
        afterBeanDiscovery.addContext(workflowScopeContext);

        WorkflowScopeManager workflowScopeManager = new WorkflowScopeManager(workflowScopeContext);
        WorkflowEngine workflowEngine = new WorkflowEngine(beanManager, workflowScopeManager);

        for (Class<?> agentClass : agentClasses) {
            AgentMetadata agentMetadata = buildMetadata(agentClass);
            Class<?> eventType = agentMetadata.getTriggerEventType();
            if (eventType == null) {
                continue;
            }
            afterBeanDiscovery.addObserverMethod()
                    .beanClass(agentClass)
                    .observedType(eventType)
                    .notifyWith(eventContext -> workflowEngine.execute(agentMetadata, eventContext.getEvent()));
        }
    }

    /**
     * Scans an agent class and builds its validated {@link AgentMetadata}.
     * <p>
     * Collects the {@code @Trigger}, {@code @Decision}, {@code @Action},
     * {@code @Outcome} and {@code @HandleException} methods, then validates the
     * agent's structure at deploy time, raising {@link DefinitionException} when:
     * there is more than one {@code @Trigger} or {@code @Outcome}, no
     * {@code @Trigger} at all, a {@code @WorkflowScoped} agent declares a general
     * {@code @Observes} method outside the trigger, or phase ordering is
     * inconsistent. Phases are sorted into execution order and exception handlers
     * are sorted most-specific-first.
     */
    private AgentMetadata buildMetadata(Class<?> agentClass) {
        List<PhaseMethod> phases = new ArrayList<>();
        List<Method> handlerMethods = new ArrayList<>();
        Method trigger = null;
        Method outcome = null;

        boolean applicationScoped = agentClass.isAnnotationPresent(ApplicationScoped.class);
        for (Method method : agentClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Trigger.class)) {
                if (trigger != null) {
                    throw new DefinitionException("@Agent " +  agentClass.getName() + " cannot have more than one @Trigger annotation");
                }
                trigger = method;
            } else if (method.isAnnotationPresent(Decision.class)) {
                phases.add(new PhaseMethod(method, PhaseType.DECISION));
            } else if (method.isAnnotationPresent(Action.class)) {
                phases.add(new PhaseMethod(method, PhaseType.ACTION));
            } else if (method.isAnnotationPresent(Outcome.class)) {
                if (outcome != null) {
                    throw new DefinitionException("@Agent " +  agentClass.getName() + " cannot have more than one @Outcome annotation");
                }
                outcome = method;
            } else if (method.isAnnotationPresent(HandleException.class)) {
                handlerMethods.add(method);
            } else if (!applicationScoped && hasObservesParam(method)) {
                throw new DefinitionException(
                  "@WorkflowScoped @Agent " + agentClass.getName() + " cannot declare @Observes out of @Trigger"
                );
            }
        }

        if (trigger == null) {
            throw new DefinitionException(
                    "@Agent " + agentClass.getName() + " must declare @Trigger");
        }

        validateOrdering(phases, agentClass);
        sortPhases(phases);
        sortHandlers(handlerMethods);

        return new AgentMetadata(agentClass, resolveAgentName(agentClass), trigger,
                extractTriggerEventType(trigger), phases, outcome, handlerMethods);
    }

    private boolean hasObservesParam(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(Observes.class)) return true;
        }
        return false;
    }

    /**
     * Determines the CDI event type that triggers the workflow.
     * <p>
     * Prefers a parameter explicitly annotated with {@code @Observes}; otherwise
     * falls back to the first parameter that is not a {@link LargeLanguageModel}.
     * Returns {@code null} when the trigger declares no event parameter.
     */
    private Class<?> extractTriggerEventType(Method trigger) {
        for (Parameter parameter : trigger.getParameters()) {
            if (parameter.isAnnotationPresent(Observes.class)) {
                return parameter.getType();
            }
        }
        for (Parameter parameter : trigger.getParameters()) {
            if (!LargeLanguageModel.class.isAssignableFrom(parameter.getType())) {
                return parameter.getType();
            }
        }
        return null;
    }

    private String resolveAgentName(Class<?> agentClass) {
        String name = agentClass.getAnnotation(Agent.class).name();
        if (name.isEmpty()) {
            String s = agentClass.getSimpleName();
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
        return name;
    }

    private void sortHandlers(List<Method> handlerMethods) {
        handlerMethods.sort((a, b) -> {
            Class<?> aClass = findExceptionParamType(a);
            Class<?> bClass = findExceptionParamType(b);
            if (aClass == null || bClass == null) {
                return 0;
            } else if (aClass.isAssignableFrom(bClass)) {
                return 1;
            } else if (bClass.isAssignableFrom(aClass)) {
                return -1;
            } else  {
                return 0;
            }
        });
    }

    private Class<?> findExceptionParamType(Method method) {
        for (Class<?> pt : method.getParameterTypes()) {
            if (Throwable.class.isAssignableFrom(pt)) return pt;
        }
        return null;
    }

    private void sortPhases(List<PhaseMethod> phases) {
        if (phases.stream().anyMatch(PhaseMethod::isExplicitlyOrdered)) {
            phases.sort(Comparator.comparingInt(PhaseMethod::getSortKey));
        }
    }

    private void validateOrdering(List<PhaseMethod> phases, Class<?> agentClass) {
        long explicit = phases.stream().filter(PhaseMethod::isExplicitlyOrdered).count();
        if (explicit > 0 && explicit < phases.size()) {
            throw new DefinitionException("Inconsistent order at @Agent " + agentClass.getName() +
                    ": all @Decision/@Action should declare @Priority or order or nothing.");
        }
    }
}
