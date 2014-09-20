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

package com.sun.enterprise.v3.services.impl.monitor;

import com.sun.enterprise.v3.services.impl.monitor.probes.ConnectionQueueProbeProvider;
import com.sun.enterprise.v3.services.impl.monitor.probes.FileCacheProbeProvider;             
import com.sun.enterprise.v3.services.impl.monitor.probes.KeepAliveProbeProvider;
import com.sun.enterprise.v3.services.impl.monitor.probes.ThreadPoolProbeProvider;
import com.sun.enterprise.v3.services.impl.monitor.stats.ConnectionQueueStatsProvider;
import com.sun.enterprise.v3.services.impl.monitor.stats.ConnectionQueueStatsProviderGlobal;
import com.sun.enterprise.v3.services.impl.monitor.stats.FileCacheStatsProvider;
import com.sun.enterprise.v3.services.impl.monitor.stats.FileCacheStatsProviderGlobal;
import com.sun.enterprise.v3.services.impl.monitor.stats.KeepAliveStatsProvider;
import com.sun.enterprise.v3.services.impl.monitor.stats.KeepAliveStatsProviderGlobal;
import com.sun.enterprise.v3.services.impl.monitor.stats.ThreadPoolStatsProvider;
import com.sun.enterprise.v3.services.impl.monitor.stats.ThreadPoolStatsProviderGlobal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;

/**
 * Grizzly monitoring manager, which is responsible for registering, unregistering
 * Grizzly statistics probes.
 * 
 * @author Alexey Stashok
 */
public class GrizzlyMonitoring {
    private static final String CONFIG_ELEMENT = "http-service";
    
    // network-listener->thread-pool-stats Map
    private final Map<String, ThreadPoolStatsProvider> threadPoolStatsProvidersMap =
            new ConcurrentHashMap<String, ThreadPoolStatsProvider>();
    // network-listener->file-cache-stats Map
    private final Map<String, FileCacheStatsProvider> fileCacheStatsProvidersMap =
            new ConcurrentHashMap<String, FileCacheStatsProvider>();
    // network-listener->keep-alive-stats Map
    private final Map<String, KeepAliveStatsProvider> keepAliveStatsProvidersMap =
            new ConcurrentHashMap<String, KeepAliveStatsProvider>();
    // network-listener->connection-queue-stats Map
    private final Map<String, ConnectionQueueStatsProvider> connectionQueueStatsProvidersMap =
            new ConcurrentHashMap<String, ConnectionQueueStatsProvider>();

    // thread-pool emitter probe
    private final ThreadPoolProbeProvider threadPoolProbeProvider;
    // file-cache emitter probe
    private final FileCacheProbeProvider fileCacheProbeProvider;
    // keep-alive emitter probe
    private final KeepAliveProbeProvider keepAliveProbeProvider;
    // connection queue emitter probe
    private final ConnectionQueueProbeProvider connectionQueueProbeProvider;
    
    public GrizzlyMonitoring() {
        threadPoolProbeProvider = new ThreadPoolProbeProvider();
        fileCacheProbeProvider = new FileCacheProbeProvider();
        keepAliveProbeProvider = new KeepAliveProbeProvider();
        connectionQueueProbeProvider = new ConnectionQueueProbeProvider();
    }

    /**
     * Get thread-pool probe provider
     * 
     * @return thread-pool probe provider
     */
    public ThreadPoolProbeProvider getThreadPoolProbeProvider() {
        return threadPoolProbeProvider;
    }

    /**
     * Get file-cache probe provider
     *
     * @return file-cache probe provider
     */
    public FileCacheProbeProvider getFileCacheProbeProvider() {
        return fileCacheProbeProvider;
    }

    /**
     * Get keep-alive probe provider
     *
     * @return keep-alive probe provider
     */
    public KeepAliveProbeProvider getKeepAliveProbeProvider() {
        return keepAliveProbeProvider;
    }

    /**
     * Get connection queue probe provider
     *
     * @return connection queue probe provider
     */
    public ConnectionQueueProbeProvider getConnectionQueueProbeProvider() {
        return connectionQueueProbeProvider;
    }

