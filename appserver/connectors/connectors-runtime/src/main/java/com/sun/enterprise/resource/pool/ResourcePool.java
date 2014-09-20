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
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.listener.PoolLifeCycleListener;
import com.sun.enterprise.resource.pool.waitqueue.PoolWaitQueue;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.resource.spi.RetryableUnavailableException;
import javax.transaction.Transaction;

/**
 * @author Tony Ng
 */
public interface ResourcePool {

    // start IASRI 4649256
    // Modify getResource() to throw PoolingException
    public ResourceHandle getResource(ResourceSpec spec,
                                      ResourceAllocator alloc,
                                      Transaction tran) throws PoolingException, RetryableUnavailableException;
    // end IASRI 4649256

    public void resourceClosed(ResourceHandle resource);

    public void resourceErrorOccurred(ResourceHandle resource);

    public void resourceEnlisted(Transaction tran, ResourceHandle resource);

    //Get status of pool
    public PoolStatus getPoolStatus();

    public void transactionCompleted(Transaction tran, int status);

    public void resizePool(boolean forced);

    // forcefully destroy all connections in the pool even if
    // connections have transactions in progress
    public void emptyPool();


    //reconfig the pool's properties
    public void reconfigurePool(ConnectorConnectionPool ccp)
            throws PoolingException;


    //cancel the resizer task in the pool
    public void cancelResizerTask();

    public void switchOnMatching();

    public PoolInfo getPoolInfo();

    public void emptyFreeConnectionsInPool();

    //accessors for self mgmt
    /**
     * Gets the max-pool-size attribute of this pool. Envisaged to be
     * used by the Self management framework to query the pool size
     * attribute for tweaking it using the setMaxPoolSize method
     *
     * @return max-pool-size value for this pool
     * @see setMaxPoolSize
     */
    public int getMaxPoolSize();

    /**
     * Gets the steady-pool-size attribute of this pool. Envisaged to be
     * used by the Self management framework to query the pool size
     * attribute for tweaking it using the setSteadyPoolSize method
     *
     * @return steady-pool-size value for this pool
     * @see setSteadyPoolSize
     */
    public int getSteadyPoolSize();

    //mutators for self mgmt
    /**
     * Sets the max-pool-size value for this pool. This attribute is
     * expected to be set by the self-management framework for an optimum
     * max-pool-size. The corresponding accessor gets this value.
     *
     * @param size - The new max-pool-size value
     * @see getMaxPoolSize
     */
    public void setMaxPoolSize(int size);

    /**
     * Sets the steady-pool-size value for this pool. This attribute is
     * expected to be set by the self-management framework for an optimum
     * steady-pool-size. The corresponding accessor gets this value.
     *
     * @param size - The new steady-pool-size value
     * @see getSteadyPoolSize
     */
    public void setSteadyPoolSize(int size);

    /**
     * Sets/Resets the flag indicating if this pool is self managed.
     * This method would be typically called by the self management
     * framework to indicate to the world (and this pool) that this
     * pool is self managed. Its very important that the self mgmt
     * framework properly sets this flag to control the dynamic reconfig
     * behavior of this pool. If this flag is set to true, all dynamic
     * reconfigs affecting the max/steady pool size of this pool will
     * be ignored.
     *
     * @param selfManaged - true to switch on self management, false otherwise
     */
    public void setSelfManaged(boolean selfManaged);

    /**
     * set pool life cycle listener
     * @param listener
     */
    public void setPoolLifeCycleListener(PoolLifeCycleListener listener);

    /**
     * remove pool life cycle listener
     */
    public void removePoolLifeCycleListener();

    /**
     * Flush Connection pool by reinitializing the connections 
     * established in the pool.
     * @return boolean indicating whether flush operation was successful or not
     * @throws com.sun.appserv.connectors.internal.api.PoolingException
     */
    public boolean flushConnectionPool() throws PoolingException;

    /**
     * block any new requests to the pool
     * Used for transparent dynamic reconfiguration of the pool
     * @param waitTimeout time for which the new requests will wait
     */
    public void blockRequests(long waitTimeout);

    /**
     * returns pool-wait-queue
     * @return wait-queue
     */
    public PoolWaitQueue getPoolWaitQueue();

    /**
     * returns wait-queue used during transparent dynamic reconfiguration
     * @return PoolWaitQueue
     */
    public PoolWaitQueue getReconfigWaitQueue();

    /**
     * returns the reconfig-wait-time
     * @return long
     */
    public long getReconfigWaitTime();
}


