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
import java.util.jar.*;
import java.util.*;
import java.io.*;
import java.util.regex.Pattern;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/**
 * Welcome file element contains the file name to use as a default welcome file
 * within web application test.
 */
public class WelcomeFile extends WebTest implements WebCheck {
    
    /**
     * Welcome file element contains the file name to use as a default welcome file
     * within web application test.
     *
     * @param descriptor the Web deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if(!isApplicable(descriptor, result)) {
            return result;
        }
        
        // Check whether the syntax of welcome-file is correct or not.
        boolean syntaxOK = checkSyntax(descriptor, result);
        
        // check whether each welcome-file exists or not
        //boolean exists = checkExists(descriptor, result);
        boolean exists = true;
        
        // report WARNING if the syntax is wrong or none of welcome-files exist.
        if (!syntaxOK) {
            result.setStatus(Result.FAILED);
        } else if (!exists) {
            result.setStatus(Result.WARNING);
        } else {
            result.setStatus(Result.PASSED);
        }
        
        return result;
    }
    
    private boolean isApplicable(WebBundleDescriptor descriptor, Result result) {
        boolean applicable = true;
        if (!descriptor.getWelcomeFiles().hasMoreElements()) {
            ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                    "There are no welcome files within the web archive [ {0} ]",
                    new Object[] {descriptor.getName()}));
            applicable = false;
        }
        return applicable;
    }
    
    private boolean checkSyntax(WebBundleDescriptor descriptor, Result result) {
        boolean syntaxOK = true;
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        for (Enumeration e = descriptor.getWelcomeFiles() ; e.hasMoreElements() ;) {
            String welcomefile = (String) e.nextElement();
            if (welcomefile.startsWith("/") || welcomefile.endsWith("/")) {
                addErrorDetails(result, compName);
                result.addErrorDetails(smh.getLocalString(
                        getClass().getName() + ".failed1",
                        "Error : Servlet 2.3 Spec 9.9 Welcome file URL [ {0} ] must be partial URLs with no trailing or leading /",
                        new Object[] {welcomefile, descriptor.getName()}));
                syntaxOK = false;
            }
        }
        return syntaxOK;
    }
    
    private boolean checkExists(WebBundleDescriptor descriptor, Result result) {
        findDynamicResourceURIs(descriptor);
        boolean exists = false;
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        for (Enumeration e = descriptor.getWelcomeFiles() ; e.hasMoreElements() ;) {
            String welcomeFile = (String) e.nextElement();
            if(fileExists(descriptor, welcomeFile) || urlMatches(welcomeFile)) {
                exists = true;
                addGoodDetails(result, compName);
                result.addGoodDetails(smh.getLocalString
                        (getClass().getName() + ".passed",
                        "Welcome file [ {0} ] contains the file name to use as a default welcome file within web application [ {1} ]",
                        new Object[] {welcomeFile, descriptor.getName()}));
            } else {
                addWarningDetails(result, compName);
                result.addWarningDetails(smh.getLocalString
                        (getClass().getName() + ".failed",
                        "Error: Welcome file [ {0} ] is not found within [ {1} ] or does not contain the file name to use as a default welcome file within web application [ {2} ]",
                        new Object[] {welcomeFile, descriptor.getModuleDescriptor().getArchiveUri(), descriptor.getName()}));
            }
        }
        return exists;
    }
    
    private boolean fileExists(WebBundleDescriptor descriptor, String fileName) {
        File webCompRoot = new File(getAbstractArchiveUri(descriptor));
        File welcomeFile = new File(webCompRoot, fileName);
        return welcomeFile.exists();
    }
    
    private Set dynamicResourceUrlPatterns = new HashSet();
    
    private void findDynamicResourceURIs(WebBundleDescriptor descriptor) {
        Set webComponentDescriptors = descriptor.getWebComponentDescriptors();
        for(Iterator iter = webComponentDescriptors.iterator(); iter.hasNext(); ) {
            WebComponentDescriptor webComponentDescriptor = (WebComponentDescriptor) iter.next();
            dynamicResourceUrlPatterns.addAll(webComponentDescriptor.getUrlPatternsSet());
        }
        // Remove the leading and trailing '/' character from each dynamicResourceUrlPatters
        Set newUrlPatterns = new HashSet();
        for(Iterator iter = dynamicResourceUrlPatterns.iterator(); iter.hasNext() ;) {
            String urlPattern = (String) iter.next();
            if (urlPattern.startsWith("/")) {
                urlPattern = urlPattern.substring(1);
            }
            if (urlPattern.endsWith("/")) {
                urlPattern = urlPattern.substring(0, urlPattern.length() - 1);
            }
            newUrlPatterns.add(urlPattern);
        }
        dynamicResourceUrlPatterns = newUrlPatterns;
    }
    
    private boolean urlMatches(String url) {
        for(Iterator iter = dynamicResourceUrlPatterns.iterator(); iter.hasNext() ;) {
            boolean matches = Pattern.matches((String)iter.next(), url);
            if (matches) {
                return true;
            }
        }
        return false;
    }
}
