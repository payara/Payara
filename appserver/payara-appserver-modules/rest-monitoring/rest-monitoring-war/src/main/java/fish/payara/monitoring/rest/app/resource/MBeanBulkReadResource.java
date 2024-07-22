/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.rest.app.resource;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import fish.payara.monitoring.rest.app.handler.MBeanAttributeReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanAttributesReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanReadHandler;
import fish.payara.monitoring.rest.app.handler.ReadHandler;

/**
 * @author Krassimir Valev
 */
@Path("")
@RequestScoped
public class MBeanBulkReadResource {

    @Inject
    private MBeanServerDelegate mDelegate;

    /**
     * Returns the {@link String} form of the {@link JSONObject} resource from the ResourceHandler.
     *
     * @param content
     *            The JSON request payload, describing the beans and attributes to read.
     * @return The {@link String} representation of the MBeanRead/MBeanAttributeRead {@link JSONObject}.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String getReadResource(final String content) {
        try (JsonReader reader = Json.createReader(new StringReader(content))) {
            // the payload can be either a single request or a bulk one (array)
            JsonStructure struct = reader.read();
            switch (struct.getValueType()) {
                case ARRAY:
                    List<JsonObject> objects = struct.asJsonArray().stream()
                            .map(value -> handleRequest(value.asJsonObject()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());

                    JsonArrayBuilder builder = Json.createArrayBuilder();
                    for (JsonObject jsonObject : objects) {
                        builder.add(jsonObject);
                    }

                    return builder.build().toString();
                case OBJECT:
                    return handleRequest(struct.asJsonObject()).orElse(JsonValue.EMPTY_JSON_OBJECT).toString();
                default:
                    return "invalid JSON structure";
            }
        }
    }

    private Optional<JsonObject> handleRequest(final JsonObject jsonObject) {
        // ignore non-read requests
        String type = jsonObject.getString("type", "");
        if (!"read".equalsIgnoreCase(type)) {
            return Optional.empty();
        }

        String mbean = jsonObject.getString("mbean", "");
        JsonValue attributes = jsonObject.getOrDefault("attribute", JsonValue.NULL);
        ReadHandler handler = getReadHandler(mbean, attributes);
        return Optional.of(handler.getResource());
    }

    private ReadHandler getReadHandler(final String mbean, final JsonValue attributes) {
        // attributes can be null, a string or a list of strings
        switch (attributes.getValueType()) {
            case ARRAY:
                String[] attributeNames = attributes.asJsonArray().stream()
                        .map(v -> ((JsonString) v).getString())
                        .toArray(String[]::new);
                return new MBeanAttributesReadHandler(mDelegate, mbean, attributeNames);
            case STRING:
                String attribute = ((JsonString) attributes).getString();
                return new MBeanAttributeReadHandler(mDelegate, mbean, attribute);
            default:
                return new MBeanReadHandler(mDelegate, mbean);
        }
    }

}
