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
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * The load-on-startup element contains an integer indicating the order
 * in which the JSP should be loaded. 
 */
public class JspLoadOnStartup extends WebTest implements WebCheck { 

      
    /**
     * The load-on-startup element contains an integer indicating the order
     * in which the JSP should be loaded. 
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
  

	boolean oneFailed = false;
	if (!descriptor.getJspDescriptors().isEmpty()) {
	    for (Iterator itr = descriptor.getJspDescriptors().iterator();
		 itr.hasNext();) {

		WebComponentDescriptor nextJspDescriptor = (WebComponentDescriptor) itr.next();
		Integer loadOnStartUp = new Integer(nextJspDescriptor.getLoadOnStartUp());
		// DOL only allows int's to be stored, test will always pass as written, so need to check against -1 placeholder
                if (loadOnStartUp.intValue() >= 0) {
		    // DOL needs to store string value representing load-on-startup value
		    result.addGoodDetails(smh.getLocalString
			("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));

		    result.addGoodDetails
			(smh.getLocalString
			 (getClass().getName() + ".passed",
			  "load-on-startup [ {0} ] value found in [ {1} ]",
			  new Object[] {loadOnStartUp.toString(),nextJspDescriptor.getName()}));
		} else {
                    if (loadOnStartUp.intValue() == -1) {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
		        result.addGoodDetails(smh.getLocalString(
			    getClass().getName() + ".passed2",
                            "load-on-startup is not specified for [ {0} ]",
			    new Object[] {nextJspDescriptor.getName()}));                    
                    } else {
                        if (!oneFailed) { 
                            oneFailed = true;
                        }
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
	        	result.addErrorDetails
		    	    (smh.getLocalString
			    (getClass().getName() + ".failed",
			    "Error: load-on-startup [ {0} ] invalid value found in [ {1} ]",
			    new Object[] {loadOnStartUp.toString(),nextJspDescriptor.getName()}));
                    }
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
				  "There are no JSP's within this web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
