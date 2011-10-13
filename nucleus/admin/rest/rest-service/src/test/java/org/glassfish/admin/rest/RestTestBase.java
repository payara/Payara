/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import java.io.ByteArrayInputStream;
import java.io.File;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.glassfish.admin.rest.clientutils.MarshallingUtils;
import org.junit.Before;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.w3c.dom.Document;

public class RestTestBase {
    protected static String baseUrl;
    protected static final String RESPONSE_TYPE = MediaType.APPLICATION_JSON;//APPLICATION_XML;
    protected static final String AUTH_USER_NAME = "dummyuser";
    protected static final String AUTH_PASSWORD = "dummypass";
    protected static final String CONTEXT_ROOT_MANAGEMENT = "management";

    protected Client client;
    protected static String adminHost;
    protected static String adminPort;
    protected static String instancePort;


    @BeforeClass
    public static void initialize() {
        adminPort = getParameter("admin.port", "4848");
        instancePort = getParameter("instance.port", "8080");
        adminHost  = getParameter("instance.host", "localhost");
        baseUrl =  "http://" + adminHost + ':'  + adminPort + '/';
    }

    protected static String getBaseUrl() {
        return baseUrl;
    }

    protected String getContextRoot() {
        return CONTEXT_ROOT_MANAGEMENT;
    }

    @Before
    public void setup() {
        if (client == null) {
            client = Client.create();
        }
    }

    protected String getAddress(String address) {
        if (address.startsWith("http://")) {
            return address;
        }

        return baseUrl + getContextRoot() + address;
    }

    protected void resetClient() {
        client.removeAllFilters();
    }

    protected void authenticate() {
        client.addFilter(new HTTPBasicAuthFilter(AUTH_USER_NAME, AUTH_PASSWORD));
    }

