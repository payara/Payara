/**
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
package fish.payara.monitoring.rest.app.resource;

import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import fish.payara.monitoring.rest.app.handler.MBeanAttributeReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanAttributesReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanReadHandler;
import fish.payara.monitoring.rest.app.handler.ReadHandler;
import fish.payara.monitoring.rest.app.handler.ResourceHandler;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.*;

/**
 * @author Krassimir Valev
 */
@Path("")
@RequestScoped
public class MBeanBulkReadResource {

    @Inject
    private MBeanServerDelegate mDelegate;

    /**
     * Returns the {@link String} form of the {@link JSONObject} resource from
     * the ResourceHandler.
     *
     * @param content The JSON request payload, describing the beans and
     * attributes to read.
     * @return The {@link String} representation of the
     * MBeanRead/MBeanAttributeRead {@link JSONObject}.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String getReadResource(final String content) throws JSONException {

        if (content != null && !content.trim().isEmpty() && isJsonValid(content)) {
            Object jsonTokener = new JSONTokener(content).nextValue();

            if (jsonTokener instanceof JSONObject) {
                JSONObject jsonObject = new JSONObject(content);

                try {
                    JSONObject request = handleRequest(jsonObject);
                    if (request != null) {
                        return request.toString();
                    } else {
                        return "{}";
                    }
                } catch (NullPointerException ex) {
                     Logger.getLogger(ResourceHandler.class.getName()).log(Level.CONFIG, "NullPointerException when getting Read resources", ex);
                }
            } else if (jsonTokener instanceof JSONArray) {
                List<JSONObject> objects = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(content);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = handleRequest(jsonArray.getJSONObject(i));
                    if (jsonObject != null) {
                        objects.add(jsonObject);
                    }
                }

                JSONArray resource = new JSONArray();

                for (JSONObject jsonObject : objects) {
                    resource.put(jsonObject);
                }

                return resource.toString();
            }
        }

        return "invalid JSON";
    }

    private JSONObject handleRequest(final JSONObject jsonObject) throws JSONException {
        // ignore non-read requests
        String type = jsonObject.optString("type");
        if (!"read".equalsIgnoreCase(type)) {
            return null;
        }

        String mbean = jsonObject.optString("mbean");
        String attributes = jsonObject.optString("attribute");
        ReadHandler handler = getReadHandler(mbean, attributes);
        return handler.getResource();
    }

    private ReadHandler getReadHandler(final String mbean, final String attributes) throws JSONException {
        // attributes can be null, a string or a list of strings
        if (attributes != null && !attributes.trim().isEmpty()) {
            Object jsonTokener = new JSONTokener(attributes).nextValue();
            if (jsonTokener instanceof JSONArray) {
                List<String> attributeNames = new ArrayList<>();

                JSONArray jsonArray = new JSONArray(attributes);

                for (int i = 0; i < jsonArray.length(); i++) {
                    attributeNames.add(jsonArray.get(i).toString());
                }
                return new MBeanAttributesReadHandler(mDelegate, mbean, attributeNames.toArray(new String[0]));

            } else if (jsonTokener instanceof String) {
                return new MBeanAttributeReadHandler(mDelegate, mbean, attributes);
            }
        }

        return new MBeanReadHandler(mDelegate, mbean);
    }

    private boolean isJsonValid(String jsonString) {
        try {
            new JSONObject(jsonString);
        } catch (JSONException jsonObjectException) {
            try {
                new JSONArray(jsonString);
            } catch (JSONException jsonArrayException) {
                return false;
            }
        }
        return true;
    }

}
