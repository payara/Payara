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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *  The Bean Provider should not declare the session bean fields in the session
 *  bean class as transient.
 *
 *  This is to allow the Container to swap out an instance's state through 
 *  techniques other than the Java Serialization protocol. For example, the 
 *  Container's Java Virtual Machine implementation may use a block of memory 
 *  to keep the instance's variables, and the Container swaps the whole memory 
 *  block to the disk instead of performing Java Serialization on the instance.
 */
public class BeanFieldsTransient extends EjbTest implements EjbCheck {


    /**
     *  The Bean Provider should not declare the session bean fields in the session
     *  bean class as transient.
     *
     *  This is to allow the Container to swap out an instance's state through 
     *  techniques other than the Java Serialization protocol. For example, the 
     *  Container's Java Virtual Machine implementation may use a block of memory 
     *  to keep the instance's variables, and the Container swaps the whole memory 
     *  block to the disk instead of performing Java Serialization on the instance.
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (descriptor instanceof EjbSessionDescriptor) {
            try {
                Class c = Class.forName(((EjbSessionDescriptor)descriptor).getEjbClassName(), false, getVerifierContext().getClassLoader());
                // fields should not be defined in the session bean class as transient.
                Field [] fields = c.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    int modifiers = fields[i].getModifiers();
                    if (!Modifier.isTransient(modifiers)) {
                        continue;
                    } else {
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString
                                (getClass().getName() + ".warning",
                                        "Warning: Field [ {0} ] defined within session bean class [ {1} ] is defined as transient.  Session bean fields should not be defined in the session bean class as transient.",
                                        new Object[] {fields[i].getName(),((EjbSessionDescriptor)descriptor).getEjbClassName()}));
                    }
                }
            } catch (ClassNotFoundException e) {
                Verifier.debug(e);
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedException",
                                "Error: [ {0} ] class not found.",
                                new Object[] {((EjbSessionDescriptor)descriptor).getEjbClassName()}));
            } catch (Throwable t) { 
                addWarningDetails(result, compName);
                result.warning(smh.getLocalString
                        (getClass().getName() + ".warningException",
                        "Warning: [ {0} ] class encountered [ {1} ]. " +
                        "Cannot access fields of class [ {2} ] which is external to [ {3} ].",
                         new Object[] {(descriptor).getEjbClassName(),t.toString(),
                         t.getMessage(),
                         descriptor.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri()}));
            }
            return result;
        }

        if(result.getStatus()!=Result.WARNING){
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                            "The session bean class has defined all fields " +
                    "as non-transient fields."));

        }
        return result;
    }
}
