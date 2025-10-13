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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
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

    private final AtomicInteger remainingCapacity;

    protected static final Logger _logger = LogDomains.getLogger(RWLockDataStructure.class,LogDomains.RSR_LOGGER);

    public RWLockDataStructure(int maxSize, ResourceHandler handler) {
        allResources = new ArrayList<>(Math.min(maxSize, 1000));
        freeResources = new ArrayDeque<>(Math.min(maxSize, 1000));
        this.maxSize = maxSize;
        this.handler = handler;
        this.remainingCapacity = new AtomicInteger(maxSize);
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, "pool.datastructure.rwlockds.init");
        }
    }

    /**
     * {@inheritDoc}
     */
    public int addResource(final ResourceAllocator allocator, int count) throws PoolingException {
        int numResAdded = 0;
        for (int i = 0; i < count && canGrow(); i++) {
            try {
                final ResourceHandle handle = handler.createResource(allocator);
                doLockSecured(() -> {
                    allResources.add(handle);
                    freeResources.offerLast(handle);
                }, writeLock);
                numResAdded++;
            } catch (Exception e) {
                increaseRemainingCapacity();
                throw new PoolingException(e.getMessage(), e);
            }
        }
        return numResAdded;
    }

    private boolean canGrow() {
        int capacity = remainingCapacity.getAndUpdate((x) -> x > 0 ? x - 1 : 0);
        return capacity > 0;
    }

    private void increaseRemainingCapacity() {
        remainingCapacity.incrementAndGet();
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
                increaseRemainingCapacity();
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
            remainingCapacity.set(maxSize);
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
        doLockSecured(() -> {
            int delta = maxSize - this.maxSize;
            remainingCapacity.getAndUpdate(x -> x + delta);
            // remaining capacity might be negative after this, but its up to ConnectionPool to remove some of the resources
            // before asking for new ones
            this.maxSize = maxSize;
        }, writeLock);
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
