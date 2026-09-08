/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2026] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.UndoableCommand;
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
@Service(name = "add-instance-to-deployment-group")
@I18n("add.instance.to.deployment.group")
@PerLookup
// Replication is scoped to the group's own members by @TargetType, so an instance that is
// contacted here is genuinely a member. An offline member is nevertheless not a failure: the
// change is queued as a pending config change and the member reconciles from the DAS
// domain.xml on its next startup, so offline/never-started members are skipped silently
// rather than degrading the command to "completed with warnings" (this matches the policy the
// commands carried before the replication was scoped, and the one the departed-instance
// replication in RemoveInstanceFromDeploymentGroupCommand passes explicitly). ifFailure is
// deliberately left at its default so a genuine replication failure is still reported.
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE},
        ifOffline = FailurePolicy.Ignore, ifNeverStarted = FailurePolicy.Ignore)
@TargetType(value = {CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = DeploymentGroups.class,
            opType = RestEndpoint.OpType.POST,
            path = "add-instance-to-deployment-group",
            description = "Add Instance to a Deployment Group")
})
public class AddInstanceToDeploymentGroupCommand implements UndoableCommand, DeploymentTargetResolver {

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

    /**
     * Runs on the DAS before the command is replicated to the deployment group's
     * members. The framework replicates this command using the {@code parameters} map,
     * so here we widen the replicated {@code instance} value to the group's <em>full</em>
     * membership (the members already recorded on the DAS plus the ones being added).
     * That way every running member — including a newly joined instance that only knew
     * about itself — reconciles to the complete membership live, instead of each member
     * only ever learning the single delta it was told about. The DAS execution itself is
     * unaffected because its {@code instanceName} field was already injected from the
     * original parameters before this method runs.
     */
    @Override
    public ActionReport.ExitCode prepare(AdminCommandContext context, ParameterMap parameters) {
        // Only the DAS drives replication; instances receive the already-widened value.
        if (!env.isDas()) {
            return ActionReport.ExitCode.SUCCESS;
        }
        DeploymentGroup deploymentGroup = domain.getDeploymentGroupNamed(deploymentGroupName);
        if (deploymentGroup == null) {
            // Let execute() report the missing-group failure; nothing to widen here.
            return ActionReport.ExitCode.SUCCESS;
        }
        // Preserve order and drop duplicates: existing members first, then the new ones.
        Set<String> fullMembership = new LinkedHashSet<>();
        for (DGServerRef ref : deploymentGroup.getDGServerRef()) {
            fullMembership.add(ref.getRef());
        }
        fullMembership.addAll(Arrays.asList(instanceName.split(",")));
        parameters.set("instance", String.join(",", fullMembership));
        return ActionReport.ExitCode.SUCCESS;
    }

    @Override
    public void undo(AdminCommandContext context, ParameterMap parameters, List<Server> instances) {
        // No rollback: prepare() makes no configuration change (it only widens the
        // replicated parameter), and the DAS remains the source of truth, so there is
        // nothing to undo if replication to a member fails.
    }

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();

