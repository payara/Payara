/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.util.Collection;
import java.util.Iterator;

/* 
 *   @class.setup_props: ; 
 */ 

/* 
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_1
 *   @test_Strategy:  
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription :If the Service Implementation Bean is an EJB, 
 *   the transaction attributes for the methods defined by the SEI do not include Mandatory.
 */ 
public class SEIEJBTxAttrChk extends WSTest implements WSCheck {

    /**
     * @param descriptor the  WebService deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint wsdescriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean pass = true;

        if (wsdescriptor.implementedByEjbComponent()) {

          EjbDescriptor descriptor = (EjbDescriptor) wsdescriptor.getEjbComponentImpl();

	  try  {
             ContainerTransaction ctx = descriptor.getContainerTransaction();

             if ((ctx != null) && 
                 (ContainerTransaction.MANDATORY.equals(ctx.getTransactionAttribute()))) {
                 // Call result.failed here : All methods are having Mandatory TX
                  result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                  result.failed(smh.getLocalString (getClass().getName() + ".failed",
                  "[{0}] of this WebService [{1}] have Mandatory Transaction Attribute.",
                  new Object[] {"All the methods", compName.toString()}));

                 return result;
             }

             Collection txMethDescs = descriptor.getTransactionMethodDescriptors();

             // get hold of the SEI Class
             String s = descriptor.getWebServiceEndpointInterfaceName();

             if (s == null) {
               // internal error, should never happen
                result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {"Service Endpoint Interface Class Name Null"}));
                pass = false;
             }	
             ClassLoader cl = getVerifierContext().getClassLoader();
             Class sei = null;

             try {
                sei = Class.forName(s, false, cl);
             }catch(ClassNotFoundException e) {
               result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {"Could not Load Service Endpoint Interface Class"}));
                pass = false;
             }

             Iterator it = txMethDescs.iterator(); 
             while (it.hasNext()) {
               // need to check if this method is part of SEI
               MethodDescriptor methdesc =(MethodDescriptor)it.next();
              if (isSEIMethod(methdesc, descriptor, sei, cl)) {
                  ctx = descriptor.getContainerTransactionFor(methdesc);
                  if ((ctx != null) && 
                     (ContainerTransaction.MANDATORY.equals(ctx.getTransactionAttribute()))) {
                     // Call result.failed here with Method details here
                     result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                     result.failed(smh.getLocalString (getClass().getName() + ".failed",
                     "[{0}] of this WebService [{1}] have Mandatory Transaction Attribute.",
                     new Object[] {methdesc.getName(), compName.toString()}));
                     pass = false;
                   }
               }
             }
           } catch (Exception e) {
             // Call result.addErrorDetails here with exception details
               result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {e.getMessage()}));
                pass = false;
           }

          if (pass) {

           result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
           result.passed(smh.getLocalString (getClass().getName() + ".passed",
                    "None of the methods of this WebService [{0}] have Mandatory Transaction Attribute.",
                           new Object[] {compName.toString()}));

          }
          
          return result;
         }
         else {
          // call result.notapplicable
          result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
          result.notApplicable(smh.getLocalString (getClass().getName() + ".notapp",
                 "Not applicable since this is not an EJB Service Endpoint.")); 

          return result;
         }
       }
 }

