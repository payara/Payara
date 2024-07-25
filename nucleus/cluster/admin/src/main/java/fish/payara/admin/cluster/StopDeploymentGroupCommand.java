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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import com.sun.enterprise.admin.util.TimeoutParamDefaultCalculator;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.v3.admin.cluster.ClusterCommandHelper;
import com.sun.enterprise.v3.admin.cluster.Strings;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.logging.Logger;

/**
 * Stops all instances in a deployment group
 *
 * @author Steve Millidge (Payara Services Limited)
 * @since 5.0
 */
@I18n("stop.dg.command")
@Service(name = "stop-deployment-group")
@PerLookup
@RestEndpoints({
        @RestEndpoint(configBean = DeploymentGroup.class,
                opType = RestEndpoint.OpType.POST,
                path = "stop-deployment-group",
                description = "Stop Deployment Group",
                params = {
                        @RestParam(name = "id", value = "$parent")
                })
})
@Progress
public class StopDeploymentGroupCommand implements AdminCommand {

    @Param(optional = false, primary = true)
    private String deploymentGroup;

    @Param(optional = true, defaultValue = "false")
    private boolean kill = false;

    @Inject
    private ServerEnvironment env;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner runner;

    @Param(optional = true, defaultValue = "false")
    private boolean verbose;

    @Param(optional = true, defaultCalculator = TimeoutParamDefaultCalculator.class)
    private int instanceTimeout;

    @Param(optional = true, defaultCalculator = TimeoutParamDefaultCalculator.class)
    private int timeout;

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

        logger.info(Strings.get("stop.dg", deploymentGroup));

        // Require that we be a DAS
        if (!env.isDas()) {
            String msg = Strings.get("cluster.command.notDas");
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        ClusterCommandHelper clusterHelper = new ClusterCommandHelper(domain,
                runner);

        ParameterMap map = new ParameterMap();
        if (kill) {
            map.add("kill", "true");
        }
        map.add("timeout", String.valueOf(instanceTimeout));

        try {
            // Run start-instance against each instance in the cluster
            String commandName = "stop-instance";
            clusterHelper.setAdminTimeout(Integer.valueOf(timeout) * 1000);
            clusterHelper.runCommand(commandName, map, deploymentGroup, context,
                    verbose);
        } catch (CommandException e) {
            String msg = e.getLocalizedMessage();
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
        }
    }
}
