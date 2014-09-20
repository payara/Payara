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

package org.glassfish.webservices;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 * InvocationHandler used to delegate calls to JAXRPC servlet impls
 * that aren't subtypes of their associated Service Endpoint Interface.
 *
 * @author Kenneth Saks
 */
public class ServletImplInvocationHandler implements InvocationHandler {

    private static final Logger logger = LogUtils.getLogger();

    private Object servletImplDelegate;
    private Class servletImplClass;
    
    public ServletImplInvocationHandler(Object delegate) {
        servletImplDelegate = delegate;
        servletImplClass    = delegate.getClass();
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) 
        throws Throwable {

        // NOTE : be careful with "args" parameter.  It is null
        //        if method signature has 0 arguments.

        Class methodClass = method.getDeclaringClass();
        if( methodClass == java.lang.Object.class )  {
            return invokeJavaObjectMethod(this, method, args);
        }

        Object returnValue = null;

        try {
            // Since impl class isn't subtype of SEI, we need to do a 
            // method lookup to get method object to use for invocation.
            Method implMethod = servletImplClass.getMethod
                (method.getName(), method.getParameterTypes());
            returnValue = implMethod.invoke(servletImplDelegate, args);
        } catch(InvocationTargetException ite) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, ite);
            }
            throw ite.getCause();
        } catch(Throwable t) {
            logger.log(Level.INFO, LogUtils.ERROR_INVOKING_SERVLETIMPL, t);
            throw t;
        }

	return returnValue;
    }

    private Object invokeJavaObjectMethod(InvocationHandler handler, 
                                          Method method, Object[] args) 
        throws Throwable {

        Object returnValue = null;

        // Can only be one of : 
        //     boolean java.lang.Object.equals(Object)
        //     int     java.lang.Object.hashCode()
        //     String  java.lang.Object.toString()
        //
        // Optimize by comparing as few characters as possible.

        switch( method.getName().charAt(0) ) {
            case 'e' :
                Object other = Proxy.isProxyClass(args[0].getClass()) ?
                    Proxy.getInvocationHandler(args[0]) : args[0];
                returnValue = Boolean.valueOf(handler.equals(other));
                break;
            case 'h' :
                returnValue = Integer.valueOf(handler.hashCode());
                break;
            case 't' :
                returnValue = handler.toString();
                break;
            default :
                throw new Throwable("Object method " + method.getName() + 
                                    "not found");
        }

        return returnValue;
    }

}
