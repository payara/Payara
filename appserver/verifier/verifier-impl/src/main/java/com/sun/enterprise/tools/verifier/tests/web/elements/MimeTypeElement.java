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

package com.sun.enterprise.tools.verifier.tests.web.elements;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import java.util.*;
import java.util.logging.Level;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import org.glassfish.web.deployment.descriptor.MimeMappingDescriptor;


/** 
 * Servlet mime-type element contains a defined mime type.  i.e. "text/plain"
 */
public class MimeTypeElement extends WebTest implements WebCheck, MimeTypes { 

    /**
     * Servlet mime-type element contains a defined mime type.  i.e. "text/plain"
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor.getMimeMappings().hasMoreElements()) {
	    boolean oneFailed = false;
	    boolean foundIt = false;
	    // get the mimeType's in this .war
	    for (Enumeration e = descriptor.getMimeMappings() ; e.hasMoreElements() ;) {
		foundIt = false;
		MimeMappingDescriptor mimemapping = (MimeMappingDescriptor)e.nextElement();
		String mimeType = mimemapping.getMimeType();
		logger.log(Level.FINE, "servlet mimeType: " + mimeType);
		int pos = mimeType.indexOf("/");
		// user defined
		// see http://www-dos.uniinc.msk.ru/tech1/1995/mime/m_tech.htm#Type

		if (mimeType.substring(pos+1).startsWith("X-") || mimeType.substring(pos+1).startsWith("x-")) {
		    foundIt = true;
		} else if (mimeType.startsWith("X-")) {
                    foundIt = true;
                } else if (mimeType.substring(0,pos).equals("text")) {
		    if (Arrays.asList(text).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("multipart")) {
		    if (Arrays.asList(multipart).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("message")) {
		    if (Arrays.asList(message).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("application")) {
		    if (Arrays.asList(application).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("image")) {
		    if (Arrays.asList(image).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("audio")) {
		    if (Arrays.asList(audio).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("video")) {
		    if (Arrays.asList(video).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		} else if (mimeType.substring(0,pos).equals("model")) {
		    if (Arrays.asList(model).contains(mimeType.substring(pos+1,mimeType.length()))) {
			foundIt = true;
		    } 
		}
   
		if (foundIt) {
		    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "Servlet mime-type [ {0} ] defined for this web application [ {1} ]",
					   new Object[] {mimeType, descriptor.getName()}));
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
					    "Error: Servlet mime-type [ {0} ] not defined for this web application [ {1} ]",
					    new Object[] {mimeType, descriptor.getName()}));
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
				  "There are no mimemappings within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}
	return result;
    }
}
