/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging.commands;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: cmott, naman mehta
 * Date: Aug 26, 2009
 * Time: 5:32:17 PM
 * To change this template use File | Settings | File Templates.
 */
@ExecuteOn({RuntimeType.DAS})
@Service(name = "list-log-levels")
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.log.levels")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-log-levels", 
        description="list-log-levels")
})
public class ListLoggerLevels implements AdminCommand {

    @Inject
    LoggingConfigImpl loggingConfig;

    @Param(primary = true, optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Inject
    Domain domain;

    @Inject
    Servers servers;

    @Inject
    Clusters clusters;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListLoggerLevels.class);

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        boolean isCluster = false;
        boolean isDas = false;
        boolean isInstance = false;
        boolean isConfig = false;
        String targetConfigName = "";

        try {
            HashMap<String, String> props = null;

            Config config = domain.getConfigNamed(target);
            if (config != null) {
                targetConfigName = target;
                isConfig = true;

                Server targetServer = domain.getServerNamed(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME);
                if (targetServer.getConfigRef().equals(target)) {
                    isDas = true;
                }
            } else {

                Server targetServer = domain.getServerNamed(target);

                if (targetServer != null && targetServer.isDas()) {
                    isDas = true;
                } else {
                    com.sun.enterprise.config.serverbeans.Cluster cluster = domain.getClusterNamed(target);
                    if (cluster != null) {
                        isCluster = true;
                        targetConfigName = cluster.getConfigRef();
                    } else if (targetServer != null) {
                        isInstance = true;
                        targetConfigName = targetServer.getConfigRef();
                    }
                }

                if (isInstance) {
                    Cluster clusterForInstance = targetServer.getCluster();
                    if (clusterForInstance != null) {
                        targetConfigName = clusterForInstance.getConfigRef();
                    }
                }
            }

            if (isCluster || isInstance) {
                props = (HashMap<String, String>) loggingConfig.getLoggingProperties(targetConfigName);
            } else if (isDas) {
                props = (HashMap<String, String>) loggingConfig.getLoggingProperties();
            } else if (isConfig) {
                // This loop is for the config which is not part of any target
                props = (HashMap<String, String>) loggingConfig.getLoggingProperties(targetConfigName);
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                String msg = localStrings.getLocalString("invalid.target.sys.props",
                        "Invalid target: {0}. Valid default target is a server named ''server'' (default) or cluster name.", target);
                report.setMessage(msg);
                return;
            }

            List<String> keys = new ArrayList<String>();
            keys.addAll(props.keySet());
            Collections.sort(keys);
            Iterator<String> it2 = keys.iterator();
            // The following Map & List are used to hold the REST data
            Map<String, String> logLevelMap = new HashMap<String, String>();
            List<String> loggerList = new ArrayList<String>();
            while (it2.hasNext()) {
                String name = it2.next();
                if (name.endsWith(".level") && !name.equals(".level")) {
                    final ActionReport.MessagePart part = report.getTopMessagePart()
                            .addChild();
                    String n = name.substring(0, name.lastIndexOf(".level"));
                   // GLASSFISH-21560: removing the condition which filter out logger ending with "Handler" 
                   // above if condition takes care of filtering out everything except log levels from logging.properties
                  //  Format of logger used is <logger_name>.level=<log_level>
                    part.setMessage(n + "\t" + "<" + (String) props.get(name) + ">");
                    logLevelMap.put(n, props.get(name)); //Needed for REST xml and JSON output
                    loggerList.add(n); //Needed for REST xml and JSON output                    
                }
            }
            // Populate the extraProperties data structure for REST...
            Properties restData = new Properties();
            restData.put("logLevels", logLevelMap);
            restData.put("loggers", loggerList);
            report.setExtraProperties(restData);

        } catch (IOException ex) {
            report.setMessage(localStrings.getLocalString("get.log.level.failed",
                    "Could not get logging levels for {0}.", target));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);


    }
}
