/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import com.sun.enterprise.admin.remote.ServerRemoteRestAdminCommand;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Resources;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * The command to flush a connection pool.
 * <p>
 * This command from 5.193 will poke all known instances and tell them to flush the connection pool. 
 * This occurs by sending the {@code _flush-connection-pool} command to them. If that command fails
 * on any instance (i.e. because the pool is not initialised/not in use on that instance) that this
 * command will return with a {@link ActionReport.ExitCode.WARNING} status code.
 * </p>
 * Pre-5.193 functionality occurs in {@link FlushConnectionPoolLocal} which previously was only executed against the DAS.
 *
 * @author jonathan coustick
 * @since 5.193
 * @see FlushConnectionPoolLocal
 */
@Service(name = "flush-connection-pool")
@PerLookup
@I18n("flush.connection.pool")
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DAS})
@ExecuteOn(value = {RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Resources.class,
            opType = RestEndpoint.OpType.POST,
            path = "flush-connection-pool",
            description = "flush-connection-pool")
})
public class FlushInstancesConnectionPool implements AdminCommand {

    private static final Logger LOGGER = Logger.getLogger("org.glassfish.connectors.admin.cli");

    @Param(name = "pool_name", primary = true)
    private String poolName;

    @Param(name = "appname", optional = true)
    private String applicationName;

    @Param(name = "modulename", optional = true)
    private String moduleName;

    @Inject
    private Applications applications;

    @Inject
    private ConnectionPoolUtil poolUtil;

    @Inject
    CommandRunner commandRunner;

    @Inject
    private Domain domain;

    @Inject
    private ServiceLocator habitat;

    @Inject
    PayaraExecutorService executor;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Resources resources = domain.getResources();
        String scope = "";
        if (moduleName != null) {
            if (!poolUtil.isValidModule(applicationName, moduleName, poolName, report)) {
                report.setMessage("Modulename is not that of a valid module: " + moduleName);
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
                return;
            }
            Application application = applications.getApplication(applicationName);
            Module module = application.getModule(moduleName);
            resources = module.getResources();
            scope = ConnectorConstants.JAVA_MODULE_SCOPE_PREFIX;
        } else if (applicationName != null) {
            if (!poolUtil.isValidApplication(applicationName, poolName, report)) {
                report.setMessage("ApplicationName is not that of a valid module: " + applicationName);
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
                return;
            }
            Application application = applications.getApplication(applicationName);
            resources = application.getResources();
            scope = ConnectorConstants.JAVA_APP_SCOPE_PREFIX;
        }

        if (!poolUtil.isValidPool(resources, poolName, scope, report)) {
            report.setMessage("Connection Pool is not valid");
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            return;
        }

        List<Future> instanceFlushes = new ArrayList<>();
        for (Server server : domain.getServers().getServer()) {

            instanceFlushes.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    ActionReport subReport = report.addSubActionsReport();
                    try {
                        if (!server.isRunning()) {
                            return;//skip servers that are stopped
                        }
                        String host = server.getAdminHost();
                        int port = server.getAdminPort();

                        ParameterMap map = new ParameterMap();
                        map.add("poolName", poolName);
                        if (applicationName != null) {
                            map.add("appname", applicationName);
                        }
                        if (moduleName != null) {
                            map.add("modulename", moduleName);
                        }

                        if (server.isDas()) {
                            CommandRunner runner = habitat.getService(CommandRunner.class);
                            CommandRunner.CommandInvocation invocation = runner.getCommandInvocation("_flush-connection-pool", subReport, context.getSubject());
                            invocation.parameters(map);
                            invocation.execute();
                        } else {
                            RemoteRestAdminCommand rac = new ServerRemoteRestAdminCommand(habitat, "_flush-connection-pool", host, port, false, "admin", null, LOGGER);
                            rac.executeCommand(map);
                            ActionReport result = rac.getActionReport();
                            subReport.setActionExitCode(result.getActionExitCode());
                            subReport.setMessage(result.getMessage());
                        }
                    } catch (CommandException ex) {
                        subReport.failure(Logger.getLogger("CONNECTORS-ADMIN"), ex.getLocalizedMessage(), ex);
                        subReport.appendMessage(server.getName());
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    }
                }
            }));

        }
        for (Future future : instanceFlushes) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        if (report.hasFailures()) {
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
        }
    }

}
