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

package com.sun.enterprise.tools.verifier.tests.webservices;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: JSR109_WS_46; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: SEI must extend the java.rmi.Remote interface.
 */

public class SEIExtendsRemoteCheck  extends WSTest implements WSCheck {

    /**
     * @param descriptor the  WebService deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        // get hold of the SEI Class
        String s = descriptor.getServiceEndpointInterface();
        if (s == null) {
            // internal error, should never happen
            result.failed(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                            "Error: Unexpected error occurred [ {0} ]",
                            new Object[] {"SEI Class Name Null"}));
            return result;

        }
        Class sei = null;
        try {
            sei = Class.forName(s, false, getVerifierContext().getClassLoader());
        } catch(ClassNotFoundException e) {
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                            "Error: Unexpected error occurred [ {0} ]",
                            new Object[] {e.toString()}));

            return result;
        }
        if (!(getVerifierContext().getSchemaVersion().compareTo("1.1") > 0)) {
            if(!java.rmi.Remote.class.isAssignableFrom(sei)) {
                result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                        "For [ {0} ]", new Object[] {compName.toString()}));
                result.failed(smh.getLocalString(getClass().getName() + ".failed",
                        "SEI [{0}] does not extend the java.rmi.Remote interface.",
                        new Object[] {s}));
            }
        } else if (java.rmi.Remote.class.isAssignableFrom(sei)) {
            result.addWarningDetails(smh.getLocalString ("tests.componentNameConstructor",
                    "For [ {0} ]", new Object[] {compName.toString()}));
            result.warning(smh.getLocalString(getClass().getName() + ".warning",
                    "SEI [{0}] is not required to extend the java.rmi.Remote interface.",
                    new Object[] {s}));
        }
        if(result.getStatus() != Result.FAILED
                && result.getStatus() != Result.WARNING) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString(getClass().getName() + ".passed",
                    "Service Enpoint is defined properly"));
        }
        return result;
    }
}

