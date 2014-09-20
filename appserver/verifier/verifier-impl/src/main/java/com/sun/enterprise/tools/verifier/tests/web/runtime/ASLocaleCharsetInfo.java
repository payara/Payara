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
import java.util.*;
import java.nio.charset.*;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>

public class ASLocaleCharsetInfo extends WebTest implements WebCheck {


    public Result check(WebBundleDescriptor descriptor) {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
	boolean oneWarning = false;
	boolean notApp = false;

        String formHintField;
        //Nlsinfo nlsInfo = descriptor.getIasWebApp().getNlsinfo();
        try{
        LocaleCharsetInfo nlsInfo = ((SunWebAppImpl)descriptor.getSunDescriptor()).getLocaleCharsetInfo();
        if (nlsInfo != null){
	    //Test 1: check validity of default-locale
	    Locale[] locales = Locale.getAvailableLocales();
            String defaultLocale=nlsInfo.getAttributeValue(LocaleCharsetInfo.DEFAULT_LOCALE);
	    if(defaultLocale == null || defaultLocale.length() == 0){
            		oneFailed = true;
                	addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                        (getClass().getName() + ".failed",
                                        "FAILED [AS-WEB local-charset-info] Empty "+ defaultLocale +" [ {0} ] is not valid.",
                                        new Object[] {defaultLocale}));
	    }else{
		boolean delocFlag=false;
            	for(int index=0;index < locales.length;index++){
                    if(defaultLocale.equalsIgnoreCase(locales[index].toString())){
                        delocFlag =true;
			break;
                     }
                }	
		if(delocFlag){
                	addGoodDetails(result, compName);
                	result.passed(smh.getLocalString
					(getClass().getName() + ".passed",
                          		"PASSED [AS-WEB locale-charset-info] Properly "+ defaultLocale +"  [ {0} ] defined in the war file.",
                           		new Object[] {defaultLocale}));

		}else{
			oneWarning = true;
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString
                                        (getClass().getName() + ".warning",
                                        "WARNING [AS-WEB local-charset-info] attribute default_locale [ {0} ] is not valid.",
                                        new Object[] {defaultLocale}));
            	}
           }
	   //Test 2: check validity of charset
	   LocaleCharsetMap[] localeCharMaps=nlsInfo.getLocaleCharsetMap();		
	   for (int rep=0; rep<localeCharMaps.length; rep++ ) {
	   	//Test 2.1: check validity of each locale
		boolean charMapLocFlag=false;
                String locale = localeCharMaps[rep].getAttributeValue(LocaleCharsetMap.LOCALE);
                if (locale == null || locale.length() == 0){
                                        oneFailed = true;
                                        addErrorDetails(result, compName);
                                        result.failed(smh.getLocalString
                                                (getClass().getName() + ".failed1",
                                                "FAILED [AS-WEB local-charset-map] attribute locale  [ {0} ] must be of finite length.",
                                                new Object[] {locale}));
		}else{
			for(int index=0;index < locales.length;index++){
				if(locale.equalsIgnoreCase(locales[index].toString())){
					charMapLocFlag=true;
					break;
				}
                	}
			if(!charMapLocFlag) {
                                        oneWarning = true;
                                        addWarningDetails(result, compName);
                                        result.warning(smh.getLocalString
                                                (getClass().getName() + ".warning1",
                                                "WARNING [AS-WEB local-charset-map] attribute locale  [ {0} ] is not valid.",
                                                new Object[] {locale}));
                        }else{
                    			addGoodDetails(result, compName);
                    			result.passed(smh.getLocalString
				  		(getClass().getName() + ".passed1",
				  		"PASSED [AS-WEB locale-charset-map] attribute locale [ {0} ] properly defined.",
				  		new Object[] {locale}));
			}
                }
	   	//Test 2.2: check validity of each charset
                String charset = localeCharMaps[rep].getAttributeValue(LocaleCharsetMap.CHARSET);
 		if( charset == null || charset.length()==0) {
					oneFailed = true;
                			addErrorDetails(result, compName);
                                        result.failed(smh.getLocalString
                                                (getClass().getName() + ".failed3",
                                                "FAILED [AS-WEB local-charset-map] attribute charset [ {0} ] must be of standard format.",
                                                new Object[] {charset}));
		}else{
			try{
					Charset cs=Charset.forName(charset);
                    			addGoodDetails(result, compName);
                    			result.passed(smh.getLocalString
						(getClass().getName() + ".passed2",
					  	"PASSED [AS-WEB locale-charset-map] attributes charset [ {0} ] properly defined.",
				  	  	new Object[] {charset}));
			}catch(UnsupportedCharsetException ex){
                                        oneWarning = true;
                                        addWarningDetails(result, compName);
                                        result.warning(smh.getLocalString
                                                (getClass().getName() + ".warning2",
                                                "WARNING [AS-WEB local-charset-map] attribute charset [ {0} ] is not valid.",
                                                new Object[] {charset}));
			}
			catch(IllegalCharsetNameException ex){
					oneFailed = true;
                                        addErrorDetails(result, compName);
                                        result.failed(smh.getLocalString
                                                (getClass().getName() + ".failed3",
                                                "FAILED [AS-WEB local-charset-map] attribute charset [ {0} ] must be of standard format.",
                                                new Object[] {charset}));
			}
		}
            }//for


            //chk for parameter encoding pending, as NLSInfo object still dont support it.// to-do vkv#

        } else {
            notApp = true;
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                     (getClass().getName() + ".notApplicable",
                      "NOT APPLICABLE [AS-WEB sun-web-app] locale-charset-info element not defined in this web archive [ {0} ].",
                      new Object[] {descriptor.getName()}));
        }
        if (oneFailed) {
            result.setStatus(Result.FAILED);
        } else
	     if (oneWarning){
                        result.setStatus(Result.WARNING);
                        }
          else	
		if(notApp) {
            result.setStatus(Result.NOT_APPLICABLE);
        }else {
            result.setStatus(Result.PASSED);
            addGoodDetails(result, compName);
            result.passed
		    (smh.getLocalString
		     (getClass().getName() + ".passed3",
		      "PASSED [AS-WEB sun-web-app] locale-charset-info elements are valid within the web archive [ {0} ] .",
                            new Object[] {descriptor.getName()} ));
        }
       }catch(Exception ex){
            oneFailed = true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed2",
                    "FAILED [AS-WEB sun-web-app] could not create the local-charset-info object"));
       }
	return result;
    }
}

    
