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

package com.sun.enterprise.tools.verifier.tests.ejb.intf;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.lang.reflect.Method;
import java.util.logging.Level;

/** 
 * Local/remote interface/business matching methods exceptions test.  
 * Verify the following:
 * 
 *   For each method defined in the local/remote interface, there must be a matching 
 *   method in the enterprise Bean's class. The matching method must have: 
 *   . All the exceptions defined in the throws clause of the matching method 
 *     of the enterprise Bean class must be defined in the throws clause of 
 *     the method of the local/remote interface. 
 */
public abstract class InterfaceMatchMethodException extends InterfaceMethodTest { 
    /**
     * <p>
     * run an individual verifier test against a declared method of the 
     * local/remote interface.
     * </p>
     * 
     * @param descriptor the deployment descriptor for the bean
     * @param method the method to run the test on
     * @return true if the test passes
     */
    
    protected boolean runIndividualMethodTest(EjbDescriptor descriptor, Method method, Result result) {
        
        boolean businessMethodFound, exceptionsMatch;
        ComponentNameConstructor compName = null;
        
        try {	  
            compName = getVerifierContext().getComponentNameConstructor();
            // retrieve the EJB Class Methods
            ClassLoader jcl = getVerifierContext().getClassLoader();            
            Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, jcl);
            Class[] methodExceptionTypes = method.getExceptionTypes();
            
            // start do while loop here....
            do {
                Method [] businessMethods = EJBClass.getDeclaredMethods();
                
                // try and find the business method in the EJB Class Methods
                businessMethodFound = false;
                exceptionsMatch = false;
                for (Method businessMethod : businessMethods) {
                    if (method.getName().equals(businessMethod.getName())) {
                        businessMethodFound = true;
                        // check the rest of the exceptions
                        Class[] businessMethodExceptionTypes = businessMethod.getExceptionTypes();
                        if (RmiIIOPUtils.isEjbFindMethodExceptionsSubsetOfFindMethodExceptions(businessMethodExceptionTypes,methodExceptionTypes)) {
                            exceptionsMatch = true;
                            break;
                        } // if the bean & local interface method exceptions match
                    } else {
                        continue;
                    }
                }  // for all the bean class business methods
                
                // now display the appropriate results for this particular business
                // method
                if (businessMethodFound && exceptionsMatch) {
                    addGoodDetails(result, compName);
                    result.addGoodDetails(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "The corresponding business method with matching " +
                            "exceptions was found."));
                    return true;
                } else if (businessMethodFound && !exceptionsMatch) {                    
                    logger.log(Level.FINE, getClass().getName() + ".debug1",
                            new Object[] {method.getDeclaringClass().getName(),method.getName()});
                    logger.log(Level.FINE, getClass().getName() + ".debug3",
                            new Object[] {method.getName()});
                    logger.log(Level.FINE, getClass().getName() + ".debug2");
                }
                
            } while (((EJBClass = EJBClass.getSuperclass()) != null) && (!(businessMethodFound && exceptionsMatch)));
            
            
            if (!exceptionsMatch) {
                addErrorDetails(result, compName);
                result.addErrorDetails(smh.getLocalString
                        (getClass().getName() + ".failed",
                                "Error: No corresponding business method with matching exceptions was found for method [ {0} ].",
                                new Object[] {method.getName()}));
            }
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                    "Error: "+ getInterfaceType() +"interface [ {0} ] does not " +
                    "exist or is not loadable within bean [ {1} ]",
                    new Object[] {getClassName(descriptor),descriptor.getName()}));
        }
        return false;
    }
    
    private String getClassName(EjbDescriptor descriptor) {
        return getInterfaceName(descriptor);
    }
}
