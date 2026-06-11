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

import fish.payara.ai.agent.cdi.WorkflowScopeManager;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Result;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WorkflowEngine {

    private final BeanManager beanManager;
    private final WorkflowScopeManager workflowScopeManager;
    private final ParameterResolver parameterResolver;

    public WorkflowEngine(BeanManager beanManager, WorkflowScopeManager workflowScopeManager) {
        this.beanManager = beanManager;
        this.workflowScopeManager = workflowScopeManager;
        this.parameterResolver = new ParameterResolver(beanManager);
    }

    public void execute(AgentMetadata agentMetadata, Object triggerEvent) {
        workflowScopeManager.activate();
        WorkflowContext workflowContext = new WorkflowContext();
        if (triggerEvent != null) {
            workflowContext.add(triggerEvent);
        }

        Object agentInstance = null;
        LargeLanguageModel llm = null;

        try {
            agentInstance = resolveBean(agentMetadata.getAgentClass());
            llm = resolveBean(LargeLanguageModel.class);

            Object triggerResult = invokePhase(agentMetadata.getTriggerMethod(), agentInstance, workflowContext, llm, null);
            workflowContext.add(triggerResult);

            for (PhaseMethod phase : agentMetadata.getSortedPhases()) {
                Object result = invokePhase(phase.getMethod(), agentInstance, workflowContext, llm, null);
                if (phase.getType() == PhaseType.DECISION) {
                    if (!shouldContinue(result)) return;
                    addDecisionResultToContext(result, workflowContext);
                } else {
                    workflowContext.add(result);
                }
            }

            if (agentMetadata.getOutcomeMethod() != null) {
                invokePhase(agentMetadata.getOutcomeMethod(), agentInstance, workflowContext, llm, null);
            }
        } catch (Exception e) {
            Throwable cause = unwrap(e);
            boolean handled = dispatchException(agentMetadata, agentInstance, workflowContext, llm, cause);
            if (!handled) {
                if (cause instanceof RuntimeException re) throw re;
                throw new RuntimeException(cause);
            }
        } finally {
            workflowScopeManager.deactivate();
        }
    }

    private boolean dispatchException(AgentMetadata metadata, Object agent,
                                      WorkflowContext ctx, LargeLanguageModel llm,
                                      Throwable exception) {
        Method best = null;
        Class<?> bestType = null;
        for (Method handler : metadata.getExceptionHandlers()) {
            Class<?> exType = findExceptionParamType(handler);
            if (exType != null && exType.isInstance(exception)) {
                if (best == null || bestType.isAssignableFrom(exType)) {
                    best = handler;
                    bestType = exType;
                }
            }
        }
        if (best == null) return false;
        try {
            invokePhase(best, agent, ctx, llm, exception);
            return true;
        } catch (Exception handlerEx) {
            Throwable cause = unwrap(handlerEx);
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        }
    }

    private Object invokePhase(Method method, Object instance, WorkflowContext ctx,
                               LargeLanguageModel llm, Throwable ex) throws Exception {
        method.setAccessible(true);
        Object[] args = parameterResolver.resolve(method, ctx, llm, ex);
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException ite) {
            throw (Exception) (ite.getCause() instanceof Exception ? ite.getCause() : ite);
        }
    }

    private boolean shouldContinue(Object result) {
        return switch (result) {
            case null ->  false;
            case Boolean b -> b;
            case Result r -> r.success();
            default -> true;
        };
    }

    private void addDecisionResultToContext(Object result, WorkflowContext ctx) {
        if (result instanceof Result r) {
            ctx.add(r.details());
        } else if (!(result instanceof Boolean)) {
            ctx.add(result);
        }
    }

    private Class<?> findExceptionParamType(Method handler) {
        for (Class<?> parameterType : handler.getParameterTypes()) {
            if (Throwable.class.isAssignableFrom(parameterType)) return parameterType;
        }
        return null;
    }

    private Throwable unwrap(Exception e) {
        return (e instanceof InvocationTargetException ite && ite.getCause() != null)
                ? ite.getCause() : e;
    }

    private <T> T resolveBean(Class<?> agentClass) {
        var beans = beanManager.getBeans(agentClass);
        if (beans.isEmpty()) {
            return null;
        }
        var bean = beanManager.resolve(beans);
        var context = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, agentClass, context);
    }
}

