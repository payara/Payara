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

import java.io.*;

import com.sun.enterprise.deployment.io.ApplicationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deploy.shared.FileArchive;

/**
 * Application PUBLIC identifier test
 * The application deployment descriptor has PUBLIC identifier with 
 * a PubidLiteral of "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN"
 */
public class AppPublicID extends ApplicationTest implements AppCheck { 

    
    String acceptablePubidLiterals[] = {
            "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN",      
            "-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN"      };

    String acceptableURLs[] = {"http://java.sun.com/j2ee/dtds/application_1_2.dtd",
			       "http://java.sun.com/dtd/application_1_3.dtd"};

    /** 
     * Application PUBLIC identifier test
     * The application deployment descriptor has PUBLIC identifier with 
     * a PubidLiteral of "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN" 
     *
     * @param descriptor the Application deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(Application descriptor) {

	Result result = getInitializedResult();
      
	// open the jar and read the XML deployment descriptor
	if (descriptor.getSpecVersion().compareTo("1.4") < 0){
        InputStream deploymentEntry=null;

        String uri = null;
	try {
                uri = getAbstractArchiveUri(descriptor);
                 FileArchive arch = new FileArchive();
                 arch.open(uri);
                 deploymentEntry = arch.getEntry(
		    ApplicationDeploymentDescriptorFile.DESC_PATH);

	    if (deploymentEntry != null) {
		BufferedReader in = new BufferedReader(new InputStreamReader(deploymentEntry));
		String s = in.readLine();
		boolean foundDOCTYPE = false, foundPubid = false, foundURL = false;

		while(s != null) {
		    // did we find the DOCTYPE entry? 
		    if(s.indexOf("DOCTYPE") > -1)
			foundDOCTYPE = true;
		    if(foundDOCTYPE){
                       for (int i=0;i<acceptablePubidLiterals.length;i++) {
			    if (s.indexOf(acceptablePubidLiterals[i]) > -1) {
			       foundPubid = true;
    			       result.addGoodDetails
				    (smh.getLocalString
				    (getClass().getName() + ".passed", 
				    "The deployment descriptor has the proper PubidLiteral: {0}", 
				    new Object[] {acceptablePubidLiterals[i]}));  
			    } 
			    //check if the URLs match as well  
			    if (s.indexOf(acceptableURLs[i]) > -1) {
				foundURL = true;
				result.addGoodDetails
				    (smh.getLocalString
				     (getClass().getName() + ".passed1", 
				      "The deployment descriptor has the proper URL corresponding the the PubIdLiteral: {0}", 
				      new Object[] {acceptableURLs[i]})); 
			    }
		       }
		    }
		    if(foundPubid && foundURL) {
			result.setStatus(Result.PASSED);
			break;
		    } else if(foundDOCTYPE && s.endsWith(">")) break; // DOCTYPE doesn't have any more lines to check
		    s = in.readLine();
		}

		if(!foundDOCTYPE){
		    
		result.failed
			(smh.getLocalString
			 (getClass().getName() + ".failed1", 
			  "No document type declaration found in the deployment descriptor for {0}",
			  new Object[] {descriptor.getName()}));
		} else if(!foundPubid) {
		    
		result.failed
			(smh.getLocalString
			 (getClass().getName() + ".failed2", 
			  "The deployment descriptor for {0} does not have the expected PubidLiteral ",
			  new Object[] {descriptor.getName()}));
		}else if (!foundURL){
		    
		result.failed(smh.getLocalString
					   (getClass().getName() + ".failed", 
					 	    "The deployment descriptor {0} doesnot have the right URL corresponding to the PubIdLiteral", 
					    new Object[] {descriptor.getName()})); 
		}
	    }

	} catch (IOException e) {
	    
		result.failed(smh.getLocalString
			  (getClass().getName() + ".IOException", 
			   "I/O error trying to open {0}", new Object[] {uri}));
	} finally {
            try {
              if (deploymentEntry != null)
                 deploymentEntry.close();
            } catch (Exception x) {}
        }
         }else{
                //NOT APPLICABLE               
                /*result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));*/
                result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
		    "NOT-APPLICABLE: No DOCTYPE found for [ {0} ]",
		     new Object[] {descriptor.getName()}));
            }
	return result;
    }
}
