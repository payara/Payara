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
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.util.Enumeration;


/** Session/Entity Bean bean-managed transaction demarcation type test.  
 * The Application Assembler must not define transaction attributes for an 
 * enterprise bean with bean-managed transaction demarcation.
 */
public class TransactionDemarcationBeanManaged extends EjbTest implements EjbCheck { 


    /** Session/Entity Bean bean-managed transaction demarcation type test.  
     * The Application Assembler must not define transaction attributes for an 
     * enterprise bean with bean-managed transaction demarcation.
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
	try {
	    // The Application Assembler must not define transaction attributes for an 
	    // enterprise bean with bean-managed transaction demarcation.
	    if ((descriptor instanceof EjbSessionDescriptor) ||
		(descriptor instanceof EjbEntityDescriptor)) {
		String transactionType = descriptor.getTransactionType();
		if (EjbDescriptor.BEAN_TRANSACTION_TYPE.equals(transactionType)) {
		    ContainerTransaction containerTransaction = null;
                    if (!descriptor.getMethodContainerTransactions().isEmpty()) {
                        for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
			    MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();
                            containerTransaction = 
                                                (ContainerTransaction) descriptor.getMethodContainerTransactions().get(methodDescriptor);
  		    
			    try {
				String transactionAttribute  = 
				    containerTransaction.getTransactionAttribute();
    
				// danny is doing this in the DOL, but is it possible to not have 
				// any value for containerTransaction.getTransactionAttribute() 
				// in the DOL? if it is possible to have blank value for this, 
				// then this check is needed here, otherwise we are done and we 
				// don't need this check here
				if (ContainerTransaction.NOT_SUPPORTED.equals(transactionAttribute)
				    || ContainerTransaction.SUPPORTS.equals(transactionAttribute)
				    || ContainerTransaction.REQUIRED.equals(transactionAttribute)
				    || ContainerTransaction.REQUIRES_NEW.equals(transactionAttribute)
				    || ContainerTransaction.MANDATORY.equals(transactionAttribute)
				    || ContainerTransaction.NEVER.equals(transactionAttribute)
				    || (!transactionAttribute.equals(""))) {
				    result.addErrorDetails(smh.getLocalString
							   ("tests.componentNameConstructor",
							    "For [ {0} ]",
							    new Object[] {compName.toString()}));
				    result.failed(smh.getLocalString
						  (getClass().getName() + ".failed",
						   "Error: TransactionAttribute [ {0} ] for method [ {1} ] is not valid.   The Application Assembler must not define transaction attributes for an enterprise bean [ {2} ] with bean-managed transaction demarcation.",
						   new Object[] {transactionAttribute, methodDescriptor.getName(),descriptor.getName()}));
				} else {
				    result.addGoodDetails(smh.getLocalString
							  ("tests.componentNameConstructor",
							   "For [ {0} ]",
							   new Object[] {compName.toString()}));
				    result.passed(smh.getLocalString
						  (getClass().getName() + ".passed",
						   "Valid: TransactionAttribute [ {0} ] for method [ {1} ] is not defined for an enterprise bean [ {2} ] with bean-managed transaction demarcation.",
						   new Object[] {transactionAttribute, methodDescriptor.getName(),descriptor.getName()}));
				} 
			    } catch (NullPointerException e) {
				result.addGoodDetails(smh.getLocalString
						      ("tests.componentNameConstructor",
						       "For [ {0} ]",
						       new Object[] {compName.toString()}));
				result.passed(smh.getLocalString
					      (getClass().getName() + ".passed1",
					       "Valid: TransactionAttribute is null for method [ {0} ] in bean [ {1} ]",
					       new Object[] {methodDescriptor.getName(),descriptor.getName()}));
				return result;
			    }
			}
		    } else {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.passed(smh.getLocalString
				     (getClass().getName() + ".passed2",
				      "Valid: There are no method permissions within this bean [ {0} ]", 
				      new Object[] {descriptor.getName()}));
		    }
		    return result; 
		} else {
		    // not container managed, but is a session/entity bean
		    result.addNaDetails(smh.getLocalString
					("tests.componentNameConstructor",
					 "For [ {0} ]",
					 new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable2",
					  "Bean [ {0} ] is not [ {1} ] managed, it is [ {2} ] managed.", 
					  new Object[] {descriptor.getName(),EjbDescriptor.BEAN_TRANSACTION_TYPE,transactionType}));
		}
		return result;
	    } else {
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "[ {0} ] not called \n with a Session or Entity bean.",
				      new Object[] {getClass()}));
		return result;
	    } 
	} catch (Throwable t) {
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
			   new Object[] {descriptor.getName(), t.getMessage(), descriptor.getName()}));
	    return result;
	}
    }
}
