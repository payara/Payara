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

import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.listener.PoolLifeCycleListener;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of PoolLifeCycleListener to listen to events related to a 
 * connection pool. The registry allows multiple listeners (ex: pool monitoring)
 * to listen to the pool's lifecyle. Maintains a list of listeners for this pool
 * identified by poolName.
 * 
 * @author Shalini M
 */
public class PoolLifeCycleListenerRegistry implements PoolLifeCycleListener {

    //List of listeners 
    protected List<PoolLifeCycleListener> poolListenersList;
    
    //name of the pool for which the registry is maintained
    private PoolInfo poolInfo;

    public PoolLifeCycleListenerRegistry(PoolInfo poolInfo) {
        this.poolInfo = poolInfo;
        poolListenersList = new ArrayList<PoolLifeCycleListener>();
    }

    /**
     * Add a listener to the list of pool life cycle listeners maintained by 
     * this registry.
     * @param listener
     */
    public void registerPoolLifeCycleListener(PoolLifeCycleListener listener) {
        poolListenersList.add(listener);
        
        //Check if poolLifeCycleListener has already been set to this. There
        //could be multiple listeners.
        if(!(poolListenersList.size() > 1)) {
            //If the pool is already created, set this registry object to the pool.
            PoolManager poolMgr = ConnectorRuntime.getRuntime().getPoolManager();
            ResourcePool pool = poolMgr.getPool(poolInfo);
            pool.setPoolLifeCycleListener(this);
        }
    }

    /**
     * Clear the list of pool lifecycle listeners maintained by the registry.
     * This happens when a pool is destroyed so the information about its 
     * listeners need not be stored.
     * @param poolName
     */
    public void unRegisterPoolLifeCycleListener(PoolInfo poolInfo) {
        //To make sure the registry is for the given pool name
        if (this.poolInfo.equals(poolInfo)) {
            if (poolListenersList != null && !poolListenersList.isEmpty()) {
                //Remove all listeners from this list
                poolListenersList.clear();
            }
        }
        //Its not needed to remove pool life cycle listener from the pool since
        //the pool will already be destroyed.
    }

    public void toString(StringBuffer stackTrace) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.toString(stackTrace);
        }
    }

    public void connectionAcquired(long resourceHandleId) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionAcquired(resourceHandleId);
        }
    }

    public void connectionRequestServed(long timeTakenInMillis) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionRequestServed(timeTakenInMillis);
        }
    }

    public void connectionTimedOut() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionTimedOut();
        }
    }

    public void connectionNotMatched() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionNotMatched();
        }
    }

    public void connectionMatched() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionMatched();
        }
    }

    public void connectionUsed(long resourceHandleId) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionUsed(resourceHandleId);
        }
    }

    public void connectionDestroyed(long resourceHandleId) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionDestroyed(resourceHandleId);
        }
    }

    public void connectionReleased(long resourceHandleId) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionReleased(resourceHandleId);
        }
    }

    public void connectionCreated() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionCreated();
        }
    }

    public void foundPotentialConnectionLeak() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.foundPotentialConnectionLeak();
        }
    }

    public void connectionValidationFailed(int count) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionValidationFailed(count);
        }
    }

    public void connectionsFreed(int count) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionsFreed(count);
        }
    }

    public void decrementConnectionUsed(long resourceHandleId) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.decrementConnectionUsed(resourceHandleId);
        }
    }

    public void decrementNumConnFree() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.decrementNumConnFree();
        }
    }
    
    public void incrementNumConnFree(boolean beingDestroyed, int steadyPoolSize) {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.incrementNumConnFree(beingDestroyed, steadyPoolSize);
        }
    }

    public void connectionRequestQueued() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionRequestQueued();
        }        
    }

    public void connectionRequestDequeued() {
        for (PoolLifeCycleListener listener : poolListenersList) {
            listener.connectionRequestDequeued();
        }
    }
}
