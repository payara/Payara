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

import com.sun.enterprise.tools.verifier.tests.app.ApplicationTest;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*; 
import java.util.jar.*;
import java.util.*;
import java.net.URI;

/**     
 * Application's listed J2EE modules exist in the Enterprise archive
 * The J2EE module element contains an ejb, java, or web element, which indicates 
 */
public class ModulesExistEjb extends ApplicationTest implements AppCheck { 


    /**     
     * Application's listed J2EE modules exist in the Enterprise archive
     * The J2EE module element contains an ejb, java, or web element, which indicates 
     * the module type and contains a path to the module file
     *
     * @param descriptor the Application deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(Application descriptor) {

	Result result = getInitializedResult();

  

	if (descriptor.getBundleDescriptors(EjbBundleDescriptor.class).size() > 0) {
	    boolean oneFailed = false;
	    for (Iterator itr = descriptor.getBundleDescriptors(EjbBundleDescriptor.class).iterator(); itr.hasNext();) {
		EjbBundleDescriptor ejbd = (EjbBundleDescriptor) itr.next();

		if (!(ejbd.getModuleDescriptor().getArchiveUri().equals(""))) {
		    JarFile jarFile = null;
                    InputStream deploymentEntry=null;
                    boolean moduleDirExists = false;
 
//		    try {
//			File applicationJarFile = null;
//                        if (Verifier.getEarFile() != null) {
//			   applicationJarFile = new File(Verifier.getEarFile());
//                        }
             
//                        if (applicationJarFile == null) {
//                            try {
                              String archBase = 
                                 getAbstractArchiveUri(descriptor);
                              String moduleName =
			         ejbd.getModuleDescriptor().getArchiveUri();
                              String moduleDir = FileUtils.makeFriendlyFilename(moduleName);
                              File f = new File(new File(URI.create(archBase)),
                                           moduleDir);
                              moduleDirExists = f.isDirectory();
//                            }catch (Exception e) { throw new IOException(e.getMessage());}
//                        }
//                        else {
//			   jarFile = new JarFile(applicationJarFile);
//			   ZipEntry deploymentEntry1 = jarFile.getEntry(
//                                ejbd.getModuleDescriptor().getArchiveUri());
//                           deploymentEntry = jarFile.getInputStream(
//                                             deploymentEntry1);
//                        }
        
			if ((deploymentEntry != null) || (moduleDirExists)) {
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".passed",
						   "J2EE EJB module [ {0} ] exists within [ {1} ]",
						   new Object[] {ejbd.getModuleDescriptor().getArchiveUri(),descriptor.getName()}));
			} else { 
                            if (!oneFailed) {
                                oneFailed = true;
                            }
			    result.addErrorDetails(smh.getLocalString
						   (getClass().getName() + ".failed",
						    "Error: J2EE EJB module [ {0} ] does not exist within [ {1} ].",
						    new Object[] {ejbd.getModuleDescriptor().getArchiveUri(),descriptor.getName()}));
			}
        
//		    } catch (FileNotFoundException ex) {
//			Verifier.debug(ex);
//                        if (!oneFailed) {
//                            oneFailed = true;
//                        }
//			
//			result.failed(smh.getLocalString
//				      (getClass().getName() + ".failedException",
//				       "Error: File not found trying to read J2EE module file [ {0} ] within [ {1} ]",
//				       new Object[] {ejbd.getModuleDescriptor().getArchiveUri(), descriptor.getName()}));
//		    } catch (IOException ex) {
//			Verifier.debug(ex);
//                        if (!oneFailed) {
//                            oneFailed = true;
//                        }
//			
//			result.failed(smh.getLocalString
//				      (getClass().getName() + ".failedException1",
//				       "Error: IO Error trying to read J2EE module file [ {0} ] within [ {1} ]",
//				       new Object[] {ejbd.getModuleDescriptor().getArchiveUri(), descriptor.getName()}));
//	            } finally {
                        try {
                          if (jarFile != null)
                              jarFile.close();
                          if (deploymentEntry != null)
                              deploymentEntry.close();
                        } catch (Exception x) {}
//                    }

		}

	    }
            if (oneFailed) {
                result.setStatus(Result.FAILED);
            } else {
                result.setStatus(Result.PASSED);
            }

	} else {
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no EJB components in application [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}
	return result;
    }
}
