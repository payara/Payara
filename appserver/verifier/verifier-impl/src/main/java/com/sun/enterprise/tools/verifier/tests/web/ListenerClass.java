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

import java.util.Enumeration;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.*;
/** 
 * Super class for all Listener tests.
 * 
 * @author Jerome Dochez
 * @version 1.0
 */
public abstract class ListenerClass extends WebTest implements WebCheck {
    
    /**
     * <p>
     * Run the verifier test against a declared individual listener class
     * </p>
     *
     * @param result is used to put the test results in
     * @param listenerClass is the individual listener class object to test
     * @return true if the test pass
     */    
    abstract protected boolean runIndividualListenerTest(Result result, Class listenerClass);
    
    /** 
     * Listener class must implement a no arg constructor.
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {
        
        AppListenerDescriptor listener = null;
        Enumeration listenerEnum;
        Result result;
        boolean oneFailed = false;
        Class listenerClass = null;   
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	
	listenerEnum = descriptor.getAppListenerDescriptors().elements();
	if (listenerEnum.hasMoreElements()) {
            result = loadWarFile(descriptor);
            while (listenerEnum.hasMoreElements()) {
		listener = (AppListenerDescriptor)listenerEnum.nextElement();

                if (listener.getListener().equals(smh.getLocalString("JAXRPCContextListener","com.sun.xml.rpc.server.http.JAXRPCContextListener"))) {
	            result.addGoodDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
                    result.passed(smh.getLocalString (getClass().getName() + ".passed1",
                    "Listener Class Name is [ {0} ], make sure it is available in classpath at runtime.", 
		       new Object[] {listener.getListener()}));
                  continue;
                }

                if ("".equals(listener.getListener())) {
	          result.addErrorDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
                  result.failed(smh.getLocalString (getClass().getName() + ".failed",
                    "Empty or Null String specified for Listener Class Name in [ {0} ].", 
		       new Object[] {compName.toString()}));
                  oneFailed = true;
                  continue;
                }

                listenerClass = loadClass(result, listener.getListener());                
                if (!runIndividualListenerTest(result, listenerClass)) 
                    oneFailed=true;                
 	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
            result = getInitializedResult();
            result.setStatus(Result.NOT_APPLICABLE);
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.tests.web.ListenerClass" + ".notApplicable",
		 "There are no listener components within the web archive [ {0} ]",
		 new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
