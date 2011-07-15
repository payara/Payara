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
import com.sun.enterprise.deploy.shared.FileArchive;

import java.io.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_51;
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: The developer must specify the jaxrpc-mapping-file if the wsdl-file is 
 *   specified. jaxrpc-mapping-file The file name is a relative path within the module.
 */
public class MappingFileCheck extends WSClientTest implements WSClientCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (ServiceReferenceDescriptor descriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (descriptor.hasWsdlFile()) {

           if(descriptor.hasMappingFile()) {
             //result.pass
             result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                    "For [ {0} ]", new Object[] {compName.toString()}));
             result.passed(smh.getLocalString (getClass().getName() + ".passed1",
                           "Mapping file specified because WSDL file is also specified."));

           }
           else {
             //result.fail, has wsdl but no mapping
             result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.failed(smh.getLocalString (getClass().getName() + ".failed1",
                "The webservice client [{0}] has a WSDL file specified but no Mapping File.",
                 new Object[] {compName.toString()}));
           }
         }
         else {
           if(descriptor.hasMappingFile()) {
             //result.fail, mapping without WSDL
             result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.failed(smh.getLocalString (getClass().getName() + ".failed1",
                "The webservice client [{0}] has a Mapping file specified but no WSDL File.",
                 new Object[] {compName.toString()}));
           }
           else {
             //result.pass, neither WSDL nor Mapping
              result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                    "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed2",
                           "Mapping file found at location [{0}] in archive.",
                           new Object[] {descriptor.getMappingFileUri()}));
           }
        }

        if (descriptor.hasMappingFile()) {
           // maybe we should not depend on DOL to get the location of Mapping file
           String mappingFile = descriptor.getMappingFileUri();

//           File f = Verifier.getArchiveFile(descriptor.getBundleDescriptor().
//                    getModuleDescriptor().getArchiveUri());
//           JarFile jarFile = null;
           InputStream deploymentEntry=null;

           try {
          
//             if (f == null) {
                 String uri = getAbstractArchiveUri(descriptor);
//                 try {
                    FileArchive arch = new FileArchive();
                    arch.open(uri);
                    deploymentEntry = arch.getEntry(mappingFile);
//                 }catch (IOException e) { throw e;}
//                }
//                else {
//
//                  jarFile = new JarFile(f);
//                  ZipEntry deploymentEntry1 =
//                      jarFile.getEntry(mappingFile);
//                  deploymentEntry = jarFile.getInputStream(deploymentEntry1);
//               }
               if (deploymentEntry == null) {
                  //result.fail, mapping file does not exist at that location
                   result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                   result.failed(smh.getLocalString (getClass().getName() + ".failed",
                    "The mapping file does not exist at the specified location [{0}] in the archive.",
                   new Object[] {mappingFile}));
               }
               else {

                    result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                    "For [ {0} ]", new Object[] {compName.toString()}));
                    result.passed(smh.getLocalString (getClass().getName() + ".passed",
                           "Mapping file found at location [{0}] in archive.",
                           new Object[] {mappingFile}));
               }
           }catch (Exception e) {
             // result.fail, mapping file does not exist at that location
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
        else {
           //result.notapplicable. Mapping file exists check not applicable
            result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString
                 ( getClass().getName() + ".notapp",
                 "This webservice client does not have a Mapping File."));
        }

        return result;
    }
 }

