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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * Servlet Interface test.
 * Servlets must implement the javax.servlet.Servlet interface 
 * either directly or indirectly through GenericServlet or HttpServlet
 */
public class ServletInterface extends WebTest implements WebCheck { 

    final String servletClassPath = "WEB-INF/classes";
      
    /**
     * Servlet Interface test.
     * Servlets must implement the javax.servlet.Servlet interface 
     * either directly or indirectly through GenericServlet or HttpServlet
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (!descriptor.getServletDescriptors().isEmpty()) {
	    boolean oneFailed = false;
            boolean notPassOrFail = true;       
	    // get the servlets in this .war
	    Set servlets = descriptor.getServletDescriptors();
	    Iterator itr = servlets.iterator();
                     
	    result = loadWarFile(descriptor);

	    // test the servlets in this .war
            
            while (itr.hasNext()) {
		WebComponentDescriptor servlet = (WebComponentDescriptor)itr.next();
		String servletClassName = servlet.getWebComponentImplementation();
		Class c = loadClass(result, servletClassName);

                // if the class could not be loaded we dont want to fail
                // , it will be caught by the ServletClass test anyway
                if (c == null) {
                   continue;
                }
                if (isJAXRPCEndpoint(servlet)) {
	            result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	            result.addGoodDetails(smh.getLocalString
				 (getClass().getName() + ".notApplicable1",
				  "Not Applicable since, Servlet [ {0} ] is a JAXRPC Endpoint.",
				  new Object[] {servletClassName}));
                    notPassOrFail = false;
                }
		else if (isImplementorOf(c, "javax.servlet.Servlet")) {
		    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "Servlet class [ {0} ] directly or indirectly implements javax.servlet.Servlet",
					   new Object[] {servletClassName}));	    
                    notPassOrFail = false;
		} else {
		    oneFailed = true;
		    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addErrorDetails(smh.getLocalString
					   (getClass().getName() + ".failed",
					    "Error: Servlet class [ {0} ] does not directly or indirectly implement javax.servlet.Servlet",
					    new Object[] {servletClassName}));
                    notPassOrFail = false;
		}                       
	    }
            // this means classloader returned null for all servlets
            if (notPassOrFail) {
               result.addWarningDetails(smh.getLocalString
                                       ("tests.componentNameConstructor",
                                        "For [ {0} ]", new Object[] {compName.toString()}));
               result.warning(smh.getLocalString
                              (getClass().getName() + ".warning",
                               "Some servlet classes could not be loaded."));
            }
	    else if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no servlet components within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}
	return result;
    }

 private boolean isJAXRPCEndpoint(WebComponentDescriptor servlet) {
  
     String servletClassName = servlet.getWebComponentImplementation();

     if (servletClassName.equals(smh.getLocalString("JAXRPCServlet","com.sun.xml.rpc.server.http.JAXRPCServlet"))) {
        // This is a standard JAXRPC servlet
        return true;
     }

     WebBundleDescriptor descriptor = servlet.getWebBundleDescriptor();
     if (descriptor.hasWebServices()) {
        WebServicesDescriptor wsdesc = descriptor.getWebServices();
        if (wsdesc.hasEndpointsImplementedBy(servlet)) {
           return true;
        }
     }
    return false;
 }

}
