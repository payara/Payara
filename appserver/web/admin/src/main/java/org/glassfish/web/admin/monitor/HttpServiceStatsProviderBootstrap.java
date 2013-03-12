/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
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
public class HttpServiceStatsProviderBootstrap implements PostConstruct {

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE =
            "org.glassfish.web.admin.monitor.LogMessages";

    @LoggerInfo(subsystem="WEB", description="WEB Admin Logger", publish=true)
    private static final String WEB_ADMIN_LOGGER = "javax.enterprise.web.admin";

    public static final Logger logger =
            Logger.getLogger(WEB_ADMIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Unable to register StatsProvider {0} with Monitoring Infrastructure. No monitoring data will be collected for {1} and {2}",
            level = "SEVERE",
            cause = "Current server config is null",
            action = "Verify if the server instance is started correctly")
    public static final String UNABLE_TO_REGISTER_STATS_PROVIDERS = "AS-WEB-ADMIN-00001";

    @LogMessageInfo(
            message = "Current server config is null",
            level = "INFO")
    public static final String NULL_CONFIG = "AS-WEB-ADMIN-00002";

    public void postConstruct() {

        if (config == null) {
            Object[] params = {VirtualServerInfoStatsProvider.class.getName(),
                    HttpServiceStatsProvider.class.getName(),
                    "http service", "virtual server"};
            logger.log(Level.SEVERE, UNABLE_TO_REGISTER_STATS_PROVIDERS, params);
            throw new ConfigurationException(rb.getString(NULL_CONFIG));
        }

        HttpService httpService = config.getHttpService();
        for (VirtualServer vs : httpService.getVirtualServer()) {
            StatsProviderManager.register(
                    "http-service",
                    PluginPoint.SERVER,
                    "http-service/" + vs.getId(),
                    new VirtualServerInfoStatsProvider(vs));
            StatsProviderManager.register(
                    "http-service",
                    PluginPoint.SERVER,
                    "http-service/" + vs.getId() + "/request",
                    new HttpServiceStatsProvider(vs.getId(), vs.getNetworkListeners(), config.getNetworkConfig()));
        }
    }
}