    /**
     * Register thread-pool statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void registerThreadPoolStatsProvider(String name) {
        ThreadPoolStatsProvider threadPoolStatsProvider = new ThreadPoolStatsProvider(name);
        ThreadPoolStatsProvider oldthreadPoolStatsProvider =
                threadPoolStatsProvidersMap.put(name, threadPoolStatsProvider);

        if (oldthreadPoolStatsProvider != null) {
            StatsProviderManager.unregister(oldthreadPoolStatsProvider);
        }
        
        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/thread-pool", threadPoolStatsProvider);
    }

    /**
     * Unregister thread-pool statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void unregisterThreadPoolStatsProvider(String name) {
        final ThreadPoolStatsProvider threadPoolStatsProvider =
                threadPoolStatsProvidersMap.remove(name);
        if (threadPoolStatsProvider != null) {
            StatsProviderManager.unregister(threadPoolStatsProvider);
        }
    }

    /**
     * Register keep-alive statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void registerKeepAliveStatsProvider(String name) {
        KeepAliveStatsProvider keepAliveStatsProvider = new KeepAliveStatsProvider(name);
        KeepAliveStatsProvider oldKeepAliveStatsProvider =
                keepAliveStatsProvidersMap.put(name, keepAliveStatsProvider);

        if (oldKeepAliveStatsProvider != null) {
            StatsProviderManager.unregister(oldKeepAliveStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/keep-alive", keepAliveStatsProvider);
    }

    /**
     * Unregister keep-alive statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void unregisterKeepAliveStatsProvider(String name) {
        final KeepAliveStatsProvider keepAliveStatsProvider =
                keepAliveStatsProvidersMap.remove(name);
        if (keepAliveStatsProvider != null) {
            StatsProviderManager.unregister(keepAliveStatsProvider);
        }
    }

    /**
     * Register file-cache statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void registerFileCacheStatsProvider(String name) {
        FileCacheStatsProvider fileCacheStatsProvider = new FileCacheStatsProvider(name);
        FileCacheStatsProvider oldFileCacheStatsProvider =
                fileCacheStatsProvidersMap.put(name, fileCacheStatsProvider);

        if (oldFileCacheStatsProvider != null) {
            StatsProviderManager.unregister(oldFileCacheStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/file-cache", fileCacheStatsProvider);
    }

    /**
     * Unregister file-cache statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void unregisterFileCacheStatsProvider(String name) {
        final FileCacheStatsProvider fileCacheStatsProvider =
                fileCacheStatsProvidersMap.remove(name);
        if (fileCacheStatsProvider != null) {
            StatsProviderManager.unregister(fileCacheStatsProvider);
        }
    }

    /**
     * Register connection queue statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void registerConnectionQueueStatsProvider(String name) {
        ConnectionQueueStatsProvider connectionQueueStatsProvider = new ConnectionQueueStatsProvider(name);
        ConnectionQueueStatsProvider oldConnectionQueueStatsProvider =
                connectionQueueStatsProvidersMap.put(name, connectionQueueStatsProvider);

        if (oldConnectionQueueStatsProvider != null) {
            StatsProviderManager.unregister(oldConnectionQueueStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/connection-queue", connectionQueueStatsProvider);
    }

    /**
     * Unregister connection queue statistics provider for a network listener
     *
     * @param name network listener name
     */
    public void unregisterConnectionQueueStatsProvider(String name) {
        final ConnectionQueueStatsProvider connectionQueueStatsProvider =
                connectionQueueStatsProvidersMap.remove(name);
        if (connectionQueueStatsProvider != null) {
            StatsProviderManager.unregister(connectionQueueStatsProvider);
        }
    }

