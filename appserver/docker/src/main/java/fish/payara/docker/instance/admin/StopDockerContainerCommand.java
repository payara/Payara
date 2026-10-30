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

package fish.payara.docker.instance.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Internal Asadmin command that stops the Docker container of an instance. This is used by the
 * stop-instance command.
 *
 * @author Andrew Pielage
 */
@Service(name = "_stop-docker-container")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@RestEndpoints({
        @RestEndpoint(configBean= Server.class,
                opType=RestEndpoint.OpType.POST,
                path="_stop-docker-container",
                description="Stops the Docker contain that this instance exists on",
                params={
                        @RestParam(name="id", value="$parent")
                })
})
public class StopDockerContainerCommand implements AdminCommand {

    @Param(name = "nodeName", alias = "node")
    String nodeName;

    @Param(name = "instanceName", alias = "instance")
    String instanceName;

    @Inject
    private Nodes nodes;

    @Inject
    private Servers servers;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        Node node = nodes.getNode(nodeName);
        Server server = servers.getServer(instanceName);

        Client client = ClientBuilder.newClient();

        WebTarget webTarget;
        if (Boolean.valueOf(node.getUseTls())) {
            webTarget = client.target("https://"
                    + node.getNodeHost()
                    + ":"
                    + node.getDockerPort()
                    + "/containers/"
                    + server.getDockerContainerId()
                    + "/stop");
        } else {
            webTarget = client.target("http://"
                    + node.getNodeHost()
                    + ":"
                    + node.getDockerPort()
                    + "/containers/"
                    + server.getDockerContainerId()
                    + "/stop");
        }

        // Send the POST request
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(
                Entity.entity(Json.createObjectBuilder().build(), MediaType.APPLICATION_JSON));

        // Check status of response and act on result, ignoring 304 (NOT MODIFIED)
        Response.StatusType responseStatus = response.getStatusInfo();
        if (!responseStatus.getFamily().equals(Response.Status.Family.SUCCESSFUL)
                && responseStatus.getStatusCode() != Response.Status.NOT_MODIFIED.getStatusCode()) {
            adminCommandContext.getActionReport().failure(adminCommandContext.getLogger(),
                    "Failed to stop Docker Container: \n" + responseStatus.getReasonPhrase());
        }
    }
}
