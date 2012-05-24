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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.pksinglefield;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

/** 
 * The primkey-field must be one of the fields declared in the cmp-field
 * elements.
 */
public class PrimekeyFieldPersistentFields extends EjbTest implements EjbCheck { 


    /** 
     * The primkey-field must be one of the fields declared in the cmp-field
     * elements.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	// The primkey-field must be one of the fields declared in the 
	// cmp-field elements
	if (descriptor instanceof EjbEntityDescriptor) {
	    String persistence =
		((EjbEntityDescriptor)descriptor).getPersistenceType();

	    if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistence)) {
		try {
		    // do i need to use this to help determine single vs. multiple 
		    // object finders, etc.
		    String primkey =
			((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName();
                    if (primkey.equals("java.lang.String")) {
                        try {
  
		            FieldDescriptor primField =
			        ((EjbCMPEntityDescriptor)descriptor).getPrimaryKeyFieldDesc();
  
		            // primField must exist in order to be valid & pass test
		            Descriptor persistentField;
		            Field field;
		            Set persistentFields =
			        ((EjbCMPEntityDescriptor)descriptor).getPersistenceDescriptor().getCMPFields();
		            Iterator iterator = persistentFields.iterator();
		            boolean foundMatch = false;
		            while (iterator.hasNext()) {
			        persistentField = (Descriptor)iterator.next();
			        if (primField != null) {
			            if (primField.getName().equals(persistentField.getName())) {
			                foundMatch = true;
			                break;
			            } else {
			                continue;
			            }
		                } else {
                                    // should already be set, can't ever be in cmp 
                                    // fields if primField doesn't exist
			            foundMatch = false;
			            break;
		                }
		            }
		            if (foundMatch) {
			        result.addGoodDetails(smh.getLocalString
						      ("tests.componentNameConstructor",
						       "For [ {0} ]",
						       new Object[] {compName.toString()}));
				result.passed(smh.getLocalString
				              (getClass().getName() + ".passed",
				               "Primary key field [ {0} ] is defined within set of container managed fields for bean [ {1} ]",
				               new Object[] {primField.getName(),descriptor.getName()}));
		            } else {
			        if (primField != null) {
			            result.addErrorDetails(smh.getLocalString
							   ("tests.componentNameConstructor",
							    "For [ {0} ]",
							    new Object[] {compName.toString()}));
				    result.failed(smh.getLocalString
					          (getClass().getName() + ".failed",
					           "Primary key field [ {0} ] is not defined within set of container managed fields for bean [ {1} ]",
					           new Object[] {primField.getName(),descriptor.getName()}));
			        } else {
                                    // unless special case, where primary key class
                                    // is java.lang.Object, then test should be N/A
                                    // not failed
                                    try {
                                        if (((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName().equals("java.lang.Object")) {

					    result.addNaDetails(smh.getLocalString
								("tests.componentNameConstructor",
								 "For [ {0} ]",
								 new Object[] {compName.toString()}));
		                            result.notApplicable(smh.getLocalString
								 (getClass().getName() + ".notApplicable2",
								  "Primkey field not defined for [ {0} ] bean.",
								  new Object[] {descriptor.getName()}));
                                    
                                        } else {
			                    result.addErrorDetails(smh.getLocalString
								   ("tests.componentNameConstructor",
								    "For [ {0} ]",
								    new Object[] {compName.toString()}));
					    result.failed(smh.getLocalString
							  (getClass().getName() + ".failed1",
							   "Primary key field is not defined within set of container managed fields for bean [ {0} ]",
							   new Object[] {descriptor.getName()}));
			                }		       
		                    } catch (NullPointerException e) {
					result.addNaDetails(smh.getLocalString
							    ("tests.componentNameConstructor",
							     "For [ {0} ]",
							     new Object[] {compName.toString()}));
		                        result.notApplicable(smh.getLocalString
							     (getClass().getName() + ".notApplicable2",
							      "Primkey field not defined for [ {0} ] bean.",
							      new Object[] {descriptor.getName()}));
		                    }
			        }		       
		            }
                        } catch (NullPointerException e) {
                            result.addErrorDetails(smh.getLocalString
						("tests.componentNameConstructor",
						 "For [ {0} ]",
						 new Object[] {compName.toString()}));
			    result.failed
                                (smh.getLocalString
                                 (getClass().getName() + ".failed2",
                                  "Error: Primary Key Field must be defined for bean [ {0} ] with primary key class set to [ {1} ]",
                                  new Object[] {descriptor.getName(),primkey}));
                        }
                    } else {
			result.addNaDetails(smh.getLocalString
					    ("tests.componentNameConstructor",
					     "For [ {0} ]",
					     new Object[] {compName.toString()}));
                        result.notApplicable(smh.getLocalString
					     (getClass().getName() + ".notApplicable3",
					      "primkey [ {0} ] is not java.lang.String for bean [ {1} ]",
					      new Object[] {primkey,descriptor.getName()}));
                    }
		} catch (NullPointerException e) {
		    result.addNaDetails(smh.getLocalString
					("tests.componentNameConstructor",
					 "For [ {0} ]",
					 new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable2",
					  "Primkey field not defined for [ {0} ] bean.",
					  new Object[] {descriptor.getName()}));
		}
	    } else {
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable1",
				      "Expected [ {0} ] managed persistence, but [ {1} ] bean has [ {2} ] managed persistence",
				      new Object[] {EjbEntityDescriptor.CONTAINER_PERSISTENCE,descriptor.getName(),persistence}));
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "{0} expected \n {1} bean, but called with {2} bean",
				  new Object[] {getClass(),"Entity","Session"}));
	}

	return result;
    }
}
