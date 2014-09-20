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

package com.sun.enterprise.tools.verifier.tests.appclient;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import java.lang.ClassLoader;

/** 
 * AppClient callback handler test.
 * The AppClient provider may provide a JAAS callback handler.
 */
public class AppClientCallbackHandler extends AppClientTest implements AppClientCheck { 

      

    /** 
     * AppClient name test.
     * The AppClient provider must assign a display-name to each AppClient module
     *
     * 
     * @param descriptor the app-client deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(ApplicationClientDescriptor descriptor) {
	Result result = getInitializedResult();
ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	String callbackHandler = descriptor.getCallbackHandler();

	if(callbackHandler == null) {
		result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));	
		result.passed(smh.getLocalString(getClass().getName() + ".passed", 
						 "AppClient callback handler is not specified"));
	}
	else if (callbackHandler.length() > 0) 
	{
	    try {
	        VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
            Class c = Class.forName(callbackHandler, false, getVerifierContext().getClassLoader());
	        Object obj = c.newInstance();
	        if( obj instanceof javax.security.auth.callback.CallbackHandler) {
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));	
		    result.passed(smh.getLocalString(getClass().getName() + ".passed1", "AppClient callback handler is : [ {0} ] and is loadable", new Object [] {callbackHandler}));
	        } else {
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString(getClass().getName() + ".failed", "Error: AppClient callback handler is not loadable."));
	        }
	    } catch (Exception e) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString(getClass().getName() + ".failed", "Error: AppClient callback handler is not loadable."));
	    }
	} else {
		// it's blank, test should not pass
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString(getClass().getName() + ".failed1", "Error: AppClient callback handler must not be blank."));
	} 
	return result;
    }
}
