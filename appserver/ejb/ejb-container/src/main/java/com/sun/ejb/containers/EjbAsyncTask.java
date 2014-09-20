/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import com.sun.ejb.EjbInvocation;
import com.sun.enterprise.util.Utility;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.lang.reflect.InvocationTargetException;

import com.sun.enterprise.security.SecurityContext;

/**
 * @author Mahesh Kannan
 */
public class EjbAsyncTask<V>
        implements Callable<V> {

    private EjbInvocation inv;

    private EjbFutureTask ejbFutureTask;

    private SecurityContext callerSecurityContext;

    public void initialize(EjbInvocation inv) {
        this.inv = inv;
        this.ejbFutureTask = inv.getEjbFutureTask();

        // Capture calling thread's security context and set
        // it on dispatch thread.
        callerSecurityContext = SecurityContext.getCurrent();
    }

    public long getInvId() {
        return inv.getInvId();
    }

    FutureTask getFutureTask() {
        return ejbFutureTask;
    }

    EjbInvocation getEjbInvocation() {
        return inv;
    }

    public V call()
            throws Exception {
        V returnValue = null;
        BaseContainer container = (BaseContainer) inv.container;
        ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
        try {
            Utility.setContextClassLoader(container.getClassLoader());

            // Must be set before preinvoke so it happens before authorization.
            SecurityContext.setCurrent(callerSecurityContext);

            container.preInvoke(inv);

            returnValue = (V) container.intercept(inv);

            if (returnValue instanceof Future) {
                returnValue = (V) ((Future) returnValue).get();
            }

        } catch (InvocationTargetException ite) {
            inv.exception = ite.getCause();
            inv.exceptionFromBeanMethod = inv.exception;
        } catch (Throwable t) {
            inv.exception = t;
        } finally {
            try {
                container.postInvoke(inv, inv.getDoTxProcessingInPostInvoke());

                // Use the same exception handling logic here as is used in the
                // various invocation handlers.  This ensures that the same
                // exception that would be received in the synchronous case will
                // be set as the cause of the ExecutionException returned from
                // Future.get().
                
                if (inv.exception != null) {
                    if (inv.isLocal) {
                        InvocationHandlerUtil.throwLocalException(
                                inv.exception, inv.method.getExceptionTypes());
                    } else {
                        InvocationHandlerUtil.throwRemoteException(
                                inv.exception, inv.method.getExceptionTypes());
                    }
                }
            } catch (Throwable th) {
                ExecutionException ee = new ExecutionException(th);
                ejbFutureTask.setResultException(ee);
                throw ee;
            } finally {
                Utility.setContextClassLoader(prevCL);
            }
        }

        ejbFutureTask.setResultValue(returnValue);
        return returnValue;
    }
}
