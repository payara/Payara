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

package com.sun.enterprise.tools.verifier.tests.wsclients;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import java.util.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_57; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: The service-endpoint-interface element defines a fully qualified Java class 
 *   that represents the Service Endpoint Interface of a WSDL port.
 */

public class PortCompRefSEIClassCheck  extends WSClientTest implements WSClientCheck {
    ComponentNameConstructor compName;

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (ServiceReferenceDescriptor descriptor) {

	Result result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();

        boolean pass = true;

        Collection ports = descriptor.getPortsInfo();

        for (Iterator it=ports.iterator(); it.hasNext();) {
            ServiceRefPortInfo ref = (ServiceRefPortInfo)it.next();
            if (!loadSEIClass(ref,result)) {
               //result.fail ref.getName(), ref.getServiceEndpointInterface
               result.addErrorDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
               result.failed(smh.getLocalString
                   (getClass().getName() + ".failed",
                    "Error: Service Endpoint Interface class [ {0} ]  not found.",
                    new Object[] {ref.getServiceEndpointInterface()}));
               
               pass = false;
            }
            else {
              //result.pass
              result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed",
              "Service Endpoint Interface class [ {0} ]  found.", 
              new Object[] {ref.getServiceEndpointInterface()}));

            }
        }

        return result;
    }

   private boolean loadSEIClass(ServiceRefPortInfo ref, Result result) {

     boolean pass = true;

     if (ref.hasServiceEndpointInterface()) {
        try {
              Class.forName(ref.getServiceEndpointInterface(), false, getVerifierContext().getClassLoader());
           } catch (ClassNotFoundException e) {
               Verifier.debug(e);
               pass = false;
           }
     }
     else {
       //result.not applicable (SEI not specified)
       result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
       result.notApplicable(smh.getLocalString
                 ( getClass().getName() + ".notapp",
                 "Not applicable since Service reference does not specify an SEI."));

     }
    return pass;
   }
 }

