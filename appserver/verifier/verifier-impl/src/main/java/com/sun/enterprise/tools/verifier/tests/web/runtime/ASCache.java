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

//import com.sun.enterprise.tools.common.dd.webapp.*;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>


public class ASCache extends WebTest implements WebCheck{

    public Result check(WebBundleDescriptor descriptor) {
        
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
        boolean notApp = false;
        String maxEntries=null;
        String timeout=null;
        int intMaxEntries;
        long longTimeout;
        String enabled=null;
        
        try{
        Cache cache = ((SunWebAppImpl)descriptor.getSunDescriptor()).getCache();
    
        if (cache != null)
        {
            maxEntries = cache.getAttributeValue(Cache.MAX_ENTRIES);
            timeout = cache.getAttributeValue(Cache.TIMEOUT_IN_SECONDS);
            enabled = cache.getAttributeValue(Cache.ENABLED);
            if (!(enabled.equalsIgnoreCase("true")) && !(enabled.equalsIgnoreCase("false")))
            {

            addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                    (getClass().getName() + ".failed3",
                    "FAILED [AS-WEB cache] enabled  [ {0} ], attribute must be a proper boolean  value. ",
                    new Object[] {enabled}));
                    oneFailed = true;
            }
            else
            {
            addGoodDetails(result, compName);
                result.passed(smh.getLocalString
                    (getClass().getName() + ".passed3",
                        "PASSED [AS-WEB cache] enabled  [ {0} ] defined properly.",
                         new Object[] {enabled}));
            }     
              

            boolean validMaxEntriesValue=true;
            boolean validTimeOutValue=true;
            try
            {
                if(maxEntries !=null && maxEntries.length() != 0)     // check maxEntries.length() != 0 because is IMPLIED att
                {
                    intMaxEntries=Integer.parseInt(maxEntries);
                    if(intMaxEntries >0 && intMaxEntries < Integer.MAX_VALUE)
                        validMaxEntriesValue=true;
                    else
                        validMaxEntriesValue=false;
                }
            }
            catch(NumberFormatException exception)
            {
                validMaxEntriesValue=false;
                if (!oneFailed)
                    oneFailed = true;
            }
            if(validMaxEntriesValue)
            {
                addGoodDetails(result, compName);
                result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "PASSED [AS-WEB cache] max-entries [ {0} ] defined properly.",
                    new Object[] {maxEntries}));
            }else
            {
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                    (getClass().getName() + ".failed",
                    "FAILED [AS-WEB cache] max-entries [ {0} ], attribute must be a proper integer value. "+
                        "Its range should be 1 to MAX_INT.",
                        new Object[] {maxEntries}));
                     oneFailed = true;
            }
            try
            {
                if(timeout  != null && timeout.length() != 0 ) // check timeout.length() != 0 because is IMPLIED att
                {
                    longTimeout=Long.parseLong(timeout);
                    if(longTimeout >= -1 && longTimeout <= Long.MAX_VALUE)
                        validTimeOutValue=true;
                    else
                        validTimeOutValue=false;
                }                 

            }catch(NumberFormatException exception) 
            {
                validTimeOutValue=false;
                    oneFailed = true;
            }
            if(validTimeOutValue)
            {
                addGoodDetails(result, compName);
                result.passed(smh.getLocalString
                    (getClass().getName() + ".passed1",
                        "PASSED [AS-WEB cache] timeout-in-seconds  [ {0} ] defined properly.",
                         new Object[] {timeout}));
            }
            else
            {
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                    (getClass().getName() + ".failed1",
                    "FAILED [AS-WEB cache] timeout-in-seconds value [ {0} ], attribute must be a proper long value. " +
                    "Its range should be between -1 and MAX_LONG.",
                    new Object[] {timeout}));
                    oneFailed = true;
            }
            
        }else
        {
            notApp = true;
            addNaDetails(result, compName);
	    result.notApplicable(smh.getLocalString
                (getClass().getName() + ".notApplicable",
                    "NOT APPLICABLE [AS-WEB cache] Element not defined for the web application.",
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
            oneFailed=true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failed2",
                    "FAILED [AS-WEB cache] Could not create the cache object"));
        }
	return result;
    }
}
