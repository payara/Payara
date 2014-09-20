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

import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

public abstract class URLPatternUnique extends WebTest implements WebCheck { 
    /** 
     * the url-pattern should be unique. Refer to bug#4903615 
	 * This test serves as a base class for three classes.
	 * It has a pure virtual function which has to be implemented in concrete subclasses.
     * Note: This right now reports WARNING, as it is not clear from spec
	 * if it should report a failure.
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {
        boolean na=true, warning=false;
        Result result = getInitializedResult();
        ComponentNameConstructor compName =
                getVerifierContext().getComponentNameConstructor();
        //this warning will be displayed depending on the final status.
        result.addWarningDetails(smh.getLocalString
                                                   ("tests.componentNameConstructor",
                                                        "For [ {0} ]",
                                                        new Object[] {compName.toString()}));

        //Assumes that DOL gives us the list of url-patterns including duplicates
        Set<String> urlPatterns=new HashSet<String>();
        for(Iterator iter=getUrlPatterns(descriptor).iterator();iter.hasNext();){
            na=false;
            String urlPattern=(String)iter.next();
            if(!urlPatterns.add(urlPattern)){
                    warning=true;
                    result.setStatus(Result.WARNING);
                    result.addWarningDetails(smh.getLocalString
                                                     (getClass().getName() + ".warning",
                                                      "url-pattern [ {0} ] already exists in web archive [ {1} ]",
                                                      new Object[] {urlPattern, descriptor.getName()}));
            }
        }

        if(na){
            result.setStatus(Result.NOT_APPLICABLE);
            result.addNaDetails(smh.getLocalString
                                               ("tests.componentNameConstructor",
                                                    "For [ {0} ]",
                                                    new Object[] {compName.toString()}));	    
            result.addNaDetails(smh.getLocalString
                                                     (getClass().getName() + ".notApplicable",
                                                    "There is no url-pattern element within the web archive [ {0} ]",
                                                    new Object[] {descriptor.getName()}));
        }else if(!warning) {
            result.passed(smh.getLocalString
                                               ("tests.componentNameConstructor",
                                                    "For [ {0} ]",
                                                    new Object[] {compName.toString()}));	    
            result.addGoodDetails(smh.getLocalString
                                                     (getClass().getName() + ".passed",
                                                    "All the url-patterns are unique within the web archive [ {0} ]",
                                                    new Object[] {descriptor.getName()}));
        }
        return result;
    }
    protected abstract Collection getUrlPatterns(WebBundleDescriptor descriptor);
}
