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

package com.sun.enterprise.tools.verifier.tests.ejb.session.ejbcreatemethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;

/**  
 * Session Bean's ejbCreate(...) methods return test.
 * Each session Bean class must define one or more ejbCreate(...) methods. 
 * The number and signatures of a session Bean's create methods are specific 
 * to each EJB class. The method signatures must follow these rules: 
 * 
 * The method name must be ejbCreate. 
 *
 * The return type must be void. 
 * 
 */
public class EjbCreateMethodReturn extends EjbTest implements EjbCheck { 


    /**  
     * Session Bean's ejbCreate(...) methods return test.
     * Each session Bean class must define one or more ejbCreate(...) methods. 
     * The number and signature of a session Bean's create methods are specific 
     * to each EJB class. The method signatures must follow these rules: 
     * 
     * The method name must be ejbCreate. 
     *
     * The return type must be void. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbSessionDescriptor) {
	    boolean oneFailed = false;
	    try {
		VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());

		Class [] ejbCreateMethodParameterTypes;
		int foundAtLeastOne = 0;
		boolean ejbCreateFound = false;
		boolean returnsVoid = false;
                // start do while loop here....
                do {
		    Method [] methods = c.getDeclaredMethods();
		    for (int i = 0; i < methods.length; i++) {
			// reset flags from last time thru loop
			ejbCreateFound = false;
			returnsVoid = false;

			// The method name must be ejbCreate. 
			if (methods[i].getName().startsWith("ejbCreate")) {
			    ejbCreateFound = true;

			    // The return type must be void. 
			    Class rt = methods[i].getReturnType();
			    if (rt.getName().equals("void")) {
				returnsVoid = true;
				foundAtLeastOne++;
			    }

			    // now display the appropriate results for this particular ejbCreate
			    // method
			    if (ejbCreateFound && returnsVoid) {
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
						       "[ {0} ] properly declares [ {1} ] method with valid 'void' return type.",
						       new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
			    } else if (ejbCreateFound && (!returnsVoid)) {
				result.addErrorDetails(smh.getLocalString
						    ("tests.componentNameConstructor",
						     "For [ {0} ]",
						     new Object[] {compName.toString()}));
				result.addErrorDetails(smh.getLocalString
						    (getClass().getName() + ".debug1",
						     "For EJB Class [ {0} ] method [ {1} ]",
						     new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
				result.addErrorDetails(smh.getLocalString
						    (getClass().getName() + ".notApplicable2",
						     "An [ {0} ] method was found, but [ {1} ] method has a non 'void' return type value.   [ {2} ] methods return types must define return 'void' type for at least one ejbCreate method.",
						     new Object[] {methods[i].getName(),methods[i].getName(),methods[i].getName()}));
				oneFailed = true;
			    } 
			}
		    }
                } while (((c = c.getSuperclass()) != null) && (foundAtLeastOne == 0));
        
		if (foundAtLeastOne == 0) {
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failed",
				   "Error: [ {0} ] does not properly declare at least one ejbCreate(...) method with valid 'void' return type.  [ {1} ] is not a valid bean.",
				   new Object[] {descriptor.getEjbClassName(),descriptor.getEjbClassName()}));
		    oneFailed = true;
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
				  new Object[] {getClass(),"Session","Entity"}));
	    return result;
	}
    }
}
