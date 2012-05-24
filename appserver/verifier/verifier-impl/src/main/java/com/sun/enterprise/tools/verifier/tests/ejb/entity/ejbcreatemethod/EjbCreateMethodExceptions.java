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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.ejbcreatemethod;

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
 * Entity Bean's ejbCreate(...) methods exceptions test.
 * Each entity Bean class may define zero or more ejbCreate(...) methods. 
 * The number and signatures of a entity Bean's create methods are specific 
 * to each EJB class. The method signatures must follow these rules: 
 * 
 * The method name must be ejbCreate. 
 *
 * Compatibility Note: EJB 1.0 allowed the ejbCreate method to throw the 
 * java.rmi.RemoteException to indicate a non-application exception. This 
 * practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise bean 
 * should throw the javax.ejb.EJBException or another RuntimeException to 
 * indicate non-application exceptions to the Container (see Section 12.2.2). 
 * Note: Treat as a warning to user in this instance.
 */
public class EjbCreateMethodExceptions extends EjbTest implements EjbCheck { 


    /** 
     * Entity Bean's ejbCreate(...) methods exceptions test.
     * Each entity Bean class may define zero or more ejbCreate(...) methods. 
     * The number and signatures of a entity Bean's create methods are specific 
     * to each EJB class. The method signatures must follow these rules: 
     * 
     * Compatibility Note: EJB 1.0 allowed the ejbCreate method to throw the 
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

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    boolean oneFailed = false;
	    int foundWarning = 0;
	    int foundAtLeastOne = 0;
	    try {
		VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());

		boolean ejbCreateFound = false;
		boolean throwsRemoteException = false;
                // start do while loop here....
                do {
		    Method [] methods = c.getDeclaredMethods();
		    for (int i = 0; i < methods.length; i++) {
			// reset flags from last time thru loop
			ejbCreateFound = false;
			throwsRemoteException = false;

			// The method name must be ejbCreate. 
			if (methods[i].getName().startsWith("ejbCreate")) {
			    foundAtLeastOne++;
			    ejbCreateFound = true;

			    // Compatibility Note: EJB 1.0 allowed the ejbCreate method to throw
			    // the java.rmi.RemoteException to indicate a non-application 
			    // exception. This practice is deprecated in EJB 1.1---an EJB 1.1 
			    // compliant enterprise bean should throw the javax.ejb.EJBException
			    // or another RuntimeException to indicate non-application 
			    // exceptions to the Container (see Section 12.2.2). 
			    // Note: Treat as a warning to user in this instance.
			    Class [] exceptions = methods[i].getExceptionTypes();
			    for (int z = 0; z < exceptions.length; ++z) {
				if (exceptions[z].getName().equals("java.rmi.RemoteException")) {
				    throwsRemoteException = true;
				    break;
				}
			    }

			    // now display the appropriate results for this particular ejbCreate
			    // method
			    if (ejbCreateFound && (!throwsRemoteException)) {
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
						       "[ {0} ] properly declares [ {1} ] method which does not throw java.rmi.RemoteException.",
						       new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
			    } else if (ejbCreateFound && throwsRemoteException) {
				result.addWarningDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
				result.addWarningDetails(smh.getLocalString
							 (getClass().getName() + ".debug1",
							  "For EJB Class [ {0} ] method [ {1} ]",
							  new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
				result.addWarningDetails(smh.getLocalString
							 (getClass().getName() + ".warning",
							  "Error: Compatibility Note:" +
							  "\n An [ {0} ] method was found, but" +
							  "\n EJB 1.0 allowed the ejbCreate method to throw the " +
							  "\n java.rmi.RemoteException to indicate a non-application" +
							  "\n exception. This practice is deprecated in EJB 1.1" +
							  "\n ---an EJB 1.1 compliant enterprise bean should" +
							  "\n throw the javax.ejb.EJBException or another " +
							  "\n RuntimeException to indicate non-application exceptions" +
							  "\n to the Container. ",
							  new Object[] {methods[i].getName()}));
				foundWarning++;
				break;
			    } 
			}
		    }
		    if (foundWarning > 0)
			break;
                } while (((c = c.getSuperclass()) != null) && (foundAtLeastOne == 0));
        
		if (foundAtLeastOne == 0) {
		    result.addNaDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  "[ {0} ] does not declare any ejbCreate(...) methods.",
					  new Object[] {descriptor.getEjbClassName()}));
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
	    } else if (foundWarning > 0) {
		result.setStatus(result.WARNING);
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
