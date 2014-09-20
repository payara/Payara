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

import com.sun.enterprise.deployment.WebBundleDescriptor;

import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import com.sun.enterprise.tools.verifier.Result;
import org.glassfish.web.deployment.runtime.Cache;
import org.glassfish.web.deployment.runtime.CacheHelper;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.runtime.WebProperty;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>




public class ASCacheHelperClass extends ASCache implements WebCheck {
    public Result check(WebBundleDescriptor descriptor) {
        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
        boolean notApp = false;
        boolean oneWarning=false;
        boolean presentHelper=false;

        try{
            Cache cache = ((SunWebAppImpl)descriptor.getSunDescriptor()).getCache();
            CacheHelper[] helperClasses=null;
            CacheHelper helperClass=null;
            WebProperty[] webProps;
            String name=null;
            String classname=null;
            String[] names=null;
            //to-do vkv# check for class-name attribute.
            if (cache != null )
                helperClasses=cache.getCacheHelper();
            if (cache != null && helperClasses !=null && helperClasses.length > 0)
            {
                names=new String[helperClasses.length];             
                for(int rep=0;rep < helperClasses.length;rep++)
                {
                    helperClass=helperClasses[rep]; 
                    if(helperClass==null)
                        continue;
                    int i = rep+1;
                    name = getXPathValue("sun-web-app/cache/cache-helper["+i+"]/@name");
                    classname = getXPathValue("sun-web-app/cache/cache-helper["+i+"]/@class-name");
                    Class hClass=null;
                    names[rep]=name;

                    if (name != null && name.length() != 0) {
                        //check if the name already exist 
                        boolean isDuplicate=false;
                        for(int rep1=0;rep1<rep;rep1++)
                        {
                            if(name.equals(names[rep1]))
                            {
                                isDuplicate=true;
                                break;
                            }

                        }
                        if(isDuplicate)
                        {
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                      (getClass().getName() + ".failed",
                                      "FAILED [AS-WEB cache-helper] name attribute [ {0} ], must be unique in the entire list of cache-helper.",
                                      new Object[] {name}));
                        }
                        else
                        {
                            if(classname!=null && classname.length()!=0) {
                                hClass = loadClass(result,classname);
                            }
                            if(hClass !=null) 
                                presentHelper=true ;
                            else
                                presentHelper=false ;
                          
                            if(!presentHelper)
                            {
                                addWarningDetails(result, compName);
                                result.warning(smh.getLocalString(
                                                    getClass().getName() + ".error",
                                                    "WARNING [AS-WEB cache-helper] " +
                                                    "name [ {0} ], class not present in the war file.",
                                                    new Object[] {name}));
                                oneWarning = true; 
                            }
                            else
                            {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "PASSED [AS-WEB cache-helper] name  [ {0} ], helper class is valid.",
					   new Object[] {name}));
                            }
                            
                        }
                    } else {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                      (getClass().getName() + ".failed1",
                                      "FAILED [AS-WEB cache-helper] name [ {0} ], either empty or null.",
                                      new Object[] {name}));
		        oneFailed = true;
                   
                    }
                    webProps=helperClass.getWebProperty();
                    if(ASWebProperty.checkWebProperties(webProps,result ,descriptor, this )){
                        oneFailed=true;
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName() + ".failed2",
                                "FAILED [AS-WEB cache-helper] Atleast one name/value pair is not valid in helper-class of [ {0} ].",
                                new Object[] {descriptor.getName()}));
                    }
                }//end of for
            }else
            {
                notApp = true;
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString
                (getClass().getName() + ".notApplicable",
                    "NOT APPLICABLE [AS-WEB cache-helper] There is no cache-helper element for the web application",
                        new Object[] {descriptor.getName()}));
            }
            if (oneFailed) {
            result.setStatus(Result.FAILED);
        } else if(oneWarning){
            result.setStatus(Result.WARNING);
        } else if(notApp) {
            result.setStatus(Result.NOT_APPLICABLE);
        }else {
            result.setStatus(Result.PASSED);
        }
	
    }catch(Exception ex){
    oneFailed = true;
    addErrorDetails(result, compName);
    result.failed(smh.getLocalString
                (getClass().getName() + ".failed3",
                    "FAILED [AS-WEB cache-helper] could not create the cache object"));
    }
        return result;
    }
} 
