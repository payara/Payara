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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.ejbfindbyprimarykey;

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

/** 
 * Define ejbFindByPrimaryKey method arguments test.  
 *
 *     Every entity enterprise Bean class must define the ejbFindByPrimaryKey 
 *     method. 
 * 
 *     The methods arguments types must be legal types for RMI-IIOP.
 */
public class EjbFindByPrimaryKeyArgs extends EjbTest implements EjbCheck { 


    /** 
     * Define ejbFindByPrimaryKey method arguments test.  
     *
     *     Every entity enterprise Bean class must define the ejbFindByPrimaryKey 
     *     method. 
     * 
     *     The methods arguments types must be legal types for RMI-IIOP.
     *  
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    String persistentType =
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.BEAN_PERSISTENCE.equals(persistentType)) {
		Class [] ejbFinderMethodParameterTypes;
		boolean ejbFindByPrimaryKeyMethodFound = false;
		boolean oneFailed = false;
		boolean isLegalRMIIIOP = false;
		try {
		    // retrieve the EJB Class Methods
		    VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		    Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
                    // start do while loop here....
                    do {
			Method [] ejbFinderMethods = EJBClass.getDeclaredMethods();
    	  
			for (int j = 0; j < ejbFinderMethods.length; ++j) {
			    if (ejbFinderMethods[j].getName().equals("ejbFindByPrimaryKey")) {
				// Every entity enterprise Bean class must define the 
				// ejbFindByPrimaryKey method. 
				ejbFindByPrimaryKeyMethodFound = true;
    
				// The methods arguments types must be legal types for RMI-IIOP. 
				ejbFinderMethodParameterTypes = ejbFinderMethods[j].getParameterTypes();
				if (RmiIIOPUtils.isValidRmiIIOPParameters(ejbFinderMethodParameterTypes)) {
				// these method parameters are valid, continue
				    isLegalRMIIIOP = true;
				}
  
				if (ejbFindByPrimaryKeyMethodFound && isLegalRMIIIOP) { 
				    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".debug1",
							   "For EJB Class [ {0} ] Finder Method [ {1} ]",
							   new Object[] {EJBClass.getName(),ejbFinderMethods[j].getName()}));
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".passed",
							   "An [ {0} ] method with valid RMI-IIOP argument types was found.",
							   new Object[] {ejbFinderMethods[j].getName()}));
				} else if (ejbFindByPrimaryKeyMethodFound && (!isLegalRMIIIOP)) {
				    oneFailed = true;
				    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".debug1",
							    "For EJB Class [ {0} ] Finder Method [ {1} ]",
							    new Object[] {EJBClass.getName(),ejbFinderMethods[j].getName()}
							    ));
				    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".failed",
							    "Error: An [ {0} ] method was found, but [ {1} ] method has illegal parameter values. [ {2} ] methods arguments types must be legal types for RMI-IIOP.",
							    new Object[] {ejbFinderMethods[j].getName(),ejbFinderMethods[j].getName(),ejbFinderMethods[j].getName()}));
				}			 
				// found one, and there should only be one, break out
				break;
			    }
			}
                    } while (((EJBClass = EJBClass.getSuperclass()) != null) && (!ejbFindByPrimaryKeyMethodFound));
          
		    if (!ejbFindByPrimaryKeyMethodFound) {
			oneFailed = true;
			result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".debug3",
						"For EJB Class [ {0} ]",
						new Object[] {descriptor.getEjbClassName()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed1",
						"Error: No ejbFindByPrimaryKey method was found in bean class."));
		    }
  
		} catch (ClassNotFoundException e) {
		    Verifier.debug(e);
		    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failedException",
				   "Error: EJB Class [ {0} ] does not exist or is not loadable.",
				   new Object[] {descriptor.getEjbClassName()}));
		    oneFailed = true;
		}
    
		if (oneFailed) {
		    result.setStatus(result.FAILED);
		} else {
		    result.setStatus(result.PASSED);
		}
	    } else { //(CONTAINER_PERSISTENCE.equals(persistentType))
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable2",
				      "Expected persistence type [ {0} ], but bean [ {1} ] has persistence type [ {2} ]",
				      new Object[] {EjbEntityDescriptor.BEAN_PERSISTENCE,descriptor.getName(),persistentType}));
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
