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

import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;
import com.sun.enterprise.deployment.InterceptorDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import java.io.Serializable;

/** 
 *
 * @author Kenneth Saks
 */    

public class SystemInterceptorProxy
{
    // Won't actually be Serialized since it only applies to Stateless/Singleton

    public Object delegate;

    private Method aroundConstruct;
    private Method postConstruct;
    private Method preDestroy;
    private Method aroundInvoke;
    private Method aroundTimeout;


    public void setDelegate(Object d) {
             
        Class delegateClass = d.getClass();

        try {

           for(Method m : delegateClass.getDeclaredMethods() ) {

               if( m.getAnnotation(PostConstruct.class) != null ) {
                   postConstruct = m;
                   prepareMethod(m);
               } else if( m.getAnnotation(PreDestroy.class) != null ) {
                   preDestroy = m;
                   prepareMethod(m);
               } else if( m.getAnnotation(AroundInvoke.class) != null ) {
                   aroundInvoke = m;
                   prepareMethod(m);
               } else if( m.getAnnotation(AroundTimeout.class) != null ) {
                   aroundTimeout = m;
                   prepareMethod(m);                         
               } else if( m.getAnnotation(AroundConstruct.class) != null ) {
                   aroundConstruct = m;
                   prepareMethod(m);                         
               }
           }

        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }

         delegate = d;

    }

    private void prepareMethod(final Method m) throws Exception {

         java.security.AccessController
                        .doPrivileged(new java.security.PrivilegedExceptionAction() {
                    public java.lang.Object run() throws Exception {
                        if (!m.isAccessible()) {
                            m.setAccessible(true);
                        }
                        return null;
                    }});

    }

    @PostConstruct
    public Object init(InvocationContext ctx) throws Exception {
        return doCall(ctx, postConstruct);
    }

    @PreDestroy
    public Object destroy(InvocationContext ctx) throws Exception {
        return doCall(ctx, preDestroy);
    }

    @AroundConstruct
    public Object create(InvocationContext ctx) throws Exception {
        return doCall(ctx, aroundConstruct);
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception {
        return doCall(ctx, aroundInvoke);
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext ctx) throws Exception {
        return doCall(ctx, aroundTimeout);
    }

    private Object doCall(InvocationContext ctx, Method m) throws Exception {
        Object returnValue = null;

        if( (delegate != null) && (m != null) ) {
            try {
                returnValue = m.invoke(delegate, ctx);
            } catch(InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                
                throw new Exception(cause);
            }
        } else {
            returnValue = ctx.proceed();
        }

        return returnValue;

    }

    public static InterceptorDescriptor createInterceptorDesc() {

        InterceptorDescriptor interceptor = new InterceptorDescriptor();

        Class interceptorClass = SystemInterceptorProxy.class;
        String interceptorName = interceptorClass.getName();

        interceptor.setInterceptorClass(interceptorClass);

        {
               LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
               desc.setLifecycleCallbackClass(interceptorName);
               desc.setLifecycleCallbackMethod("create");
               interceptor.addCallbackDescriptor(CallbackType.AROUND_CONSTRUCT, desc);
        }

        {
               LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
               desc.setLifecycleCallbackClass(interceptorName);
               desc.setLifecycleCallbackMethod("init");
               interceptor.addCallbackDescriptor(CallbackType.POST_CONSTRUCT, desc);
        }

        {
               LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
               desc.setLifecycleCallbackClass(interceptorName);
               desc.setLifecycleCallbackMethod("destroy");
               interceptor.addCallbackDescriptor(CallbackType.PRE_DESTROY, desc);
        }
        
        {
               LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
               desc.setLifecycleCallbackClass(interceptorName);
               desc.setLifecycleCallbackMethod("aroundInvoke");
               interceptor.addAroundInvokeDescriptor(desc);
        }

        {
               LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
               desc.setLifecycleCallbackClass(interceptorName);
               desc.setLifecycleCallbackMethod("aroundTimeout");
               interceptor.addAroundTimeoutDescriptor(desc);
        }


        return interceptor;

    }


}
