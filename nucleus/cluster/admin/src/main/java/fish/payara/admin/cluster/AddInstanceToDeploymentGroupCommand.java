/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import fish.payara.enterprise.config.serverbeans.DGServerRef;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.enterprise.config.serverbeans.DeploymentGroups;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Adds an instance to a deployment group
 *
 * @since 5.0
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "add-instance-to-deployment-group")
@I18n("add.instance.to.deployment.group")
@PerLookup
@ExecuteOn(value={RuntimeType.ALL}, ifFailure = FailurePolicy.Ignore, ifOffline = FailurePolicy.Ignore, ifNeverStarted = FailurePolicy.Ignore)
@RestEndpoints({
    @RestEndpoint(configBean = DeploymentGroups.class,
            opType = RestEndpoint.OpType.POST,
            path = "add-instance-to-deployment-group",
            description = "Add Instance to a Deployment Group")
})
public class AddInstanceToDeploymentGroupCommand implements AdminCommand {

    @Param(name = "instance")
    String instanceName;

    @Param(name = "deploymentGroup")
    String deploymentGroupName;

    @Inject
    private Domain domain;

    @Inject
    ServerEnvironment env;

    @Inject
    CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();

        DeploymentGroup deploymentGroup = domain.getDeploymentGroupNamed(deploymentGroupName);
        if (deploymentGroup == null && env.isDas()) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Deployment Group " + deploymentGroup + " does not exist");
            return;
        }

        List<String> instances = Arrays.asList(instanceName.split(","));

        for (String instance : instances) {
            Server server = domain.getServerNamed(instance);

            if (server == null && env.isDas()) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Instance " + instance + " does not exist");
                return;
            }

            // OK set up the reference
            try {
                ConfigSupport.apply((DeploymentGroup dg1) -> {
                    DGServerRef deploymentGroupServerRef = dg1.createChild(DGServerRef.class);
                    deploymentGroupServerRef.setRef(instance);
                    dg1.getDGServerRef().add(deploymentGroupServerRef);
                    return deploymentGroupServerRef;
                }, deploymentGroup);

            } catch (TransactionFailure e) {
                report.setMessage("Failed to add instance to deployment group");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
            }

            // now run the command to add application ref to the instance
            for (ApplicationRef applicationRef : deploymentGroup.getApplicationRef()) {
                CommandInvocation inv = commandRunner.getCommandInvocation("create-application-ref", report, context.getSubject());
                ParameterMap parameters = new ParameterMap();
                parameters.add("target", instance);
                parameters.add("name", applicationRef.getRef());
                String virtualServers = applicationRef.getVirtualServers();
                if (virtualServers == null || virtualServers.isEmpty()) {
                    virtualServers = getVirtualServers(server);
                }
                parameters.add("virtualservers", virtualServers);
                parameters.add("enabled", applicationRef.getEnabled());
                parameters.add("lbenabled", applicationRef.getLbEnabled());
                inv.parameters(parameters).execute();
            }

            // for all resource refs add resource ref to instance
            for (ResourceRef resourceRef : deploymentGroup.getResourceRef()) {
                CommandInvocation inv = commandRunner.getCommandInvocation("create-resource-ref", report, context.getSubject());
                ParameterMap parameters = new ParameterMap();
                parameters.add("target", instance);
                parameters.add("reference_name", resourceRef.getRef());
                parameters.add("enabled", resourceRef.getEnabled());
                inv.parameters(parameters).execute();

            }
        }
    }

    private String getVirtualServers(Server server) {
        Config config = domain.getConfigs().getConfigByName(
                server.getConfigRef());

        StringJoiner virtualServers = new StringJoiner(",");
        if (config != null) {
            HttpService httpService = config.getHttpService();
            if (httpService != null) {
                List<VirtualServer> hosts = httpService.getVirtualServer();
                if (hosts != null) {
                    for (VirtualServer host : hosts) {
                        if (!("__asadmin").equals(host.getId())) {
                            virtualServers.add(host.getId());
                        }
                    }
                }
            }
        }
        return virtualServers.toString();
    }
}