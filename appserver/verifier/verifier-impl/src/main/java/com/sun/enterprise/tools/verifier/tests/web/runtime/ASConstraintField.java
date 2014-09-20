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

package com.sun.enterprise.tools.verifier.tests.web.runtime;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.deployment.WebBundleDescriptor;

/** constraint-field *
 *    Attribute: name, scope, cache-on-match, cache-on-match-failure
 */

public class ASConstraintField extends WebTest implements WebCheck {

    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String value = null;
	int count = 0;
        boolean oneFailed = false;
        try{
            count = getCountNodeSet("sun-web-app/cache/cache-mapping/constraint-field");
            if (count>0){
                for(int i=1;i<=count;i++){
                    //name attribute
                    value = getXPathValue("sun-web-app/cache/cache-mapping/constraint-field["+i+"]/@name");
                    if (value==null || value.length()==0){
                        oneFailed = true;
                        result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.failed(smh.getLocalString
                            (getClass().getName() + ".failed",
                            "FAILED [AS-WEB constraint-field] :  name attribute is required",
                            new Object[] {descriptor.getName()}));
                    }else{
                        result.addGoodDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.passed(smh.getLocalString( getClass().getName() + ".passed",
                              "PASSED [AS-WEB constraint-field] : name attribute is {1}",
                              new Object[] {descriptor.getName(),value}));
                    }

                    //scope attribute
                    value = getXPathValue("sun-web-app/cache/cache-mapping/constraint-field["+i+"]/@scope");
                    if (value==null || value.length()==0){
                        result.addNaDetails(smh.getLocalString
	                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                            "NOT APPLICABLE [AS-WEB constraint-field] : scope attribute not defined"));
                    }else{
                        String scopeValue[] = {"context.attribute", "request.header", "request.parameter", "request.cookie", "request.attribute", "session.attribute" };
                        boolean found = false;
                        for (int j=0;j<scopeValue.length;j++){
                            if (scopeValue[j].compareTo(value) ==0){
                                found = true;
                            }
                        }
                        if (found){
                            result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
                            result.passed(smh.getLocalString( getClass().getName() + ".passed1",
                                "PASSED [AS-WEB constraint-field] : scope attribute is {1}",
                                new Object[] {descriptor.getName(),value}));
                        }else{
                            oneFailed = true;
                            result.addErrorDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                            result.failed(smh.getLocalString
                                (getClass().getName() + ".failed1",
                                "FAILED [AS-WEB constraint-field] :  scope attribute must be one of context.attribute, request.header, request.parameter, request.cookie, request.attribute, session.attribute",
                                new Object[] {descriptor.getName()}));
                        }
                    }
                    //cache-on-match % boolean "(yes | no | on | off | 1 | 0 | true | false)">
                    value = getXPathValue("sun-web-app/cache/cache-mapping/constraint-field["+i+"]/@cache-on-match");
                    if (value==null || value.length()==0){
                        result.addNaDetails(smh.getLocalString
	                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable1",
                            "NOT APPLICABLE [AS-WEB constraint-field] : cache-on-match attribute not defined"));
                    }else{
                        String cacheOnMatchValue[] = {"yes", "no", "on", "off", "1", "0", "true", "false" };
                        boolean foundCacheOnMatch = false;
                        for (int j=0;j<cacheOnMatchValue.length;j++){
                            if (cacheOnMatchValue[j].compareTo(value) ==0){
                                foundCacheOnMatch = true;
                            }
                        }
                        if (foundCacheOnMatch){
                            result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
                            result.passed(smh.getLocalString( getClass().getName() + ".passed2",
                                "PASSED [AS-WEB constraint-field] : cache-on-match attribute is {1}",
                                new Object[] {descriptor.getName(),value}));
                        }else{
                            oneFailed = true;
                            result.addErrorDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                            result.failed(smh.getLocalString
                                (getClass().getName() + ".failed2",
                                "FAILED [AS-WEB constraint-field] :  cache-on-match attribute must be one of yes, no, on, off, 1, 0, true, false",
                                new Object[] {descriptor.getName()}));
                        }
                    }

                    //cache-on-match-failure
                    value = getXPathValue("sun-web-app/cache/cache-mapping/constraint-field["+i+"]/@cache-on-match-failure");
                    if (value==null || value.length()==0){
                        result.addNaDetails(smh.getLocalString
	                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable2",
                            "NOT APPLICABLE [AS-WEB constraint-field] : cache-on-match-failure attribute not defined"));
                    }else{
                        String cacheOnMatchFailureValue[] = {"yes", "no", "on", "off", "1", "0", "true", "false" };
                        boolean found = false;
                        for (int j=0;j<cacheOnMatchFailureValue.length;j++){
                            if (cacheOnMatchFailureValue[j].compareTo(value) ==0){
                                found = true;
                            }
                        }
                        if (found){
                            result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]", new Object[] {compName.toString()}));
                            result.passed(smh.getLocalString( getClass().getName() + ".passed3",
                                "PASSED [AS-WEB constraint-field] : cache-on-match-failure attribute is {1}",
                                new Object[] {descriptor.getName(),value}));
                        }else{
                            oneFailed = true;
                            result.addErrorDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                            result.failed(smh.getLocalString
                                (getClass().getName() + ".failed3",
                                "FAILED [AS-WEB constraint-field] :  cache-on-match-failure attribute must be one of yes, no, on, off, 1, 0, true, false",
                                new Object[] {descriptor.getName()}));
                        }
                    }
                }
            }else{
                result.addNaDetails(smh.getLocalString
		    ("tests.componentNameConstructor",
		    "For [ {0} ]",
		    new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable3",
                    "NOT APPLICABLE [AS-WEB sun-web-app] : constraint-field Element not defined"));
            }
            if(oneFailed)
                result.setStatus(Result.FAILED);
        }catch(Exception ex){
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed4",
                    "FAILED [AS-WEB sun-web-app] could not create the constraint-field object"));
        }
	return result;
    }

}
