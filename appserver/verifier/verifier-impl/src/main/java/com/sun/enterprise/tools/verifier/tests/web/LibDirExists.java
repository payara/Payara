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

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import java.util.*;
import java.io.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * Servlet lib directory resides in WEB-INF/lib directory test.
 */
public class LibDirExists extends WebTest implements WebCheck { 

    final String servletLibDirPath = "WEB-INF/lib";
      
    /** 
     * Servlet lib directory resides in WEB-INF/lib directory test.
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (!descriptor.getServletDescriptors().isEmpty()) {
	    boolean oneFailed = false;
	    int na = 0;
	    boolean foundIt = false;
	    // get the servlets in this .war
	    Set servlets = descriptor.getServletDescriptors();
	    Iterator itr = servlets.iterator();
	    // test the servlets in this .war
	    while (itr.hasNext()) {
		foundIt = false;
		WebComponentDescriptor servlet = (WebComponentDescriptor)itr.next();
//		try {
                    File warfile = new File(System.getProperty("java.io.tmpdir"));
		    warfile = new File(warfile, "wartmp");
//                    File f = Verifier.getArchiveFile(
//                             descriptor.getModuleDescriptor().getArchiveUri());
		    File warLibDir = null;

//                    if (f != null) {
//                        VerifierUtils.copyArchiveToDir(f, warfile);
//		        warLibDir = new File(warfile, servletLibDirPath);
//                    }
//                    else {
                      String uri = getAbstractArchiveUri(descriptor);
		      warLibDir = new File(uri, servletLibDirPath);
//                    }

                    if (warLibDir.isDirectory()) {
		        foundIt = true;
                    } 
/*		} catch (IOException e) {
		    if (!oneFailed ) {
			oneFailed = true;
		    }
		    Verifier.debug(e);
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));

		    result.addErrorDetails(smh.getLocalString
					   (getClass().getName() + ".IOException",
					    "Error: IOError trying to open [ {0} ], {1}",
					    new Object[] {Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri()), e.getMessage()}));
		}*/
   
		if (foundIt) {
		    result.addGoodDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "Servlet lib dir [ {0} ] resides in WEB-INF/lib directory of [ {1} ].",
					   new Object[] {servletLibDirPath,uri}));
		} else {
		    na++;
		    result.addNaDetails(smh.getLocalString
					("tests.componentNameConstructor",
					 "For [ {0} ]",
					 new Object[] {compName.toString()}));
		    result.addNaDetails(smh.getLocalString
					   (getClass().getName() + ".notApplicable2",
					    "Servlet lib dir [ {0} ] does not reside in [ {1} ].",
					    new Object[] {servletLibDirPath,uri}));
		}
	    }
            File wartmp = new File(System.getProperty("java.io.tmpdir"));
	    wartmp = new File(wartmp, "wartmp");
	    deleteDirectory(wartmp.getAbsolutePath());
	    if (na == descriptor.getServletDescriptors().size()) {
		result.setStatus(Result.NOT_APPLICABLE);
	    } else if (oneFailed) {
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
				  "There are no servlet components within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}
	return result;
    }   
}
