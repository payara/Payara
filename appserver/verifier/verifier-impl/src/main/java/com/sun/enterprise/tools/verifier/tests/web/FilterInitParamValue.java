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


import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;

import java.util.Enumeration;
import java.util.Vector;



/**
 *
 *  @author Jerome Dochez
 */
public class FilterInitParamValue extends WebTest implements WebCheck {

    /**
     * Param Value exists test.
     *
     * @param descriptor the Web deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneWarning = false, onePassed = false;

        Enumeration filterEnum = descriptor.getServletFilterDescriptors().elements();
        if (filterEnum.hasMoreElements()) {
            // get the filters in this .war
            while (filterEnum.hasMoreElements()) {
                ServletFilterDescriptor filter = (ServletFilterDescriptor) filterEnum.nextElement();
                Vector epVector = filter.getInitializationParameters();

                if (epVector.size() != 0) {
                    for ( int i = 0; i < epVector.size(); i++) {
                        EnvironmentProperty ep = (EnvironmentProperty)epVector.elementAt(i);
                        String epValue = ep.getValue();
                        if (epValue.length() != 0) {
                            onePassed=true;
                            addGoodDetails(result, compName);
                            result.addGoodDetails(smh.getLocalString
                                              ("com.sun.enterprise.tools.verifier.tests.web.FilterInitParamValue" + ".passed",
                                               "Param value exists for the filter [ {0} ].",
                                               new Object[] {filter.getName()}));
                        } else {
                            oneWarning = true;
                            addWarningDetails(result, compName);
                            result.addWarningDetails(smh.getLocalString
                                    ("com.sun.enterprise.tools.verifier.tests.web.FilterInitParamValue" + ".warning",
                                            "WARNING: Param value entry for the filter [ {0} ] should be of finite length.",
                                            new Object[] {filter.getName()}));
                        }
                    }
                } else {
                    addNaDetails(result, compName);
                    result.notApplicable(smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.tests.web.FilterInitParamValue" + ".notApplicable",
                                    "There are no initialization parameters for the filter [ {0} ] within the web archive [ {1} ]",
                                    new Object[] {filter.getName(), descriptor.getName()}));

                }
            }
            if (oneWarning) {
                result.setStatus(Result.WARNING);
            } else if (onePassed){
                result.setStatus(Result.PASSED);
            }
        } else {
            result.addNaDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.web.FilterInitParamValue" + ".notApplicable1",
                            "There are no filters defined within the web archive [ {0} ]",
                            new Object[] {descriptor.getName()}));
        }
        return result;
    }
}
