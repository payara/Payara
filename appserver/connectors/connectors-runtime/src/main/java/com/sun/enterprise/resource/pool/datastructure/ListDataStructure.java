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

package com.sun.enterprise.resource.pool.datastructure;

import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.pool.ResourceHandler;
import com.sun.enterprise.resource.pool.datastructure.strategy.ResourceSelectionStrategy;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * List based datastructure that can be used by connection pool <br>
 *
 * @author Jagadish Ramu
 */
public class ListDataStructure implements DataStructure {
    private final ArrayList<ResourceHandle> free;
    private final ArrayList<ResourceHandle> resources;
    //Max Size of the datastructure.Depends mostly on the max-pool-size of 
    // the connection pool.
    private int maxSize;
    private final DynamicSemaphore dynSemaphore;

    private ResourceHandler handler;
    private ResourceSelectionStrategy strategy;

    public ListDataStructure(String parameters, int maxSize, ResourceHandler handler, String strategyClass) {
        resources = new ArrayList<ResourceHandle>((maxSize > 1000) ? 1000 : maxSize);
        free = new ArrayList<ResourceHandle>((maxSize > 1000) ? 1000 : maxSize);
        this.handler = handler;
        initializeStrategy(strategyClass);
        dynSemaphore = new DynamicSemaphore();
        setMaxSize(maxSize);
    }

    
    /**
     * Set maxSize based on the new max pool size set on the connection pool 
     * during a reconfiguration. 
     * 1. When permits contained within the dynamic semaphore are greater than 0,
     * maxSize is increased and hence so many permits are released.
     * 2. When permits contained within the dynamic semaphore are less than 0, 
     * maxSize has reduced to a smaller value. Hence so many permits are reduced 
     * from the semaphore's available limit for the subsequent resource requests 
     * to act based on the new configuration.
     * @param newMaxSize
     */
    public synchronized void setMaxSize(int newMaxSize) {
            
        //Find currently open with the current maxsize
        int permits = newMaxSize - this.maxSize;

        if (permits == 0) {
            //None are open
            return;
        } else if (permits > 0) {
            //Case when no of permits are increased
            this.dynSemaphore.release(permits);
        } else {
            //permits would be a -ve value
            //Case when no of permits are to be reduced.
            permits *= -1;
            this.dynSemaphore.reducePermits(permits);
        }
        this.maxSize = newMaxSize;
    }
    
    private void initializeStrategy(String strategyClass) {
        //TODO
    }

    /**
     * creates a new resource and adds to the datastructure.
     *
     * @param allocator ResourceAllocator
     * @param count     Number (units) of resources to create
     * @return int number of resources added
     */
    public int addResource(ResourceAllocator allocator, int count) throws PoolingException {
        int numResAdded = 0;
        for (int i = 0; i < count && resources.size() < maxSize; i++) {
            boolean lockAcquired = dynSemaphore.tryAcquire();
            if(lockAcquired) {
                try {
                    ResourceHandle handle = handler.createResource(allocator);
                    synchronized (resources) {
                        synchronized (free) {
                            free.add(handle);
                            resources.add(handle);
                            numResAdded++;
                        }
                    }
                } catch (Exception e) {
                    dynSemaphore.release();
                    PoolingException pe = new PoolingException(e.getMessage());
                    pe.initCause(e);
                    throw pe;
                }
            }
        }
        return numResAdded;
    }

    /**
     * get a resource from the datastructure
     *
     * @return ResourceHandle
     */
    public ResourceHandle getResource() {
        ResourceHandle resource = null;
        if (strategy != null) {
            resource = strategy.retrieveResource();
        } else {
            synchronized (free) {
                if (free.size() > 0){
                    resource = free.remove(0);
                }
            }
        }
        return resource;
    }

    /**
     * remove the specified resource from the datastructure
     *
     * @param resource ResourceHandle
     */
    public void removeResource(ResourceHandle resource) {
        boolean removed = false;
        synchronized (resources) {
            synchronized (free) {
                free.remove(resource);
                removed = resources.remove(resource);
            }
        }
        if(removed) {
            dynSemaphore.release();
            handler.deleteResource(resource);
        }
    }

    /**
     * returns the resource to the datastructure
     *
     * @param resource ResourceHandle
     */
    public void returnResource(ResourceHandle resource) {
        synchronized (free) {
            free.add(resource);
        }
    }

    /**
     * get the count of free resources in the datastructure
     *
     * @return int count
     */
    public int getFreeListSize() {
        return free.size();
    }

    /**
     * remove & destroy all resources from the datastructure.
     */
    public void removeAll() {
        synchronized (resources) {
            synchronized (free) {
                while (resources.size() > 0) {
                    ResourceHandle handle = resources.remove(0);
                    free.remove(handle);
                    dynSemaphore.release();
                    handler.deleteResource(handle);
                }
            }
        }
        free.clear();
        resources.clear();
    }

    /**
     * get total number of resources in the datastructure
     *
     * @return int count
     */
    public int getResourcesSize() {
        return resources.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList<ResourceHandle> getAllResources() {
        return this.resources;
    }

    /**
     * Semaphore whose available permits change according to the 
     * changes in max-pool-size via a reconfiguration.
     */
    private static final class DynamicSemaphore extends Semaphore {
        
        DynamicSemaphore() {
            //Default is 0
            super(0);
        }
        
        @Override
        protected void reducePermits(int size) {
            super.reducePermits(size);
        }
    }
}
