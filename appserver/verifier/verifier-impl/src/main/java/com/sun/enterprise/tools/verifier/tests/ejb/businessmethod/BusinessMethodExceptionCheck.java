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

package com.sun.enterprise.tools.verifier.tests.ejb.businessmethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;

/**
 * Enterprise Bean's business(...) methods exceptions test.
 * Each enterprise Bean class must define zero or more business(...) methods.
 * The method signatures must follow these rules:
 * Compatibility Note:  EJB 2.1 specification states that All the  exceptions defined in the
 * throws clause of the matching method of the session bean  class must be  defined in the
 * throws clause  of  the method of the remote  interface. (see Section 7.11.5)
 * Note: Treat as a warning to user in this instance.
 */

public class BusinessMethodExceptionCheck extends EjbTest implements EjbCheck {
    Result result = null;
    ComponentNameConstructor compName = null;
    
    
    /**
     * Enterprise Bean's business(...) methods exceptions test.
     * Each enterprise Bean class must define zero or more business(...) methods.
     * The method signatures must follow these rules:
     * 
     * Compatibility Note:EJB 2.1 specification states that All the  exceptions defined in the
     * throws clause of the matching method of the session bean  class must be  defined in the
     * throws clause  of  the method of the remote  interface. (see Section 7.11.5)
     * Note: Treat as a warning to user in this instance.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    
    public Result check(EjbDescriptor descriptor) {
        
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        
        if (descriptor instanceof EjbSessionDescriptor) {
            if(descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName())) {
                commonToBothInterfaces(descriptor.getRemoteClassName(),descriptor);
            }
            if(descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName())) {
                commonToBothInterfaces(descriptor.getLocalClassName(),descriptor);
            }
            
        } 
        if(result.getStatus() != Result.FAILED &&
                result.getStatus() != Result.WARNING) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "All the exceptions defined in the throws clause of the matching "+
                    "business methods are defined in the throws clause of the method "+
                    "of the remote interface "));
        }
        return result;
    }
    
    
    /**
     * This method is responsible for the logic of the test. It is called for 
     * both local and remote interfaces.
     * @param intf the remote Interface of the Ejb
     * @param descriptor the Enterprise Java Bean deployment descriptor
     */
    
    
    
    private void commonToBothInterfaces(String intf,EjbDescriptor descriptor) {
        try {
            Class intfClass = Class.forName(intf, false, getVerifierContext().getClassLoader());
            Class beanClass = Class.forName(descriptor.getEjbClassName(), 
                                            false, 
                                            getVerifierContext().getClassLoader());
            for (Method remoteMethod : intfClass.getMethods()) {
                
                // we don't test the EJB methods,testing only business methods
                if (remoteMethod.getDeclaringClass().getName().equals("javax.ejb.EJBObject")||
                        remoteMethod.getDeclaringClass().getName().equals("javax.ejb.EJBLocalObject"))
                    continue;
                
                Class [] parameterTypes = remoteMethod.getParameterTypes();
                Method method =getMethod(beanClass,remoteMethod.getName(),parameterTypes);
                if(method == null)
                    continue;
                Class [] remoteExceptions = remoteMethod.getExceptionTypes();
                Class [] exceptions       = method.getExceptionTypes();
                
                /* EJB 2.1 specification has such statement in 7.11.5 Session  Bean's
                Remote Interface section:"All the  exceptions defined in the throws
                clause of the matching method of the session bean  class must be  defined
                in the throws clause  of  the method of the remote interface."
                */
                for (Class exception : exceptions) {
                    boolean foundOne = false;
                    for (Class remoteException : remoteExceptions) 
                        if(remoteException.getName().equals(exception.getName())) {
                            foundOne = true;
                            break;
                        }
                    if(!foundOne) {
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString
                                (getClass().getName() + ".warning",
                                "Not Compatible Exception: A public business " +
                                "method [ {0} ] was found, but according to " +
                                "the EJB specification, all the exceptions " +
                                "defined in the throws clause of the matching " +
                                "method of the session bean class must be " +
                                "defined in the throws clause of the method " +
                                "of the remote interface. Exception [ {1} ] " +
                                "is not defined in the bean's remote interface.",
                                new Object[] {method.getName(),exception.getName()}));
                    }
                }//end of for
            }//end of for
        } catch (Exception e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                            (getClass().getName() + ".failed",
                            "Remote interface [ {0} ] or bean class [ {1} ] does " +
                            "not exist or is not loadable within bean [ {2} ].",
                            new Object[] {intf,descriptor.getEjbClassName(),descriptor.getName()}));
        }//end of catch block
    }//End of CommonToBoth function
    
}

