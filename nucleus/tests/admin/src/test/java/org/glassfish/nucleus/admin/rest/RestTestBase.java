/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.nucleus.admin.rest;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.glassfish.admin.rest.client.ClientWrapper;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import static org.testng.AssertJUnit.*;
import org.w3c.dom.Document;

public class RestTestBase {
    protected static String baseUrl = "http://localhost:4848";
    protected static final String RESPONSE_TYPE = MediaType.APPLICATION_JSON;//APPLICATION_XML;
    protected static final String AUTH_USER_NAME = "dummyuser";
    protected static final String AUTH_PASSWORD = "dummypass";
    protected static final String CONTEXT_ROOT_MANAGEMENT = "/management";
    private static final HttpAuthenticationFeature basicAuthFilter = HttpAuthenticationFeature.basic(AUTH_USER_NAME, AUTH_PASSWORD);
    protected static String adminHost;
    protected static String adminPort;
    protected static String adminUser;
    protected static String adminPass;
    protected static String instancePort;

    private static String currentTestClass = "";
    protected Client client;

//    @BeforeClass
    public static void initialize() {
        adminPort = getParameter("admin.port", "4848");
        instancePort = getParameter("instance.port", "8080");
        adminHost = getParameter("instance.host", "localhost");
        adminUser = getParameter("user.name", "admin");
        adminPass = getParameter("user.pass", "");
        baseUrl = "http://" + adminHost + ':' + adminPort + '/';

        final RestTestBase rtb = new RestTestBase();
        rtb.get("/domain/rotate-log");
    }

//    @AfterClass
    public static void captureLog() {
        try {

            if (!currentTestClass.isEmpty()) {
                RestTestBase rtb = new RestTestBase();
                Client client = new ClientWrapper(new HashMap<String, String>(), adminUser, adminPass);
                Response cr = client.target(rtb.getAddress("/domain/view-log")).
                        request().
                        get(Response.class);

                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("target/surefire-reports/" + currentTestClass + "-server.log")));
                out.write(cr.readEntity(String.class));
                out.close();
            }
        } catch (Exception ex) {
            Logger.getLogger(RestTestBase.class.getName()).
                    log(Level.INFO, null, ex);
        }
    }

    protected static String getBaseUrl() {
        return baseUrl;
    }

    protected String getContextRoot() {
        return CONTEXT_ROOT_MANAGEMENT;
    }

//    @BeforeMethod(alwaysRun = true)
    public void setup() {
        currentTestClass = this.getClass().getName();
    }

    protected String getAddress(String address) {
        if (address.startsWith("http://")) {
            return address;
        }

        return baseUrl + getContextRoot() + address;
    }

