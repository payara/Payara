/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.jvnet.hk2.annotations.Service;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.ejb.Container;
import com.sun.logging.LogDomains;
import java.lang.reflect.Method;
import javax.ejb.EJBException;

/**
 * @author Mahesh Kannan
 */
@Service
public class EjbAsyncInvocationManager {
    private static final Logger _logger = LogDomains.getLogger(EjbAsyncInvocationManager.class, LogDomains.EJB_LOGGER);

    private AtomicLong invCounter = new AtomicLong();
    
    // Map of Remote Future<> tasks.
    private ConcurrentHashMap<Long, EjbFutureTask> remoteTaskMap =
            new ConcurrentHashMap<Long, EjbFutureTask>();

    public Future createLocalFuture(EjbInvocation inv) {
        return createFuture(inv);
    }

    public Future createRemoteFuture(EjbInvocation inv, Container container, GenericEJBHome ejbHome) {

        // Create future but only use the local task to register in the
        // remote task map. We'll be replacing the result value with a
        // remote future task.
        EjbFutureTask localFutureTask = (EjbFutureTask) createFuture(inv);

        EjbRemoteFutureTask returnFuture = new EjbRemoteFutureTask(inv.getInvId(), ejbHome);

        // If this is a future task for a remote invocation
        // and the method has Future<T> return type, add the
        // corresponding local task to the async map.
        // TODO Need to work on cleanup logic.  Maybe we should store
        // this in a per-container data structure so cleanup is automatic
        // on container shutdown / undeployment.  Otherwise, we need a way to easily
        // identify all tasks for a given container for cleanup.
        Method m = inv.getMethod();
        if( !(m.getReturnType().equals(Void.TYPE))) {
            remoteTaskMap.put(inv.getInvId(), localFutureTask);
        }

        return returnFuture;
    }

    private Future createFuture(EjbInvocation inv) {
        // Always create a Local future task that is associated with the
        // invocation.
        EjbFutureTask futureTask = new EjbFutureTask(new EjbAsyncTask(), this);

        // Assign a unique id to this async task
        long invId = invCounter.incrementAndGet();
        inv.setInvId(invId);
        inv.setEjbFutureTask(futureTask);

        if( _logger.isLoggable(Level.FINE) ) {          
            _logger.log(Level.FINE, "Creating new async future task " + inv);
        }

        return futureTask;
    }


    public Future submit(EjbInvocation inv) {

        //We need to clone this invocation as submitting
        //so that the inv is *NOT* shared between the
        //current thread and the executor service thread
        EjbInvocation asyncInv = inv.clone();

        //EjbInvocation.clone clears the txOpsManager field so getTransactionOperationsManager()
        // returns null *after* EjbInvocation.clone(). However, in this case, we do want the original
        // TransactionOperationsManager so we explicitly set it after calling clone.
        //
        //Note: EjbInvocation implements TransactionOperationsManager so we can use asyncInv to
        //  be the TransactionOperationsManager for the new cloned invocation
        asyncInv.setTransactionOperationsManager(asyncInv);

        //In most of the cases we don't want registry entries from being reused in the cloned
        //  invocation, in which case, this method must be called. I am not sure if async
        //  ejb invocation must call this (It never did and someone in ejb team must investigate
        //  if clearRegistry() must be called from here)


        inv.clearYetToSubmitStatus();
        asyncInv.clearYetToSubmitStatus();

        EjbFutureTask futureTask = asyncInv.getEjbFutureTask();

        // EjbAsyncTask.initialize captures calling thread's
        // CallerPrincipal and sets it on the dispatch thread
        // before authorization.
        futureTask.getEjbAsyncTask().initialize(asyncInv);
        
        EjbContainerUtil ejbContainerUtil = EjbContainerUtilImpl.getInstance();
        return ejbContainerUtil.getThreadPoolExecutor(null).submit(futureTask.getEjbAsyncTask());
    }

    public void cleanupContainerTasks(Container container) {

        Set<Map.Entry<Long, EjbFutureTask>> entrySet = remoteTaskMap.entrySet();
        Iterator<Map.Entry<Long, EjbFutureTask>> iterator = entrySet.iterator();

        List<Long> removedTasks = new ArrayList<Long>();

        while(iterator.hasNext()) {

            Map.Entry<Long, EjbFutureTask> next = iterator.next();

            EjbAsyncTask task = next.getValue().getEjbAsyncTask();
            if( task.getEjbInvocation().container == container ) {

                removedTasks.add(task.getInvId());

                if( _logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Cleaning up async task " + task.getFutureTask());
                }

                // TODO Add some additional checking here for in-progress
                // tasks.
                iterator.remove();
            }
        }

        _logger.log(Level.FINE, "Cleaning up " + removedTasks.size() + "async tasks for " +
                   "EJB " + container.getEjbDescriptor().getName() + " .  Total of " +
                   remoteTaskMap.size() + " remaining");

        return;
    }