    /**
     * Register server wide thread-pool statistics provider
     */
    public void registerThreadPoolStatsProviderGlobal(String name) {
        ThreadPoolStatsProvider threadPoolStatsProvider = new ThreadPoolStatsProviderGlobal(name);
        ThreadPoolStatsProvider oldthreadPoolStatsProvider =
                threadPoolStatsProvidersMap.put(name, threadPoolStatsProvider);

        if (oldthreadPoolStatsProvider != null) {
            StatsProviderManager.unregister(oldthreadPoolStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/thread-pool", threadPoolStatsProvider);
    }

    /**
     * Unregister server wide thread-pool statistics provider
     */
    public void unregisterThreadPoolStatsProviderGlobal(String name) {
        final ThreadPoolStatsProvider threadPoolStatsProvider =
                threadPoolStatsProvidersMap.remove(name);
        if (threadPoolStatsProvider != null) {
            StatsProviderManager.unregister(threadPoolStatsProvider);
        }
    }

    /**
     * Register server wide keep-alive statistics provider for a network listener
     */
    public void registerKeepAliveStatsProviderGlobal(String name) {
        KeepAliveStatsProvider keepAliveStatsProvider = new KeepAliveStatsProviderGlobal(name);
        KeepAliveStatsProvider oldKeepAliveStatsProvider =
                keepAliveStatsProvidersMap.put(name, keepAliveStatsProvider);

        if (oldKeepAliveStatsProvider != null) {
            StatsProviderManager.unregister(oldKeepAliveStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/keep-alive", keepAliveStatsProvider);
    }

    /**
     * Unregister server wide keep-alive statistics provider
     */
    public void unregisterKeepAliveStatsProviderGlobal(String name) {
        final KeepAliveStatsProvider keepAliveStatsProvider =
                keepAliveStatsProvidersMap.remove(name);
        if (keepAliveStatsProvider != null) {
            StatsProviderManager.unregister(keepAliveStatsProvider);
        }
    }

    /**
     * Register server wide file-cache statistics provider for a network listener
     */
    public void registerFileCacheStatsProviderGlobal(String name) {
        FileCacheStatsProvider fileCacheStatsProvider = new FileCacheStatsProviderGlobal(name);
        FileCacheStatsProvider oldFileCacheStatsProvider =
                fileCacheStatsProvidersMap.put(name, fileCacheStatsProvider);

        if (oldFileCacheStatsProvider != null) {
            StatsProviderManager.unregister(oldFileCacheStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/file-cache", fileCacheStatsProvider);
    }

    /**
     * Unregister serve wide file-cache statistics provider for a network listener
     */
    public void unregisterFileCacheStatsProviderGlobal(String name) {
        final FileCacheStatsProvider fileCacheStatsProvider =
                fileCacheStatsProvidersMap.remove(name);
        if (fileCacheStatsProvider != null) {
            StatsProviderManager.unregister(fileCacheStatsProvider);
        }
    }

    /**
     * Register server wide connection queue statistics provider for a network listener
     */
    public void registerConnectionQueueStatsProviderGlobal(String name) {
        ConnectionQueueStatsProvider connectionQueueStatsProvider = new ConnectionQueueStatsProviderGlobal(name);
        ConnectionQueueStatsProvider oldConnectionQueueStatsProvider =
                connectionQueueStatsProvidersMap.put(name, connectionQueueStatsProvider);

        if (oldConnectionQueueStatsProvider != null) {
            StatsProviderManager.unregister(oldConnectionQueueStatsProvider);
        }

        StatsProviderManager.register(CONFIG_ELEMENT, PluginPoint.SERVER,
                subtreePrefix(name) + "/connection-queue", connectionQueueStatsProvider);
    }

    /**
     * Unregister server wide connection queue statistics provider for a network listener
     */
    public void unregisterConnectionQueueStatsProviderGlobal(String name) {
        final ConnectionQueueStatsProvider connectionQueueStatsProvider =
                connectionQueueStatsProvidersMap.remove(name);
        if (connectionQueueStatsProvider != null) {
            StatsProviderManager.unregister(connectionQueueStatsProvider);
        }
    }

    public ConnectionQueueStatsProvider getConnectionQueueStatsProvider(String name) {
        return connectionQueueStatsProvidersMap.get(name);
    }

    public FileCacheStatsProvider getFileCacheStatsProvider(String name) {
        return fileCacheStatsProvidersMap.get(name);
    }

    public KeepAliveStatsProvider getKeepAliveStatsProvider(String name) {
        return keepAliveStatsProvidersMap.get(name);
    }

    public ThreadPoolStatsProvider getThreadPoolStatsProvider(String name) {
        return threadPoolStatsProvidersMap.get(name);
    }

    private String subtreePrefix(String name) {
        return "network/" + name;
    }
}
