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

package com.sun.enterprise.tools.verifier.tests.ejb.homeintf;

import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/**  
 * Enterprise Bean's ejbHome methods exceptions test.
 * Each enterprise Bean class may define zero or more business(...) methods. 
 * The method signatures must follow these rules: 
 * 
 * Compatibility Note: EJB 1.0 allowed the ejbHome methods to throw the 
 * java.rmi.RemoteException to indicate a non-application exception. This 
 * practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise bean 
 * should throw the javax.ejb.EJBException or another RuntimeException to 
 * indicate non-application exceptions to the Container (see Section 12.2.2). 
 * Note: Treat as a warning to user in this instance.
 */
public class HomeMethodException extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    
    /**  
     * Enterprise Bean's ejbHome methods exceptions test.
     * Each enterprise Bean class may define zero or more business(...) methods. 
     * The method signatures must follow these rules: 
     * 
     * Compatibility Note: EJB 1.0 allowed the ejbHome methods to throw the 
     * java.rmi.RemoteException to indicate a non-application exception. This 
     * practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise bean 
     * should throw the javax.ejb.EJBException or another RuntimeException to 
     * indicate non-application exceptions to the Container (see Section 12.2.2). 
     * Note: Treat as a warning to user in this instance.
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        
        if ((descriptor instanceof EjbSessionDescriptor)  ||
                (descriptor instanceof EjbEntityDescriptor)) {
            if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName())) {
                commonToBothInterfaces(descriptor.getHomeClassName(),descriptor);
            }
            if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName())) {
                commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor);
            }
            
        }
        if(result.getStatus()!=Result.FAILED && 
                result.getStatus() != Result.WARNING) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Valid Home method(s)."));
        }
        return result;
    }
    
    /** 
     * This method is responsible for the logic of the test. It is called for 
     * both local and remote home interfaces.
     * 
     * @param home for the Home Interface of the Ejb
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * This parameter may be optional depending on the test 
     */
    
    
    private void commonToBothInterfaces(String home,EjbDescriptor descriptor) {
        try {
            ClassLoader jcl = getVerifierContext().getClassLoader();
            Class rc = Class.forName(home, false, jcl);
            
            for (Method homeMethod : rc.getMethods()) {
                
                if (homeMethod.getDeclaringClass().getName().equals("javax.ejb.EJBHome")||
                        homeMethod.getDeclaringClass().getName().equals("javax.ejb.EJBLocalHome")) 
                    continue;
                if (homeMethod.getName().startsWith("create") || 
                        homeMethod.getName().startsWith("find") || 
                        homeMethod.getName().startsWith("remove")) 
                    continue;
                
                Class beanClass = Class.forName(descriptor.getEjbClassName(), false, jcl);
                
                for (Method method : beanClass.getMethods()) {
                    
                    String methodName = "ejbHome" + 
                            Character.toUpperCase(homeMethod.getName().charAt(0)) + 
                            homeMethod.getName().substring(1);
                    
                    if (method.getName().equals(methodName)) {
                        
                        // Compatibility Note: EJB 1.0 allowed the business methods to throw
                        // the java.rmi.RemoteException to indicate a non-application 
                        // exception. This practice is deprecated in EJB 1.1---an EJB 1.1 
                        // compliant enterprise bean should throw the javax.ejb.EJBException
                        // or another RuntimeException to indicate non-application 
                        // exceptions to the Container (see Section 12.2.2). 
                        // Note: Treat as a warning to user in this instance 
                        Class [] exceptions = method.getExceptionTypes();
                        if(EjbUtils.isValidRemoteException(exceptions)) {
                            addWarningDetails(result, compName);
                            result.warning(smh.getLocalString
                                    (getClass().getName() + ".warning",
                                    "Error: Compatibility Note:" +
                                    "\n A public Home method [ {0} ] was found, but" +
                                    "\n EJB 1.0 allowed the 'ejbHome' methods to throw the " +
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
                    "Error: Remote interface [ {0} ] or bean class [ {1} ] does " +
                    "not exist or is not loadable within bean [ {2} ].",
                    new Object[] {home,descriptor.getEjbClassName(),descriptor.getName()}));
        }  
    }
}
