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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import com.sun.enterprise.deploy.shared.FileArchive;

import java.io.*;


/** 
 * The Web form-error-page value defines the location in the web application 
 * where the page can be used for error page can be found within web 
 * application test
 */
public class FormErrorPage extends WebTest implements WebCheck { 

    
    /** 
     * The Web form-error-page value defines the location in the web application 
     * where the page can be used for error page can be found within web 
     * application test
     *
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor.getLoginConfiguration() != null) {
	    boolean foundIt = false;
//            ZipEntry ze=null;
//            JarFile jar=null;
            FileArchive arch=null;
            
	    String formErrorPage = descriptor.getLoginConfiguration().getFormErrorPage();
            if (formErrorPage.length() > 0) {
	       
                try{
                    
//                    File f = Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
//                    if(f==null){
                        
                        String uri=getAbstractArchiveUri(descriptor);
                        
                        try{
                            arch = new FileArchive();
                            arch.open(uri);
                        }catch(IOException e){
                            throw e;
                        }
//                    }else{
//                        jar = new JarFile(f);
//                    }
                    if (formErrorPage.startsWith("/")) 
                        formErrorPage=formErrorPage.substring(1);
//                    if (f!=null){
//                        ze = jar.getEntry(formErrorPage);
//                        foundIt = (ze != null);
//                    }
//                    else{
                        File fep = new File(new File(arch.getURI()), formErrorPage);
                        if(fep.exists())
                            foundIt=true;
                        fep = null;
//                    }
//                    if (jar!=null)
//                        jar.close();
                }catch (Exception ex) {
		    //should be aldready set?
		    foundIt = false;
	        }               
	        if (foundIt) {
		    result.addGoodDetails(smh.getLocalString
			("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
		    result.passed(smh.getLocalString
			          (getClass().getName() + ".passed",
			           "The form-error-page [ {0} ] value defines the location in the web application where the error page that is displayed when login is not successful can be found within web application [ {1} ]",
			           new Object[] {formErrorPage, descriptor.getName()}));
	        } else {
		    result.addErrorDetails(smh.getLocalString
			("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString
			      (getClass().getName() + ".failed",
			       "Error: The form-error-page [ {0} ] value does not define the location in the web application where the error page that is displayed when login is not successful can be found within web application [ {1} ]",
			       new Object[] {formErrorPage, descriptor.getName()}));
	        }
	    } else {
		result.addNaDetails(smh.getLocalString
			("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
	        result.notApplicable(smh.getLocalString
	    			 (getClass().getName() + ".notApplicable",
	    			  "There are no form-error-page elements within this web archive [ {0} ]",
	    			  new Object[] {descriptor.getName()}));
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
			("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no form-error-page elements within this web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
