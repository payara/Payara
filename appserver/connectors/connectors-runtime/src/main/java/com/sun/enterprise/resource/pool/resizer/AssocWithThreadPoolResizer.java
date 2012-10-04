/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.resource.AssocWithThreadResourceHandle;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceState;
import com.sun.enterprise.resource.pool.PoolProperties;
import com.sun.enterprise.resource.pool.ResourceHandler;
import com.sun.enterprise.resource.pool.datastructure.DataStructure;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.resource.ResourceException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Resizer for Associate With Thread type pools to remove unusable connections
 * and maintain steady pool size.
 * 
 * @author Shalini M
 */
public class AssocWithThreadPoolResizer extends Resizer {

    public AssocWithThreadPoolResizer(PoolInfo poolInfo, DataStructure ds,
            PoolProperties pp, ResourceHandler handler,
            boolean preferValidateOverRecreate) {
        super(poolInfo, ds, pp, handler, preferValidateOverRecreate);
    }

    /**
     * Scale down pool by a <code>size &lt;= pool-resize-quantity</code>
     *
     * @param forced            scale-down only when forced
     * @param scaleDownQuantity no. of resources to remove
     */
    @Override
    protected void scaleDownPool(int scaleDownQuantity, boolean forced) {
        if (pool.getResizeQuantity() > 0 && forced) {

            scaleDownQuantity = (scaleDownQuantity <=
                    (ds.getResourcesSize() - pool.getSteadyPoolSize())) ? scaleDownQuantity : 0;

            debug("Scaling down pool by quantity : " + scaleDownQuantity);
            Set<ResourceHandle> resourcesToRemove = new HashSet<ResourceHandle>();
            try {
                for (ResourceHandle h : ds.getAllResources()) {
                    if (scaleDownQuantity > 0) {
                        synchronized (h.lock) {
                            if (!h.isBusy()) {
                                resourcesToRemove.add(h);
                                ((AssocWithThreadResourceHandle) h).setDirty();
                                scaleDownQuantity--;
                            }
                        }
                    }
                }
            } finally {
                for (ResourceHandle resourceToRemove : resourcesToRemove) {
                    if (ds.getAllResources().contains(resourceToRemove)) {
                        ds.removeResource(resourceToRemove);
                    }
                }
            }
        }
    }

    /**
     * Get the free connections list from the pool, remove idle-timed-out resources
     * and then invalid resources.
     *
     * @return int number of resources removed
     */
    @Override
    protected int removeIdleAndInvalidResources() {

        int poolSizeBeforeRemoval = ds.getResourcesSize();
        int noOfResourcesRemoved = 0;
        // let's cache the current time since precision is not required here.
        long currentTime = System.currentTimeMillis();
        int validConnectionsCounter = 0;
        int idleConnKeptInSteadyCounter = 0;
        ResourceState state;

        Set<ResourceHandle> resourcesToValidate = new HashSet<ResourceHandle>();
        Set<ResourceHandle> resourcesToRemove = new HashSet<ResourceHandle>();
        try {
            //iterate through all the resources to find idle-time lapsed ones.
            for (ResourceHandle h : ds.getAllResources()) {
                synchronized (h.lock) {
                    state = h.getResourceState();
                    if (!state.isBusy()) {
                        if (currentTime - state.getTimestamp() < pool.getIdleTimeout()) {
                            //Should be added for validation.
                            if (state.isUnenlisted() && state.isFree()) {
                                if (((AssocWithThreadResourceHandle) h).isAssociated()) {
                                    ((AssocWithThreadResourceHandle) h).setAssociated(false);
                                    validConnectionsCounter++;
                                    resourcesToValidate.add(h);
                                }
                            }
                        } else {
                            boolean isResourceEligibleForRemoval =
                                    isResourceEligibleForRemoval(h, validConnectionsCounter);
                            if (!isResourceEligibleForRemoval) {
                                //preferValidateOverrecreate true and connection is valid within SPS
                                validConnectionsCounter++;
                                idleConnKeptInSteadyCounter++;
                                debug("PreferValidateOverRecreate: Keeping idle resource "
                                        + h + " in the steady part of the free pool "
                                        + "as the RA reports it to be valid (" + validConnectionsCounter
                                        + " <= " + pool.getSteadyPoolSize() + ")");
                            } else {
                                //Add this to remove later
                                resourcesToRemove.add(h);
                                ((AssocWithThreadResourceHandle) h).setDirty();
                            }
                        }
                    }
                }
            }
        } finally {
            for (ResourceHandle resourceToRemove : resourcesToRemove) {
                if (ds.getAllResources().contains(resourceToRemove)) {
                    ds.removeResource(resourceToRemove);
                }
            }
        }

        //remove invalid resources from the free (active) resources list.
        //Since the whole pool is not locked, it may happen that some of these 
        //resources may be given to applications.
        int noOfInvalidResources = removeInvalidResources(resourcesToValidate);

        //These statistic computations will work fine as long as resizer
        //locks the pool throughout its operations.
        if (preferValidateOverRecreate) {
            debug("Idle resources validated and kept in the steady pool for pool [ "
                    + poolInfo + " ] - " + idleConnKeptInSteadyCounter);
            debug("Number of Idle resources freed for pool [ " + poolInfo + " ] - "
                    + (resourcesToRemove.size()));
            debug("Number of Invalid resources removed for pool [ " + poolInfo + " ] - "
                    + noOfInvalidResources);
        } else {
            debug("Number of Idle resources freed for pool [ " + poolInfo + " ] - "
                    + resourcesToRemove.size());
            debug("Number of Invalid resources removed for pool [ " + poolInfo + " ] - "
                    + noOfInvalidResources);
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
    private int removeInvalidResources(Set<ResourceHandle> freeConnectionsToValidate) {
        int invalidConnectionsCount = 0;
        try {
            debug("Sending a set of free connections to RA, "
                    + "of size : " + freeConnectionsToValidate.size());
            try {
                for (ResourceHandle handle : freeConnectionsToValidate) {
                    if (handle != null) {
                        Set connectionsToTest = new HashSet();
                        connectionsToTest.add(handle.getResource());
                        Set invalidConnections = handler.getInvalidConnections(connectionsToTest);
                        if (invalidConnections != null && invalidConnections.size() > 0) {
                            invalidConnectionsCount = validateAndRemoveResource(handle, invalidConnections);
                        } else {
                            //valid resource
                        }
                    }
                }
            } finally {
                debug("No. of invalid connections received from RA : " + invalidConnectionsCount);
            }
        } catch (ResourceException re) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "ResourceException while trying to get invalid connections from MCF", re);
            }
        } catch (Exception e) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Exception while trying to get invalid connections from MCF", e);
            }
        }
        return invalidConnectionsCount;
    }
}
