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

import java.util.Set;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.tests.web.*;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>


public class ASResourceRefName extends WebTest implements WebCheck {

    public Result check(WebBundleDescriptor descriptor) {
        String resrefName;
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
//Start Bugid:4703107
        ResourcePrincipal resPrincipal;
//End Bugid:4703107
        boolean oneFailed = false;
        boolean notApp = false;
        try{
            Set<ResourceReferenceDescriptor> resRefs = descriptor.getResourceReferenceDescriptors();
            if (resRefs != null && resRefs.size() > 0) {
                for (ResourceReferenceDescriptor resRef : resRefs) {
                    resrefName = resRef.getName();
                    if (validResRefName(resrefName,descriptor)) {
                        addGoodDetails(result, compName);
                        result.passed(smh.getLocalString
                                (getClass().getName() + ".passed",
                                        "PASSED [AS-WEB sun-web-app] resource-ref name [ {0} ] properly defined in the war file.",
                                        new Object[] {resrefName}));

                    }
                    else {
                        oneFailed = true;
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName() + ".failed",
                                        "FAILED [AS-WEB sun-web-app] resource-ref name [ {0} ] is not valid, either empty or not defined in web.xml.",
                                        new Object[] {resrefName}));
                    }
                    //Start Bugid:4703107
                    resPrincipal = resRef.getResourcePrincipal();
                    if(resPrincipal != null){
                        boolean defResourcePrincipalValid = true;
                        String defaultname = resPrincipal.getName();
                        String defaultpassword = resPrincipal.getPassword();
                        if((defaultname == null)||(defaultname.length() == 0)){
                            oneFailed=true;
                            defResourcePrincipalValid = false;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed2",
                                            "FAILED [AS-WEB resource-ref] name field in DefaultResourcePrincipal of ResourceRef [ {0} ] is not specified or is an empty string.",
                                            new Object[] {resrefName}));
                        }
                        if((defaultpassword == null)||(defaultpassword.length() == 0)){
                            oneFailed=true;
                            defResourcePrincipalValid = false;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed3",
                                            "FAILED [AS-WEB resource-ref] password field in DefaultResourcePrincipal of ResourceRef [ {0} ] is not specified or is an empty string.",
                                            new Object[] {resrefName}));
                        }
                        if(defResourcePrincipalValid){
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString
                                    (getClass().getName() + ".passed3",
                                            "PASSED [AS-WEB resource-ref]  DefaultResourcePrincipal of ResourceRef [ {0} ] properly defined",
                                            new Object[] {resrefName}));
                        }
                    }
                    //End Bugid:4703107
                }
            } else {
                notApp = true;
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString
                        (getClass().getName() + ".notApplicable",
                                "NOT APPLICABLE [AS-WEB sun-web-app] resource-ref element not defined in the web archive [ {0} ].",
                                new Object[] {descriptor.getName()}));
            }
            if (oneFailed) {
                result.setStatus(Result.FAILED);
            } else if(notApp) {
                result.setStatus(Result.NOT_APPLICABLE);
            }else {
                result.setStatus(Result.PASSED);
                addGoodDetails(result, compName);
                result.passed
                        (smh.getLocalString
                        (getClass().getName() + ".passed2",
                                "PASSED [AS-WEB sun-web-app] resource-ref element(s) are valid within the web archive [ {0} ] .",
                                new Object[] {descriptor.getName()} ));
            }
        }catch(Exception ex){
            oneFailed=true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failed4", "FAILED [AS-WEB resource-env-ref] Could not create the resource-ref"));

        }
        return result;
    }

    boolean validResRefName(String name,WebBundleDescriptor descriptor){
        boolean valid =true;
        if(name !=null && name.length()!=0) {
            try{
                descriptor.getResourceReferenceByName(name);
            }
            catch(IllegalArgumentException e){
                valid=false;
            }
        }  else{
            valid=false;

        }

        return valid;
    }
}
