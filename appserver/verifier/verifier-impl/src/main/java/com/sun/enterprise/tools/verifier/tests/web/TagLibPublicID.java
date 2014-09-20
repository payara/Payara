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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.TagLibDescriptor;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.deployment.WebBundleDescriptor;

/**
 *
 */
public class TagLibPublicID extends WebTest implements WebCheck {

    public Result check(WebBundleDescriptor descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        String acceptablePubidLiterals[] = {
            "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN" ,
            "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN" };

        String acceptableURLs[] = {"http://java.sun.com/j2ee/dtds/web-jsptaglibrary_1_1.dtd",
                                   "http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd"};
        VerifierTestContext context = getVerifierContext();
        TagLibDescriptor tlds[] = context.getTagLibDescriptors();

        addGoodDetails(result, compName);
        result.passed(smh.getLocalString
                (getClass().getName() + ".passed",
                        "Test passed successfully"));

        if (tlds != null && tlds.length !=0) {
            boolean oneFailed = false;
            // iterate over all the tag lib descriptors present in war file
            for (int i=0;i<tlds.length;i++) {
                String publicID = tlds[i].getPublicID();
                String systemID = tlds[i].getSystemID();
                if (publicID==null) continue;
                boolean match = false;
                for (int k=0;k<acceptablePubidLiterals.length;k++) {
                    if (publicID.compareTo(acceptablePubidLiterals[k])==0 && systemID.compareTo(acceptableURLs[k])==0) {
                        match=true;
                        addGoodDetails(result, compName);
                        result.passed
                                (smh.getLocalString
                                (getClass().getName() + ".passed1",
                                        "The deployment descriptor [ {0} ] has the proper PubidLiteral: [ {1} ] and sytemID: [ {2} ]",
                                        new Object[] {tlds[i].getUri(), acceptablePubidLiterals[k], acceptableURLs[k]}));
                        break;
                    }
                }

                if (!match) {
                    oneFailed=true;
                    addErrorDetails(result, compName);
                    result.addErrorDetails
                            (smh.getLocalString
                            (getClass().getName() + ".failed",
                                    "The deployment descriptor for [ {0} ] does not have an expected PubidLiteral or SystemID",
                                    new Object[] {tlds[i].getUri()}));

                }
            }
            if(oneFailed)
                result.setStatus(Result.FAILED);
            return result;

        }
        return result;
    }
}
