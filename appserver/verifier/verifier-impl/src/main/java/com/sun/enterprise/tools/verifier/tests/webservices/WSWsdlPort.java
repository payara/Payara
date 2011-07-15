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
import javax.xml.namespace.QName;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription:  
 *                     Requirement from Web Service for J2EE, Version 1.0
 *                     Section 7.1.2
 *                    " Port s QName. In addition to specifying the WSDL document, 
 *                     the developer must also specify the WSDL port QName in the 
 *                     wsdl-port element for each Port defined in the deployment descriptor.
 *                     Requirement from Web Service for J2EE, Version 1.0
 *                     7.1.5 Web Services Deployment Descriptor DTD
 *                    <!-- The port-component element associates a WSDL port with a 
 *                    Web service interface and implementation. It defines the name 
 *                    of the port as a component, optional description, optional display name, 
 *                    optional iconic representations, WSDL port QName, Service Endpoint Interface, 
 *                    Service Implementation Bean. Used in: webservices --> 
 *                    <!ELEMENT port-component (description?, display-name?, small-icon?, large-icon?, 
 *                    port-component-name, wsdl-port, service-endpoint-interface, service-impl-bean, handler*)>
 */

public class WSWsdlPort extends WSTest implements WSCheck {
    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {
        
	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean pass = true;
        try {

          javax.xml.namespace.QName wsdlport = descriptor.getWsdlPort();
          // check to see that wsdl-port is specified in the service endpoint.
          if ( wsdlport != null) { 
             // get the local part 
             String localpart = wsdlport.getLocalPart();
             // String namespaceuri = wsdlport.getNamespaceURI();

             if ( localpart == null || localpart.equals("") ) { 
                // Error: localpart is not specified
               pass = false;
             }
             
              //if ( namespaceuri == null || namespaceuri.equals("")) { 
              //pass = false;
              //}

          } else {
            // Error: wsdl-port is missing for this endpoint
            pass = false;
          }

          if (pass) {
              result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed",
                          "The wsdl-port in the webservices.xml file for [{0}] is specified for the endpoint",
                           new Object[] {compName.toString()}));
            }
            else {
             result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.failed(smh.getLocalString (getClass().getName() + ".failed",
               "The  wsdl-port in the webservices.xml file for [{0}] is not correctly specified for the endpoint",
                new Object[] {compName.toString()}));
            }
        }catch (Exception e) {
            //result.fail
            result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {e.getMessage()}));
        }
        return result;
    }
}
