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
 * The session-timeout element deinfes the default session timeout interval 
 * for all sessions created in this web application.  The units used must
 * be expressed in whole minutes.
 */
public class SessionTimeout extends WebTest implements WebCheck { 

    
    /** 
     * The session-timeout element deinfes the default session timeout interval 
     * for all sessions created in this web application.  The units used must
     * be expressed in whole minutes.
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	boolean na = false;
	boolean foundIt = false;
	Integer sessionTimeout = descriptor.getSessionConfig().getSessionTimeout();
	// tomcat doesn't throw exception to DOL if you pass "ten" to xml element,
	// it initializes session-timeout to -1, hence this check
	if (sessionTimeout.intValue() == -1 ) {
	    na = true;
	} else if (sessionTimeout.intValue() >= 0 ) {
	    foundIt = true;
	} else {
	    foundIt = false;
	}
   
	// always true until DOL lets something other than integer thru...
	if (na) {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
			  (getClass().getName() + ".notApplicable",
			   "Not Applicable: Servlet session-timeout [ {0} ] element does not define the default session timeout interval.",
			   new Object[] {sessionTimeout.toString()}));
	} else if (foundIt) {
	    result.addGoodDetails(smh.getLocalString
				  ("tests.componentNameConstructor",
				   "For [ {0} ]",
				   new Object[] {compName.toString()}));	
	    result.passed(smh.getLocalString
			      (getClass().getName() + ".passed",
			   "Servlet session-timeout [ {0} ] element defines the default session timeout interval expressed in whole minutes.",
			   new Object[] {sessionTimeout.toString()}));
	} else {
	    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failed",
			   "Error: Servlet session-timeout [ {0} ] element does not define the default session timeout interval expressed in whole minutes.",
			   new Object[] {sessionTimeout.toString()}));
	}
	return result;
    }
}
