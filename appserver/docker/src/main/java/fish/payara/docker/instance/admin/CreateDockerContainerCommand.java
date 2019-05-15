package fish.payara.docker.instance.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import fish.payara.docker.DockerConstants;
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
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;

@Service(name = "_create-docker-container")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.POST,
                path = "_create-docker-container",
                description = "Create a Docker Container and Instance on the specified nodeName")
})
public class CreateDockerContainerCommand implements AdminCommand {

    private static final Logger logger = Logger.getLogger(CreateDockerContainerCommand.class.getName());

    @Param(name = "nodeName", alias = "node")
    String nodeName;

    @Param(name = "instanceName", alias = "instance", primary = true)
    String instanceName;

    @Inject
    private Servers servers;

    @Inject
    private Nodes nodes;

    @Inject
    private CommandRunner commandRunner;

    private Properties containerConfig;
    private List<String> processedProperties;

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

        if (dasHost == null || dasHost.equals("") || dasPort == null || dasPort.equals("")) {
            actionReport.failure(logger, "Could not retrieve DAS host address or port");
            return;
        }

        // Get the instance that we've got registered in the domain.xml to grab its config
        Server server = servers.getServer(instanceName);
        if (server == null) {
            actionReport.failure(logger, "No instance registered in domain with name: " + instanceName);
            return;
        }

        containerConfig = new Properties();

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
        JsonObject jsonObject = constructJsonRequest(node, dasHost, dasPort);

        // Create web target with query
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = null;
        if (Boolean.valueOf(node.getUseTls())) {
            webTarget = client.target("https://"
                    + node.getNodeHost()
                    + ":"
                    + node.getDockerPort()
                    + "/containers/create");
        } else {
            webTarget = client.target("http://"
                    + node.getNodeHost()
                    + ":"
                    + node.getDockerPort()
                    + "/containers/create");
        }
        webTarget = webTarget.queryParam(DockerConstants.DOCKER_NAME_KEY, instanceName);

