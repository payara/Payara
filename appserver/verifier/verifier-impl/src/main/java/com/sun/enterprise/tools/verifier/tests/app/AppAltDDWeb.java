/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deploy.shared.FileArchive;

/**
 * The alt-dd element specifies a URI to the post-assembly deployment descriptor
 * relative to the root of the application 
 */

public class AppAltDDWeb extends ApplicationTest implements AppCheck { 


    /** 
     * The alt-dd element specifies a URI to the post-assembly deployment descriptor
     * relative to the root of the application 
     *
     * @param descriptor the Application deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(Application descriptor) {

	Result result = getInitializedResult();

 
	if (descriptor.getBundleDescriptors(WebBundleDescriptor.class).size() > 0) {
	    boolean oneFailed = false;
            int na = 0;
	    for (Iterator itr = descriptor.getBundleDescriptors(WebBundleDescriptor.class).iterator(); itr.hasNext();) {
		WebBundleDescriptor wbd = (WebBundleDescriptor) itr.next();

		if (wbd.getModuleDescriptor().getAlternateDescriptor()!=null) {
		    if (!(wbd.getModuleDescriptor().getAlternateDescriptor().equals(""))) {
                        InputStream deploymentEntry=null;
//                        File f = null;
//                        if (Verifier.getEarFile() != null)
//                            f = new File(Verifier.getEarFile());
                        
			try {
//                            if (f==null){
                                String uri = getAbstractArchiveUri(descriptor);
//                                try {
                                    FileArchive arch = new FileArchive();
                                    arch.open(uri);
                                    deploymentEntry = arch.getEntry(wbd.getModuleDescriptor().getAlternateDescriptor());
//                                }catch (Exception e) { }
//                            }else{
//
//                                jarFile = new JarFile(f);
//                                ZipEntry deploymentEntry1 = jarFile.getEntry(wbd.getModuleDescriptor().getAlternateDescriptor());
//                                if (deploymentEntry1 != null)
//                                    deploymentEntry = jarFile.getInputStream(deploymentEntry1);
//                            }

			    if (deploymentEntry != null) {
				result.addGoodDetails(smh.getLocalString
						      (getClass().getName() + ".passed",
						       "Found alternate web deployment descriptor URI file [ {0} ] within [ {1} ]",
						       new Object[] {wbd.getModuleDescriptor().getAlternateDescriptor(),wbd.getName()}));
			    } else { 
                                if (!oneFailed) {
                                    oneFailed = true;
                                }
				result.addErrorDetails(smh.getLocalString
						       (getClass().getName() + ".failed",
							"Error: No alternate web deployment descriptor URI file found, looking for [{0} ] within [ {1} ]",
							new Object[] {wbd.getModuleDescriptor().getAlternateDescriptor(), wbd.getName()}));
			    }
			    //jarFile.close();
        
			} catch (FileNotFoundException ex) {
			    Verifier.debug(ex);
                            if (!oneFailed) {
                                oneFailed = true;
                            }
			    
		result.failed(smh.getLocalString
					  (getClass().getName() + ".failedException",
					   "Error: File not found trying to read deployment descriptor file [ {0} ] within [ {1} ]",
					   new Object[] {wbd.getModuleDescriptor().getAlternateDescriptor(), wbd.getName()}));
			} catch (IOException ex) {
			    Verifier.debug(ex);
                            if (!oneFailed) {
                                oneFailed = true;
                            }
			    
		result.failed(smh.getLocalString
					  (getClass().getName() + ".failedException1",
					   " Error: IO Error trying to read deployment descriptor file [ {0} ] within [ {1} ]",
					   new Object[] {wbd.getModuleDescriptor().getAlternateDescriptor(), wbd.getName()}));
	                } finally {
                            try {
                                if (deploymentEntry != null)
                                    deploymentEntry.close();
                            } catch (Exception x) {}
                        }

		    }
		} else {
                    na++;
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable1",
					  "There is no java web alternative deployment descriptor in [ {0} ]",
					  new Object[] {wbd.getName()}));
		}
	    }
            if (oneFailed) {
                result.setStatus(Result.FAILED);
            } else if (na == descriptor.getBundleDescriptors(WebBundleDescriptor.class).size()) {
                result.setStatus(Result.NOT_APPLICABLE);
            } else {
                result.setStatus(Result.PASSED);
            }

	} else {
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no web components in application [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

 
	return result;
    }
}
