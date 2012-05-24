/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.ejb.ejb30;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * The methods of the business interface may declare arbitrary application 
 * exceptions. However, the methods of the business interface should not throw 
 * the java.rmi.RemoteException, even if the interface is a remote business 
 * interface or the bean class is annotated WebService or the method as 
 * WebMethod.
 * The methods of the business interface may only throw the 
 * java.rmi.RemoteException if the interface extends java.rmi.Remote.
 * 
 * @author Vikas Awasthi
 */
public class BusinessInterfaceException extends EjbTest {

    public Result check(EjbDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        Set<String> localAndRemoteClassNames = descriptor.getLocalBusinessClassNames();
        localAndRemoteClassNames.addAll(descriptor.getRemoteBusinessClassNames());
        
        for (String localOrRemoteClass : localAndRemoteClassNames) 
            checkForRemoteException(localOrRemoteClass,result,compName);

        if(result.getStatus() != Result.WARNING) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Business interface(s) if any are valid."));
        }
        return result;
    }
    
    private void checkForRemoteException(String className, 
                                        Result result, 
                                        ComponentNameConstructor compName) {
        try {
            Class c = Class.forName(className, 
                                    false, 
                                    getVerifierContext().getClassLoader());
            // do not check further if the business interface extends java.rmi.Remote 
            if(java.rmi.Remote.class.isAssignableFrom(c))
                return;
            Method[] methods = c.getMethods();
            for (Method method : methods) {
                Class[] exceptions = method.getExceptionTypes();
                for (Class exception : exceptions) {
                    if(java.rmi.RemoteException.class.isAssignableFrom(exception)) {
                        result.getFaultLocation().setFaultyClassAndMethod(method);
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString
                                        (getClass().getName() + ".warning",
                                        "java.rmi.RemoteException is thrown " +
                                        "in method [ {0} ] of business interface [ {1} ]",
                                        new Object[] {method.getName(), className}));
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                            (getClass().getName() + ".failed1",
                            "[ {0} ] not found.",
                            new Object[] {className}));
        }
    }
}
