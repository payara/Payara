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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.pkmultiplefield;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/** 
 * Enterprise Java Bean primary key class constuctor test.  
 * The primary key class must have a public constructor that takes no 
 * parameters.
 */
public class PrimaryKeyClassConstructor extends EjbTest implements EjbCheck { 


    /**
     * Enterprise Java Bean primary key class constuctor test.  
     * The primary key class must have a public constructor that takes no 
     * parameters.
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
		// do we have  primekey that maps to single or multiple fields in entity        // bean class?  if primekey-field exist, then primekey maps to single
		// field in entity bean class and this test in notApplicable
		try {
                    FieldDescriptor fd = ((EjbCMPEntityDescriptor)descriptor).getPrimaryKeyFieldDesc();
                    if (fd != null) {
                        String pkf = fd.getName();
                        if (pkf.length() > 0) {
			    // N/A case
			    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.notApplicable(smh.getLocalString
					         (getClass().getName() + ".notApplicable2",
					          "Entity Bean [ {0} ] with primekey-field non-blank, test not applicable.",
					          new Object[] {descriptor.getEjbClassName()}));
                        }
		    } else {
			try {
			    VerifierTestContext context = getVerifierContext();
			    ClassLoader jcl = context.getClassLoader();
			    Class c = Class.forName(((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName(), false, getVerifierContext().getClassLoader());
      
			    boolean foundOne = false;
			    Constructor [] constructors = c.getConstructors();
			    for (int i = 0; i < constructors.length; i++) {
				int modifiers = constructors[i].getModifiers();
				if (Modifier.isPublic(modifiers)) {
				    Class [] constructorParameterTypes;
				    constructorParameterTypes = constructors[i].getParameterTypes();
				    if (constructorParameterTypes.length > 0) {
					continue;
				    } else {
					foundOne = true;
					break;
				    }
				}
			    }
          
			    if (foundOne) {
				result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				result.passed(smh.getLocalString
					      (getClass().getName() + ".passed",
					       "This primary key class [ {0} ] has a public constructor method with no parameters.",
					       new Object[] {((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName()}));
			    } else {
				result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				result.failed(smh.getLocalString
					      (getClass().getName() + ".failed",
					       "Error: A public constructor method with no parameters was not found in the primary key class [ {0} ].",  
					       new Object[] {((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName()}));
			    }
			} catch (ClassNotFoundException e) {
			    Verifier.debug(e);
			    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.failed(smh.getLocalString
					  (getClass().getName() + ".failedException",
					   "Error: [ {0} ] class not found.",
					   new Object[] {((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName()}));
			}  
		    }  
		} catch (NullPointerException e) {
		    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
					 (getClass().getName() + ".failedException2",
					  "Error: Primkey field not defined within [ {0} ] bean.",
					  new Object[] {descriptor.getName()}));
		}
		return result;
 
	    } else { //if (BEAN_PERSISTENCE.equals(persistence)) {
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable1",
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
				  "{0} expected {1} bean, but called with {2}.",
				  new Object[] {getClass(),"Entity","Session"}));
	    return result;
	}
    }
}
