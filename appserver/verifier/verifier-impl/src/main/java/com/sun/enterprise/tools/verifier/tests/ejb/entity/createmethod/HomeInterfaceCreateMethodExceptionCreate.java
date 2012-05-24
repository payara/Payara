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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.createmethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/**
 * create<Method> method tests
 * Entity beans home interface create method throws CreateException test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * signature: 
 * 
 * An Entity Bean's home interface defines zero or more create(...) methods. 
 * 
 * The throws clause must include javax.ejb.CreateException. 
 */
public class HomeInterfaceCreateMethodExceptionCreate extends EjbTest implements EjbCheck { 
    boolean foundAtLeastOneCreate = false;
    Result result = null;
    ComponentNameConstructor compName = null;
    /**
     * Entity beans home interface create method throws CreateException test.
     * 
     * The following are the requirements for the enterprise Bean's home interface 
     * signature: 
     * 
     * An Entity Bean's home interface defines zero or more create(...) methods. 
     * 
     * The throws clause must include javax.ejb.CreateException. 
     *    
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();
	boolean oneFailed = false;

	if (descriptor instanceof EjbEntityDescriptor) {
	    if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName()))
		oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor);
	    if(oneFailed == false) {
		if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName()))
		    oneFailed = commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor);
	    }

	    if (!foundAtLeastOneCreate) {
		result.addGoodDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
		result.addGoodDetails(smh.getLocalString
				      (getClass().getName() + ".debug3",
				       "For Home Interface [ {0} ]",
				       new Object[] {descriptor.getHomeClassName()}));
		result.addGoodDetails(smh.getLocalString
				      (getClass().getName() + ".notApplicable1",
				       "No create method was found, test not applicable." ));
		result.setStatus(result.PASSED);
	    } else {
		if (oneFailed) {
		    result.setStatus(result.FAILED);
		} else {
		    result.setStatus(result.PASSED);
		}
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
    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home interface of the Ejb. 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */


    private boolean commonToBothInterfaces(String home, EjbDescriptor descriptor) {
	boolean oneFailed = false;
	
	// RULE: Entity home interface are only allowed to have create 
	//       methods which must throw javax.ejb.CreateException
	try {
	    VerifierTestContext context = getVerifierContext();
	    ClassLoader jcl = context.getClassLoader();
	    Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
	    Method methods[] = c.getDeclaredMethods();
	    Class [] methodExceptionTypes;
	    boolean throwsCreateException = false;
	    
	    for (int i=0; i< methods.length; i++) {
		// clear these from last time thru loop
		throwsCreateException = false;
		if (methods[i].getName().startsWith("create")) {
		    // set this once to indicate that test is applicable, if you didn't
		    // find any create methods, that's okay too, as entity beans can
		    // have  zero or more create methods, & when you have zero, test
		    // is N/A
		    if (!foundAtLeastOneCreate) {
			foundAtLeastOneCreate = true;
		    }  
		    
		    methodExceptionTypes = methods[i].getExceptionTypes();
		    
		    // methods must also throw javax.ejb.CreateException
		    for (int kk = 0; kk < methodExceptionTypes.length; ++kk) {
			if (methodExceptionTypes[kk].getName().equals("javax.ejb.CreateException")) {
			    throwsCreateException = true;
			    break;
			}
		    }
		    
		    //report for this particular create method found in home interface
		    // now display the appropriate results for this particular create
		    // method
		    if (throwsCreateException ) {
			result.addGoodDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString
					      (getClass().getName() + ".debug1",
					       "For Home Interface [ {0} ] Method [ {1} ]",
					       new Object[] {c.getName(),methods[i].getName()}));
			result.addGoodDetails(smh.getLocalString
					      (getClass().getName() + ".passed",
					       "The create method which must throw javax.ejb.CreateException was found."));
		    } else if (!throwsCreateException) {
			oneFailed = true;
			result.addErrorDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".debug1",
						"For Home Interface [ {0} ] Method [ {1} ]",
						new Object[] {home,methods[i].getName()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed",
						"Error: A create method was found, but did not throw javax.ejb.CreateException." ));
			break;
		    }  // end of reporting for this particular 'create' method
		} // if the home interface found a "create" method
	    } // for all the methods within the home interface class, loop
	    
	    return oneFailed;
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home interface [ {0} ] does not exist or is not loadable within bean [ {1} ]",
			   new Object[] {home, descriptor.getName()}));
	    return oneFailed;
	}
    }
}
