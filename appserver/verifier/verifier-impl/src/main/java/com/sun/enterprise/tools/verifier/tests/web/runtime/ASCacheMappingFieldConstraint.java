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
import org.glassfish.web.deployment.runtime.Cache;
import org.glassfish.web.deployment.runtime.CacheMapping;
import org.glassfish.web.deployment.runtime.ConstraintField;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>

public class ASCacheMappingFieldConstraint extends ASCache implements WebCheck {



    public Result check(WebBundleDescriptor descriptor) {


	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneFailed = false;
        boolean notApp = false;
        boolean doneAtleastOnce=false;
              
        try{
        Cache cache = ((SunWebAppImpl)descriptor.getSunDescriptor()).getCache();
        CacheMapping[] cacheMapp=null;
        String servletName;
        String urlPattern;
        ConstraintField[] fieldConstraints=null;

        String mappingFor=null;

        if (cache != null ){
          cacheMapp=cache.getCacheMapping();
        }


         if (cache != null && cacheMapp !=null && cacheMapp.length !=0 ) {
            for(int rep=0;rep < cacheMapp.length;rep++){

		servletName = cacheMapp[rep].getServletName();
                urlPattern = cacheMapp[rep].getURLPattern();

                if(servletName !=null)
                    mappingFor=servletName;
                else
                    mappingFor=urlPattern;

                fieldConstraints = cacheMapp[rep].getConstraintField();
                if(fieldConstraints !=null && fieldConstraints.length != 0)
                {
                    for(int rep1=0;rep1 < fieldConstraints.length;rep1++){
                        if(fieldConstraints[rep1] !=null){
                            doneAtleastOnce=true;
                            if(checkFieldConstraint(fieldConstraints[rep1],result,mappingFor,descriptor,compName)){
                                //nothing more required
                            }
                            else{
                                oneFailed = true;
                                addErrorDetails(result, compName);
                                result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                    "FAILED [AS-WEB cache-mapping] List of field-constraint in cache-mapping for [ {0} ] is not proper,  within the web archive/(gf/)sun-web.xml of [ {1} ].",
                                    new Object[] {mappingFor,descriptor.getName()}));

                            }
                        } 
                    }//end of for(int rep1=0;rep1 < fieldConstraints.length;rep1++)
                } 
                
            }//end of for(int rep=0;rep < cacheMapp.length;rep++)

          }else {
              notApp=true;
          }

        if (oneFailed) {
            result.setStatus(Result.FAILED);
        } else if(notApp || !doneAtleastOnce) {
            result.setStatus(Result.NOT_APPLICABLE);
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                                     (getClass().getName() + ".notApplicable",
                                      "NOT APPLICABLE [AS-WEB cache-mapping ] constraint-field not defined in [ {0} ].",
                                      new Object[] {descriptor.getName()}));



        }else {
            result.setStatus(Result.PASSED);
        } 
        }catch(Exception ex){
            oneFailed = true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
                    "FAILED [AS-WEB cache-mapping] could not create the cache object"));
        }
	return result;
    }

    

    boolean checkFieldConstraint(ConstraintField fieldCons, Result result,String mappingFor,
                        WebBundleDescriptor descriptor,ComponentNameConstructor compName) {

          boolean valid=true;
          String fieldName;
          String[] values;
          if (fieldCons != null) {
            // fieldName= fieldCons.getAttributeValue("name");
             fieldName= fieldCons.getAttributeValue(ConstraintField.NAME);
             values=fieldCons.getValue();
             if(fieldName!=null && ! fieldName.equals("")){
                addGoodDetails(result, compName);
                result.passed(smh.getLocalString
					  (getClass().getName() + ".passed1",
					   "PASSED [AS-WEB cache-mapping] Proper field-constraint/fieldName  [ {0} ]  defined for [ {1} ].",
					   new Object[] {fieldName,mappingFor}));

             }else{
                valid=false;
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                                      (getClass().getName() + ".failed1",
                                      "FAILED [AS-WEB cache-mapping] field-constraint/fieldName [ {0} ] defined for [ {1} ], attribute can not be empty.",
                                      new Object[] {fieldName,mappingFor}));

             }

             for(int rep=0;values !=null && rep < values.length;rep++){
                if(values[rep]!=null && ! values[rep].equals("")) {
                    addGoodDetails(result, compName);
                    result.passed(smh.getLocalString
					  (getClass().getName() + ".passed2",
					   "PASSED [AS-WEB cache-mapping]Proper field-constraint/value   [ {0} ]  defined for [ {1} ], within the web archive/(gf/)sun-web.xml of [ {2} ].",
					   new Object[] {values[rep],mappingFor,descriptor.getName()}));

                }else {
                    valid=false;
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                                      (getClass().getName() + ".failed2",
                                      "FAILED [AS-WEB cache-mapping] field-constraint/value [ {0} ] defined for [ {1} ], can not be empty, within the web archive/(gf/)sun-web.xml of [ {2} ].",
                                      new Object[] {values[rep],mappingFor,descriptor.getName()}));

                }

             }
          }

          return valid;
    }
     

}

