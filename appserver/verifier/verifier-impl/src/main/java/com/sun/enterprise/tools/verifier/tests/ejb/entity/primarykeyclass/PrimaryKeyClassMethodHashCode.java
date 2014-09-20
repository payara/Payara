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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.primarykeyclass;

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
 * Primary key class provide implementation of hashCode() methods test.  
 *
 * Enterprise Bean's primary key class 
 * The class must provide suitable implementation of the hashCode() 
 * method to simplify the management of the primary keys by client code.
 *
 */
public class PrimaryKeyClassMethodHashCode extends EjbTest implements EjbCheck { 


    /** 
     * Primary key class provide implementation of hashCode() methods test.  
     *
     * Enterprise Bean's primary key class 
     * The class must provide suitable implementation of the hashCode() 
     * method to simplify the management of the primary keys by client code.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    String transactionType = descriptor.getTransactionType();
	    if (EjbDescriptor.CONTAINER_TRANSACTION_TYPE.equals(transactionType))
		{

		    boolean hasDefinedHashCodeMethod = false;
		    boolean oneFailed = false;
		    int lc = 0;

		    // RULE: Primary key class must defined HashCode() method
		    try {
			VerifierTestContext context = getVerifierContext();
			ClassLoader jcl = context.getClassLoader();
			// retrieve the EJB primary key class 
			Class c = Class.forName(((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName(), false, getVerifierContext().getClassLoader());
			Method methods[] = c.getDeclaredMethods();
			for (int i=0; i< methods.length; i++)
			    {
				if (methods[i].getName().equals("hashCode")){
				    // this is the right primary key class method hashCode()
				    hasDefinedHashCodeMethod = true;
				    // used in output below
				    lc = i;
				    break;
				}
			    }

			if (hasDefinedHashCodeMethod) 
			    {
				result.addGoodDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
				result.addGoodDetails(smh.getLocalString
						      (getClass().getName() + ".debug1",
						       "For EJB primary key class [ {0} ]",
						       new Object[] {((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName()}));
				result.addGoodDetails(smh.getLocalString
						      (getClass().getName() + ".passed",
						       "Primary key class method [ {0} ] was defined in the primary key class.",
						       new Object[] {methods[lc].getName()}));
			    } else if (!hasDefinedHashCodeMethod) {
				oneFailed = true;
				result.addErrorDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
				result.addErrorDetails(smh.getLocalString
						      (getClass().getName() + ".debug1",
						       "For EJB primary key class [ {0} ]",
						       new Object[] {((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName()}));
				result.addErrorDetails(smh.getLocalString
						       (getClass().getName() + ".failed",
							"Error: Primary key class method hashCode() was not defined in the primary key class."));
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
				       new Object[] {((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName(), descriptor.getName()})
				      );
		    }

		    if (oneFailed) 
			result.setStatus(result.FAILED);
		    else
			result.setStatus(result.PASSED);

		} else {
		    // not container managed, but is a entity bean
		    result.addNaDetails(smh.getLocalString
						  ("tests.componentNameConstructor",
						   "For [ {0} ]",
						   new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable2",
					  "Bean [ {0} ] is not {1} managed, it is [ {2} ] managed.",
					  new Object[] {descriptor.getName(),EjbDescriptor.CONTAINER_TRANSACTION_TYPE,transactionType}));
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
