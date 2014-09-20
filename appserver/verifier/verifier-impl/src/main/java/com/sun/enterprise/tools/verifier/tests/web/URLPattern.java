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

package com.sun.enterprise.tools.verifier.tests.web;

import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.descriptor.*;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.ServletFilterMapping;
import com.sun.enterprise.deployment.web.WebResourceCollection;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.web.deployment.descriptor.JspConfigDescriptorImpl;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;

/**
 * The content of the url-pattern element follows the rules specified in 
 * section 10 of the servlet spec.
 * This abstract class serves as the base of some concrete classes like 
 * URLPatternErrorCheck, URLPatternWarningCheck & URLPatternContainsCRLF.
 * This class implements the check method, but inside the check method it calls a pure virtual function
 * called checkUrlPatternAndSetResult. This pure virtual function is implemented in the two derived classes.
 */
public abstract class URLPattern extends WebTest implements WebCheck {
    //These variables are needed because Result object does not maintain state.
    protected boolean oneFailed=false, oneWarning=false;

    /**
     * The content of the url-pattern element follows the rules specified in 
     * section 10 of the servlet spec.
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        result.setStatus(Result.NOT_APPLICABLE);
        result.addNaDetails(smh.getLocalString
                ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
        result.addNaDetails(smh.getLocalString
                (getClass().getName() + ".notApplicable",
                        "There is no url-pattern element within the web archive [ {0} ]",
                        new Object[] {descriptor.getName()}));
        checkWebResourceCollections(descriptor, result, compName);
        checkServletMappings(descriptor, result, compName);
        checkServletFilterMappings(descriptor, result, compName);
        checkJspGroupProperties(descriptor, result, compName);

        if(oneFailed) result.setStatus(Result.FAILED);
        else if(oneWarning) result.setStatus(Result.WARNING);
        return result;
    }

    //Each derived test should implement this method
    protected abstract void checkUrlPatternAndSetResult(String urlPattern, Descriptor descriptor, Result result, ComponentNameConstructor compName);

    private void checkWebResourceCollections(WebBundleDescriptor descriptor, Result result, ComponentNameConstructor compName){
        Enumeration e=descriptor.getSecurityConstraints();
        while (e.hasMoreElements()) {
            SecurityConstraint securityConstraint = (SecurityConstraint) e.nextElement();
            for (WebResourceCollection webResourceCollection : securityConstraint.getWebResourceCollections()) {
                for (String s : webResourceCollection.getUrlPatterns()) {
                    checkUrlPatternAndSetResult(s, descriptor, result, compName);
                }
            }
        }
    }

    private void checkServletMappings(WebBundleDescriptor descriptor, Result result, ComponentNameConstructor compName){
        for(Iterator iter=descriptor.getWebComponentDescriptors().iterator();iter.hasNext();)
            for(Iterator iter2=((WebComponentDescriptor)iter.next()).getUrlPatternsSet().iterator(); iter2.hasNext();
                checkUrlPatternAndSetResult((String)iter2.next(), descriptor, result, compName));
    }

    private void checkServletFilterMappings(WebBundleDescriptor descriptor, Result result, ComponentNameConstructor compName){
        for(Iterator iter=descriptor.getServletFilterMappings().iterator();iter.hasNext();){
            ServletFilterMapping filterMapping=(ServletFilterMapping)iter.next();
            if(filterMapping.getUrlPatterns().size() > 0) {
                for(String url : filterMapping.getUrlPatterns())
                    checkUrlPatternAndSetResult(url, descriptor, result, compName);
            }
        }
    }

    //This method checks for url-patterns appearing in jsp-config element in an web-app.
    private void checkJspGroupProperties(WebBundleDescriptor descriptor, Result result, ComponentNameConstructor compName){
        JspConfigDescriptorImpl jspC=((WebBundleDescriptorImpl)descriptor).getJspConfigDescriptor();
        if (jspC==null) return;
        for (JspPropertyGroupDescriptor desc : jspC.getJspPropertyGroups()) {
            for (String urlPattern : desc.getUrlPatterns()) {
                checkUrlPatternAndSetResult(urlPattern, descriptor, result,
                    compName);
            }
        }
    }
}
