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

package com.sun.enterprise.resource.pool.resizer;

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceState;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.pool.PoolProperties;
import com.sun.enterprise.resource.pool.ResourceHandler;
import com.sun.enterprise.resource.pool.datastructure.DataStructure;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resizer to remove unusable connections, maintain steady-pool <br>
 * <code>
 * Remove all invalid and idle resources, as a result one of the following may happen<br>
 * i)   equivalent to "pool-resize" quantity of resources are removed<br>
 * ii)  less than "pool-reize" quantity of resources are removed<br>
 * remove more resources to match pool-resize quantity, atmost scale-down till steady-pool-size<br>
 * iii) more than "pool-resize" quantity of resources are removed<br>
 * (1) if pool-size is less than steady-pool-size, bring it back to steady-pool-size.<br>
 * (2) if pool-size is greater than steady-pool-size, don't do anything.<br></code>
 *
 * @author Jagadish Ramu
 */
public class
        Resizer extends TimerTask {
    protected PoolInfo poolInfo;
    protected DataStructure ds;
    protected PoolProperties pool;
    protected ResourceHandler handler;
    protected boolean preferValidateOverRecreate = false;

    protected final static Logger _logger = LogDomains.getLogger(Resizer.class, LogDomains.RSR_LOGGER);

    public Resizer(PoolInfo poolInfo, DataStructure ds, PoolProperties pp, ResourceHandler handler,
            boolean preferValidateOverRecreate) {
        this.poolInfo = poolInfo;
        this.ds = ds;
        this.pool = pp;
        this.handler = handler;
        this.preferValidateOverRecreate = preferValidateOverRecreate;
    }

    public void run() {
        debug("Resizer for pool " + poolInfo);
        try {
            resizePool(true);
        } catch(Exception ex) {
            Object[] params = new Object[]{poolInfo, ex.getMessage()};
            _logger.log(Level.WARNING, "resource_pool.resize_pool_error", params);
        }
    }

    /**
     * Resize the pool
     *
     * @param forced when force is true, scale down the pool.
     */
    public void resizePool(boolean forced) {

        //If the wait queue is NOT empty, don't do anything.
        if (pool.getWaitQueueLength() > 0) {
            return;
        }

        //remove invalid and idle resource(s)
        int noOfResourcesRemoved = removeIdleAndInvalidResources();
        int poolScaleDownQuantity = pool.getResizeQuantity() - noOfResourcesRemoved;

        //scale down pool by atmost "resize-quantity"
        scaleDownPool(poolScaleDownQuantity, forced);

        //ensure that steady-pool-size is maintained
        ensureSteadyPool();

        debug("No. of resources held for pool [ " + poolInfo + " ] : " + ds.getResourcesSize());
    }

    /**
     * Make sure that steady pool size is maintained after all idle-timed-out,
     * invalid and scale-down resource removals.
     */
    private void ensureSteadyPool() {
        if (ds.getResourcesSize() < pool.getSteadyPoolSize()) {
            // Create resources to match the steady pool size
            for (int i = ds.getResourcesSize(); i < pool.getSteadyPoolSize(); i++) {
                try {
                    handler.createResourceAndAddToPool();
                } catch (PoolingException ex) {
                    Object[] params = new Object[]{poolInfo, ex.getMessage()};
                    _logger.log(Level.WARNING, "resource_pool.resize_pool_error", params);
                }
            }
        }
    }

    /**
     * Scale down pool by a <code>size &lt;= pool-resize-quantity</code>
     *
     * @param forced            scale-down only when forced
     * @param scaleDownQuantity no. of resources to remove
     */
    protected void scaleDownPool(int scaleDownQuantity, boolean forced) {

        if (pool.getResizeQuantity() > 0 && forced) {

            scaleDownQuantity = (scaleDownQuantity <= (ds.getResourcesSize() - pool.getSteadyPoolSize())) ? scaleDownQuantity : 0;

            ResourceHandle h;
            while (scaleDownQuantity > 0 && ((h = ds.getResource()) != null)) {
                ds.removeResource(h);
                scaleDownQuantity--;
            }
        }
    }

    /**
     * Get the free connections list from the pool, remove idle-timed-out resources
     * and then invalid resources.
     *
     * @return int number of resources removed
     */
    protected int removeIdleAndInvalidResources() {

        int poolSizeBeforeRemoval = ds.getResourcesSize();
        int noOfResourcesRemoved;
        //Find all Connections that are free/not-in-use
        ResourceState state;
        int size = ds.getFreeListSize();
        // let's cache the current time since precision is not required here.
        long currentTime = System.currentTimeMillis();
        int validConnectionsCounter = 0;
        int idleConnKeptInSteadyCounter = 0;
        
        //iterate through all thre active resources to find idle-time lapsed ones.
        ResourceHandle h;
        Set<ResourceHandle> activeResources = new HashSet<ResourceHandle>();
        Set<String> resourcesToValidate = new HashSet<String>();
        try {
            while ((h = ds.getResource()) != null ) {
                state = h.getResourceState();
                if (currentTime - state.getTimestamp() < pool.getIdleTimeout()) {
                    //Should be added for validation.
                    validConnectionsCounter++;
                    resourcesToValidate.add(h.toString());
                    activeResources.add(h);
                } else {
                    boolean isResourceEligibleForRemoval = 
                            isResourceEligibleForRemoval(h, validConnectionsCounter);
                    if(!isResourceEligibleForRemoval) {
                        //preferValidateOverrecreate true and connection is valid within SPS
                        validConnectionsCounter++;
                        idleConnKeptInSteadyCounter++;
                        activeResources.add(h);
                        debug("PreferValidateOverRecreate: Keeping idle resource "
                                + h + " in the steady part of the free pool "
                                + "as the RA reports it to be valid (" + validConnectionsCounter
                                + " <= " + pool.getSteadyPoolSize() + ")");

                    } else {
                        //Add to remove
                        ds.removeResource(h);
                    }
                }
            }
        } finally {
            for(ResourceHandle activeResource : activeResources) {
                ds.returnResource(activeResource);
            }
        }

        //remove invalid resources from the free (active) resources list.
        //Since the whole pool is not locked, it may happen that some of these resources may be
        //given to applications.
        removeInvalidResources(resourcesToValidate);

        //These statistic computations will work fine as long as resizer locks the pool throughout its operations.
        if (preferValidateOverRecreate) {
            debug("Idle resources validated and kept in the steady pool for pool [ " +
                    poolInfo + " ] - " + idleConnKeptInSteadyCounter);
            debug("Number of Idle resources freed for pool [ " + poolInfo + " ] - " +
                    (size - activeResources.size() - idleConnKeptInSteadyCounter));
            debug("Number of Invalid resources removed for pool [ " + poolInfo + " ] - " +
                    (activeResources.size() - ds.getFreeListSize() + idleConnKeptInSteadyCounter));
        } else {
            debug("Number of Idle resources freed for pool [ " + poolInfo + " ] - " +
                    (size - activeResources.size()));
            debug("Number of Invalid resources removed for pool [ " + poolInfo + " ] - " +
                    (activeResources.size() - ds.getFreeListSize()));
        }
        noOfResourcesRemoved = poolSizeBeforeRemoval - ds.getResourcesSize();
        return noOfResourcesRemoved;
    }

    /**
     * Removes invalid resource handles in the pool while resizing the pool.
     * Uses the Connector 1.5 spec 6.5.3.4 optional RA feature to obtain
     * invalid ManagedConnections
     *
     * @param freeConnectionsToValidate Set of free connections
     */
    private void removeInvalidResources(Set<String> freeConnectionsToValidate) {
        try {
            debug("Sending a set of free connections to RA, " +
                    "of size : " + freeConnectionsToValidate.size());
            int invalidConnectionsCount = 0;
            ResourceHandle handle;
            Set<ResourceHandle> validResources = new HashSet<ResourceHandle>();
            try {
                while ((handle = ds.getResource()) != null ) {
                    //validate if the connection is one in the freeConnectionsToValidate
                    if (freeConnectionsToValidate.contains(handle.toString())) {
                        Set connectionsToTest = new HashSet();
                        connectionsToTest.add(handle.getResource());
                        Set invalidConnections = handler.getInvalidConnections(connectionsToTest);
                        if (invalidConnections != null && invalidConnections.size() > 0) {
                            invalidConnectionsCount = validateAndRemoveResource(handle, invalidConnections);
                        } else {
                            //valid resource, return to pool
                            validResources.add(handle);
                        }
                    } else {
                        //valid resource, return to pool
                        validResources.add(handle);
                    }
                }
            } finally {
                for(ResourceHandle resourceHandle : validResources){
                    ds.returnResource(resourceHandle);
                }
                validResources.clear();
                debug("No. of invalid connections received from RA : " + invalidConnectionsCount);
            }
        } catch (ResourceException re) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "ResourceException while trying to get invalid connections from MCF", re);
            }
        } catch (Exception e) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Exception while trying to get invalid connections from MCF", e);
            }
        }
    }


    protected static void debug(String debugStatement) {
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE, debugStatement);
    }

    protected int validateAndRemoveResource(ResourceHandle handle, Set invalidConnections) {
        int invalidConnectionsCount = 0;
        for (Object o : invalidConnections) {
            ManagedConnection invalidConnection = (ManagedConnection) o;
            if (invalidConnection.equals(handle.getResource())) {
                ds.removeResource(handle);
                handler.invalidConnectionDetected(handle);
                invalidConnectionsCount++;
            }
        }
        return invalidConnectionsCount;
    }

    protected boolean isResourceEligibleForRemoval(ResourceHandle h,
            int validConnectionsCounter) {
        boolean isResourceEligibleForRemoval = false;

        ResourceState state = h.getResourceState();
        //remove all idle-time lapsed resources.
        ResourceAllocator alloc = h.getResourceAllocator();
        if (preferValidateOverRecreate && alloc.hasValidatingMCF()) {
            //validConnectionsCounter is incremented if the connection
            //is valid but only till the steady pool size.
            if (validConnectionsCounter < pool.getSteadyPoolSize()
                    && alloc.isConnectionValid(h)) {

                h.setLastValidated(System.currentTimeMillis());
                state.touchTimestamp();
            } else {
                //Connection invalid and hence remove resource.
                if (_logger.isLoggable(Level.FINEST)) {
                    if (validConnectionsCounter <= pool.getSteadyPoolSize()) {
                        _logger.log(Level.FINEST, "PreferValidateOverRecreate: "
                                + "Removing idle resource " + h
                                + " from the free pool as the RA reports it to be invalid");
                    } else {
                        _logger.log(Level.FINEST, "PreferValidateOverRecreate: "
                                + "Removing idle resource " + h
                                + " from the free pool as the steady part size has "
                                + "already been exceeded (" + validConnectionsCounter + " > "
                                + pool.getSteadyPoolSize() + ")");
                    }
                }
                isResourceEligibleForRemoval = true;
            }
        } else {
            isResourceEligibleForRemoval = true;
        }
        return isResourceEligibleForRemoval;
    }
}
