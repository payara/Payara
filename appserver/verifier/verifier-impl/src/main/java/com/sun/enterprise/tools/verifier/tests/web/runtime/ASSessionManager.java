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

public class ASSessionManager extends WebTest implements WebCheck{


public Result check(WebBundleDescriptor descriptor) {


	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneFailed = false;
        boolean notApp = false;
        try{
        SessionConfig sessionConfig = ((SunWebAppImpl)descriptor.getSunDescriptor()).getSessionConfig();
        SessionManager sessionMgr=null;

        StoreProperties stroeProp=null;
        ManagerProperties mgrProps=null;

        WebProperty[] SPwebProps=null;
        WebProperty[] MPwebProps=null;
        if(sessionConfig !=null){
            sessionMgr = sessionConfig.getSessionManager();
        }
        if (sessionConfig!=null && sessionMgr !=null){
            mgrProps=sessionMgr.getManagerProperties();
            stroeProp =sessionMgr.getStoreProperties();

            if(stroeProp !=null )
            SPwebProps=stroeProp.getWebProperty();

            if(mgrProps !=null)
            MPwebProps=mgrProps.getWebProperty();

            if((SPwebProps ==null|| SPwebProps.length==0) && (MPwebProps ==null || MPwebProps.length==0)){
                notApp = true;
            } else {
                  if (SPwebProps !=null && SPwebProps.length>0)
                       if(ASWebProperty.checkWebProperties(SPwebProps,result ,descriptor, this )){
                           oneFailed=true;
                           addErrorDetails(result, compName);
                           result.failed(smh.getLocalString
					   (getClass().getName() + ".failed",
					    "FAILED [AS-WEB session-manager] store-properties - Atleast one name/value pair is not valid in [ {0} ].",
					    new Object[] {descriptor.getName()}));
                         }

                  if (MPwebProps !=null && MPwebProps.length>0)
                        if(ASWebProperty.checkWebProperties(MPwebProps,result ,descriptor, this )){
                            oneFailed=true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
					   (getClass().getName() + ".failed1",
					    "FAILED [AS-WEB session-manager] manager-properties - Atleast one name/value pair is not valid in [ {0} ].",
					    new Object[] {descriptor.getName()}));
                         }
            }


        } else {
             notApp = true;
        }
        if(notApp) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "NOT APPLICABLE [AS-WEB session-config] session-manager element not defined in the web archive [ {0} ].",
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
                    (getClass().getName() + ".passed",
                    "PASSED [AS-WEB session-config] session manager element(s) and their manager-properties and/or store-properties are valid within the web archive [ {0} ].",
                    new Object[] {descriptor.getName()} ));
        }
        }catch(Exception ex){
        oneFailed = true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed2",
                    "FAILED [AS-WEB session-config] could not create the session-config object"));
        
        }
        return result;
    }
}