        DeploymentGroup deploymentGroup = domain.getDeploymentGroupNamed(deploymentGroupName);
        if (deploymentGroup == null) {
            if (env.isDas()) {
                // On the DAS a missing group is a genuine error.
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Deployment Group " + deploymentGroupName + " does not exist");
                return;
            }
            // On an instance the group may not be in the in-memory config yet: it is created
            // by create-deployment-group, which runs on the DAS only, so an instance that was
            // already running when the group was created never learned about it. Create it
            // locally here (rather than dropping the replicated command) so a running member
            // converges live to a group created after it started, instead of only picking the
            // membership up from the DAS domain.xml on the next restart.
            try {
                ConfigSupport.apply((DeploymentGroups dgs) -> {
                    DeploymentGroup newDeploymentGroup = dgs.createChild(DeploymentGroup.class);
                    newDeploymentGroup.setName(deploymentGroupName);
                    dgs.getDeploymentGroup().add(newDeploymentGroup);
                    return newDeploymentGroup;
                }, domain.getDeploymentGroups());
            } catch (TransactionFailure e) {
                report.setMessage("Failed to create deployment group " + deploymentGroupName + " on the instance");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
            deploymentGroup = domain.getDeploymentGroupNamed(deploymentGroupName);
            if (deploymentGroup == null) {
                // Defensive: the group was just created, so this should not happen.
                return;
            }
        }

        List<String> instances = Arrays.asList(instanceName.split(","));

        if (!env.isDas() && !reconcileMembership(deploymentGroup, instances, report)) {
            return;
        }

        for (String instance : instances) {
            Server server = domain.getServerNamed(instance);

            if (server == null && env.isDas()) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Instance " + instance + " does not exist");
                return;
            }

            // On an instance that received this command via replication the reference may
            // already be present in its in-memory config (either synced at startup or kept by
            // reconcileMembership above); adding it again would fail the replicated command,
            // so treat it as a no-op.
            if (!env.isDas() && deploymentGroup.getDGServerRefByRef(instance) != null) {
                continue;
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

            // Application-refs and resource-refs are created on the target instance
            // through explicit command invocations that only make sense on the DAS;
            // instances that receive this command via replication only need their own
            // in-memory group membership updated (done above).
            if (env.isDas()) {
                // now run the command to add application ref to the instance
                for (ApplicationRef applicationRef : deploymentGroup.getApplicationRef()) {
                    CommandInvocation inv = commandRunner.getCommandInvocation("create-application-ref", report, context.getSubject());
                    ParameterMap parameters = new ParameterMap();
                    parameters.add("target", instance);
                    // create-application-ref declares the application name as its primary
                    // operand (@Param(primary=true) name). Supply it under the "DEFAULT"
                    // operand key rather than "name": the latter resolves locally on the DAS
                    // but is not carried as the operand when the command is replicated to the
                    // target instance, which then fails with "Cannot find name in
                    // create-application-ref command model".
                    parameters.add("DEFAULT", applicationRef.getRef());
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
    }

    /**
     * Reconciles the deployment group's membership in an <em>instance's</em> own configuration
     * with the membership carried by the replicated command, by dropping the
     * {@link DGServerRef}s that are no longer part of it. The additions are left to the caller.
     * <p>
     * {@link #prepare(AdminCommandContext, ParameterMap)} widens the replicated
     * {@code instance} parameter on the DAS to the group's <em>full</em> membership, so on an
     * instance {@code membership} is authoritative and not a delta. Only adding the missing
     * references would not be enough: an instance that joins a group whose membership shrank
     * since its last full {@code domain.xml} sync would keep the departed members as phantom
     * entries in its own config until its next restart, because the removals were replicated
     * only to the members of the time and it was not one of them.
     * <p>
     * This is deliberately confined to instances. On the DAS {@code instanceName} still holds
     * the caller's delta (the field is injected before {@code prepare()} widens the parameter
     * map), so reconciling there would wipe the existing members.
     *
     * @return {@code true} if the local configuration is in a usable state, {@code false} if
     *         the reconciliation failed and the command should stop.
     */
    private boolean reconcileMembership(DeploymentGroup deploymentGroup, List<String> membership,
            ActionReport report) {
        Set<String> expected = new LinkedHashSet<>(membership);
        List<DGServerRef> departed = new ArrayList<>();
        for (DGServerRef ref : deploymentGroup.getDGServerRef()) {
            if (!expected.contains(ref.getRef())) {
                departed.add(ref);
            }
        }
        if (departed.isEmpty()) {
            return true;
        }
        try {
            ConfigSupport.apply((DeploymentGroup dg1) -> {
                for (DGServerRef departedRef : departed) {
                    dg1.getDGServerRef().remove(departedRef);
                }
                return null;
            }, deploymentGroup);
        } catch (TransactionFailure e) {
            report.setMessage("Failed to reconcile the membership of deployment group "
                    + deploymentGroupName + " on the instance");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return false;
        }
        return true;
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