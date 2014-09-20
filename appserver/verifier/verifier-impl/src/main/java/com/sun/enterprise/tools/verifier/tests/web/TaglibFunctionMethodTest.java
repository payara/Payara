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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.TagLibDescriptor;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.TagLibTest;
import com.sun.enterprise.tools.verifier.web.FunctionDescriptor;

/**
 * The specified method, in function-signature element, must be a public static
 * method in the specified class, and must be specified using a fully-qualified
 * return type followed by the method name, followed by the fully-qualified
 * argument types in parenthesis, separated by commas.
 * 
 * @author Sudipto Ghosh
 */
public class TaglibFunctionMethodTest extends TagLibTest implements WebCheck {

    public Result check(WebBundleDescriptor descriptor) {
        ComponentNameConstructor compName =
                getVerifierContext().getComponentNameConstructor();
        Result result = getInitializedResult();
        VerifierTestContext context = getVerifierContext();
        TagLibDescriptor tlds[] = context.getTagLibDescriptors();
        FunctionDescriptor[] fnDesc = null;

        if (tlds == null) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                            "No tag lib files are specified"));
            return result;
        }

        for (TagLibDescriptor tld : tlds) {
            if (tld.getSpecVersion().compareTo("2.0") >= 0) {
                fnDesc = tld.getFunctionDescriptors();
                if (fnDesc != null)
                    for (FunctionDescriptor fd : fnDesc)
                        checkMethodExistence(result, fd, tld, compName);
            }
        }
        if (result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString(getClass()
                    .getName() +
                    ".passed", "All methods defined in the function-signature element" +
                    "of the tag lib descriptor are properly defined."));
        }
        return result;
    }

    private void checkMethodExistence(Result result, FunctionDescriptor fnDesc,
                                      TagLibDescriptor tld,
                                      ComponentNameConstructor compName) {
        ClassLoader cl = getVerifierContext().getClassLoader();
        String signature = fnDesc.getFunctionSignature();
        String className = fnDesc.getFunctionClass();
        String methodName = getName(signature);
        String retType = getRetType(signature);
        String[] par = getParameters(signature);
        Class [] param = getParamTypeClass(par, cl);
        try {
            Class c = Class.forName(className, false, cl);
            boolean passed = false;
            Method[] methods= c.getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) &&
                        parametersMatch(m, param) &&
                        Modifier.toString(m.getModifiers()).contains("static") &&
                        returnTypeMatch(m, retType)) {
                    passed = true;
                    break;
                }
            }
            if(!passed) {
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString(getClass().getName() +
                        ".failed",
                        "Error: The method [ {0} ] as defined in function-signature" +
                        "element of [ {1} ] does not exists or is not a" +
                        "public static method in class [ {2} ]",
                        new Object[]{methodName, tld.getUri(), className }));
            }
        } catch (ClassNotFoundException e) {
            //do nothing. If class is not found, JSP compiler will anyway report
        }
    }
}