        // Send the POST request
        Response response = null;
        try {
            response = webTarget.queryParam(DockerConstants.DOCKER_NAME_KEY, instanceName)
                    .request(MediaType.APPLICATION_JSON).post(Entity.entity(jsonObject, MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Encountered an exception sending request to Docker: \n", ex);
        }

        // Check status of response and act on result
        if (response != null) {
            Response.StatusType responseStatus = response.getStatusInfo();
            if (!responseStatus.getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
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

    private JsonObject constructJsonRequest(Node node, String dasHost, String dasPort) {
        JsonObjectBuilder rootObjectBuilder = Json.createObjectBuilder();

        rootObjectBuilder.add(DockerConstants.DOCKER_IMAGE_KEY, node.getDockerImage());

        // If no user properties specified, go with defaults
        if (containerConfig.isEmpty()) {
            rootObjectBuilder.add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                    .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                    .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                                    .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                    .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                    .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"));
            rootObjectBuilder.add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                    .add(DockerConstants.PAYARA_DAS_HOST + "=" + dasHost)
                    .add(DockerConstants.PAYARA_DAS_PORT + "=" + dasPort)
                    .add(DockerConstants.PAYARA_NODE_NAME + "=" + nodeName)
                    .add(DockerConstants.INSTANCE_NAME + "=" + instanceName));
        } else {
            translatePropertyValuesToJson(rootObjectBuilder, dasHost, dasPort);
        }

        return rootObjectBuilder.build();
    }

    private JsonObjectBuilder translatePropertyValuesToJson(JsonObjectBuilder rootObjectBuilder, String dasHost,
                                                            String dasPort) {
        processedProperties = new ArrayList<>();
        boolean hostConfigAdded = false;
        boolean envConfigAdded = false;

        for (String property : containerConfig.stringPropertyNames()) {
            if (processedProperties.contains(property)) {
                continue;
            }

            // Check if any of the properties are in the same namespace as our defaults
            if (property.startsWith(DockerConstants.DOCKER_HOST_CONFIG_KEY)) {
                hostConfigAdded = true;
                addHostConfigProperties(rootObjectBuilder);
                continue;
            } else if (property.startsWith(DockerConstants.DOCKER_CONTAINER_ENV)) {
                envConfigAdded = true;
                addEnvProperties(rootObjectBuilder, dasHost, dasPort);
                continue;
            }

            // Check if this is a nested property
            if (property.contains(".")) {
                // Recurse through the properties and add any other properties that fall under the same namespace
                addNestedProperties(rootObjectBuilder, property);
            } else {
                // Not a nested property, add it as is
                String propertyValue = containerConfig.getProperty(property);
                addPropertyToJson(rootObjectBuilder, property, propertyValue);
                processedProperties.add(property);
            }
        }

        if (!hostConfigAdded) {
            rootObjectBuilder.add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                    .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                    .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, nodes.getNode(nodeName).getDockerPasswordFile())
                                    .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                    .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                    .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"));
        }

        if (!envConfigAdded) {
            rootObjectBuilder.add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                    .add(DockerConstants.PAYARA_DAS_HOST + "=" + dasHost)
                    .add(DockerConstants.PAYARA_DAS_PORT + "=" + dasPort)
                    .add(DockerConstants.PAYARA_NODE_NAME + "=" + nodeName)
                    .add(DockerConstants.INSTANCE_NAME + "=" + instanceName));
        }

        return rootObjectBuilder;
    }

    private void addHostConfigProperties(JsonObjectBuilder rootObjectBuilder) {
        JsonObjectBuilder hostConfigObjectBuilder = Json.createObjectBuilder();
        List<String> hostConfigProperties = new ArrayList<>();
        for (String property : containerConfig.stringPropertyNames()) {
            if (property.startsWith(DockerConstants.DOCKER_HOST_CONFIG_KEY)) {
                hostConfigProperties.add(property);
            }
        }

        // Sort them into alphabetical order to group them all related properties together
        hostConfigProperties.sort(Comparator.comparing(String::toString));

        // Populate HostConfig defaults map so we can check if any get overridden
        Map<String, Boolean> defaultsOverridden = new HashMap<>();
        defaultsOverridden.put(DockerConstants.DOCKER_MOUNTS_KEY, false);
        defaultsOverridden.put(DockerConstants.DOCKER_NETWORK_MODE_KEY, false);

        loopOverNestedProperties(rootObjectBuilder, hostConfigObjectBuilder, hostConfigProperties, defaultsOverridden);

        // Add any remaining defaults
        if (!defaultsOverridden.get(DockerConstants.DOCKER_MOUNTS_KEY)) {
            hostConfigObjectBuilder.add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                            .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, nodes.getNode(nodeName).getDockerPasswordFile())
                            .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                            .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)));
        }

        if (!defaultsOverridden.get(DockerConstants.DOCKER_NETWORK_MODE_KEY)) {
            hostConfigObjectBuilder.add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host");
        }

        // Finally, add host config object to final Json request object
        rootObjectBuilder.add(DockerConstants.DOCKER_HOST_CONFIG_KEY, hostConfigObjectBuilder);
    }

    private void addNestedProperties(JsonObjectBuilder rootObjectBuilder, String originalProperty) {
        JsonObjectBuilder topLevelObjectBuilder = Json.createObjectBuilder();
        List<String> nestedProperties = new ArrayList<>();
        String topLevelProperty = originalProperty.substring(0, originalProperty.indexOf("."));
        for (String property : containerConfig.stringPropertyNames()) {
            if (property.startsWith(topLevelProperty)) {
                nestedProperties.add(property);
            }
        }

        // Sort them into alphabetical order to group them all related properties together
        nestedProperties.sort(Comparator.comparing(String::toString));

        // Loop over nested properties and add to Json
        loopOverNestedProperties(rootObjectBuilder, topLevelObjectBuilder, nestedProperties, null);

        // Finally, add top level object builder to root Json request object builder
        rootObjectBuilder.add(topLevelProperty, topLevelObjectBuilder);
    }

    private void addEnvProperties(JsonObjectBuilder jsonObjectBuilder, String dasHost, String dasPort) {
        String envConfigString = containerConfig.getProperty(DockerConstants.DOCKER_CONTAINER_ENV);

        if (!envConfigString.contains(DockerConstants.PAYARA_DAS_HOST)) {
            envConfigString += "|" + DockerConstants.PAYARA_DAS_HOST + "=" + dasHost;
        }

        if (envConfigString.contains(DockerConstants.PAYARA_DAS_PORT)) {
            envConfigString += "|" + DockerConstants.PAYARA_DAS_PORT + "=" + dasPort;
        }

        if (envConfigString.contains(DockerConstants.PAYARA_NODE_NAME)) {
            envConfigString += "|" + DockerConstants.PAYARA_NODE_NAME + "=" + nodeName;
        }

        if (envConfigString.contains(DockerConstants.INSTANCE_NAME)) {
            envConfigString += "|" + DockerConstants.INSTANCE_NAME + "=" + instanceName;
        }

        // We can't currently have '=' in a system property value, so for this special case substitute ':' as we
        // add to Json
        envConfigString = envConfigString.replaceAll(":", "=");

        addPropertyToJson(jsonObjectBuilder, DockerConstants.DOCKER_CONTAINER_ENV, envConfigString);
    }

    private void loopOverNestedProperties(JsonObjectBuilder rootObjectBuilder, JsonObjectBuilder topLevelObjectBuilder,
            List<String> sortedNestedProperties, Map<String, Boolean> defaultsOverridden) {
        for (String property : sortedNestedProperties) {
            if (processedProperties.contains(property)) {
                continue;
            }

            if (defaultsOverridden != null) {
                // Check if property overrides any of our defaults
                switch (property) {
                    case DockerConstants.DOCKER_HOST_CONFIG_KEY + "." + DockerConstants.DOCKER_MOUNTS_KEY:
                        defaultsOverridden.put(DockerConstants.DOCKER_MOUNTS_KEY, true);
                        break;
                    case DockerConstants.DOCKER_HOST_CONFIG_KEY + "." + DockerConstants.DOCKER_NETWORK_MODE_KEY:
                        defaultsOverridden.put(DockerConstants.DOCKER_NETWORK_MODE_KEY, true);
                        break;
                }
            }

            Map<String, JsonObjectBuilder> propertyComponentObjectBuilders = new HashMap<>();
            propertyComponentObjectBuilders.put(DockerConstants.DOCKER_HOST_CONFIG_KEY, topLevelObjectBuilder);
            recurseOverNested(rootObjectBuilder, sortedNestedProperties, property, propertyComponentObjectBuilders, null);
        }
    }

    private void recurseOverNested(JsonObjectBuilder jsonObjectBuilder, List<String> sortedProperties,
                                                String property, Map<String, JsonObjectBuilder> propertyComponentObjectBuilders, String parent) {
        List<String> propertyComponents = Arrays.asList(property.split("\\."));

        for (String propertyComponent : propertyComponents) {
            // We don't need to make a builder for the last component, as it isn't an object
            if (propertyComponents.indexOf(propertyComponent) != propertyComponents.size() - 1) {
                propertyComponentObjectBuilders.putIfAbsent(propertyComponent, Json.createObjectBuilder());
            }
        }

        // Add lowest level property component to immediate parent builder (second last in list)
        String immediateParent = propertyComponents.get(propertyComponents.size() - 2);

        JsonObjectBuilder immediateParentObjectBuilder;
        if (parent != null && immediateParent.equals(parent)) {
            immediateParentObjectBuilder = jsonObjectBuilder;
        } else {
            immediateParentObjectBuilder = propertyComponentObjectBuilders.get(immediateParent);
        }
        String propertyComponentKey = propertyComponents.get(propertyComponents.size() - 1);
        String propertyValue = containerConfig.getProperty(property);
        addPropertyToJson(immediateParentObjectBuilder, propertyComponentKey, propertyValue);
        processedProperties.add(property);

        // If there are more properties, check if the immediate parent has any extra children by checking the next property,
        // moving up the list until we reach the root property component
        if (sortedProperties.indexOf(property) + 1 != sortedProperties.size()) {
            String nextProperty = sortedProperties.get(sortedProperties.indexOf(property) + 1);
            for (int i = propertyComponents.size() - 2; i > -1; i--) {
                String remainingParents = "";
                for (int j = 0; j < i + 1; j++) {
                    remainingParents += propertyComponents.get(j);

                    if (j != i) {
                        remainingParents += ".";
                    }
                }

                if (nextProperty.startsWith(remainingParents)) {
                    // We've found a property in the same namespace, recurse into this method to add this next property
                    // to the object builder
                    recurseOverNested(
                            propertyComponentObjectBuilders.get(propertyComponents.get(i)),
                            sortedProperties,
                            nextProperty, propertyComponentObjectBuilders, immediateParent);
                    // We don't want to keep looping as we'll end up adding stuff added in the recursed method call
                    // above
                    break;
                } else {
                    if (i != 0) {
                        // If we haven't found another property in the same namespace, add the current object builder to its parent
                        JsonObjectBuilder parentObjectBuilder = propertyComponentObjectBuilders.get(propertyComponents.get(i - 1));
                        parentObjectBuilder.add(propertyComponents.get(i), propertyComponentObjectBuilders.get(propertyComponents.get(i)));
                        propertyComponentObjectBuilders.remove(propertyComponents.get(i));
                    }
                }
            }
        } else {
            // If there are no more properties, make sure to add the last object builder to its parent
            // Only do so if it's more than two levels deep though, as otherwise we've already added it
            if (propertyComponents.size() > 2) {
                propertyComponentObjectBuilders.get(propertyComponents.get(0)).add(propertyComponents.get(1), propertyComponentObjectBuilders.get(propertyComponents.get(1)));
                propertyComponentObjectBuilders.remove(propertyComponents.get(1));
            }
        }
    }

    private void addPropertyToJson(JsonObjectBuilder jsonObjectBuilder, String property, String propertyValue) {
        if (propertyValue.startsWith("[") && propertyValue.endsWith("]")) {
            propertyValue = propertyValue.replaceAll("\\[", "").replaceAll("\\]", "");
            // If it is an array, check if there are objects in this array that we need to deal with
            if (propertyValue.contains(",")) {
                // We have the split operator for an array and an object, the Docker Rest API does not currently have
                // any Arrays of Objects with in turn contain arrays or further objects
                JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
                for (String arrayElement : propertyValue.split("\\|")) {
                    JsonObjectBuilder arrayObjectBuilder = Json.createObjectBuilder();
                    for (String object : arrayElement.split(",")) {
                        String[] keyValue = object.split(":");
                        arrayObjectBuilder.add(keyValue[0], keyValue[1]);
                    }
                    jsonArrayBuilder.add(arrayObjectBuilder);
                }

                jsonObjectBuilder.add(property, jsonArrayBuilder);
            } else {
                // Just an array
                JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
                for (String arrayElement : propertyValue.split("\\|")) {
                    jsonArrayBuilder.add(arrayElement);
                }
                jsonObjectBuilder.add(property, jsonArrayBuilder);
            }
        } else {
            // Just a value
            jsonObjectBuilder.add(property, propertyValue);
        }
    }

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
