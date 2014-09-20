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

package com.sun.enterprise.tools.verifier.tests.ejb.entity;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

/** 
 * Entity beans home interface method exceptions match test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * signature: 
 * 
 * An Entity Bean's home interface defines one or more create(...) methods. 
 * 
 * All the exceptions defined in the throws clause of an ejbPostCreate method 
 * of the enterprise Bean class must be defined in the throws clause of the 
 * matching create method of the home interface. 
 * 
 */
public class HomeInterfacePostCreateMethodExceptionMatch extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    boolean foundAtLeastOneCreate = false;

    /**
     * Entity beans home interface method exceptions match test.
     * 
     * The following are the requirements for the enterprise Bean's home interface 
     * signature: 
     * 
     * An Entity Bean's home interface defines one or more create(...) methods. 
     * 
     * All the exceptions defined in the throws clause of an ejbPostCreate method 
     * of the enterprise Bean class must be defined in the throws clause of the 
     * matching create method of the home interface. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    boolean oneFailed = false;
	  
	    // RULE: entity home interface are only allowed to have create 
	    //       methods which match ejbPostCreate, and exceptions match Bean's
	  
	    if(descriptor.getHomeClassName() != null) {
		oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor);
	    }
	    if(oneFailed == false) {
		if(descriptor.getLocalHomeClassName() != null) {
		    oneFailed = commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor);
		}
	    }

	    if (!foundAtLeastOneCreate) {
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.addNaDetails(smh.getLocalString
				    (getClass().getName() + ".debug3",
				     "In Home Interface ",
				     new Object[] {}));
		result.addNaDetails(smh.getLocalString
				    (getClass().getName() + ".notApplicable1",
				     "No create method was found, test not applicable." ));
		result.setStatus(result.NOT_APPLICABLE);
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
     * @param home for the Home Interface of the Ejb 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */

    private boolean commonToBothInterfaces(String home, EjbDescriptor descriptor) {
	int ejbPostCreateMethodLoopCounter = 0;
	boolean oneFailed = false;
	foundAtLeastOneCreate = false;
	try {
	    Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
	    Class methodReturnType;
	    Class [] methodParameterTypes;
	    Class [] methodExceptionTypes;
	    Class [] ejbPostCreateMethodExceptionTypes;
	    Class [] ejbPostCreateMethodParameterTypes;
	    boolean ejbPostCreateFound = false;
	    boolean signaturesMatch = false;
	    boolean exceptionsMatch = false;
	    boolean createExists = false;

	    Method [] homeMethods = c.getDeclaredMethods();
	    Vector<Method> createMethodSuffix = new Vector<Method>();
	    for (int i = 0; i < homeMethods.length; i++) {
		// The method name must start with create. 
		if (homeMethods[i].getName().startsWith("create")) {
		    foundAtLeastOneCreate = true;
		    createMethodSuffix.addElement( (Method)homeMethods[i]);
		}
	    }
	    if(foundAtLeastOneCreate == false)
		return false;
	    
	    Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
	    do {
		Method [] methods = EJBClass.getDeclaredMethods();
		// find matching "ejbCreate" in bean class
		for (int i = 0; i < methods.length; i++) {
		    ejbPostCreateFound = false;
		    signaturesMatch = false;
		    exceptionsMatch = false;
		    Method [] ejbPostCreateMethods = EJBClass.getDeclaredMethods();
		    //ejbPostCreateMethods1 = null;
		    //ejbPostCreateMethods1 = ejbPostCreateMethods;
		    ejbPostCreateMethodLoopCounter = 0;
		    //   Method [] ejbPostCreateMethods = EJBClass.getDeclaredMethods();
		    if (methods[i].getName().startsWith("ejbPostCreate")) {
		        ejbPostCreateFound = true;
			String matchSuffix = methods[i].getName().substring(13);
			for (int k = 0; k < createMethodSuffix.size(); k++) {
			    if (matchSuffix.equals(((Method)(createMethodSuffix.elementAt(k))).getName().substring(6))) {
				// clear these from last time thru loop
				    // retrieve the EJB Class Methods
				methodParameterTypes = ((Method)(createMethodSuffix.elementAt(k))).getParameterTypes();
				ejbPostCreateMethodParameterTypes = methods[i].getParameterTypes();
				if (Arrays.equals(methodParameterTypes,ejbPostCreateMethodParameterTypes)) {
				    signaturesMatch = true;
				    methodExceptionTypes = ((Method)(createMethodSuffix.elementAt(k))).getExceptionTypes();
				    ejbPostCreateMethodExceptionTypes = methods[i].getExceptionTypes();
				    // methodExceptionTypes needs to be
				    // a superset of all the possible exceptions thrown in
				    // ejbPostCreateMethodExceptionTypes
				    // All the exceptions defined in the throws clause of the 
				    // matching ejbCreate and ejbPostCreate methods of the 
				    // enterprise Bean class must be included in the throws 
				    // clause of the matching create method of the home interface
				    // (i.e the set of exceptions defined for the create method 
				    // must be a superset of the union of exceptions defined for 
				    // the ejbCreate and ejbPostCreate methods)
				    if (RmiIIOPUtils.isEjbFindMethodExceptionsSubsetOfFindMethodExceptions(ejbPostCreateMethodExceptionTypes,methodExceptionTypes)) {
					exceptionsMatch = true;
					// used to display output below
					ejbPostCreateMethodLoopCounter = k;
					break;
				    }
				} // method params match
			    } // found ejbPostCreate
			} // for all the business methods within the bean class, loop
			
			//report for this particular create method found in home interface
			//if we know that ejbPostCreateFound got set to true in the above 
			// loop, check other booleans, otherwise skip test, set status 
			// to FAILED below
			
			// now display the appropriate results for this particular create
			// method
			if (ejbPostCreateFound && signaturesMatch && exceptionsMatch) {
			    result.addGoodDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".debug1",
						   "For Home Interface [ {0} ] Method [ {1} ]",
						   new Object[] {home,((Method)(createMethodSuffix.elementAt(ejbPostCreateMethodLoopCounter))).getName()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".passed",
						   "The corresponding [ {0} ] method with matching exceptions was found in [ {1} ].",
						   new Object[] {methods[i].getName(), EJBClass.getName()}));
			} else if (ejbPostCreateFound && signaturesMatch && !exceptionsMatch) {
			    result.addErrorDetails(smh.getLocalString
						("tests.componentNameConstructor",
						 "For [ {0} ]",
						 new Object[] {compName.toString()}));
			    result.addErrorDetails(smh.getLocalString
				     (getClass().getName() + ".debug1",
				      "For Home Interface [ {0} ] Method [ {1} ]",
				      new Object[] {home,((Method)(createMethodSuffix.elementAt(ejbPostCreateMethodLoopCounter))).getName()}));
			    result.failed(smh.getLocalString
				     (getClass().getName() + ".notApplicableDebug",
				      "A corresponding [ {0} ] method was found in [ {1} ], but the exceptions did not match.", 
				      new Object[] {methods[i].getName(),EJBClass.getName()}));
			    oneFailed = true;
			    break;
			} else if (ejbPostCreateFound && !signaturesMatch) {
			    result.addErrorDetails(smh.getLocalString
						   ("tests.componentNameConstructor",
						    "For [ {0} ]",
						    new Object[] {compName.toString()}));
			    result.addErrorDetails(smh.getLocalString
				     (getClass().getName() + ".debug3",
				      "For Home Interface ",
				      new Object[] {}));
			    result.failed(smh.getLocalString
				     (getClass().getName() + ".notApplicableDebug2",
				      "A corresponding [ {0} ] method was found in [ {1} ], but the parameters did not match.", 
				      new Object[] {methods[i].getName(),EJBClass.getName()}));
			    oneFailed = true;
			    break;
			}
		    }
		}
		if (oneFailed == true)
		    break;
	    } while (((EJBClass = EJBClass.getSuperclass()) != null) && (!(ejbPostCreateFound && signaturesMatch && exceptionsMatch)));
	    return oneFailed; 
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home interface [ {0} ] or EJB class [ {1} ] does not exist or is not loadable within bean [ {2} ]",
			   new Object[] {home, descriptor.getEjbClassName(), descriptor.getName()}));
	    return oneFailed;
	}
    }
}
