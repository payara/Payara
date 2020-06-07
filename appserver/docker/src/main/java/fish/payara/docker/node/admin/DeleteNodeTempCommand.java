/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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

package fish.payara.docker.node.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
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

import javax.inject.Inject;

import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

/**
 * Command to delete a temporary node.
 *
 * @author AndrewPielage <andrew.pielage@payara.fish>
 */
@Service(name = "_delete-node-temp")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
        @RestEndpoint(configBean = Nodes.class,
                opType = POST,
                path = "_delete-node-temp",
                description = "Delete Node Temp")
})
public class DeleteNodeTempCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    private String name;

    @Inject
    private Nodes nodes;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Node node = nodes.getNode(name);

        if (node == null) {
            // No node to delete nothing to do here
            actionReport.setActionExitCode(FAILURE);
            actionReport.setMessage("No node found with given name: " + name);
            return;
        }

        if (!(node.getType().equals("TEMP"))) {
            // No node to delete nothing to do here
            actionReport.setActionExitCode(FAILURE);
            actionReport.setMessage("Node with given name is not a temp node: " + name);
            return;

        }

        CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation(
                "_delete-node", actionReport, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("DEFAULT", name);
        commandInvocation.parameters(commandParameters);

        commandInvocation.execute();
    }
}
