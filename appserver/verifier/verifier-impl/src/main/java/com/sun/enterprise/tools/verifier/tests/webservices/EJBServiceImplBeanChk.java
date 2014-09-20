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
import java.lang.*;
import com.sun.enterprise.tools.verifier.tests.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_4; JSR109_WS_5; JSR109_WS_6; JSR109_WS_7; 
 *                   JSR109_WS_8; JSR109_WS_9; JSR109_WS_47;
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: 
 *   The Service Implementation Bean (SLSB) must have a default public constructor.
 *
 *   The Service Implementation Bean may implement the Service Endpoint 
 *   Interface, but it is not required to do so. The bean must implement all the method 
 *   signatures of the SEI. The Service Implementation Bean methods are not required to throw 
 *   javax.rmi.RemoteException. The business methods of the bean must be public and must not 
 *   be final or static. It may implement other methods in addition to those defined by the SEI.
 *
 *   The Service Implementation Bean (SLSB) class must be public, must not be final and must 
 *   not be abstract.
 *
 *   The Service Implementation Bean (SLSB)class must not define the finalize() method.
 *
 *   Currently, Service Implementation Bean (SLSB) must implement the ejbCreate() and 
 *   ejbRemove() methods which take no arguments. This is a requirement of the EJB container, 
 *   but generally can be stubbed out with an empty implementations.
 *
 *   The Stateless Session Bean must implement the javax.ejb.SessionBean interface either 
 *   directly or indirectly.
 *
 *   All the exceptions defined in the throws clause of the matching method of the session bean 
 *   class must be defined in the throws clause of the method of the web service endpoint 
 *   interface. 
 */
public class EJBServiceImplBeanChk extends WSTest implements WSCheck {

    /**
     * @param wsdescriptor the  WebService deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint wsdescriptor) {
   
      Result result = getInitializedResult();
      ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

     EjbDescriptor descriptor = wsdescriptor.getEjbComponentImpl();

      if (descriptor != null) {

          // get hold of the ServiceImplBean Class
          String beanClass = descriptor.getEjbClassName();
          // since non-empty ness is enforced by schema, this is an internal error
          if ((beanClass == null) || ((beanClass != null) && (beanClass.length() == 0))) {
            // internal error 
             result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {"Service Implementation Bean Class Name Null"}));
          }
          Class<?> bean = null;
        
          try {
            bean = Class.forName(beanClass, false, getVerifierContext().getClassLoader());
          } catch (ClassNotFoundException e) {
            result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
                "The [{0}] Class [{1}] could not be Loaded",new Object[] {"Service Impl Bean", beanClass}));
          }
        
          // get hold of the SEI Class
          String s = descriptor.getWebServiceEndpointInterfaceName();

          if ((s == null)  || (s.length() == 0)){
               // internal error, should never happen
             result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {"Service Endpoint Interface Class Name Null"}));
          }

          Class<?> sei = null;

          try {
               sei = Class.forName(s, false, getVerifierContext().getClassLoader());
          }catch(ClassNotFoundException e) {
            result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
                "The [{0}] Class [{1}] could not be Loaded",new Object[] {"SEI", s}));

          }

          // it should be a stateless session bean
          boolean isSLSB = (javax.ejb.SessionBean.class).isAssignableFrom(bean);
          boolean implementsSEI = sei.isAssignableFrom(bean);

          if (!isSLSB) {
            //result.fail does not implement javax.ejb.SessionBean interface
            result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
            result.failed(smh.getLocalString
              ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
              new Object[] {"The Service Implementation Bean Does not Implement SessionBean Interface"}));
          }
          else {
            // result.passed 
             result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.passed(smh.getLocalString (
                          "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                           new Object[] {"The Service Impl Bean implements SessionBean Interface"}));
          }

          EndPointImplBeanClassChecker checker = new EndPointImplBeanClassChecker(sei,bean,result,getVerifierContext().getSchemaVersion()); 

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
      else {
         // result.notapplicable
         result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
         result.notApplicable(smh.getLocalString
                 ("com.sun.enterprise.tools.verifier.tests.webservices.notapp",
                 "[{0}]", new Object[] {"Not Applicable since this is a JAX-RPC Service Endpoint"}));

      }
       return result;
    }
 }

