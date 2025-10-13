/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admin.cluster;

import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import com.sun.enterprise.admin.util.TimeoutParamDefaultCalculator;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.v3.admin.cluster.ClusterCommandHelper;
import com.sun.enterprise.v3.admin.cluster.Strings;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Restarts all instances in a deployment group
 *
 * @author Steve Millidge (Payara Services Limited)
 * @since 5.0
 */
@Service(name = "restart-deployment-group")
@ExecuteOn(value = {RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE) // don't prevent _synchronize-files
@PerLookup
@RestEndpoints({
        @RestEndpoint(configBean = DeploymentGroup.class,
                opType = RestEndpoint.OpType.POST,
                path = "restart-deployment-group",
                description = "Restart Deployment Group",
                params = {
                        @RestParam(name = "id", value = "$parent")
                })
})
public class RestartDeploymentGroupCommand implements AdminCommand {

    private static final String NL = System.lineSeparator();

    @Inject
    private ServerEnvironment env;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner runner;

    @Inject
    private PayaraExecutorService executor;

    @Param(optional = false, primary = true)
    private String deploymentGroup;

    @Param(optional = true, defaultValue = "false")
    private boolean verbose;

    @Param(optional = true, defaultValue = "true")
    private boolean rolling;

    @Param(optional = true, defaultValue = "5000")
    private String delay;

    @Param(optional = true, defaultCalculator = TimeoutParamDefaultCalculator.class)
    private int instanceTimeout;

    @Param(optional = true, defaultCalculator = TimeoutParamDefaultCalculator.class)
    private int timeout;

    private ActionReport report;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger = context.getLogger();

        if (timeout <= 0) {
            String msg = "Timeout must be at least 1 second long.";
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if (instanceTimeout <= 0) {
            String msg = "Instance Timeout must be at least 1 second long.";
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        logger.info(Strings.get("restart.dg", deploymentGroup));

        // Require that we be a DAS
        if (!env.isDas()) {
            String msg = Strings.get("cluster.command.notDas");
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if (rolling) {
            CountDownLatch commandTimeout = new CountDownLatch(1);
            ScheduledFuture<?> commandFuture = executor.schedule(() -> {
                doRolling(context);
                commandTimeout.countDown();
            }, 500, TimeUnit.MILLISECONDS);
            try {
                if (!commandTimeout.await(timeout <= 0 ? RemoteRestAdminCommand.getReadTimeout() : timeout, TimeUnit.SECONDS)) {
                    String msg = Strings.get("restart.dg.timeout", deploymentGroup);

                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    return;
                }
            } catch (InterruptedException e) {
                return;
            } finally {
                commandFuture.cancel(true);
            }
        } else {

            ClusterCommandHelper clusterHelper = new ClusterCommandHelper(domain,
                    runner);

            ParameterMap pm = new ParameterMap();
            pm.add("delay", delay);
            pm.add("timeout", String.valueOf(instanceTimeout));
            try {
                // Run restart-instance against each instance in the Deployment Group
                String commandName = "restart-instance";
                clusterHelper.setAdminTimeout(timeout * 1000);
                clusterHelper.runCommand(commandName, pm, deploymentGroup, context,
                        verbose, rolling);
            } catch (CommandException e) {
                String msg = e.getLocalizedMessage();
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
            }
        }
    }

    private void doRolling(AdminCommandContext context) {
        List<Server> servers = domain.getServersInTarget(deploymentGroup);
        StringBuilder output = new StringBuilder();
        Logger logger = context.getLogger();

        for (Server server : servers) {
            ParameterMap instanceParameterMap = new ParameterMap();
            // Set the instance name as the operand for the commnd
            instanceParameterMap.set("DEFAULT", server.getName());
            instanceParameterMap.add("timeout", String.valueOf(instanceTimeout));

            ActionReport instanceReport = runner.getActionReport("plain");
            instanceReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            CommandRunner.CommandInvocation invocation = runner.getCommandInvocation(
                    "stop-instance", instanceReport, context.getSubject());
            invocation.parameters(instanceParameterMap);

            String msg = "stop-instance" + " " + server.getName();
            logger.info(msg);
            if (verbose) {
                output.append(msg).append(NL);
            }
            invocation.execute();
            logger.info(invocation.report().getMessage());
            if (verbose) {
                output.append(invocation.report().getMessage()).append(NL);
            }
            instanceParameterMap = new ParameterMap();
            // Set the instance name as the operand for the commnd
            instanceParameterMap.set("DEFAULT", server.getName());
            instanceParameterMap.add("timeout", String.valueOf(instanceTimeout));
            instanceReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            invocation = runner.getCommandInvocation(
                    "start-instance", instanceReport, context.getSubject());
            invocation.parameters(instanceParameterMap);
            msg = "start-instance" + " " + server.getName();
            logger.info(msg);
            if (verbose) {
                output.append(msg).append(NL);
            }
            invocation.execute();
            logger.info(invocation.report().getMessage());
            if (verbose) {
                output.append(invocation.report().getMessage()).append(NL);
            }
            try {
                long delayVal = Long.parseLong(delay);
                if (delayVal > 0) {
                    Thread.sleep(delayVal);
                }
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

}
