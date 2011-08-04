/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package org.glassfish.admin.rest.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

/**
 *
 * @author jasonlee
 */
public abstract class RestClientBase {

    protected static final String RESPONSE_TYPE = MediaType.APPLICATION_JSON;
    protected Map<String, Object> entityValues = new HashMap<String, Object>();
    protected List<String> children;
    protected int status;
    protected String message;
    protected Client client;
    private boolean initialized = false;
    private RestClientBase parent;
    private boolean isNew = false;

    protected RestClientBase(Client c, RestClientBase p) {
        client = c;
        parent = p;
    }

    protected RestClientBase getParent() {
        return parent;
    }

    protected String getRestUrl() {
        return getParent().getRestUrl() + getSegment();
    }

    protected abstract String getSegment();

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean save() {
        ClientResponse response = client.resource(getRestUrl()).accept(RESPONSE_TYPE).post(ClientResponse.class, buildMultivaluedMap(entityValues));
        boolean success = isSuccess(response);

        if (!success) {
            status = response.getStatus();
            message = response.getEntity(String.class);
        } else {
            isNew = false;
        }

        return success;
    }

    public boolean delete() {
        ClientResponse response = client.resource(getRestUrl()).accept(RESPONSE_TYPE).delete(ClientResponse.class);
        boolean success = isSuccess(response);

        if (!success) {
            status = response.getStatus();
            message = response.getEntity(String.class);
        }

        return success;
    }

    public RestResponse execute(Method method, String endpoint) {
        return execute (method, endpoint, new HashMap<String, Object>());
    }

    public RestResponse execute(Method method, String endPoint, Map<String, Object> payload) {
        final WebResource request = client.resource(getRestUrl() + endPoint);
        ClientResponse clientResponse;
        switch (method) {
            case POST: {
                clientResponse = request.accept(RESPONSE_TYPE).post(ClientResponse.class, buildMultivaluedMap(payload));
                break;
            }
            case DELETE: {
                addQueryParams(payload, request);
                clientResponse = request.accept(RESPONSE_TYPE).delete(ClientResponse.class);
                break;
            }
            default: {
                addQueryParams(payload, request);
                clientResponse = request.accept(RESPONSE_TYPE).get(ClientResponse.class);
            }
        }

        Map<String, Object> responseMap = processJsonMap(clientResponse.getEntity(String.class));

        RestResponse response = new RestResponse();
        response.setStatus(clientResponse.getStatus());
        response.setMessage((String)responseMap.get("message"));

        return response;
    }

    protected boolean isSuccess(ClientResponse response) {
        int status = response.getStatus();
        return ((status == 200) || (status == 201));
    }

    protected boolean isNew() {
        return isNew;
    }

    protected void setIsNew() {
        this.isNew = true;
    }

    protected synchronized void initialize() {
        if (!initialized) {
            ClientResponse clientResponse = client.resource(getRestUrl()).accept(RESPONSE_TYPE).get(ClientResponse.class);
            Map<String, Object> responseMap = processJsonMap(clientResponse.getEntity(String.class));
            status = clientResponse.getStatus();

            getEntityValues(responseMap);
            getChildren(responseMap);
            initialized = true;
        }
    }

    protected <T> T getValue(String key, Class<T> clazz) {
        initialize();
        T retValue = null;
        Object value = entityValues.get(key);
        if ((value != null) && !(value.equals(JSONObject.NULL))) {
            retValue = (T) value;
        }
        return retValue;
    }

    protected <T> void setValue(String key, T value) {
        initialize();
        entityValues.put(key, value);
    }

    protected Map<String, String> getEntityMetadata(Map<String, Object> extraProperties) {
        Map<String, String> metadata = new HashMap<String, String>();
        List<Map<String, Object>> methods = (List<Map<String, Object>>) extraProperties.get("methods");

        for (Map<String, Object> entry : methods) {
            if ("POST".equals(entry.get("name"))) {
                Map<String, Map> params = (Map<String, Map>) entry.get("messageParameters");

                if (params != null) {
                    for (Map.Entry<String, Map> param : params.entrySet()) {
                        String paramName = param.getKey();
                        Map<String, String> md = (Map<String, String>) param.getValue();
                        metadata.put(paramName, md.get("type"));

                    }
                }
            }
        }

        return metadata;
    }

    protected void getEntityValues(Map<String, Object> responseMap) {
        entityValues = new HashMap<String, Object>();

        Map<String, Object> extraProperties = (Map<String, Object>) responseMap.get("extraProperties");
        if (extraProperties != null) {
            Map<String, String> entity = (Map) extraProperties.get("entity");

            if (entity != null) {
                Map<String, String> metadata = getEntityMetadata(extraProperties);
                for (Map.Entry<String, String> entry : entity.entrySet()) {
                    String type = metadata.get(entry.getKey());
                    Object value = null;
                    if ("int".equals(type)) {
                        value = Integer.parseInt(entry.getValue());
                    } else if ("boolean".equals(type)) {
                        value = Boolean.parseBoolean(entry.getValue());
                    } else {
                        value = entry.getValue();
                    }
                    entityValues.put(entry.getKey(), value);
                }
            }
        }
    }

    protected void getChildren(Map<String, Object> responseMap) {
        children = new ArrayList<String>();

        Map<String, Object> extraProperties = (Map<String, Object>) responseMap.get("extraProperties");
        if (extraProperties != null) {
            Map<String, String> childResources = (Map) extraProperties.get("childResources");

            if (childResources != null) {
                for (Map.Entry<String, String> child : childResources.entrySet()) {
                    children.add(child.getKey());
                }
            }
        }
    }

    protected void addQueryParams(Map<String, Object> payload, WebResource resource) {
        if ((payload != null) && !payload.isEmpty()) {
            resource.queryParams(buildMultivaluedMap(payload));
        }


    }

    private MultivaluedMap buildMultivaluedMap(Map<String, Object> payload) {
        MultivaluedMap formData = new MultivaluedMapImpl();
        for (final Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            if (JSONObject.NULL.equals(value)) {
                value = null;
            } else if (value != null) {
                value = value.toString();
            }
            formData.add(entry.getKey(), value);
        }
        return formData;
    }

    private Map<String, Object> processJsonMap(String json) {
        Map<String, Object> map;
        try {
            map = processJsonObject(new JSONObject(json));
        } catch (JSONException e) {
            map = new HashMap();
        }
        return map;
    }

    private static Map processJsonObject(JSONObject jo) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Iterator i = jo.keys();
            while (i.hasNext()) {
                String key = (String) i.next();
                Object value = jo.get(key);
                if (value instanceof JSONArray) {
                    map.put(key, processJsonArray((JSONArray) value));
                } else if (value instanceof JSONObject) {
                    map.put(key, processJsonObject((JSONObject) value));
                } else {
                    map.put(key, value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return map;
    }

    private static List processJsonArray(JSONArray ja) {
        List results = new ArrayList();

        try {
            for (int i = 0; i < ja.length(); i++) {
                Object entry = ja.get(i);
                if (entry instanceof JSONArray) {
                    results.add(processJsonArray((JSONArray) entry));
                } else if (entry instanceof JSONObject) {
                    results.add(processJsonObject((JSONObject) entry));
                } else {
                    results.add(entry);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return results;
    }

    public static enum Method { GET, PUT, POST, DELETE };
}