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
import com.sun.enterprise.config.serverbeans.SystemProperty;
import fish.payara.docker.DockerConstants;
import fish.payara.docker.instance.JsonRequestConstructor;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;

/**
 * Internal Asadmin command used to create a Docker container. This is called by the create-instance command.
 *
 * @author Andrew Pielage
 */
@Service(name = "_create-docker-container")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.POST,
                path = "_create-docker-container",
                description = "Create a Docker Container for the defined Instance on the specified nodeName")
})
public class CreateDockerContainerCommand implements AdminCommand {

    private static final Logger logger = Logger.getLogger(CreateDockerContainerCommand.class.getName());

    @Param(name = "nodeName", alias = "node")
    private String nodeName;

    @Param(name = "instanceName", alias = "instance", primary = true)
    private String instanceName;

    @Inject
    private Servers servers;

    @Inject
    private Nodes nodes;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        ActionReport actionReport = adminCommandContext.getActionReport();

        // Get the Node Object and validate
        Node node = nodes.getNode(nodeName);
        if (node == null) {
            actionReport.failure(logger, "No nodeName found with given name: " + nodeName);
            return;
        }

        if (!node.getType().equals("DOCKER")) {
            actionReport.failure(logger, "Node is not of type DOCKER, nodeName is of type: " + node.getType());
            return;
        }

        // Get the DAS hostname and port and validate
        String dasHost = "";
        String dasPort = "";
        for (Server server : servers.getServer()) {
            if (server.isDas()) {
                dasHost = server.getAdminHost();
                dasPort = Integer.toString(server.getAdminPort());
                break;
            }
        }

        if (dasHost == null || dasHost.trim().isEmpty() || dasPort.trim().isEmpty()) {
            actionReport.failure(logger, "Could not retrieve DAS host address or port");
            return;
        }

        // Get the instance that we've got registered in the domain.xml to grab its config
        Server server = servers.getServer(instanceName);
        if (server == null) {
            actionReport.failure(logger, "No instance registered in domain with name: " + instanceName);
            return;
        }

        // Pull the image if it hasn't been downloaded or built yet
        pullImage(node);

