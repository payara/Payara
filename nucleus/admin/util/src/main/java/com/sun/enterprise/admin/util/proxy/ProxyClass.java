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

package com.sun.enterprise.admin.util.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A proxy class
 */
public class ProxyClass implements InvocationHandler {

    private static InheritableThreadLocal
            callStackHolder = new InheritableThreadLocal() {
        protected synchronized Object initialValue() {
            return new CallStack();
        }
    };

    private static Logger _logger = getLogger();

    private Object delegate;
    private Interceptor interceptor;

    /** Creates a new instance of Proxy */
    public ProxyClass(Object handler, Interceptor interceptor) {
        delegate = handler;
        this.interceptor = interceptor;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Call call = new Call(method, args);
        CallStack callStack = (CallStack)callStackHolder.get();
        callStack.beginCall(call);
        try {
            interceptor.preInvoke(callStack);
        } catch (Throwable t) {
            _logger.log(Level.FINE, "Preinvoke failed for MBeanServer interceptor [{0}].",
                    t.getMessage());
            _logger.log(Level.FINEST,
                    "Preinvoke exception for MBeanServer interceptor.", t);
        }
        Object result = null;
        boolean success = true;
        Throwable failReason = null;
        try {
            result = method.invoke(delegate, args);
        } catch (InvocationTargetException ite) {
            success = false;
            failReason = ite.getTargetException();
            throw failReason;
        } catch (Throwable t) {
            success = false;
            failReason = t;
            throw failReason;
        } finally {
            if (!success) {
                call.setState(CallState.FAILED);
                call.setFailureReason(failReason);
            }
            call.setResult(result);
            
            if(!(call.getState().isFailed()))
                call.setState(CallState.SUCCESS);
            
            try {
                interceptor.postInvoke(callStack);
            } catch (Throwable t) {
                _logger.log(Level.FINE, "Postinvoke failed for MBeanServer interceptor [{0}].",
                        t.getMessage());
                _logger.log(Level.FINEST,
                        "Postinvoke exception for MBeanServer interceptor.", t);
            }
            callStack.endCall();
        }
        return result;
    }

    private static Logger getLogger() {
        String loggerName = System.getProperty("com.sun.aas.admin.logger.name");
        if (loggerName == null) {
            loggerName = "global";
        }
        return Logger.getLogger(loggerName);
    }
}
