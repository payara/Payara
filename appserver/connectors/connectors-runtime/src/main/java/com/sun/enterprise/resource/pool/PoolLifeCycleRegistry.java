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
import com.sun.enterprise.resource.listener.PoolLifeCycle;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of PoolLifeCycle to listen to events related to a 
 * connection pool creation or destroy. The registry allows multiple listeners 
 * (ex: pool monitoring or self management)
 * to listen to the pool's lifecyle. Maintains a list of listeners for this pool
 * identified by poolName.
 * 
 * @author Shalini M
 */
@Singleton
public class PoolLifeCycleRegistry implements PoolLifeCycle {

    //List of listeners 
    protected List<PoolLifeCycle> lifeCycleListeners = new ArrayList<PoolLifeCycle>();
    private static PoolLifeCycleRegistry __poolLifeCycleRegistry = 
            new PoolLifeCycleRegistry();
    
    public PoolLifeCycleRegistry() {
    }

    public static PoolLifeCycleRegistry getRegistry() {
        if(__poolLifeCycleRegistry == null) {
            throw new RuntimeException("PoolLifeCycleRegistry not initialized");
        }
        return __poolLifeCycleRegistry;
    }
    /**
     * Add a listener to the list of pool life cycle listeners maintained by 
     * this registry for the <code>poolName</code>.
     * @param poolName
     * @param listener
     */
    public void registerPoolLifeCycle(PoolLifeCycle listener) {
        lifeCycleListeners.add(listener);
        
        //Check if lifecycleListeners has already been set to this. There
        //could be multiple listeners.
        if(!(lifeCycleListeners.size() > 1)) {
            //If the pool is already created, set this registry object to the pool.
            PoolManager poolMgr = ConnectorRuntime.getRuntime().getPoolManager();
            poolMgr.registerPoolLifeCycleListener(this);
        }
    }

    /**
     * Clear the list of pool lifecycle listeners maintained by the registry.
     * This happens when a pool is destroyed so the information about its 
     * listeners need not be stored.
     */
    public void unRegisterPoolLifeCycle(PoolLifeCycle listener) {
        if (lifeCycleListeners != null) {
            if(!lifeCycleListeners.isEmpty()) {
                lifeCycleListeners.remove(listener);
            } else {
                //TODO V3 : think about unregistering the registry?
            }
        }
    }
    
    /**
     * Invoke poolCreated for all listeners of this pool.
     * @param poolInfo
     */
    public void poolCreated(PoolInfo poolInfo) {
        for (PoolLifeCycle listener : lifeCycleListeners) {
            listener.poolCreated(poolInfo);
        }
    }

    /**
     * Invoke poolDestroyed for all listeners of this pool.
     * @param poolInfo
     */
    public void poolDestroyed(PoolInfo poolInfo) {
        for (PoolLifeCycle listener : lifeCycleListeners) {
            listener.poolDestroyed(poolInfo);
        }
    }
}
