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
import com.sun.enterprise.tools.verifier.*;
import java.util.*;
import com.sun.enterprise.tools.verifier.tests.*;
import java.lang.reflect.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids:  JSR109_WS_19; JSR109_WS_23; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: Service Implementations using a stateless session bean must be defined 
 *   in the ejb-jar.xml deployment descriptor file using the session element.
 *
 *   For a stateless session bean implementation, the ejb-link element 
 *   associates the port-component with a session element in the ejb-jar.xml. The ejb-link 
 *   element may not refer to a session element defined in another module.
 */

public class EJBEndpointIsSLSBCheck extends WSTest implements WSCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint wsdescriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (wsdescriptor.implementedByEjbComponent()) {
            EjbDescriptor ejbdesc = wsdescriptor.getEjbComponentImpl();

            if (ejbdesc == null) {

               result.addErrorDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
                 result.failed(smh.getLocalString
                   (getClass().getName() + ".failed1",
                    "Service Implementation bean Could Not be Resolved from the ejb-link specified"));
               return result;
             }

            if (ejbdesc instanceof EjbSessionDescriptor) {
               EjbSessionDescriptor session = (EjbSessionDescriptor)ejbdesc;
               if (EjbSessionDescriptor.STATELESS.equals(session.getSessionType())) {
                   result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
                   result.passed(smh.getLocalString
                   (getClass().getName() + ".passed",
                   "Service Implementation bean defined in ejb-jar.xml using {0} session element",                   new Object[] {"stateless"}));
               }
               else {
                 // result.fail, endpoint can be a stateful session bean
                 result.addErrorDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
                 result.failed(smh.getLocalString
                   (getClass().getName() + ".failed",
                    "Service Implementation bean cannot be Stateful Session Bean"));
               }
            }
            else {
              // result.fail, service endpoint should be Session Bean
              result.addErrorDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
              result.failed(smh.getLocalString
                 (getClass().getName() + ".failed2",
                 "Service Implementation bean Should be a Session Bean"));
            }
  
        }
        else {

          // result.notapp
          result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
          result.notApplicable(smh.getLocalString
                 (getClass().getName() + ".notapp",
                 "This is a JAX-RPC Service Endpoint"));
        }

        return result;
    }
 }

