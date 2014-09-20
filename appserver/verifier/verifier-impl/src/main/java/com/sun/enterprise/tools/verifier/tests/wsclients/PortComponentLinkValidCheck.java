/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.wsclients;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_55; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: The port-component-link element links a port-component-ref to a 
 *   specific port-component required to be made available by a service reference. 
 *   The value of a port-component-link must be the port-component-name of a port-component 
 *   in the same module or another module in the same application unit. The syntax for 
 *   specification follows the syntax defined for ejb-link in the EJB 2.0 specification.
 */

public class PortComponentLinkValidCheck  extends WSClientTest implements WSClientCheck {
    ComponentNameConstructor compName;

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (ServiceReferenceDescriptor descriptor) {

	Result result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();

        boolean pass = true;

        Collection ports = descriptor.getPortsInfo();

        for (Iterator it=ports.iterator(); it.hasNext();) {
            ServiceRefPortInfo ref = (ServiceRefPortInfo)it.next();

            // check if this test is applicable first
            if (!ref.hasPortComponentLinkName()) { 
              //result.notapplicable, since port-comp-link does not exist in port-comp-ref
              result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
              result.notApplicable(smh.getLocalString
                 ( getClass().getName() + ".notapp",
                 "Not applicable since port-comp-link does not exist in port-comp-ref [{0}].",
                  new Object[] {ref.getName()}));
               } 

               else if (ref.getPortComponentLink() != null) {
               pass = true; 
               } 
               else if (!isLinkValid(ref)) {
                     //result.fail ref.getName(), ref.getPortComponentLinkName()
                       result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                       "For [ {0} ]", new Object[] {compName.toString()}));
                       result.failed(smh.getLocalString (getClass().getName() + ".failed",
                       "Invalid port-component-link [{0}] in WebService client [{1}].",
                        new Object[] {ref.getPortComponentLinkName(),compName.toString()}));
                        pass = false;
                }  
              
        }
        if (pass) {
              //result.pass
              result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed",
             "All port-component-link(s) in this service reference are valid."));
        }
        return result;
    }

   private boolean isLinkValid(ServiceRefPortInfo ref) {
   boolean pass = true;

   WebServiceEndpoint port = null;

      String linkName = ref.getPortComponentLinkName();
// == get the application
     Application application =
                 ref.getServiceReference().getBundleDescriptor().getApplication();

      if(  (linkName != null) && (linkName.length() > 0) && (application != null) )    { 
         int hashIndex = linkName.indexOf('#');
//         boolean absoluteLink = (hashIndex != -1);
         // Resolve <module>#<port-component-name> style link
         String relativeModuleUri = linkName.substring(0, hashIndex);
         String portName = linkName.substring(hashIndex + 1);
// == get bundle(s)
         Set webBundles = application.getBundleDescriptors(WebBundleDescriptor.class);
         Set ejbBundles = application.getBundleDescriptors(EjbBundleDescriptor.class);
// ==
         // iterate through the ejb jars in this J2EE Application
         Iterator ejbBundlesIterator = ejbBundles.iterator();
         EjbBundleDescriptor ejbBundle = null;
// == while...
         while (ejbBundlesIterator.hasNext()) {
         ejbBundle = (EjbBundleDescriptor)ejbBundlesIterator.next();
//         if (Verifier.getEarFile() != null){
           try {
              String archiveuri = ejbBundle.getModuleDescriptor().getArchiveUri();
              if ( relativeModuleUri.equals(archiveuri) ) {
              LinkedList<EjbBundleDescriptor> bundles = new LinkedList<EjbBundleDescriptor>();
                 bundles.addFirst(ejbBundle);
                 for(Iterator iter = bundles.iterator(); iter.hasNext();) {
                    BundleDescriptor next = (BundleDescriptor) iter.next();
                    port = next.getWebServiceEndpointByName(portName);
                    if( port != null ) {
                       pass = true;     
                       break;
                    }
                 }
              }
            }catch(Exception e) {}
//          }
          } // while block

         // iterate through the wars in this J2EE Application
         Iterator webBundlesIterator = webBundles.iterator();
         WebBundleDescriptor webBundle = null;
// == while...
         while (webBundlesIterator.hasNext()) {
         webBundle = (WebBundleDescriptor)webBundlesIterator.next();
//         if (Verifier.getEarFile() != null){
           try {
              String archiveuri = webBundle.getModuleDescriptor().getArchiveUri();
              if ( relativeModuleUri.equals(archiveuri) ) {
              LinkedList<WebBundleDescriptor> bundles = new LinkedList<WebBundleDescriptor>();
                 bundles.addFirst(webBundle);
                 for(Iterator iter = bundles.iterator(); iter.hasNext();) {
                    BundleDescriptor next = (BundleDescriptor) iter.next();
                    port = next.getWebServiceEndpointByName(portName);
                    if( port != null ) {
                       pass = true;    
                       break;
                    }
                 }
              }
            }catch(Exception e) {}
//          }
          } // while block
       } // 
       if ( port == null)
          pass = false;
     return pass; 

    } // end of method 

 }
