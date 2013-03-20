/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers.interceptors;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Mahesh Kannan
 *         Date: Mar 10, 2008
 */
class CallbackChainImpl {

    protected CallbackInterceptor[] interceptors;
    protected int size;
    private Method method = null;

    CallbackChainImpl(CallbackInterceptor[] interceptors) {
        this.interceptors = interceptors;
        this.size = (interceptors == null) ? 0 : interceptors.length;

        // set invocation method if there is one on the bean class
        if (size > 0 && interceptors[size-1].isBeanCallback()) {
            method = interceptors[size-1].method;
        }
    }

    public Object invokeNext(int index, CallbackInvocationContext invContext)
            throws Throwable {

        invContext.method = method;
        Object result = null;
        if (index < size) {
            result = interceptors[index].intercept(invContext);
        } else {
            invContext.invokeSpecial();
        }

        return result;
    }

    public String toString() {
        StringBuilder bldr = new StringBuilder("CallbackInterceptorChainImpl");
        for (CallbackInterceptor inter : interceptors) {
            bldr.append("\n\t\t").append(inter);
        }

        return bldr.toString();
    }

    /**
     * Prepend an interceptor to an existing callback chain.
     * @param interceptor
     */
    public void prependInterceptor(CallbackInterceptor interceptor) {

        size++;

        CallbackInterceptor[] newArray = new CallbackInterceptor[size];
        newArray[0] = interceptor;
        for(int i = 1; i < size; i++) {
            newArray[i] = interceptors[i - 1];
        }

        interceptors = newArray;
    }
}
