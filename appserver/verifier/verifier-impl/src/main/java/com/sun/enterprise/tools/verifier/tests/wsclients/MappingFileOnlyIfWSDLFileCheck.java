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
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids:  JSR109_WS_48; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: If the wsdl-file is not specified in the deployment descriptor, the 
 *   jaxrpc-mapping-file must not be specified.
 */

public class MappingFileOnlyIfWSDLFileCheck extends WSClientTest implements WSClientCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (ServiceReferenceDescriptor descriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (!descriptor.hasWsdlFile()) {
            if (descriptor.hasMappingFile()) {
               // result.fail, mapping file specified without WSDL
                result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                     "For [ {0} ]", new Object[] {compName.toString()}));
                result.failed(smh.getLocalString (getClass().getName() + ".failed",
                "Mapping file specified for this webservice [{0}] without a corresponding WSDL file.",
                      new Object[] {compName.toString()}));
            }
            else {
              // result.pass
              result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed",
             "No Mapping file specified since there is no WSDL file."));

            }
        }
        else {
         // not applicable
         // NOTE: the other way around things are being checked in another test
         result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
         result.notApplicable(smh.getLocalString
                 ( getClass().getName() + ".notapp",
                 "Not Applicable since there is a WSDL file specified."));
 
        }

        return result;
    }
 }

