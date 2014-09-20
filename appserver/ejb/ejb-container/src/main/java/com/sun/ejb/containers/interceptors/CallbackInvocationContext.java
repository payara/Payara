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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.interceptor.InvocationContext;

import com.sun.ejb.containers.EJBContextImpl;
import com.sun.ejb.containers.BaseContainer;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.container.common.spi.util.InterceptorInfo;

import java.util.Map;
import java.util.HashMap;


/**
 * Concrete InvocationContext implementation passed to callback methods 
 * defined in interceptor classes.
 */
public class CallbackInvocationContext implements InvocationContext {


    private Map contextData;
    private int callbackIndex = 0;
    private CallbackChainImpl callbackChain;
    private Object[] interceptorInstances;
    private Object targetObjectInstance;
    private CallbackType eventType;
    Method method;

    // For AroundConstruct callback
    private Class targetObjectClass;
    private Constructor<?> ctor = null;
    private Class[] ctorParamTypes = null;
    private Object[] ctorParams = null;
    private BaseContainer container = null;
    private EJBContextImpl ctx = null;
    private InterceptorInfo interceptorInfo = null;

    public CallbackInvocationContext(Object targetObjectInstance,
                                     Object[] interceptorInstances,
                                     CallbackChainImpl chain) {
        this.targetObjectInstance = targetObjectInstance;
        this.interceptorInstances = interceptorInstances;
        callbackChain = chain;
    }

    public CallbackInvocationContext(Object targetObjectInstance,
                                     Object[] interceptorInstances,
                                     CallbackChainImpl chain,
                                     CallbackType eventType) {
        this(targetObjectInstance, interceptorInstances, chain);

        this.eventType = eventType;
    }

    /**
     * AroundConstruct
     */
    public CallbackInvocationContext(Class targetObjectClass,
                                     Object[] interceptorInstances,
                                     CallbackChainImpl chain,
                                     CallbackType eventType,
                                     InterceptorInfo interceptorInfo) {
        this(null, interceptorInstances, chain, eventType);

        this.targetObjectClass = targetObjectClass;

        Constructor<?>[] ctors = targetObjectClass.getConstructors();
        for(Constructor<?> ctor0 : ctors) {
            ctor = ctor0;
            if(ctor0.getParameterTypes().length == 0) {
                // We are looking for a no-arg constructor
                break;
            }
        }

        ctorParamTypes = ctor.getParameterTypes();
        ctorParams = new Object[ctorParamTypes.length]; 

        this.interceptorInfo = interceptorInfo;
    }

    /**
     * AroundConstruct
     */
    public CallbackInvocationContext(Class targetObjectClass,
                                     Object[] interceptorInstances,
                                     CallbackChainImpl chain,
                                     CallbackType eventType,
                                     BaseContainer container,
                                     EJBContextImpl ctx) {
        this(targetObjectClass, interceptorInstances, chain, eventType, null);

        this.container = container;
        this.ctx = ctx;
    }

    // InvocationContext methods

    @Override
    public Object getTarget() {
        return targetObjectInstance;
    }

    public Object[] getInterceptorInstances() {
        return interceptorInstances;
    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Constructor<?> getConstructor() {
        if (eventType == CallbackType.AROUND_CONSTRUCT) {
            return ctor;
        }
        return null;
    }

    @Override
    public Method getMethod() {
        if (eventType == CallbackType.AROUND_CONSTRUCT) {
            return null;
        }
        return method;
    }

    
    @Override
    public Object[] getParameters() {
        if (eventType == CallbackType.AROUND_CONSTRUCT) {
            return ctorParams;
        } else {
            throw new IllegalStateException("not applicable to Callback methods");
        }
    }

    @Override
    public void setParameters(Object[] params) {
        if (eventType == CallbackType.AROUND_CONSTRUCT) {
            checkSetParameters(params);
            ctorParams = params;
        } else {
            throw new IllegalStateException("not applicable to Callback methods");
        }
    }


    @Override
    public Map<String, Object> getContextData() {
        if( contextData == null ) {
            contextData = new HashMap();
        }

        return contextData;
    }
    
    @Override
    public Object proceed() throws Exception {
        try {
            callbackIndex++;
            return callbackChain.invokeNext(callbackIndex, this);
        } catch (Exception ex) {
            throw ex;
        } catch (Throwable th) {
            throw new Exception(th);
        }
    }

    /**
      * Called from Interceptor Chain to create the bean instance.
      */
    public void invokeSpecial() throws Throwable {
        if (eventType == CallbackType.AROUND_CONSTRUCT) {
            if (container == null) {
                targetObjectInstance = targetObjectClass.newInstance();
                interceptorInfo.setTargetObjectInstance(targetObjectInstance);
            } else {
                container.createEjbInstance(ctorParams, ctx);
                targetObjectInstance = ctx.getEJB();
            }
        } // else do nothing? XXX
    }

    private void checkSetParameters(Object[] params) {
       if( ctor != null) {

            if ((params == null) && (ctorParamTypes.length != 0)) {
                throw new IllegalArgumentException("Wrong number of parameters for "
                        + " constructor: " + ctor);
            }
            if (ctorParamTypes.length != params.length) {
                throw new IllegalArgumentException("Wrong number of parameters for "
                        + " constructor: " + ctor);
            }
            int index = 0 ;
            for (Class type : ctorParamTypes) {
                if (params[index] == null) {
                    if (type.isPrimitive()) {
                        throw new IllegalArgumentException("Parameter type mismatch for constructor "
                                + ctor + ".  Attempt to set a null value for Arg["
                            + index + "]. Expected a value of type: " + type.getName());
                    }
                } else if (type.isPrimitive()) {
                    if (! InterceptorUtil.hasCompatiblePrimitiveWrapper(type, params[index].getClass())) {
                        throw new IllegalArgumentException("Parameter type mismatch for constructor "
                                + ctor + ".  Arg["
                            + index + "] type: " + params[index].getClass().getName()
                            + " is not compatible with the expected type: " + type.getName());
                    }
                } else if (! type.isAssignableFrom(params[index].getClass())) {
                    throw new IllegalArgumentException("Parameter type mismatch for constructor "
                            + ctor + ".  Arg["
                        + index + "] type: " + params[index].getClass().getName()
                        + " does not match the expected type: " + type.getName());
                }
                index++;
            }
        } else {
            throw new IllegalStateException("Internal Error: Got null constructor");
        }
    }
}

