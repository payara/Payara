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
import com.sun.enterprise.tools.verifier.tests.ejb.EjbUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;
import java.util.Set;

/**  
 * Enterprise Bean's business(...) methods exceptions test.
 * Each enterprise Bean class must define zero or more business(...) methods. 
 * The method signatures must follow these rules: 
 * 
 * Compatibility Note: EJB 1.0 allowed the business methods to throw the 
 * java.rmi.RemoteException to indicate a non-application exception. This 
 * practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise bean 
 * should throw the javax.ejb.EJBException or another RuntimeException to 
 * indicate non-application exceptions to the Container (see Section 12.2.2). 
 * Note: Treat as a warning to user in this instance.
 */
public class BusinessMethodException extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    
    /** 
     * Enterprise Bean's business(...) methods exceptions test.
     * Each enterprise Bean class must define zero or more business(...) methods. 
     * The method signatures must follow these rules: 
     * 
     * Compatibility Note: EJB 1.0 allowed the business methods to throw the 
     * java.rmi.RemoteException to indicate a non-application exception. This 
     * practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise bean 
     * should throw the javax.ejb.EJBException or another RuntimeException to 
     * indicate non-application exceptions to the Container (see Section 12.2.2). 
     * Note: Treat as a warning to user in this instance.
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        
        if ((descriptor instanceof EjbSessionDescriptor)  ||
                (descriptor instanceof EjbEntityDescriptor)) {
            if(descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName())) 
                commonToBothInterfaces(descriptor.getRemoteClassName(),descriptor);
            
            if(descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName())) 
                commonToBothInterfaces(descriptor.getLocalClassName(),descriptor);
            
            Set<String> localAndRemoteInterfaces = descriptor.getLocalBusinessClassNames();
            localAndRemoteInterfaces.addAll(descriptor.getRemoteBusinessClassNames());
            
            for (String localOrRemoteIntf : localAndRemoteInterfaces) 
                commonToBothInterfaces(localOrRemoteIntf, descriptor);
        }
        
        if(result.getStatus() != Result.FAILED && 
                result.getStatus() != Result.WARNING) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                                    (getClass().getName() + ".passed",
                                    "Proper declaration of business method(s) found."));
        }
        return result;
    }
    
    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param intf or component for the Remote/Local interface of the Ejb. 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     */
    
    
    private void commonToBothInterfaces(String intf, EjbDescriptor descriptor) {
        try {
            Class intfClass = Class.forName(intf, 
                                            false, 
                                            getVerifierContext().getClassLoader());
            
            for (Method remoteMethod : intfClass.getMethods()) {
                // we don't test the EJB methods
                if (remoteMethod.getDeclaringClass().getName().equals("javax.ejb.EJBObject")||
                        remoteMethod.getDeclaringClass().getName().equals("javax.ejb.EJBLocalObject")) 
                    continue;
                
                Class beanClass = Class.forName(descriptor.getEjbClassName(), 
                                                false, 
                                                getVerifierContext().getClassLoader());
                
                for (Method method: beanClass.getMethods()) {
                    if (method.getName().equals(remoteMethod.getName())) {
                        
                    // Compatibility Note: EJB 1.0 allowed the business methods to throw
                    // the java.rmi.RemoteException to indicate a non-application 
                    // exception. This practice is deprecated in EJB 1.1---an EJB 1.1 
                    // compliant enterprise bean should throw the javax.ejb.EJBException
                    // or another RuntimeException to indicate non-application 
                    // exceptions to the Container (see Section 12.2.2). 
                    // Note: Treat as a warning to user in this instance 
                        Class [] exceptions = method.getExceptionTypes();
                        if (EjbUtils.isValidRemoteException(exceptions)) {
                            addWarningDetails(result, compName);
                            result.warning(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                    "Error: Compatibility Note:" +
                                    "\n A public business method [ {0} ] was found, but" +
                                    "\n EJB 1.0 allowed the business methods to throw the " +
                                    "\n java.rmi.RemoteException to indicate a non-application" +
                                    "\n exception. This practice is deprecated in EJB 1.1" +
                                    "\n ---an EJB 1.1 compliant enterprise bean should" +
                                    "\n throw the javax.ejb.EJBException or another " +
                                    "\n RuntimeException to indicate non-application exceptions" +
                                    "\n to the Container. ",
                                    new Object[] {method.getName()}));
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                            (getClass().getName() + ".failedException",
                            "Error: Remote interface [ {0} ] or bean class [ {1} ] " +
                            "does not exist or is not loadable within bean [ {2} ].",
                            new Object[] {intf,descriptor.getEjbClassName(),descriptor.getName()}));
        }  
    }
}
