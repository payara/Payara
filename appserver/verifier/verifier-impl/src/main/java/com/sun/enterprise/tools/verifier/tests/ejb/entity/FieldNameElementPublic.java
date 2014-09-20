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

package com.sun.enterprise.tools.verifier.tests.ejb.entity;

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

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.logging.Level;

/** 
 * The field-name element name must be a public field of the enterprise bean 
 * class or one of its superclasses. 
 */
public class FieldNameElementPublic extends EjbTest implements EjbCheck { 

    /**
     * The field-name element name must be a public field of the enterprise bean
     * class or one of its superclasses.
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
                if (EjbCMPEntityDescriptor.CMP_1_1!=((EjbCMPEntityDescriptor) descriptor).getCMPVersion()) {
		    result.addNaDetails(smh.getLocalString
					("tests.componentNameConstructor",
					 "For [ {0} ]",
					 new Object[] {compName.toString()}));
	            result.notApplicable(smh.getLocalString
				 ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.notApplicable3",
				  "Test do not apply to this cmp-version of container managed persistence EJBs"));
        	    return result;                    
                }   
                
		// RULE: Entity w/Container managed persistence bean provider must 
		//       specify container managed fields in the persistent-fields 
		//       element. The field-name element name must be a public field 
		//       of the enterprise bean class or one of its superclasses. 

		boolean resolved = false;
		boolean oneFailed = false;
		
		if (!((EjbCMPEntityDescriptor)descriptor).getPersistenceDescriptor().getCMPFields().isEmpty()) {
		    // check class to get all fields that actually exist
		    try {
			VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();

			for (Iterator itr = 
				 ((EjbCMPEntityDescriptor)descriptor).getPersistenceDescriptor().getCMPFields().iterator();
			     itr.hasNext();) {

			    FieldDescriptor nextPersistentField = (FieldDescriptor)itr.next();
			    String fieldName = nextPersistentField.getName();
			    Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());

                            // start do while loop here....
			    do {
				// Returns an array containing Field objects
				// reflecting all the accessible public fields of the class 
				// or interface represented by this Class object.
				Field fields[] = c.getFields();
    
				//loop thru all field array elements and ensure fieldName exist
				for (int i=0; i < fields.length; i++) {
				    if (fieldName.equals(fields[i].getName())) {
					resolved = true;
					logger.log(Level.FINE, getClass().getName() + ".debug1",
                            new Object[] {fieldName,fields[i].toString(),c.getName()});
					result.addGoodDetails(smh.getLocalString
							      ("tests.componentNameConstructor",
							       "For [ {0} ]",
							       new Object[] {compName.toString()}));
					result.addGoodDetails
					    (smh.getLocalString
					     (getClass().getName() + ".passed",
					      "[ {0} ] found in public fields of bean [ {1} ]",
					      new Object[] {fieldName,c.getName()}));
					break;
				    } else {					
                        logger.log(Level.FINE, getClass().getName() + ".debug",
                             new Object[] {fieldName,fields[i].toString(),c.getName()});   
				    }
				}
			    } while (((c = c.getSuperclass()) != null) && (!resolved));
 
			    // before you go onto the next field name, tell me whether you
			    // resolved the last field name okay
			    if (!resolved) {
				if (!oneFailed) {
				    oneFailed = true;
				}
				result.addErrorDetails(smh.getLocalString
						       ("tests.componentNameConstructor",
							"For [ {0} ]",
							new Object[] {compName.toString()}));
				result.addErrorDetails
				    (smh.getLocalString
				     (getClass().getName() + ".failed1",
				      "Error: [ {0} ] not found in public fields of bean [ {1} ]",
				      new Object[] {fieldName,descriptor.getEjbClassName()}));
			    }
			    // clear the resolved flag for the next field name
			    if (resolved) {
				resolved = false;
			    }
			}
 
			if (oneFailed) {
			    result.setStatus(Result.FAILED);
			} else {
			    result.setStatus(Result.PASSED);
			}

		    } catch (Exception e) {
			Verifier.debug(e);
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.failed  
			    (smh.getLocalString
			     (getClass().getName() + ".failedException",
			      "Error: [ {0} ] within bean [ {1} ]",
			      new Object[] {e.getMessage(),descriptor.getName()}));
		    }
		} else {
		    // persistent fields are empty
		    result.addNaDetails(smh.getLocalString
					("tests.componentNameConstructor",
					 "For [ {0} ]",
					 new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  "No persistent fields are defined for bean [ {0} ]",
					  new Object[] {descriptor.getName()}));
		}
	    } else { //BEAN_PERSISTENCE.equals(persistentType)
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable2",
				      "Expected [ {0} ] managed persistence, but [ {1} ] bean has [ {2} ] managed persistence.",
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
				  "{0} expected \n {1} bean, but called with {2} bean",
				  new Object[] {getClass(),"Entity","Session"}));
	    return result;
	} 
    }
}
