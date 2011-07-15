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

package com.sun.enterprise.tools.verifier.tests.webservices;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import java.util.*;
import com.sun.enterprise.tools.verifier.tests.*;
import java.lang.reflect.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_27; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: The urlpattern of the servlet-mapping must be an exact match pattern 
 *   (i.e. it must not contain an asterisk (?*?)).
 */

public class ServletUrlPatternExactCheck extends WSTest implements WSCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint desc) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean pass = true;

        if (desc.implementedByWebComponent()) {
            WebBundleDescriptor webBundle = (WebBundleDescriptor)desc.getBundleDescriptor();
            WebComponentDescriptor webComponent =
                (WebComponentDescriptor) webBundle.
                getWebComponentByCanonicalName(desc.getWebComponentLink());
            if(webComponent != null && webComponent.isServlet()) {
                Enumeration en = webComponent.getUrlPatterns();
                while(en.hasMoreElements()) {
                  String pattern =(String) en.nextElement();
                  if (pattern.indexOf("*") == -1) {
                     // result.pass
                      result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                      result.passed(smh.getLocalString (getClass().getName() + ".passed",
                      "The urlpattern for this servlet-mapping [{0}] is exact.",
                      new Object[] {pattern}));
                     
                  }
                  else {
                     // result.fail
                     result.addErrorDetails(smh.getLocalString
                                  ("tests.componentNameConstructor", "For [ {0} ]",
                                   new Object[] {compName.toString()}));
                     result.failed(smh.getLocalString (getClass().getName() + ".failed",
                     "The urlpattern for this servlet-mapping [{0}] contains '*' and is not exact.",
                     new Object[] {pattern}));

                     pass = false;
                  }
                  // we assume there is one servlet-mapping specified, 
                  // this check is done in another test
                  break;
                }
            }
         }
         else {
           //result.notapp
            result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString
                 (getClass().getName() + ".notapp",
                 "Not Applicable since this not a JAX-RPC Service Endpoint."));
         }

        return result;
    }
 }