    RemoteAsyncResult remoteCancel(Long asyncTaskID) {
        EjbFutureTask task = getLocalTaskForID(asyncTaskID);

        if( _logger.isLoggable(Level.FINE) ) {
            EjbAsyncTask asyncTask = task.getEjbAsyncTask();
            _logger.log(Level.FINE, "Enter remoteCancel for async task " + asyncTaskID +
                    " : " + asyncTask.getEjbInvocation());
        }

        RemoteAsyncResult result = null;

        if( task.isDone() ) {

            // Since the task is done just return the result on this
            // internal remote request.
            result = new RemoteAsyncResult();
            
            result.resultException = task.getResultException();
            result.resultValue = task.getResultValue();
            result.asyncID = asyncTaskID;

            // The client object won't make another request once it
            // has the result so we can remove it from the container map.
            remoteTaskMap.remove(asyncTaskID);

        } else {

            // Set flag on invocation so bean method has visibility to
            // the fact that client called cancel()
            EjbInvocation inv = task.getEjbAsyncTask().getEjbInvocation();
            inv.setWasCancelCalled(true);

        }

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Exit remoteCancel for async task " + asyncTaskID +
                    " : " + task);
        }

        return result;
    }

    RemoteAsyncResult remoteIsDone(Long asyncTaskID) {

        EjbFutureTask task = getLocalTaskForID(asyncTaskID);

        if( _logger.isLoggable(Level.FINE) ) {
            EjbAsyncTask asyncTask = task.getEjbAsyncTask();
            _logger.log(Level.FINE, "Enter remoteisDone for async task " + asyncTaskID +
                    " : " + asyncTask.getEjbInvocation());
        }

        // If not done, just return null.
        RemoteAsyncResult result = null;

        if( task.isDone() ) {

            // Since the task is done just return the result on this
            // internal remote request.
            result = new RemoteAsyncResult();

            result.resultException = task.getResultException();
            result.resultValue = task.getResultValue();
            result.asyncID = asyncTaskID;

            // The client object won't make another request once it
            // has the result so we can remove it from the container map.
            remoteTaskMap.remove(asyncTaskID);

        }


        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Exit remoteIsDone for async task " + asyncTaskID +
                    " : " + task);
        }

        return result;
    }

    RemoteAsyncResult remoteGet(Long asyncTaskID) {
        EjbFutureTask task = getLocalTaskForID(asyncTaskID);

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Enter remoteGet for async task " + asyncTaskID +
                    " : " + task.getEjbAsyncTask().getEjbInvocation());
        }

        RemoteAsyncResult result = new RemoteAsyncResult();
        result.asyncID = asyncTaskID;

        try {

            result.resultValue = task.get();

        } catch(Throwable t) {

            result.resultException = t;
        }

        remoteTaskMap.remove(asyncTaskID);

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Exit remoteGet for async task " + asyncTaskID +
                    " : " + task);
        }

        return result;
    }

    RemoteAsyncResult remoteGetWithTimeout(Long asyncTaskID, Long timeout, TimeUnit unit)
            throws TimeoutException {

        EjbFutureTask task = getLocalTaskForID(asyncTaskID);

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Enter remoteGetWithTimeout for async task " + asyncTaskID +
                    " timeout=" + timeout + " , unit=" + unit + " : " +
                    task.getEjbAsyncTask().getEjbInvocation());
        }

        RemoteAsyncResult result = new RemoteAsyncResult();
        result.asyncID = asyncTaskID;

        try {

            result.resultValue = task.get(timeout, unit);

        } catch(TimeoutException to) {

            if( _logger.isLoggable(Level.FINE) ) {
                _logger.log(Level.FINE, "TimeoutException for async task " + asyncTaskID +
                    " : " + task);
            }   

            throw to;

        } catch(Throwable t) {

            result.resultException = t;
        }

        // As long as we're not throwing a TimeoutException, just remove the task
        // from the map.
        remoteTaskMap.remove(asyncTaskID);

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Exit remoteGetWithTimeout for async task " + asyncTaskID +
                    " : " + task);
        }
        
        return result;
    }


    private EjbFutureTask getLocalTaskForID(Long asyncTaskID) {
        EjbFutureTask task = remoteTaskMap.get(asyncTaskID);

        if( task == null ) {
            _logger.log(Level.FINE, "Could not find async task for ID " + asyncTaskID);

            throw new EJBException("Could not find Local Async task corresponding to ID " +
                asyncTaskID);                             
        }
        
        return task;
    }
}
