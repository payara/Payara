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
import com.sun.enterprise.admin.util.InstanceStateService;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.util.ServerHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.enterprise.config.serverbeans.DGServerRef;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.enterprise.config.serverbeans.DeploymentGroups;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.InstanceState;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
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
@Service(name = "remove-instance-from-deployment-group")
@I18n("remove.instance.from.deployment.group")
@PerLookup
// See AddInstanceToDeploymentGroupCommand: replication is scoped to the group's members and
// offline handling for the REMAINING members is left at the @ExecuteOn defaults (ifOffline =
// Warn, ifNeverStarted = Ignore). An offline member that stays in the group legitimately misses
// this removal, so its config is stale until it is next started with sync full or sync normal,
// and the standard "seems to be offline" warning is the correct signal for the operator. The
// JUST-REMOVED instances are handled separately: running ones are converged live through
// replicateRemovalToDepartedRunningInstances (with FailurePolicy.Ignore so a stop in the
// reachability window stays quiet), while offline ones are reported with the same
// "seems to be offline" warning by warnOfflineDepartedInstances, unless a caller that always
// operates on an already-stopped instance (delete-instance, via
// PreUnregisterInstanceDeploymentGroupCommand) suppresses it with notifyoffline=false.
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = DeploymentGroups.class,
            opType = RestEndpoint.OpType.POST,
            path = "remove-instance-from-deployment-group",
            description = "Remove Instance From a Deployment Group")
})
public class RemoveInstanceFromDeploymentGroupCommand implements UndoableCommand, DeploymentTargetResolver {

    private static final LocalStringManagerImpl CLUSTER_UTIL_STRINGS =
            new LocalStringManagerImpl(ClusterOperationUtil.class);

    @Param(name = "instance")
    String instanceName;

    @Param(name = "deploymentGroup")
    String deploymentGroupName;

    // Whether an offline just-removed instance should produce the "seems to be offline"
    // warning. Defaults to true; delete-instance passes false because it always removes an
    // already-stopped instance, for which the warning would only be noise.
    @Param(name = "notifyoffline", optional = true, defaultValue = "true")
    String notifyOffline;

    @Inject
    private Domain domain;

    @Inject
    ServerEnvironment env;

