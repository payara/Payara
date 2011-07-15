/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;

import javax.ejb.EJBException;

import com.sun.ejb.containers.InvocationHandlerUtil;

/** 
 * This is an invocation handler for a remote EJB 3.x business
 * interface.  It is used to convert invocations made on the
 * EJB 3.0 business interface to its corresponding RMI-IIOP 
 * object.
 *
 * @@@ Need to handle serialization/deserialization
 *
 * @author Kenneth Saks
 */    

public final class RemoteBusinessIntfInvocationHandler  
    implements InvocationHandler, Serializable {

    private Class businessInterface;
    private java.rmi.Remote delegate;

    public RemoteBusinessIntfInvocationHandler(Class businessIntf,
                                               java.rmi.Remote stub) {
        businessInterface = businessIntf;
        delegate = stub;
    }

    public Object invoke(Object proxy, Method method, Object[] args) 
        throws Throwable {

        // NOTE : be careful with "args" parameter.  It is null
        //        if method signature has 0 arguments.

        Class methodClass = method.getDeclaringClass();
        if( methodClass == java.lang.Object.class )  {
            return InvocationHandlerUtil.
                invokeJavaObjectMethod(this, method, args);    
        }

        Object returnValue = null;
        Throwable t = null;
        try {
            Method m = delegate.getClass().getMethod
                (method.getName(), method.getParameterTypes());
                                                     
            returnValue = m.invoke(delegate, args);
        } catch(NoSuchMethodException nsme) {
            t = nsme;
        } catch(InvocationTargetException ite) {
            t = ite.getCause();
        } catch(Throwable c) {
            t = c;
        }

        if( t != null ) {
            if( t instanceof java.rmi.RemoteException ) {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(t);
                throw ejbEx;
            } else {
                throw t;
            }
        }

        return returnValue;
    }
    
}
