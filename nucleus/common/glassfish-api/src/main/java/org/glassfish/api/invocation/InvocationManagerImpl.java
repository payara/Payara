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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
    private InheritableThreadLocal<InvocationArray<ComponentInvocation>> frames;

    private final ThreadLocal<Stack<ApplicationEnvironment>> applicationEnvironments = withInitial(Stack<ApplicationEnvironment>::new);
    private final ThreadLocal<Deque<Method>> webServiceMethods = withInitial(ArrayDeque<Method>::new);

    private Map<ComponentInvocationType, List<RegisteredComponentInvocationHandler>> regCompInvHandlerMap = new HashMap<>();

    private final ComponentInvocationHandler[] invHandlers;

    public InvocationManagerImpl() {
        this(null);
    }

    @Inject
    private InvocationManagerImpl(@Optional IterableProvider<ComponentInvocationHandler> handlers) {
        if (handlers == null) {
            invHandlers = null;
        } else {
            LinkedList<ComponentInvocationHandler> localHandlers = new LinkedList<ComponentInvocationHandler>();
            for (ComponentInvocationHandler handler : handlers) {
                localHandlers.add(handler);
            }

            if (localHandlers.size() > 0) {
                invHandlers = localHandlers.toArray(new ComponentInvocationHandler[localHandlers.size()]);
            } else {
                invHandlers = null;
            }
        }

        frames = new InheritableThreadLocal<InvocationArray<ComponentInvocation>>() {

            protected InvocationArray<ComponentInvocation> initialValue() {
                return new InvocationArray<>();
            }

            protected InvocationArray<ComponentInvocation> childValue(InvocationArray<ComponentInvocation> parentValue) {
                return computeChildTheadInvocation(parentValue);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setThreadInheritableInvocation(List<? extends ComponentInvocation> parentValue) {
        frames.set(computeChildTheadInvocation((InvocationArray<ComponentInvocation>) parentValue));
    }

    public <T extends ComponentInvocation> void preInvoke(T invocation) throws InvocationException {

        InvocationArray<ComponentInvocation> invocationArray = frames.get();
        if (invocation.getInvocationType() == SERVICE_STARTUP) {
            invocationArray.setInvocationAttribute(SERVICE_STARTUP);
            return;
        }

        int beforeSize = invocationArray.size();
        ComponentInvocation previousInvocation = beforeSize > 0 ? invocationArray.get(beforeSize - 1) : null;

        // If ejb call EJBSecurityManager, for servlet call RealmAdapter
        ComponentInvocationType invocationType = invocation.getInvocationType();

        if (invHandlers != null) {
            for (ComponentInvocationHandler handler : invHandlers) {
                handler.beforePreInvoke(invocationType, previousInvocation, invocation);
            }
        }

        List<RegisteredComponentInvocationHandler> setCIH = regCompInvHandlerMap.get(invocationType);
        if (setCIH != null) {
            for (int i = 0; i < setCIH.size(); i++) {
                setCIH.get(i).getComponentInvocationHandler().beforePreInvoke(invocationType, previousInvocation, invocation);
            }
        }

        // Push this invocation on the stack
        invocationArray.add(invocation);

        if (invHandlers != null) {
            for (ComponentInvocationHandler handler : invHandlers) {
                handler.afterPreInvoke(invocationType, previousInvocation, invocation);
            }
        }

        if (setCIH != null) {
            for (int i = 0; i < setCIH.size(); i++) {
                setCIH.get(i).getComponentInvocationHandler().afterPreInvoke(invocationType, previousInvocation, invocation);
            }
        }

    }

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

        try {
            ComponentInvocationType invType = invocation.getInvocationType();

            if (invHandlers != null) {
                for (ComponentInvocationHandler handler : invHandlers) {
                    handler.beforePostInvoke(invType, prevInv, curInv);
                }
            }

            List<RegisteredComponentInvocationHandler> setCIH = regCompInvHandlerMap.get(invType);
            if (setCIH != null) {
                for (int i = 0; i < setCIH.size(); i++) {
                    setCIH.get(i).getComponentInvocationHandler().beforePostInvoke(invType, prevInv, curInv);
                }
            }

        } finally {
            // pop the stack
            invocationArray.remove(beforeSize - 1);

            if (invHandlers != null) {
                for (ComponentInvocationHandler handler : invHandlers) {
                    handler.afterPostInvoke(invocation.getInvocationType(), prevInv, invocation);
                }
            }

            ComponentInvocationType invType = invocation.getInvocationType();

            List<RegisteredComponentInvocationHandler> setCIH = regCompInvHandlerMap.get(invType);
            if (setCIH != null) {
                for (int i = 0; i < setCIH.size(); i++) {
                    setCIH.get(i).getComponentInvocationHandler().afterPostInvoke(invType, prevInv, curInv);
                }
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
            ComponentInvocation parrentInvocation = parentInvocationArray.get(parentInvocationArray.size() - 1);

            // TODO: The following is ugly. The logic of what needs to be in the
            // new ComponentInvocation should be with the respective container

            if (parrentInvocation.getInvocationType() == SERVLET_INVOCATION) {

                // If this is a thread created by user in servlet's service method
                // create a new ComponentInvocation with transaction
                // left to null and instance left to null
                // so that the resource won't be enlisted or registered

                ComponentInvocation invocation = new ComponentInvocation();
                invocation.setComponentInvocationType(parrentInvocation.getInvocationType());
                invocation.setComponentId(parrentInvocation.getComponentId());
                invocation.setAppName(parrentInvocation.getAppName());
                invocation.setModuleName(parrentInvocation.getModuleName());
                invocation.setContainer(parrentInvocation.getContainer());
                invocation.setJndiEnvironment(parrentInvocation.getJndiEnvironment());

                childInvocationArray.add(invocation);
            } else if (parrentInvocation.getInvocationType() != EJB_INVOCATION) {

                // Push a copy of invocation onto the new result
                // ArrayList
                ComponentInvocation cpy = new ComponentInvocation();
                cpy.componentId = parrentInvocation.getComponentId();
                cpy.setComponentInvocationType(parrentInvocation.getInvocationType());
                cpy.instance = parrentInvocation.getInstance();
                cpy.container = parrentInvocation.getContainerContext();
                cpy.transaction = parrentInvocation.getTransaction();

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
        List<RegisteredComponentInvocationHandler> setRegCompInvHandlers = regCompInvHandlerMap.get(type);

        if (setRegCompInvHandlers == null) {
            setRegCompInvHandlers = new ArrayList<RegisteredComponentInvocationHandler>();
            regCompInvHandlerMap.put(type, setRegCompInvHandlers);
        }

        if (setRegCompInvHandlers.size() == 0) {
            setRegCompInvHandlers.add(handler);
        }
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
}
