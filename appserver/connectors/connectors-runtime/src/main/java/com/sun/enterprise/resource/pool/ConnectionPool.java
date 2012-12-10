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

package com.sun.enterprise.resource.pool;

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.appserv.connectors.internal.spi.BadConnectionEventListener;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.service.ConnectorAdminServiceUtils;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.ResourceState;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.listener.PoolLifeCycleListener;
import com.sun.enterprise.resource.pool.datastructure.DataStructure;
import com.sun.enterprise.resource.pool.datastructure.DataStructureFactory;
import com.sun.enterprise.resource.pool.resizer.Resizer;
import com.sun.enterprise.resource.pool.waitqueue.PoolWaitQueue;
import com.sun.enterprise.resource.pool.waitqueue.PoolWaitQueueFactory;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.RetryableUnavailableException;
import javax.transaction.Transaction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connection Pool for Connector & JDBC resources<br>
 *
 * @author Jagadish Ramu
 */
public class ConnectionPool implements ResourcePool, ConnectionLeakListener,
        ResourceHandler, PoolProperties {

    protected final static StringManager localStrings =
            StringManager.getManager(ConnectionPool.class);
    protected final static Logger _logger = LogDomains.getLogger(ConnectionPool.class,LogDomains.RSR_LOGGER);


    //pool life-cycle config properties
    protected int maxPoolSize;          // Max size of the pool
    protected int steadyPoolSize;       // Steady size of the pool
    protected int resizeQuantity;       // used by resizer to downsize the pool
    protected int maxWaitTime;          // The total time a thread is willing to wait for a resource object.
    protected long idletime;            // time (in ms) before destroying a free resource


    //pool config properties
    protected boolean failAllConnections = false;
    protected boolean matchConnections = false;
    protected boolean validation = false;
    protected boolean preferValidateOverRecreate = false;
    // hold on to the resizer task so we can cancel/reschedule it.
    protected Resizer resizerTask;


    protected volatile boolean poolInitialized = false;
    protected Timer timer;

    //advanced pool config properties
    protected boolean connectionCreationRetry_;
    protected int connectionCreationRetryAttempts_;
    protected long conCreationRetryInterval_;
    protected long validateAtmostPeriodInMilliSeconds_;
    protected int maxConnectionUsage_;
    //To validate a Sun RA Pool Connection if it hasnot been validated
    //  in the past x sec. (x=idle-timeout)
    //The property will be set from system property -
    //  com.sun.enterprise.connectors.ValidateAtmostEveryIdleSecs=true
    private boolean validateAtmostEveryIdleSecs = false;

    protected String resourceSelectionStrategyClass;

    protected PoolLifeCycleListener poolLifeCycleListener;

    //Gateway used to control the concurrency within the round-trip of resource access.
    protected ResourceGateway gateway;
    protected String resourceGatewayClass;

    protected ConnectionLeakDetector leakDetector;

    protected DataStructure ds;
    protected String dataStructureType;
    protected String dataStructureParameters;

    protected PoolWaitQueue waitQueue;
    protected PoolWaitQueue reconfigWaitQueue;
    private long reconfigWaitTime ;
    protected String poolWaitQueueClass;

    protected final PoolInfo poolInfo; //poolName

    private PoolTxHelper poolTxHelper;

    // NOTE: This resource allocator may not be the same as the allocator passed in to getResource()
    protected ResourceAllocator allocator;

    private boolean selfManaged_;

    private boolean blocked = false;
    
    
    public ConnectionPool(PoolInfo poolInfo, Hashtable env) throws PoolingException {
        this.poolInfo = poolInfo;
        setPoolConfiguration(env);
        initializePoolDataStructure();
        initializeResourceSelectionStrategy();
        initializePoolWaitQueue();
        poolTxHelper = new PoolTxHelper(this.poolInfo);
        gateway = ResourceGateway.getInstance(resourceGatewayClass);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Connection Pool : " + this.poolInfo);
        }
    }

    protected void initializePoolWaitQueue() throws PoolingException {
        waitQueue = PoolWaitQueueFactory.createPoolWaitQueue(poolWaitQueueClass);
        reconfigWaitQueue = PoolWaitQueueFactory.createPoolWaitQueue(poolWaitQueueClass);
    }

    protected void initializePoolDataStructure() throws PoolingException {
        ds = DataStructureFactory.getDataStructure(dataStructureType, dataStructureParameters,
                maxPoolSize, this, resourceSelectionStrategyClass);
    }

    protected void initializeResourceSelectionStrategy() {
        //do nothing
    }

    private void setPoolConfiguration(Hashtable env) throws PoolingException {

        ConnectorConnectionPool poolResource = getPoolConfigurationFromJndi(env);
        idletime = Integer.parseInt(poolResource.getIdleTimeoutInSeconds()) * 1000L;
        maxPoolSize = Integer.parseInt(poolResource.getMaxPoolSize());
        steadyPoolSize = Integer.parseInt(poolResource.getSteadyPoolSize());

        if (maxPoolSize < steadyPoolSize) {
            maxPoolSize = steadyPoolSize;
        }
        resizeQuantity = Integer.parseInt(poolResource.getPoolResizeQuantity());

        maxWaitTime = Integer.parseInt(poolResource.getMaxWaitTimeInMillis());
        //Make sure it's not negative.
        if (maxWaitTime < 0) {
            maxWaitTime = 0;
        }

        failAllConnections = poolResource.isFailAllConnections();

        validation = poolResource.isIsConnectionValidationRequired();

        validateAtmostEveryIdleSecs = poolResource.isValidateAtmostEveryIdleSecs();
        dataStructureType = poolResource.getPoolDataStructureType();
        dataStructureParameters = poolResource.getDataStructureParameters();
        poolWaitQueueClass = poolResource.getPoolWaitQueue();
        resourceSelectionStrategyClass = poolResource.getResourceSelectionStrategyClass();
        resourceGatewayClass = poolResource.getResourceGatewayClass();
        reconfigWaitTime = poolResource.getDynamicReconfigWaitTimeout();

        setAdvancedPoolConfiguration(poolResource);
    }

    protected ConnectorConnectionPool getPoolConfigurationFromJndi(Hashtable env) throws PoolingException {
        ConnectorConnectionPool poolResource;
        try {
            String jndiNameOfPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolInfo);
            poolResource = (ConnectorConnectionPool)
                    ConnectorRuntime.getRuntime().getResourceNamingService().lookup(poolInfo, jndiNameOfPool, env);
        } catch (NamingException ex) {
            throw new PoolingException(ex);
        }
        return poolResource;
    }

    // This method does not need to be synchronized since all caller methods are,
    // but it does not hurt. Just to be safe.
    protected synchronized void initPool(ResourceAllocator allocator)
            throws PoolingException {

        if (poolInitialized) {
            return;
        }

        this.allocator = allocator;

        createResources(this.allocator, steadyPoolSize - ds.getResourcesSize());

        // if the idle time out is 0, then don't schedule the resizer task
        if (idletime > 0) {
            scheduleResizerTask();
        }

        //Need to set the numConnFree of monitoring statistics to the steadyPoolSize
        //as monitoring might be ON during the initialization of pool.
        //Need not worry about the numConnUsed here as it would be initialized to
        //0 automatically.
        if(poolLifeCycleListener != null) {
            poolLifeCycleListener.connectionsFreed(steadyPoolSize);
        }
        
        poolInitialized = true;
    }

    /**
     * Schedules the resizer timer task. If a task is currently scheduled,
     * it would be canceled and a new one is scheduled.
     */
    private void scheduleResizerTask() {
        if (resizerTask != null) {
            //cancel the current task
            resizerTask.cancel();
            resizerTask = null;
        }

        resizerTask = initializeResizer();

        if (timer == null) {
            timer = ConnectorRuntime.getRuntime().getTimer();
        }

        timer.scheduleAtFixedRate(resizerTask, idletime, idletime);
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("scheduled resizer task");
        }
    }

    protected Resizer initializeResizer() {
        return new Resizer(poolInfo, ds, this, this, preferValidateOverRecreate);
    }

    /**
     * add a resource with status busy and not enlisted
     *
     * @param alloc ResourceAllocator
     * @throws PoolingException when unable to add a resource
     */
    public void addResource(ResourceAllocator alloc) throws PoolingException {
        int numResCreated = ds.addResource(alloc, 1);
        if(numResCreated > 0) {
            for(int i=0; i< numResCreated; i++) {
                if(poolLifeCycleListener != null) { 
                    poolLifeCycleListener.incrementNumConnFree(false, steadyPoolSize);
                }
            }
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Pool: resource added");
        }
    }

    /**
     * marks resource as free. This method should be used instead of directly calling
     * resoureHandle.getResourceState().setBusy(false)
     * OR
     * getResourceState(resourceHandle).setBusy(false)
     * as this method handles stopping of connection leak tracing
     * If connection leak tracing is enabled, takes care of stopping
     * connection leak tracing
     *
     * @param resourceHandle Resource
     */
    protected void setResourceStateToFree(ResourceHandle resourceHandle) {
        getResourceState(resourceHandle).setBusy(false);
        leakDetector.stopConnectionLeakTracing(resourceHandle, this);
    }

    /**
     * marks resource as busy. This method should be used instead of directly calling
     * resoureHandle.getResourceState().setBusy(true)
     * OR
     * getResourceState(resourceHandle).setBusy(true)
     * as this method handles starting of connection leak tracing
     * If connection leak tracing is enabled, takes care of starting
     * connection leak tracing
     *
     * @param resourceHandle Resource
     */
    protected void setResourceStateToBusy(ResourceHandle resourceHandle) {
        getResourceState(resourceHandle).setBusy(true);
        leakDetector.startConnectionLeakTracing(resourceHandle, this);
    }


    /**
     * returns resource from the pool.
     *
     * @return a free pooled resource object matching the ResourceSpec
     * @throws PoolingException - if any error occurrs
     *                          - or the pool has reached its max size and the
     *                          max-connection-wait-time-in-millis has expired.
     */
    public ResourceHandle getResource(ResourceSpec spec, ResourceAllocator alloc, Transaction txn)
            throws PoolingException, RetryableUnavailableException  {
        //Note: this method should not be synchronized or the
        //      startTime would be incorrect for threads waiting to enter

       /*
        * Here are all the comments for the method put together for
        * easy reference.
        *  1.
           // - Try to get a free resource. Note: internalGetResource()
           // will create a new resource if none is free and the max has
           // not been reached.
           // - If can't get one, get on the wait queue.
           // - Repeat this until maxWaitTime expires.
           // - If maxWaitTime == 0, repeat indefinitely.

           2.
           //the doFailAllConnectionsProcessing method would already
           //have been invoked by now.
           //We simply go ahead and create a new resource here
           //from the allocator that we have and adjust the resources
           //list accordingly so as to not exceed the maxPoolSize ever
           //(i.e if steadyPoolSize == maxPoolSize )
           ///Also since we are creating the resource out of the allocator
           //that we came into this method with, we need not worry about
           //matching
        */
        ResourceHandle result = null;

        long startTime = System.currentTimeMillis();
        long elapsedWaitTime;
        long remainingWaitTime = 0;

        while (true) {
            if (gateway.allowed()) {
                //See comment #1 above
                JavaEETransaction jtx = ((JavaEETransaction) txn);
                Set resourcesSet = null;
                if(jtx != null){
	                resourcesSet = jtx.getResources(poolInfo);
		        }
                //allow when the pool is not blocked or at-least one resource is
                //already obtained in the current transaction.
                if (!blocked || (resourcesSet != null && resourcesSet.size() > 0)) {
                    try {
                        result = internalGetResource(spec, alloc, txn);
                    } finally {
                        gateway.acquiredResource();
                    }
                }
            }
            if (result != null) {
                // got one, return it
                if (poolLifeCycleListener != null) {
                    poolLifeCycleListener.connectionAcquired(result.getId());
                    elapsedWaitTime = System.currentTimeMillis() - startTime;
                    poolLifeCycleListener.connectionRequestServed(elapsedWaitTime);
                    if (_logger.isLoggable( Level.FINE) ) {
                        _logger.log(Level.FINE, "Resource Pool: elapsed time " +
                                "(ms) to get connection for [" + spec + "] : " +
                                elapsedWaitTime);
                    }
                }
                //got one - seems we are not doing validation or matching
                //return it
                break;
            } else {
                // did not get a resource.
                if (maxWaitTime > 0) {
                    elapsedWaitTime = System.currentTimeMillis() - startTime;
                    if (elapsedWaitTime < maxWaitTime) {
                        // time has not expired, determine remaining wait time.
                        remainingWaitTime = maxWaitTime - elapsedWaitTime;
                    } else {
                        if (!blocked) {
                            // wait time has expired
                            if (poolLifeCycleListener != null) {
                                poolLifeCycleListener.connectionTimedOut();
                            }
                            String msg = localStrings.getStringWithDefault(
                                    "poolmgr.no.available.resource",
                                    "No available resource. Wait-time expired.");
                            throw new PoolingException(msg);
                        }
                    }
                }

                if (!blocked) {
                    //add to wait-queue
                    Object waitMonitor = waitQueue.addToQueue();
                    if (poolLifeCycleListener != null) {
                        poolLifeCycleListener.connectionRequestQueued();
                    }
                    synchronized (waitMonitor) {
                        try {
                            logFine("Resource Pool: getting on wait queue");
                            waitMonitor.wait(remainingWaitTime);

                        } catch (InterruptedException ex) {
                            //Could be system shutdown.
                            break;
                        }

                        //try to remove in case that the monitor has timed
                        // out.  We dont expect the queue to grow to great numbers
                        // so the overhead for removing inexistant objects is low.
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, "removing wait monitor from queue: " + waitMonitor);
                        }
                        if (waitQueue.removeFromQueue(waitMonitor)) {
                            if (poolLifeCycleListener != null) {
                                poolLifeCycleListener.connectionRequestDequeued();
                            }
                        }
                    }
                } else {
                    //add to reconfig-wait-queue
                    Object reconfigWaitMonitor = reconfigWaitQueue.addToQueue();
                    synchronized (reconfigWaitMonitor) {
                        try {
                            if(reconfigWaitTime > 0){
                                if(_logger.isLoggable(Level.FINEST)) {
                                    _logger.finest("[DRC] getting into reconfig wait queue for time ["+reconfigWaitTime+"]");
                                }
                                reconfigWaitMonitor.wait(reconfigWaitTime);
                            }
                        } catch (InterruptedException ex) {
                            //Could be system shutdown.
                            break;
                        }
                        //try to remove in case that the monitor has timed
                        // out.  We don't expect the queue to grow to great numbers
                        // so the overhead for removing inexistent objects is low.
                        if(_logger.isLoggable(Level.FINEST)) {
                            _logger.log(Level.FINEST, "[DRC] removing wait monitor from reconfig-wait-queue: " +
                                reconfigWaitMonitor);
                        }

                        reconfigWaitQueue.removeFromQueue(reconfigWaitMonitor);

                        if(_logger.isLoggable(Level.FINEST)) {
                            _logger.log(Level.FINEST, "[DRC] throwing Retryable-Unavailable-Exception");
                        }
                        RetryableUnavailableException rue = new RetryableUnavailableException("Pool Reconfigured, " +
                                "Connection Factory can retry the lookup");
                        rue.setErrorCode(BadConnectionEventListener.POOL_RECONFIGURED_ERROR_CODE);
                        throw rue;
                    }
                }
            }
        }

        alloc.fillInResourceObjects(result);
        return result;
    }

    /**
     * Overridden in AssocWithThreadResourcePool to fetch the resource
     * cached in the ThreadLocal
     * In ConnectionPool this simply returns null.
     *
     * @param spec  ResourceSpec
     * @param alloc ResourceAllocator to create a resource
     * @param tran  Transaction
     * @return ResourceHandle resource from ThreadLocal
     */
    protected ResourceHandle prefetch(ResourceSpec spec,
                                      ResourceAllocator alloc, Transaction tran) {
        return null;
    }

    protected ResourceHandle internalGetResource(ResourceSpec spec,
                                                 ResourceAllocator alloc,
                                                 Transaction tran) throws PoolingException {
        if (!poolInitialized) {
            initPool(alloc);
        }
        ResourceHandle result;

        result = getResourceFromTransaction(tran, alloc, spec);
        if(result != null){
            return result;
        }
        
        result = prefetch(spec, alloc, tran);
        if (result != null) {
            return result;
        }

        // We didnt get a connection that is already enlisted in the current transaction (if any).
        result = getUnenlistedResource(spec, alloc, tran);
        if (result != null) {
            if (maxConnectionUsage_ > 0) {
                result.incrementUsageCount();
            }
            if (poolLifeCycleListener != null) {
                poolLifeCycleListener.connectionUsed(result.getId());
                //Decrement numConnFree
                poolLifeCycleListener.decrementNumConnFree();
            }
        }
        return result;
    }

    /**
     * Try to get a resource from current transaction if it is shareable<br>
     * @param tran Current Transaction
     * @param alloc ResourceAllocator
     * @param spec ResourceSpec
     * @return result ResourceHandle
     */
    private ResourceHandle getResourceFromTransaction(Transaction tran, ResourceAllocator alloc, ResourceSpec spec) {
        ResourceHandle result = null;
        try {
            //comment-1: sharing is possible only if caller is marked
            //shareable, so abort right here if that's not the case
            if (tran != null && alloc.shareableWithinComponent()) {
                //TODO should be handled by PoolTxHelper
                JavaEETransaction j2eetran = (JavaEETransaction) tran;
                // case 1. look for free and enlisted in same tx
                Set set = j2eetran.getResources(poolInfo);
                if (set != null) {
                    Iterator iter = set.iterator();
                    while (iter.hasNext()) {
                        ResourceHandle h = (ResourceHandle) iter.next();
                        if (h.hasConnectionErrorOccurred()) {
                            iter.remove();
                            continue;
                        }

                        ResourceState state = h.getResourceState();
                        /*
                         * One can share a resource only for the following conditions:
                         * 1. The caller resource is shareable (look at the outermost
                         *    if marked comment-1
                         * 2. The resource enlisted inside the transaction is shareable
                         * 3. We are dealing with XA resources OR
                         *    We are dealing with a non-XA resource that's not in use
                         *    Note that sharing a non-xa resource that's in use involves
                         *    associating physical connections.
                         * 4. The credentials of the resources match
                         */
                        if (h.getResourceAllocator().shareableWithinComponent()) {
                            if (spec.isXA() || poolTxHelper.isNonXAResourceAndFree(j2eetran, h)) {
                                if (matchConnections) {
                                    if (!alloc.matchConnection(h)) {
                                        if (poolLifeCycleListener != null) {
                                            poolLifeCycleListener.connectionNotMatched();
                                        }
                                        continue;
                                    }
                                    if (h.hasConnectionErrorOccurred()) {
                                        if (failAllConnections) {
                                            //if failAllConnections has happened, we flushed the
                                            //pool, so we don't have to do iter.remove else we
                                            //will get a ConncurrentModificationException
                                            result = null;
                                            break;
                                        }
                                        iter.remove();
                                        continue;
                                    }
                                    if (poolLifeCycleListener != null) {
                                        poolLifeCycleListener.connectionMatched();
                                    }
                                }
                                if (state.isFree())
                                    setResourceStateToBusy(h);
                                result = h;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (ClassCastException e) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Pool: getResource : " +
                    "transaction is not JavaEETransaction but a " + tran.getClass().getName(), e);
            }
        }
        return result;
    }


    /**
     * To provide an unenlisted,  valid, matched resource from pool.
     *
     * @param spec  ResourceSpec
     * @param alloc ResourceAllocator
     * @param tran  Transaction
     * @return ResourceHandle resource from pool
     * @throws PoolingException Exception while getting resource from pool
     */
    protected ResourceHandle getUnenlistedResource(ResourceSpec spec, ResourceAllocator alloc,
                                                   Transaction tran) throws PoolingException {
        return getResourceFromPool(alloc, spec);
    }

    /**
     * Check whether the connection is valid
     *
     * @param h     Resource to be validated
     * @param alloc Allocator to validate the resource
     * @return boolean representing validation result
     */
    protected boolean isConnectionValid(ResourceHandle h, ResourceAllocator alloc) {
        boolean connectionValid = true;

        if (validation || validateAtmostEveryIdleSecs) {
            long validationPeriod;
            //validation period is idle timeout if validateAtmostEveryIdleSecs is set to true
            //else it is validateAtmostPeriodInMilliSeconds_
            if (validation)
                validationPeriod = validateAtmostPeriodInMilliSeconds_;
            else
                validationPeriod = idletime;
            boolean validationRequired = true;
            long currentTime = h.getLastValidated();
            if (validationPeriod > 0) {
                currentTime = System.currentTimeMillis();
                long timeSinceValidation = currentTime - h.getLastValidated();
                if (timeSinceValidation < validationPeriod)
                    validationRequired = false;
            }
            if (validationRequired) {
                if (!alloc.isConnectionValid(h)) {
                    connectionValid = false;
                    incrementNumConnFailedValidation();
                } else {
                    h.setLastValidated(currentTime);
                }
            }
        }
        return connectionValid;
    }

    /**
     * check whether the connection retrieved from the pool matches with the request.
     *
     * @param resource Resource to be matched
     * @param alloc    ResourceAllocator used to match the connection
     * @return boolean representing the match status of the connection
     */
    protected boolean matchConnection(ResourceHandle resource, ResourceAllocator alloc) {
        boolean matched = true;
        if (matchConnections) {
            matched = alloc.matchConnection(resource);
            if (poolLifeCycleListener != null) {
                if (matched) {
                    poolLifeCycleListener.connectionMatched();
                } else {
                    poolLifeCycleListener.connectionNotMatched();
                }
            }
        }
        return matched;
    }

    /**
     * return resource in free list. If none is found, try to scale up the pool/purge pool and <br>
     * return a new resource. returns null if the pool new resources cannot be created. <br>
     *
     * @param alloc ResourceAllocator
     * @return ResourceHandle resource from pool
     * @throws PoolingException if unable to create a new resource
     */
    protected ResourceHandle getResourceFromPool(ResourceAllocator alloc, ResourceSpec spec)
            throws PoolingException {

        // the order of serving a resource request
        // 1. free and enlisted in the same transaction
        // 2. free and unenlisted
        // Do NOT give out a connection that is
        // free and enlisted in a different transaction
        ResourceHandle result = null;

        ResourceHandle h;
        ArrayList<ResourceHandle> freeResources = new ArrayList<ResourceHandle>();
        try{
            while ((h = ds.getResource()) != null) {

                if (h.hasConnectionErrorOccurred()) {
                    ds.removeResource(h);
                    continue;
                }

                if (matchConnection(h, alloc)) {

                    boolean isValid = isConnectionValid(h, alloc);
                    if (h.hasConnectionErrorOccurred() || !isValid) {
                        if (failAllConnections) {
                            createSingleResourceAndAdjustPool(alloc, spec);
                            //no need to match since the resource is created with the allocator of caller.
                            break;
                        } else {
                            ds.removeResource(h);
                            //resource is invalid, continue iteration.
                            continue;
                        }
                    }
                    if(h.isShareable() == alloc.shareableWithinComponent()){
                        // got a matched, valid resource
                        result = h;
                        break;
                    }else{
                        freeResources.add(h);
                    }
                } else {
                    freeResources.add(h);
                }
            }
        }finally{
            //return all unmatched, free resources
            for (ResourceHandle freeResource : freeResources) {
                ds.returnResource(freeResource);
            }
            freeResources.clear();
        }

        if (result != null) {
            // set correct state
            setResourceStateToBusy(result);
        } else {
            result = resizePoolAndGetNewResource(alloc);
        }
        return result;
    }

    /**
     * Scale-up the pool to serve the new request. <br>
     * If pool is at max-pool-size and free resources are found, purge unmatched<br>
     * resources, create new connections and serve the request.<br>
     *
     * @param alloc ResourceAllocator used to create new resources
     * @return ResourceHandle newly created resource
     * @throws PoolingException when not able to create resources
     */
    private ResourceHandle resizePoolAndGetNewResource(ResourceAllocator alloc) throws PoolingException {
        //Must be called from the thread holding the lock to this pool.
        ResourceHandle result = null;
        int numOfConnsToCreate = 0;
        if (ds.getResourcesSize() < steadyPoolSize) {
            // May be all invalid resources are destroyed as
            // a result no free resource found and no. of resources is less than steady-pool-size
            numOfConnsToCreate = steadyPoolSize - ds.getResourcesSize();
        } else if (ds.getResourcesSize() + resizeQuantity <= maxPoolSize) {
            //Create and add resources of quantity "resizeQuantity"
            numOfConnsToCreate = resizeQuantity;
        } else if (ds.getResourcesSize() < maxPoolSize) {
            // This else if "test condition" is not needed. Just to be safe.
            // still few more connections (less than "resizeQuantity" and to reach the count of maxPoolSize)
            // can be added
            numOfConnsToCreate = maxPoolSize - ds.getResourcesSize();
        }
        if (numOfConnsToCreate > 0) {
            createResources(alloc, numOfConnsToCreate);
            result = getMatchedResourceFromPool(alloc);
        } else if (ds.getFreeListSize() > 0) {
            //pool cannot create more connections as it is at max-pool-size.
            //If there are free resources at max-pool-size, then none of the free resources
            //has matched this allocator's request (credential). Hence purge free resources
            //of size <=resizeQuantity
            if (purgeResources(resizeQuantity) > 0) {
                result = resizePoolAndGetNewResource(alloc);
            }
        }
        return result;
    }

    //TODO can't this be replaced by getResourceFromPool ?
    private ResourceHandle getMatchedResourceFromPool(ResourceAllocator alloc) {
        ResourceHandle handle;
        ResourceHandle result = null;
        ArrayList<ResourceHandle> activeResources = new ArrayList<ResourceHandle>();

        try{
            while ((handle = ds.getResource()) != null) {
                if (matchConnection(handle, alloc)) {
                    result = handle;
                    setResourceStateToBusy(result);
                    break;
                } else {
                    activeResources.add(handle);
            }
        }
        }finally{
            //return unmatched resources
            for (ResourceHandle activeResource : activeResources) {
                ds.returnResource(activeResource);
            }
            activeResources.clear();
        }
        return result;
    }

    /**
     * Try to purge resources by size <=  quantity <br>
     *
     * @param quantity maximum no. of resources to remove. <br>
     * @return resourceCount No. of resources actually removed. <br>
     */
    private int purgeResources(int quantity) {
        //Must be called from the thread holding the lock to this pool.
        int totalResourcesRemoved = 0;
        int freeResourcesCount = ds.getFreeListSize();
        int resourcesCount = (freeResourcesCount >= quantity) ?
                quantity : freeResourcesCount;
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Purging resources of size : " + resourcesCount);
        }
        for (int i = resourcesCount - 1; i >= 0; i--) {

            ResourceHandle resource = ds.getResource();
            if (resource != null) {
                ds.removeResource(resource);
                totalResourcesRemoved += 1;
            }
        }
        return totalResourcesRemoved;
    }

    /**
     * This method will be called from the getUnenlistedResource method if
     * we detect a failAllConnection flag.
     * Here we simply create a new resource and replace a free resource in
     * the pool by this resource and then give it out.
     * This replacement is required since the steadypoolsize might equal
     * maxpoolsize and in that case if we were not to remove a resource
     * from the pool, our resource would be above maxPoolSize
     *
     * @param alloc ResourceAllocator to create resource
     * @param spec  ResourceSpec
     * @return newly created resource
     * @throws PoolingException when unable to create a resource
     */
    protected ResourceHandle createSingleResourceAndAdjustPool(
            ResourceAllocator alloc, ResourceSpec spec)
            throws PoolingException {

        ResourceHandle handle = ds.getResource();
        if (handle != null) {
            ds.removeResource(handle);
        }

        ResourceHandle result = getNewResource(alloc);
        if (result != null) {
            alloc.fillInResourceObjects(result);
            result.getResourceState().setBusy(true);
        }

        return result;
    }


    /**
     * Method to be used to create resource, instead of calling ResourceAllocator.createConfigBean().
     * This method handles the connection creation retrial in case of failure
     *
     * @param resourceAllocator ResourceAllocator
     * @return ResourceHandle newly created resource
     * @throws PoolingException when unable create a resource
     */
    protected ResourceHandle createSingleResource(ResourceAllocator resourceAllocator) throws PoolingException {
        ResourceHandle resourceHandle;
        int count = 0;
        long startTime = 0;
        while (true) {
            try {
                count++;
                startTime = System.currentTimeMillis();
                resourceHandle = resourceAllocator.createResource();
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Time taken to create a single "
                            + "resource : "
                            + resourceHandle.getResourceSpec().getResourceId()
                            + " and adding to the pool (ms) : "
                            + (System.currentTimeMillis() - startTime));
                }
                if (validation || validateAtmostEveryIdleSecs)
                    resourceHandle.setLastValidated(System.currentTimeMillis());
                break;
            } catch (Exception ex) {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Connection creation failed for " + count + " time. It will be retried, "
                        + "if connection creation retrial is enabled.", ex);
                }
                if (!connectionCreationRetry_ || count > connectionCreationRetryAttempts_)
                    throw new PoolingException(ex);
                try {
                    Thread.sleep(conCreationRetryInterval_);
                } catch (InterruptedException ie) {
                    //ignore this exception
                }
            }
        }
        return resourceHandle;
    }

    /**
     * Create specified number of resources.
     *
     * @param alloc ResourceAllocator
     * @param size  number of resources to create.
     * @throws PoolingException When unable to create a resource
     */
    private void createResources(ResourceAllocator alloc, int size) throws PoolingException {
        for (int i = 0; i < size; i++) {
            createResourceAndAddToPool(alloc);
        }
    }


    public void setPoolLifeCycleListener(PoolLifeCycleListener listener) {
        this.poolLifeCycleListener = listener;
    }

    public void removePoolLifeCycleListener() {
        poolLifeCycleListener = null;
    }

    public void deleteResource(ResourceHandle resourceHandle) {
        try {
            resourceHandle.getResourceAllocator().destroyResource(resourceHandle);
        } catch (Exception ex) {
            Object[] args = new Object[] {
                    resourceHandle.getResourceSpec().getPoolInfo(),
                    ex.getMessage() == null ? "" : ex.getMessage() };
            _logger.log(Level.WARNING, "poolmgr.destroy_resource_failed", args);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "poolmgr.destroy_resource_failed", ex);
            }
        } finally {
            //if connection leak tracing is running on connection being
            //destroyed due to error, then stop it
            if (resourceHandle.getResourceState().isBusy())
                leakDetector.stopConnectionLeakTracing(resourceHandle, this);
            if (poolLifeCycleListener != null) {
                poolLifeCycleListener.connectionDestroyed(resourceHandle.getId());

                if (resourceHandle.getResourceState().isBusy()) {
                    //Destroying a Connection due to error
                    poolLifeCycleListener.decrementConnectionUsed(resourceHandle.getId());
                    if(!resourceHandle.isMarkedForReclaim()) {
                        //If a connection is not reclaimed (in case of a reconfig)
                        //increment numConnFree
                        poolLifeCycleListener.incrementNumConnFree(true, steadyPoolSize);
                    }
                } else {
                    //Destroying a free Connection
                    poolLifeCycleListener.decrementNumConnFree();
                }
            }
        }
    }

    /**
     * this method is called to indicate that the resource is
     * not used by a bean/application anymore
     */
    public void resourceClosed(ResourceHandle h)
            throws IllegalStateException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Pool: resourceClosed: " + h);
        }

        ResourceState state = getResourceState(h);
        if (state == null) {
            throw new IllegalStateException("State is null");
        }

        if (!state.isBusy()) {
            throw new IllegalStateException("state.isBusy() : false");
        }

        setResourceStateToFree(h);  // mark as not busy
        state.touchTimestamp();

        if (state.isUnenlisted() || (poolTxHelper.isNonXAResource(h) && 
                poolTxHelper.isLocalTransactionInProgress() 
                && poolTxHelper.isLocalResourceEligibleForReuse(h))) {
            freeUnenlistedResource(h);
        }
        
        if (poolLifeCycleListener != null) {
            poolLifeCycleListener.connectionReleased(h.getId());
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Pool: resourceFreed: " + h);
        }
    }

    /**
     * If the resource is used for <i>maxConnectionUsage</i> times, destroy and create one
     *
     * @param handle Resource to be checked
     */
    protected void performMaxConnectionUsageOperation(ResourceHandle handle) {
        ds.removeResource(handle);
        _logger.log(Level.INFO, "resource_pool.remove_max_used_conn",
                new Object[]{handle.getId(), handle.getUsageCount()});

        if (poolLifeCycleListener != null) {
            poolLifeCycleListener.decrementConnectionUsed(handle.getId());
        }

        //compensate with a new resource only when the pool-size is less than steady-pool-size
        if (ds.getResourcesSize() < steadyPoolSize) {
            try {
                createResourceAndAddToPool(handle.getResourceAllocator());
            } catch (Exception e) {
                _logger.log(Level.WARNING, "resource_pool.failed_creating_resource", e);
            }
        }
    }

    protected void freeUnenlistedResource(ResourceHandle h) {
        freeResource(h);
    }

    protected void freeResource(ResourceHandle resourceHandle) {
        if(cleanupResource(resourceHandle)) {
            //Only when resource handle usage count is more than maxConnUsage
            if (maxConnectionUsage_ > 0 &&
                    resourceHandle.getUsageCount() >= maxConnectionUsage_) {
                 performMaxConnectionUsageOperation(resourceHandle);
            } else {
                // Put it back to the free collection.
                ds.returnResource(resourceHandle);
                //update the monitoring data
                if (poolLifeCycleListener != null) {
                    poolLifeCycleListener.decrementConnectionUsed(resourceHandle.getId());
                    poolLifeCycleListener.incrementNumConnFree(false, steadyPoolSize);
                }
            }
            //for both the cases of free.add and maxConUsageOperation, a free resource is added.
            // Hence notify waiting threads
            notifyWaitingThreads();
        }
    }
    
    protected boolean cleanupResource(ResourceHandle handle) {
        boolean cleanupSuccessful = true;
        // cleanup resource
        try {
            ResourceAllocator alloc = handle.getResourceAllocator();
            alloc.cleanup(handle);
        } catch (PoolingException ex) {
            Object[] params = new Object[]{poolInfo, ex};
            _logger.log(Level.WARNING, "cleanup.resource.failed", params);
            cleanupSuccessful = false;
            resourceErrorOccurred(handle);
        }
        return cleanupSuccessful;
    }

    public void resourceErrorOccurred(ResourceHandle h)
            throws IllegalStateException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Pool: resourceErrorOccurred: " + h);
        }

        if (failAllConnections) {
            doFailAllConnectionsProcessing();
            return;
        }

        ResourceState state = getResourceState(h);
        // The reason is that normally connection error is expected
        // to occur only when the connection is in use by the application.
        // When there is connection validation involved, the connection
        // can be checked for validity "before" it is passed to the
        // application i.e. when the resource is still free. Since,
        // the connection error can occur when the resource
        // is free, the following is being commented out.

        /*if (state == null ||
        state.isBusy() == false) {
            throw new IllegalStateException();
        } */

        if (state == null) {
            throw new IllegalStateException();
        }

        // changed order of commands

        //Commenting resources.remove() out since we will call an iter.remove()
        //in the getUnenlistedResource method in the if check after
        //matchManagedConnections or in the internalGetResource method
        //If we were to call remove directly here there is always the danger
        //of a ConcurrentModificationExceptionbeing thrown when we return
        //
        //In case of this method being called asynchronously, since
        //the resource has been marked as "errorOccured", it will get
        //removed in the next iteration of getUnenlistedResource
        //or internalGetResource
        ds.removeResource(h);
    }

    private void doFailAllConnectionsProcessing() {
        logFine("doFailAllConnectionsProcessing entered");
        cancelResizerTask();
        if (poolLifeCycleListener != null) {
            poolLifeCycleListener.connectionValidationFailed(ds.getResourcesSize());
        }
        
        emptyPool();
        try {
            createResources(allocator, steadyPoolSize);
        } catch (PoolingException pe) {
            //Ignore and hope the resizer does its stuff
            logFine("in doFailAllConnectionsProcessing couldn't create steady resources");
        }
        scheduleResizerTask();
        logFine("doFailAllConnectionsProcessing done - created new resources");

    }

    /**
     * this method is called when a resource is enlisted in
     *
     * @param tran     Transaction
     * @param resource ResourceHandle
     */
    public void resourceEnlisted(Transaction tran, ResourceHandle resource)
            throws IllegalStateException {
            poolTxHelper.resourceEnlisted(tran, resource);
    }

    /**
     * this method is called when transaction tran is completed
     *
     * @param tran   Transaction
     * @param status status of transaction
     */
    public void transactionCompleted(Transaction tran, int status) throws IllegalStateException {
        List<ResourceHandle> delistedResources = poolTxHelper.transactionCompleted(tran, status, poolInfo);
        for (ResourceHandle resource : delistedResources) {
            // Application might not have closed the connection.
            if (isResourceUnused(resource)) {
                freeResource(resource);
            }
        }
    }

    protected boolean isResourceUnused(ResourceHandle h) {
        return h.getResourceState().isFree();
    }


    public ResourceHandle createResource(ResourceAllocator alloc) throws PoolingException {
        //NOTE : Pool should not call this method directly, it should be called only by pool-datastructure
        ResourceHandle result = createSingleResource(alloc);

        ResourceState state = new ResourceState();
        state.setBusy(false);
        state.setEnlisted(false);
        result.setResourceState(state);

        if (poolLifeCycleListener != null) {
            poolLifeCycleListener.connectionCreated();
        }
        return result;
    }

    public void createResourceAndAddToPool() throws PoolingException {
        createResourceAndAddToPool(allocator);
    }

    public Set getInvalidConnections(Set connections) throws ResourceException {
        return allocator.getInvalidConnections(connections);
    }

    public void invalidConnectionDetected(ResourceHandle h) {
        incrementNumConnFailedValidation();
    }

    public void resizePool(boolean forced) {
        resizerTask.resizePool(forced);
    }


    protected void notifyWaitingThreads() {
        // notify the first thread in the waitqueue
        Object waitMonitor = null;
        synchronized (waitQueue) {
            if (waitQueue.getQueueLength() > 0) {
                waitMonitor = waitQueue.remove();
                if(poolLifeCycleListener != null) {
                    poolLifeCycleListener.connectionRequestDequeued();
                }
            }
        }
        if (waitMonitor != null) {
            synchronized (waitMonitor) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Notifying wait monitor : " + waitMonitor.toString());
                }
                waitMonitor.notifyAll();
            }
        } else {
            logFine(" Wait monitor is null");
        }
    }

    private void incrementNumConnFailedValidation() {
        if (poolLifeCycleListener != null) {
            poolLifeCycleListener.connectionValidationFailed(1);
        }
    }

    private ResourceHandle getNewResource(ResourceAllocator alloc) throws PoolingException {
        ds.addResource(alloc, 1);
        return ds.getResource();
    }


    private ResourceState getResourceState(ResourceHandle h) {
        return h.getResourceState();
    }

    public void emptyPool() {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "EmptyPool: Name = " + poolInfo);
        }
        ds.removeAll();
    }

    public void emptyFreeConnectionsInPool() {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Emptying free connections in pool : " + poolInfo);
        }

        ResourceHandle h;
        while ((h = ds.getResource()) != null) {
            ds.removeResource(h);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Pool [");
        sb.append(poolInfo);
        sb.append("] PoolSize=");
        sb.append(ds.getResourcesSize());
        sb.append("  FreeResources=");
        sb.append(ds.getFreeListSize());
        sb.append("  QueueSize=");
        sb.append(waitQueue.getQueueLength());
        sb.append(" matching=");
        sb.append((matchConnections ? "on" : "off"));
        sb.append(" validation=");
        sb.append((validation ? "on" : "off"));
        return sb.toString();
    }

    /**
     * @inheritDoc
     */
    public void blockRequests(long waitTimeout){
        blocked = true;
        this.reconfigWaitTime = waitTimeout;
    }

    /**
     * @inheritDoc
     */
    public PoolWaitQueue getPoolWaitQueue(){
        return waitQueue;
    }

    /**
     * @inheritDoc
     */
    public PoolWaitQueue getReconfigWaitQueue(){
        return reconfigWaitQueue;
    }


    public long getReconfigWaitTime() {
        return reconfigWaitTime;
    }

    /**
     * Reinitialize connections established in the connection pool and 
     * bring the pool to steady pool size. 
     * 
     * @throws com.sun.appserv.connectors.internal.api.PoolingException
     */
    public synchronized boolean flushConnectionPool() throws PoolingException {

        logFine("Flush Connection Pool entered");        
        
        if(!poolInitialized) {
            _logger.log(Level.WARNING, "poolmgr.flush_noop_pool_not_initialized", getPoolInfo());
            String exString = localStrings.getString("poolmgr.flush_noop_pool_not_initialized",
                    poolInfo.toString());
            throw new PoolingException(exString);
        }
        
        try {
            cancelResizerTask();
            ds.removeAll();
            scheduleResizerTask();
            increaseSteadyPoolSize(steadyPoolSize);
        } catch(PoolingException ex) {
            _logger.log(Level.WARNING, "pool.flush_pool_failure", 
                    new Object[] {getPoolInfo(), ex.getMessage()});
            throw ex;
        }
        logFine("Flush Connection Pool done");        
            
        return true;
    }
    
    /**
     * Reconfigure the Pool's properties. The reconfigConnectorConnectionPool
     * method in the ConnectorRuntime will use this method (through PoolManager)
     * if it needs to just change pool properties and not recreate the pool
     *
     * @param poolResource - the ConnectorConnectionPool JavaBean that holds
     *                     the new pool properties
     * @throws PoolingException if the pool resizing fails
     */
    public synchronized void reconfigurePool(ConnectorConnectionPool poolResource)
            throws PoolingException {
        int _idleTime = Integer.parseInt(poolResource.getIdleTimeoutInSeconds())
                * 1000;
        if (poolInitialized) {
            if (_idleTime != idletime && _idleTime != 0) {
                idletime = _idleTime;
                scheduleResizerTask();
            }
            if (_idleTime == 0) {
                //resizerTask.cancel();
                cancelResizerTask();
            }
        }
        idletime = _idleTime;

        resizeQuantity = Integer.parseInt(poolResource.getPoolResizeQuantity());

        maxWaitTime = Integer.parseInt(poolResource.getMaxWaitTimeInMillis());
        //Make sure it's not negative.
        if (maxWaitTime < 0) {
            maxWaitTime = 0;
        }

        validation = poolResource.isIsConnectionValidationRequired();
        failAllConnections = poolResource.isFailAllConnections();
        setAdvancedPoolConfiguration(poolResource);

        //Self managed quantities. These are ignored if self management
        //is on
        if (!isSelfManaged()) {
            int _maxPoolSize = Integer.parseInt(poolResource.getMaxPoolSize());
            int oldMaxPoolSize = maxPoolSize;

            if (_maxPoolSize < steadyPoolSize) {
                //should not happen, admin must throw exception when this condition happens.
                //as a precaution set max pool size to steady pool size
                maxPoolSize = steadyPoolSize;
            } else {
                maxPoolSize = _maxPoolSize;
            }

            if (oldMaxPoolSize != maxPoolSize) {
                ds.setMaxSize(maxPoolSize);
            }
            int _steadyPoolSize = Integer.parseInt(poolResource.getSteadyPoolSize());
            int oldSteadyPoolSize = steadyPoolSize;

            if (_steadyPoolSize > maxPoolSize) {
                //should not happen, admin must throw exception when this condition happens.
                //as a precaution set steady pool size to max pool size
                steadyPoolSize = maxPoolSize;
            } else {
                steadyPoolSize = _steadyPoolSize;
            }

            if (poolInitialized) {
                //In this case we need to kill extra connections in the pool
                //For the case where the value is increased, we need not
                //do anything
                //num resources to kill is decided by the resources in the pool.
                //if we have less than current maxPoolSize resources, we need to
                //kill less.
                int toKill = ds.getResourcesSize() - maxPoolSize;

                if (toKill > 0)
                    killExtraResources(toKill);
            }
            reconfigureSteadyPoolSize(oldSteadyPoolSize, _steadyPoolSize);
        }
    }

    protected void reconfigureSteadyPoolSize(int oldSteadyPoolSize,
                                           int newSteadyPoolSize) throws PoolingException {
        if(oldSteadyPoolSize != steadyPoolSize) {
            if(poolInitialized) {
                if(oldSteadyPoolSize < steadyPoolSize) {
                    increaseSteadyPoolSize(newSteadyPoolSize);
                    if (poolLifeCycleListener != null) {
                        poolLifeCycleListener.connectionsFreed(steadyPoolSize);
                    }
                }
            }
        }
    }

    /**
     * sets advanced pool properties<br>
     * used during pool configuration (initialization) and re-configuration<br>
     *
     * @param poolResource Connector Connection Pool
     */
    private void setAdvancedPoolConfiguration(ConnectorConnectionPool poolResource) {
        matchConnections = poolResource.matchConnections();
        preferValidateOverRecreate = poolResource.isPreferValidateOverRecreate();
        maxConnectionUsage_ = Integer.parseInt(poolResource.getMaxConnectionUsage());
        connectionCreationRetryAttempts_ = Integer.parseInt
                (poolResource.getConCreationRetryAttempts());
        //Converting seconds to milliseconds as TimerTask will take input in milliseconds
        conCreationRetryInterval_ =
                Integer.parseInt(poolResource.getConCreationRetryInterval()) * 1000L;
        connectionCreationRetry_ = connectionCreationRetryAttempts_ > 0;

        validateAtmostPeriodInMilliSeconds_ =
                Integer.parseInt(poolResource.getValidateAtmostOncePeriod()) * 1000L;
        boolean connectionLeakReclaim_ = poolResource.isConnectionReclaim();
        long connectionLeakTimeoutInMilliSeconds_ = Integer.parseInt(
                poolResource.getConnectionLeakTracingTimeout()) * 1000L;

        boolean connectionLeakTracing_ = connectionLeakTimeoutInMilliSeconds_ > 0;
        if (leakDetector == null) {
            leakDetector = new ConnectionLeakDetector(poolInfo, connectionLeakTracing_,
                    connectionLeakTimeoutInMilliSeconds_, connectionLeakReclaim_);
        } else {
            leakDetector.reset(connectionLeakTracing_,
                    connectionLeakTimeoutInMilliSeconds_, connectionLeakReclaim_);
        }
    }

    /**
     * Kill the extra resources.<br>
     * The maxPoolSize being reduced causes this method to
     * be called
     */
    private void killExtraResources(int numToKill) {
        cancelResizerTask();

        ResourceHandle h;
        for (int i = 0; i < numToKill && ((h = ds.getResource()) != null); i++) {
            ds.removeResource(h);
        }
        scheduleResizerTask();
    }

    /*
    * Increase the number of steady resources in the pool
    * if we detect that the steadyPoolSize has been increased
    */
    private void increaseSteadyPoolSize(int newSteadyPoolSize)
            throws PoolingException {
        cancelResizerTask();
        for (int i = ds.getResourcesSize(); i < newSteadyPoolSize; i++) {
            createResourceAndAddToPool(allocator);
        }
        scheduleResizerTask();
    }

    /**
     * @param alloc ResourceAllocator
     * @throws PoolingException when unable to create a resource
     */
    private void createResourceAndAddToPool(ResourceAllocator alloc) throws PoolingException {
        addResource(alloc);
    }

    /**
     * Switch on matching of connections in the pool.
     */
    public void switchOnMatching() {
        matchConnections = true;
    }

    /**
     * query the name of this pool. Required by monitoring
     *
     * @return the name of this pool
     */
    public PoolInfo getPoolInfo() {
        return poolInfo;
    }

    public synchronized void cancelResizerTask() {

        logFine("Cancelling resizer");
        if (resizerTask != null) {
            resizerTask.cancel();
        }
        resizerTask = null;

        if (timer != null) {
            timer.purge();
        }
    }


    /**
     * This method can be used for debugging purposes
     */
    public synchronized void dumpPoolStatus() {
        _logger.log(Level.INFO, "Name of pool :" + poolInfo);
        _logger.log(Level.INFO, "Free connections :" + ds.getFreeListSize());
        _logger.log(Level.INFO, "Total connections :" + ds.getResourcesSize());
        _logger.log(Level.INFO, "Pool's matching is :" + matchConnections);
    }


    private void logFine(String msg) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(msg);
        }
    }

    //Self management methods
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getResizeQuantity() {
        return resizeQuantity;
    }

    public long getIdleTimeout() {
        return idletime;
    }

    public int getWaitQueueLength() {
        return waitQueue.getQueueLength();
    }

    public int getSteadyPoolSize() {
        return steadyPoolSize;
    }


    public void setMaxPoolSize(int size) {
        if (size < ds.getResourcesSize()) {
            synchronized (this) {
                int toKill = ds.getResourcesSize() - size;
                if (toKill > 0) {
                    try {
                        killExtraResources(toKill);
                    } catch (Exception re) {
                        //ignore for now
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, "setMaxPoolSize:: killExtraResources " +
                                    "throws exception: " + re.getMessage());
                        }
                    }
                }
            }
        }
        maxPoolSize = size;
    }

    public void setSteadyPoolSize(int size) {
        steadyPoolSize = size;
    }

    public void setSelfManaged(boolean selfManaged) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Setting selfManaged to : " + selfManaged + " in pool : " + poolInfo);
        }
        selfManaged_ = selfManaged;
    }

    protected boolean isSelfManaged() {
        return selfManaged_;
    }

    public void potentialConnectionLeakFound() {
        if (poolLifeCycleListener != null)
            poolLifeCycleListener.foundPotentialConnectionLeak();
    }

    public void printConnectionLeakTrace(StringBuffer stackTrace) {
        if (poolLifeCycleListener != null) {
            String msg = localStrings.getStringWithDefault("monitoring.statistics", "Monitoring Statistics :");
            stackTrace.append("\n");
            stackTrace.append(msg);
            stackTrace.append("\n");
            //TODO : change toString() to a more specific method name
            poolLifeCycleListener.toString(stackTrace);
        }
    }

    public void reclaimConnection(ResourceHandle handle) {
        //all reclaimed connections must be killed instead of returning them
        //to the pool 
        //Entity beans when used in bean managed transaction will face an issue
        //since connections are destroyed during reclaim. Stateful session beans 
        // will work fine.
        String msg = localStrings.getString("reclaim.leaked.connection", poolInfo);
        _logger.log(Level.INFO, msg);
        ds.removeResource(handle);
        notifyWaitingThreads();

    }

    /**
     * Get Connection Pool status by computing the free/used values of the 
     * connections in the pool. Computations are based on whether the pool 
     * is initialized or not when this method is invoked. 

     * @return PoolStatus object
     */
    public PoolStatus getPoolStatus() {
        PoolStatus poolStatus = new PoolStatus(this.poolInfo);
        int numFree = (this.poolInitialized) ? ds.getFreeListSize() : 0;
        int numUsed = (this.poolInitialized) ? ds.getResourcesSize()-ds.getFreeListSize() : 0;
        poolStatus.setNumConnFree(numFree);
        poolStatus.setNumConnUsed(numUsed);
        return poolStatus;
    }
}
