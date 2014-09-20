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

package com.sun.enterprise.tools.verifier.tests.ejb.session.ejbcreatemethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

/**
 * Session beans home interface create method match bean class test.
 * 
 * The following are the requirements for the Session Bean's home interface 
 * signature: 
 * 
 * A Session Bean's home interface defines one or more create(...) methods. 
 * 
 * Each create method must be named ``create'', and it must match one of the 
 * ejbCreate methods defined in the enterprise Bean class. The matching 
 * ejbCreate method must have the same number and types of arguments. 
 * 
 */
public class EjbCreateMatchesCreate extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    boolean foundAtLeastOneCreate = false;

    /** 
     * Session beans home interface create method match bean class test.
     * 
     * The following are the requirements for the Session Bean's home interface 
     * signature: 
     * 
     * A Session Bean's home interface defines one or more create(...) methods. 
     * 
     * Each create method must be named ``create'', and it must match one of the 
     * ejbCreate methods defined in the enterprise Bean class. The matching 
     * ejbCreate method must have the same number and types of arguments. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbSessionDescriptor) {
            
            if (((descriptor.getHomeClassName() == null) || "".equals(descriptor.getHomeClassName())) &&
                ((descriptor.getLocalHomeClassName() == null) || "".equals(descriptor.getLocalHomeClassName()))) {

                if (implementsEndpoints(descriptor)) {
                    result.addNaDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                         new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString
                       ("com.sun.enterprise.tools.verifier.tests.ejb.webservice.notapp",
                       "Not Applicable because, EJB [ {0} ] implements a Service Endpoint Interface.",
                       new Object[] {compName.toString()}));
                    result.setStatus(result.NOT_APPLICABLE);
                 return result;
                 }
            }

	    boolean oneFailed = false;
	    // RULE: session home interface are only allowed to have create 
	    //       methods which match ejbCreate, 
	    oneFailed = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor.getLocalHomeClassName(),descriptor);
	    if (oneFailed) {
		result.setStatus(result.FAILED);
	    } else {
		result.setStatus(result.PASSED);
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
				  new Object[] {getClass(),"Session","Entity"}));
	    return result;
	} 
    }

    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home interface of the Ejb. 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */


    private boolean commonToBothInterfaces(String remote, String local,EjbDescriptor descriptor) {
	boolean oneFailed = false;
	try {
	    VerifierTestContext context = getVerifierContext();
	    ClassLoader jcl = context.getClassLoader();
	    Class [] methodParameterTypes;
	    Class [] businessMethodParameterTypes;
	    boolean signaturesMatch = false;
	
	    boolean found = false;
	    Vector<Method> createMethodSuffix = new Vector<Method>();
	    
	    if (local != null) {
		Class localhome = Class.forName(local, false, getVerifierContext().getClassLoader());
		Method [] localhomeMethods = localhome.getDeclaredMethods();
		for (int i = 0; i < localhomeMethods.length; i++) {
		    // The method name must start with create. 
		    if (localhomeMethods[i].getName().startsWith("create")) {
			createMethodSuffix.addElement( (Method)localhomeMethods[i]);
			foundAtLeastOneCreate  = true;
			
		    }
		}
	    }
	    if (remote != null) {
		Class home = Class.forName(remote, false, getVerifierContext().getClassLoader());
		Method [] homeMethods = home.getDeclaredMethods();
		for (int i = 0; i < homeMethods.length; i++) {
		    // The method name must start with create. 
		    if (homeMethods[i].getName().startsWith("create")) {
			createMethodSuffix.addElement( (Method)homeMethods[i]);
			foundAtLeastOneCreate  = true;
			
		    }
		}
	    }
	    if (foundAtLeastOneCreate == false) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException2",
			       "Error: no create<Method> method exists!",
			       new Object[] {}));
		return true;
	    }
	    
	    Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
	    // start do while loop here....
            Method [] methods = EJBClass.getMethods();
            // find matching "ejbCreate" in bean class
            for (int k = 0; k < createMethodSuffix.size(); k++) {
                for (int j = 0; j < methods.length; j++) {
                    found = false;
                    if (methods[j].getName().startsWith("ejbCreate")) {
                        found = true;
                        String matchSuffix = methods[j].getName().substring(9);
                        signaturesMatch = false;
                        if (matchSuffix.equals(((Method)(createMethodSuffix.elementAt(k))).getName().substring(6))) {
                            methodParameterTypes = ((Method)(createMethodSuffix.elementAt(k))).getParameterTypes();
                            businessMethodParameterTypes = methods[j].getParameterTypes();
                            if (Arrays.equals(methodParameterTypes,businessMethodParameterTypes)) {
                                signaturesMatch = true;
                                // now display the appropriate results for this particular ejbCreate
                                // method
                                result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                                result.addGoodDetails(smh.getLocalString
                                (getClass().getName() + ".debug1",
                                "For Home Interface Method [ {0} ]",
                                new Object[] {((Method)(createMethodSuffix.elementAt(k))).getName()}));
                                result.addGoodDetails(smh.getLocalString
                                (getClass().getName() + ".passed",
                                "The corresponding ejbCreate method with matching parameters was found."));
                                break;
                            }
                        }
                    }
                }
                if (signaturesMatch == false) {
                    oneFailed = true;
                    result.addErrorDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                    "For [ {0} ]",
                    new Object[] {compName.toString()}));
                    result.addErrorDetails(smh.getLocalString
                    (getClass().getName() + ".debug3",
                    "For Home Interface",
                    new Object[] {}));
                    result.addErrorDetails(smh.getLocalString
                    (getClass().getName() + ".failed",
                    "Error: No corresponding ejbCreate<Method> method with matching parameters was found." ));
                    
                }
            }
	    if (found == false && foundAtLeastOneCreate == true){
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException1",
			       "Error: ejbCreate<Method> method corresponding to the create<Method> method does not exist!",
			       new Object[] {}));
		
	    }
	    if (found == false && foundAtLeastOneCreate == false){
	     	result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
				     (getClass().getName() + ".failedException2",
				      "Error: no create<Method> method exists!",
			       new Object[] {}));
	    }
	    return oneFailed;  
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home (Local/Remote) interface or  bean class [ {0} ] does not exist or is not loadable within bean [ {1} ]",
			   new Object[] {descriptor.getEjbClassName(),descriptor.getName()}));
	    oneFailed = true;
	    return oneFailed;
	}

    }
}
