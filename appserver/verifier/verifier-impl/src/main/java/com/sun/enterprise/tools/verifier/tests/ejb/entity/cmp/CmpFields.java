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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

/** 
 * Container-managed fields valid type test.
 *
 * The Bean Provider must ensure that the Java types assigned to the 
 * container-managed fields are one of the following: Java primitive types, 
 * Java serializable types, or references to enterprise beans' remote or home 
 * interfaces. 
 *
 */
public class CmpFields extends EjbTest implements EjbCheck { 


    /** 
     * Container-managed fields valid type test.
     *
     * The Bean Provider must ensure that the Java types assigned to the 
     * container-managed fields are one of the following: Java primitive types, 
     * Java serializable types, or references to enterprise beans' remote or home 
     * interfaces. 
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    String persistentType = 
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistentType)) {
                
                // this test apply only to 1.x cmp beans, in 2.x fields are virtual fields only
                if (EjbCMPEntityDescriptor.CMP_1_1 != ((EjbCMPEntityDescriptor) descriptor).getCMPVersion()) {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	            result.notApplicable(smh.getLocalString
				 ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.notApplicable3",
				  "Test do not apply to this cmp-version of container managed persistence EJBs"));
        	    return result;                    
                }   
                
		VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		// RULE: container-managed fields are one of the following: 
		// Java primitive types, Java serializable types, or references 
		// to enterprise beans' remote or home interfaces. 
		Set persistentFields = 
		    ((EjbCMPEntityDescriptor)descriptor).getPersistenceDescriptor().getCMPFields();
		Iterator iterator = persistentFields.iterator();
	  	
		Descriptor persistentField;
		Field field;
		boolean oneFailed = false;
		while (iterator.hasNext()) {
		    persistentField = (Descriptor)iterator.next();
		    boolean foundField = false;
		    try {                                            
			Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
			// start do while loop here....
			do {
			    try {                                            
			        Field f = c.getDeclaredField(persistentField.getName());
			        foundField = true;
			        Class persistentFieldClassType = f.getType();

				if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName()) &&
				   descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName())) {
				    if (RmiIIOPUtils.isPersistentFieldTypeValid(persistentFieldClassType,descriptor.getHomeClassName(),descriptor.getRemoteClassName())) {
					result.addGoodDetails(smh.getLocalString
							      ("tests.componentNameConstructor",
							       "For [ {0} ]",
							       new Object[] {compName.toString()}));
					result.addGoodDetails(smh.getLocalString
					(getClass().getName() + ".passed",
					"Valid type assigned to container managed field [ {0} ] found in bean [ {1} ]",
					new Object[] {((Descriptor)persistentField).getName(),c.getName()}));
				    } else {
					oneFailed = true;
					result.addErrorDetails(smh.getLocalString
							       ("tests.componentNameConstructor",
								"For [ {0} ]",
								new Object[] {compName.toString()}));
					result.addErrorDetails(smh.getLocalString
					   (getClass().getName() + ".failed",
					   "Error: Invalid type assigned to container managed field [ {0} ] found in bean [ {1} ]",
					    new Object[] {((Descriptor)persistentField).getName(),c.getName()}));
				    }
				}
				
				if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName()) && descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName())) {
				    if (RmiIIOPUtils.isPersistentFieldTypeValid(persistentFieldClassType,descriptor.getLocalHomeClassName(),descriptor.getLocalClassName())) {
					result.addGoodDetails(smh.getLocalString
							       ("tests.componentNameConstructor",
								"For [ {0} ]",
								new Object[] {compName.toString()}));
					result.addGoodDetails(smh.getLocalString
					(getClass().getName() + ".passed",
					"Valid type assigned to container managed field [ {0} ] found in bean [ {1} ]",
					new Object[] {((Descriptor)persistentField).getName(),c.getName()}));
				    } else {
					oneFailed = true;
					result.addErrorDetails(smh.getLocalString
							       ("tests.componentNameConstructor",
								"For [ {0} ]",
								new Object[] {compName.toString()}));
					result.addErrorDetails(smh.getLocalString
							       (getClass().getName() + ".failed",
								"Error: Invalid type assigned to container managed field [ {0} ] found in bean [ {1} ]",
					new Object[] {((Descriptor)persistentField).getName(),c.getName()}));
				    }
				}
				
			    } catch (NoSuchFieldException e) {
				foundField = false;
			    }
                        } while (((c = c.getSuperclass()) != null) && (!foundField));
		        if (!foundField) {
                            if (!oneFailed) {
			        oneFailed = true;
                            }
			    result.addErrorDetails(smh.getLocalString
						   ("tests.componentNameConstructor",
						    "For [ {0} ]",
						    new Object[] {compName.toString()}));
			    result.addErrorDetails(smh.getLocalString
						   (getClass().getName() + ".failedException1",
						    "Error: field [ {0} ] not found in class [ {1} ]",
						    new Object[] {((Descriptor)persistentField).getName(),descriptor.getEjbClassName()}));
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
				       new Object[] {descriptor.getEjbClassName()}));
		    }
		}
		if (oneFailed) {
		    result.setStatus(Result.FAILED);
		} else {
		    result.setStatus(Result.PASSED);
		}
	    } else { // if (BEAN_PERSISTENCE.equals(persistentType))
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable1",
				      "Expected persistence type [ {0} ], but [ {1} ] bean has persistence type [ {2} ]",
				      new Object[] {EjbEntityDescriptor.CONTAINER_PERSISTENCE,descriptor.getName(),persistentType}));
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
