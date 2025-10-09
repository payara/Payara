/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.enterprise.config.serverbeans.DGServerRef;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.enterprise.config.serverbeans.DeploymentGroups;
import java.util.Arrays;
import java.util.List;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
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
@Service(name = "remove-instance-from-deployment-group")
@I18n("remove.instance.from.deployment.group")
@PerLookup
@ExecuteOn(value={RuntimeType.ALL}, ifFailure = FailurePolicy.Ignore, ifOffline = FailurePolicy.Ignore, ifNeverStarted = FailurePolicy.Ignore)
@RestEndpoints({
    @RestEndpoint(configBean = DeploymentGroups.class,
            opType = RestEndpoint.OpType.POST,
            path = "remove-instance-from-deployment-group",
            description = "Remove Instance From a Deployment Group")
})
public class RemoveInstanceFromDeploymentGroupCommand implements AdminCommand {

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
        if (deploymentGroup == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Deployment Group " + deploymentGroupName + " does not exist");
            return;
        }

        List<String> instances = Arrays.asList(instanceName.split(","));

        for (String instance : instances) {
            Server server = domain.getServerNamed(instance);

            if (server == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Instance " + instance + " does not exist");
                return;
            }

            DGServerRef deploymentGroupServerRef = deploymentGroup.getDGServerRefByRef(instance);
            if (deploymentGroupServerRef == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Deployment Group " + deploymentGroupName + " does not contain server " + instance);
                return;
            }

            // OK set up the reference
            try {
                ConfigSupport.apply((DeploymentGroup dg1) -> {
                    dg1.getDGServerRef().remove(deploymentGroupServerRef);
                    return null;
                }, deploymentGroup);

            } catch (TransactionFailure e) {
                report.setMessage("Failed to remove instance from the deployment group");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
            }

            // now run the command to remove application deploymentGroupServerRef to the instance
            for (ApplicationRef applicationRef : deploymentGroup.getApplicationRef()) {
                CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("delete-application-ref", report, context.getSubject());
                ParameterMap parameters = new ParameterMap();
                parameters.add("target", instance);
                parameters.add("name", applicationRef.getRef());
                inv.parameters(parameters).execute();
            }

            // now run the command to remove resource deploymentGroupServerRef to the instance
            for (ResourceRef resourceRef : deploymentGroup.getResourceRef()) {
                CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("delete-resource-ref", report, context.getSubject());
                ParameterMap parameters = new ParameterMap();
                parameters.add("target", instance);
                parameters.add("reference_name", resourceRef.getRef());
                inv.parameters(parameters).execute();
            }

        }
    }
}
