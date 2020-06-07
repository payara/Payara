/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2016 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.web.admin.monitor;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.config.serverbeans.VirtualServer;

import fish.payara.monitoring.collect.MonitoringDataCollection;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.web.admin.LogFacade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;

import org.jvnet.hk2.config.ConfigurationException;

/**
 *
 * @author PRASHANTH ABBAGANI
 */
@Service(name = "http-service")
@Singleton
public class HttpServiceStatsProviderBootstrap implements PostConstruct, MonitoringDataSource {

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    private MonitoringService monitoringService;

    private static final Logger logger = LogFacade.getLogger();

    private static final ResourceBundle rb = logger.getResourceBundle();

    private final Map<String, HttpServiceStatsProvider> httpServiceStatsProviders = new ConcurrentHashMap<>();

    @Override
    public void postConstruct() {

        if (config == null) {
            Object[] params = {VirtualServerInfoStatsProvider.class.getName(),
                    HttpServiceStatsProvider.class.getName(),
                    "http service", "virtual server"};
            logger.log(Level.SEVERE, LogFacade.UNABLE_TO_REGISTER_STATS_PROVIDERS, params);
            throw new ConfigurationException(rb.getString(LogFacade.NULL_CONFIG));
        }

        HttpService httpService = config.getHttpService();
        for (VirtualServer vs : httpService.getVirtualServer()) {
            String id = vs.getId();
            StatsProviderManager.register(
                    "http-service",
                    PluginPoint.SERVER,
                    "http-service/" + id,
                    new VirtualServerInfoStatsProvider(vs));
            HttpServiceStatsProvider httpServiceStatsProvider = new HttpServiceStatsProvider(id,
                    vs.getNetworkListeners(), config.getNetworkConfig());
            httpServiceStatsProviders.put(id, httpServiceStatsProvider);
            StatsProviderManager.register(
                    "http-service",
                    PluginPoint.SERVER,
                    "http-service/" + id + "/request",
                    httpServiceStatsProvider);
        }
    }

    static {
        MonitoringDataCollection.register(CountStatistic.class,
                (collector, count) -> collector.collect(count.getName(), count.getCount()));
    }

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (!"true".equals(monitoringService.getMonitoringEnabled()) ||
            !"HIGH".equals(monitoringService.getModuleMonitoringLevels().getHttpService())) {
            return;
        }
        MonitoringDataCollector http = collector.in("http").prefix("Server");
        for (HttpServiceStatsProvider provider : httpServiceStatsProviders.values()) {
            http.collectObject(provider, MonitoringDataCollection::collectObject);
        }
    }
}
