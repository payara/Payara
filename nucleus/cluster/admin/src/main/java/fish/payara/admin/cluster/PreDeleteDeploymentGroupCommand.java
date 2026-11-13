/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.enterprise.config.serverbeans.DGServerRef;
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
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Takes every member out of a deployment group through
 * {@code remove-instance-from-deployment-group}, before {@code delete-deployment-group} removes
 * the (now empty) group from the configuration.
 * <p>
 * {@code delete-deployment-group} is the generic {@code @Delete} CRUD command of the
 * {@code deployment-group} element, which only removes that element on the DAS. It replicates
 * nothing to the group's members and does not clean up the membership first, so the group's
 * other running members keep the deleted group (and their fellow members' {@code server} and
 * {@code config} elements) in their own configuration until they are restarted, and an offline
 * member gets no warning that it must be started to converge. Routing each member out through
 * {@code remove-instance-from-deployment-group} first reuses that command's live convergence and
 * its "seems to be offline" warning, leaving the empty group for the delete to remove.
 * <p>
 * All members are removed in a single {@code remove-instance-from-deployment-group} call rather
 * than one at a time. A whole-group teardown removes every member, so scoping the removal to the
 * entire membership at once leaves no member behind in the group: the replication framework, which
 * resolves the command's target members only after the DAS has dropped their references, then finds
 * the group empty and warns no one, while {@code remove-instance-from-deployment-group} still
 * converges each running member live and warns once about each offline member. Removing members one
 * at a time would instead warn an offline member twice — once as a still-present member while a
 * fellow member ahead of it is removed, and again as the departed member when it is itself removed.
 * <p>
 * {@code notifyoffline} is deliberately left at its default (true): deleting a group that still
 * has an offline member is exactly the case the operator should be warned about, unlike
 * delete-instance (see {@link PreUnregisterInstanceDeploymentGroupCommand}), which suppresses the
 * warning because the instance being deleted is expected to be offline.
 * <p>
 * It has to run {@code Timing.Before} the delete: once {@code delete-deployment-group} has run,
 * the {@code deployment-group} element this command reads its membership from is gone.
 * <p>
 * No {@code @RestEndpoint} is declared: this is only ever run in process by the supplemental
 * command executor on the DAS, never invoked remotely.
 *
 * @since 7.2026.10
 */
@Service(name = "_pre-delete-deployment-group")
@Supplemental(value = "delete-deployment-group", on = Supplemental.Timing.Before,
        ifFailure = FailurePolicy.Warn)
@I18n("pre.delete.deployment.group")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
public class PreDeleteDeploymentGroupCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    String deploymentGroupName;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        DeploymentGroup deploymentGroup = domain.getDeploymentGroupNamed(deploymentGroupName);
        if (deploymentGroup == null) {
            // Nothing to empty; delete-deployment-group reports the missing group.
            return;
        }

        // Snapshot the names: invoking the command below mutates the membership this list is
        // derived from.
        List<String> memberNames = new ArrayList<>();
        for (DGServerRef ref : deploymentGroup.getDGServerRef()) {
            memberNames.add(ref.getRef());
        }
        if (memberNames.isEmpty()) {
            // Empty group; nothing to remove, delete-deployment-group just drops the element.
            return;
        }

        // Remove the whole membership in a single call: see the class javadoc for why one call,
        // rather than one per member, is what avoids warning an offline member twice.
        CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(
                "remove-instance-from-deployment-group", report, context.getSubject());
        ParameterMap parameters = new ParameterMap();
        parameters.add("instance", String.join(",", memberNames));
        // The command declares the group under "deploymentGroup"; it surfaces it as the
        // replication target itself, see its getTarget().
        parameters.add("deploymentGroup", deploymentGroupName);
        inv.parameters(parameters).execute();
    }
}
