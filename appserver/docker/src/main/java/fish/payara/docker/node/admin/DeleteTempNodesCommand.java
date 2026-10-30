/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.docker.node.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;

import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

/**
 * Command that deletes all temporary nodes that have no running instances.
 *
 * @author AndrewPielage <andrew.pielage@payara.fish>
 */
@Service(name = "_delete-temp-nodes")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
        @RestEndpoint(configBean = Nodes.class,
                opType = POST,
                path = "_delete-temp-nodes",
                description = "Deletes all temporary nodes not in use")
})
public class DeleteTempNodesCommand implements AdminCommand {

    @Inject
    private Nodes nodes;

    @Inject
    private Servers servers;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {
        for (Node node : nodes.getNode()) {
            if (node.getType().equals("TEMP") && commandRunner != null) {
                ParameterMap parameterMap = new ParameterMap();
                parameterMap.add("name", node.getName());

                // If the node still has instances registered to it, delete them if they're not running
                if (node.nodeInUse()) {
                    deleteServersOnNode(node, context);
                } else {
                    commandRunner.getCommandInvocation("_delete-node-temp",
                            context.getActionReport(),
                            context.getSubject())
                            .parameters(parameterMap)
                            .execute();
                }
            }
        }
    }

    private void deleteServersOnNode(Node node, AdminCommandContext context) {
        for (Server server : servers.getServersOnNode(node)) {
            ParameterMap parameterMap = new ParameterMap();
            parameterMap.add("instance_name", server.getName());

            // delete-instance command deletes TEMP nodes if their last instance is deleted.
            commandRunner.getCommandInvocation("delete-instance",
                    context.getActionReport(),
                    context.getSubject())
                    .parameters(parameterMap)
                    .execute();
        }
    }
}
