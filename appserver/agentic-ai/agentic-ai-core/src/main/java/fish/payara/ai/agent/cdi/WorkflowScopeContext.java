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
package fish.payara.ai.agent.cdi;

import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * CDI {@link AlterableContext} backing the {@code @WorkflowScoped} normal scope.
 * <p>
 * Bean instances are stored per thread in a {@link ThreadLocal} so that each
 * workflow invocation — which runs on its own thread — gets an isolated set of
 * {@code @WorkflowScoped} beans. The context is activated before a workflow
 * starts and deactivated when it ends (see {@link WorkflowScopeManager}); while
 * inactive, any bean access raises {@link ContextNotActiveException}.
 */
public class WorkflowScopeContext implements AlterableContext {
    private static final ThreadLocal<Map<Contextual<?>, BeanInstance<?>>> STORE =
            new ThreadLocal<>();

    @Override
    public Class<? extends Annotation> getScope() {
        return WorkflowScoped.class;
    }

    /**
     * Returns the contextual instance for the current workflow, creating and
     * storing it on first access.
     *
     * @return the existing instance, or a newly created one if none exists yet
     * @throws ContextNotActiveException if no workflow context is active on this thread
     */
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        checkActive();
        Map<Contextual<?>, BeanInstance<?>> map = STORE.get();
        BeanInstance<T> existing = (BeanInstance<T>) map.get(contextual);
        if (existing != null) { 
            return existing.instance;
        }
        T instance = contextual.create(creationalContext);
        map.put(contextual, new BeanInstance<>(contextual, instance, creationalContext));
        return instance;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        checkActive();
        BeanInstance<T> bi = (BeanInstance<T>) STORE.get().get(contextual);
        return bi == null ? null : bi.instance;
    }

    @Override
    public boolean isActive() {
        return STORE.get() != null;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        Map<Contextual<?>, BeanInstance<?>> map = STORE.get();
        if (map == null) {
            return;
        }
        BeanInstance<?> bi = map.remove(contextual);
        if (bi != null) {
            bi.destroy();
        }
    }

    /**
     * Activates a fresh, empty workflow context for the current thread. Must be
     * called before a workflow starts using {@code @WorkflowScoped} beans.
     */
    void activate() {
        STORE.set(new HashMap<>());
    }

    /**
     * Destroys every bean held in the current workflow context (invoking their
     * {@code @PreDestroy} callbacks) and detaches the context from the thread.
     * Called when the workflow ends, on success or failure.
     */
    void deactivate() {
        Map<Contextual<?>, BeanInstance<?>> map = STORE.get();
        if (map != null) {
            map.values().forEach(BeanInstance::destroy);
            STORE.remove();
        }
    }

    private void checkActive() {
        if (!isActive()) {
            throw new ContextNotActiveException(
                    "WorkflowScoped context is not active on this thread");
        }
    }
}
