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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.util.Enumeration;

/** 
 * ContainerTransaction Style 2 - Each container transaction element consists 
 * of a list of one or more method elements, and the trans-attribute element. 
 * The container transaction element specifies that all the listed methods are 
 * assigned the specified transaction attribute value.
 *
 * Style 2: 
 *    <method> 
 *      <ejb-name> EJBNAME</ejb-name> 
 *      <method-name>METHOD</method-name> 
 *    </method> 
 * This style is used for referring to a specified method of the remote or home
 * interface of the specified enterprise bean. If there are multiple methods 
 * with the same overloaded name, this style refers to all the methods with the
 * same name. There must be at most one container transaction element that uses
 * the Style 2 method element for a given method name.  If there is also a 
 * container transaction element that uses Style 1 element for the same bean, 
 * the value specified by the Style 2 element takes precedence.
 */
public class ContainerTransactionStyle2 extends EjbTest implements EjbCheck { 

    /**
     * Each container transaction element consists of a list of one or more 
     * method elements, and the trans-attribute element. The container transaction 
     * element specifies that all the listed methods are assigned the specified 
     * transaction attribute value.
     *
     * Style 2: 
     *    <method> 
     *      <ejb-name> EJBNAME</ejb-name> 
     *      <method-name>METHOD</method-name> 
     *    </method> 
     * This style is used for referring to a specified method of the remote or home
     * interface of the specified enterprise bean. If there are multiple methods 
     * with the same overloaded name, this style refers to all the methods with the
     * same name. There must be at most one container transaction element that uses
     * the Style 2 method element for a given method name.  If there is also a 
     * container transaction element that uses Style 1 element for the same bean, 
     * the value specified by the Style 2 element takes precedence.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	// hack try/catch block around test, to exit gracefully instead of
	// crashing verifier on getMethodDescriptors() call, XML mods cause
	// java.lang.ClassNotFoundException: verifier.ejb.hello.BogusEJB
	// Replacing <ejb-class>verifier.ejb.hello.HelloEJB with
	//  <ejb-class>verifier.ejb.hello.BogusEJB...
	try  {
	    boolean oneFailed = false;
            if (!descriptor.getMethodContainerTransactions().isEmpty()) {
	        int na = 0;
		for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
		    MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();
                    // see if it's a style 2
                    if (methodDescriptor.getParameterClassNames() == null) {
	                int foundIt = 0;

                        // found style 2, now see if it's the only one 
                        // for given method name
                        for (Enumeration eee = descriptor.getMethodContainerTransactions().keys(); eee.hasMoreElements();) {

		            MethodDescriptor matchingMethodDescriptor = (MethodDescriptor) eee.nextElement();
                            // see if this md is style 2 ?
                            if (matchingMethodDescriptor.getParameterClassNames() == null) {
                                // now see if it's the same name as previously
                                // encountered method name
		                if (methodDescriptor.getName().equals(matchingMethodDescriptor.getName())) {
			            foundIt++;
                                }
                            }
                        }  // report after this inner loop

                         
		        // report for this particular set of Container tx's
                        // DOL only saves one container tx with "*", so can't fail...
		        if (foundIt == 1) {
			    result.addGoodDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
		            result.passed(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "Container Transaction method name [ {0} ] defined only once in [ {1} ] bean.",
					   new Object[] {methodDescriptor.getName(), descriptor.getName()}));
		        } else if (foundIt > 1) {
                            if (!oneFailed) {
	                        oneFailed = false;
                            }
			    result.addErrorDetails(smh.getLocalString
						   ("tests.componentNameConstructor",
						    "For [ {0} ]",
						    new Object[] {compName.toString()}));
		            result.failed(smh.getLocalString
					  (getClass().getName() + ".failed",
					   "Error: Container Transaction method name [ {0} ] is defined [ {1} ] times in [ {2} ] bean.  Method name container transaction style{3} is allowed only once per bean.",
					   new Object[] {methodDescriptor.getName(), new Integer(foundIt), descriptor.getName(),new Integer(2)}));
		        } else {
                            na++;
			    result.addNaDetails(smh.getLocalString
						("tests.componentNameConstructor",
						 "For [ {0} ]",
						 new Object[] {compName.toString()}));
		            result.notApplicable(smh.getLocalString
						 (getClass().getName() + ".notApplicable1",
						  "Container Transaction method name [ {0} ] not defined in [ {1} ] bean.",
						  new Object[] {methodDescriptor.getName(), descriptor.getName()}));
		        } 
		    } else { // not a style 2
                        na++;
			result.addNaDetails(smh.getLocalString
					    ("tests.componentNameConstructor",
					     "For [ {0} ]",
					     new Object[] {compName.toString()}));
		        result.notApplicable(smh.getLocalString
					     (getClass().getName() + ".notApplicable2",
					      "Container Transaction method name [ {0} ] not defined as style{1} container transaction within [ {2} ].",
					      new Object[] {methodDescriptor.getName(), new Integer(2), descriptor.getName()}));
                    }
		    
		}  // for all method container tx's

                if (oneFailed) {
                    result.setStatus(result.FAILED);
                } else if (na == descriptor.getMethodContainerTransactions().size()){
                    result.setStatus(result.NOT_APPLICABLE);
                } else {
                    result.setStatus(result.PASSED);
                }
	        return result; 
	    } else {  // if !descriptor.getMethodContainerTransactions().isEmpty
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "There are no container transactions within this bean [ {0} ]", 
				      new Object[] {descriptor.getName()}));
	        return result; 
	    }
	} catch (Throwable t) {
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException2",
			   "Error: [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
			   new Object[] {descriptor.getName(), t.getMessage(), descriptor.getName()}));
	    return result;
	}
    }
}
