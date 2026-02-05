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

import fish.payara.ai.agent.cdi.WorkflowScopeContext;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.WorkflowContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executor for agent workflows.
 * <p>
 * Orchestrates the execution of workflow phases (Trigger -> Decision -> Action -> Outcome)
 * and handles parameter injection for workflow methods.
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
@ApplicationScoped
public class WorkflowExecutor {

    private static final Logger logger = Logger.getLogger(WorkflowExecutor.class.getName());

    private final Map<Class<?>, AgentMetadata> metadataCache = new ConcurrentHashMap<>();

    @Inject
    private LargeLanguageModel largeLanguageModel;

    @Inject
    private WorkflowContext workflowContext;

    /**
     * Executes a complete workflow for the given agent with the provided trigger data.
     *
     * @param agent       the agent instance
     * @param triggerData the data that triggered the workflow
     * @param <T>         the type of trigger data
     * @return the workflow result, or null if no outcome
     */
    public <T> Object executeWorkflow(Object agent, T triggerData) {
        String workflowId = WorkflowScopeContext.startWorkflow();

        try {
            logger.log(Level.INFO, "Starting workflow for agent: {0}, workflowId: {1}",
                    new Object[]{agent.getClass().getSimpleName(), workflowId});

            AgentMetadata metadata = getMetadata(agent.getClass());
            WorkflowState state = new WorkflowState(triggerData);

            // Set trigger event in context
            if (workflowContext instanceof WorkflowContextImpl) {
                ((WorkflowContextImpl) workflowContext).setTriggerEvent(triggerData);
            }

            // Phase 1: Trigger
            executeTriggerPhase(agent, metadata, state);

            // Phase 2: Decision
            if (!executeDecisionPhase(agent, metadata, state)) {
                logger.log(Level.INFO, "Workflow stopped by decision phase");
                return state.getLastResult();
            }

            // Phase 3: Actions
            executeActionPhase(agent, metadata, state);

            // Phase 4: Outcome
            return executeOutcomePhase(agent, metadata, state);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during workflow execution", e);
            return handleException(agent, getMetadata(agent.getClass()), e, triggerData);
        } finally {
            WorkflowScopeContext.endWorkflow();
            logger.log(Level.INFO, "Workflow completed: {0}", workflowId);
        }
    }

    private void executeTriggerPhase(Object agent, AgentMetadata metadata, WorkflowState state) throws Exception {
        logger.log(Level.FINE, "Executing trigger phase");

        for (Method triggerMethod : metadata.getTriggerMethods()) {
            Object[] args = buildArguments(triggerMethod, state);
            Object result = invokeMethod(agent, triggerMethod, args);
            if (result != null) {
                state.setLastResult(result);
            }
        }
    }

    private boolean executeDecisionPhase(Object agent, AgentMetadata metadata, WorkflowState state) throws Exception {
        logger.log(Level.FINE, "Executing decision phase");

        for (Method decisionMethod : metadata.getDecisionMethods()) {
            Object[] args = buildArguments(decisionMethod, state);
            Object result = invokeMethod(agent, decisionMethod, args);

            // Interpret decision result
            if (result == null) {
                return false; // null means stop workflow
            }

            if (result instanceof Boolean) {
                if (!(Boolean) result) {
                    return false; // false means stop workflow
                }
            } else if (result instanceof Result) {
                Result decisionResult = (Result) result;
                state.setDecisionResult(decisionResult);
                if (!decisionResult.success()) {
                    return false;
                }
                if (decisionResult.details() != null) {
                    state.setLastResult(decisionResult.details());
                }
            } else {
                // Any other non-null object means proceed
                state.setLastResult(result);
            }
        }

        return true;
    }

