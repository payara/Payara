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


import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.v3.admin.cluster.NodeUtils;
import fish.payara.docker.DockerConstants;
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
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;

/**
 * Asadmin command for creating a Docker Node.
 *
 * @author Andrew Pielage
 */
@Service(name = "create-node-docker")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean= Nodes.class,
                opType=RestEndpoint.OpType.POST,
                path="create-node-docker",
                description="Create a Docker Node to spawn containers on")
})
public class CreateNodeDockerCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    String name;

    @Param(name = NodeUtils.PARAM_NODEHOST)
    String nodehost;

    @Param(name = NodeUtils.PARAM_NODEDIR, optional = true)
    String nodedir;

    @Param(name = NodeUtils.PARAM_INSTALLDIR, optional = true, defaultValue = "/opt/payara/payara6")
    String installdir;

    @Param(name = "dockerPasswordFile", alias = "dockerpasswordfile")
    String dockerPasswordFile;

    @Param(name = "dockerImage", alias = "dockerimage", optional = true, defaultValue = DockerConstants.DEFAULT_IMAGE_NAME)
    String dockerImage;

    @Param(name = "dockerPort", optional = true, alias = "dockerport")
    Integer dockerPort;

    @Param(name = "useTls", alias = "usetls", optional = true)
    Boolean useTls;

    @Inject
    private CommandRunner commandRunner;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values are the parameter values
     *
     * @param adminCommandContext information
     */
    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        ActionReport actionReport = adminCommandContext.getActionReport();

        CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation(
                "_create-node", actionReport, adminCommandContext.getSubject());

        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);

        map.add(NodeUtils.PARAM_INSTALLDIR, DockerConstants.PAYARA_INSTALL_DIR);

        if (StringUtils.ok(nodehost)) {
            map.add("nodehost", nodehost);
        }

        if (StringUtils.ok(nodedir)) {
            map.add("nodedir", nodedir);
        }

        if (StringUtils.ok(installdir)) {
            map.add("installdir", installdir);
        }

        if (StringUtils.ok(dockerPasswordFile)) {
            map.add("dockerPasswordFile", dockerPasswordFile);
        }

        if (StringUtils.ok(dockerImage)) {
            // Check if docker image has version tag (can't add non-constant as default parameter value)
            if (dockerImage.equals(DockerConstants.DEFAULT_IMAGE_NAME)) {
                dockerImage = DockerConstants.DEFAULT_IMAGE_NAME + ":"
                        + Version.getMajorVersion() + "." + Version.getMinorVersion();
            }
            map.add("dockerImage", dockerImage);
        } else {
            // Version can't be added to default of parameter or attribute due to not being a constant
            dockerImage = DockerConstants.DEFAULT_IMAGE_NAME + ":"
                    + Version.getMajorVersion() + "." + Version.getMinorVersion();
            map.add("dockerImage", dockerImage);
        }

        if (dockerPort != null) {
            map.add("dockerPort", Integer.toString(dockerPort));
        }

        if (useTls != null) {
            map.add("useTls", useTls.toString());
        }

        map.add("type","DOCKER");
        commandInvocation.parameters(map);
        commandInvocation.execute();
    }
}
