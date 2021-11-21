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
// Portions Copyright [2021] Payara Foundation and/or affiliates

package com.sun.enterprise.resource.pool.datastructure;

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.pool.ResourceHandler;
import com.sun.logging.LogDomains;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReadWriteLock based datastructure for pool
 * @author Jagadish Ramu
 */
public class RWLockDataStructure implements DataStructure {

    private final ResourceHandler handler;
    private int maxSize;

    private final List<ResourceHandle> allResources;
    private final Deque<ResourceHandle> freeResources;

    private final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = reentrantLock.writeLock();

    protected static final Logger _logger = LogDomains.getLogger(RWLockDataStructure.class,LogDomains.RSR_LOGGER);

    public RWLockDataStructure(int maxSize, ResourceHandler handler) {
        allResources = new ArrayList<>(Math.min(maxSize, 1000));
        freeResources = new ArrayDeque<>(Math.min(maxSize, 1000));
        this.maxSize = maxSize;
        this.handler = handler;
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, "pool.datastructure.rwlockds.init");
        }
    }

    /**
     * {@inheritDoc}
     */
    public int addResource(final ResourceAllocator allocator, int count) throws PoolingException {
        int numResAdded = 0;
        for (int i = 0; i < count && isNotFull(); i++) {
            try {
                final ResourceHandle handle = handler.createResource(allocator);
                boolean added = addResourceInternal(handle);
                if (added) {
                    numResAdded++;
                } else {
                    handler.deleteResource(handle);
                }
            } catch (Exception e) {
                throw new PoolingException(e.getMessage(), e);
            }
        }
        return numResAdded;
    }

    private boolean isNotFull() {
        return doLockSecured(() -> allResources.size() < maxSize, readLock);
    }

    private boolean addResourceInternal(final ResourceHandle handle) {
        return doLockSecured(() -> {
            if (allResources.size() < maxSize) {
                boolean added = allResources.add(handle);
                if (added) {
                    freeResources.offerLast(handle);
                }
                return added;
            }
            return false;
        }, writeLock);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceHandle getResource() {
        return doLockSecured(() -> {
            final ResourceHandle resourceHandle = freeResources.pollFirst();
            if (resourceHandle != null) {
                resourceHandle.setBusy(true);
            }
            return resourceHandle;
        }, writeLock);
    }

    /**
     * {@inheritDoc}
     */
    public void removeResource(ResourceHandle resource) {
        boolean removed = doLockSecured(() -> {
            final boolean removedResource = allResources.remove(resource);
            if (removedResource) {
                freeResources.remove(resource);
            }
            return removedResource;
        }, writeLock);
        if (removed) {
            handler.deleteResource(resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void returnResource(final ResourceHandle resource) {
        doLockSecured(() -> {
            resource.setBusy(false);
            freeResources.offerFirst(resource);
        }, writeLock);
    }

    /**
     * {@inheritDoc}
     */
    public int getFreeListSize() {
        return doLockSecured(freeResources::size, readLock);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        final List<ResourceHandle> removedResources = new ArrayList<>();
        doLockSecured(() -> {
            removedResources.addAll(allResources);
            allResources.clear();
            freeResources.clear();
        }, writeLock);
        for(ResourceHandle resourceHandle : removedResources) {
            handler.deleteResource(resourceHandle);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getResourcesSize() {
        return doLockSecured(allResources::size, readLock);
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

    private void doLockSecured(Runnable lockSecuredProc, Lock lock) {
        lock.lock();
        try {
            lockSecuredProc.run();
        } finally {
            lock.unlock();
        }
    }

    private <T> T doLockSecured(Supplier<T> lockSecuredFunc, Lock lock) {
        lock.lock();
        try {
            return lockSecuredFunc.get();
        } finally {
            lock.unlock();
        }
    }

}
