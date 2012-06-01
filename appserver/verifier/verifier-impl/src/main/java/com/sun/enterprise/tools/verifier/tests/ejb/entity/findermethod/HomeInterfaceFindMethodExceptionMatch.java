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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.findermethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Entity beans home interface find<METHOD> method exceptions match test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * find<METHOD> method signature: 
 * 
 * An Entity Bean's home interface defines one or more find<METHOD>(...) 
 * methods. 
 * 
 * All the exceptions defined in the throws clause of an ejbFind<METHOD>(...) 
 * method of the enterprise Bean class must be defined in the throws clause of 
 * the matching find<METHOD>(...) method of the home interface. 
 * 
 */
public class HomeInterfaceFindMethodExceptionMatch extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    /**
     * Entity beans home interface find<METHOD> method exceptions match test.
     * 
     * The following are the requirements for the enterprise Bean's home interface 
     * find<METHOD> method signature: 
     * 
     * An Entity Bean's home interface defines one or more find<METHOD>(...) 
     * methods. 
     * 
     * All the exceptions defined in the throws clause of an ejbFind<METHOD>(...) 
     * method of the enterprise Bean class must be defined in the throws clause of 
     * the matching find<METHOD>(...) method of the home interface. 
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
	    String persistence =
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.BEAN_PERSISTENCE.equals(persistence)) {
		if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName())) {
		    oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor);
		}
		if(oneFailed == false) {
		    if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName())) {
			oneFailed = commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor);
		    }
		}

		if (oneFailed) {
		    result.setStatus(result.FAILED);
		} else {
		    result.setStatus(result.PASSED);
		}
   
		return result;
	    } else { //if (CONTAINER_PERSISTENCE.equals(persistence))
		result.addNaDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable2",
				      "Expected [ {0} ] managed persistence, but [ {1} ] bean has [ {2} ] managed persistence.",
				      new Object[] {EjbEntityDescriptor.BEAN_PERSISTENCE,descriptor.getName(),persistence}));
		return result;
	    }
        
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
     * @param interfaceType determines the type of interface (remote/local)
     * @return boolean the results for this assertion i.e if a test has failed or not
     */

    private boolean commonToBothInterfaces(String home, EjbDescriptor descriptor) {
	boolean oneFailed = false;
	int ejbFinderMethodLoopCounter = 0;
	// RULE: entity home interface are only allowed to have find<METHOD> 
	//       methods which match ejbfind<METHOD>, and exceptions match Bean's
	try {
	    VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
	    Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
	    Method methods[] = c.getDeclaredMethods();
	    Class methodReturnType;
	    Class [] methodParameterTypes;
	    Class [] methodExceptionTypes;
	    Class [] ejbFinderMethodExceptionTypes;
	    Class [] ejbFinderMethodParameterTypes;
	    boolean ejbFinderFound = false;
	    boolean signaturesMatch = false;
	    boolean exceptionsMatch = false;
	    
	    
	    for (int i=0; i< methods.length; i++) {
		if (methods[i].getName().startsWith("find")) {
		    // clear these from last time thru loop
		    ejbFinderFound = false;
		    signaturesMatch = false;
		    exceptionsMatch = false;
		    // retrieve the EJB Class Methods
		    Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
		    // start do while loop here....
		    do {
			Method [] ejbFinderMethods = EJBClass.getDeclaredMethods();
			// find matching "ejbFind<METHOD>" in bean class
			for (int z=0; z< ejbFinderMethods.length; z++) {
			    if (ejbFinderMethods[z].getName().startsWith("ejbFind")) {
				// check rest of string to see if findAccount matches
				// ejbFindAccount
				if (methods[i].getName().toUpperCase().equals
				    (ejbFinderMethods[z].getName().toUpperCase().substring(3))) {
				    // found one, see if it matches same number and types
				    // of arguments, exceptions too, 
				    
				    ejbFinderFound = true;
				    methodParameterTypes = methods[i].getParameterTypes();
				    ejbFinderMethodParameterTypes = ejbFinderMethods[z].getParameterTypes();
				    if (Arrays.equals(methodParameterTypes,ejbFinderMethodParameterTypes)) {
					signaturesMatch = true;
					
					methodExceptionTypes = methods[i].getExceptionTypes();
					ejbFinderMethodExceptionTypes = ejbFinderMethods[z].getExceptionTypes();
					
					// All the exceptions defined in the throws clause of the
					// matching ejbFind method of the
					// enterprise Bean class must be included in the throws
					// clause of the matching find method of the home interface
					// including findByPrimaryKey, this home interface
					// find method must define a superset of all the 
					// exceptions thrown in the ejbFind method of the bean class
					// so there may not be a 1-1 mapping of exceptions
					// also, for all ejbFind/find combo's any unchecked 
					// exceptions thrown by the ejbFind<METHOD> in the bean 
					// class doesn't need to be thrown in the corresponding
					// find<METHOD> of the home interface , these unchecked
					// exceptions "subclass of RuntimeException" i.e
					// out of memory exception are handled by the container, 
					// who throws a Runtime exception to the appropriate 
					// instance/object
					
					if (RmiIIOPUtils.isEjbFindMethodExceptionsSubsetOfFindMethodExceptions(ejbFinderMethodExceptionTypes,methodExceptionTypes)) {
					    exceptionsMatch = true;
					    // used to display output below
					    ejbFinderMethodLoopCounter = z;
					    break;
					}
				    } // method params match
				}  // check rest of string to see if findAccount 
				//  matches ejbFindAccount
			    } // found ejbFind<METHOD>
			} // for all the business methods within the bean class, loop
			
			//report for this particular find method found in home interface
			//if we know that ejbFinderFound got set to true in the above 
			// loop, check other booleans, otherwise skip test, set status 
			// to FAILED below
			
			// now display the appropriate results for this particular find
			// method
			if (ejbFinderFound && signaturesMatch && exceptionsMatch) {
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
						   "The corresponding [ {0} ] method with matching exceptions was found.",
						   new Object[] {ejbFinderMethods[ejbFinderMethodLoopCounter].getName()}));
			} else if (ejbFinderFound && signaturesMatch && !exceptionsMatch) {
			    logger.log(Level.FINE, getClass().getName() + ".debug1",
                        new Object[] {c.getName(),methods[i].getName()});
                logger.log(Level.FINE, getClass().getName() + ".debug3",
                        new Object[] {"ejb"+methods[i].getName().toUpperCase().substring(0,1)+methods[i].getName().substring(1)});
                logger.log(Level.FINE, getClass().getName() + ".debug2");

			} else if (ejbFinderFound && !signaturesMatch) {
                logger.log(Level.FINE, getClass().getName() + ".debug1",
                        new Object[] {c.getName(),methods[i].getName()});
                logger.log(Level.FINE, getClass().getName() + ".debug4",
                        new Object[] {"ejb"+methods[i].getName().toUpperCase().substring(0,1)+methods[i].getName().substring(1)});
                logger.log(Level.FINE, getClass().getName() + ".debug2");

			}
			
		    } while (((EJBClass = EJBClass.getSuperclass()) != null) && (!(ejbFinderFound && signaturesMatch && exceptionsMatch)));
		    
		    
		    if (!ejbFinderFound && !signaturesMatch && !exceptionsMatch) {
			oneFailed = true;
			result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".debug1",
						"For Home Interface [ {0} ] Method [ {1} ]",
						new Object[] {c.getName(),methods[i].getName()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed",
						"Error: No corresponding [ {0} ] method with matching signatures was found." ,
						new Object[] {"ejb"+methods[i].getName().toUpperCase().substring(0,1)+methods[i].getName().substring(1)}));
		    }  // end of reporting for this particular 'find' method
		} // if the home interface found a "find" method
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
			   "Error: Home interface [ {0} ] or EJB class [ {1} ] does not exist or is not loadable within bean [ {2} ]",
			   new Object[] {home, descriptor.getEjbClassName(),descriptor.getName()}));
	    return oneFailed;
	}
    }
}
