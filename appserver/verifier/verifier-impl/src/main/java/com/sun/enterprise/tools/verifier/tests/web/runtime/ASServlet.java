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


import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import org.glassfish.web.deployment.runtime.*;

/*  servlet
 *     servlet-name
 *     principal-name ?
 *     webservice-endpoint *
 */   

public class ASServlet extends WebTest implements WebCheck {

    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        //String[] servlets=null;//######
        String servletName;
        String prinName;
	boolean oneFailed = false;

        try{
            Float runtimeSpecVersion = getRuntimeSpecVersion();
            Servlet[] servlets = ((SunWebAppImpl)descriptor.getSunDescriptor()).getServlet();
            if (servlets !=null && servlets.length > 0){
	    for (int rep=0; rep<servlets.length; rep++ ){
                servletName=servlets[rep].getServletName();//######
                // <addition> srini@sun.com Bug : 4699658
                //prinName=servlets[rep].getPrincipalName();//######
               // prinName=servlets[rep].getPrincipalName().trim();//######
                // </addition> Bug : 4699658
                
                      if(validServletName(servletName,descriptor)){

                      addGoodDetails(result, compName);
                      result.passed(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "PASSED [AS-WEB servlet] servlet-name [ {0} ] properly defined in the war file.",
					   new Object[] {servletName}));

                      }else{

                	addErrorDetails(result, compName);
		        result.failed(smh.getLocalString
					   (getClass().getName() + ".failed",
					    "FAILED [AS-WEB servlet] servlet-name [ {0} ] is not a valid, either empty or not defined in web.xml.",
					    new Object[] {servletName}));
		        oneFailed = true;

                      }
                      prinName=servlets[rep].getPrincipalName();
                      if(prinName != null && ! "".equals(prinName)){
                          addGoodDetails(result, compName);
                          result.passed(smh.getLocalString
			      (getClass().getName() + ".passed1",
			       "PASSED [AS-WEB servlet] principal-name [ {0} ] properly defined in the war file.",
			       new Object[] {prinName}));
                      }else{
                          if (runtimeSpecVersion.compareTo(new Float("2.4")) <0 ){
                              result.failed(smh.getLocalString
                                  (getClass().getName() + ".failed1",
                                  "FAILED [AS-WEB servlet ] principal-name [ {0} ] cannot be an empty string.",
                                  new Object[] {prinName}));
                              oneFailed = true;
                          }else{
                              addNaDetails(result, compName);
                              result.notApplicable(smh.getLocalString
                                  (getClass().getName() + ".notApplicable1",
                                  "NOT APPLICABLE [AS-WEB servlet] principal-name not defined",
                                  new Object[] {descriptor.getName()}));
                          }

                      }

	    }
    	}else{
            addNaDetails(result, compName);
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "NOT APPLICABLE [AS-WEB sun-web-app] servlet element(s) not defined in the web archive [ {0} ].",
				  new Object[] {descriptor.getName()}));
	    return result;
	}

	if (oneFailed)
	    {
		result.setStatus(Result.FAILED);
	    } else {
        	addGoodDetails(result, compName);
		result.passed
		    (smh.getLocalString
		     (getClass().getName() + ".passed2",
		      "PASSED [AS-WEB sun-web-app] servlet element(s) are valid within the web archive [ {0} ] .",
                            new Object[] {descriptor.getName()} ));
	    }
       }catch(Exception ex){
            oneFailed = true;
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed2",
                    "FAILED [AS-WEB sun-web-app] could not create the servlet object"));
       }
	return result;
    }

    boolean validServletName(String servletName, WebBundleDescriptor descriptor){
          boolean valid=false;
          if (servletName != null && servletName.length() != 0) {
              Set servlets = descriptor.getServletDescriptors();
                    Iterator itr = servlets.iterator();
                    // test the servlets in this .war
                    while (itr.hasNext()) {
                        //ServletDescriptor servlet = (ServletDescriptor) itr.next();
                        WebComponentDescriptor servlet = (WebComponentDescriptor) itr.next();
                        String thisServletName = servlet.getCanonicalName();
			if (servletName.equals(thisServletName)) {
                            valid = true;
                            break;
                        }
                    }

          }
          return valid;
    }
}
