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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.util.Iterator;

/**
 * The ejb-name must be unique amoung the names of the enterprise beans within
 * the same ejb-jar file.
 */
public class EjbNameUnique extends EjbTest implements EjbCheck {

    /**
     * The ejb-name must be unique amoung the names of the enterprise beans within
     * the same ejb-jar file.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        Result result = getInitializedResult();
        String ejbName = descriptor.getName();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        // initialize needed by ejb loop
        int found = 0;

        // The ejb-name must be unique amoung the names of the enterprise beans
        // within the same ejb-jar file.
        // i.e. need to loop through all ejb's within this jar and get their
        // respective ejbName's, then do a string compare and make sure their are
        // no duplicates.
        for (Iterator itr =descriptor.getEjbBundleDescriptor().getEjbs().iterator();
             itr.hasNext();) {
            EjbDescriptor ejbDescriptor = (EjbDescriptor) itr.next();
            if (ejbDescriptor.getName().equals(ejbName)) {
                found++;
                if (found > 1) {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                            (getClass().getName() + ".failed",
                            "Error: [ {0} ] has found [ {1} ] duplicate ejb name(s) within the same jar.",
                            new Object[] {ejbName, new Integer((found - 1))}));
                }
            }
        }
        
        if (result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "Valid: [ {0} ] was found once within jar, ejb-name is unique.",
                    new Object[] {ejbName}));
        }
        return result;
    }
}
