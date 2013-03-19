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
import java.lang.reflect.InvocationTargetException;
import javax.interceptor.InvocationContext;


import java.util.Map;
import java.util.HashMap;


/**
 * Concrete InvocationContext implementation passed to callback methods 
 * defined in interceptor classes.
 */
public class AroundInvokeInvocationContext extends CallbackInvocationContext
    implements InterceptorManager.AroundInvokeContext {

    private Method method;
    private int interceptorIndex = 0;
    private InterceptorManager.InterceptorChain chain;
    private Object[] parameters;


    public AroundInvokeInvocationContext(Object targetObjectInstance,
                                     Object[] interceptorInstances,
                                     InterceptorManager.InterceptorChain chain,
                                     Method m,
                                     Object[] params
                                     ) {
        super(targetObjectInstance, interceptorInstances, null);
        method = m;
        this.chain = chain;
        parameters = params;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object proceed()
        throws Exception
    {
        try {
            interceptorIndex++;
            return chain.invokeNext(interceptorIndex, this);
        } catch (Exception ex) {
            throw ex;
        } catch (Throwable th) {
            throw new Exception(th);
        } finally {
            interceptorIndex--;
        }
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) {
        InterceptorUtil.checkSetParameters(params, getMethod());
        parameters = params;

    }


    /**
      * Called from Interceptor Chain to invoke the actual bean method.
      * This method must throw any exception from the bean method *as is*,
      * without being wrapped in an InvocationTargetException.  The exception
      * thrown from this method will be propagated through the application's
      * interceptor code, so it must not be changed in order for any exception
      * handling logic in that code to function properly.
      */
    public  Object invokeBeanMethod() throws Throwable {

        try {

            return method.invoke(getTarget(), parameters);

        } catch(InvocationTargetException ite) {
            throw ite.getCause();
        }
        
    }



}

