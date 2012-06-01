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
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

/** 
 * Entity bean's Primary Key Class type test. 
 * If the enterprise bean is a Entity Bean, the Bean provider specifies
 * the fully qualified name of the Entity bean's primary key class in the 
 * "primary-class" element. The Bean provider 'must' specify the primary key
 * class for an Entity with bean managed persistence, and 'may' (but is not
 * required to) specify the primary key class for an Entity with 
 * Container-managed persistence. 
 *
 * Special case: Unknown primary key class
 * In special situations, the Bean Provider may choose not to specify the 
 * primary key class for an entity bean with container-managed persistence. This
 * case happens if the Bean Provider wants to allow the Deployer to select the 
 * primary key fields at deployment time. The Deployer uses instructions 
 * supplied by the Bean Provider (these instructions are beyond the scope of 
 * the EJB spec.) to define a suitable primary key class.
 *  
 * In this special case, the type of the argument of the findByPrimaryKey method
 * must be declared as java.lang.Object, and the return value of ejbCreate() 
 * must be declared as java.lang.Object. The Bean Provider must specify the 
 * primary key class in the deployment descriptor as of the type 
 * java.lang.Object.
 *  
 * The primary key class is specified at deployment time when the Bean Provider
 * develops enterprise beans that is intended to be used with multiple back-ends
 * that provide persistence, and when these multiple back-ends require different
 * primary key structures.
 */
public class PrimaryKeyClassOptType extends EjbTest implements EjbCheck { 


    /** 
     * Entity bean's Primary Key Class type test.
     * If the enterprise bean is a Entity Bean, the Bean provider specifies
     * the fully qualified name of the Entity bean's primary key class in the
     * "primary-class" element. The Bean provider 'must' specify the primary key
     * class for an Entity with bean managed persistence, and 'may' (but is not
     * required to) specify the primary key class for an Entity with
     * Container-managed persistence.
     *
     * Special case: Unknown primary key class
     * In special situations, the Bean Provider may choose not to specify the 
     * primary key class for an entity bean with container-managed persistence. This
     * case happens if the Bean Provider wants to allow the Deployer to select the 
     * primary key fields at deployment time. The Deployer uses instructions 
     * supplied by the Bean Provider (these instructions are beyond the scope of 
     * the EJB spec.) to define a suitable primary key class.
     *  
     * In this special case, the type of the argument of the findByPrimaryKey method
     * must be declared as java.lang.Object, and the return value of ejbCreate() 
     * must be declared as java.lang.Object. The Bean Provider must specify the 
     * primary key class in the deployment descriptor as of the type 
     * java.lang.Object.
     *  
     * The primary key class is specified at deployment time when the Bean Provider
     * develops enterprise beans that is intended to be used with multiple back-ends
     * that provide persistence, and when these multiple back-ends require different
     * primary key structures.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    String persistence = 
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistence)) {
		String primkey = 
		    ((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName();

		// primkey can be not set, via setting xml element
                // <prim-key-class> to "java.lang.Object"
                if (primkey.equals("java.lang.Object")) {
		    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));	
		    result.passed(smh.getLocalString
				  (getClass().getName() + ".passed",
				   "Properly defined primary key class type [ {0} ]",
				   new Object[] {"java.lang.Object"}));
		} else {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  "Primary Key Class is [ {0} ]",
					  new Object[] {primkey}));
		}

		return result;

	    } else if (EjbEntityDescriptor.BEAN_PERSISTENCE.equals(persistence)) {
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable2",
				      "Entity Bean with [ {0} ] managed persistence, primkey mandatory.",
				      new Object[] {persistence}));
		return result;
	    } 
	    else {
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable3",
				      "Expected [ {0} ] managed persistence, but [ {1} ] bean has [ {2} ] managed persistence.",
				      new Object[] {EjbEntityDescriptor.CONTAINER_PERSISTENCE,descriptor.getName(),persistence}));
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
}
