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

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_26; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: No more than one servlet mapping may be specified for a servlet that is 
 *   linked to by a port-component.
 */

public class OnlyOneServletMappingCheck extends WSTest implements WSCheck {

    /**
     * @param desc the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint desc) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (desc.implementedByWebComponent()) {
            WebBundleDescriptor webBundle = (WebBundleDescriptor)desc.getBundleDescriptor();
            WebComponentDescriptor webComponent =
                (WebComponentDescriptor) webBundle.
                getWebComponentByCanonicalName(desc.getWebComponentLink());
            if(webComponent != null && webComponent.isServlet()) {
               int sz = getSize(webComponent.getUrlPatternsSet());
               if (sz == 0) {
                  //result.fail , no servlet-mapping for servlet linked to port-component
                  result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                  result.failed(smh.getLocalString
                       (getClass().getName() + ".failed",
                       "Found [{0}] servlet mappings for the servlet linked to by this port-component.",
                       new Object[] {"0"}));
               }
               if (sz > 1) {
                  //result.fail , more than one servlet-mapping for servlet linked to port-component
                  result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                  result.failed(smh.getLocalString
                       (getClass().getName() + ".failed",
                       "Found [{0}] servlet mappings for the servlet linked to by this port-component.",
                       new Object[] {Integer.toString(sz)}));
                } 
                else {
                  //result.pass , one servlet-mapping for servlet linked to port-component
                   result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                   result.passed(smh.getLocalString (getClass().getName() + ".passed",
                   "Found only one servlet mapping for the servlet linked to by this port-component."));

                }
            }
         }
         else {

            result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString(getClass().getName() + ".notapp",
                 " Not applicable since this is Not a JAX-RPC Service Endpoint."));

         }

        return result;
    }

    /**
     * This is a hack, since descriptors from backend contain
     * an extra url pattern.
     * @param urlPatterns
     * @return
     */
    private int getSize(Set urlPatterns) {
        int size = urlPatterns.size();
        if (getVerifierContext().isAppserverMode()) //only if backend
            for (Object url : urlPatterns) {
                String urlPattern = (String)url;
                if(urlPattern.indexOf("__container") != -1)
                    size--;
            }
        return size;
    }
 }

