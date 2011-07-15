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

import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.pool.ResourceHandler;
import com.sun.enterprise.resource.pool.datastructure.strategy.ResourceSelectionStrategy;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.logging.LogDomains;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReadWriteLock based datastructure for pool
 * @author Jagadish Ramu
 */
public class RWLockDataStructure implements DataStructure {

    private ResourceHandler handler;
    private ResourceSelectionStrategy strategy;
    private int maxSize;

    private final ArrayList<ResourceHandle> resources;
    private ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock = reentrantLock.readLock();
    private ReentrantReadWriteLock.WriteLock writeLock = reentrantLock.writeLock();

    protected final static Logger _logger =
            LogDomains.getLogger(RWLockDataStructure.class,LogDomains.RSR_LOGGER);

    public RWLockDataStructure(String parameters, int maxSize,
                                              ResourceHandler handler, String strategyClass) {
        resources = new ArrayList<ResourceHandle>((maxSize > 1000) ? 1000 : maxSize);
        this.maxSize = maxSize;
        this.handler = handler;
        initializeStrategy(strategyClass);
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, "pool.datastructure.rwlockds.init");
        }
    }

    private void initializeStrategy(String strategyClass) {
        //TODO
    }

    /**
     * {@inheritDoc}
     */
    public int addResource(ResourceAllocator allocator, int count) throws PoolingException {
        int numResAdded = 0;
        writeLock.lock();
        //for now, coarser lock. finer lock needs "resources.size() < maxSize()" once more.
        try {
            for (int i = 0; i < count && resources.size() < maxSize; i++) {
                ResourceHandle handle = handler.createResource(allocator);
                resources.add(handle);
                numResAdded++;
            }
        } catch (Exception e) {
            PoolingException pe = new PoolingException(e.getMessage());
            pe.initCause(e);
            throw pe;
        } finally {
            writeLock.unlock();
        }
        return numResAdded;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceHandle getResource() {
        readLock.lock();
        for(int i=0; i<resources.size(); i++){
            ResourceHandle h = resources.get(i);
            if (!h.isBusy()) {
                readLock.unlock();
                writeLock.lock();
                try {
                    if (!h.isBusy()) {
                        h.setBusy(true);
                        return h;
                    } else {
                        readLock.lock();
                        continue;
                    }
                }finally {
                    writeLock.unlock();
                }
            } else {
                continue;
            }
        }
        readLock.unlock();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void removeResource(ResourceHandle resource) {
        boolean removed = false;
        writeLock.lock();
        try {
            removed = resources.remove(resource);
        } finally {
            writeLock.unlock();
        }
        if(removed) {
            handler.deleteResource(resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void returnResource(ResourceHandle resource) {
        writeLock.lock();
        try{
            resource.setBusy(false);
        }finally{
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getFreeListSize() {
        //inefficient implementation.
        int free = 0;
        readLock.lock();
        try{
            Iterator it = resources.iterator();
            while (it.hasNext()) {
                ResourceHandle rh = (ResourceHandle)it.next();
                if(!rh.isBusy()){
                    free++;
                }
            }
        }finally{
            readLock.unlock();
        }
        return free;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        writeLock.lock();
        try {
            Iterator it = resources.iterator();
            while (it.hasNext()) {
                handler.deleteResource((ResourceHandle) it.next());
                it.remove();
            }
        } finally {
            writeLock.unlock();
        }
        resources.clear();
    }

    /**
     * {@inheritDoc}
     */
    public int getResourcesSize() {
        return resources.size();
    }

    /**
     * Set maxSize based on the new max pool size set on the connection pool 
     * during a reconfiguration. 
     * 
     * @param maxSize
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public ArrayList<ResourceHandle> getAllResources() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
