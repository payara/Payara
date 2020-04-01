/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2016-2020] [Payara Foundation and/or its affiliates]
package org.glassfish.api.invocation;

import static java.lang.ThreadLocal.withInitial;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.EJB_INVOCATION;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVICE_STARTUP;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.UN_INITIALIZED;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class InvocationManagerImpl implements InvocationManager {

    // This TLS variable stores an ArrayList.
    // The ArrayList contains ComponentInvocation objects which represent
    // the stack of invocations on this thread. Accesses to the ArrayList
    // don't need to be synchronized because each thread has its own ArrayList.
    private final InheritableThreadLocal<InvocationArray<ComponentInvocation>> frames;

    private final ThreadLocal<Stack<ApplicationEnvironment>> applicationEnvironments = withInitial(Stack<ApplicationEnvironment>::new);
    private final ThreadLocal<Deque<Method>> webServiceMethods = withInitial(ArrayDeque<Method>::new);

    private final ConcurrentMap<ComponentInvocationType, ListComponentInvocationHandler> singleTypeHandlers = new ConcurrentHashMap<>();
    private final ComponentInvocationHandler multiTypeHandler;

    public InvocationManagerImpl() {
        this(null);
    }

    @Inject
    private InvocationManagerImpl(@Optional IterableProvider<ComponentInvocationHandler> handlers) {
        this.multiTypeHandler = initInvocationHandlers(handlers);

        frames = new InheritableThreadLocal<InvocationArray<ComponentInvocation>>() {

            protected InvocationArray<ComponentInvocation> initialValue() {
                return new InvocationArray<>();
            }

            protected InvocationArray<ComponentInvocation> childValue(InvocationArray<ComponentInvocation> parentValue) {
                return computeChildTheadInvocation(parentValue);
            }
        };
    }

    private static ComponentInvocationHandler initInvocationHandlers(IterableProvider<ComponentInvocationHandler> handlers) {
        if (handlers == null) {
            return null;
        }
        List<ComponentInvocationHandler> hs = new ArrayList<>();
        handlers.forEach(hs::add);
        if (hs.isEmpty()) {
            return null;
        }
        if (hs.size() == 1) {
            return hs.get(0);
        }
        return new ListComponentInvocationHandler(hs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setThreadInheritableInvocation(List<? extends ComponentInvocation> parentValue) {
        frames.set(computeChildTheadInvocation((InvocationArray<ComponentInvocation>) parentValue));
    }

    @Override
    public <T extends ComponentInvocation> void preInvoke(T invocation) throws InvocationException {

        InvocationArray<ComponentInvocation> invocationArray = frames.get();
        if (invocation.getInvocationType() == SERVICE_STARTUP) {
            invocationArray.setInvocationAttribute(SERVICE_STARTUP);
            return;
        }

        int beforeSize = invocationArray.size();
        ComponentInvocation previousInvocation = beforeSize > 0 ? invocationArray.get(beforeSize - 1) : null;

        ComponentInvocationType type = invocation.getInvocationType();
        ComponentInvocationHandler singleTypeHandler = singleTypeHandlers.get(type);

        try {
            if (multiTypeHandler != null) {
                multiTypeHandler.beforePreInvoke(type, previousInvocation, invocation);
            }
            if (singleTypeHandler != null) {
                singleTypeHandler.beforePreInvoke(type, previousInvocation, invocation);
            }
        } finally {
            // Push this invocation on the stack
            invocationArray.add(invocation);

            if (multiTypeHandler != null) {
                multiTypeHandler.afterPreInvoke(type, previousInvocation, invocation);
            }
            if (singleTypeHandler != null) {
                singleTypeHandler.afterPreInvoke(type, previousInvocation, invocation);
            }
        }
    }

    @Override
    public <T extends ComponentInvocation> void postInvoke(T invocation) throws InvocationException {

        // Get this thread's ArrayList
        InvocationArray<ComponentInvocation> invocationArray = frames.get();
        if (invocation.getInvocationType() == SERVICE_STARTUP) {
            invocationArray.setInvocationAttribute(UN_INITIALIZED);
            return;
        }

        int beforeSize = invocationArray.size();
        if (beforeSize == 0) {
            throw new InvocationException();
        }

        ComponentInvocation prevInv = beforeSize > 1 ? invocationArray.get(beforeSize - 2) : null;
        ComponentInvocation curInv = invocationArray.get(beforeSize - 1);

        ComponentInvocationType type = invocation.getInvocationType();
        ComponentInvocationHandler singleTypeHandler = singleTypeHandlers.get(type);
        try {
            if (multiTypeHandler != null) {
                multiTypeHandler.beforePostInvoke(type, prevInv, curInv);
            }
            if (singleTypeHandler != null) {
                singleTypeHandler.beforePostInvoke(type, prevInv, curInv);
            }
        } finally {
            // pop the stack
            invocationArray.remove(beforeSize - 1);

            if (multiTypeHandler != null) {
                multiTypeHandler.afterPostInvoke(type, prevInv, invocation);
            }
            if (singleTypeHandler != null) {
                singleTypeHandler.afterPostInvoke(type, prevInv, curInv);
            }
        }
    }

    /**
     * return true iff no invocations on the stack for this thread
     * @return
     */
    @Override
    public boolean isInvocationStackEmpty() {
        InvocationArray<ComponentInvocation> invocations = frames.get();
        return invocations == null || invocations.size() == 0;
    }

    /**
     * return the Invocation object of the component being called
     * @param <T>
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentInvocation> T getCurrentInvocation() {
        InvocationArray<ComponentInvocation> invocations = frames.get();
        int invocationsSize = invocations.size();
        if (invocationsSize == 0) {
            return null;
        }

        return (T) invocations.get(invocationsSize - 1);
    }

    /**
     * return the Invocation object of the caller
     * return null if none exist (e.g. caller is from another VM)
     * @param <T>
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentInvocation> T getPreviousInvocation() throws InvocationException {
        InvocationArray<ComponentInvocation> invocations = frames.get();
        int invocationsSize = invocations.size();
        if (invocationsSize < 2) {
            return null;
        }

        return (T) invocations.get(invocationsSize - 2);
    }

    @Override
    public List<? extends ComponentInvocation> getAllInvocations() {
        return frames.get();
    }

    @Override
    public List<? extends ComponentInvocation> popAllInvocations() {
        List<? extends ComponentInvocation> result = frames.get();
        frames.set(null);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAllInvocations(List<? extends ComponentInvocation> invocations) {
        frames.set(new InvocationArray<>((List<ComponentInvocation>) invocations));
    }

    private InvocationArray<ComponentInvocation> computeChildTheadInvocation(InvocationArray<ComponentInvocation> parentValue) {

        // Always creates a new ArrayList
        InvocationArray<ComponentInvocation> childInvocationArray = new InvocationArray<ComponentInvocation>();
        InvocationArray<ComponentInvocation> parentInvocationArray = parentValue;

        if (parentInvocationArray != null && parentInvocationArray.size() > 0 && parentInvocationArray.outsideStartup()) {

            // Get current invocation
            ComponentInvocation parentInvocation = parentInvocationArray.get(parentInvocationArray.size() - 1);

            // TODO: The following is ugly. The logic of what needs to be in the
            // new ComponentInvocation should be with the respective container

            if (parentInvocation.getInvocationType() == SERVLET_INVOCATION) {

                // If this is a thread created by user in servlet's service method
                // create a new ComponentInvocation with transaction
                // left to null and instance left to null
                // so that the resource won't be enlisted or registered

                ComponentInvocation invocation = new ComponentInvocation();
                invocation.setComponentInvocationType(parentInvocation.getInvocationType());
                invocation.setComponentId(parentInvocation.getComponentId());
                invocation.setAppName(parentInvocation.getAppName());
                invocation.setModuleName(parentInvocation.getModuleName());
                invocation.setContainer(parentInvocation.getContainer());
                invocation.setJndiEnvironment(parentInvocation.getJndiEnvironment());

                childInvocationArray.add(invocation);
            } else if (parentInvocation.getInvocationType() != EJB_INVOCATION) {

                // Push a copy of invocation onto the new result
                // ArrayList
                ComponentInvocation cpy = new ComponentInvocation();
                cpy.componentId = parentInvocation.getComponentId();
                cpy.setComponentInvocationType(parentInvocation.getInvocationType());
                cpy.instance = parentInvocation.getInstance();
                cpy.container = parentInvocation.getContainerContext();
                cpy.transaction = parentInvocation.getTransaction();

                childInvocationArray.add(cpy);
            }
        }

        return childInvocationArray;
    }


    static class InvocationArray<T extends ComponentInvocation> extends ArrayList<T> {

        private static final long serialVersionUID = 1L;

        private ComponentInvocationType invocationAttribute;

        private InvocationArray(List<T> invocations) {
            super(invocations);
        }

        private InvocationArray() {
        }

        public void setInvocationAttribute(ComponentInvocationType attribute) {
            this.invocationAttribute = attribute;
        }

        public ComponentInvocationType getInvocationAttribute() {
            return invocationAttribute;
        }

        public boolean outsideStartup() {
            return getInvocationAttribute() != SERVICE_STARTUP;
        }
    }

    @Override
    public void registerComponentInvocationHandler(ComponentInvocationType type, RegisteredComponentInvocationHandler handler) {
        singleTypeHandlers.computeIfAbsent(type,
                key -> new ListComponentInvocationHandler(new CopyOnWriteArrayList<>())) // OBS! must be thread safe List here
            .add(handler.getComponentInvocationHandler());
    }

    @Override
    public void pushAppEnvironment(ApplicationEnvironment env) {
        applicationEnvironments.get().push(env);
    }

    @Override
    public ApplicationEnvironment peekAppEnvironment() {
        Stack<ApplicationEnvironment> stack = applicationEnvironments.get();

        if (stack.isEmpty()) {
            return null;
        }

        return stack.peek();
    }

    @Override
    public void popAppEnvironment() {
        Stack<ApplicationEnvironment> stack = applicationEnvironments.get();

        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    @Override
    public void pushWebServiceMethod(Method method) {
        webServiceMethods.get().push(method);
    }

    @Override
    public Method peekWebServiceMethod() {
        return webServiceMethods.get().peek();
    }

    @Override
    public void popWebServiceMethod() {
        webServiceMethods.get().pollFirst();
    }

    /**
     * Adapter to make {@link List} of {@link ComponentInvocationHandler} usable as a single
     * {@link ComponentInvocationHandler} to break down all moving parts in this implementation.
     */
    private static final class ListComponentInvocationHandler implements ComponentInvocationHandler {

        private final List<ComponentInvocationHandler> handlers;

        ListComponentInvocationHandler(List<ComponentInvocationHandler> handlers) {
            this.handlers = handlers;
        }

        void add(ComponentInvocationHandler handler) {
            handlers.add(handler);
        }

        @Override
        public void beforePreInvoke(ComponentInvocationType type, ComponentInvocation prev, ComponentInvocation cur)
                throws InvocationException {
            handlers.forEach(handler -> handler.beforePreInvoke(type, prev, cur));
        }

        @Override
        public void afterPreInvoke(ComponentInvocationType type, ComponentInvocation prev, ComponentInvocation cur)
                throws InvocationException {
            handlers.forEach(handler -> handler.afterPreInvoke(type, prev, cur));
        }

        @Override
        public void beforePostInvoke(ComponentInvocationType type, ComponentInvocation prev, ComponentInvocation cur)
                throws InvocationException {
            handlers.forEach(handler -> handler.beforePostInvoke(type, prev, cur));
        }

        @Override
        public void afterPostInvoke(ComponentInvocationType type, ComponentInvocation prev, ComponentInvocation cur)
                throws InvocationException {
            handlers.forEach(handler -> handler.afterPostInvoke(type, prev, cur));
        }

    }
}
