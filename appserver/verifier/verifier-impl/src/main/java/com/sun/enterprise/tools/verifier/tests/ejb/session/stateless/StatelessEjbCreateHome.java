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

package com.sun.enterprise.tools.verifier.tests.ejb.session.stateless;

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
 * Stateless session enterprise beans class single create method test.
 * The session enterprise Bean class must define a single ejbCreate method 
 * that takes no arguments. 
 */
public class StatelessEjbCreateHome extends EjbTest implements EjbCheck { 


    /** 
     * Stateless session enterprise beans class single create method test.
     * The session enterprise Bean class must define a single ejbCreate method 
     * that takes no arguments. 
     *    
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbSessionDescriptor) {
	    String stateType = ((EjbSessionDescriptor)descriptor).getSessionType();
	    if (EjbSessionDescriptor.STATELESS.equals(stateType)) {
		// RULE: The stateless session enterprise Bean class must define a 
		// single ejbCreate method that takes no arguments. 
		try {
		    VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		    Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
		    Method m= null;
		    int foundThisManyTimes = 0;
                    // start do while loop here....
                    do {
		        Method methods[] = c.getDeclaredMethods();
		        for (int i=0; i< methods.length; i++) {
			    if (!methods[i].getName().equals("ejbCreate")){
			        continue;
			    }
			    if (foundThisManyTimes == 0) {
			        m = methods[i];
			        foundThisManyTimes++;
			    } else {
			        foundThisManyTimes++;
			    }
		        }
                    } while (((c = c.getSuperclass()) != null) && (m == null));

		    //if we know that m got set to create in the above loop, check params
		    // otherwise skip test, set status to FAILED below,
		    if ((m != null) && (foundThisManyTimes == 1)) {
			Class cc[] = m.getParameterTypes();
			if (cc.length > 0) {
			    result.addErrorDetails(smh.getLocalString
						   ("tests.componentNameConstructor",
						    "For [ {0} ]",
						    new Object[] {compName.toString()}));
			    result.failed(smh.getLocalString
					  (getClass().getName() + ".failed",
					   "Error: The ejbCreate method has one or more parameters \n" +
					   "within bean [ {0} ].  Stateless session are only allowed \n" +
					   "to have ejbCreate methods with no arguments.",
					   new Object[] {descriptor.getEjbClassName()}));
			} else {
			    result.addGoodDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
			    result.passed(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "Valid: This bean's [ {0} ] ejbCreate method has no parameters."
					   + "\n Stateless session beans can only have a ejbCreate method"
					   + "\n with no parameters.",
					   new Object[] {descriptor.getEjbClassName()}));
			}
		    } else if ((m != null) && (foundThisManyTimes > 1)) {
			// set status to FAILED, 'cause there is more than one 
			// create methods to begin with, regardless of its parameters
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.failed(smh.getLocalString
				      (getClass().getName() + ".failed2",
				       "Error: [ {0} ] ejbCreate methods exists within bean [ {1} ].  The EJB class must have only one ejbCreate method for stateless session bean. ",
				       new Object[] {new Integer(foundThisManyTimes),descriptor.getEjbClassName()}));
		    } else {
			// set status to FAILED, 'cause there is not even
			// a create method to begin with, regardless of its parameters
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.failed(smh.getLocalString
				      (getClass().getName() + ".failed3",
				       "Error: No ejbCreate method exists within bean [ {0} ]",
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
				   "Error: Class [ {0} ] not found within bean [ {1} ]",
				   new Object[] {descriptor.getEjbClassName(), descriptor.getName()}));
		}
		return result;
          
	    } else if (EjbSessionDescriptor.STATEFUL.equals(stateType)) {
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable1",
				      "{0} expected {1} Session bean, but called with {2} Session bean.",
				      new Object[] {getClass(),EjbSessionDescriptor.STATELESS,stateType}));
		return result;
	    } else {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failed4",
			       "Error: [ {0} ] is not valid stateType within bean [ {1} ].",
			       new Object[] {stateType, descriptor.getName()}));
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
				  new Object[] {getClass(),"Session","Entity"}));
	    return result;
	} 
    }
}
