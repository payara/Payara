/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.logging.LogDomains;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.listener.PoolLifeCycle;
import com.sun.enterprise.resource.pool.PoolLifeCycleListenerRegistry;
import com.sun.enterprise.resource.pool.PoolLifeCycleRegistry;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;

import java.util.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.connectors.config.JdbcConnectionPool;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.resources.api.PoolInfo;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.Singleton;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.jvnet.hk2.component.Habitat;

/**
 * Bootstrap operations of stats provider objects are done by this class.
 * Registering of provider to the StatsProviderManager, adding pools to the 
 * PoolLifeCycle listeners are done during the bootstrap. 
 * Depending on the lifecycle of the pool - creation/destroy, the listeners
 * are added or removed and providers registered/unregistered.
 * 
 * This is an implementation of PoolLifeCycle. All pool creation or destroy 
 * events are got and based on the type, provider is registered for a pool if pool 
 * is created or provider is unregistered if pool is destroyed. Monitoring 
 * levels when changed from HIGH-> OFF or
 * OFF->HIGH are taken care and appropriate monitoring levels are set.
 * 
 * @author Shalini M
 */
@Service
@Scoped(Singleton.class)
public class ConnectionPoolStatsProviderBootstrap implements PostConstruct, 
        PoolLifeCycle {

    protected final static Logger logger =
    LogDomains.getLogger(ConnectionPoolStatsProviderBootstrap.class,LogDomains.RSR_LOGGER);

    @Inject
    private PoolManager poolManager;

    @Inject
    private Habitat habitat;
    
    //List of all jdbc pool stats providers that are created and stored.
    private List<JdbcConnPoolStatsProvider> jdbcStatsProviders = null;
    
    //List of all connector conn pool stats providers that are created and stored
    private List<ConnectorConnPoolStatsProvider> ccStatsProviders = null;

    //Map of all ConnectionPoolEmitterImpl(s) for different pools
    private Map<PoolInfo, ConnectionPoolEmitterImpl> poolEmitters = null;
    private Map<PoolInfo, PoolLifeCycleListenerRegistry> poolRegistries = null;
    private ConnectorRuntime runtime;

    public ConnectionPoolStatsProviderBootstrap() {
        jdbcStatsProviders = new ArrayList<JdbcConnPoolStatsProvider>();
        ccStatsProviders = new ArrayList<ConnectorConnPoolStatsProvider>();
        poolEmitters = new HashMap<PoolInfo, ConnectionPoolEmitterImpl>();
        poolRegistries = new HashMap<PoolInfo, PoolLifeCycleListenerRegistry>();
        runtime = ConnectorRuntime.getRuntime();
        
    }

    public void addToPoolEmitters(PoolInfo poolInfo, ConnectionPoolEmitterImpl emitter) {
        poolEmitters.put(poolInfo, emitter);
    }

    /**
     * All Jdbc Connection pools are added to the pool life cycle listener so as
     * to listen to creation/destroy events. If the JdbcPoolTree is not built, 
     * by registering to the StatsProviderManager, its is done here.
     */
    public void registerProvider() {
        registerPoolLifeCycleListener();
    }
    
    public void postConstruct() {
        if(logger.isLoggable(Level.FINEST)) {
            logger.finest("[Monitor]In the JDBCPoolStatsProviderBootstrap");
        }

       //createMonitoringConfig();
    }

    /**
     * Registers the pool lifecycle listener for this pool by creating a 
     * new ConnectionPoolEmitterImpl object for this pool.
     * @param poolInfo
     * @return registry of pool lifecycle listeners
     */
    private PoolLifeCycleListenerRegistry registerPool(PoolInfo poolInfo,
            ConnectionPoolProbeProvider poolProvider) {
        PoolLifeCycleListenerRegistry poolRegistry = null;
        if(poolRegistries.get(poolInfo)==null){
            poolRegistry =
                new PoolLifeCycleListenerRegistry(poolInfo);
            poolRegistries.put(poolInfo, poolRegistry);
        }else{
            poolRegistry = poolRegistries.get(poolInfo);
        }
        ConnectionPoolEmitterImpl emitter =
                new com.sun.enterprise.resource.pool.monitor.ConnectionPoolEmitterImpl(
                poolInfo, poolProvider);
        poolRegistry.registerPoolLifeCycleListener(emitter);
        addToPoolEmitters(poolInfo, emitter);
        return poolRegistry;
    }

    public Resources getResources(){
        return habitat.getComponent(Domain.class).getResources();
    }

    public ConnectionPoolProbeProviderUtil getProbeProviderUtil(){
        return habitat.getComponent(ConnectionPoolProbeProviderUtil.class);
    }

    /**
     * Register jdbc connection pool to the StatsProviderManager. 
     * Add the pool lifecycle listeners for the pool to receive events on 
     * change of any of the monitoring attribute values. 
     * Finally, add this provider to the list of jdbc providers maintained.
     * @param poolInfo
     */
    private void registerJdbcPool(PoolInfo poolInfo) {
        if(poolManager.getPool(poolInfo) != null) {
            getProbeProviderUtil().createJdbcProbeProvider();
            //Found in the pool table (pool has been initialized/created)
            JdbcConnPoolStatsProvider jdbcPoolStatsProvider =
                    new JdbcConnPoolStatsProvider(poolInfo, logger);
            StatsProviderManager.register(
                    "jdbc-connection-pool",
                    PluginPoint.SERVER,
                    ConnectorsUtil.getPoolMonitoringSubTreeRoot(poolInfo, true), jdbcPoolStatsProvider);
            //String jdbcPoolName = jdbcPoolStatsProvider.getJdbcPoolName();
            PoolLifeCycleListenerRegistry registry = registerPool(poolInfo, 
                    getProbeProviderUtil().getJdbcProbeProvider());
            jdbcPoolStatsProvider.setPoolRegistry(registry);
            jdbcStatsProviders.add(jdbcPoolStatsProvider);
        }
    }
    
    /**
     * Register connector connection pool to the StatsProviderManager. 
     * Add the pool lifecycle listeners for the pool to receive events on 
     * change of any of the monitoring attribute values. 
     * Finally, add this provider to the list of connector connection pool 
     * providers maintained.
     * @param poolInfo
     */
    private void registerCcPool(PoolInfo poolInfo) {
        if(poolManager.getPool(poolInfo) != null) {
            getProbeProviderUtil().createJcaProbeProvider();
            //Found in the pool table (pool has been initialized/created)
            ConnectorConnPoolStatsProvider ccPoolStatsProvider =
                    new ConnectorConnPoolStatsProvider(poolInfo, logger);

            StatsProviderManager.register(
                    "connector-connection-pool",
                    PluginPoint.SERVER,
                    ConnectorsUtil.getPoolMonitoringSubTreeRoot(poolInfo, true), ccPoolStatsProvider);

            PoolLifeCycleListenerRegistry registry = registerPool(
                    poolInfo, getProbeProviderUtil().getJcaProbeProvider());
            ccPoolStatsProvider.setPoolRegistry(registry);
            
            ccStatsProviders.add(ccPoolStatsProvider);

            if(!ConnectorsUtil.isApplicationScopedResource(poolInfo)){
                ResourcesUtil resourcesUtil = ResourcesUtil.createInstance();
                ResourcePool pool = resourcesUtil.getPoolConfig(poolInfo);
                Resources resources = resourcesUtil.getResources(poolInfo);
                String raName = resourcesUtil.getRarNameOfResource(pool, resources);

                ConnectorConnPoolStatsProvider connectorServicePoolStatsProvider =
                        new ConnectorConnPoolStatsProvider(poolInfo, logger);

                String dottedNamesHierarchy = null;
                String monitoringModuleName = null;

                if(ConnectorsUtil.isJMSRA(raName)){
                    monitoringModuleName =  ConnectorConstants.MONITORING_JMS_SERVICE_MODULE_NAME;
                    dottedNamesHierarchy = ConnectorConstants.MONITORING_JMS_SERVICE +
                        ConnectorConstants.MONITORING_SEPARATOR + ConnectorConstants.MONITORING_CONNECTION_FACTORIES
                            + ConnectorConstants.MONITORING_SEPARATOR +
                            ConnectorsUtil.escapeResourceNameForMonitoring(poolInfo.getName());

                }else{
                    monitoringModuleName =  ConnectorConstants.MONITORING_CONNECTOR_SERVICE_MODULE_NAME;
                    dottedNamesHierarchy = ConnectorConstants.MONITORING_CONNECTOR_SERVICE_MODULE_NAME +
                    ConnectorConstants.MONITORING_SEPARATOR + raName + ConnectorConstants.MONITORING_SEPARATOR +
                    ConnectorsUtil.escapeResourceNameForMonitoring(poolInfo.getName());
                }

                StatsProviderManager.register(monitoringModuleName, PluginPoint.SERVER,
                    dottedNamesHierarchy, connectorServicePoolStatsProvider);

                if(logger.isLoggable(Level.FINE)){
                    logger.log(Level.FINE, "Registered pool-monitoring stats [ "+dottedNamesHierarchy+" ]  " +
                        "for [ " + raName + " ] with monitoring-stats-registry.");
                }

                /* no need to create multiple probe provider instances, one per pool will
                   work for multiple stats providers 
                PoolLifeCycleListenerRegistry poolLifeCycleListenerRegistry = registerPool(
                        poolInfo, getProbeProviderUtil().getJcaProbeProvider());
                */

                connectorServicePoolStatsProvider.setPoolRegistry(registry);
                ccStatsProviders.add(connectorServicePoolStatsProvider);
            }
        }
    }

    /**
     * Register <code> this </code> to PoolLifeCycleRegistry so as to listen to 
     * PoolLifeCycle events - pool creation or destroy.
     */
    private void registerPoolLifeCycleListener() {
        //Register provider only for server and not for clients
        if(runtime.isServer()) {
            PoolLifeCycleRegistry poolLifeCycleRegistry = PoolLifeCycleRegistry.getRegistry();
            poolLifeCycleRegistry.registerPoolLifeCycle(this);
        }
    }

    /**
     * Unregister Jdbc/Connector Connection pool from the StatsProviderManager.
     * Remove the pool lifecycle listeners associated with this pool.
     * @param poolInfo
     */
    private void unregisterPool(PoolInfo poolInfo) {
        if(jdbcStatsProviders != null) {
            Iterator i = jdbcStatsProviders.iterator();
            while (i.hasNext()) {
                JdbcConnPoolStatsProvider jdbcPoolStatsProvider = (JdbcConnPoolStatsProvider) i.next();
                if (poolInfo.equals(jdbcPoolStatsProvider.getPoolInfo())) {
                    //Get registry and unregister this pool from the registry
                    PoolLifeCycleListenerRegistry poolRegistry = jdbcPoolStatsProvider.getPoolRegistry();
                    poolRegistry.unRegisterPoolLifeCycleListener(poolInfo);
                    StatsProviderManager.unregister(jdbcPoolStatsProvider);

                    i.remove();
                }
            }
        }
        if(ccStatsProviders != null) {
            Iterator i = ccStatsProviders.iterator();
            while (i.hasNext()) {
                ConnectorConnPoolStatsProvider ccPoolStatsProvider = 
                        (ConnectorConnPoolStatsProvider) i.next();
                if (poolInfo.equals(ccPoolStatsProvider.getPoolInfo())) {
                    //Get registry and unregister this pool from the registry
                    PoolLifeCycleListenerRegistry poolRegistry = ccPoolStatsProvider.getPoolRegistry();
                    poolRegistry.unRegisterPoolLifeCycleListener(poolInfo);
                    StatsProviderManager.unregister(ccPoolStatsProvider);

                    i.remove();

                }
            }
        }
        unregisterPoolAppProviders(poolInfo);
        poolRegistries.remove(poolInfo);
    }

    public void unregisterPoolAppProviders(PoolInfo poolInfo) {
        ConnectionPoolEmitterImpl emitter = poolEmitters.get(poolInfo);
        //If an emitter was created for the poolInfo
        if (emitter != null) {
            emitter.unregisterAppStatsProviders();
        }
    }

    /**
     * Find if the monitoring is enabled based on the monitoring level : 
     * <code> strEnabled </code>
     * @param strEnabled
     * @return 
     */
    public boolean getEnabledValue(String strEnabled) {
        if ("OFF".equals(strEnabled)) {
            return false;
        }
        return true;
    }

    /**
     * When a pool is created (or initialized) the pool should be registered
     * to the  StatsProviderManager. Also, the pool lifecycle
     * listener needs to be registered for this pool to track events on change
     * of any monitoring attributes.
     * @param poolInfo
     */
    public void poolCreated(PoolInfo poolInfo) {
        if(logger.isLoggable(Level.FINEST)) {
            logger.finest("Pool created : " + poolInfo);
        }
        if(runtime.isServer()) {
            ResourcePool pool = runtime.getConnectionPoolConfig(poolInfo);
            if(pool instanceof JdbcConnectionPool) {
                registerJdbcPool(poolInfo);
            } else if (pool instanceof ConnectorConnectionPool) {
                registerCcPool(poolInfo);
            } /*else if (poolInfo.getName().contains(ConnectorConstants.DATASOURCE_DEFINITION_JNDINAME_PREFIX)){
                registerJdbcPool(poolInfo);
            }*/
        }
    }

    /**
     * When a pool is destroyed, the pool should be unregistered from the 
     * StatsProviderManager. Also, the pool's lifecycle listener
     * should be unregistered.
     * @param poolInfo
     */
    public void poolDestroyed(PoolInfo poolInfo) {
        if(logger.isLoggable(Level.FINEST)) {
            logger.finest("Pool Destroyed : " + poolInfo);
        }
        if (runtime.isServer()) {
            unregisterPool(poolInfo);
        }
    }

    /**
     * Creates jdbc-connection-pool, connector-connection-pool, connector-service
     * config elements for monitoring.
     */
    //private void createMonitoringConfig() {
    //   createMonitoringConfig(JDBC_CONNECTION_POOL, JdbcConnectionPoolMI.class);
    //   createMonitoringConfig(CONNECTOR_CONNECTION_POOL, ConnectorConnectionPoolMI.class);
    //}

    /**
     * Creates config elements for monitoring.
     *
     * Check if the monitoring config has been created.
     * If it has not, then add it.
     */
    /*private void createMonitoringConfig(final String name, final Class monitoringItemClass) {
        if (monitoringService == null) {
            logger.log(Level.SEVERE, "monitoringService is null. " +
                    "jdbc-connection-pool and connector-connection-pool monitoring config not created");
            return;
        }
        List<MonitoringItem> itemList = monitoringService.getMonitoringItems();
        boolean hasMonitorConfig = false;
        for (MonitoringItem mi : itemList) {
            if (mi.getName().equals(name)) {
                hasMonitorConfig = true;
            }
        }

        try {
            if (!hasMonitorConfig) {
                ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {

                    public Object run(MonitoringService param) throws PropertyVetoException, TransactionFailure {

                        MonitoringItem newItem = (MonitoringItem) param.createChild(monitoringItemClass);
                        newItem.setName(name);
                        newItem.setLevel(MonitoringItem.LEVEL_OFF);
                        param.getMonitoringItems().add(newItem);
                        return newItem;
                    }
                }, monitoringService);
            }
        } catch (TransactionFailure tfe) {
            logger.log(Level.SEVERE, "Exception adding " + name + " MonitoringItem", tfe);
        }
    }*/
}
