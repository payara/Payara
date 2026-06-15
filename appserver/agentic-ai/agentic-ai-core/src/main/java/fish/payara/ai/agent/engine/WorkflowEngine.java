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

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.executable.ExecutableValidator;

/**
 * Orchestrates the execution of an agent workflow.
 * <p>
 * A single {@link #execute(AgentMetadata, Object)} call drives a whole workflow
 * for one triggering event: it activates the {@code @WorkflowScoped} context,
 * invokes the {@code @Trigger} phase, then the ordered {@code @Decision} and
 * {@code @Action} phases, and finally the optional {@code @Outcome} phase. The
 * context is always destroyed when execution finishes, whether it completes
 * normally, terminates early, or fails.
 * <p>
 * Each workflow runs on the calling thread; the {@code @WorkflowScoped} context
 * is bound to that thread, so concurrent workflow invocations stay isolated.
 */
public class WorkflowEngine {

    private final BeanManager beanManager;
    private final WorkflowScopeManager workflowScopeManager;
    private final ParameterResolver parameterResolver;
    private final ExecutableValidator executableValidator;

    public WorkflowEngine(BeanManager beanManager, WorkflowScopeManager workflowScopeManager, ExecutableValidator executableValidator) {
        this.beanManager = beanManager;
        this.workflowScopeManager = workflowScopeManager;
        this.parameterResolver = new ParameterResolver(beanManager);
        this.executableValidator = executableValidator;
    }

    /**
     * Runs the full workflow for a single triggering event.
     * <p>
     * Phases execute in order: {@code @Trigger}, then the pre-sorted
     * {@code @Decision}/{@code @Action} phases, then {@code @Outcome}. The result
     * of each phase is added to the {@link WorkflowContext} so later phases can
     * receive it by type. A {@code @Decision} that signals failure (see
     * {@link #shouldContinue(Object)}) stops the workflow without running the
     * remaining phases or the outcome. Any exception thrown by a phase is routed
     * to a matching {@code @HandleException} method; if none handles it, the
     * exception is rethrown to the container.
     *
     * @param agentMetadata the discovered metadata describing the agent's phases
     * @param triggerEvent  the CDI event that started the workflow, seeded into
     *                      the workflow context (may be {@code null})
     */
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

    /**
     * Routes a thrown exception to the most specific matching
     * {@code @HandleException} method.
     * <p>
     * Among the handlers whose exception parameter is assignable from the thrown
     * exception, the one with the most specific (most derived) exception type is
     * chosen, following the Java exception hierarchy. If the handler returns
     * normally the workflow is considered recovered; if it throws, that exception
     * propagates to the container.
     *
     * @return {@code true} if a handler was found and completed normally;
     * {@code false} if no handler matched (caller must rethrow)
     */
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

    /**
     * Resolves the method's parameters and invokes a single phase method on the
     * agent instance.
     * <p>
     * Any exception thrown by the phase body is unwrapped from the reflective
     * {@link InvocationTargetException} so callers see the original cause.
     *
     * @param ex the in-flight exception, passed only when invoking a
     *           {@code @HandleException} method; {@code null} otherwise
     * @return the phase's return value, or {@code null} for {@code void} phases
     */
    private Object invokePhase(Method method, Object instance, WorkflowContext ctx,
                               LargeLanguageModel llm, Throwable ex) throws Exception {
        method.setAccessible(true);
        Object[] args = parameterResolver.resolve(method, ctx, llm, ex);
        validateParameters(instance, method, args);
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException ite) {
            throw (Exception) (ite.getCause() instanceof Exception ? ite.getCause() : ite);
        }
    }

    /**
     * Decides whether the workflow should proceed past a {@code @Decision} phase.
     * <p>
     * The workflow stops when a decision returns {@code null}, {@code false}, or
     * a {@link Result} whose {@code success()} is {@code false}; any other
     * non-null value lets the workflow continue.
     *
     * @param result the value returned by the {@code @Decision} method
     * @return {@code true} to continue the workflow, {@code false} to terminate
     */
    private boolean shouldContinue(Object result) {
        return switch (result) {
            case null -> false;
            case Boolean b -> b;
            case Result r -> r.success();
            default -> true;
        };
    }

    /**
     * Publishes the data carried by a {@code @Decision} result into the workflow
     * context so later phases can receive it by type.
     * <p>
     * For a {@link Result} the {@code details()} payload is published; a plain
     * {@code Boolean} carries no data and is ignored; any other domain object is
     * published as-is.
     */
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

    /**
     * Validates the resolved parameters of a phase method against any Jakarta
     * Validation constraints declared on them, before the method is invoked.
     *
     * @throws ConstraintViolationException if one or more parameters violate a
     *         constraint; the exception is routed to a matching
     *         {@code @HandleException} method like any other failure
     */
    private void validateParameters(Object instance, Method method, Object[] args) {
        if (executableValidator == null) {
            return;
        }
        var violations =
                executableValidator.validateParameters(instance, method, args);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
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

