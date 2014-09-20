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

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.web.WebResourceCollection;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.web.deployment.descriptor.SecurityConstraintImpl;

import java.util.Enumeration;


/**
 * The web-resource-name element contains the name of this web resource
 * collection
 */
public class WebResourceName extends WebTest implements WebCheck
{


    /**
     * The web-resource-name element contains the name of this web resource
     * collection
     *
     * @param descriptor the Web deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor)
    {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (descriptor.getSecurityConstraints().hasMoreElements())
        {
            boolean oneFailed = false;
            int na = 0;
            int noSc = 0;
            boolean foundIt = false;
            // get the security constraint's in this .war
            for (Enumeration e = descriptor.getSecurityConstraints(); e.hasMoreElements();)
            {
                foundIt = false;
                noSc++;
                SecurityConstraintImpl securityConstraintImpl = (SecurityConstraintImpl) e.nextElement();
                if (!securityConstraintImpl.getWebResourceCollections().isEmpty())
                {
                    for (WebResourceCollection webResCollection: securityConstraintImpl.getWebResourceCollections())
                    {
                        String webRCName = webResCollection.getName();
                        // cannot be blank
                        if (webRCName.length() > 0)
                        {
                            foundIt = true;
                        }
                        else
                        {
                            foundIt = false;
                        }

                        if (foundIt)
                        {
                            result.addGoodDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                            "For [ {0} ]",
                                            new Object[]{compName.toString()}));
                            result.addGoodDetails(smh.getLocalString
                                    (getClass().getName() + ".passed",
                                            "web-resource-name [ {0} ] contains the name of this web resource collection within web application [ {1} ]",
                                            new Object[]{webRCName, descriptor.getName()}));
                        }
                        else
                        {
                            if (!oneFailed)
                            {
                                oneFailed = true;
                            }
                            result.addErrorDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                            "For [ {0} ]",
                                            new Object[]{compName.toString()}));
                            result.addErrorDetails(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                            "Error: web-resource-name [ {0} ] does not contain the name of this web resource collection within web application [ {1} ]",
                                            new Object[]{webRCName, descriptor.getName()}));
                        }
                    }
                }
                else
                {
                    result.addNaDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[]{compName.toString()}));
                    result.notApplicable(smh.getLocalString
                            (getClass().getName() + ".notApplicable2",
                                    "There are no web web resource collections in the web security constraint within [ {0} ]",
                                    new Object[]{descriptor.getName()}));
                    na++;
                }
            }
            if (oneFailed)
            {
                result.setStatus(Result.FAILED);
            }
            else if (na == noSc)
            {
                result.setStatus(Result.NOT_APPLICABLE);
            }
            else
            {
                result.setStatus(Result.PASSED);
            }
        }
        else
        {
            result.addNaDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[]{compName.toString()}));
            result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                            "There are no web-resource-name elements within the web archive [ {0} ]",
                            new Object[]{descriptor.getName()}));
        }

        return result;
    }
}