        createContainer(adminCommandContext, actionReport, node, server, dasHost, dasPort);
    }

    private void pullImage(Node node) {
        // Create web target with query
        WebTarget webTarget = createWebTarget(node, "/images/create");
        webTarget = webTarget.queryParam(DockerConstants.DOCKER_FROM_IMAGE_KEY, node.getDockerImage());

        // Send the POST request
        Response response = null;
        try {
            response = webTarget.queryParam(DockerConstants.DOCKER_NAME_KEY, instanceName)
                    .request(MediaType.APPLICATION_JSON).post(Entity.entity(JsonObject.EMPTY_JSON_OBJECT,
                            MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Encountered an exception sending request to Docker: \n", ex);
        }

        // Check status of response and act on result
        if (response != null) {
            Response.StatusType responseStatus = response.getStatusInfo();
            if (!responseStatus.getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                // Log the failure
                logger.fine("Failed to pull Docker Image: \n" + responseStatus.getReasonPhrase());
            }
        } else {
            // If the response is null, clearly something has gone wrong, so treat is as a failure
            logger.fine( "Failed to pull Docker Image");
        }
    }

    private void createContainer(AdminCommandContext adminCommandContext, ActionReport actionReport, Node node,
            Server server, String dasHost, String dasPort) {
        Properties containerConfig = new Properties();

        // Add all instance-level system properties, stripping the "Docker." prefix
        for (SystemProperty systemProperty : server.getSystemProperty()) {
            if (systemProperty.getName().startsWith("Docker.")) {
                containerConfig.put(systemProperty.getName().substring(systemProperty.getName().indexOf(".") + 1),
                        systemProperty.getValue());
            }
        }

        // Add Docker system properties from config, making sure not to override any instance-level properties
        for (SystemProperty systemProperty : server.getConfig().getSystemProperty()) {
            if (systemProperty.getName().startsWith("Docker.")) {
                containerConfig.putIfAbsent(
                        systemProperty.getName().substring(systemProperty.getName().indexOf(".") + 1),
                        systemProperty.getValue());
            }
        }

        // Create the JSON Object to send
        JsonObject jsonObject = JsonRequestConstructor.constructJsonRequest(containerConfig, node, server,
                dasHost, dasPort);

        // Create web target with query
        WebTarget webTarget = createWebTarget(node, "/containers/create");
        webTarget = webTarget.queryParam(DockerConstants.DOCKER_NAME_KEY, instanceName);

        // Send the POST request
        Response response = null;
        try {
            response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(jsonObject, MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Encountered an exception sending request to Docker: \n", ex);
        }

        // Check status of response and act on result
        if (response != null) {
            Response.StatusType responseStatus = response.getStatusInfo();
            if (responseStatus.getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                // Read the container ID from the response
                JsonObject jsonResponse = response.readEntity(JsonObject.class);

                if (jsonResponse != null) {
                    String dockerContainerId = jsonResponse.getString("Id");

                    // Set the Docker container ID either to the ID of the container, or to the instance name if the the
                    // ID can't be obtained
                    if (dockerContainerId != null && !dockerContainerId.trim().isEmpty()) {
                        setDockerContainerId(actionReport, server, dockerContainerId);
                    } else {
                        setDockerContainerId(actionReport, server, instanceName);
                    }
                }
            } else {
                // Log the failure
                actionReport.failure(logger, "Failed to create Docker Container: \n"
                        + responseStatus.getReasonPhrase());

                // Attempt to unregister the instance so we don't have an instance entry that can't be used
                unregisterInstance(adminCommandContext, actionReport);
            }
        } else {
            // If the response is null, clearly something has gone wrong, so treat is as a failure
            actionReport.failure(logger, "Failed to create Docker Container");

            // Attempt to unregister the instance so we don't have an instance entry that can't be used
            unregisterInstance(adminCommandContext, actionReport);
        }
    }

    private WebTarget createWebTarget(Node node, String endpoint) {
        Client client = ClientBuilder.newClient();

        WebTarget webTarget = client.target((Boolean.parseBoolean(node.getUseTls()) ? "https://" : "http://")
                + node.getNodeHost()
                + ":"
                + node.getDockerPort()
                + (endpoint.startsWith("/") ? endpoint : "/" + endpoint));

        return webTarget;
    }

    private void setDockerContainerId(ActionReport actionReport, Server server, String dockerContainerId) {
        try {
            ConfigSupport.apply(serverProxy -> {
                serverProxy.setDockerContainerId(dockerContainerId);
                return serverProxy;
            }, server);
        } catch (TransactionFailure transactionFailure) {
            actionReport.failure(logger, "Could not set Docker Container ID for instance", transactionFailure);
        }
    }

    /**
     * Lifecycle helper method that attempts to remove an instance registry if we failed to create the corresponding
     * Docker container
     *
     * @param adminCommandContext
     * @param actionReport
     */
    private void unregisterInstance(AdminCommandContext adminCommandContext, ActionReport actionReport) {
        if (commandRunner != null) {
            actionReport.appendMessage("\n\nWill attempt to unregister instance...");

            ActionReport subActionReport = actionReport.addSubActionsReport();
            CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_unregister-instance",
                    subActionReport, adminCommandContext.getSubject());
            ParameterMap commandParameters = new ParameterMap();
            commandParameters.add("DEFAULT", instanceName);
            commandInvocation.parameters(commandParameters);
            commandInvocation.execute();

            // The unregister instance command doesn't actually log any messages to the asadmin prompt, so let's
            // give a more useful message
            if (subActionReport.getActionExitCode() == SUCCESS) {
                actionReport.appendMessage("\nSuccessfully unregistered instance");
            } else {
                actionReport.appendMessage("\nFailed to unregister instance, user intervention will be required");
            }
        }
    }
}
