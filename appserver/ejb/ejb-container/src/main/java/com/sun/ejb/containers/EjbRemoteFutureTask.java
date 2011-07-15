/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import java.rmi.RemoteException;

import java.io.Serializable;

import javax.ejb.EJBException;

/**
 * @author Ken Saks
 */
public class EjbRemoteFutureTask<V>
    implements Future<V>, Serializable {


    private Long asyncId;

    private GenericEJBHome server;

    // Used to remember if cancel() was called already
    private boolean cancelCalled = false;
        

    private boolean complete = false;
    private V resultValue;
    private Throwable resultException;


    public EjbRemoteFutureTask(Long id, GenericEJBHome home) {

       asyncId = id;
       server = home;

    }

    public boolean cancel(boolean mayInterruptIfRunning) {

        if( !cancelCalled ) {

            cancelCalled = true;

            // mayInterruptIfRunning only determines whether the bean method
            // has visibility to the fact that the caller called Future.cancel().
            if( mayInterruptIfRunning ) {

                try {
                    //GenericEJBHome server2 = (GenericEJBHome)
                       //     javax.rmi.PortableRemoteObject.narrow(server, GenericEJBHome.class);
                    RemoteAsyncResult result = server.cancel(asyncId);
                    if( result != null ) {
                        if( result.resultException != null ) {
                            setResultException(result.resultException);
                        } else {
                            setResultValue((V) result.resultValue);
                        }
                    }

                } catch(RemoteException re) {

                    throw new EJBException("Exception during cancel operation", re);

                }

            }
        }

        // For now we don't even try checking to see if the task has started running.
        // Just return false so the caller knows the task could not be cancelled.      
        return false;
    }


    public V get() throws ExecutionException {

        // If get() has already been called, produce the same behavior
        // as initial call, except if get(timeout, unit) resulted in a
        // TimeoutException

        if( !complete ) {

            try {
                //GenericEJBHome server2 = (GenericEJBHome)
                            //javax.rmi.PortableRemoteObject.narrow(server, GenericEJBHome.class);
                RemoteAsyncResult result = server.get(asyncId);
                if( result != null ) {
                    if( result.resultException != null ) {
                        setResultException(result.resultException);
                    } else {
                        setResultValue((V) result.resultValue);
                    }
                }

            } catch(RemoteException re) {
                setResultException(re);
            }
        }

        if( resultException != null ) {
            if( resultException instanceof ExecutionException ) {
                throw (ExecutionException) resultException;
            } else {
                throw new ExecutionException(resultException);
            }
        }

        return resultValue;
    }

    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        // If get() has already been called, produce the same behavior
        // as initial call, except if get(timeout, unit) resulted in a
        // TimeoutException        

        if( !complete ) {

            try {

                RemoteAsyncResult result = server.getWithTimeout(asyncId, timeout, unit.toString());
                if( result != null ) {
                    if( result.resultException != null ) {
                        setResultException(result.resultException);
                    } else {
                        setResultValue((V) result.resultValue);
                    }
                }

            } catch(TimeoutException te) {
                throw te;
            } catch(RemoteException re) {
                setResultException(re);
            }
        }

        if( resultException != null ) {
            if( resultException instanceof ExecutionException ) {
                throw (ExecutionException) resultException;
            } else {
                throw new ExecutionException(resultException);
            }
        } 

        return resultValue;
    }


    public boolean isCancelled() {
        // For now, we don't ever actually forcibly cancel a task
        // that hasn't executed.
        return false;
    }


    public boolean isDone() {

        // Per the Future javadoc.  It's a little odd that isDone()
        // is required to return true even if cancel() was called but
        // returned false.  However, that's the behavior.  There's nothing
        // stopping the caller from still calling get() though.
        boolean isDone = cancelCalled || complete;

        if( !isDone ) {
            // Ask server.
            try {
                RemoteAsyncResult result = server.isDone(asyncId);
                if( result != null ) {
                    isDone = true;
                    if( result.resultException != null ) {
                        setResultException(result.resultException);
                    } else {
                        setResultValue((V) result.resultValue);
                    }
                }
            } catch(RemoteException re) {
                throw new EJBException(re);
            }
        }

        return isDone;
    }


    private void setResultValue(V v) {
        resultValue = v;
        complete = true;
    }

    private void setResultException(Throwable t) {
        resultException = t;
        complete = true;
    }


}
