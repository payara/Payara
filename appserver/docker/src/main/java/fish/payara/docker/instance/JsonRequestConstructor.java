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

package fish.payara.docker.instance;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.docker.DockerConstants;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Helper class that constructs the JSON requests used for creating Docker Containers.
 *
 * @author Andrew Pielage
 */
public class JsonRequestConstructor {

    /**
     * Builds the Json Object from all supplied configuration to send to Docker.
     *
     * @param node The Payara Server node
     * @param dasHost The IP address of the DAS
     * @param dasPort The admin port of the DAS
     * @return Json Object representing all supplied and default Docker container configuration.
     */
    public static JsonObject constructJsonRequest(Properties containerConfig, Node node, Server server,
            String dasHost, String dasPort) {
        JsonObjectBuilder rootObjectBuilder = Json.createObjectBuilder();

        // Add the image straight away - this is never overridden
        rootObjectBuilder.add(DockerConstants.DOCKER_IMAGE_KEY, node.getDockerImage());

        // If no user properties specified, go with defaults, otherwise go over the system properties and add them to
        // the Json object
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
                    .add(DockerConstants.PAYARA_NODE_NAME + "=" + node.getName())
                    .add(DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef())
                    .add(DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName()));
        } else {
            translatePropertyValuesToJson(rootObjectBuilder, containerConfig, node, server,
                    dasHost, dasPort);
        }

        return rootObjectBuilder.build();
    }

    /**
     * Go over all system properties and add them to the Json object.
     *
     * @param rootObjectBuilder The top-level object builder that will contain all Docker configuration
     * @param dasHost The IP address that the DAS is running on
     * @param dasPort The admin port of the DAS
     */
    private static void translatePropertyValuesToJson(JsonObjectBuilder rootObjectBuilder,
            Properties containerConfig, Node node, Server server,
            String dasHost, String dasPort) {
        List<String> processedProperties = new ArrayList<>();
        boolean hostConfigAdded = false;
        boolean envConfigAdded = false;


        for (String property : containerConfig.stringPropertyNames()) {
            // As we recurse over nested properties, we add the processed ones to this list, so check that we're
            // not going to process the same property twice
            if (processedProperties.contains(property)) {
                continue;
            }

            // If the property is in the same namespace as our defaults, handle them here
            if (property.startsWith(DockerConstants.DOCKER_HOST_CONFIG_KEY)) {
                hostConfigAdded = true;
                addHostConfigProperties(rootObjectBuilder, containerConfig, processedProperties, node);
                continue;
            } else if (property.startsWith(DockerConstants.DOCKER_CONTAINER_ENV)) {
                envConfigAdded = true;
                addEnvProperties(rootObjectBuilder, containerConfig, node, server, dasHost, dasPort);
                continue;
            }

            // Check if this is a nested property
            if (property.contains(".")) {
                // Recurse through the properties and add any other properties that fall under the same namespace
                addNestedProperties(rootObjectBuilder, containerConfig, processedProperties, property);
            } else {
                // Not a nested property, add it as a plain key:value
                String propertyValue = containerConfig.getProperty(property);
                addPropertyToJson(rootObjectBuilder, property, propertyValue);
                processedProperties.add(property);
            }
        }

        // If we haven't added any HostConfig or Env settings, add the defaults here
        if (!hostConfigAdded) {
            rootObjectBuilder.add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                    .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                    .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                                    .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                    .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                    .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"));
        }
        if (!envConfigAdded) {
            rootObjectBuilder.add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                    .add(DockerConstants.PAYARA_DAS_HOST + "=" + dasHost)
                    .add(DockerConstants.PAYARA_DAS_PORT + "=" + dasPort)
                    .add(DockerConstants.PAYARA_NODE_NAME + "=" + node.getName())
                    .add(DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef())
                    .add(DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName()));
        }
    }

    /**
     * Loops over nested properties in the 'HostConfig' namespace and adds them to the Json builder.
     * @param rootObjectBuilder The top-level Json builder
     */
    private static void addHostConfigProperties(JsonObjectBuilder rootObjectBuilder, Properties containerConfig,
            List<String> processedProperties, Node node) {
        JsonObjectBuilder hostConfigObjectBuilder = Json.createObjectBuilder();

        // Populate HostConfig defaults map so we can check if any get overridden
        Map<String, Boolean> defaultsOverridden = new HashMap<>();
        defaultsOverridden.put(DockerConstants.DOCKER_MOUNTS_KEY, false);
        defaultsOverridden.put(DockerConstants.DOCKER_NETWORK_MODE_KEY, false);

        // Loop over all properties and add to HostConfig Json builder
        loopOverNestedProperties(rootObjectBuilder, containerConfig, processedProperties,
                DockerConstants.DOCKER_HOST_CONFIG_KEY, hostConfigObjectBuilder, defaultsOverridden);

        // Add any remaining defaults
        if (!defaultsOverridden.get(DockerConstants.DOCKER_MOUNTS_KEY)) {
            hostConfigObjectBuilder.add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                            .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                            .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                            .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)));
        }
        if (!defaultsOverridden.get(DockerConstants.DOCKER_NETWORK_MODE_KEY)) {
            hostConfigObjectBuilder.add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host");
        }

        // Finally, add host config object to final Json request object
        rootObjectBuilder.add(DockerConstants.DOCKER_HOST_CONFIG_KEY, hostConfigObjectBuilder);
    }

    /**
     * Generic version of addHostConfigProperties, that loops over nested properties namespace and adds them to
     * the Json builder.
     * @param rootObjectBuilder The root-level Json builder
     * @param originalProperty The first property that we found in this namespace
     */
    private static void addNestedProperties(JsonObjectBuilder rootObjectBuilder, Properties containerConfig,
            List<String> processedProperties, String originalProperty) {
        JsonObjectBuilder topLevelObjectBuilder = Json.createObjectBuilder();
        String topLevelProperty = originalProperty.substring(0, originalProperty.indexOf('.'));

        // Loop over nested properties and add to Json
        loopOverNestedProperties(rootObjectBuilder, containerConfig, processedProperties, topLevelProperty,
                topLevelObjectBuilder, null);

        // Finally, add top level object builder to root Json request object builder
        rootObjectBuilder.add(topLevelProperty, topLevelObjectBuilder);
    }

    /**
     * Adds the Env array properties to the root Json builder
     * @param rootObjectBuilder The top-level Json builder
     * @param dasHost The IP address that the DAS is situated on
     * @param dasPort The admin port of the DAS
     */
    private static void addEnvProperties(JsonObjectBuilder rootObjectBuilder, Properties containerConfig,
            Node node, Server server, String dasHost, String dasPort) {
        String envConfigString = containerConfig.getProperty(DockerConstants.DOCKER_CONTAINER_ENV)
                .replaceAll("\\[", "")
                .replaceAll("\\]", "");

        // Check if we need to add any of our defaults
        if (!envConfigString.contains(DockerConstants.PAYARA_DAS_HOST)) {
            envConfigString += "|" + DockerConstants.PAYARA_DAS_HOST + "=" + dasHost;
        }
        if (!envConfigString.contains(DockerConstants.PAYARA_DAS_PORT)) {
            envConfigString += "|" + DockerConstants.PAYARA_DAS_PORT + "=" + dasPort;
        }
        if (!envConfigString.contains(DockerConstants.PAYARA_NODE_NAME)) {
            envConfigString += "|" + DockerConstants.PAYARA_NODE_NAME + "=" + node.getName();
        }
        if (!envConfigString.contains(DockerConstants.PAYARA_CONFIG_NAME)) {
            envConfigString += "|" + DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef();
        }
        if (!envConfigString.contains(DockerConstants.PAYARA_INSTANCE_NAME)) {
            envConfigString += "|" + DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName();
        }

        // We can't currently have '=' in a system property value, so for this special case substitute ':' as we
        // add to Json
        envConfigString = envConfigString.replaceAll(":", "=");

        // Add the surround square brackets around the String again so that it gets detected as an Array
        envConfigString = "[" + envConfigString + "]";

        // Finally, add to top-level Json builder
        addPropertyToJson(rootObjectBuilder, DockerConstants.DOCKER_CONTAINER_ENV, envConfigString);
    }

    /**
     * Loops over all nested properties in a given namespace and adds them to the top-level builder of said namespace
     *
     * @param rootObjectBuilder The top level object builder
     * @param topLevelObjectBuilder The object builder of the top level component of the property
     * @param defaultsOverridden The map of booleans for if a default has been overridden
     */
    private static void loopOverNestedProperties(JsonObjectBuilder rootObjectBuilder, Properties containerConfig,
            List<String> processedProperties, String topLevelProperty,
            JsonObjectBuilder topLevelObjectBuilder, Map<String, Boolean> defaultsOverridden) {
        // Gather all properties in the same namespace as the top level property
        List<String> nestedProperties = new ArrayList<>();
        for (String property : containerConfig.stringPropertyNames()) {
            if (property.startsWith(topLevelProperty)) {
                nestedProperties.add(property);
            }
        }

        // Sort them into alphabetical order to group them all related properties together
        nestedProperties.sort(Comparator.comparing(String::toString));

        for (String property : nestedProperties) {
            // Only process if we haven't already
            if (processedProperties.contains(property)) {
                continue;
            }

            // Check if property overrides any of our defaults
            if (defaultsOverridden != null) {
                switch (property) {
                    case DockerConstants.DOCKER_HOST_CONFIG_KEY + "." + DockerConstants.DOCKER_MOUNTS_KEY:
                        defaultsOverridden.put(DockerConstants.DOCKER_MOUNTS_KEY, true);
                        break;
                    case DockerConstants.DOCKER_HOST_CONFIG_KEY + "." + DockerConstants.DOCKER_NETWORK_MODE_KEY:
                        defaultsOverridden.put(DockerConstants.DOCKER_NETWORK_MODE_KEY, true);
                        break;
                }
            }

            // Create a Map of Json builders for each level of the property
            Map<String, JsonObjectBuilder> propertyComponentObjectBuilders = new HashMap<>();
            propertyComponentObjectBuilders.put(topLevelProperty, topLevelObjectBuilder);

            // Recurse over the namespace and add all of them to the Json builders
            recurseOverNested(rootObjectBuilder, containerConfig, processedProperties, nestedProperties, property,
                    propertyComponentObjectBuilders, null);
        }
    }

    /**
     * Recurses over all properties in a given namespace, and adds them all to their Json builders
     * @param parentObjectBuilder The Json object builder of the parent property
     * @param nestedProperties The list of sorted properties to recurse over
     * @param property The property to add to Json
     * @param propertyComponentObjectBuilders The map of Json builders still under construction
     * @param parent The parent component property
     */
    private static void recurseOverNested(JsonObjectBuilder parentObjectBuilder, Properties containerConfig,
            List<String> processedProperties, List<String> nestedProperties, String property,
            Map<String, JsonObjectBuilder> propertyComponentObjectBuilders, String parent) {
        List<String> propertyComponents = Arrays.asList(property.split("\\."));

        // Check if we need to create any more Object Builders
        for (String propertyComponent : propertyComponents) {
            // We don't need to make a builder for the last component, as it isn't an object, it's a value
            if (propertyComponents.indexOf(propertyComponent) != propertyComponents.size() - 1) {
                propertyComponentObjectBuilders.putIfAbsent(propertyComponent, Json.createObjectBuilder());
            }
        }

        // Add lowest level property component to immediate parent builder (second last in list)
        String immediateParent = propertyComponents.get(propertyComponents.size() - 2);

        // Use the passed in object builder if the immediate parent is the same as the previous property
        JsonObjectBuilder immediateParentObjectBuilder;
        if (immediateParent.equals(parent)) {
            immediateParentObjectBuilder = parentObjectBuilder;
        } else {
            immediateParentObjectBuilder = propertyComponentObjectBuilders.get(immediateParent);
        }

        // Add the property to the Json builder
        String propertyComponentKey = propertyComponents.get(propertyComponents.size() - 1);
        String propertyValue = containerConfig.getProperty(property);
        addPropertyToJson(immediateParentObjectBuilder, propertyComponentKey, propertyValue);
        processedProperties.add(property);

        // If there are more properties, check if each parent has any extra children
        if (nestedProperties.indexOf(property) + 1 != nestedProperties.size()) {
            // Get the next property in the list
            String nextProperty = nestedProperties.get(nestedProperties.indexOf(property) + 1);

            // For each parent component in the property (e.g. fee & fih & foh for the property fee.fih.foh.fum)
            for (int i = propertyComponents.size() - 2; i > 0; i--) {
                // Build a string of all remaining parents (so 1st run would be fee.fih.foh, 2nd would be fee.fih etc.)
                StringBuffer parents = new StringBuffer();
                for (int j = 0; j < i + 1; j++) {
                    parents.append(propertyComponents.get(j));

                    if (j != i) {
                        parents.append(".");
                    }
                }

                // Check if the next property is at the same level in the namespace,
                // or if we need to go further up the namespace
                if (nextProperty.startsWith(parents.toString())) {
                    // We've found a property at the same level in the namespace,
                    // recurse into this method to add this next property to the same object builder
                    recurseOverNested(
                            propertyComponentObjectBuilders.get(propertyComponents.get(i)),
                            containerConfig, processedProperties, nestedProperties, nextProperty,
                            propertyComponentObjectBuilders, immediateParent);
                    // We don't want to keep looping as we'll end up adding stuff added in the recursive method call
                    // above
                    break;
                } else {
                    if (i != 0) {
                        // If we haven't found another property in the same namespace, add the current object builder
                        // to its parent
                        JsonObjectBuilder parentPropertyComponentObjectBuilder = propertyComponentObjectBuilders.get(
                                propertyComponents.get(i - 1));
                        parentPropertyComponentObjectBuilder.add(propertyComponents.get(i),
                                propertyComponentObjectBuilders.get(propertyComponents.get(i)));
                        propertyComponentObjectBuilders.remove(propertyComponents.get(i));
                    }
                }
            }
        } else {
            // If there are no more properties, make sure to add the remaining object builders to their parents
            // Only do so if it's more than two levels deep though, as otherwise we've already added it
            if (propertyComponents.size() > 2) {
                for (int i = propertyComponents.size() - 2; i > 0; i--) {
                    propertyComponentObjectBuilders.get(propertyComponents.get(i - 1))
                            .add(propertyComponents.get(i),
                                    propertyComponentObjectBuilders.get(propertyComponents.get(i)));
                    propertyComponentObjectBuilders.remove(
                            propertyComponents.get(propertyComponents.indexOf(immediateParent)));
                }
            }
        }
    }

    /**
     * Adds the given property and property value to the provided JsonObjectBuilder
     *
     * @param jsonObjectBuilder The object builder to add the property to
     * @param property The name of the property to add
     * @param propertyValue The value of the property to add
     */
    private static void addPropertyToJson(JsonObjectBuilder jsonObjectBuilder, String property, String propertyValue) {
        // Check for array
        if (propertyValue.startsWith("[") && propertyValue.endsWith("]")) {
            propertyValue = propertyValue.replaceAll("\\[", "").replaceAll("\\]", "");
            // If it is an array, check if there are objects in this array that we need to deal with
            if (propertyValue.contains(",")) {
                // We have the split operator for an array and an object, so assume it is an array of objects as
                // objects with arrays are added differently
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
}
