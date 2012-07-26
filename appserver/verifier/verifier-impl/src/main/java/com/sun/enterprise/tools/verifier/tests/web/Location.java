/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.web;

import java.util.*;
import java.io.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deploy.shared.FileArchive;
import org.glassfish.web.deployment.descriptor.ErrorPageDescriptor;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;


/**
 * Location element contains the location of the resource in the web 
 * application
 */
public class Location extends WebTest implements WebCheck { 

    
    /**
     * Location element contains the location of the resource in the web 
     * application
     *
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (((WebBundleDescriptorImpl)descriptor).getErrorPageDescriptors().hasMoreElements()) {
	    boolean oneFailed = false;
	    boolean foundIt = false;
//            ZipEntry ze = null;
//            JarFile jar =null;
            FileArchive arch=null;
	    // get the errorpage's in this .war
	    for (Enumeration e = ((WebBundleDescriptorImpl)descriptor).getErrorPageDescriptors() ; e.hasMoreElements() ;) {
		foundIt = false;
		ErrorPageDescriptor errorpage = (ErrorPageDescriptor) e.nextElement();
		String location = errorpage.getLocation();
            String uri = null;
                try{
//                    File f = Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
//                    if(f==null){
                        uri=getAbstractArchiveUri(descriptor);
                        try{
                            arch = new FileArchive();
                            arch.open(uri);
                        }catch(IOException ioe){throw ioe;}
//                    }else{
//                        jar = new JarFile(f);
//                    }
                    if (location.startsWith("/"))
                        location = location.substring(1);
//                    if (f!=null){
//                        ze = jar.getEntry(location);
//                        foundIt = (ze != null);
//                    }
//                    else{
                        File loc = new File(new File(arch.getURI()), location);
                        if(loc.exists())
                            foundIt=true;
                        loc = null;
//                    }
//                    if (jar!=null)
//                        jar.close();
                }catch (Exception ex) {
		    if (!oneFailed){
                        oneFailed = true;
                    }
	        }
                if (foundIt) {
		    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "Location [ {0} ] contains the location of the resource within web application [ {1} ]",
					   new Object[] {location, descriptor.getName()}));
		} else {
		    if (!oneFailed) {
			oneFailed = true;
		    }
		    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addErrorDetails(smh.getLocalString
					   (getClass().getName() + ".failed",
					    "Error: Location [ {0} ] is not found within [ {1} ] or does not contain the location of the resource within web application [ {2} ]",
					    new Object[] {location, uri, descriptor.getName()}));
		}
	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no location elements within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
