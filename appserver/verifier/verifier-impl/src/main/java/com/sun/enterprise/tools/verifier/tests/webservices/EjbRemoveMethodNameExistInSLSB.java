/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.webservices;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;

import java.lang.reflect.Method;

/*
 *   @class.setup_props: ;
 */

/*
 *   @testName: check
 *   @assertion_ids:  JSR109_WS_05; 
 *   @test_Strategy:
 *   @class.testArgs: 
 *   @testDescription: Service Implementation Bean(SLSB) must implement the ejbRemove() method which take no 
 *   arguments.
 *
 *   This is a requirement of the EJB container,but generally can be stubbed out with an empty implementations
 */

public class EjbRemoveMethodNameExistInSLSB extends WSTest implements WSCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint wsdescriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	boolean foundFailure=false;
        if (wsdescriptor.implementedByEjbComponent()) {
            EjbDescriptor ejbdesc = wsdescriptor.getEjbComponentImpl();
            if (ejbdesc != null && (ejbdesc instanceof EjbSessionDescriptor)) {
                EjbSessionDescriptor descriptor = (EjbSessionDescriptor)ejbdesc;
                if (EjbSessionDescriptor.STATELESS.equals(descriptor.getSessionType())) {
                    try {
                        //VerifierTestContext context = getVerifierContext();
                        ClassLoader jcl = getVerifierContext().getClassLoader();
                        Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
                        int foundAtLeastOne = 0;

                        do {
                            Method [] methods = c.getDeclaredMethods();
                            for (int i = 0; i < methods.length; i++) {
                                // The method name must be ejbRemove.
                                if (methods[i].getName().startsWith("ejbRemove")) {
                                    foundAtLeastOne++;
                            		result.addGoodDetails(smh.getLocalString
                                            ("tests.componentNameConstructor",
                                                    "For [ {0} ]",
                                                    new Object[] {compName.toString()}));
                                    result.addGoodDetails(smh.getLocalString
                                            (getClass().getName() + ".passed",
                                                    "[ {0} ] declares [ {1} ] method.",
                                                    new Object[] {descriptor.getEjbClassName(),methods[i].getName()}));
                                }
                            }
                        } while (((c = c.getSuperclass()) != null) && (foundAtLeastOne == 0));
                        if (foundAtLeastOne == 0){
                            foundFailure = true;
                            result.addErrorDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                            "For [ {0} ]",
                                            new Object[] {compName.toString()}));
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                            "Error: [ {0} ] does not properly declare at least one ejbRemove() method.  [ {1} ] is not a valid bean.",
                                            new Object[] {descriptor.getEjbClassName(),descriptor.getEjbClassName()}));
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
                        return result;
                    }
                } else {
                    result.addNaDetails(smh.getLocalString
                            ("tests.componentNameConstructor", "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString
                            (getClass().getName() + ".notApplicable",
                                    "NOT APPLICABLE :Service Implementation bean is not a stateless Session Bean"));
                    return result;
                }
            } else {
                result.addNaDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString
                        (getClass().getName() + ".notApplicable1",
                                "NOT APPLICABLE:Service Implementation bean is null or not a session bean descriptor "));
                return result;
            }

            if (foundFailure) {
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
                    (getClass().getName() + ".notApplicable2",
                            "Not Applicable: Service Implementation bean is not implemented by Ejb."));
            return result;
        }
    }
}
