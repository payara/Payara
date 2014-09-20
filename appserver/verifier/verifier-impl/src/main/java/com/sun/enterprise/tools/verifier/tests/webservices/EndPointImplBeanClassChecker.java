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
import java.lang.reflect.*;
import com.sun.enterprise.tools.verifier.tests.*;

// The condition of bean class here is that it does not implement SEI
// So we need to check if it implements all method signatures in SEI.
/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids:  ; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 */
public class EndPointImplBeanClassChecker  extends WSTest implements WSCheck {

    private Class sei;
    private Class bean;
    private Result result;
    private boolean isEjbEndpoint;
    private String schemaVersion;

    public EndPointImplBeanClassChecker (Class seiClz, Class beanClz, Result resultInst, String version ) {

       sei = seiClz;
       bean = beanClz;
       result = resultInst;
       isEjbEndpoint = true;
       schemaVersion = version;
    }

    public EndPointImplBeanClassChecker (Class seiClz, Class beanClz, Result resultInst, 
                                                                         boolean isEjb) {
       sei = seiClz;
       bean = beanClz;
       result = resultInst;
       isEjbEndpoint = isEjb;
    }

    /**
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {
      return null;
    }

    public boolean check (ComponentNameConstructor compName) {

        boolean pass = true;

        // should define default public constructor
        try {
          bean.getConstructor(new Class[0]); 
          // result.pass has public default constructor
          result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
          result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean has a public default Constructor"}));

        }catch (NoSuchMethodException e) {
         //result.fail no default public constructor
         result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
         result.failed(smh.getLocalString
             ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
             new Object[] {"The Service Impl Bean does not have a default public Constructor"}));
         pass = false;
        }catch (LinkageError e){
            Verifier.debug(e);
            pass = false;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.failed1",
                            "Error: [ {0} ] class used in [ {1} ] class cannot be found.",
                            new Object[] {e.getMessage().replace('/','.'), bean.getName()}));
        }

        // class should be public, not final and not abstract
        int modifiers = bean.getModifiers();
        if (Modifier.isPublic(modifiers)) {
           //result.pass
          result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
          result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean is public"}));
         }
         else {
           // result.fail EndPoint Impl class not public
         result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
         result.failed(smh.getLocalString
             ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
             new Object[] {"The Service Impl Bean is not public"}));
           pass = false;
         }

        if (Modifier.isFinal(modifiers)) {
          // result.fail EndPoint Impl class declared final
         result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
         result.failed(smh.getLocalString
             ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
             new Object[] {"The Service Impl Bean is does not implement SEI, but is decalred final"}));
           pass = false;
         }
         else {
          // result.pass
          result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
          result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean class is not declared final"}));
         }

         if (Modifier.isAbstract(modifiers)) {
           // result.fail endpoint impl class cannot be asbtract
         result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
         result.failed(smh.getLocalString
             ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
             new Object[] {"The Service Impl Bean class is decalred final"}));
           pass = false;
         }
         else {
           // result.pass
          result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
          result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean class is not abstract"}));
         }

        // should not define finalize()	
        try {
          if (bean.getDeclaredMethod("finalize", new Class[0]) != null) {
              //result.fail, bean should not declare finalize
              result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.failed(smh.getLocalString
                  ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
                  new Object[] {"The Service Impl Bean class defines finalize() method"}));
              pass = false;
           }
        }catch(NoSuchMethodException e) {
           //result.pass, does not declare finalize
          result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
          result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean class does not define finalize() method"}));
        }catch (LinkageError e){
            Verifier.debug(e);
            pass = false;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.failed1",
                            "Error: [ {0} ] class used in [ {1} ] class cannot be found.",
                            new Object[] {e.getMessage().replace('/','.'), bean.getName()}));
        }
        if (isEjbEndpoint && !(schemaVersion.compareTo("1.1")>0)) {
           // should define a NoArg ejbCreate()
           try {
             if (bean.getDeclaredMethod("ejbCreate", new Class[0]) != null) {
                 //result.pass, bean has  no-arg ejbCreate()
                 result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
                 result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean class defines ejbCreate()"}));
              }
           }catch(NoSuchMethodException e) {
              //result.fail, does not declare ejbCreate
              result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                 "For [ {0} ]", new Object[] {compName.toString()}));
              result.failed(smh.getLocalString
                   ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
                   new Object[] {"The Service Impl Bean class has no ejbCreate()"}));
              pass = false;
           }catch (LinkageError e){
            Verifier.debug(e);
            pass = false;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.failed1",
                            "Error: [ {0} ] class used in [ {1} ] class cannot be found.",
                            new Object[] {e.getMessage().replace('/','.'), bean.getName()}));
        }
           // should define a NoArg ejbRemove()
           try {
             if (bean.getDeclaredMethod("ejbRemove", new Class[0]) != null) {
                 //result.pass, bean has  no-arg ejbRemove()
                 result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
                 result.passed(smh.getLocalString (
                       "com.sun.enterprise.tools.verifier.tests.webservices.passed", "[{0}]",
                       new Object[] {"The Service Impl Bean class defines ejbRemove()"}));
              }
           }catch(NoSuchMethodException e) {
              //result.fail, does not declare ejbRemove
              result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                 "For [ {0} ]", new Object[] {compName.toString()}));
              result.failed(smh.getLocalString
                   ("com.sun.enterprise.tools.verifier.tests.webservices.failed", "[{0}]",
                   new Object[] {"The Service Impl Bean class has no ejbRemove()"}));
              pass = false;
           }catch (LinkageError e){
            Verifier.debug(e);
            pass = false;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString (
                    "com.sun.enterprise.tools.verifier.tests.webservices.failed1",
                    "Error: [ {0} ] class used in [ {1} ] class cannot be found.",
                    new Object[] {e.getMessage().replace('/','.'), bean.getName()}));
        }
        }

     return pass;
    }
    
    public Vector getSEIMethodsNotImplemented() {
 
      Vector<String> ret = new Vector<String>();
      // here the methods could be in the base class as well
      Method[] beanPubMethods = bean.getMethods();
      Method[] seiMethods = sei.getMethods();
      for ( int i=0; i < seiMethods.length; i++) { 
          if (!hasMatchingMethod(seiMethods[i], beanPubMethods)) {
             // doesnt have matching method
             ret.add(seiMethods[i].toString());
          }
      }
      return ret;
    }

    private boolean hasMatchingMethod(Method meth, Method[] tobeChecked) {

     for (int i=0; i < tobeChecked.length; i++) {
         if (WSTest.matchesSignatureAndReturn(meth, tobeChecked[i]))
            return true;
     }
     return false;
    }
 }