//    @BeforeMethod
    protected Client getClient() {
        if (client == null) {
            client = new ClientWrapper(new HashMap<String, String>(), adminUser, adminPass);
            if (Boolean.parseBoolean(System.getProperty("DEBUG"))) {
                client.register(new LoggingFilter());
            }
        }
        return client;
    }

    protected void resetClient() {
        client = null;
        getClient();
    }

    protected void authenticate() {
        getClient().register(basicAuthFilter);
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

    protected boolean isSuccess(Response response) {
        int status = response.getStatus();
        return ((status >= 200) && (status <= 299));
    }

    protected void checkStatusForSuccess(Response cr) {
        int status = cr.getStatus();
        if (!isSuccess(cr)) {
            String message = getErrorMessage(cr);
            fail("Expected a status between 200 and 299 (inclusive).  Found " + status
                    + ((message != null) ? ":  " + message : ""));
        }
    }

    protected void checkStatusForFailure(Response cr) {
        int status = cr.getStatus();
        if (isSuccess(cr)) {
            fail("Expected a status less than 200 or greater than 299 (inclusive).  Found " + status);
        }
    }

    protected String getResponseType() {
        return RESPONSE_TYPE;
    }

    protected Response get(String address) {
        return get(address, new HashMap<String, String>());
    }

    protected Response get(String address, Map<String, String> payload) {
        WebTarget target = getClient().target(getAddress(address));
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }
        return target.request(getResponseType())
                .get(Response.class);
    }

    protected Response options(String address) {
        return getClient().target(getAddress(address)).
                request(getResponseType()).
                options(Response.class);
    }

    protected Response post(String address, Map<String, String> payload) {
        return getClient().target(getAddress(address)).
                request(getResponseType()).
                post(Entity.entity(buildMultivaluedMap(payload), MediaType.APPLICATION_FORM_URLENCODED), Response.class);
    }

    protected Response post(String address) {
        return getClient().target(getAddress(address)).
                request(getResponseType()).
                post(Entity.entity(null, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
    }

    protected Response put(String address, Map<String, String> payload) {
        return getClient().target(getAddress(address)).
                request(getResponseType()).
                put(Entity.entity(buildMultivaluedMap(payload), MediaType.APPLICATION_FORM_URLENCODED), Response.class);
    }

    protected Response put(String address) {
        return getClient().target(getAddress(address)).
                request(getResponseType()).
                put(Entity.entity(null, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
    }

    protected Response postWithUpload(String address, Map<String, Object> payload) {
        FormDataMultiPart form = new FormDataMultiPart();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if ((entry.getValue() instanceof File)) {
                form.getBodyParts().
                        add((new FileDataBodyPart(entry.getKey(), (File) entry.getValue())));
            } else {
                form.field(entry.getKey(), entry.getValue(), MediaType.TEXT_PLAIN_TYPE);
            }
        }
        return getClient().target(getAddress(address)).
                request(getResponseType()).
                post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA), Response.class);
    }

    protected Response delete(String address) {
        return delete(address, new HashMap<String, String>());
    }

    protected Response delete(String address, Map<String, String> payload) {
        WebTarget target = getClient().target(getAddress(address));
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }
        return target.request(getResponseType())
                .delete(Response.class);
    }

    /**
     * This method will parse the provided XML document and return a map of the attributes and values on the root
     * element
     *
     * @param response
     * @return
     */
    protected Map<String, String> getEntityValues(Response response) {
        Map<String, String> map = new HashMap<String, String>();

        String xml = response.readEntity(String.class);
        Map responseMap = MarshallingUtils.buildMapFromDocument(xml);
        Object obj = responseMap.get("extraProperties");
        if (obj != null) {
            return (Map) ((Map) obj).get("entity");
        } else {
            return map;
        }
    }

    protected List<String> getCommandResults(Response response) {
        String document = response.readEntity(String.class);
        List<String> results = new ArrayList<String>();
        Map map = MarshallingUtils.buildMapFromDocument(document);
        String message = (String) map.get("message");
        if (message != null && !"".equals(message)) {
            results.add(message);
        }

        Object children = map.get("children");
        if (children instanceof List) {
            for (Object child : (List) children) {
                Map childMap = (Map) child;
                message = (String) childMap.get("message");
                if (message != null) {
                    results.add(message);
                }
            }
        }

        return results;
    }

    protected Map<String, String> getChildResources(Response response) {
        Map responseMap = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        Map<String, Map> extraProperties = (Map<String, Map>) responseMap.get("extraProperties");
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

    public List<Map<String, String>> getProperties(Response response) {
        Map responseMap = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        Map extraProperties = (Map) responseMap.get("extraProperties");
        if (extraProperties != null) {
            return (List) extraProperties.get("properties");
        }
        return new ArrayList<Map<String, String>>();
    }

    private MultivaluedMap buildMultivaluedMap(Map<String, String> payload) {
        if (payload instanceof MultivaluedMap) {
            return (MultivaluedMap) payload;
        }
        MultivaluedMap formData = new MultivaluedHashMap();
        if (payload != null) {
            for (final Map.Entry<String, String> entry : payload.entrySet()) {
                formData.add(entry.getKey(), entry.getValue());
            }
        }
        return formData;
    }

    protected String getErrorMessage(Response cr) {
        String message = null;
        Map map = MarshallingUtils.buildMapFromDocument(cr.readEntity(String.class));
        if (map != null) {
            message = (String) map.get("message");
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