    private void executeActionPhase(Object agent, AgentMetadata metadata, WorkflowState state) throws Exception {
        logger.log(Level.FINE, "Executing action phase with {0} action(s)", metadata.getActionMethods().size());

        for (Method actionMethod : metadata.getActionMethods()) {
            Object[] args = buildArguments(actionMethod, state);
            Object result = invokeMethod(agent, actionMethod, args);
            if (result != null) {
                state.setLastResult(result);
            }
        }
    }

    private Object executeOutcomePhase(Object agent, AgentMetadata metadata, WorkflowState state) throws Exception {
        logger.log(Level.FINE, "Executing outcome phase");

        Object finalResult = state.getLastResult();

        for (Method outcomeMethod : metadata.getOutcomeMethods()) {
            Object[] args = buildArguments(outcomeMethod, state);
            Object result = invokeMethod(agent, outcomeMethod, args);
            if (result != null) {
                finalResult = result;
            }
        }

        return finalResult;
    }

    private Object handleException(Object agent, AgentMetadata metadata, Exception exception, Object triggerData) {
        for (Method handlerMethod : metadata.getExceptionHandlerMethods()) {
            try {
                Object[] args = buildExceptionHandlerArguments(handlerMethod, exception, triggerData);
                return invokeMethod(agent, handlerMethod, args);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in exception handler", e);
            }
        }

        // No handler found or handler failed, rethrow
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw new RuntimeException("Workflow execution failed", exception);
    }

    private Object[] buildArguments(Method method, WorkflowState state) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            args[i] = resolveArgument(paramType, state);
        }

        return args;
    }

    private Object resolveArgument(Class<?> paramType, WorkflowState state) {
        // WorkflowContext injection
        if (WorkflowContext.class.isAssignableFrom(paramType)) {
            return workflowContext;
        }

        // LargeLanguageModel injection
        if (LargeLanguageModel.class.isAssignableFrom(paramType)) {
            return largeLanguageModel;
        }

        // Result injection (from decision phase)
        if (Result.class.isAssignableFrom(paramType) && state.getDecisionResult() != null) {
            return state.getDecisionResult();
        }

        // Trigger data
        if (state.getTriggerData() != null && paramType.isInstance(state.getTriggerData())) {
            return state.getTriggerData();
        }

        // Last result from previous phase
        if (state.getLastResult() != null && paramType.isInstance(state.getLastResult())) {
            return state.getLastResult();
        }

        // Check decision result details
        if (state.getDecisionResult() != null && state.getDecisionResult().details() != null
                && paramType.isInstance(state.getDecisionResult().details())) {
            return state.getDecisionResult().details();
        }

        return null;
    }

    private Object[] buildExceptionHandlerArguments(Method method, Exception exception, Object triggerData) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();

            if (Exception.class.isAssignableFrom(paramType) || Throwable.class.isAssignableFrom(paramType)) {
                args[i] = exception;
            } else if (WorkflowContext.class.isAssignableFrom(paramType)) {
                args[i] = workflowContext;
            } else if (triggerData != null && paramType.isInstance(triggerData)) {
                args[i] = triggerData;
            } else {
                args[i] = null;
            }
        }

        return args;
    }

    private Object invokeMethod(Object target, Method method, Object[] args) throws Exception {
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private AgentMetadata getMetadata(Class<?> agentClass) {
        return metadataCache.computeIfAbsent(agentClass, AgentMetadata::fromClass);
    }

    /**
     * Internal state holder for workflow execution.
     */
    private static class WorkflowState {
        private final Object triggerData;
        private Object lastResult;
        private Result decisionResult;

        WorkflowState(Object triggerData) {
            this.triggerData = triggerData;
        }

        Object getTriggerData() {
            return triggerData;
        }

        Object getLastResult() {
            return lastResult;
        }

        void setLastResult(Object lastResult) {
            this.lastResult = lastResult;
        }

        Result getDecisionResult() {
            return decisionResult;
        }

        void setDecisionResult(Result decisionResult) {
            this.decisionResult = decisionResult;
        }
    }
}