    protected <T> T getTestClass(Class<T> clazz) {
        try {
            T test = clazz.newInstance();
            ((RestTestBase) test).setup();
            return test;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static String generateRandomString() {
        SecureRandom random = new SecureRandom();

        return new BigInteger(130, random).toString(16);
    }

    protected static int generateRandomNumber() {
        Random r = new Random();
        return Math.abs(r.nextInt()) + 1;
    }

    protected int generateRandomNumber(int max) {
        Random r = new Random();
        return Math.abs(r.nextInt(max - 1)) + 1;
    }

    protected boolean isSuccess(ClientResponse response) {
        int status = response.getStatus();
        return ((status == 200) || (status == 201));
    }

    protected void checkStatusForSuccess(ClientResponse cr) {
        int status = cr.getStatus();
        if ((status < 200) || (status > 299)) {
            String message = getErrorMessage(cr);
            fail("Expected a status between 200 and 299 (inclusive).  Found " + status +
                    ((message != null) ? ":  " + message : ""));
        }
    }

    protected void checkStatusForFailure(ClientResponse cr) {
        int status = cr.getStatus();
        if ((status < 200) && (status > 299)) {
            fail("Expected a status less than 200 or greater than 299 (inclusive).  Found " + status);
        }
    }

    protected ClientResponse get(String address) {
        return get(address, new HashMap<String, String>());
    }

    protected ClientResponse get(String address, Map<String, String> payload) {
        return client.resource(getAddress(address)).queryParams(buildMultivaluedMap(payload)).accept(RESPONSE_TYPE).get(ClientResponse.class);
    }

    protected ClientResponse options(String address) {
        return client.resource(getAddress(address)).accept(RESPONSE_TYPE).options(ClientResponse.class);
    }

    protected ClientResponse post(String address, Map<String, String> payload) {
        return client.resource(getAddress(address)).accept(RESPONSE_TYPE).post(ClientResponse.class, buildMultivaluedMap(payload));
    }

    protected ClientResponse post(String address) {
        return client.resource(getAddress(address)).accept(RESPONSE_TYPE).post(ClientResponse.class);
    }

    protected ClientResponse put(String address, Map<String, String> payload) {
        return client.resource(getAddress(address)).accept(RESPONSE_TYPE).put(ClientResponse.class, buildMultivaluedMap(payload));
    }

    protected ClientResponse put(String address) {
        return client.resource(getAddress(address)).accept(RESPONSE_TYPE).put(ClientResponse.class);
    }

    protected ClientResponse postWithUpload(String address, Map<String, Object> payload) {
        FormDataMultiPart form = new FormDataMultiPart();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if ((entry.getValue() instanceof File)) {
                form.getBodyParts().add((new FileDataBodyPart(entry.getKey(), (File)entry.getValue())));
            } else {
                form.field(entry.getKey(), entry.getValue(), MediaType.TEXT_PLAIN_TYPE);
            }
        }
        return client.resource(getAddress(address)).type(MediaType.MULTIPART_FORM_DATA).accept(RESPONSE_TYPE).post(ClientResponse.class, form);
    }

    protected ClientResponse delete(String address) {
        return delete(address, new HashMap<String, String>());
    }

    protected ClientResponse delete(String address, Map<String, String> payload) {
        return client.resource(getAddress(address))
                .queryParams(buildMultivaluedMap(payload))
                .accept(RESPONSE_TYPE)
                .delete(ClientResponse.class);
    }

    /**
     * This method will parse the provided XML document and return a map of the attributes and values on the root element
     *
     * @param response
     * @return
     */
    protected Map<String, String> getEntityValues(ClientResponse response) {
        Map<String, String> map = new HashMap<String, String>();

        String xml = response.getEntity(String.class);
        Map responseMap = MarshallingUtils.buildMapFromDocument(xml);
        Object obj = responseMap.get("extraProperties");
        if (obj != null) {
            return (Map)((Map) obj).get("entity");
        } else {
            return map;
        }
    }

    protected List<String> getCommandResults(ClientResponse response) {
        String document = response.getEntity(String.class);
        List<String> results = new ArrayList<String>();
        Map map = MarshallingUtils.buildMapFromDocument(document);
        String message = (String)map.get("message");
        if (message != null && !"".equals(message)) {
            results.add(message);
        }

        Object children = map.get("children");
        if (children instanceof List) {
            for (Object child : (List)children) {
                Map childMap = (Map) child;
                message = (String)childMap.get("message");
                if (message != null) {
                    results.add(message);
                }
            }
        }

        return results;
    }

    protected Map<String, String> getChildResources(ClientResponse response) {
        Map responseMap = MarshallingUtils.buildMapFromDocument(response.getEntity(String.class));
        Map<String, Map> extraProperties = (Map<String, Map>)responseMap.get("extraProperties");
        if (extraProperties != null) {
            return (Map<String, String>) extraProperties.get("childResources");
        }

        return new HashMap<String, String>();
    }

    public Document getDocument(String input) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(input.getBytes()));
            return doc;
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Map<String, String>> getProperties(ClientResponse response) {
        Map responseMap = MarshallingUtils.buildMapFromDocument(response.getEntity(String.class));
        Map extraProperties = (Map)responseMap.get("extraProperties");
        if (extraProperties != null) {
            return (List)extraProperties.get("properties");
        }
        return new ArrayList<Map<String, String>>();
    }

    private MultivaluedMap buildMultivaluedMap(Map<String, String> payload) {
        if (payload instanceof MultivaluedMap) {
            return (MultivaluedMap)payload;
        }
        MultivaluedMap formData = new MultivaluedMapImpl();
        if (payload != null) {
            for (final Map.Entry<String, String> entry : payload.entrySet()) {
                formData.add(entry.getKey(), entry.getValue());
            }
        }
        return formData;
    }

    protected String getErrorMessage(ClientResponse cr) {
        String message = null;
        Map map = MarshallingUtils.buildMapFromDocument(cr.getEntity(String.class));
        if (map != null) {
            message = (String)map.get("message");
        }

        return message;
    }

    protected static String getParameter(String paramName, String defaultValue) {
        String value = System.getenv(paramName);
        if (value == null) {
            value = System.getProperty(paramName);
        }
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }
}