    @Inject
    CommandRunner commandRunner;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private InstanceStateService instanceStateService;

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
     * Runs on the DAS before the command is replicated to the deployment group's members.
     * The {@code notifyoffline} option only governs the DAS-local offline warning about the
     * just-removed instances (see {@link #warnOfflineDepartedInstances}); the members never act
     * on it. Strip it from the replicated {@code parameters} so it is not sent to them: a member
     * still holding a previously released command model (see {@link #getTarget}) would otherwise
     * reject the replicated command with "Option notifyoffline is invalid". The DAS's own field
     * is already injected before this runs, so removing it here does not affect the DAS.
     */
    @Override
    public ActionReport.ExitCode prepare(AdminCommandContext context, ParameterMap parameters) {
        if (env.isDas()) {
            parameters.remove("notifyoffline");
        }
        return ActionReport.ExitCode.SUCCESS;
    }

    @Override
    public void undo(AdminCommandContext context, ParameterMap parameters, List<Server> instances) {
        // No rollback: prepare() only drops a replicated parameter and makes no configuration
        // change, and the DAS remains the source of truth, so there is nothing to undo.
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

        // On a member that received this removal via replication, dropping the dg-server-ref
        // above is not enough to reach the state a restart would produce: the member also holds
        // the departed instance's own server and config elements (see below), which would
        // linger until it is next restarted. Drop that footprint too.
        if (!env.isDas() && !removedInstances.isEmpty()) {
            dropDepartedMemberFootprint(deploymentGroupName, removedInstances, report);
        }

        // The framework replicates this command only to the group's CURRENT members, which
        // no longer include the instances just removed above, so a removed but still-running
        // instance would keep its now-stale membership in its own config until its next
        // restart. Replicate the removal to the departed instances that are still running so
        // they drop their reference live too; offline ones are handled separately below.
        if (env.isDas() && !removedInstances.isEmpty()) {
            replicateRemovalToDepartedRunningInstances(removedInstances, context);
            if (Boolean.parseBoolean(notifyOffline)) {
                warnOfflineDepartedInstances(removedInstances, context);
            }
        }
    }

    /**
     * Warns the operator about just-removed instances that are offline. An offline instance did
     * not receive this removal, so its own config still lists the group until it is next started
     * with sync full or sync normal; the warning is the actionable signal that it must be
     * started to converge.
     * <p>
     * This mirrors the "seems to be offline" warning the replication framework emits for an
     * offline member that stays in the group, but it is produced locally on the DAS because the
     * framework no longer replicates to the removed instances (they are no longer group members,
     * so they are outside the {@code @TargetType} scope). Reachability is decided by a live
     * admin-port check ({@link ServerHelper#isRunning()}) rather than
     * {@code InstanceStateService.getState()}, whose cache can be stale (see
     * {@link #replicateRemovalToDepartedRunningInstances}); a never-started instance is skipped,
     * matching the framework's {@code ifNeverStarted = Ignore} default, because it syncs its
     * whole config from the DAS on its first start.
     */
    private void warnOfflineDepartedInstances(List<String> removedInstances, AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        for (String instance : removedInstances) {
            Server server = domain.getServerNamed(instance);
            if (server == null) {
                continue;
            }
            if (instanceStateService.getState(instance) == InstanceState.StateType.NEVER_STARTED) {
                continue;
            }
            if (!new ServerHelper(server, server.getConfig()).isRunning()) {
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
                report.appendMessage("\n" + CLUSTER_UTIL_STRINGS.getLocalString(
                        "clusterutil.warnoffline",
                        "WARNING: Instance {0} seems to be offline; command {1} was not replicated to that instance",
                        instance, "remove-instance-from-deployment-group"));
            }
        }
    }

    /**
     * On an instance that received this removal via replication, drops the {@code server} and
     * {@code config} elements it was holding only because the removed instances were fellow
     * group members, so it converges to the state a restart would produce instead of keeping
     * their now-inert footprint until then.
     * <p>
     * When an instance parses the {@code domain.xml} it synced from the DAS,
     * {@code InstanceReaderFilter} keeps the {@code server} and {@code config} elements of its
     * <em>fellow group members</em> alongside its own — this is what
     * {@link AddInstanceToDeploymentGroupCommand} transports live (through
     * {@code _register-instance-at-instance} and {@code _copy-config-at-instance}) when a member
     * joins. Removing only the {@code dg-server-ref} therefore leaves the departed member's
     * {@code server} and {@code config} behind until the next restart; this drops them too.
     * <p>
     * Two cases are handled. A member that stays in the group drops each removed instance it no
     * longer shares any group with. The removed instance itself (when it is the one executing
     * this replicated command) drops its ex-fellow members' footprint and the now-inert group
     * element from its own config, since it is no longer a member and a restart would not
     * recreate either here.
     */
    private void dropDepartedMemberFootprint(String groupName, List<String> removed, ActionReport report) {
        String me = env.getInstanceName();
        if (removed.contains(me)) {
            DeploymentGroup group = domain.getDeploymentGroupNamed(groupName);
            if (group != null) {
                // Former fellow members whose footprint this instance held. Two sources, unioned:
                // the members still referenced by the group (a one-at-a-time removal, where only
                // this instance departed so its ex-fellows are still listed) and the members
                // removed alongside this instance in the same call (a whole-group teardown or a
                // multi-instance remove, where their references are already gone). Snapshot before
                // the group element is removed below, which is what the loop reads them from.
                Set<String> peers = new LinkedHashSet<>();
                for (DGServerRef ref : group.getDGServerRef()) {
                    if (!ref.getRef().equals(me)) {
                        peers.add(ref.getRef());
                    }
                }
                for (String departed : removed) {
                    if (!departed.equals(me)) {
                        peers.add(departed);
                    }
                }
                // Remove the group element first, then each peer's footprint. While the group
                // still exists, DeploymentGroup.getReference() reports the config-ref of its
                // first surviving member (see DeploymentGroup.Duck.getReference), so the group
                // counts as a second reference container of that peer's config and the <= 1
                // guard in removeStandaloneInstanceFootprint would keep the config. Worse, as
                // each peer's server is removed the group's reported reference shifts to the
                // next surviving peer, protecting every peer config in turn. Dropping the group
                // first leaves only the peer's own server referencing its config, so the whole
                // footprint is removed as intended.
                removeGroupElement(group, report);
                for (String peer : peers) {
                    if (!sharesAnotherGroup(me, peer, groupName)) {
                        removeStandaloneInstanceFootprint(peer, report);
                    }
                }
            }
            return;
        }
        for (String departed : removed) {
            if (!departed.equals(me) && !sharesAnotherGroup(me, departed, groupName)) {
                removeStandaloneInstanceFootprint(departed, report);
            }
        }
    }

    /**
     * Returns whether instances {@code a} and {@code b} are both members of some deployment
     * group other than {@code excludeGroup}. If they are, they remain fellow members and one
     * must keep holding the other's {@code server}/{@code config}, so it must not be dropped.
     */
    private boolean sharesAnotherGroup(String a, String b, String excludeGroup) {
        for (DeploymentGroup group : domain.getDeploymentGroups().getDeploymentGroup()) {
            if (group.getName().equals(excludeGroup)) {
                continue;
            }
            if (group.getDGServerRefByRef(a) != null && group.getDGServerRefByRef(b) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the {@code server} element of {@code instanceName} from this instance's own
     * configuration, together with its auto-generated {@code <name>-config} when nothing else
     * references it.
     * <p>
     * Only a standalone instance's footprint is removed: a clustered instance's {@code server}
     * and {@code config} are owned by its cluster and kept in sync through cluster replication,
     * not through deployment-group membership. The config is only removed when it is the
     * instance's own auto-generated {@code <name>-config} and no other reference container still
     * uses it, mirroring the guard in {@code Server.DeleteDecorator}.
     */
    private void removeStandaloneInstanceFootprint(String instanceName, ActionReport report) {
        Server target = domain.getServerNamed(instanceName);
        if (target == null || target.getCluster() != null) {
            return;
        }
        String configName = target.getConfigRef();
        Config config = (configName != null) ? domain.getConfigNamed(configName) : null;
        boolean removeConfig = config != null
                && configName.equals(instanceName + "-config")
                && domain.getReferenceContainersOf(config).size() <= 1;
        try {
            ConfigSupport.apply((Servers servers) -> {
                Server server = servers.getServer(instanceName);
                if (server != null) {
                    servers.getServer().remove(server);
                }
                return null;
            }, domain.getServers());
            if (removeConfig) {
                ConfigSupport.apply((Configs configs) -> {
                    Config namedConfig = configs.getConfigByName(configName);
                    if (namedConfig != null) {
                        configs.getConfig().remove(namedConfig);
                    }
                    return null;
                }, domain.getConfigs());
            }
        } catch (TransactionFailure e) {
            // Non-fatal: a leftover server/config only means this member keeps it until its next
            // restart, which is exactly the pre-fix behaviour, so warn rather than fail.
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            report.appendMessage("\nFailed to remove the configuration of departed instance "
                    + instanceName + " from " + env.getInstanceName()
                    + "; it will be cleared on the next restart.");
        }
    }

    /**
     * Removes the deployment-group element itself from this instance's own configuration, used
     * when this instance has just been removed from the group and therefore should no longer
     * carry it. A restart would not recreate it here, since {@code InstanceReaderFilter} only
     * keeps the groups an instance is a member of.
     */
    private void removeGroupElement(DeploymentGroup group, ActionReport report) {
        String groupName = group.getName();
        try {
            ConfigSupport.apply((DeploymentGroups groups) -> {
                for (DeploymentGroup candidate : groups.getDeploymentGroup()) {
                    if (candidate.getName().equals(groupName)) {
                        groups.getDeploymentGroup().remove(candidate);
                        break;
                    }
                }
                return null;
            }, domain.getDeploymentGroups());
        } catch (TransactionFailure e) {
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            report.appendMessage("\nFailed to remove deployment group " + groupName + " from "
                    + env.getInstanceName() + "; it will be cleared on the next restart.");
        }
    }

    /**
     * Replicates this removal to the just-removed instances that are still running so they
     * drop the deployment-group reference from their own live config without waiting for a
     * restart. Only running instances are contacted here; an offline just-removed instance is
     * handled by {@link #warnOfflineDepartedInstances}, which warns the operator that it must be
     * started to converge (its config still lists the group until then).
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
