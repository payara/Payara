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

package com.sun.ejb.containers;

import com.sun.ejb.EjbInvocation;

import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;


/**
 * @author Mahesh Kannan
 */
public class EjbFutureTask<V>
    extends FutureTask<V> {

    private EjbAsyncTask ejbAsyncTask;

    // Used to remember if cancel() was called already
    private boolean cancelCalled = false;
        
    // State which could be set from both the caller's thread and
    // the thread on which the task is executing.
    private volatile boolean complete = false;
    private volatile V resultValue;
    private volatile Throwable resultException;


    public EjbFutureTask(EjbAsyncTask<V> callable, EjbAsyncInvocationManager mgr) {
        super(callable);
        this.ejbAsyncTask = callable;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {

        if( !cancelCalled ) {

            cancelCalled = true;

            // mayInterruptIfRunning only determines whether the bean method
            // has visibility to the fact that the caller called Future.cancel().
            if( mayInterruptIfRunning ) {
                EjbInvocation inv = ejbAsyncTask.getEjbInvocation();
                inv.setWasCancelCalled(true);
            }
        }

        // For now we don't even try checking to see if the task has started running.
        // Just return false so the caller knows the task could not be cancelled.      
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {

        // If get() has already been called, produce the same behavior
        // as initial call, except if get(timeout, unit) resulted in a
        // TimeoutException

        if( !complete ) {
            try {
                super.get();
                // result value is set directly by AsyncTask
            } catch(ExecutionException ee) {
                // already set directly by AsyncTask
            } catch(InterruptedException ie) {
                setResultException(ie);
            } catch(RuntimeException re) {
                setResultException(re);
            }
        }

        // We really shouldn't get CancellationException or
        // InterruptedException, but throw whatever kind we get.
        if( resultException != null ) {
           if( resultException instanceof ExecutionException ) {
               throw (ExecutionException) resultException;
           } else if( resultException instanceof InterruptedException) {
               throw (InterruptedException) resultException;
           } else {
               throw (RuntimeException) resultException;
           }
        }

        return resultValue;
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        // If get() has already been called, produce the same behavior
        // as initial call, except if get(timeout, unit) resulted in a
        // TimeoutException        

        if( !complete ) {
            try {
                super.get(timeout, unit);
            } catch(ExecutionException ee) {
                // already set directly by AsyncTask
            } catch(TimeoutException t) {
                // If it's a TimeoutException, complete will not have been set.
                // In that case just rethrow the TimeoutException without
                // remembering it in resultException.  That way, the caller
                // can call get() or get(timeout, unit) to try again.
                throw t;
            } catch(InterruptedException ie) {
                setResultException(ie);
            } catch(RuntimeException re) {
                setResultException(re);
            }
        }

        // We really shouldn't get CancellationException or
        // InterruptedException, but throw whatever kind we get.
        if( resultException != null ) {
           if( resultException instanceof ExecutionException ) {
               throw (ExecutionException) resultException;
           } else if( resultException instanceof InterruptedException) {
               throw (InterruptedException) resultException;
           } else {
               throw (RuntimeException) resultException;
           }
        }

        return resultValue;
        
    }

    @Override
    public boolean isCancelled() {
        // For now, we don't ever actually forcibly cancel a task
        // that hasn't executed.
        return false;
    }

    @Override
    public boolean isDone() {
        // Per the Future javadoc.  It's a little odd that isDone()
        // is required to return true even if cancel() was called but
        // returned false.  However, that's the behavior.  There's nothing
        // stopping the caller from still calling get() though.
        return (cancelCalled || complete);
    }

    EjbAsyncTask getEjbAsyncTask() {
        return ejbAsyncTask;
    }

    long getInvId() {
        return ejbAsyncTask.getInvId();
    }

    void setResultValue(V v) {
        // EjbAsyncTask calls this directly.  That way
        // we can return true from isDone() after completion of
        // the task, even if get() was not called.
        resultValue = v;
        complete = true;

    }

    void setResultException(Throwable t) {
        // EjbAsyncTask calls this directly.  That way
        // we can return true from isDone() after completion of
        // the task, even if get() was not called.
        resultException = t;
        complete = true;
    }

    // Internal method to retrieve any result value
    V getResultValue() {
        return resultValue;
    }

    Throwable getResultException() {
        return resultException;
    }

    public String toString() {

        StringBuffer sbuf = new StringBuffer();

        sbuf.append("EjbFutureTask  ");
        sbuf.append("taskId="+ejbAsyncTask.getInvId());
        sbuf.append(",cancelCalled="+cancelCalled);
        sbuf.append(",complete="+complete);
        if( complete ) {
            if( resultException == null ) {
                sbuf.append(",resultValue="+resultValue);
            } else {
                sbuf.append(",resultException="+resultException);
            }

        }

        return sbuf.toString();
    }
}
