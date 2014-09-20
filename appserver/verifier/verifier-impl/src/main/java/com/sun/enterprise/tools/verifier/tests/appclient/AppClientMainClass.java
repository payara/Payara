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

package com.sun.enterprise.tools.verifier.tests.appclient;

import java.lang.reflect.Modifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;

/**
 * Application clients start execution at the main method of the class specified
 * in the Main-Class attribute in the manifest file of the application client’s
 * JAR file. It must be specified in the MANIFEST file.
 * @author Sudipto Ghosh
 */
public class AppClientMainClass extends AppClientTest implements AppClientCheck  {

    public Result check(ApplicationClientDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        String mainClass = descriptor.getMainClassName();
        if (mainClass != null && mainClass.length() > 0) {
            try { 
                Class c = Class.forName(mainClass, false, getVerifierContext().getClassLoader());
                if(!Modifier.isPublic(c.getModifiers())) {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString(getClass().getName() + ".failed2",
                            "ERROR: Appclient main-class [ {0} ] as specified in the Manifest file is not public.",
                            new Object[] {mainClass}));
                }
            } catch (ClassNotFoundException cnfe) {
                if(debug)
                    cnfe.printStackTrace();
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString(getClass().getName() + ".failed1",
                        "ERROR: Appclient main-class [ {0} ] as specified in the" +
                        " Manifest file is not loadable.",
                        new Object[] {mainClass}));
            }
        } else {
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failed",
                            "Appclient main-class is not found. Please check the " +
                    "main-class entry of your appclient manifest file."));
        }
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString(getClass().getName() + ".passed",
                    "main-class entry is defined properly."));
        }
        return result;
    }
}
