/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;

/**
 * Asadmin command for updating the settings of a Docker Node.
 *
 * @author Andrew Pielage
 */
@Service(name = "update-node-docker")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean= Node.class,
                opType=RestEndpoint.OpType.POST,
                path="update-node-docker",
                description="Updates the configuration of a Docker Node",
                params={
                @RestParam(name="id", value="$parent")
        })
})
public class UpdateNodeDockerCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    String name;

    @Param(name = "nodehost", optional = true)
    String nodehost;

    @Param(name = "nodedir", optional = true)
    String nodedir;

    @Param(name = "installdir", optional = true)
    String installdir;

    @Param(name = "dockerImage", optional = true, alias = "dockerimage")
    String dockerImage;

    @Param(name = "dockerPasswordFile", optional = true, alias = "dockerpasswordfile")
    String dockerPasswordFile;

    @Param(name = "dockerPort", optional = true, alias = "dockerport")
    Integer dockerPort;

    @Param(name = "useTls", alias = "usetls", optional = true)
    Boolean useTls;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private Nodes nodes;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        ActionReport actionReport = adminCommandContext.getActionReport();
        Node node = nodes.getNode(name);

        if (node == null) {
            actionReport.setMessage("No node with given name: " + name);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (node.isDefaultLocalNode()) {
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            actionReport.setMessage("Cannot update default node with this command");
            return;
        }

        if (!StringUtils.ok(nodehost) && !StringUtils.ok(node.getNodeHost())) {
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            actionReport.setMessage("A node must have a host");
            return;
        }

        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", name);

        if (nodehost != null) {
            parameterMap.add("nodehost", nodehost);
        }

        if (nodedir != null) {
            parameterMap.add("nodedir", nodedir);
        }

        if (installdir != null) {
            parameterMap.add("installdir", installdir);
        }

        if (dockerImage != null) {
            parameterMap.add("dockerImage", dockerImage);
        }

        if (dockerPasswordFile != null) {
            parameterMap.add("dockerPasswordFile", dockerPasswordFile);
        }

        if (dockerPort != null) {
            parameterMap.add("dockerPort", Integer.toString(dockerPort));
        }

        if (useTls != null) {
            parameterMap.add("useTls", useTls.toString());
        }

        if (parameterMap.size() > 1) {
            CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation(
                    "_update-node", actionReport, adminCommandContext.getSubject());
            commandInvocation.parameters(parameterMap);
            commandInvocation.execute();
        }
    }
}
