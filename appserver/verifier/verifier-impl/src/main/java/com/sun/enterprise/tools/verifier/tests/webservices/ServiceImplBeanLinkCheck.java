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
 *   @assertion_ids:  JSR109_WS_19; JSR109_WS_22; JSR109_WS_23; JSR109_WS_33; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: Service Implementations using a stateless session bean must be 
 *   defined in the ejb-jar.xml deployment descriptor file using the session element.
 *
 *   The developer declares the implementation of the Web service using 
 *   the service-impl-bean element of the deployment descriptor.
 *
 *   For a stateless session bean implementation, the ejb-link element associates the 
 *   port-component with a session element in the ejb-jar.xml. The ejb-link element may 
 *   not refer to a session element defined in another module.
 *
 *   The service-impl-bean element defines the Web service implementation. A service 
 *   implementation can be an EJB bean class or JAX-RPC web component.
 */

public class ServiceImplBeanLinkCheck extends WSTest implements WSCheck {

    public boolean resolveComponentLink(WebServiceEndpoint desc, Result result) {
        boolean resolved = false;
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if( desc.implementedByEjbComponent()) {
            EjbBundleDescriptor ejbBundle = (EjbBundleDescriptor)desc.getBundleDescriptor();
            if( ejbBundle.hasEjbByName(desc.getEjbLink())) {
                EjbDescriptor ejb = ejbBundle.getEjbByName(desc.getEjbLink());
                if (ejb != null) {
                    resolved = true;
                    //result.pass , ejb-link resolved
                     result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                     result.passed(smh.getLocalString (getClass().getName() + ".passed",
                    "[{0}] link of service-impl-bean element resolved successfully.",
                           new Object[] {desc.getEjbLink()}));

                 }
                 else {
                   //result.fail, ejb-link could not be resolved
                    result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString (getClass().getName() + ".failed",
                    "Could not resolve [{0}] link of service-impl-bean element.",
                  new Object[] {desc.getEjbLink()}));

                 }
            }
        } else if( desc.implementedByWebComponent()) {
            WebBundleDescriptor webBundle = (WebBundleDescriptor)desc.getBundleDescriptor();
            WebComponentDescriptor webComponent =
                (WebComponentDescriptor) webBundle.
                  getWebComponentByCanonicalName(desc.getWebComponentLink());
            if( webComponent != null && webComponent.isServlet()) {
                resolved = true;
                //result.pass servlet-link resolved
                result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                         "For [ {0} ]", new Object[] {compName.toString()}));
                result.passed(smh.getLocalString (getClass().getName() + ".passed",
                 "[{0}] link of service-impl-bean element resolved successfully.",
                new Object[] {desc.getWebComponentLink()}));
            }
            else {
                   //result.fail, servlet-link could not be resolved
                   result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                          "For [ {0} ]", new Object[] {compName.toString()}));
                   result.failed(smh.getLocalString (getClass().getName() + ".failed",
                   "Could not resolve [{0}] link of service-impl-bean element.",
                   new Object[] {desc.getWebComponentLink()}));
            }
        }
        return resolved;
    }

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint wsdescriptor) {

	Result result = getInitializedResult();
        boolean pass = resolveComponentLink(wsdescriptor, result);
        return result;
    }
 }

