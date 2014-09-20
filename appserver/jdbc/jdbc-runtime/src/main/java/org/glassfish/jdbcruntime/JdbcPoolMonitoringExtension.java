/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jdbcruntime;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.connectors.ConnectionPoolMonitoringExtension;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.pool.PoolLifeCycleListenerRegistry;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.resource.pool.monitor.ConnectionPoolAppProbeProvider;
import com.sun.enterprise.resource.pool.monitor.ConnectionPoolProbeProviderUtil;
import com.sun.enterprise.resource.pool.monitor.ConnectionPoolStatsProviderBootstrap;
import com.sun.logging.LogDomains;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.jdbc.pool.monitor.JdbcConnPoolAppProbeProvider;
import org.glassfish.jdbc.pool.monitor.JdbcConnPoolAppStatsProvider;
import org.glassfish.jdbc.pool.monitor.JdbcConnPoolProbeProvider;
import org.glassfish.jdbc.pool.monitor.JdbcConnPoolStatsProvider;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Shalini M
 */
@Service
public class JdbcPoolMonitoringExtension implements ConnectionPoolMonitoringExtension {


    @Inject
    private Provider<ConnectionPoolProbeProviderUtil> connectionPoolProbeProviderUtilProvider;

    @Inject
    private Provider<ConnectionPoolStatsProviderBootstrap> connectionPoolStatsProviderBootstrapProvider;

    @Inject
    private PoolManager poolManager;

    private ConnectorRuntime runtime;

    private final static Logger logger =
        LogDomains.getLogger(JdbcPoolMonitoringExtension.class, LogDomains.RSR_LOGGER);

    //List of all jdbc pool stats providers that are created and stored.
    private List<JdbcConnPoolStatsProvider> jdbcStatsProviders = null;
    private List<JdbcConnPoolAppStatsProvider> jdbcPoolAppStatsProviders = null;

    public JdbcPoolMonitoringExtension() {
        jdbcStatsProviders = new ArrayList<JdbcConnPoolStatsProvider>();
        jdbcPoolAppStatsProviders = new ArrayList<JdbcConnPoolAppStatsProvider>();
        runtime = ConnectorRuntime.getRuntime();
    }

    public ConnectionPoolProbeProviderUtil getProbeProviderUtil(){
        return connectionPoolProbeProviderUtilProvider.get();
    }

    /**
     * Register jdbc connection pool to the StatsProviderManager.
     * Add the pool lifecycle listeners for the pool to receive events on
     * change of any of the monitoring attribute values.
     * Finally, add this provider to the list of jdbc providers maintained.
     * @param poolInfo
     */
    public void registerPool(PoolInfo poolInfo) {
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
            PoolLifeCycleListenerRegistry registry =
                    connectionPoolStatsProviderBootstrapProvider.get().registerPool(
                            poolInfo,
                            getProbeProviderUtil().getJdbcProbeProvider());
            jdbcPoolStatsProvider.setPoolRegistry(registry);
            jdbcStatsProviders.add(jdbcPoolStatsProvider);
        }
    }

    /**
     * Unregister Jdbc Connection pool from the StatsProviderManager.
     * Remove the pool lifecycle listeners associated with this pool.
     * @param poolInfo
     */
    public void unregisterPool(PoolInfo poolInfo) {
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
        connectionPoolStatsProviderBootstrapProvider.get().postUnregisterPool(poolInfo);
    }

    /**
     * Register the jdbc connection pool Stats Provider object to the
     * monitoring framework under the specific application name monitoring
     * sub tree.
     *
     * @param appName
     * @return
     */
    public ConnectionPoolAppProbeProvider registerConnectionPool(
            PoolInfo poolInfo, String appName) {
        ConnectionPoolAppProbeProvider probeAppProvider = null;
        ResourcePool pool = runtime.getConnectionPoolConfig(poolInfo);
        if (pool instanceof JdbcConnectionPool) {
            probeAppProvider = new JdbcConnPoolAppProbeProvider();
            JdbcConnPoolAppStatsProvider jdbcPoolAppStatsProvider =
                    new JdbcConnPoolAppStatsProvider(poolInfo, appName);
            StatsProviderManager.register(
                    "jdbc-connection-pool",
                    PluginPoint.SERVER,
                    "resources/" + ConnectorsUtil.escapeResourceNameForMonitoring(
                            poolInfo.getName()) + "/" + appName,
                    jdbcPoolAppStatsProvider);
            jdbcPoolAppStatsProviders.add(jdbcPoolAppStatsProvider);
        }
        return probeAppProvider;
    }

    /**
     * Unregister the AppStatsProviders registered for this connection pool.
     */
    public void unRegisterConnectionPool() {
        Iterator jdbcProviders = jdbcPoolAppStatsProviders.iterator();
        while (jdbcProviders.hasNext()) {
            JdbcConnPoolAppStatsProvider jdbcPoolAppStatsProvider =
                    (JdbcConnPoolAppStatsProvider) jdbcProviders.next();
            StatsProviderManager.unregister(jdbcPoolAppStatsProvider);
        }
        jdbcPoolAppStatsProviders.clear();
    }

    public JdbcConnPoolProbeProvider createProbeProvider() {
        return new JdbcConnPoolProbeProvider();
    }



}
