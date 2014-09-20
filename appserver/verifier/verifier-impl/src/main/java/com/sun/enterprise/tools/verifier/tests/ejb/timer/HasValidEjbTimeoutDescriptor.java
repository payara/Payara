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

package com.sun.enterprise.tools.verifier.tests.ejb.timer;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.SpecVersionMapper;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;


/**
 * Check that the transaction attributes for the ejbTimeout method are one
 * of the following -
 * RequiresNew or NotSupported
 *
 * @version 
 * @author Anisha Malhotra
 */
public class HasValidEjbTimeoutDescriptor extends EjbTest {
    Result result = null;
    ComponentNameConstructor compName = null;
    /**
     * Run a verifier test to check the transaction attributes of the
     * ejbTimeout method. The allowed attributes are -
     * RequiresNew or NotSupported.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();

        if(descriptor.isTimedObject()) {

            if (descriptor.getTransactionType().equals
                    (EjbDescriptor.CONTAINER_TRANSACTION_TYPE)) {
                MethodDescriptor methodDesc = descriptor.getEjbTimeoutMethod();
                ContainerTransaction txAttr =
                        descriptor.getContainerTransactionFor(methodDesc);
                String version = getVerifierContext().getJavaEEVersion();
                if(txAttr != null) {
                    String ta = txAttr.getTransactionAttribute();
                    if ((version.compareTo(SpecVersionMapper.JavaEEVersion_5) >= 0) &&
                            !(ContainerTransaction.REQUIRES_NEW.equals(ta)
                            || ContainerTransaction.NOT_SUPPORTED.equals(ta)
                            || ContainerTransaction.REQUIRED.equals(ta))) {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName()+".failed1",
                                "Error : Bean [ {0} ] Transaction attribute for timeout method" +
                                "must be Required, RequiresNew or NotSupported",
                                new Object[] {descriptor.getName()}));
                    } else if ((version.compareTo(SpecVersionMapper.JavaEEVersion_5) < 0) &&
                            !(ContainerTransaction.REQUIRES_NEW.equals(ta)
                            || ContainerTransaction.NOT_SUPPORTED.equals(ta))) {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName()+".failed2",
                                "Error : Bean [ {0} ] Transaction attribute for ejbTimeout " +
                                "must be RequiresNew or NotSupported",
                                new Object[] {descriptor.getName()}));

                    }
                } else if(version.compareTo(SpecVersionMapper.JavaEEVersion_5)<0) {
                    // Transaction attribute for ejbTimeout not specified in the DD
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                            (getClass().getName()+".failed3",
                            "Transaction attribute for Timeout is not specified for [ {0} ]",
                            new Object[] {descriptor.getName()}));
                }
            }
        }

        if (result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName()+".passed",
                    "Transaction attributes are properly specified"));

        }
        return result;
    }
}
