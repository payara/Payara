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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.lang.reflect.Method;

/** 
 * For each method defined in the home interface, there must be a matching
 * method in the enterprise Bean's class prefixed with ejbHome. 
 *
 * @author Jerome Dochez
 * @version
 */
abstract public class HomeMethodNameMatch extends HomeMethodTest {  
    
    /**
     * <p>
     * run an individual home method test 
     * </p>
     * 
     * @param method the home method to test
     * @param descriptor the deployment descriptor for the entity bean
     * @param result the result object
     */

    protected void runIndividualHomeMethodTest(Method method, EjbDescriptor descriptor, Result result) {
        
        Method m;
	ComponentNameConstructor compName = null;
	try {	  
	    compName = getVerifierContext().getComponentNameConstructor();
	    // retrieve the remote interface methods
	    ClassLoader jcl = getVerifierContext().getClassLoader();
	    Class ejbClass = Class.forName(descriptor.getEjbClassName(), false, jcl);
            // Bug: 4952890. first character of this name should be converted to UpperCase. 
            String methodName = method.getName().replaceFirst(method.getName().substring(0,1),
                                                              method.getName().substring(0,1).toUpperCase());
            String expectedMethodName = "ejbHome" + methodName;
            do {
                // retrieve the EJB Class Methods
                m = getMethod(ejbClass, expectedMethodName, method.getParameterTypes());                                    
            } while (((ejbClass = ejbClass.getSuperclass()) != null) && (m==null));

            if (m != null) {
                // now display the appropriate results for this particular business
                // method
                addGoodDetails(result, compName);
                result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "For method [ {1} ] in Home Interface [ {0} ], a ejbHome<METHOD> name matching method was found",
                    new Object[] {method.getDeclaringClass().getName(), method.getName()})); 
            } else {
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                    (getClass().getName() + ".failed",  
                    "Error : For method [ {1} ] defined in Home Interface [ {0} ], no ejbHome<METHOD> name matching method was found" ,
                    new Object[] {method.getDeclaringClass().getName(), method.getName()}));
	    }
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
        addErrorDetails(result, compName);
	    result.failed(smh.getLocalString(
			   getClass().getName() + ".failedException",
			   "Error: Home interface [ {0} ] does not exist or is not loadable within bean [ {1} ]",
			   new Object[] {getClassName(descriptor),descriptor.getName()}));
	}
    }

    private String getClassName(EjbDescriptor descriptor) {
	return getHomeInterfaceName(descriptor);
    } 
}
