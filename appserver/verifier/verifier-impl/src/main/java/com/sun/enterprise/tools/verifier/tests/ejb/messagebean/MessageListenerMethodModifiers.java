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

package com.sun.enterprise.tools.verifier.tests.ejb.messagebean;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.MethodUtils;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Message listener methods must not be declared as final or static.
 * 
 * @author Vikas Awasthi
 */
public class MessageListenerMethodModifiers extends MessageBeanTest {

    public Result check(EjbMessageBeanDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = 
                            getVerifierContext().getComponentNameConstructor();

        ClassLoader cl = getVerifierContext().getClassLoader();
        try {
            Class intfCls = Class.forName(descriptor.getMessageListenerType(), false, cl);
            Class ejbCls = Class.forName(descriptor.getEjbClassName(), false, cl);
            Method[] intfMethods = intfCls.getMethods();
            for (Method method : intfMethods) {
                for (Method ejbMethod : ejbCls.getMethods()) {
                    // if matching method is found then check the assertion
                    if (MethodUtils.methodEquals(ejbMethod, method)) {
                        if(Modifier.isFinal(ejbMethod.getModifiers()) ||
                                Modifier.isStatic(ejbMethod.getModifiers())) {
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                            "Wrong method [ {0} ]",
                                            new Object[]{ejbMethod}));
                        }
                        break;
                    }
                }// another test will report failure if listener method is not found
            }
        } catch (ClassNotFoundException e) {} // will be caught in other tests

        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Valid message listener method(s)."));
        }
        return result;
    }
}
