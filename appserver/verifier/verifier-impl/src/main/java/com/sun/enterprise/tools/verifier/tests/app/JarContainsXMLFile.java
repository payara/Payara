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

package com.sun.enterprise.tools.verifier.tests.app;

import com.sun.enterprise.deployment.io.ApplicationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deploy.shared.FileArchive;

import java.io.*;
import java.util.jar.*;

/** An enterprise archive (.ear) file must contain the XML-based deployment descriptor.
 * The deployment descriptor must be named META-INF/application.xml in the JAR file.
 */
public class JarContainsXMLFile extends ApplicationTest implements AppCheck { 


    /** An enterprise archive (.ear) file must contain the XML-based deployment descriptor.
     * The deployment descriptor must be named META-INF/application.xml in the JAR file.
     * 
     * @param descriptor the Application deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(Application descriptor) {

	Result result = getInitializedResult();

    // This test can not have a max-version set in xml file,
    // hence we must exclude this test based on platform version.
    if(getVerifierContext().getJavaEEVersion().
            compareTo(SpecVersionMapper.JavaEEVersion_5) >= 0) {
        result.setStatus(Result.NOT_APPLICABLE);
        return result;
    }


	JarFile jarFile = null;
        InputStream deploymentEntry=null;
	try {
//	    File applicationJarFile = null;
//            if (Verifier.getEarFile() != null)
//                applicationJarFile = new File(Verifier.getEarFile());
             
	    // should try to validate against SAX XML parser before
	    // continuing, report syntax errors and drop out, otherwise
	    // continue...

//            if (applicationJarFile == null) {
//               try {
                 FileArchive arch = (FileArchive)getVerifierContext().
                                    getAbstractArchive();
                 deploymentEntry = arch.getEntry(
		               ApplicationDeploymentDescriptorFile.DESC_PATH);
//               }catch (IOException e) { throw e;}
//            }
//            else {
//
//	      jarFile = new JarFile(applicationJarFile);
//	      ZipEntry deploymentEntry1 =
//		jarFile.getEntry(ApplicationDeploymentDescriptorFile.DESC_PATH);
//              deploymentEntry = jarFile.getInputStream(deploymentEntry1);
//            }

	    if (deploymentEntry != null) {
		result.passed(smh.getLocalString
			      (getClass().getName() + ".passed",
			       "Found deployment descriptor xml file [ {0} ]",
			       new Object[] {ApplicationDeploymentDescriptorFile.DESC_PATH}));
	    } else { 
		
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failed",
			       "Error: No deployment descriptor xml file found, looking for [ {0} ]",
			       new Object[] {ApplicationDeploymentDescriptorFile.DESC_PATH}));
	    }

	} catch (FileNotFoundException ex) {
	    Verifier.debug(ex);
	    
		result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: File not found trying to read deployment descriptor file [ {0} ]",
			   new Object[] {ApplicationDeploymentDescriptorFile.DESC_PATH}));
	} catch (IOException ex) {
	    Verifier.debug(ex);
	    
		result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException1",
			   "Error: IO Error trying to read deployment descriptor file [ {0} ]",
			   new Object[] {ApplicationDeploymentDescriptorFile.DESC_PATH}));
	} finally {
            try {
              if (jarFile != null)
                    jarFile.close();
              if (deploymentEntry != null)
                   deploymentEntry.close();
            } catch (Exception x) {}
        }

	return result;
    }
}
