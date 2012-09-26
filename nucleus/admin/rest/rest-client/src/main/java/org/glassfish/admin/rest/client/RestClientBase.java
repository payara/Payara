/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.client.utils.Util;

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
    protected RestClientBase parent;

    private boolean initialized = false;
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
        Response response = client.target(getRestUrl())
                .request(RESPONSE_TYPE)
                .post(buildMultivalueMapEntity(entityValues, MediaType.MULTIPART_FORM_DATA_TYPE), Response.class);
        boolean success = isSuccess(response);

        if (!success) {
            status = response.getStatus();
            message = response.readEntity(String.class);
        } else {
            isNew = false;
        }

        return success;
    }

    public boolean delete() {
        Response response = client.target(getRestUrl())
                .request(RESPONSE_TYPE)
                .delete(Response.class);
        boolean success = isSuccess(response);

        if (!success) {
            status = response.getStatus();
            message = response.readEntity(String.class);
        }

        return success;
    }

    public RestResponse execute(Method method, String endpoint, boolean needsMultiPart) {
        return execute (method, endpoint, new HashMap<String, Object>(), needsMultiPart);
    }

    public RestResponse execute(Method method, String endPoint, Map<String, Object> payload) {
        return execute(method, endPoint, payload, false);
    }

    public RestResponse execute(Method method, String endPoint, Map<String, Object> payload, boolean needsMultiPart) {
        final WebTarget target = client.target(getRestUrl() + endPoint);
        Response clientResponse;
        switch (method) {
            case POST: {
//                 TODO - JERSEY2
//                if (needsMultiPart) {
//                    clientResponse = target
//                            .request(RESPONSE_TYPE)
//                            .post(buildFormDataMultipart(payload, MediaType.MULTIPART_FORM_DATA_TYPE), Response.class);
//                } else
                {
                    clientResponse = target.request(RESPONSE_TYPE)
                            .post(buildMultivalueMapEntity(payload, null), Response.class);

                }
                break;
            }
            case PUT: {
//                TODO - JERSEY2
//                if (needsMultiPart) {
//                    clientResponse = target
//                            .request(RESPONSE_TYPE)
//                            .put(buildFormDataMultipart(payload, MediaType.MULTIPART_FORM_DATA_TYPE), Response.class);
//                } else
                {
                    clientResponse = target
                            .request(RESPONSE_TYPE)
                            .put(buildMultivalueMapEntity(payload, null), Response.class);

                }
                break;
            }
            case DELETE: {
//                addQueryParams(payload, target);
                clientResponse = targetWithQueryParams(target, buildMultivalueMap(payload)).request(RESPONSE_TYPE).delete(Response.class);
                break;
            }
            default: {
//                addQueryParams(payload, target);
                clientResponse = targetWithQueryParams(target, buildMultivalueMap(payload)).request(RESPONSE_TYPE).get(Response.class);
            }
        }

        return new RestResponse(clientResponse);
    }

    private static WebTarget targetWithQueryParams(WebTarget target, MultivaluedMap<String, Object> paramMap) {
        for (Entry<String, List<Object>> param : paramMap.entrySet()) {
            target = target.queryParam(param.getKey(), param.getValue());
        }
        return target;
    }

    protected boolean isSuccess(Response response) {
        int responseStatus = response.getStatus();
        return ((responseStatus == 200) || (responseStatus == 201));
    }

    protected boolean isNew() {
        return isNew;
    }

    protected void setIsNew() {
        this.isNew = true;
    }

    protected synchronized void initialize() {
        if (!initialized) {
            Response clientResponse = client.target(getRestUrl()).request(RESPONSE_TYPE).get(Response.class);
            Map<String, Object> responseMap = Util.processJsonMap(clientResponse.readEntity(String.class));
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

    /*
    protected void addQueryParams(Map<String, Object> payload, WebResource resource) {
        if ((payload != null) && !payload.isEmpty()) {
//            resource.queryParams(buildMultivalueMap(payload));
        }
    }
    */

//    TODO - JERSEY2
//    protected Entity<FormDataMultiPart> buildFormDataMultipart(Map<String, Object> payload, MediaType type) {
//        FormDataMultiPart formData = new FormDataMultiPart();
//        Logger logger = Logger.getLogger(RestClientBase.class.getName());
//        for (final Map.Entry<String, Object> entry : payload.entrySet()) {
//            final Object value = entry.getValue();
//            final String key = entry.getKey();
//            if (value instanceof Collection) {
//                for (Object obj : ((Collection) value)) {
//                    try {
//                        formData.field(key, obj, MediaType.TEXT_PLAIN_TYPE);
//                    } catch (ClassCastException ex) {
//                        if (logger.isLoggable(Level.FINEST)) {
//                            logger.log(Level.FINEST, "Unable to add key (\"{0}\") w/ value (\"{1}\").",
//                                new Object[]{key, obj});
//                        }
//
//                        // Allow it to continue b/c this property most likely
//                        // should have been excluded for this request
//                    }
//                }
//            } else {
//                //formData.putSingle(key, (value != null) ? value.toString() : value);
//                try {
//                    if (value instanceof File) {
//                        formData.getBodyParts().add((new FileDataBodyPart(key, (File)value)));
//                    } else {
//                        formData.field(key, value, MediaType.TEXT_PLAIN_TYPE);
//                    }
//                } catch (ClassCastException ex) {
//                    if (logger.isLoggable(Level.FINEST)) {
//                        logger.log(Level.FINEST,
//                                "Unable to add key (\"{0}\") w/ value (\"{1}\")." ,
//                                new Object[]{key, value});
//                    }
//                    // Allow it to continue b/c this property most likely
//                    // should have been excluded for this request
//                }
//            }
//        }
//        return Entity.entity(formData, type);
//    }

    private Entity<MultivaluedMap<String, Object>> buildMultivalueMapEntity(Map<String, Object> payload, MediaType type) {
        return Entity.entity(buildMultivalueMap(payload), type);
    }

    private MultivaluedMap<String, Object> buildMultivalueMap(Map<String, Object> payload) {
        MultivaluedMap formData = new MultivaluedHashMap();
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

    public static enum Method { GET, PUT, POST, DELETE };
}
