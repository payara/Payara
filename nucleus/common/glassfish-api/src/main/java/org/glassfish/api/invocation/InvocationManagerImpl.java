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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.api.invocation;

import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.emptyList;
import static java.util.logging.Level.WARNING;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.EJB_INVOCATION;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVICE_STARTUP;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.UN_INITIALIZED;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class InvocationManagerImpl implements InvocationManager {

    private static final Logger LOGGER = Logger.getLogger(InvocationManagerImpl.class.getName());

    private final InheritableThreadLocal<InvocationFrames> framesByThread;
    private final ThreadLocal<Deque<ApplicationEnvironment>> appEnvironments = withInitial(ArrayDeque::new);
    private final ThreadLocal<Deque<Method>> webServiceMethods = withInitial(ArrayDeque::new);
    private final ConcurrentMap<ComponentInvocationType, ListComponentInvocationHandler> typeHandlers = new ConcurrentHashMap<>();
    private final ComponentInvocationHandler allTypesHandler;

    public InvocationManagerImpl() {
        this((ComponentInvocationHandler) null);
    }

    public InvocationManagerImpl(ComponentInvocationHandler... handlers) {
        this(Arrays.asList(handlers));
    }

    @Inject
    private InvocationManagerImpl(@Optional IterableProvider<ComponentInvocationHandler> handlers) {
        this((Iterable<ComponentInvocationHandler>) handlers);
    }

    private InvocationManagerImpl(Iterable<ComponentInvocationHandler> handlers) {
        this.allTypesHandler = initInvocationHandlers(handlers);

        framesByThread = new InheritableThreadLocal<InvocationFrames>() {

            @Override
            protected InvocationFrames initialValue() {
                return new InvocationFrames();
            }

            @Override
            protected InvocationFrames childValue(InvocationFrames parentValue) {
                return computeChildTheadInvocation(parentValue);
            }
        };
    }

    private static ComponentInvocationHandler initInvocationHandlers(Iterable<ComponentInvocationHandler> handlers) {
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

    @Override
    public void setThreadInheritableInvocation(List<? extends ComponentInvocation> parentValue) {
        framesByThread.set(computeChildTheadInvocation(InvocationFrames.valueOf(parentValue)));
    }

    @Override
    public <T extends ComponentInvocation> void preInvoke(T invocation) throws InvocationException {

        InvocationFrames frames = framesByThread.get();
        if (invocation.getInvocationType() == SERVICE_STARTUP) {
            frames.setState(SERVICE_STARTUP);
            LOGGER.finest(() -> "Not storing service startup invocation on the stack:" + invocation);
            return;
        }

        ComponentInvocation prev = frames.peekLast();
        ComponentInvocationType type = invocation.getInvocationType();
        ComponentInvocationHandler typeHandler = typeHandlers.get(type);

        try {
            if (allTypesHandler != null) {
                allTypesHandler.beforePreInvoke(type, prev, invocation);
            }
            if (typeHandler != null) {
                typeHandler.beforePreInvoke(type, prev, invocation);
            }
        } finally {
            // Push this invocation on the stack
            frames.addLast(invocation);
            LOGGER.finest(() -> "Added invocation "+frames.size()+" on the stack:\n" + invocation);

            if (allTypesHandler != null) {
                allTypesHandler.afterPreInvoke(type, prev, invocation);
            }
            if (typeHandler != null) {
                typeHandler.afterPreInvoke(type, prev, invocation);
            }
        }
    }

    @Override
    public <T extends ComponentInvocation> void postInvoke(T invocation) throws InvocationException {
        InvocationFrames frames = framesByThread.get();
        if (invocation.getInvocationType() == SERVICE_STARTUP) {
            frames.setState(UN_INITIALIZED);
            LOGGER.finest(() -> "Skipping SERVICE_STARTUP invocation :" + invocation);
            return;
        }

        Iterator<ComponentInvocation> iter = frames.descendingIterator();
        if (!iter.hasNext()) {
            throw new InvocationException("No invocation on invocation stack. Expected invocation: " + invocation);
        }

        ComponentInvocation current = iter.next(); // the last is the current is "invocation"
        if (isInconsistentUse(invocation, current)) {
            LOGGER.log(WARNING, "postInvoke not called with top of the invocation stack. Expected:\n{0}\nbut was:\n{1}",
                    new Object[] { current, invocation });
            LOGGER.log(Level.FINE, "Stacktrace: ",
                    new IllegalStateException("This exception is not thrown, it is only to trace the invocation"));
        }
        ComponentInvocation prev = iter.hasNext() ? iter.next() : null;

        ComponentInvocationType type = invocation.getInvocationType();
        ComponentInvocationHandler typeHandler = typeHandlers.get(type);
        try {
            if (allTypesHandler != null) {
                allTypesHandler.beforePostInvoke(type, prev, current);
            }
            if (typeHandler != null) {
                typeHandler.beforePostInvoke(type, prev, current);
            }
        } finally {
            // pop the stack
            ComponentInvocation removed = frames.removeLast();
            LOGGER.finest(() -> "Removed\n"+removed+ "\nafter postInvoke of\n"+invocation);

            if (allTypesHandler != null) {
                allTypesHandler.afterPostInvoke(type, prev, current);
            }
            if (typeHandler != null) {
                typeHandler.afterPostInvoke(type, prev, current);
            }
        }
    }

    private static boolean isInconsistentUse(ComponentInvocation a, ComponentInvocation b) {
        if (a == null || b == null) {
            return a != b;
        }
        if (a.getClass() != b.getClass()) {
            return true;
        }
        return a.getClass().getSimpleName().equals("WebComponentInvocation") ? a.instance != b.instance : a != b; // Effectively we ignore WebComponentInvocations for now
    }

    /**
     * @return true iff no invocations on the stack for this thread
     */
    @Override
    public boolean isInvocationStackEmpty() {
        InvocationFrames invocations = framesByThread.get();
        return invocations == null || invocations.isEmpty();
    }

    /**
     * @return the Invocation object of the component being called
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentInvocation> T getCurrentInvocation() {
        if (isInvocationStackEmpty()) {
            return null;
        }
        return (T) framesByThread.get().peekLast();
    }

    /**
     * @return the Invocation object of the caller or null if none exist (e.g. caller is from another VM)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentInvocation> T getPreviousInvocation() throws InvocationException {
        Iterator<ComponentInvocation> iter = framesByThread.get().descendingIterator();
        if (!iter.hasNext()) {
            return null;
        }
        iter.next();
        if (!iter.hasNext()) {
            return null;
        }
        return (T) iter.next();
    }

    @Override
    public List<? extends ComponentInvocation> getAllInvocations() {
        InvocationFrames frames = framesByThread.get();
        return frames == null ? emptyList() : new ArrayList<>(frames);
    }

    @Override
    public List<? extends ComponentInvocation> popAllInvocations() {
        InvocationFrames frames = framesByThread.get();
        if (frames == null) {
            return Collections.emptyList();
        }
        frames.state = UN_INITIALIZED;
        List<? extends ComponentInvocation> result = new ArrayList<>(frames);
        frames.clear();
        LOGGER.finest(() -> "Removed invocations of a thread: " + result);
        return result;
    }

    @Override
    public void putAllInvocations(List<? extends ComponentInvocation> invocations) {
        LOGGER.fine(() -> "Forcefully set invocations of a thread to\n"+invocations);
        framesByThread.set(InvocationFrames.valueOf(invocations));
    }

    static InvocationFrames computeChildTheadInvocation(InvocationFrames parent) {
        InvocationFrames childFrames = new InvocationFrames();
        InvocationFrames parentFrames = parent;

        if (parentFrames != null && !parentFrames.isEmpty() && !parentFrames.isStartup()) {

            // Get current invocation
            ComponentInvocation parentFrame = parentFrames.getLast();

            // TODO: The following is ugly. The logic of what needs to be in the
            // new ComponentInvocation should be with the respective container

            ComponentInvocationType parentType = parentFrame.getInvocationType();
            if (parentType == SERVLET_INVOCATION) {

                // If this is a thread created by user in servlet's service method
                // create a new ComponentInvocation with transaction
                // left to null and instance left to null
                // so that the resource won't be enlisted or registered

                ComponentInvocation invocation = new ComponentInvocation();
                invocation.setComponentInvocationType(parentType);
                invocation.setComponentId(parentFrame.getComponentId());
                invocation.setAppName(parentFrame.getAppName());
                invocation.setModuleName(parentFrame.getModuleName());
                invocation.setContainer(parentFrame.getContainer());
                invocation.setJndiEnvironment(parentFrame.getJndiEnvironment());

                childFrames.add(invocation);
            } else if (parentType != EJB_INVOCATION) {
                // Push a copy of invocation onto the new result
                childFrames.add(new ComponentInvocation(
                        parentFrame.getComponentId(),
                        parentType,
                        parentFrame.getInstance(),
                        parentFrame.getContainerContext(),
                        parentFrame.getTransaction()));
            }
        }
        LOGGER.finest(() -> "Computed new invocation stack for child thread: " + childFrames);
        return childFrames;
    }


    static final class InvocationFrames extends ArrayDeque<ComponentInvocation> {

        private static final long serialVersionUID = 1L;

        private ComponentInvocationType state;

        static InvocationFrames valueOf(Collection<? extends ComponentInvocation> invocations) {
            return invocations instanceof InvocationFrames
                    ? (InvocationFrames) invocations
                    : new InvocationFrames(invocations == null ? emptyList() : invocations);
        }

        private InvocationFrames(Collection<? extends ComponentInvocation> invocations) {
            super(invocations);
        }

        InvocationFrames() {
        }

        void setState(ComponentInvocationType state) {
            this.state = state;
        }

        boolean isStartup() {
            return state == SERVICE_STARTUP;
        }
    }

    @Override
    public void registerComponentInvocationHandler(ComponentInvocationType type, RegisteredComponentInvocationHandler handler) {
        typeHandlers.computeIfAbsent(type,
                key -> new ListComponentInvocationHandler(new CopyOnWriteArrayList<>())) // OBS! must be thread safe List here
            .add(handler.getComponentInvocationHandler());
    }

    @Override
    public void pushAppEnvironment(ApplicationEnvironment env) {
        appEnvironments.get().addLast(env);
    }

    @Override
    public ApplicationEnvironment peekAppEnvironment() {
        return appEnvironments.get().peekLast();
    }

    @Override
    public void popAppEnvironment() {
        appEnvironments.get().pollLast();
    }

    @Override
    public void pushWebServiceMethod(Method method) {
        webServiceMethods.get().addLast(method);
    }

    @Override
    public Method peekWebServiceMethod() {
        return webServiceMethods.get().peekLast();
    }

    @Override
    public void popWebServiceMethod() {
        webServiceMethods.get().pollLast();
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
