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
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/**  
 * Enterprise Bean's home methods argument RMI IIOP test.
 * Each enterprise Bean class must define zero or more home methods. 
 * The method signatures must follow these rules: 
 * 
 * The methods return value must be legal types for RMI-IIOP. 
 */
public class HomeMethodRmiIIOPReturn extends EjbTest implements EjbCheck { 



    /** 
     * Enterprise Bean's home methods argument RMI IIOP test.
     * Each enterprise Bean class must define zero or more home methods. 
     * The method signatures must follow these rules: 
     * 
     * The methods return value must be legal types for RMI-IIOP. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if ((descriptor instanceof EjbSessionDescriptor)  ||
	    (descriptor instanceof EjbEntityDescriptor)) {
	    boolean oneFailed = false;
	    int foundAtLeastOne = 0;
	    try {
		if(descriptor.getHomeClassName() == null || "".equals(descriptor.getHomeClassName())) {
		    addNaDetails(result, compName);
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  " [ {0} ] does not have a remote home interface. ",
					  new Object[] {descriptor.getEjbClassName()}));
		    return result;
		}
		ClassLoader jcl = getVerifierContext().getClassLoader();
		Class rc = Class.forName(descriptor.getHomeClassName(), false, jcl);

		Class methodReturnType;
		boolean homeMethodFound = false;
		boolean isLegalRMIIIOPReturn = false;
		for (Method remoteMethod : rc.getMethods()) {

                    // we don't test the EJB methods
                    if (remoteMethod.getDeclaringClass().getName().equals("javax.ejb.EJBHome")) 
                        continue;
		    if (remoteMethod.getName().startsWith("create") || 
			remoteMethod.getName().startsWith("find") || 
			remoteMethod.getName().startsWith("remove")) 
			continue;
                                        
		    // reset flags from last time thru loop
		    Class c = Class.forName(descriptor.getEjbClassName(), false, jcl);
		    // start do while loop here....
		    do {
			
			for (Method method : c.getDeclaredMethods()) {

			    // reset flags from last time thru loop
			    homeMethodFound = false;
			    isLegalRMIIIOPReturn = false;
			    String methodName = "ejbHome" + Character.toUpperCase(remoteMethod.getName().charAt(0)) + remoteMethod.getName().substring(1);
			    if (method.getName().equals(methodName)) {
				foundAtLeastOne++;
				homeMethodFound = true;
				// The methods arguments types must be legal types for
				// RMI-IIOP.  This means that their return values must
				// be of valid types for RMI-IIOP,
				methodReturnType = method.getReturnType();
				if (RmiIIOPUtils.isValidRmiIIOPReturnType(methodReturnType)) {
				    // this is the right return type for method
				    isLegalRMIIIOPReturn = true;
				} // return valid
			
				// now display the appropriate results for this particular business
				// method
				if (homeMethodFound && isLegalRMIIIOPReturn ) {
				    addGoodDetails(result, compName);
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".passed",
							   "[ {0} ] properly declares ejbHome<METHOD> method [ {1} ] with valid RMI-IIOP return type.",
							   new Object[] {descriptor.getEjbClassName(),method.getName()}));
				} else if (homeMethodFound && !isLegalRMIIIOPReturn) {
				    oneFailed = true;
				    addErrorDetails(result, compName);
				    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".failed",
							    "Error: ejbHome<METHOD> method [ {0} ] was found, but ejbHome<METHOD> method has illegal return value.   ejbHome<METHOD> methods return type must be legal types for RMI-IIOP.",
							    new Object[] {method.getName()}));
				} 
			    }
			}
			if (oneFailed == true)
			    break;
		    } while (((c = c.getSuperclass()) != null) && (!homeMethodFound));
		}
		if (foundAtLeastOne == 0) {
		    addNaDetails(result, compName);
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  " [ {0} ] does not declare any ejbHome<METHOD> methods. ",
					  new Object[] {descriptor.getEjbClassName()}));
		}
	    } catch (ClassNotFoundException e) {
		Verifier.debug(e);
		oneFailed = true;
		addErrorDetails(result, compName);
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException",
			       "Error: Remote interface [ {0} ] or bean class [ {1} ] does not exist or is not loadable within bean [ {2} ].",
			       new Object[] {descriptor.getRemoteClassName(),descriptor.getEjbClassName(),descriptor.getName()}));
	    }  

	    if (oneFailed) {
		result.setStatus(Result.FAILED);
            } else if (foundAtLeastOne == 0) {
                result.setStatus(Result.NOT_APPLICABLE);
	    } else {
		result.setStatus(Result.PASSED);
	    }

	    return result;
 
	} else {
	    addNaDetails(result, compName);
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "{0} expected {1} bean or {2} bean, but called with {3}.",
				  new Object[] {getClass(),"Session","Entity",descriptor.getName()}));
	    return result;
	}
    }
}
