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
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/** 
 * Entity beans home interface find<METHOD> method return type test.
 *
 * The following are the requirements for the signatures of the finder methods 
 * defined in Bean's home interface:
 *
 * An Entity Bean's home interface defines one or more find<METHOD>(...) 
 * methods.
 *
 * The return type for a find<METHOD>(...) method must be the enterprise 
 * Bean's remote interface type or a collection thereof.
 *
 */
public class HomeInterfaceFindMethodReturn extends EjbTest implements EjbCheck { 

    Result result = null;
    ComponentNameConstructor compName = null;
    /**
     * Entity beans home interface find<METHOD> method return type test.
     *
     * The following are the requirements for the signatures of the finder methods 
     * defined in Bean's home interface:
     *
     * An Entity Bean's home interface defines one or more find<METHOD>(...) 
     * methods.
     *
     * The return type for a find<METHOD>(...) method must be the enterprise 
     * Bean's remote interface type or a collection thereof.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
            String persistence =
                ((EjbEntityDescriptor)descriptor).getPersistenceType();
            if (EjbEntityDescriptor.BEAN_PERSISTENCE.equals(persistence)) {
		boolean oneFailed = false;
		// RULE: Entity home interface are allowed to have find<METHOD> 
		//       methods which returns the entity Bean's remote interface 
		//       or a collection thereof. 
		if(descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName()) &&
		   descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName())) {
		    oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor.getRemoteClassName(),descriptor);
		}
		if(oneFailed == false) {
		    if(descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName()) &&
		       descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName())) {
			oneFailed = commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor.getLocalClassName(),descriptor);
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
     * @param remote for Remote/Local interface
     * @return boolean the results for this assertion i.e if a test has failed or not
     */

    private boolean commonToBothInterfaces(String home, String remote, EjbDescriptor descriptor) {
	boolean oneFailed = false;
	try {
		    VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		    Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
		    Class rc = Class.forName(remote, false, getVerifierContext().getClassLoader());
		    Method methods[] = c.getDeclaredMethods();
		    Class methodReturnType;
		    boolean validReturn = false;
  
		    for (int i=0; i< methods.length; i++) {
			// clear these from last time thru loop
			validReturn = false;
			if (methods[i].getName().startsWith("find")) {
			    // return type must be the remote interface 
			    // or collection thereof
			    methodReturnType = methods[i].getReturnType();
			    if ((methodReturnType.getName().equals(rc.getName())) ||
				(methodReturnType.getName().equals("java.util.Collection")) ||
				(methodReturnType.getName().equals("java.util.Enumeration"))) { 
				// this is the right return type for find method
				validReturn = true;
			    } else {
				validReturn = false;
			    } // return valid
  
			    //report for this particular find method found in home interface
			    // now display the appropriate results for this particular find
			    // method
			    if (validReturn) {
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
						       "The find<METHOD> which returns remote interface or a collection there of was found."));
			    } else if (!validReturn) {
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
							"Error: A find<METHOD> was found, but the return type [ {0} ] was not the Remote interface [ {1} ] or a collection there of." ,
							new Object[] {methodReturnType.getName(),rc.getName()}));
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
				   "Error: Home interface [ {0} ] or Remote interface [ {1} ] does not exist or is not loadable within bean [ {2} ]",
				   new Object[] {home,remote, descriptor.getName()}));
		    return oneFailed;
		}
    }
}
