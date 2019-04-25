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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.servermgmt.logging;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.admin.servermgmt.RepositoryConfig;
import com.sun.enterprise.admin.servermgmt.pe.PEFileLayout;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.io.FileUtils;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.inject.Inject;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import static com.sun.enterprise.admin.servermgmt.SLogger.*;

/**
 * Startup service to update existing domain.xml to move log-service entries to
 * logging.properties file.
 *
 * @author Carla Mott
 */
@Service
public class UpgradeLogging implements ConfigurationUpgrade, PostConstruct {

    @Inject
    Configs configs;
    @Inject
    LoggingConfigImpl logConfig;

    @Override
    public void postConstruct() {
        for (Config config : configs.getConfig()) {
            doUpgrade(config);
        }
    }

    private void doUpgrade(Config config) {
        // v3 uses logging.properties to configure the logging facility.
        // move all log-service elements to logging.properties
        final LogService logService = config.getLogService();

        // check if null and exit
        if (logService == null)
            return;
        // get a copy of the logging.properties file

        try {
            RepositoryConfig rc = new RepositoryConfig();
            String configDir = rc.getRepositoryRoot() + File.separator + rc.getRepositoryName()
                    + File.separator + rc.getInstanceName() + File.separator + "config";
            PEFileLayout layout = new PEFileLayout(rc);
            File src = new File(layout.getTemplatesDir(), PEFileLayout.LOGGING_PROPERTIES_FILE);
            File dest = new File(configDir, PEFileLayout.LOGGING_PROPERTIES_FILE);
            if (!dest.exists())
                FileUtils.copy(src, dest);

        }
        catch (IOException ioe) {
            getLogger().log(Level.SEVERE, FAIL_CREATE_LOG_PROPS, ioe);
        }

        try {
            //Get the logLevels
            ModuleLogLevels mll = logService.getModuleLogLevels();

            Map<String, String> logLevels = mll.getAllLogLevels();
            String file = logService.getFile();
            String payaraNotificationFile = logService.getPayaraNotificationFile();
            String instanceRoot = System.getProperty("com.sun.aas.instanceRoot");
            if (file.contains(instanceRoot)) {
                file = file.replace(instanceRoot, "${com.sun.aas.instanceRoot}");
            }
            
            if (payaraNotificationFile.contains(instanceRoot)) {
                payaraNotificationFile = payaraNotificationFile.replace(instanceRoot, "${com.sun.aas.instanceRoot}");
            }
            
            logLevels.put("file", file);
            logLevels.put("payara-notification-file", payaraNotificationFile);
            logLevels.put("use-system-logging", logService.getUseSystemLogging());
            //this can have multiple values so need to add
            logLevels.put("log-handler", logService.getLogHandler());
            logLevels.put("log-filter", logService.getLogFilter());
            logLevels.put("log-to-file", logService.getLogToFile());
            logLevels.put("payara-notification-log-to-file", logService.getPayaraNotificationLogToFile());
            logLevels.put("log-to-console", logService.getLogToConsole());
            logLevels.put("log-rotation-limit-in-bytes", logService.getLogRotationLimitInBytes());
            logLevels.put("payara-notification-log-rotation-limit-in-bytes", logService.getPayaraNotificationLogRotationLimitInBytes());
            logLevels.put("log-rotation-timelimit-in-minutes", logService.getLogRotationTimelimitInMinutes());
            logLevels.put("payara-notification-log-rotation-timelimit-in-minutes", logService.getPayaraNotificationLogRotationTimelimitInMinutes());
            logLevels.put("alarms", logService.getAlarms());
            logLevels.put("retain-error-statistics-for-hours", logService.getRetainErrorStatisticsForHours());
            logLevels.put("log-standard-streams", logService.getLogStandardStreams());
            final Map<String, String> m = new HashMap<String, String>(logLevels);

            ConfigSupport.apply(new SingleConfigCode<Config>() {
                @Override
                public Object run(Config c) throws PropertyVetoException, TransactionFailure {

                    try {
                        //update logging.properties
                        logConfig.setLoggingProperties(m);
                        c.setLogService(null);
                    }
                    catch (IOException e) {
                        getLogger().log(Level.SEVERE, FAIL_UPDATE_LOG_PROPS, e);
                    }
                    return null;
                }
            }, config);
        }
        catch (TransactionFailure tf) {
            getLogger().log(Level.SEVERE, FAIL_UPGRADE_LOG_SERVICE, tf);
            throw new RuntimeException(tf);
        }
    }
}
