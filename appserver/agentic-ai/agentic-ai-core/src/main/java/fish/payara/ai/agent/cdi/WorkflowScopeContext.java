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
package fish.payara.ai.agent.cdi;

import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI Context implementation for @WorkflowScoped beans.
 * <p>
 * This context is active during the execution of an agent workflow, spanning
 * trigger, decision, action, and outcome phases. Beans in this scope are created
 * when the workflow starts and destroyed when it completes.
 * <p>
 * The context uses a ThreadLocal to track the current workflow, allowing concurrent
 * workflows to have independent scopes.
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
public class WorkflowScopeContext implements AlterableContext {

    private static final Logger logger = Logger.getLogger(WorkflowScopeContext.class.getName());

    /**
     * Thread-local storage for the current workflow ID.
     */
    private static final ThreadLocal<String> currentWorkflowId = new ThreadLocal<>();

    /**
     * Storage for workflow-scoped bean instances, keyed by workflow ID.
     */
    private static final Map<String, WorkflowBeanStore> workflowStores = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Annotation> getScope() {
        return WorkflowScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        String workflowId = getCurrentWorkflowId();
        if (workflowId == null) {
            throw new ContextNotActiveException("WorkflowScoped context is not active. " +
                    "Ensure you are within an active workflow execution.");
        }

        WorkflowBeanStore store = workflowStores.computeIfAbsent(workflowId, k -> new WorkflowBeanStore());
        return store.get(contextual, creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        String workflowId = getCurrentWorkflowId();
        if (workflowId == null) {
            throw new ContextNotActiveException("WorkflowScoped context is not active. " +
                    "Ensure you are within an active workflow execution.");
        }

        WorkflowBeanStore store = workflowStores.get(workflowId);
        if (store == null) {
            return null;
        }
        return store.get(contextual);
    }

    @Override
    public boolean isActive() {
        return currentWorkflowId.get() != null;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        String workflowId = getCurrentWorkflowId();
        if (workflowId != null) {
            WorkflowBeanStore store = workflowStores.get(workflowId);
            if (store != null) {
                store.destroy(contextual);
            }
        }
    }

    /**
     * Gets the current workflow ID for this thread.
     *
     * @return the workflow ID, or null if no workflow is active
     */
    public static String getCurrentWorkflowId() {
        return currentWorkflowId.get();
    }

    /**
     * Starts a new workflow scope and returns the workflow ID.
     *
     * @return the new workflow ID
     */
    public static String startWorkflow() {
        String workflowId = UUID.randomUUID().toString();
        currentWorkflowId.set(workflowId);
        workflowStores.put(workflowId, new WorkflowBeanStore());
        logger.log(Level.FINE, "Started workflow scope: {0}", workflowId);
        return workflowId;
    }

    /**
     * Starts a workflow scope with a specific ID (useful for resuming workflows).
     *
     * @param workflowId the workflow ID to use
     */
    public static void startWorkflow(String workflowId) {
        currentWorkflowId.set(workflowId);
        workflowStores.computeIfAbsent(workflowId, k -> new WorkflowBeanStore());
        logger.log(Level.FINE, "Started/resumed workflow scope: {0}", workflowId);
    }

    /**
     * Ends the current workflow scope and destroys all beans in the scope.
     */
    public static void endWorkflow() {
        String workflowId = currentWorkflowId.get();
        if (workflowId != null) {
            WorkflowBeanStore store = workflowStores.remove(workflowId);
            if (store != null) {
                store.destroyAll();
            }
            currentWorkflowId.remove();
            logger.log(Level.FINE, "Ended workflow scope: {0}", workflowId);
        }
    }

    /**
     * Suspends the current workflow scope (removes from thread but keeps beans alive).
     *
     * @return the workflow ID that was suspended
     */
    public static String suspendWorkflow() {
        String workflowId = currentWorkflowId.get();
        currentWorkflowId.remove();
        logger.log(Level.FINE, "Suspended workflow scope: {0}", workflowId);
        return workflowId;
    }

    /**
     * Internal storage for beans within a single workflow scope.
     */
    private static class WorkflowBeanStore {

        private final Map<Contextual<?>, BeanInstance<?>> instances = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            BeanInstance<T> instance = (BeanInstance<T>) instances.computeIfAbsent(contextual, c -> {
                T bean = contextual.create(creationalContext);
                return new BeanInstance<>(bean, creationalContext);
            });
            return instance.getInstance();
        }

        @SuppressWarnings("unchecked")
        <T> T get(Contextual<T> contextual) {
            BeanInstance<T> instance = (BeanInstance<T>) instances.get(contextual);
            return instance != null ? instance.getInstance() : null;
        }

        void destroy(Contextual<?> contextual) {
            BeanInstance<?> instance = instances.remove(contextual);
            if (instance != null) {
                destroyInstance(contextual, instance);
            }
        }

        void destroyAll() {
            instances.forEach(this::destroyInstance);
            instances.clear();
        }

        @SuppressWarnings("unchecked")
        private <T> void destroyInstance(Contextual<T> contextual, BeanInstance<?> instance) {
            BeanInstance<T> typedInstance = (BeanInstance<T>) instance;
            contextual.destroy(typedInstance.getInstance(), typedInstance.getCreationalContext());
        }
    }

    /**
     * Holder for a bean instance and its creational context.
     */
    private static class BeanInstance<T> {
        private final T instance;
        private final CreationalContext<T> creationalContext;

        BeanInstance(T instance, CreationalContext<T> creationalContext) {
            this.instance = instance;
            this.creationalContext = creationalContext;
        }

        T getInstance() {
            return instance;
        }

        CreationalContext<T> getCreationalContext() {
            return creationalContext;
        }
    }
}
