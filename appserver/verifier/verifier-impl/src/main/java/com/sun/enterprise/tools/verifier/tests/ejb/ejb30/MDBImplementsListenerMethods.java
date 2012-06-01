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

package com.sun.enterprise.tools.verifier.tests.ejb.ejb30;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ejb.MethodUtils;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;

import java.lang.reflect.Method;

/**
 * The message driven bean class must implement the message listener interface 
 * or the methods of the message listener interface.
 * 
 * @author Vikas Awasthi
 */
public class MDBImplementsListenerMethods extends MessageBeanTest {
    
    public Result check(EjbMessageBeanDescriptor descriptor) {

        try {
            ClassLoader cl = getVerifierContext().getClassLoader();
            Class intfCls = Class.forName(descriptor.getMessageListenerType(), false, cl);
            Class ejbCls = Class.forName(descriptor.getEjbClassName(), false, cl);
            
            if(!intfCls.isAssignableFrom(ejbCls)) {
                Method[] methods = intfCls.getMethods();
                for (Method method : methods) {
                    boolean foundOne = false;
                    for (Method ejbMethod : ejbCls.getMethods()) {
                        if(MethodUtils.methodEquals(ejbMethod, method)) {
                            foundOne = true;
                            break;
                        }
                    }
                    if(!foundOne) {
                        addErrorDetails(result, compName);
                        result.failed(
                                smh.getLocalString(getClass().getName()+".failed",
                                "Message bean [ {0} ] neither implements listener " +
                                "interface [ {1} ] nor implements listener " +
                                "interface method [ {2} ]",
                                new Object[] {ejbCls.getSimpleName(), intfCls.getName(), method}));
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            //ignore as this error will be caught by other tests
            logger.fine(descriptor.getEjbClassName() + " Not found");
        }
        
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(
                    smh.getLocalString(getClass().getName()+".passed",
                            "Valid Message bean [ {0} ]",
                            new Object[] {descriptor.getEjbClassName()}));
        }

        return result;
    }
}
