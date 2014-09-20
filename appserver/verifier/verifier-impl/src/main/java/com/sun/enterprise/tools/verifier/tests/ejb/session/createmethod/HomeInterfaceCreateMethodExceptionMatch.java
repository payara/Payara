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

package com.sun.enterprise.tools.verifier.tests.ejb.session.createmethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

/** 
 * Session beans home interface method exceptions match test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * signature: 
 * 
 * A Session Bean's home interface defines one or more create(...) methods. 
 * 
 * All the exceptions defined in the throws clause of an ejbCreate method of 
 * the enterprise Bean class must be defined in the throws clause of the 
 * matching create method of the home interface. 
 * 
 */
public class HomeInterfaceCreateMethodExceptionMatch extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    boolean foundAtLeastOneCreate = false;


    /** 
     * Session beans home interface method exceptions match test.
     * 
     * The following are the requirements for the enterprise Bean's home interface 
     * signature: 
     * 
     * A Session Bean's home interface defines one or more create(...) methods. 
     * 
     * All the exceptions defined in the throws clause of an ejbCreate method of 
     * the enterprise Bean class must be defined in the throws clause of the 
     * matching create method of the home interface. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbSessionDescriptor) {
            if (((descriptor.getHomeClassName() == null) || "".equals(descriptor.getHomeClassName())) &&
                ((descriptor.getLocalHomeClassName() == null) || "".equals(descriptor.getLocalHomeClassName()))) {

                if (implementsEndpoints(descriptor)) {

                    result.addNaDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                         new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString
                       ("com.sun.enterprise.tools.verifier.tests.ejb.webservice.notapp",
                       "Not Applicable because, EJB [ {0} ] implements a Service Endpoint Interface.",
                       new Object[] {compName.toString()}));
                    result.setStatus(result.NOT_APPLICABLE);
                 }
                 else {

                  result.addErrorDetails(smh.getLocalString
                                   ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                  result.addErrorDetails(smh.getLocalString
                                    ("com.sun.enterprise.tools.verifier.tests.ejb.webservice.failedhome",
                                    "Ejb [ {0} ] does not have local or remote Home interfaces",
                                    new Object[] {descriptor.getEjbClassName()}));
                  result.setStatus(result.FAILED);
                  return result;

                 }
              return result;
            }

	    boolean oneFailed = false;
 
	    oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor.getLocalHomeClassName(),(EjbSessionDescriptor)descriptor);
		
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

    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home interface of the Ejb. 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */

    private boolean commonToBothInterfaces(String home, String local,EjbSessionDescriptor descriptor) {
	boolean oneFailed = false;
	int ejbCreateMethodLoopCounter = 0;
	// RULE: session home interface are only allowed to have create 
	//       methods which match ejbCreate, and exceptions match Bean's
	try {
	    VerifierTestContext context = getVerifierContext();
	    ClassLoader jcl = context.getClassLoader();
	    Class methodReturnType;
	    Class [] methodParameterTypes;
	    Class [] methodExceptionTypes;
	    Class [] ejbCreateMethodExceptionTypes;
	    Class [] ejbCreateMethodParameterTypes;
	    boolean ejbCreateFound = false;
	    boolean exceptionsMatch = false;
	    Vector<Method> createMethodSuffix = new Vector<Method>();

	    if (home != null) {
		Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
		Method [] homeMethods = c.getDeclaredMethods();
		for (int i = 0; i < homeMethods.length; i++) {
		    // The method name must start with create. 
		    if (homeMethods[i].getName().startsWith("create")) {
			createMethodSuffix.addElement( (Method)homeMethods[i]);
			foundAtLeastOneCreate = true;
		    }
		}
	    }
	    if (local != null) {
		Class c = Class.forName(local, false, getVerifierContext().getClassLoader());
		Method [] homeMethods = c.getDeclaredMethods();
		for (int i = 0; i < homeMethods.length; i++) {
		    // The method name must start with create. 
		    if (homeMethods[i].getName().startsWith("create")) {
			createMethodSuffix.addElement( (Method)homeMethods[i]);
			foundAtLeastOneCreate = true;
		    }
		}
	    }
	    if (foundAtLeastOneCreate == false) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException2",
			       "Error: no create<Method> method exists!",
			       new Object[] {}));
		return true;
	    }
	    Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
	    
	    do {
		Method [] methods = EJBClass.getDeclaredMethods();
		// find matching "ejbCreate" in bean class
		for (int i = 0; i < methods.length; i++) {
		    ejbCreateFound = false;
		    exceptionsMatch = false;
		    if (methods[i].getName().startsWith("ejbCreate")) {
			String matchSuffix = methods[i].getName().substring(9);
			for (int k = 0; k < createMethodSuffix.size(); k++) {
			    if (matchSuffix.equals(((Method)(createMethodSuffix.elementAt(k))).getName().substring(6))) {
				// clear these from last time thru loop
				// retrieve the EJB Class Methods
				methodParameterTypes = ((Method)(createMethodSuffix.elementAt(k))).getParameterTypes();
				ejbCreateMethodParameterTypes = methods[i].getParameterTypes();
				if (Arrays.equals(methodParameterTypes,ejbCreateMethodParameterTypes)) {
				    ejbCreateFound = true;
				    methodExceptionTypes = ((Method)(createMethodSuffix.elementAt(k))).getExceptionTypes();
				    ejbCreateMethodExceptionTypes = methods[i].getExceptionTypes();
				    if (RmiIIOPUtils.isEjbFindMethodExceptionsSubsetOfFindMethodExceptions(ejbCreateMethodExceptionTypes,methodExceptionTypes)) {
					exceptionsMatch = true;
					// used to display output below
					ejbCreateMethodLoopCounter = k;
					break;
				    }
				} // method params match
			    } // found ejbCreate
			}
			
			//report for this particular create method found in home interface
			//if we know that ejbCreateFound got set to true in the above 
			// loop, check other booleans, otherwise skip test, set status 
			// to FAILED below
			
			// now display the appropriate results for this particular create
			// method
			if (ejbCreateFound && exceptionsMatch) {
			    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".debug1",
						   "For Home Interface Method [ {0} ]",
						   new Object[] {((Method)(createMethodSuffix.elementAt(ejbCreateMethodLoopCounter))).getName()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".passed",
						   "The corresponding [ {0} ] method with matching exceptions was found.",
						   new Object[] {methods[i].getName()}));
			}
			
			if (ejbCreateFound && !exceptionsMatch) {
			    oneFailed = true;
			    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addErrorDetails(smh.getLocalString
						   (getClass().getName() + ".debug1",
						    "For Home Interface Method [ {0} ]",
						    new Object[] {((Method)(createMethodSuffix.elementAt(ejbCreateMethodLoopCounter))).getName()}));
			    result.addErrorDetails(smh.getLocalString
						   (getClass().getName() + ".failed",
						    "Error: A corresponding [ {0} ] method was found, but the exceptions defined by method [ {1} ] are not defined within matching create() method.",
						    new Object[] {"ejb"+methods[i].getName().toUpperCase().substring(0,1)+methods[i].getName().substring(1),"ejb"+methods[i].getName().toUpperCase().substring(0,1)+methods[i].getName().substring(1)}));
			} else if (!ejbCreateFound) {
			    // set status to FAILED, 'cause there is not even an
			    // ejbCreate method to begin with, regardless of its parameters
			    oneFailed = true;
			    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addErrorDetails(smh.getLocalString
						   (getClass().getName() + ".debug1",
						    "For Home Interface Method [ {0} ]",
						    new Object[] {((Method)(createMethodSuffix.elementAt(ejbCreateMethodLoopCounter))).getName()}));
			    result.addErrorDetails(smh.getLocalString
						   (getClass().getName() + ".failed1",
						    "Error: No corresponding ejbCreate method was found." ));
			    break;
			}  // end of reporting for this particular 'create' method
		    } // if the home interface found a "create" method
		} // for all the methods within the home interface class, loop
		if (oneFailed == true)
		    break;
	    } while (((EJBClass = EJBClass.getSuperclass()) != null) && (!(ejbCreateFound && exceptionsMatch)));
	    return oneFailed;
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home (Remote/Local) interface or Bean class [ {0} ] does not exist or is not loadable within bean [ {1} ]",
			   new Object[] {descriptor.getEjbClassName(),descriptor.getName()}));
	    return oneFailed;
	}
    }
}
