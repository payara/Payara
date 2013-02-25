/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers.interceptors;

import java.rmi.UnmarshalException;
import javax.ejb.AccessLocalException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;

import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

import com.sun.ejb.containers.InvocationHandlerUtil;

import java.util.Map;

import com.sun.enterprise.container.common.spi.InterceptorInvoker;

/** 
 *
 * @author Kenneth Saks
 */    

public final class InterceptorInvocationHandler  
    implements InvocationHandler, InterceptorInvoker {

    // The actual instance of the application class
    private Object targetInstance;

    // The object held by the application
    private Object clientProxy;

    private Object[] interceptorInstances;
    private InterceptorManager interceptorManager;

    private static Object[] emptyArray = new Object[] {};


    public void init(Object targetInstance, Object[] interceptorInstances,
                     Object clientProxy, InterceptorManager manager)
                     {
        this.targetInstance = targetInstance;
        this.interceptorInstances = interceptorInstances;
        this.clientProxy = clientProxy;
        interceptorManager = manager;

    }

    public Object getProxy() {
        return clientProxy;
    }

    public Object getTargetInstance() {
        return targetInstance;
    }

    public Object[] getInterceptorInstances() {
        return interceptorInstances;
    }

    public void invokeAroundConstruct() throws Exception {

         invokeCallback(CallbackType.AROUND_CONSTRUCT);
         targetInstance = interceptorManager.getTargetInstance();

    }

    public void invokePostConstruct() throws Exception {

         invokeCallback(CallbackType.POST_CONSTRUCT);

    }

    public void invokePreDestroy() throws Exception {

         invokeCallback(CallbackType.PRE_DESTROY);
    }

    private void invokeCallback(CallbackType type) throws Exception {

        try {
            interceptorManager.intercept(type, targetInstance,
                    interceptorInstances);
        } catch(Exception e) {
            throw e;
        } catch(Throwable t) {
            throw new Exception(t);
        }

    }


    public Object invoke(Object proxy, Method method, Object[] args) 
        throws Throwable {


        Class methodClass = method.getDeclaringClass();
        if( methodClass == java.lang.Object.class )  {
            return InvocationHandlerUtil.
                invokeJavaObjectMethod(this, method, args);    
        }

        Object returnValue = null;
      
        try {


            Method beanClassMethod = targetInstance.getClass().getMethod
                (method.getName(), method.getParameterTypes());

            InterceptorManager.InterceptorChain chain =
                    interceptorManager.getAroundInvokeChain(null, beanClassMethod);

            Object[] theArgs = (args == null) ? emptyArray : args;

            // Create context for around invoke invocation.  Make sure method set on
            // InvocationContext is from bean class.
            AroundInvokeInvocationContext invContext =
                    new AroundInvokeInvocationContext(targetInstance, interceptorInstances, chain,
                            beanClassMethod, theArgs );

            returnValue = interceptorManager.intercept(chain, invContext);

        } catch(NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch(InvocationTargetException ite) {
            throw ite.getCause();
        }

        return returnValue;

    }

    @Override
    public String toString() {
        return (targetInstance != null)? targetInstance.toString() : super.toString();
    }

}
