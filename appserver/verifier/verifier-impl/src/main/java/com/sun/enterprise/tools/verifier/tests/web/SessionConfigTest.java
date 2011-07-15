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

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.XpathPrefixResolver;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;


/**
 *
 *
 */
public class SessionConfigTest extends WebTest implements WebCheck {


    /**
     * The session-config element defines the session parameters for this web application
     * The deployment descriptor instance file must not contain multiple elements of session-config.
     *
     * @param descriptor the Web deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */

    public Result check(WebBundleDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        //This test is not applicable for application based on Servlet Spec 2.3
        String prefix = XpathPrefixResolver.fakeXPrefix;
        String query = prefix + ":" + "web-app/" + prefix + ":" + "session-config";
        int count = getNonRuntimeCountNodeSet(query);
        
        if ( count == 0 || count == -1) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                    "Not Applicable: Servlet session-config element is not Specified."));
        } else if ( count  == 1 ) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed" ,
                    "The session-config element is specified correctly"));
        } else if ( count > 1 ) {
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failed",
                    "The deployment descriptor instance contains multiple elements of session-config element"));
        }
        return result;
    }
}
