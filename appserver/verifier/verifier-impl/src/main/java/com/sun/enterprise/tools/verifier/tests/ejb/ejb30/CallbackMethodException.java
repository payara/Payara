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

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Lifecycle callback interceptor methods must not throw application exceptions. 
 * 
 * Any exception other than derived from java.lang.RuntimeException or 
 * java.rmi.RemoteException is an application exception.
 * 
 * @author Vikas Awasthi
 */
public class CallbackMethodException extends EjbTest {

    public Result check(EjbDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        ClassLoader cl = getVerifierContext().getClassLoader();

        Set<LifecycleCallbackDescriptor> callbackDescs = 
                                        new HashSet<LifecycleCallbackDescriptor>();
        
        for (EjbInterceptor interceptor : descriptor.getInterceptorClasses()) {
            callbackDescs.addAll(interceptor.getPostConstructDescriptors());
            callbackDescs.addAll(interceptor.getPreDestroyDescriptors());
            callbackDescs.addAll(interceptor.getCallbackDescriptors(
                        LifecycleCallbackDescriptor.CallbackType.PRE_PASSIVATE));
            callbackDescs.addAll(interceptor.getCallbackDescriptors(
                        LifecycleCallbackDescriptor.CallbackType.POST_ACTIVATE));
        }

        if(descriptor.hasPostConstructMethod())
            callbackDescs.addAll(descriptor.getPostConstructDescriptors());
        if(descriptor.hasPreDestroyMethod())
            callbackDescs.addAll(descriptor.getPreDestroyDescriptors());
        
        // session descriptor has two extra interceptor methods.
        if(descriptor instanceof EjbSessionDescriptor) {
            EjbSessionDescriptor ejbSessionDescriptor = ((EjbSessionDescriptor)descriptor);
            if(ejbSessionDescriptor.hasPostActivateMethod())
                callbackDescs.addAll(ejbSessionDescriptor.getPostActivateDescriptors());
            if(ejbSessionDescriptor.hasPrePassivateMethod())
                callbackDescs.addAll(ejbSessionDescriptor.getPrePassivateDescriptors());
        }

        for (LifecycleCallbackDescriptor callbackDesc : callbackDescs) {
            try {
                Method method = callbackDesc.getLifecycleCallbackMethodObject(cl);
                Class[] excepClasses = method.getExceptionTypes();
                for (Class exception : excepClasses) {
                    if(!(RuntimeException.class.isAssignableFrom(exception) ||
                            java.rmi.RemoteException.class.isAssignableFrom(exception))) {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                        (getClass().getName() + ".failed",
                                        "Method [ {0} ] throws an application exception.",
                                        new Object[] {method}));
                    }
                }
            } catch (Exception e) {}// will be caught in other tests
        }
        
        if(result.getStatus()!=Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Valid Callback methods."));
        }
        
        return result;
    }
}
