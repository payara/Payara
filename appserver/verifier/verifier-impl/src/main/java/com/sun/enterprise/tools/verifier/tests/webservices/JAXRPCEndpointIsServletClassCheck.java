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
 *   @assertion_ids:  JSR109_WS_20; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: Service Implementations using a JAX-RPC Service Endpoint must be defined 
 *   in the web.xml deployment descriptor file using the servlet-class element.
 */
public class JAXRPCEndpointIsServletClassCheck extends WSTest implements WSCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint wsdescriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        ClassLoader loader = getVerifierContext().getClassLoader();

        if (wsdescriptor.implementedByWebComponent()) {
            WebBundleDescriptor webBundle = (WebBundleDescriptor)wsdescriptor.getBundleDescriptor();
            WebComponentDescriptor webComponent =
                (WebComponentDescriptor) webBundle.
                  getWebComponentByCanonicalName(wsdescriptor.getWebComponentLink());
            if( webComponent != null && webComponent.isServlet()) {
                String servletClass = wsdescriptor.getWebComponentImpl().
                                      getWebComponentImplementation();
                if ((servletClass == null) || (!wsdescriptor.getWebComponentImpl().isServlet())) {
                     //result.fail, webcomponentimpl for webservice is not servlet
                      result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                      result.failed(smh.getLocalString
                      ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
                       new Object[] {"The WebComponent implementation for this JAX-RPC endpoint is not a servlet"}));

                }
                else {
                     // try to load the servlet class
                  try {
                    Class cl = Class.forName(servletClass, false, getVerifierContext().getClassLoader());
                      //result.pass
                      result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                      result.passed(smh.getLocalString (
                          "com.sun.enterprise.tools.verifier.tests.webservices.clpassed", 
                          "The [{0}] Class [{1}] exists and was loaded successfully.",
                           new Object[] {"Servlet Class", servletClass}));

                  }catch (ClassNotFoundException e) {
                      //result.fail could not find servlet class
                      result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                            "For [ {0} ]", new Object[] {compName.toString()}));
                      result.failed(smh.getLocalString (
                            "com.sun.enterprise.tools.verifier.tests.webservices.clfailed",
                            "The [{0}] Class [{1}] could not be Loaded",
                             new Object[] {"Servlet Class", servletClass}));

                  }
                }
            }
            else {
                //result.fail, servlet-link could not be resolved
                result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
                  ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
                  new Object[] {"The servlet-link for this JAX-RPC Endpoint could not be resolved"}));
 
            }
        }
        else {
          //result.notapplicable
          result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
          result.notApplicable(smh.getLocalString
                 ("com.sun.enterprise.tools.verifier.tests.webservices.notapp",
                 "[{0}]", new Object[] {"Not Applicable since this is NOT a JAX-RPC Service Endpoint"}));

        }

        return result;
    }
 }

