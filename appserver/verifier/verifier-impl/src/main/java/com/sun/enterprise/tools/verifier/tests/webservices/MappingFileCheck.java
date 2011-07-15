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
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deploy.shared.FileArchive;

import java.io.*;

/*
*   @class.setup_props: ;
*/

/*
*   @testName: check
*   @assertion_ids: JSR109_WS_15; JSR109_WS_30;
*   @test_Strategy:
*   @class.testArgs: Additional arguments (if any) to be passed when execing the client
*   @testDescription: The developer is responsible for packaging, either by containment or
*   reference, the WSDL file, Service Endpoint Interface class, Service Implementation Bean
*   class, and their dependent classes, JAX-RPC mapping file along with a Web services
*   deployment descriptor in a J2EE module.
*
*   jaxrpc-mapping-file The file name is a relative path within the module.
*/
public class MappingFileCheck extends WSTest implements WSCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String mappingFile = descriptor.getWebService().getMappingFileUri();
        if (getVerifierContext().getSchemaVersion().compareTo("1.1") > 0) {
            if (mappingFile != null) {
                addWarningDetails(result, compName);
                result.warning(smh.getLocalString (getClass().getName() + ".warning",
                        "The mapping file as specified in location [ {0} ] is not required.",
                        new Object[] {mappingFile}));
                return result;
            }
        } else {
            InputStream deploymentEntry=null;
            try {
                String uri = getAbstractArchiveUri(descriptor);
                try {
                    FileArchive arch = new FileArchive();
                    arch.open(uri);
                    deploymentEntry = arch.getEntry(mappingFile);
                }catch (IOException e) { throw e;}
                if (deploymentEntry == null) {
                    //result.fail, mapping file does not exist at that location
                    result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                            "For [ {0} ]", new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString (getClass().getName() + ".failed",
                            "The mapping file does not exist at the specified location [{0}] in the archive.",
                            new Object[] {mappingFile}));

                }
            }catch (Exception e) {
                result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                        "For [ {0} ]", new Object[] {compName.toString()}));
                result.failed(smh.getLocalString (getClass().getName() + ".failed",
                        "The mapping file does not exist at the specified location [{0}] in the archive.",
                        new Object[] {mappingFile}));
            }
            finally {
                try {
                    if (deploymentEntry != null)
                        deploymentEntry.close();
                }catch(IOException e) {}
            }
        }
        if(result.getStatus() != Result.FAILED || result.getStatus() != Result.WARNING) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString (getClass().getName() + ".passed",
                    "mapping file requirements are satisfied"));
        }
        return result;
    }
}
