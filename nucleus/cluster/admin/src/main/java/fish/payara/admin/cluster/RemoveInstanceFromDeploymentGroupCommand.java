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

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.util.ServerHelper;
import fish.payara.enterprise.config.serverbeans.DGServerRef;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.enterprise.config.serverbeans.DeploymentGroups;
import java.util.ArrayList;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.DeploymentTargetResolver;
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
// See AddInstanceToDeploymentGroupCommand: replication is scoped to the group's members, and
// an offline member is skipped silently because it reconciles from the DAS domain.xml on its
// next startup. This also keeps the annotation-driven replication consistent with the explicit
// FailurePolicy.Ignore used below when replicating to the just-removed instances.
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE},
        ifOffline = FailurePolicy.Ignore, ifNeverStarted = FailurePolicy.Ignore)
@TargetType(value = {CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = DeploymentGroups.class,
            opType = RestEndpoint.OpType.POST,
            path = "remove-instance-from-deployment-group",
            description = "Remove Instance From a Deployment Group")
})
public class RemoveInstanceFromDeploymentGroupCommand implements AdminCommand, DeploymentTargetResolver {

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

    @Inject
    private ServiceLocator serviceLocator;

    /**
     * Resolves the replication target for this command. The admin console and REST
     * clients pass the deployment group under its own {@code deploymentGroup} parameter,
     * but the replication framework derives the command's target (for {@code @TargetType}
     * validation and for scoping the replication to the group's members) from the
     * {@code target} parameter. Expose the group as {@code target} so the cluster executor
     * sees it, while <em>keeping</em> the {@code deploymentGroup} parameter untouched.
     * <p>
     * The parameter is deliberately still declared as {@code deploymentGroup} (its name in
     * every released build) rather than being renamed to {@code target} with an alias:
     * {@code InstanceRestCommandExecutor} builds the replicated request from a cached
     * command model ({@code ~/.gfclient/cache}) that is not revalidated before use, so a
     * member still holding the previously released model only recognises the option under
     * the {@code deploymentGroup} name. Renaming it to {@code target} makes such a member
     * reject the replicated command with "Option deploymentGroup is required but was not
     * specified", which is why the group name is surfaced as {@code target} additively here
     * instead of moving the option.
     */
    @Override
    public String getTarget(ParameterMap parameters) {
        String target = parameters.getOne("target");
        if (target == null) {
            target = parameters.getOne("deploymentGroup");
            if (target != null) {
                parameters.set("target", target);
            }
        }
        return target;
    }

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        DeploymentGroup deploymentGroup = domain.getDeploymentGroupNamed(deploymentGroupName);
        if (deploymentGroup == null) {
            // On the DAS a missing group is a genuine error. On an instance it only means
            // the in-memory config has not yet caught up with the DAS; the authoritative
            // state is synced from the DAS domain.xml on the next startup, so treat it as a
            // no-op instead of reporting a failure that surfaces as a misleading warning.
            if (env.isDas()) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Deployment Group " + deploymentGroupName + " does not exist");
            }
            return;
        }

        List<String> instances = Arrays.asList(instanceName.split(","));
        List<String> removedInstances = new ArrayList<>();

        for (String instance : instances) {
            Server server = domain.getServerNamed(instance);

            if (server == null && env.isDas()) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Instance " + instance + " does not exist");
                return;
            }

            DGServerRef deploymentGroupServerRef = deploymentGroup.getDGServerRefByRef(instance);
            if (deploymentGroupServerRef == null) {
                // Same rationale as above: on an instance the reference may simply not be
                // present in the not-yet-synced config, so only fail on the DAS.
                if (env.isDas()) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("Deployment Group " + deploymentGroupName + " does not contain server " + instance);
                    return;
                }
                continue;
            }

            // OK set up the reference
            try {
                ConfigSupport.apply((DeploymentGroup dg1) -> {
                    dg1.getDGServerRef().remove(deploymentGroupServerRef);
                    return null;
                }, deploymentGroup);
                removedInstances.add(instance);

            } catch (TransactionFailure e) {
                report.setMessage("Failed to remove instance from the deployment group");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
            }

            // Application-refs and resource-refs are removed from the target instance
            // through explicit command invocations that only make sense on the DAS;
            // instances that receive this command via replication only need their own
            // in-memory group membership updated (done above) and must keep their own
            // application/resource refs intact.
            if (env.isDas()) {
                // now run the command to remove application deploymentGroupServerRef to the instance
                for (ApplicationRef applicationRef : deploymentGroup.getApplicationRef()) {
                    CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("delete-application-ref", report, context.getSubject());
                    ParameterMap parameters = new ParameterMap();
                    parameters.add("target", instance);
                    // delete-application-ref declares the application name as its primary
                    // operand (@Param(primary=true) name). Supply it under the "DEFAULT"
                    // operand key rather than "name": the latter resolves locally on the DAS
                    // but is not carried as the operand when the command is replicated to the
                    // target instance, which then fails with "Cannot find name in
                    // delete-application-ref command model". This matches how the deployment
                    // commands themselves replicate their primary operand (see
                    // DeleteApplicationRefCommand replicating undeploy with "DEFAULT").
                    parameters.add("DEFAULT", applicationRef.getRef());
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

        // The framework replicates this command only to the group's CURRENT members, which
        // no longer include the instances just removed above, so a removed but still-running
        // instance would keep its now-stale membership in its own config until its next
        // restart. Replicate the removal to the departed instances that are still running so
        // they drop their reference live too; offline ones are skipped and reconcile from the
        // DAS domain.xml on the next startup (which is also why no misleading offline warning
        // is produced for them).
        if (env.isDas() && !removedInstances.isEmpty()) {
            replicateRemovalToDepartedRunningInstances(removedInstances, context);
        }
    }

    /**
     * Replicates this removal to the just-removed instances that are still running so they
     * drop the deployment-group reference from their own live config without waiting for a
     * restart. Only running instances are contacted: an offline instance is not a member at
     * replication time anyway and reconciles its now-inert refs from the DAS on the next
     * startup, so contacting it would only risk the misleading "seems to be offline" warning
     * this command is designed to avoid.
     *
     * Whether an instance is running is decided by a live admin-port reachability check
     * ({@link ServerHelper#isRunning()}), not by {@code InstanceStateService.getState()}:
     * the latter is a cache that is only refreshed when something actively pings the
     * instance (e.g. list-instances), so right after start-deployment-group it can still
     * report a stale non-RUNNING state for an instance that is in fact up, which would make
     * this convergence silently skip a genuinely running instance.
     */
    private void replicateRemovalToDepartedRunningInstances(List<String> removedInstances, AdminCommandContext context) {
        List<Server> departedRunning = new ArrayList<>();
        for (String instance : removedInstances) {
            Server server = domain.getServerNamed(instance);
            if (server != null && new ServerHelper(server, server.getConfig()).isRunning()) {
                departedRunning.add(server);
            }
        }
        if (departedRunning.isEmpty()) {
            return;
        }
        ParameterMap parameters = new ParameterMap();
        parameters.set("instance", String.join(",", removedInstances));
        // Pass the group under "deploymentGroup" (the command's declared @Param), not
        // "target": this path goes straight to ClusterOperationUtil, so the outbound
        // request is built from the member's command model, which only recognises the
        // "deploymentGroup" option. Each contacted member resolves it back to its own
        // replication target through getTarget().
        parameters.set("deploymentGroup", deploymentGroupName);
        // offline/never-started policies stay Ignore so that if an instance stops in the
        // narrow window between the reachability check above and this call, it is skipped
        // silently rather than reintroducing the "seems to be offline" warning.
        ClusterOperationUtil.replicateCommand(
                "remove-instance-from-deployment-group",
                FailurePolicy.Warn,
                FailurePolicy.Ignore,
                FailurePolicy.Ignore,
                departedRunning,
                context,
                parameters,
                serviceLocator);
    }
}
