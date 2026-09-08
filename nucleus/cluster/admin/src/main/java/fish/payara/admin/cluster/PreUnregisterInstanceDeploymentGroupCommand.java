/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2026 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
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
import org.glassfish.api.admin.Supplemental;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Takes an instance that is about to be deleted out of its deployment groups through
 * {@code remove-instance-from-deployment-group}, before {@code _unregister-instance} removes
 * the instance from the configuration.
 * <p>
 * {@code Server.DeleteDecorator} already drops the {@code dg-server-ref} elements, but it does
 * so with a direct configuration transaction on the DAS. That is not a command, so nothing is
 * replicated, and the group's other members keep the deleted instance in their own
 * configuration until they are restarted. Routing the removal through the command instead
 * reuses its replication, so the remaining running members converge live, and it also cleans
 * up the application-refs and resource-refs the group had pushed onto the instance.
 * <p>
 * This runs for both {@code delete-instance} and {@code delete-local-instance}, because both
 * reach the configuration through {@code _unregister-instance}. By the time the decorator
 * runs there is no reference left for it to remove, so it becomes a no-op fallback.
 *
 * @since 5.0
 */
@Service(name = "_pre-unregister-instance-deployment-group")
@Supplemental(value = "_unregister-instance", on = Supplemental.Timing.Before,
        ifFailure = FailurePolicy.Warn)
@I18n("pre.unregister.instance.deployment.group")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "_pre-unregister-instance-deployment-group",
            description = "_pre-unregister-instance-deployment-group")
})
public class PreUnregisterInstanceDeploymentGroupCommand implements AdminCommand {

    @Param(name = "node", optional = true)
    String node;

    @Param(name = "name", primary = true)
    String instanceName;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        Server server = domain.getServerNamed(instanceName);
        if (server == null) {
            // Nothing to take out of a group; _unregister-instance reports the missing instance.
            return;
        }

        // Snapshot the names: invoking the command below mutates the deployment groups this
        // list is derived from.
        List<String> groupNames = new ArrayList<>();
        for (DeploymentGroup deploymentGroup : server.getDeploymentGroup()) {
            groupNames.add(deploymentGroup.getName());
        }

        for (String groupName : groupNames) {
            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(
                    "remove-instance-from-deployment-group", report, context.getSubject());
            ParameterMap parameters = new ParameterMap();
            parameters.add("instance", instanceName);
            // The command declares the group under "deploymentGroup"; it surfaces it as the
            // replication target itself, see its getTarget().
            parameters.add("deploymentGroup", groupName);
            inv.parameters(parameters).execute();
        }
    }
}
