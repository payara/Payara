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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.util.Enumeration;

/**
 * Optionally implemented SessionSynchronization interface transaction 
 * demarcation test.
 * If an enterprise bean implements the javax.ejb.SessionSynchronization
 * interface, the Application Assembler can specify only the following values
 * for the transaction attributes of the bean's methods:
 *   Required
 *   RequiresNew
 *   Mandatory
 */
public class TransactionDemarcationSessionSynchronizationInterface extends EjbTest implements EjbCheck {


    /**
     * Optionally implemented SessionSynchronization interface transaction 
     * demarcation test.
     * If an enterprise bean implements the javax.ejb.SessionSynchronization
     * interface, the Application Assembler can specify only the following values
     * for the transaction attributes of the bean's methods:
     *   Required
     *   RequiresNew
     *   Mandatory
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFound = false;

        if (descriptor instanceof EjbSessionDescriptor) {
            try {
                VerifierTestContext context = getVerifierContext();
                ClassLoader jcl = context.getClassLoader();
                Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
                // walk up the class tree
                do {
                    Class[] interfaces = c.getInterfaces();

                    for (int i = 0; i < interfaces.length; i++) {
                        if (interfaces[i].getName().equals("javax.ejb.SessionSynchronization")) {
                            oneFound = true;
                            break;
                        }
                    }
                } while ((c=c.getSuperclass()) != null);

            } catch (ClassNotFoundException e) {
                Verifier.debug(e);
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedException1",
                                "Error: [ {0} ] class not found.",
                                new Object[] {descriptor.getEjbClassName()}));
                return result;
            }

            // If an enterprise bean implements the javax.ejb.SessionSynchronization
            // interface, the Application Assembler can specify only the following
            // values for the transaction attributes of the bean's methods:
            //   Required, RequiresNew, Mandatory
            if (oneFound) {
                String transactionAttribute = "";
                ContainerTransaction containerTransaction = null;
                boolean oneFailed = false;
                if (!descriptor.getMethodContainerTransactions().isEmpty()) {
                    for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
                        MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();
                        containerTransaction =
                                (ContainerTransaction) descriptor.getMethodContainerTransactions().get(methodDescriptor);

                        if (!(containerTransaction != null && properAttribDefined(containerTransaction))) {
                            transactionAttribute  =
                                    containerTransaction.getTransactionAttribute();
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                            "Error: TransactionAttribute [ {0} ] for method [ {1} ] is not valid.",
                                            new Object[] {transactionAttribute, methodDescriptor.getName()}));

                        }
                    }
                }
            }
        }

        if(result.getStatus()!=Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                            "TransactionAttributes are defined properly for the bean"));
        }
        return result;
    }

    private boolean properAttribDefined(ContainerTransaction containerTransaction) {
        String transactionAttribute  = containerTransaction.getTransactionAttribute();
        return (ContainerTransaction.REQUIRED.equals(transactionAttribute)
                || ContainerTransaction.REQUIRES_NEW.equals(transactionAttribute)
                || ContainerTransaction.MANDATORY.equals(transactionAttribute));

    }
}
