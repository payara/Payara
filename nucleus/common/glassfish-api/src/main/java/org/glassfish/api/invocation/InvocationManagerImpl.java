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

package org.glassfish.api.invocation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import java.util.HashMap;
import java.util.Map;
import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import org.glassfish.hk2.api.IterableProvider;

import javax.inject.Inject;

@Service
@Singleton
public class InvocationManagerImpl
        implements InvocationManager {

    // This TLS variable stores an ArrayList. 
    // The ArrayList contains ComponentInvocation objects which represent
    // the stack of invocations on this thread. Accesses to the ArrayList
    // dont need to be synchronized because each thread has its own ArrayList.
    private InheritableThreadLocal<InvocationArray<ComponentInvocation>> frames;
    
    private final ThreadLocal<Stack<ApplicationEnvironment>> applicationEnvironments =
            new ThreadLocal<Stack<ApplicationEnvironment>>() {
        @Override
        protected Stack<ApplicationEnvironment> initialValue() {
            return new Stack<ApplicationEnvironment>();
        }
        
    };
    
    private Map<ComponentInvocationType,List<RegisteredComponentInvocationHandler>>  regCompInvHandlerMap
            = new HashMap<ComponentInvocationType, List<RegisteredComponentInvocationHandler>>();

    private final ComponentInvocationHandler[] invHandlers;
    
    public InvocationManagerImpl() {
        this(null);
    }

    @Inject
    private InvocationManagerImpl(@Optional IterableProvider<ComponentInvocationHandler> handlers) {
        if (handlers == null) {
            invHandlers = null;
        }
        else {
            LinkedList<ComponentInvocationHandler> localHandlers = new LinkedList<ComponentInvocationHandler>();
            for (ComponentInvocationHandler handler : handlers) {
                localHandlers.add(handler);
            }
            
            if (localHandlers.size() > 0) {
                invHandlers = localHandlers.toArray(new ComponentInvocationHandler[localHandlers.size()]);
            }
            else {
                invHandlers = null;
            }
        }

        frames = new InheritableThreadLocal<InvocationArray<ComponentInvocation>>() {
            protected InvocationArray initialValue() {
                return new InvocationArray();
            }

            // if this is a thread created by user in servlet's service method
            // create a new ComponentInvocation with transaction
            // set to null and instance set to null
            // so that the resource won't be enlisted or registered
            protected InvocationArray<ComponentInvocation> childValue(InvocationArray<ComponentInvocation> parentValue) {
                // always creates a new ArrayList
                InvocationArray<ComponentInvocation> result = new InvocationArray<ComponentInvocation>();
                InvocationArray<ComponentInvocation> v = parentValue;
                if (v.size() > 0 && v.outsideStartup()) {
                    // get current invocation
                    ComponentInvocation parentInv = v.get(v.size() - 1);
                    /*
                    TODO: The following is ugly. The logic of what needs to be in the
                      new ComponentInvocation should be with the respective container
                    */
                    if (parentInv.getInvocationType() == ComponentInvocationType.SERVLET_INVOCATION) {

                        ComponentInvocation inv = new ComponentInvocation();
                        inv.componentId = parentInv.getComponentId();
                        inv.setComponentInvocationType(parentInv.getInvocationType());
                        inv.instance = null;
                        inv.container = parentInv.getContainerContext();
                        inv.transaction = null;
                        result.add(inv);
                    } else if (parentInv.getInvocationType() != ComponentInvocationType.EJB_INVOCATION) {
                        // Push a copy of invocation onto the new result
                        // ArrayList
                        ComponentInvocation cpy = new ComponentInvocation();
                        cpy.componentId = parentInv.getComponentId();
                        cpy.setComponentInvocationType(parentInv.getInvocationType());
                        cpy.instance = parentInv.getInstance();
                        cpy.container = parentInv.getContainerContext();
                        cpy.transaction = parentInv.getTransaction();
                        result.add(cpy);
                    }

                }
                return result;
            }
        };
    }

    public <T extends ComponentInvocation> void preInvoke(T inv)
            throws InvocationException {

        InvocationArray<ComponentInvocation> v = frames.get();
        if (inv.getInvocationType() == ComponentInvocationType.SERVICE_STARTUP) {
            v.setInvocationAttribute(ComponentInvocationType.SERVICE_STARTUP);
            return;
        }

        int beforeSize = v.size();
        ComponentInvocation prevInv = beforeSize > 0 ? v.get(beforeSize - 1) : null;

        // if ejb call EJBSecurityManager, for servlet call RealmAdapter
        ComponentInvocationType invType = inv.getInvocationType();

        if (invHandlers != null) {
            for (ComponentInvocationHandler handler : invHandlers) {
                handler.beforePreInvoke(invType, prevInv, inv);
            }
        }
        
       
        List<RegisteredComponentInvocationHandler> setCIH = regCompInvHandlerMap.get(invType);
        if (setCIH != null) {
            for (int i=0;i<setCIH.size();i++) {
                setCIH.get(i).getComponentInvocationHandler().beforePreInvoke(invType, prevInv, inv);
            }
        }
  

        //push this invocation on the stack
        v.add(inv);

        if (invHandlers != null) {
            for (ComponentInvocationHandler handler : invHandlers) {
                handler.afterPreInvoke(invType, prevInv, inv);
            }
        }
        
        if (setCIH != null) {
            for (int i=0;i<setCIH.size();i++) {
                setCIH.get(i).getComponentInvocationHandler().afterPreInvoke(invType, prevInv, inv);
            }            
        }

    }

    public <T extends ComponentInvocation> void postInvoke(T inv)
            throws InvocationException {

        // Get this thread's ArrayList
        InvocationArray<ComponentInvocation> v = frames.get();
        if (inv.getInvocationType() == ComponentInvocationType.SERVICE_STARTUP) {
            v.setInvocationAttribute(ComponentInvocationType.UN_INITIALIZED);
            return;
        }

        int beforeSize = v.size();
        if (beforeSize == 0) {
            throw new InvocationException();
        }

        ComponentInvocation prevInv = beforeSize > 1 ? v.get(beforeSize - 2) : null;
        ComponentInvocation curInv = v.get(beforeSize - 1);

        try {
            ComponentInvocationType invType = inv.getInvocationType();

            if (invHandlers != null) {
                for (ComponentInvocationHandler handler : invHandlers) {
                    handler.beforePostInvoke(invType, prevInv, curInv);
                }
            }

            List<RegisteredComponentInvocationHandler> setCIH = regCompInvHandlerMap.get(invType);
            if (setCIH != null) {
                for (int i=0;i<setCIH.size();i++) {
                    setCIH.get(i).getComponentInvocationHandler().beforePostInvoke(invType, prevInv, curInv);
                }
            }
              
            

        } finally {
            // pop the stack
            v.remove(beforeSize - 1);

            if (invHandlers != null) {
                for (ComponentInvocationHandler handler : invHandlers) {
                    handler.afterPostInvoke(inv.getInvocationType(), prevInv, inv);
                }
            }
            
            ComponentInvocationType invType = inv.getInvocationType();
            

            List<RegisteredComponentInvocationHandler> setCIH = regCompInvHandlerMap.get(invType);
            if (setCIH != null) {
                for (int i=0;i<setCIH.size();i++) {
                    setCIH.get(i).getComponentInvocationHandler().afterPostInvoke(invType, prevInv, curInv);
                }
            }

        }

    }

    /**
     * return true iff no invocations on the stack for this thread
     */
    public boolean isInvocationStackEmpty() {
        ArrayList v = frames.get();
        return ((v == null) || (v.size() == 0));
    }

    /**
     * return the Invocation object of the component
     * being called
     */
    public <T extends ComponentInvocation> T getCurrentInvocation() {
        ArrayList v = (ArrayList) frames.get();
        int size = v.size();
        if (size == 0) {
            return null;
        }
        return (T) v.get(size - 1);
    }

    /**
     * return the Inovcation object of the caller
     * return null if none exist (e.g. caller is from
     * another VM)
     */
    public <T extends ComponentInvocation> T getPreviousInvocation()
            throws InvocationException {

        ArrayList v = frames.get();
        int i = v.size();
        if (i < 2) return null;
        return (T) v.get(i - 2);
    }

    public List getAllInvocations() {
        return frames.get();
    }

    static class InvocationArray<T extends ComponentInvocation> extends java.util.ArrayList<T> {
        private ComponentInvocationType invocationAttribute;

        public void setInvocationAttribute(ComponentInvocationType attribute) {
            this.invocationAttribute = attribute;
        }

        public ComponentInvocationType getInvocationAttribute() {
            return invocationAttribute;
        }

        public boolean outsideStartup() {
            return getInvocationAttribute()
                    != ComponentInvocationType.SERVICE_STARTUP;
        }
    }

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
        Stack<ApplicationEnvironment> stack = applicationEnvironments.get();
        
        stack.push(env);
    }
    
    @Override
    public ApplicationEnvironment peekAppEnvironment() {
        Stack<ApplicationEnvironment> stack = applicationEnvironments.get();
        
        if (stack.isEmpty()) return null;
        
        return stack.peek();
    }

    @Override
    public void popAppEnvironment() {
        Stack<ApplicationEnvironment> stack = applicationEnvironments.get();
        
        if (!stack.isEmpty()) stack.pop();
    }
}






