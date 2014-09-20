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
 *   @assertion_ids: JSR109_WS_16; JSR109_WS_17; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: The Web services deployment descriptor location within the EJB-JAR file 
 *   is META-INF/webservices.xml.
 *
 *   A Web services deployment descriptor is located in a WAR at WEB-INF/webservices.xml.
 */

public class WSXMLLocCheck extends WSTest implements WSCheck {

    // webservices.xml
    private String ejbWSXmlLoc = "META-INF/webservices.xml";
    private String jaxrpcWSXmlLoc = "WEB-INF/webservices.xml";

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

//        boolean pass = true;

//        File f = Verifier.getArchiveFile(descriptor.getBundleDescriptor().
//                 getModuleDescriptor().getArchiveUri());
//        JarFile jarFile = null;
        InputStream deploymentEntry=null;

        try {
//             if (f == null) {
              String uri = getAbstractArchiveUri(descriptor);
//              try {
                 FileArchive arch = new FileArchive();
                 arch.open(uri);
                 if (descriptor.implementedByEjbComponent()) {
                    deploymentEntry = arch.getEntry(ejbWSXmlLoc);
                 }
                 else if (descriptor.implementedByWebComponent()) {
                    deploymentEntry = arch.getEntry(jaxrpcWSXmlLoc);
                 }
                 else {
                    throw new Exception("Niether implemented by EJB nor by WEB Component");
                 }
//               }catch (IOException e) { throw e;}
//             }
//             else {
//
//               jarFile = new JarFile(f);
//               ZipEntry deploymentEntry1 = null;
//               if (descriptor.implementedByEjbComponent()) {
//                   deploymentEntry1 = jarFile.getEntry(ejbWSXmlLoc);
//               }
//               else if (descriptor.implementedByWebComponent()) {
//                   deploymentEntry1 = jarFile.getEntry(jaxrpcWSXmlLoc);
//               }
//               else {
//                    throw new Exception("Niether implemented by EJB nor by WEB Component");
//               }
//               deploymentEntry = jarFile.getInputStream(deploymentEntry1);
//            }
            if (deploymentEntry != null) {
              // webservices XML exists 
              // result.pass
              result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed",
                          "The webservices.xml file for [{0}] is located at the correct place.",
                           new Object[] {compName.toString()}));

            }
            else {
             // ws xml is does not exist
             //result.fail
             result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.failed(smh.getLocalString (getClass().getName() + ".failed",
               "The webservices.xml file for [{0}] is not located in WEB-INF/META-INF directory as applicable.",
                new Object[] {compName.toString()}));

//             pass = false;
            }
        }catch (Exception e) {
            //result.fail
            result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {e.getMessage()}));
//            pass = false;
        }
        finally {

           try {
           if (deploymentEntry != null)
               deploymentEntry.close();
           }catch (IOException e) {}
        }

        return result;
    }
 }

