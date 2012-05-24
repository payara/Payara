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

package com.sun.enterprise.tools.verifier.tests.ejb.session;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;

/**
 * Session beans home interface no finder methods name test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * signature: 
 * 
 * Since all session objects hide their identity, there is no need to provide 
 * a finder for them. The home interface for a session object must not define 
 * any finder methods.
 */
public class HomeInterfaceNoFinderMethodNames extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;

    /**
     * Session beans home interface no finder methods name test.
     * 
     * The following are the requirements for the enterprise Bean's home interface 
     * signature: 
     * 
     * Since all session objects hide their identity, there is no need to provide 
     * a finder for them. The home interface for a session object must not define 
     * any finder methods.
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();

        if (descriptor instanceof EjbSessionDescriptor) {
            // RULE: Session home interface are not allowed to have any
            //       finder methods
            if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName()))
                commonToBothInterfaces(descriptor.getHomeClassName(),(EjbSessionDescriptor)descriptor);
            if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName()))
                commonToBothInterfaces(descriptor.getLocalHomeClassName(),(EjbSessionDescriptor)descriptor);
        }
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "Valid: no finder methods were found.  Session bean's home interface" +
                    " is not allowed to have any finder methods."));
        }
        return result;
    }

    /**
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home interface of the Ejb.
     */

    private void commonToBothInterfaces(String home, EjbSessionDescriptor descriptor) {
        try {
            Class c = Class.forName(home,
                                false,
                                getVerifierContext().getClassLoader());
            for(Method methods : c.getDeclaredMethods()) {
                if(methods.getName().startsWith("find")) {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                            (getClass().getName() + ".debug1",
                                    "For Home Interface [ {0} ] Method [ {1} ]",
                                    new Object[] {c.getName(),methods.getName()}));
                    result.addErrorDetails(smh.getLocalString
                            (getClass().getName() + ".failed",
                                    "Improperly named method [ {0} ] was found. Session bean's home interface is not allowed to have any finder methods.",
                                    new Object[] {methods.getName()}));
                }
            }

        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                            "Error: Home interface [ {0} ] does not exist or is not loadable within bean [ {1} ]",
                            new Object[] {home, descriptor.getName()}));
        }
    }
}
