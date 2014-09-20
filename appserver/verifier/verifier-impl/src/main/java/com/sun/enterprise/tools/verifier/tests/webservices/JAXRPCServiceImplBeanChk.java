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
 *   @assertion_ids: JSR109_WS_10; JSR109_WS_11; JSR109_WS_12; JSR109_WS_13; 
 *                   JSR109_WS_14; JSR109_WS_47;
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *
 *   The Service Implementation Bean  must have a default public constructor.
 *
 *   The Service Implementation Bean may implement the Service Endpoint Interface as defined by 
 *   the JAX-RPC Servlet model. The bean must implement all the method signatures of the SEI.  
 *   The business methods of the bean must be public and must not be static. 
 *
 *   If the Service Implementation Bean does not implement the SEI, the business methods 
 *   must not be final, must not be static.The bean must implement all the method signatures 
 *   of the SEI.
 *
 *   The Service Implementation Bean  class must be public, must not be final and must 
 *   not be abstract.
 *
 *   The Service Implementation Bean class must not define the finalize() method.
 *
 *   All the exceptions defined in the throws clause of the matching method of the session bean 
 *   class must be defined in the throws clause of the method of the web service endpoint 
 *   interface. 
 *
 */
public class JAXRPCServiceImplBeanChk extends WSTest implements WSCheck {

    /**
     * @param descriptor the  WebService deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {
   
      Result result = getInitializedResult();
      ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

      boolean pass = true;

      if (descriptor.implementedByWebComponent()) {

          Class<?> bean = loadImplBeanClass(descriptor, result);
          if (bean == null) {
            result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
            result.failed(smh.getLocalString
              ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
              new Object[] {"Could not Load the Service Implementation Bean class for the JAX-RPC Endpoint"}));
 
          }
          else {
        
          // get hold of the SEI Class
          String s = descriptor.getServiceEndpointInterface();

          if ((s == null)  || (s.length() == 0)){
               // internal error, should never happen
               result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {"SEI Class Name is Null"}));
          }

          Class<?> sei = loadSEIClass(descriptor, result);

          if (sei == null) {
             result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.failed(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.tests.webservices.WSTest.SEIClassExists",
                 "Error: Service Endpoint Interface class [ {0} ]  not found.",
                 new Object[] {descriptor.getServiceEndpointInterface()}));
           }
           else {
            boolean implementsSEI = sei.isAssignableFrom(bean);
            EndPointImplBeanClassChecker checker = 
            new EndPointImplBeanClassChecker(sei,bean,result,false); 
            if (implementsSEI) {
              // result.passed 
               result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
               result.passed(smh.getLocalString (
                          "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                           new Object[] {"The Service Impl Bean implements SEI"}));

            }
            else {
         
               // business methods of the bean should be public, not final and not static
               // This check will happen as part of this call
               Vector notImpl = checker.getSEIMethodsNotImplemented();
               if (notImpl.size() > 0) {
                 // result.fail, Set the not implemented methods into the result info??
                 result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
                 result.failed(smh.getLocalString
                 ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
                 new Object[] {"The Service Implementation Bean Does not Implement ALL SEI Methods"}));

               }
               else {
                 // result.pass :All SEI methods implemented
                 result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
                 result.passed(smh.getLocalString (
                          "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                           new Object[] {"The Service Impl Bean implements  all Methods of the SEI"}));

               }
            }

            // class should be public, not final and not abstract
            // should not define finalize()	
           if (checker.check(compName)) {
                // result.passed  stuff done inside the check() method nothing todo here
              result.setStatus(Result.PASSED);
            }
            else {
                // result.fail :  stuff done inside the check() method nothing todo here
                result.setStatus(Result.FAILED);
            }
         }
       }
    }
    else {
         // result.notapplicable
         result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
         result.notApplicable(smh.getLocalString
                 ("com.sun.enterprise.tools.verifier.tests.webservices.notapp",
                 "[{0}]", new Object[] {"Not Applicable since this is an EJB Service Endpoint"}));
    }

   return result;
  }

 }

