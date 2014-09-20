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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.ejbpostcreatemethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

/**  
 * Entity Bean's ejbPostCreate(...) methods test.
 * Each entity Bean class may define zero or more ejbPostCreate(...) methods. 
 * The number and signatures of a entity Bean's create methods are specific 
 * to each EJB class. The method signatures must follow these rules: 
 * 
 * The method name must be ejbPostCreate. 
 *
 * The methods arguments must be the same as the arguments of the 
 * matching ejbCreate(...) method. 
 * 
 */
public class EjbPostCreateMethodArgs extends EjbTest implements EjbCheck { 


    /** 
     * Entity Bean's ejbPostCreate(...) methods test.
     * Each entity Bean class may define zero or more ejbPostCreate(...) methods. 
     * The number and signatures of a entity Bean's create methods are specific 
     * to each EJB class. The method signatures must follow these rules: 
     * 
     * The method name must be ejbPostCreate. 
     *
     * The methods arguments must be the same as the arguments of the 
     * matching ejbCreate(...) method. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    boolean oneFailed = false;
	    int foundAtLeastOne = 0;
	    try {
		VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());

		Class [] ejbPostCreateMethodParameterTypes;
		Class [] ejbCreateMethodParameterTypes;
		boolean signaturesMatch = false;
		boolean ejbCreateExists = false;

		Method [] methods = c.getDeclaredMethods();
		Vector<Method> createMethodSuffix = new Vector<Method>();
		for (int i = 0; i < methods.length; i++) {
		    // The method name must start with create. 
		    if (methods[i].getName().startsWith("ejbCreate")) {
			createMethodSuffix.addElement((Method)methods[i]);
			ejbCreateExists = true;
		    }
		}
		
                // start do while loop here....
                do {
		    for (int i = 0; i < methods.length; i++) {
			// reset flags from last time thru loop
			signaturesMatch = false;
			
			// The method name must be ejbPostCreate. 
			if (methods[i].getName().startsWith("ejbPostCreate")) {
			    foundAtLeastOne++;

			    String matchSuffix = methods[i].getName().substring(13);
			    for (int k = 0; k < createMethodSuffix.size(); k++) {
				if (matchSuffix.equals(((Method)createMethodSuffix.elementAt(k)).getName().substring(9))) {
				    ejbCreateMethodParameterTypes = ((Method)createMethodSuffix.elementAt(k)).getParameterTypes();
				    ejbPostCreateMethodParameterTypes = methods[i].getParameterTypes();
				    if (Arrays.equals(ejbCreateMethodParameterTypes,ejbPostCreateMethodParameterTypes)) {
					signaturesMatch = true;
					break;
				    }
				}
			    }
			    if (signaturesMatch) {
				result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				result.addGoodDetails(smh.getLocalString
						      (getClass().getName() + ".debug1",
						       "For EJB Class [ {0} ] method [ {1} ]",
						       new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
				result.addGoodDetails(smh.getLocalString
						      (getClass().getName() + ".passed",
						       "[ {0} ] declares [ {1} ] method with parameters that match corresponding [ {2} ] method.",
						       new Object[] {descriptor.getEjbClassName(),methods[i].getName(), "ejbCreate<method>"}));
			    } else if (!signaturesMatch) {
				oneFailed = true;
				result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				result.addErrorDetails(smh.getLocalString
						       (getClass().getName() + ".debug1",
							"For EJB Class [ {0} ] method [ {1} ]",
							new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
				result.addErrorDetails(smh.getLocalString
						       (getClass().getName() + ".failed",
							"Error: An [ {0} ] method was found, but [ {1} ] method parameters did not match any corresponding [ {2} ] method parameters.",
							new Object[] {methods[i].getName(),methods[i].getName(),"ejbCreate<method>"}));
				break;
			    } 
			}
		    }
		    if (oneFailed == true)
			break;
                } while (((c = c.getSuperclass()) != null) && (foundAtLeastOne == 0));
        
		if (ejbCreateExists == false) {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  "[ {0} ] does not declare any ejbPostCreate(...) methods.",
					  new Object[] {descriptor.getEjbClassName()}));
		}
		if (foundAtLeastOne == 0 && ejbCreateExists == true){
		    oneFailed=true;
		    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failedException1",
				   "Error: ejbPostCreate<Method> method corresponding to the ejbCreate<Method> method does not exist!",
				   new Object[] {}));
		    
		}
	    } catch (ClassNotFoundException e) {
		Verifier.debug(e);
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException",
			       "Error: [ {0} ] class not found.",
			       new Object[] {descriptor.getEjbClassName()}));
		oneFailed = true;
	    }  

	    if (oneFailed) {
		result.setStatus(result.FAILED);
            } else if (foundAtLeastOne == 0) {
                result.setStatus(result.NOT_APPLICABLE);
	    } else {
		result.setStatus(result.PASSED);
	    }

	    return result;
 
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} bean, but called with {2} bean.",
				  new Object[] {getClass(),"Entity","Session"}));
	    return result;
	}
    }
}
