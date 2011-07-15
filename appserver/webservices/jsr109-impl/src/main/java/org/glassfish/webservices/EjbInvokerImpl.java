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

package org.glassfish.webservices;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.Invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * This extends InvokerImpl - the difference is this creates
 * a Map of methods from class to proxy class
 */
public class EjbInvokerImpl extends InvokerImpl {
    
    private final HashMap<Method,Method> methodMap = new HashMap<Method,Method>();

    public EjbInvokerImpl(Class endpointImpl, Invoker core,
            Object inv, WebServiceContextImpl w) {
        super(core, inv, w);

        Class proxyClass = invokeObject.getClass();
        for(Method x : endpointImpl.getMethods()) {
            try {
                Method mappedMethod =
                    proxyClass.getMethod(x.getName(), x.getParameterTypes());
                methodMap.put(x, mappedMethod);
            } catch (NoSuchMethodException noex) {
                // We do not take any action because these may be excluded @WebMethods
                // or EJB business methods that are not @WebMethods etc
            }
        }
    }

    /**
     * Here is where we actually call the endpoint method
     */
    public Object invoke(Packet p, Method m, Object... args )
                                throws InvocationTargetException, IllegalAccessException {
        Method mappedMethod = methodMap.get(m);
        if(mappedMethod != null)
            return(super.invoke(p, mappedMethod,  args));
        throw new IllegalAccessException("Unable to find invocation method for "+m+". Map="+methodMap);
    }
}
