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
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/**  
 * Entity Bean's with container managed persistence ejbCreate(...) and 
 * ejbPostCrete(...) methods must be defined to return the primary key class 
 * type. The implementation of the ejbCreate(...) methods should be coded to 
 * return a null. The returned value is ignored by the Container.
 * 
 * The method signatures must follow these rules: 
 * 
 * The return type must be defined to return the primary key class type.
 * 
 */
public class CmpEjbCreateMethod extends EjbTest implements EjbCheck { 


    /** 
     * Entity Bean's with container managed persistence ejbCreate(...) and 
     * ejbPostCrete(...) methods must be defined to return the primary key class
     * type. The implementation of the ejbCreate(...) methods should be coded to
     * return a null. The returned value is ignored by the Container.
     * 
     * The method signatures must follow these rules: 
     * 
     * The return type must be defined to return the primary key class type.
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

		boolean oneFailed = false;
		int foundAtLeastOne = 0;
		try {
		    VerifierTestContext context = getVerifierContext();
		    ClassLoader jcl = context.getClassLoader();
		    Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
		    Method [] methods1 = c.getDeclaredMethods();
		    int loopCounter = 0;
  
		    boolean ejbCreateFound = false;
		    boolean returnsPrimaryKeyClassType = false;
                    // start do while loop here....
                    do {
		        Method [] methods = c.getDeclaredMethods();
		        methods1 = null;
		        methods1 = methods;
			for (int i = 0; i < methods.length; i++) {
			    // reset flags from last time thru loop
			    ejbCreateFound = false;
			    loopCounter++;
			    returnsPrimaryKeyClassType = false;
  
			    // The method name must be ejbCreate. 
			    if (methods[i].getName().startsWith("ejbCreate")) {
				foundAtLeastOne++;
				ejbCreateFound = true;
  
				// The return type must be be defined to 
				// return the primary key class type
				Class rt = methods[i].getReturnType();
				Class str = Class.forName(((EjbEntityDescriptor)descriptor).getPrimaryKeyClassName());
				do {
				    if (rt.getName().equals(str.getName())) {
					returnsPrimaryKeyClassType = true;
				    }
				    str = str.getSuperclass();
				}while( str != null && returnsPrimaryKeyClassType != true ) ;
  
				// now display the appropriate results for this particular ejbCreate
				// method
				if (ejbCreateFound && returnsPrimaryKeyClassType) {
				    result.addGoodDetails(smh.getLocalString
							   ("tests.componentNameConstructor",
							    "For [ {0} ]",
							    new Object[] {compName.toString()}));
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".debug1",
							   "For EJB Class [ {0} ] ejbCreate(...) Method [ {1} ]",
							   new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".passed",
							   "[ {0} ] properly returns the primary key class type.",
							   new Object[] {descriptor.getEjbClassName()}));
				} else if (ejbCreateFound && !returnsPrimaryKeyClassType) {
            			    oneFailed = true;
				    result.addErrorDetails(smh.getLocalString
							   ("tests.componentNameConstructor",
							    "For [ {0} ]",
							    new Object[] {compName.toString()}));
	            		    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".debug1",
							    "For EJB Class [ {0} ] ejbCreate(...) Method [ {1} ]",
							    new Object[] {descriptor.getEjbClassName(),methods1[loopCounter].getName()}));
		            	    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".failed",
							    "Error: An ejbCreate(...) method was found, but did not properly return the primary key class type."));
				} 
			    }
			}
                    } while (((c = c.getSuperclass()) != null) && (!(ejbCreateFound && returnsPrimaryKeyClassType)));
          
		    if (foundAtLeastOne == 0) {
			result.addNaDetails(smh.getLocalString
					    ("tests.componentNameConstructor",
					     "For [ {0} ]",
					     new Object[] {compName.toString()}));
			result.notApplicable(smh.getLocalString
					     (getClass().getName() + ".notApplicable1",
					      "[ {0} ] does not declare any ejbCreate(...) methods.",
					      new Object[] {descriptor.getEjbClassName()}));
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
  
		if (oneFailed) {
		    result.setStatus(result.FAILED);
		} else if (foundAtLeastOne == 0)  {
		    result.setStatus(result.NOT_APPLICABLE);
		} else {
		    result.setStatus(result.PASSED);
		}
  
		return result;

	    } else { // if (BEAN_PERSISTENCE.equals(persistence)) {
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable2",
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
