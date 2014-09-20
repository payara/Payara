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

package com.sun.enterprise.connectors.work;


import com.sun.corba.ee.spi.threadpool.WorkQueue;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.connectors.work.context.WorkContextHandlerImpl;
import com.sun.enterprise.connectors.work.monitor.WorkManagementProbeProvider;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.work.*;

import org.glassfish.logging.annotation.LogMessageInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorkCoordinator : Coordinates one work's execution. Handles all
 * exception conditions and does JTS coordination.
 *
 * @author Binod P.G
 */
public final class WorkCoordinator {

    static final int WAIT_UNTIL_START = 1;
    static final int WAIT_UNTIL_FINISH = 2;
    static final int NO_WAIT = 3;

    static final int CREATED = 1;
    static final int STARTED = 2;
    static final int COMPLETED = 3;
    static final int TIMEDOUT = 4;

    private volatile int waitMode;
    private volatile int state = CREATED;

    private final javax.resource.spi.work.Work work;
    private final long timeout;
    private long startTime;
    private final ExecutionContext ec;
    private final WorkQueue queue;
    private final WorkListener listener;
    private volatile WorkException exception;
    private final Object lock;
    private static int seed;
    private final int id;

    private static final Logger logger = LogFacade.getLogger();

    private WorkManagementProbeProvider probeProvider = null;

    private ConnectorRuntime runtime;
    private String raName = null;

    private WorkContextHandlerImpl contextHandler;

    /**
     * Constructs a coordinator
     *
     * @param work     A work object as submitted by the resource adapter
     * @param timeout  timeout for the work instance
     * @param ec       ExecutionContext object.
     * @param queue    WorkQueue of the threadpool, to which the work
     *                 will be submitted
     * @param listener WorkListener object from the resource adapter.
     */
    public WorkCoordinator(javax.resource.spi.work.Work work,
                           long timeout,
                           ExecutionContext ec,
                           WorkQueue queue,
                           WorkListener listener, WorkManagementProbeProvider probeProvider,
                           ConnectorRuntime runtime, String raName,
                           WorkContextHandlerImpl handler) {

        this.work = work;
        this.timeout = timeout;
        this.ec = ec;
        this.queue = queue;
        this.listener = listener;
        this.id = increaseSeed();
        this.runtime = runtime;
        this.lock = new Object();
        this.probeProvider = probeProvider;
        this.raName = raName;
        this.contextHandler = handler;
    }

    public String getRAName(){
        return raName;
    }
 
    /**
     * Submits the work to the queue and generates a work accepted event.
     */
    public void submitWork(int waitModeValue) {
        this.waitMode = waitModeValue;
        this.startTime = System.currentTimeMillis();
        if (listener != null) {
            listener.workAccepted(
                    new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
        }
        if (probeProvider != null) {
            probeProvider.workSubmitted(raName);
            probeProvider.workQueued(raName);
        }
        queue.addWork(new OneWork(work, this, Thread.currentThread().getContextClassLoader()));
    }

    @LogMessageInfo(
            message = "Resource adapter association failed.",
            comment = "Failed to associate Resource Adapter bean to Work instance.",
            level = "SEVERE",
            cause = "Resource Adapter throws exception during ManagedConnectionFactory.setResourceAdapter().",
            action = "[1] If you are using third party resource adapter, contact resource adapter vendor." +
                     "[2] If you are a resource adapter developer, please check the resource adapter code.",
            publish = true)
    private static final String RAR_RA_ASSOCIATE_ERROR = "AS-RAR-05005";

    /**
     * Pre-invoke operation. This does the following
     * <pre>
     * 1. Notifies the <code> WorkManager.startWork </code> method.
     * 2. Checks whether the wok has already been timed out.
     * 3. Recreates the transaction with JTS.
     * </pre>
     */
    public void preInvoke() {

        // If the work is just scheduled, check whether it has timed out or not. 
        if (waitMode == NO_WAIT && timeout > -1) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (probeProvider != null) {
                probeProvider.workWaitedFor(raName, elapsedTime);
            }

            if (elapsedTime > timeout) {
                workTimedOut();
            }
        }


        // If the work is timed out then return.
        if (!proceed()) {
            if (probeProvider != null) {
                probeProvider.workDequeued(raName);
            }
            return;
        }else{
            if (probeProvider != null) {
                probeProvider.workProcessingStarted(raName);
                probeProvider.workDequeued(raName);
            }
        }

        // associate ResourceAdapter if the Work is RAA
        if(work instanceof ResourceAdapterAssociation){
            try{
                runtime.associateResourceAdapter(raName, (ResourceAdapterAssociation)work);
            }catch(ResourceException re){
                logger.log(Level.SEVERE, RAR_RA_ASSOCIATE_ERROR, re);
            }
        }

        // Change the status to started.
        setState(STARTED);

        if (waitMode == WAIT_UNTIL_START) {
            unLock();
        }

        // All set to do start the work. So send the event.
        if (listener != null) {
            listener.workStarted(
                    new WorkEvent(this, WorkEvent.WORK_STARTED, work, null));
        }

        //set the unauthenticated securityContext before executing the work            
        com.sun.enterprise.security.SecurityContext.setUnauthenticatedContext();

    }

    public void setupContext(OneWork oneWork) throws WorkException {
        contextHandler.setupContext(getExecutionContext(ec, work), this, oneWork);
    }

