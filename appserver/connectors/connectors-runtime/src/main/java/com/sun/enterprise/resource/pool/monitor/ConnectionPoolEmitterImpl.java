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

package com.sun.enterprise.resource.pool.monitor;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.listener.PoolLifeCycleListener;
import com.sun.logging.LogDomains;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of PoolLifeCycleListener interface to listen to events related
 * to jdbc monitoring. The methods invoke the probe providers internally to 
 * provide the monitoring related information.
 * 
 * @author Shalini M
 */
public class ConnectionPoolEmitterImpl implements PoolLifeCycleListener {
    private String poolName;
    private String appName;
    private String moduleName;
    private PoolInfo poolInfo;
    private ConnectionPoolProbeProvider poolProbeProvider;
    //Map of app names and respective emitters for a pool.
    private Map<PoolInfo, Map<String, ConnectionPoolAppEmitterImpl>> appStatsMap = null;
    //Map of app names for a resource handle id
    private Map<Long, String> resourceAppAssociationMap;
    private static Logger _logger = LogDomains.getLogger(ConnectionPoolEmitterImpl.class,
            LogDomains.RSR_LOGGER);
    private List<ConnectorConnPoolAppStatsProvider> ccPoolAppStatsProviders = null;
    private ConnectorRuntime runtime;

    //keep a static reference to InitialContext so as to avoid performance issues.
    private volatile static InitialContext ic = null;

    /**
     * Constructor
     * @param poolInfo connection pool on whose behalf this emitter emits pool related
     * probe events
     * @param provider
     */
    public ConnectionPoolEmitterImpl(PoolInfo poolInfo, ConnectionPoolProbeProvider provider) {
        this.poolInfo = poolInfo;
        this.poolName = poolInfo.getName();
        this.appName = poolInfo.getApplicationName();
        this.moduleName = poolInfo.getModuleName();
        this.poolProbeProvider = provider;
        this.ccPoolAppStatsProviders = new ArrayList<ConnectorConnPoolAppStatsProvider>();
        this.appStatsMap = new HashMap<PoolInfo, Map<String, ConnectionPoolAppEmitterImpl>>();
	this.resourceAppAssociationMap = new ConcurrentHashMap<Long, String>();	
        runtime = ConnectorRuntime.getRuntime();
        if (ic == null) {
            synchronized (ConnectionPoolEmitterImpl.class) {
                if(ic == null) {
                    try{
                        ic = new InitialContext();
                    } catch (NamingException e) {
                        //ignore
                    }
                }
            }
        }
    }

    /**
     * Fires probe event that a stack trace is to be printed on the server.log.
     * The stack trace is mainly related to connection leak tracing for the 
     * given jdbc connection pool.
     * @param stackTrace
     */
    public void toString(StringBuffer stackTrace) {
        stackTrace.append("\n Monitoring Statistics for \n" + poolName);
        poolProbeProvider.toString(poolName, appName, moduleName, stackTrace);
    }
    
    /**
     * Fires probe event that a connection has been acquired by the application 
     * for the given jdbc connection pool.
     */
    public void connectionAcquired(long resourceHandleId) {
        ConnectionPoolAppEmitterImpl appEmitter =
                detectAppBasedProviders(getAppName(resourceHandleId));
        poolProbeProvider.connectionAcquiredEvent(poolName, appName, moduleName);
        if(appEmitter != null) {
            appEmitter.connectionAcquired();
        }
    }

    /**
     * Fires probe event related to the fact that a connection request is served
     * in the time <code>timeTakenInMillis</code> for the given jdbc connection 
     * pool.
     * 
     * @param timeTakenInMillis time taken to serve a connection
     */    
    public void connectionRequestServed(long timeTakenInMillis) {
        poolProbeProvider.connectionRequestServedEvent(poolName, appName, moduleName, timeTakenInMillis);
    }

    /**
     * Fires probe event related to the fact that the given jdbc connection pool
     * has got a connection timed-out event.
     */
    public void connectionTimedOut() {
        poolProbeProvider.connectionTimedOutEvent(poolName, appName, moduleName);
    }

    /**
     * Fires probe event that a connection under test does not match the 
     * current request for the given jdbc connection pool.
     */
    public void connectionNotMatched() {
        poolProbeProvider.connectionNotMatchedEvent(poolName, appName, moduleName);        
    }

    /**
     * Fires probe event that a connection under test matches the current
     * request for the given jdbc connection pool.
     */
    public void connectionMatched() {
        poolProbeProvider.connectionMatchedEvent(poolName, appName, moduleName);        
    }

