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

package com.sun.enterprise.tools.verifier.tests.ejb.session;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

/** 
 * Session Bean Transaction demarcation type test.  
 * For bean managed session beans, it doesn't make sense to have 
 * container transactions.
 */
public class TransactionTypeNullForContainerTX extends EjbTest implements EjbCheck { 


    /** 
     * Session Bean Transaction demarcation type test.  
     * For bean managed session beans, it doesn't make sense to have 
     * container transactions.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbSessionDescriptor) {
	    String transactionType = descriptor.getTransactionType();
	    if (EjbDescriptor.BEAN_TRANSACTION_TYPE.equals(transactionType)) {
		// taken from DOL - remember that for bean managed session beans, 
		// it doesn't make sense to have container transactions
		// you'll have to enforce this in the object model somewhere, 
		// and in the UI
                try {
		    if (descriptor.getMethodContainerTransactions().size() > 0) {
		        // shouldn't have container transaction for bean managed session 
		        // since container transaction is not null, it's defined, we fail
		        // test
		        result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.failed(smh.getLocalString
				      (getClass().getName() + ".failed",
				       "Error: Session Beans [ {0} ] with [ {1} ] managed \n" +
				       "transaction demarcation should not have container \n" +
				       "transactions defined.",
				       new Object[] {descriptor.getName(),transactionType}));
		    } else {
		        // container transaction is null, not defined, which is correct
		        // shouldn't have container transaction for bean managed session 
		        result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.passed(smh.getLocalString
				      (getClass().getName() + ".passed",
				       "This session bean [ {0} ] is [ {1} ] managed and correctly declares no container transactions.",
				       new Object[] {descriptor.getName(),transactionType}));
		    }
		    return result;
                } catch (NullPointerException e) {
		    // container transaction is null, not defined, which is correct
		    // shouldn't have container transaction for bean managed session 
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.passed(smh.getLocalString
				  (getClass().getName() + ".passed",
				   "This session bean [ {0} ] is [ {1} ] managed and correctly declares no container transactions.",
				   new Object[] {descriptor.getName(),transactionType}));
		    return result;
		}
		
	    } else {
		// not bean/container managed, but is a session/entity bean
		// (i.e it's CONTAINER_TRANSACTION_TYPE)
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable1",
				      "Session bean [ {0} ], expected [ {1} ] managed, but called with [ {2} ] managed.",
				      new Object[] {descriptor.getName(),EjbDescriptor.BEAN_TRANSACTION_TYPE, EjbDescriptor.CONTAINER_TRANSACTION_TYPE}));
		return result;
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} \n bean, but called with {2} bean.",
				  new Object[] {getClass(),"Session","Entity"}));
	    return result;
	} 
    }
}