    /**
     * Post-invoke operation. This does the following after the work is executed.
     * <pre>
     * 1. Releases the transaction with JTS.
     * 2. Generates work completed event.
     * 3. Clear the thread context.
     * </pre>
     */
    public void postInvoke() {
        boolean txImported = (getExecutionContext(ec, work) != null && getExecutionContext(ec, work).getXid() != null);
        try {
            JavaEETransactionManager tm = getTransactionManager();
            if (txImported) {
                tm.release(getExecutionContext(ec, work).getXid());
            }
        } catch (WorkException ex) {
            setException(ex);
        } finally {
            try {
                if(!isTimedOut()){
                    if (probeProvider != null) {
                        probeProvider.workProcessingCompleted(raName);
                        probeProvider.workProcessed(raName);
                    }

                    //If exception is not null, the work has already been rejected.
                    if (listener != null) {
                        listener.workCompleted(
                                new WorkEvent(this, WorkEvent.WORK_COMPLETED, work,
                                        getException()));
                    }
                }

                //Also release the TX from the record of TX Optimizer
                if (txImported) {
                    getTransactionManager().clearThreadTx();
                }
            } catch(Exception e) {
	            logger.log(Level.WARNING, e.getMessage());
            }finally{
                //reset the securityContext once the work has completed            
                com.sun.enterprise.security.SecurityContext.setUnauthenticatedContext();
            }
        }

        setState(COMPLETED);
        if (waitMode == WAIT_UNTIL_FINISH) {
            unLock();
        }
    }

    /**
     * Times out the thread
     */
    private void workTimedOut() {
        setState(TIMEDOUT);
        exception = new WorkRejectedException();
        exception.setErrorCode(WorkException.START_TIMED_OUT);
        if (listener != null) {
            listener.workRejected(
                    new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception));
        }
        if (probeProvider != null) {
            probeProvider.workTimedOut(raName);
        }
    }

    /**
     * Checks the work is good to proceed with further processing.
     *
     * @return true if the work is good and false if it is bad.
     */
    public boolean proceed() {
        return !isTimedOut() && exception == null;
    }

    public boolean isTimedOut() {
        return getState() == TIMEDOUT;
    }

    /**
     * Retrieves the exception created during the work's execution.
     *
     * @return a <code>WorkException</code> object.
     */
    public WorkException getException() {
        return exception;
    }

    /**
     * Accepts an exception object and converts to a
     * <code>WorkException</code> object.
     *
     * @param e Throwable object.
     */
    public void setException(Throwable e) {
        if (getState() < STARTED) {
            if (e instanceof WorkRejectedException) {
                exception = (WorkException) e;
            } else if (e instanceof WorkException) {
                WorkException we = (WorkException) e;
                exception = new WorkRejectedException(we);
                exception.setErrorCode(we.getErrorCode());
            } else {
                exception = new WorkRejectedException(e);
                exception.setErrorCode(WorkException.UNDEFINED);
            }
        } else {
            if (e instanceof WorkCompletedException) {
                exception = (WorkException) e;
            } else if (e instanceof WorkException) {
                WorkException we = (WorkException) e;
                exception = new WorkCompletedException(we);
                exception.setErrorCode(we.getErrorCode());
            } else {
                exception = new WorkCompletedException(e);
                exception.setErrorCode(WorkException.UNDEFINED);
            }
        }
    }

    /**
     * Lock the thread upto the end of execution or start of work
     * execution.
     */
    public void lock() {

        if (!lockRequired()) {
            return;
        }

        try {
            synchronized (lock) {
                while (checkStateBeforeLocking()) {
                    if (timeout != -1) {
                        lock.wait(timeout);
                    } else {
                        lock.wait();
                    }
                }
            }

            if (getState() < STARTED) {
                workTimedOut();
            }
            if (lockRequired()) {
                synchronized (lock) {
                    if (checkStateBeforeLocking()) {
                        lock.wait();
                    }
                }
            }

        } catch (Exception e) {
            setException(e);
        }
    }

    /**
     * Unlocks the thread.
     */
    private void unLock() {
        try {
            synchronized (lock) {
                lock.notifyAll();
            }
        } catch (Exception e) {
            setException(e);
        }
    }

    /**
     * Returns the string representation of WorkCoordinator.
     *
     * @return Unique identification concatenated by work object.
     */
    public String toString() {
        return id + ":" + work;
    }

    /**
     * Sets the state of the work  coordinator object
     *
     * @param state CREATED or Either STARTED or COMPLETED or TIMEDOUT
     */
    public synchronized void setState(int state) {
        this.state = state;
    }

    /**
     * Retrieves the state of the work coordinator object.
     *
     * @return Integer represnting the state.
     */
    public synchronized int getState() {
        return state;
    }

    private boolean lockRequired() {
        if (!proceed()) {
            return false;
        }
        if (waitMode == NO_WAIT) {
            return false;
        }
        if (waitMode == WAIT_UNTIL_FINISH) {
            return getState() < COMPLETED;
        }
        if (waitMode == WAIT_UNTIL_START) {
            return getState() < STARTED;
        }
        return false;
    }

    /**
     * It is possible that state is modified just before
     * the lock is obtained. So check it again.
     * Access the variable directly to avoid nested locking.
     */
    private boolean checkStateBeforeLocking() {
        if (waitMode == WAIT_UNTIL_FINISH) {
            return state < COMPLETED;
        }
        if (waitMode == WAIT_UNTIL_START) {
            return state < STARTED;
        }
        return false;
    }

    private JavaEETransactionManager getTransactionManager() {
        return runtime.getTransactionManager();
    }

    public static ExecutionContext getExecutionContext(ExecutionContext ec, Work work) {
        if (ec == null) {
            return WorkContextHandlerImpl.getExecutionContext(work);
        }
        return ec;
    }
    
    public static synchronized int increaseSeed() {
        return ++seed;
    }

}
