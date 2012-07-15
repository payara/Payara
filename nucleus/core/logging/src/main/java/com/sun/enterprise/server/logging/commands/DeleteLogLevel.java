/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 5 Apr, 2011
 * Time: 4:29:29 PM
 * To change this template use File | Settings | File Templates.
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG})
@Service(name = "delete-log-levels")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("delete.log.levels")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-log-levels", 
        description="delete-log-levels")
})
public class DeleteLogLevel implements AdminCommand {
    @Param(name = "logger_name", primary = true, separator = ':')
    String properties;

    @Param(optional = true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Inject
    LoggingConfigImpl loggingConfig;

    @Inject
    Domain domain;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteLogLevel.class);

    public void execute(AdminCommandContext context) {


        final ActionReport report = context.getActionReport();
        boolean isCluster = false;
        boolean isDas = false;
        boolean isInstance = false;
        StringBuffer successMsg = new StringBuffer();
        boolean isConfig = false;
        boolean success = false;
        String targetConfigName = "";

        Map<String, String> m = new HashMap<String, String>();
        try {

            String loggerNames[] = properties.split(":");

            for (final Object key : loggerNames) {
                final String logger_name = (String) key;
                m.put(logger_name + ".level", null);
            }


            Config config = domain.getConfigNamed(target);
            if (config != null) {
                targetConfigName = target;
                isConfig = true;

                Server targetServer = domain.getServerNamed(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME);
                if (targetServer != null && targetServer.getConfigRef().equals(target)) {
                    isDas = true;
                }
                targetServer = null;
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
                loggingConfig.deleteLoggingProperties(m, targetConfigName);
                success = true;
            } else if (isDas) {
                loggingConfig.deleteLoggingProperties(m);
                success = true;
            } else if (isConfig) {
                // This loop is for the config which is not part of any target
                loggingConfig.deleteLoggingProperties(m, targetConfigName);
                success = true;
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                String msg = localStrings.getLocalString("invalid.target.sys.props",
                        "Invalid target: {0}. Valid default target is a server named ''server'' (default) or cluster name.", target);
                report.setMessage(msg);
                return;
            }

            if (success) {
                successMsg.append(localStrings.getLocalString(
                        "delete.log.level.success", "These logging levels are deleted for {0}.", target));
                report.setMessage(successMsg.toString());
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }


        } catch (IOException e) {
            report.setMessage(localStrings.getLocalString("delete.log.level.failed",
                    "Could not delete logger levels for {0}.", target));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
