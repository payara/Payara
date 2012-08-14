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

import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import org.glassfish.web.deployment.runtime.Cache;
import org.glassfish.web.deployment.runtime.CacheMapping;
import org.glassfish.web.deployment.runtime.CacheHelper;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;

import java.util.Set;
import java.util.Iterator;


//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>

public class ASCacheMapping extends ASCache implements WebCheck {


    boolean oneWarning = false;
    boolean oneFailed = false;	
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean notApp = false;
        //Cache cache = getCache(descriptor);
        try{
            Cache cache = ((SunWebAppImpl)descriptor.getSunDescriptor()).getCache();
            CacheMapping[] cacheMapp=null;
            String servletName=null;
            String urlPattern=null;
            String timeout=null;
            String[] httpMethods;
            //boolean[] keyFields;
            String cacheHelperRef;
            if (cache != null ){
                cacheMapp=cache.getCacheMapping();
            }
            if (cache != null && cacheMapp !=null && cacheMapp.length != 0 ){
                for(int rep=0;rep < cacheMapp.length;rep++){
                    servletName = cacheMapp[rep].getServletName();

                    urlPattern = cacheMapp[rep].getURLPattern();

                    timeout = cacheMapp[rep].getTimeout();
                    httpMethods = cacheMapp[rep].getHttpMethod();
                    cacheHelperRef = cacheMapp[rep].getCacheHelperRef();
                    if(servletName != null){
                        if(validServletName(servletName,descriptor)){
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString
                                    (getClass().getName() + ".passed",
                                            "PASSED [AS-WEB cache-mapping] servlet-name  [ {0} ] properly defined.",
                                            new Object[] {servletName}));
                            }
                        else{
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                            "FAILED [AS-WEB cache-mapping] servlet-name [ {0} ], does not exist in the web application.",
                                            new Object[] {servletName}));
                            oneFailed = true;
                            }
                    }
                    else if(urlPattern !=null){
                        if(validURL(urlPattern)){
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString
                                    (getClass().getName() + ".passed1",
                                            "PASSED [AS-WEB cache-mapping] url-pattern [ {0} ] properly defined.",
                                            new Object[] {urlPattern}));
                            }else{
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed1",
                                            "FAILED [AS-WEB cache-mapping] url-pattern [ {0} ], does not exist in  the web application.",
                                            new Object[] {urlPattern}));
                            oneFailed = true;
                        }
                    }
                    if(cacheHelperRef !=null){
                        //test cache-helper-ref
                        if(validCacheHelperRef(cacheHelperRef,cache)){
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString
                                    (getClass().getName() + ".passed2",
                                            "PASSED [AS-WEB cache-mapping] cache-helper-ref element [ {0} ]  defined properly.",
                                            new Object[] {cacheHelperRef}));
                        }
                        else{
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed2",
                                            "FAILED [AS-WEB cache-mapping] cache-helper-ref [ {0} ] not valid, either empty or  cache-helper not defined for it.",
                                            new Object[] {cacheHelperRef}));
                            }
                    }else{
                        if(timeout != null){
                            int i = rep +1;
                            String timeoutName = getXPathValue("sun-web-app/cache/cache-mapping["+i+"]/timeout/@name");
                            if(validTimeout(timeout,timeoutName)){
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString
                                        (getClass().getName() + ".passed3",
                                                "PASSED [AS-WEB cache-mapping] timeout element [ {0} ] properly defined.",
                                                new Object[] {new Integer(timeout)}));
                            }else{
                                oneFailed = true;
                                addErrorDetails(result, compName);
                                result.failed(smh.getLocalString
                                      (getClass().getName() + ".failed3",
                                      "FAILED [AS-WEB cache-mapping] timeout element [{0}] must be a Long ( Not less than -1 and not more that MAX_LONG) and its name attribute [{1}] can not be empty/null.",
                                      new Object[] {timeout,timeoutName}));
                            }
                        }
                        //<addition author="irfan@sun.com" [bug/rfe]-id="4706026" >
                        int j = rep+1;
                        int count = getCountNodeSet("sun-web-app/cache/cache-mapping["+j+"]/refresh-field");
                        if(count>0) // refresh field element present
                        {
                            String cacheMapName = null;
                            if(cacheMapp[rep].getServletName()!=null)
                                cacheMapName = cacheMapp[rep].getServletName();
                            else
                                cacheMapName = cacheMapp[rep].getURLPattern();
                            String name = getXPathValue("sun-web-app/cache/cache-mapping["+j+"]/refresh-field/@name");
                            if(name!=null && name.length()!=0)
                            {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString(getClass().getName()+".passed3a",
                                        "PASSED [AS-WEB cache-mapping] for {0}, refresh-field name [{1}] has been furnished",
                                        new Object[]{cacheMapName,name}));
                            }else
                            {
                                addErrorDetails(result, compName);
                                result.failed(smh.getLocalString(getClass().getName()+".failed3a",
                                        "FAILED [AS-WEB cache-mapping] for {0}, refresh-field name [{1}] cannot be empty/null string",
                                        new Object[]{cacheMapName,name}));
                                        oneFailed = true;
                            }
                        }
                        //</addition>
                        if(checkHTTPMethodList(httpMethods,result,compName,descriptor)){

                        }
                        else{
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                            (getClass().getName() + ".failed4",
                            "FAILED [AS-WEB cache-mapping] http-method - List of HTTP methods is not proper, "+
                            " atleast one of the method name in the list is empty/null "));

                        }
                       //<addition author="irfan@sun.com" [bug/rfe]-id="4706026" >
                        if ((getCountNodeSet("sun-web-app/cache/cache-mapping["+j+"]/key-field"))>0)
                            testForKeyFields(cache, j, result, compName);
                        //</addition>
                    }
                }
            }else{
                notApp = true;
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString
                                 (getClass().getName() + ".notApplicable",
                                  "NOT APPLICABLE [AS-WEB cache-mapping] element not defined",
                                  new Object[] {descriptor.getName()}));
            }
            if (oneFailed){
                result.setStatus(Result.FAILED);
            }else if(oneWarning){
                result.setStatus(Result.WARNING);
            }
            else if(notApp){
                result.setStatus(Result.NOT_APPLICABLE);
            }else{
                result.setStatus(Result.PASSED);
            }
        }catch(Exception ex){
            oneFailed = true;
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                (getClass().getName() + ".failed7",
                    "FAILED [AS-WEB cache-mapping] could not create the cache object"));
        }
	return result;
    }

    boolean validURL(String URL){
          boolean valid=false;
          if (URL != null) {
              if ((URL.startsWith("/")) ||
                ((URL.startsWith("/")) && (URL.endsWith("/*"))) ||
                (URL.startsWith("*."))) {
                            valid = true;
              }
          }
          return valid;
    }

    boolean validServletName(String servletName, WebBundleDescriptor descriptor){
        boolean valid=false;
          if (servletName != null && servletName.length() != 0){
              Set servlets = descriptor.getServletDescriptors();
              Iterator itr = servlets.iterator();
                    // test the servlets in this .war
                    while (itr.hasNext()){
                        String thisServletName = ((WebComponentDescriptor)itr.next()).getCanonicalName();
                        if (servletName.equals(thisServletName)){
                            valid = true;
                            break;
                        }
                    }
          }
          return valid;
    }

    boolean validTimeout(String timeout,String timeoutName){
          boolean valid=false;
          if (timeout != null) {
              try{
                  long timeoutValue = Long.parseLong(timeout);
                  if(timeoutValue >= -1 && timeoutValue <= Long.MAX_VALUE){
                    //if(Integer.parseInt(timeout) >= -1)  {      //4705932      
                        //check the name is non-empty      
                        if(timeoutName !=null && timeoutName.length() != 0)
                                valid = true;
                    }
              }  catch(NumberFormatException exception){ 
                  //nothing required
              }
             
          } else {//since optional field
               valid = true;
          }
          return valid;
    }

    boolean checkHTTPMethodList(String[] httpMethods, Result result, ComponentNameConstructor compName,WebBundleDescriptor descriptor ){

          boolean valid=true;
          if (httpMethods != null) {
             for(int rep=0;rep < httpMethods.length;rep++){
                if(httpMethods[rep]!=null &&  !(httpMethods[rep].trim().equals("")))
                 {
                    if((httpMethods[rep].equalsIgnoreCase("GET")
                      || httpMethods[rep].equalsIgnoreCase("POST") || httpMethods[rep].equalsIgnoreCase("HEAD")))
                    {
                        addGoodDetails(result, compName);
                        result.passed(smh.getLocalString
					  (getClass().getName() + ".passed4",
					   "PASSED [AS-WEB cache-mapping ] http-method  [ {0} ] properly defined in the WEB DD.",
					   new Object[] {httpMethods[rep]}));
                    }else{
                        oneWarning = true;
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString
					   (getClass().getName() + ".warning",
					    "WARNING [AS-WEB cache-mapping] http-method name [ {0} ] present, suggested to be GET | POST | HEAD.",
					    new Object[] {httpMethods[rep]}));
                    }

                }
                else{
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
					   (getClass().getName() + ".failed5",
					    "FAILED [AS-WEB cache-mapping] http-method name [ {0} ] is invalid, it should be GET | POST | HEAD.",
					    new Object[] {httpMethods[rep]}));

                    valid=false;
                }

             }

          }
          return valid;
    }
    
    boolean validCacheHelperRef(String helperRef, Cache cache){
          boolean valid=false;
          if (helperRef.length() != 0){
            CacheHelper[] helperClasses=null;
            CacheHelper helperClass=null;
            String name=null;
            if (cache != null ){
               helperClasses=cache.getCacheHelper();
            }
            if (cache != null && helperClasses !=null){
                for(int rep=0;rep < helperClasses.length;rep++){
                    helperClass=helperClasses[rep]; 
                    if(helperClass==null)
                        continue;
                    int i = rep +1;
                    name = getXPathValue("sun-web-app/cache/cache-helper["+i+"]/@name");
                    if(helperRef.equals(name)){
                        valid=true; 
                        break;
                    }  
                }
            }
          }
          return valid;
    }

    public void testForKeyFields(Cache cache, int mapCount, Result result, ComponentNameConstructor compName)
    {
        int keyCount = getCountNodeSet("sun-web-app/cache/cache-mapping["+mapCount+"]/key-field");
        String cacheMapName = getXPathValue("sun-web-app/cache/cache-mapping["+mapCount+"]/servlet-name");
        if (cacheMapName == null)
        cacheMapName = getXPathValue("sun-web-app/cache/cache-mapping["+mapCount+"]/url-pattern");
        if (keyCount>0){
            for (int k=1;k<=keyCount;k++){
                String  name = getXPathValue("sun-web-app/cache/cache-mapping["+mapCount+"]/key-field["+k+"]/@name");
                if(name!=null && name.length()==0)
                {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString(getClass().getName()+".failed6",
                            "FAILED [AS-WEB cache-mapping] for {0}, key-field #{1}, name cannot be an empty string",
                            new Object[]{cacheMapName,new Integer(k)}));
                    oneFailed = true;
                }
                else
                {
                    addGoodDetails(result, compName);
                    result.passed(smh.getLocalString(getClass().getName()+".passed5",
                            "PASSED [AS-WEB cache-mapping] for {0}, key-field #{1} name value furnished",
                            new Object[]{cacheMapName,new Integer(k)}));
                }
            }
        }
    }
}
