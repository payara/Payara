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
 * ContainerTransaction Style 1 - Each container transaction element consists 
 * of a list of one or more method elements, and the trans-attribute element. 
 * The container transaction element specifies that all the listed methods are 
 * assigned the specified transaction attribute value.
 *
 * Style 1: 
 *    <method> 
 *      <ejb-name> EJBNAME</ejb-name> 
 *      <method-name>*</method-name> 
 *    </method> 
 * This style is used to specify a default value of the transaction attribute 
 * for the methods for which there is no Style 2 or Style 3 element specified. 
 * There must be at most one container transaction element that uses the Style 1
 * method element for a given enterprise bean.
 */
public class ContainerTransactionStyle1 extends EjbTest implements EjbCheck { 


    /**
     * Each container transaction element consists of a list of one or more 
     * method elements, and the trans-attribute element. The container transaction 
     * element specifies that all the listed methods are assigned the specified 
     * transaction attribute value.
     *
     * Style 1: 
     *    <method> 
     *      <ejb-name> EJBNAME</ejb-name> 
     *      <method-name>*</method-name> 
     *    </method> 
     * This style is used to specify a default value of the transaction attribute 
     * for the methods for which there is no Style 2 or Style 3 element specified. 
     * There must be at most one container transaction element that uses the Style 1
     * method element for a given enterprise bean.
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
	    boolean na = false;
	    int foundWildCard = 0;
            if (!descriptor.getMethodContainerTransactions().isEmpty()) {
		for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
		    MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();
  
		    if (methodDescriptor.getName().equals(MethodDescriptor.ALL_METHODS)) {
			foundWildCard++;
		    }
		}

		// report for this particular set of Container tx's
                // DOL only saves one container tx with "*", so can't fail...
		if (foundWildCard == 1) {
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.passed(smh.getLocalString
				  (getClass().getName() + ".passed",
				   "Container Transaction method name [ {0} ] defined only once in [ {1} ] bean.",
				   new Object[] {MethodDescriptor.ALL_METHODS, descriptor.getName()}));
		} else if (foundWildCard > 1) {
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failed",
				   "Error: Container Transaction method name [ {0} ] is defined [ {1} ] times in [ {2} ] bean.  Method name container transaction style [ {3} ] is allowed only once per bean.",
				   new Object[] {MethodDescriptor.ALL_METHODS, new Integer(foundWildCard), descriptor.getName(),MethodDescriptor.ALL_METHODS}));
		} else {
		    result.addNaDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  "Container Transaction method name [ {0} ] not defined in [ {1} ] bean.",
					  new Object[] {MethodDescriptor.ALL_METHODS, descriptor.getName()}));
		} 
		
	    } else {  // if (methodDescriptorsIterator.hasNext())
		result.addNaDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "There are no method permissions within this bean [ {0} ]", 
				      new Object[] {descriptor.getName()}));
	    }
	    return result; 
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
