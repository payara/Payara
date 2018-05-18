/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.processor;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.normaliseUrl;

import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;
import fish.payara.microprofile.openapi.impl.model.info.InfoImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;

/**
 * A processor to apply any configuration options to the model, and fill any
 * required but currently empty values.
 */
public class BaseProcessor implements OASProcessor {

    private final String applicationPath;

    public BaseProcessor(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {

        // Set the OpenAPI version if it hasn't been set
        if (api.getOpenapi() == null) {
            api.setOpenapi("3.0.0");
        }

        // Set the info if it hasn't been set
        if (api.getInfo() == null) {
            api.setInfo(new InfoImpl().title("Deployed Resources").version("1.0.0"));
        }

        // Add the config specified servers
        if (config != null && config.getServers().size() > 0) {
            // Clear all the other servers
            api.getServers().clear();
            // Add all the specified ones
            config.getServers().forEach(serverUrl -> api.addServer(new ServerImpl().url(serverUrl)));
        }

        // Add the default server if there are none
        if (api.getServers().size() == 0) {
            api.addServer(
                    new ServerImpl().url("http://localhost:8080" + applicationPath).description("Default Server."));
        }

        // Add the path servers
        if (config != null && !config.getPathServerMap().isEmpty()) {
            for (Entry<String, Set<String>> entry : config.getPathServerMap().entrySet()) {
                // Get the normalised path
                String path = normaliseUrl(entry.getKey());

                // If the path doesn't exist, create it
                if (!api.getPaths().containsKey(path)) {
                    api.getPaths().addPathItem(path, new PathItemImpl());
                }

                // Clear the current list of servers
                api.getPaths().get(path).getServers().clear();

                // Add each url
                for (String serverUrl : entry.getValue()) {
                    api.getPaths().get(path).addServer(new ServerImpl().url(serverUrl));
                }
            }
        }

        // Add the operation servers
        if (config != null && !config.getOperationServerMap().isEmpty()) {
            for (Entry<String, Set<String>> entry : config.getOperationServerMap().entrySet()) {

                // Find the matching operation
                for (PathItem pathItem : api.getPaths().values()) {
                    for (Operation operation : pathItem.readOperations()) {
                        if (operation.getOperationId().equals(entry.getKey())) {

                            // Clear the current list of servers
                            operation.getServers().clear();

                            // Add each server url to the operation
                            for (String serverUrl : entry.getValue()) {
                                operation.addServer(new ServerImpl().url(serverUrl));
                            }
                        }
                    }
                }
            }
        }

        return api;
    }

}