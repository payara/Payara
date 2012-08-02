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

package com.sun.enterprise.tools.verifier.tests.web.runtime;

import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.tests.web.*;
import org.glassfish.web.deployment.runtime.*;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>

public class ASWebProperty extends WebTest implements WebCheck{


public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();

	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneFailed = false;
        boolean notApp = false;
        
        try{
            WebProperty[] webProps = ((SunWebAppImpl)descriptor.getSunDescriptor()).getWebProperty();
            if (webProps.length > 0){
                oneFailed=checkWebProperties(webProps,result ,descriptor, this ) ;
            }else{
            notApp = true;
            addNaDetails(result, compName);
	    result.notApplicable(smh.getLocalString
                                 (getClass().getName() + ".notApplicable",
                                  "NOT APPLICABLE [AS-WEB sun-web-app] web property element not defined within the web archive [ {0} ].",
                                  new Object[] {descriptor.getName()}));
        }
        if (oneFailed) {
            result.setStatus(Result.FAILED);
        } else if(notApp) {
            result.setStatus(Result.NOT_APPLICABLE);
        }else {
            result.setStatus(Result.PASSED);
        }

        }catch(Exception ex){
            oneFailed = true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
                    "FAILED [AS-WEB sun-web-app] could not create the web-property object"));
        }

	return result;
    }
   public static boolean checkWebProperties(WebProperty[] webProps, Result result ,WebBundleDescriptor descriptor, Object obj ) {
       String compName = result.getComponentName();
       String name;
       String value;
       boolean oneFailed = false;
       String[] names=null;
       if (webProps.length > 0){
           names=new String[webProps.length];
           for (int rep=0; rep<webProps.length; rep++ ){
               name = webProps[rep].getAttributeValue(WebProperty.NAME); //*************needs verification from ko]umar Sg
               value = webProps[rep].getAttributeValue(WebProperty.VALUE);
               names[rep]=name;
               if (name !=null && value !=null && name.length() != 0 && value.length() != 0){
                   //check if the name already exist in this web-prop
                   boolean isDuplicate=false;
                   for(int rep1=0;rep1<rep;rep1++)
                   {
                       if(name.equals(names[rep1])){
                           isDuplicate=true;
                            break;
                       }

                    }

                    if(!isDuplicate){
                        result.addGoodDetails(smh.getLocalString
                                                ("tests.componentNameConstructor",
                                                 "For [ {0} ]",
                                                 new Object[] {compName}));
                        result.passed(smh.getLocalString
					  (obj.getClass().getName() + ".passed",
					   "PASSED [AS-WEB property] Proper web property with name  [ {0} ] and value [ {1} ] defined.",
					   new Object[] {name, value}));
                    }else{
                    if (!oneFailed)
                        oneFailed = true;
                    result.addErrorDetails(smh.getLocalString
                                            ("tests.componentNameConstructor",
                                             "For [ {0} ]",
                                             new Object[] {compName}));
                    result.failed(smh.getLocalString
                                      (obj.getClass().getName() + ".failed2",
                                      "FAILED [AS-WEB property] name [ {0} ] and value [ {1} ], the name must be unique in the entire list of web property.",
                                      new Object[] {name, value}));
                    }

               }else{
                   if (!oneFailed)
                        oneFailed = true;
                   result.addErrorDetails(smh.getLocalString
                                            ("tests.componentNameConstructor",
                                             "For [ {0} ]",
                                             new Object[] {compName}));
                   result.failed(smh.getLocalString
                                      (obj.getClass().getName() + ".failed1",
                                      "FAILED [AS-WEB property] name [ {0} ] and value [ {1} ], attributes must be of finite length.",
                                      new Object[] {name, value}));
               }
            }

        }

        return oneFailed;

   }

}