    /**
     * Fires probe event that a connection is destroyed for the 
     * given jdbc connection pool.
     */
    public void connectionDestroyed(long resourceHandleId) {
        poolProbeProvider.connectionDestroyedEvent(poolName, appName, moduleName);
        // Clearing the resource handle id appName mappings stored
        // This is useful in cases where connection-leak-reclaim is ON where we destroy
        // the connection. In this case, connection-release would not have happened.
        resourceAppAssociationMap.remove(resourceHandleId);
    }

    /**
     * Fires probe event that a connection is released for the given jdbc
     * connection pool.
     */
    public void connectionReleased(long resourceHandleId) {
        ConnectionPoolAppEmitterImpl appEmitter =
                detectAppBasedProviders(getAppName(resourceHandleId));
        poolProbeProvider.connectionReleasedEvent(poolName, appName, moduleName);
        if(appEmitter != null) {
            appEmitter.connectionReleased();
        }
        // Clearing the resource handle id appName mappings stored
        resourceAppAssociationMap.remove(resourceHandleId);
    }

    /**
     * Fires probe event that a connection is created for the given jdbc
     * connection pool.
     */
    public void connectionCreated() {
        poolProbeProvider.connectionCreatedEvent(poolName, appName, moduleName);
    }
    
    /**
     * Fires probe event related to the fact that the given jdbc connection pool
     * has got a connection leak event.
     *
     */
    public void foundPotentialConnectionLeak() {
        poolProbeProvider.potentialConnLeakEvent(poolName, appName, moduleName);
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool has
     * got a connection validation failed event.
     * 
     * @param count number of times the validation failed
     */
    public void connectionValidationFailed(int count) {
        poolProbeProvider.connectionValidationFailedEvent(poolName, appName, moduleName, count);
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool has
     * got a connection used event.
     */
    public void connectionUsed(long resourceHandleId) {
        ConnectionPoolAppEmitterImpl appEmitter =
                detectAppBasedProviders(getAppName(resourceHandleId));
        poolProbeProvider.connectionUsedEvent(poolName, appName, moduleName);
        if (appEmitter != null) {
            appEmitter.connectionUsed();
        }
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool has
     * got a connection freed event.
     * 
     * @param count number of connections freed to pool
     */
    public void connectionsFreed(int count) {
        poolProbeProvider.connectionsFreedEvent(poolName, appName, moduleName, count);
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool has
     * got a decrement connection used event.
     * 
     */
    public void decrementConnectionUsed(long resourceHandleId) {
        ConnectionPoolAppEmitterImpl appEmitter =
                detectAppBasedProviders(getAppName(resourceHandleId));
        poolProbeProvider.decrementConnectionUsedEvent(poolName, appName, moduleName);
        if(appEmitter != null) {
            appEmitter.decrementConnectionUsed();
        }
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool has
     * got a decrement free connections size event.
     * 
     */
    public void decrementNumConnFree() {
        poolProbeProvider.decrementNumConnFreeEvent(poolName, appName, moduleName);
    }
    
    /**
     * Fires probe event related to the fact the given jdbc connection pool has
     * got a decrement free connections size event.
     * 
     * @param beingDestroyed if the connection is destroyed due to error
     * @param steadyPoolSize 
     */
    public void incrementNumConnFree(boolean beingDestroyed, int steadyPoolSize) {
        poolProbeProvider.incrementNumConnFreeEvent(poolName, appName, moduleName, beingDestroyed, steadyPoolSize);
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool's 
     * wait queue length has been incremented
     * 
     */
    public void connectionRequestQueued() {
        poolProbeProvider.connectionRequestQueuedEvent(poolName, appName, moduleName);
    }

    /**
     * Fires probe event related to the fact the given jdbc connection pool's 
     * wait queue length has been decremented.
     * 
     */
    public void connectionRequestDequeued() {
        poolProbeProvider.connectionRequestDequeuedEvent(poolName, appName, moduleName);
    }

    private String getAppName(long resourceHandleId) {

        // if monitoring is disabled, avoid sending events
        // as we need to do "java:app/AppName" to get applicationName for each
        // acquire/return connection call which is a performance bottleneck.
        if(!runtime.isJdbcPoolMonitoringEnabled() && !runtime.isConnectorPoolMonitoringEnabled()){
            return null;
        }

        String appName = resourceAppAssociationMap.get(resourceHandleId);
        if(appName == null){
            try {
                if(ic == null){
                    synchronized(ConnectionPoolEmitterImpl.class) {
                        if(ic == null) {
                            ic = new InitialContext();
                        }
                    }
                }
                appName = (String) ic.lookup("java:app/AppName");
                resourceAppAssociationMap.put(resourceHandleId, appName);
            } catch (NamingException ex) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Unable to get application name using "
                            + "java:app/AppName method");
                }

            }
        }
        return appName;
    }

    /**
     * Detect if a Stats Provider has already been registered to the
     * monitoring framework for this appName and if so, return the specific
     * emitter. If not already registered, create and register the
     * Stats Provider object to the monitoring framework and add to the list
     * of emitters.
     *
     * @param appName
     * @return
     */
    private ConnectionPoolAppEmitterImpl detectAppBasedProviders(String appName) {

        ConnectionPoolAppProbeProvider probeAppProvider = null;
        ConnectionPoolAppEmitterImpl connPoolAppEmitter = null;

        if (appName == null) {
            //Case when appname cannot be detected. Emitter cannot exist for
            //a null appName for any pool.
            return null;
        }

        if (appStatsMap.containsKey(poolInfo)) {
            //Some apps have been registered for this pool.
            //Find if this appName is already registered.
            //All appEmitters for this pool
            Map<String, ConnectionPoolAppEmitterImpl> appEmitters = appStatsMap.get(poolInfo);
            //Check if the appEmitters list has an emitter for the appName.
            ConnectionPoolAppEmitterImpl emitter = appEmitters.get(appName);
            if(emitter != null) {
                //This appName has already been registered to StatsProviderManager
                return emitter;
            } else {
                if (!ConnectorsUtil.isApplicationScopedResource(poolInfo)) {
                    //register to the StatsProviderManager and add to the list.
                    probeAppProvider = registerConnectionPool(appName);
                    connPoolAppEmitter = addToList(appName, probeAppProvider,
                            appEmitters);
                }
            }
        } else {
            if (!ConnectorsUtil.isApplicationScopedResource(poolInfo)) {
                //Does not contain any app providers associated with this poolname
                //Create a map of app emitters for the appName and add them to the
                //appStatsMap
                probeAppProvider = registerConnectionPool(appName);
                Map<String, ConnectionPoolAppEmitterImpl> appEmitters =
                        new HashMap<String, ConnectionPoolAppEmitterImpl>();
                connPoolAppEmitter = addToList(appName, probeAppProvider, appEmitters);
            }
        }
        return connPoolAppEmitter;
    }

    /**
     * Register the jdbc/connector connection pool Stats Provider object to the
     * monitoring framework under the specific application name monitoring
     * sub tree.
     *
     * @param appName
     * @return
     */
    private ConnectionPoolAppProbeProvider registerConnectionPool(String appName) {
        ResourcePool pool = runtime.getConnectionPoolConfig(poolInfo);
        ConnectionPoolAppProbeProvider probeAppProvider =
                runtime.getProbeProviderUtil().getConnPoolBootstrap().registerPool(poolInfo, appName);
        if (pool instanceof ConnectorConnectionPool) {
            probeAppProvider = new ConnectorConnPoolAppProbeProvider();
            ConnectorConnPoolAppStatsProvider ccPoolAppStatsProvider =
                    new ConnectorConnPoolAppStatsProvider(poolInfo, appName);
            StatsProviderManager.register(
                    "connector-connection-pool",
                    PluginPoint.SERVER,
                    "resources/" + ConnectorsUtil.escapeResourceNameForMonitoring(poolName) + "/" + appName,
                    ccPoolAppStatsProvider);
            ccPoolAppStatsProviders.add(ccPoolAppStatsProvider);
        }
        return probeAppProvider;
    }

    /**
     * Add to the pool emitters list. the connection pool application emitter
     * for the specific poolInfo and appName.
     * @param appName
     * @param probeAppProvider
     * @param appEmitters
     * @return
     */
    private ConnectionPoolAppEmitterImpl addToList(String appName,
            ConnectionPoolAppProbeProvider probeAppProvider,
            Map<String, ConnectionPoolAppEmitterImpl> appEmitters) {
        ConnectionPoolAppEmitterImpl connPoolAppEmitter = null;
        if (probeAppProvider != null) {
            //Add the newly created probe provider to the list.
            connPoolAppEmitter = new ConnectionPoolAppEmitterImpl(poolName,
                    appName, probeAppProvider);
            //NOTE : this appName here is different from "appName" instance variable.
            appEmitters.put(appName, connPoolAppEmitter);
            appStatsMap.put(poolInfo, appEmitters);
        }
        runtime.getProbeProviderUtil().
                getConnPoolBootstrap().addToPoolEmitters(poolInfo, this);
        return connPoolAppEmitter;
    }

    /**
     * Unregister the AppStatsProviders registered for this connection pool.
     */
    public void unregisterAppStatsProviders() {
        runtime.getProbeProviderUtil().getConnPoolBootstrap().unRegisterPool();
        Iterator ccProviders = ccPoolAppStatsProviders.iterator();
        while (ccProviders.hasNext()) {
            ConnectorConnPoolAppStatsProvider ccPoolAppStatsProvider =
                    (ConnectorConnPoolAppStatsProvider) ccProviders.next();
            StatsProviderManager.unregister(ccPoolAppStatsProvider);
        }
        ccPoolAppStatsProviders.clear();
    }
}
