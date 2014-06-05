/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class PropertiesBagTest extends RestTestBase {

    protected static final String PROP_DOMAIN_NAME = "administrative.domain.name";
    protected static final String URL_DOMAIN_PROPERTIES = "/domain/property";
    protected static final String URL_JAVA_CONFIG_PROPERTIES = "/domain/configs/config/default-config/java-config/property";
    protected static final String URL_SERVER_PROPERTIES = "/domain/servers/server/server/property";
    protected static final String URL_DERBYPOOL_PROPERTIES = "/domain/resources/jdbc-connection-pool/DerbyPool/property";
    private static final String REQUEST_FORMAT = MediaType.APPLICATION_JSON;

    @Test
    public void propertyRetrieval() {
        Response response = get(URL_DOMAIN_PROPERTIES);
        checkStatusForSuccess(response);
        List<Map<String, String>> properties = getProperties(response);
        assertTrue(isPropertyFound(properties, PROP_DOMAIN_NAME));
    }

    @Test
    public void javaConfigProperties() {
        createAndDeleteProperties(URL_JAVA_CONFIG_PROPERTIES);
    }

    @Test
    public void serverProperties() {
        createAndDeleteProperties(URL_SERVER_PROPERTIES);
    }

    @Test(enabled=false)
    public void propsWithEmptyValues() {
        List<Map<String, String>> properties = new ArrayList<Map<String, String>>();
        final String empty = "empty" + generateRandomNumber();
        final String foo = "foo" + generateRandomNumber();
        final String bar = "bar" + generateRandomNumber();
        final String abc = "abc" + generateRandomNumber();

        properties.add(createProperty(empty,""));
        properties.add(createProperty(foo,"foovalue"));
        properties.add(createProperty(bar,"barvalue"));
        createProperties(URL_DERBYPOOL_PROPERTIES, properties);
        List<Map<String, String>> newProperties = getProperties(get(URL_DERBYPOOL_PROPERTIES));

        assertFalse(isPropertyFound(newProperties, empty));
        assertTrue(isPropertyFound(newProperties, foo));
        assertTrue(isPropertyFound(newProperties, bar));

        properties.clear();
        properties.add(createProperty(abc,"abcvalue"));
        createProperties(URL_DERBYPOOL_PROPERTIES, properties);
        newProperties = getProperties(get(URL_DERBYPOOL_PROPERTIES));

        assertTrue(isPropertyFound(newProperties, abc));
        assertFalse(isPropertyFound(newProperties, empty));
        assertFalse(isPropertyFound(newProperties, foo));
        assertFalse(isPropertyFound(newProperties, bar));
    }

    @Test(enabled=false)
    public void testOptimizedPropertyHandling() {
        // First, test changing one property and adding a new
        List<Map<String, String>> properties = new ArrayList<Map<String, String>>();
        properties.add(createProperty("PortNumber","1527"));
        properties.add(createProperty("Password","APP"));
        properties.add(createProperty("User","APP"));
        properties.add(createProperty("serverName","localhost"));
        properties.add(createProperty("DatabaseName","sun-appserv-samples"));
        properties.add(createProperty("connectionAttributes",";create=false"));
        properties.add(createProperty("foo","bar","test"));
        createProperties(URL_DERBYPOOL_PROPERTIES, properties);

        List<Map<String, String>> newProperties = getProperties(get(URL_DERBYPOOL_PROPERTIES));
        for (Map<String, String> property : newProperties) {
            if (property.get("name").equals("connectionAttributes")) {
                assertEquals(";create=false", property.get("value"));
            } else if (property.get("name").equals("foo")) {
                assertEquals("bar", property.get("value"));
                assertEquals("test", property.get("description"));
            }
        }

        // Test updating the description and value
        properties.clear();
        properties.add(createProperty("foo","bar 2","test 2"));
        createProperties(URL_DERBYPOOL_PROPERTIES, properties);

        newProperties = getProperties(get(URL_DERBYPOOL_PROPERTIES));
        assertNotSame(1, newProperties);
        for (Map<String, String> property : newProperties) {
            if (property.get("name").equals("foo")) {
                assertEquals("bar 2", property.get("value"));
                assertEquals("test 2", property.get("description"));
            }
        }

        // Now test changing that property back and deleting the new one
        properties.clear();
        properties.add(createProperty("PortNumber","1527"));
        properties.add(createProperty("Password","APP"));
        properties.add(createProperty("User","APP"));
        properties.add(createProperty("serverName","localhost"));
        properties.add(createProperty("DatabaseName","sun-appserv-samples"));
        properties.add(createProperty("connectionAttributes",";create=true"));

        createProperties(URL_DERBYPOOL_PROPERTIES, properties);

        newProperties = getProperties(get(URL_DERBYPOOL_PROPERTIES));
        for (Map<String, String> property : newProperties) {
            if (property.get("name").equals("connectionAttributes")) {
                assertEquals(";create=true", property.get("value"));
            } else if (property.get("name").equals("foo")) {
                fail("The property was not deleted as expected.");
            }
        }
    }

    // the prop name can not contain .
    // need to remove the . test when http://java.net/jira/browse/GLASSFISH-15418  is fixed
//    @Test
    public void testPropertiesWithDots() {
        List<Map<String, String>> properties = new ArrayList<Map<String, String>>();
        final String prop = "some.property.with.dots." + generateRandomNumber();
        final String description = "This is the description";
        final String value = generateRandomString();
        properties.add(createProperty(prop, value, description));
        createProperties(URL_DERBYPOOL_PROPERTIES, properties);
        List<Map<String, String>> newProperties = getProperties(get(URL_DERBYPOOL_PROPERTIES));
        Map<String, String> newProp = getProperty(newProperties, prop);
        assertTrue(newProp != null);
        assertTrue(value.equals(newProp.get("value")));
        assertTrue(description.equals(newProp.get("description")));
    }
    
    // This operation is taking a REALLY long time from the console, probably due
    // to improper properties handling when create the RA config.  However, when
    // updating the config's properties, we need to verfiy that only the changed
    // properties are updated, as the broker restarts after every property is
    // saved. This test will create the jmsra config with a set of properties,
    // then update only one the object's properties, which should be a very quick,
    // inexpensive operation.
    @Test(enabled=false)
    public void testJmsRaCreateAndUpdate() {
        List<Map<String, String>> props = new ArrayList<Map<String, String>>(){{
           add(createProperty("AddressListBehavior", "random"));
           add(createProperty("AddressListIterations", "3"));
           add(createProperty("AdminPassword", "admin"));
           add(createProperty("AdminUserName", "admin"));
           add(createProperty("BrokerInstanceName", "imqbroker"));
           add(createProperty("BrokerPort", "7676"));
           add(createProperty("BrokerStartTimeOut", "60000"));
           add(createProperty("BrokerType", "DIRECT"));
           add(createProperty("ConnectionUrl", "mq\\://localhost\\:7676/"));
           add(createProperty("ReconnectAttempts", "3"));
           add(createProperty("ReconnectEnabled", "true"));
           add(createProperty("ReconnectInterval", "5000"));
           add(createProperty("RmiRegistryPort", "8686"));
           add(createProperty("doBind", "false"));
           add(createProperty("startRMIRegistry", "false"));
        }};
        final String propertyList = buildPropertyList(props);
        Map<String, String> attrs = new HashMap<String, String>() {{
            put("objecttype","user");
            put("id","jmsra");
            put("threadPoolIds","thread-pool-1");
            put("property", propertyList);
        }};

        final String URL = "/domain/resources/resource-adapter-config";
        delete(URL+"/jmsra");
        Response response = post(URL, attrs);
        assertTrue(isSuccess(response));

        // Change one property value (AddressListIterations) and update the object
        props = new ArrayList<Map<String, String>>(){{
           add(createProperty("AddressListBehavior", "random"));
           add(createProperty("AddressListIterations", "4"));
           add(createProperty("AdminPassword", "admin"));
           add(createProperty("AdminUserName", "admin"));
           add(createProperty("BrokerInstanceName", "imqbroker"));
           add(createProperty("BrokerPort", "7676"));
           add(createProperty("BrokerStartTimeOut", "60000"));
           add(createProperty("BrokerType", "DIRECT"));
           add(createProperty("ConnectionUrl", "mq\\://localhost\\:7676/"));
           add(createProperty("ReconnectAttempts", "3"));
           add(createProperty("ReconnectEnabled", "true"));
           add(createProperty("ReconnectInterval", "5000"));
           add(createProperty("RmiRegistryPort", "8686"));
           add(createProperty("doBind", "false"));
           add(createProperty("startRMIRegistry", "false"));
        }};
        createProperties(URL+"/jmsra/property", props);

        delete(URL+"/jmsra");
    }

    //Disable this test for now. The functionality this tests is not available in nucleus
    //@Test
    public void test20810() {
        Map<String, String> payload = new HashMap<>();
        payload.put("persistenceScope","session");
        payload.put("disableJreplica","false");
        payload.put("persistenceType","replicated");
        payload.put("availabilityEnabled","true");
        payload.put("persistenceFrequency","web-method");
        payload.put("persistenceStoreHealthCheckEnabled","false");
        payload.put("ssoFailoverEnabled","false");
        
        final String wcaUri = "/domain/configs/config/default-config/availability-service/web-container-availability";
        Response r = post(wcaUri, payload);
        assertTrue(isSuccess(r));
        
        assertTrue(isSuccess(get(wcaUri)));

        r = getClient()
                .target(getAddress("/domain/configs/config/default-config/availability-service/web-container-availability/property")).
                request(getResponseType())
                .post(Entity.json(new JSONArray()), Response.class);
        assertTrue(isSuccess(r));
        
        assertTrue(isSuccess(get(wcaUri)));
    }
    
    protected String buildPropertyList(List<Map<String, String>> props) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (Map<String, String> prop : props) {
            sb.append(sep).append(prop.get("name")).append("=").append(prop.get("value"));
            sep = ":";
        }

        return sb.toString();
    }

    protected void createAndDeleteProperties(String endpoint) {
        Response response = get(endpoint);
        checkStatusForSuccess(response);
        assertNotNull(getProperties(response));

        List<Map<String, String>> properties = new ArrayList<Map<String, String>>();

        for(int i = 0, max = generateRandomNumber(16); i < max; i++) {
            properties.add(createProperty("property_" + generateRandomString(), generateRandomString(), generateRandomString()));
        }

        createProperties(endpoint, properties);
        response = delete(endpoint);
        checkStatusForSuccess(response);
    }

    protected Map<String, String> createProperty(final String name, final String value) {
        return createProperty(name, value, null);
    }
    
    protected Map<String, String> createProperty(final String name, final String value, final String description) {
        return new HashMap<String, String>() {{
                put ("name", name);
                put ("value", value);
                if (description != null) {
                    put ("description", description);
                }
            }};
    }

    protected void createProperties(String endpoint, List<Map<String, String>> properties) {
        final String payload = buildPayload(properties);
        Response response = getClient().target(getAddress(endpoint))
            .request(RESPONSE_TYPE)
            .post(Entity.entity(payload, REQUEST_FORMAT), Response.class);
        checkStatusForSuccess(response);
        response = get(endpoint);
        checkStatusForSuccess(response);

        // Retrieve the properties and make sure they were created.
//        List<Map<String, String>> newProperties = getProperties(response);
//
//        for (Map<String, String> property : properties) {
//            assertTrue(isPropertyFound(newProperties, property.get("name")));
//        }
    }

    // Restore and verify the default domain properties
    protected void restoreDomainProperties() {
        final HashMap<String, String> domainProps = new HashMap<String, String>() {{
            put("name", PROP_DOMAIN_NAME);
            put("value", "domain1");
        }};
        Response response = getClient().target(getAddress(URL_DOMAIN_PROPERTIES))
                .request(RESPONSE_TYPE)
                .put(Entity.entity(buildPayload(new ArrayList<Map<String, String>>() {{ add(domainProps); }}), REQUEST_FORMAT), Response.class);
        checkStatusForSuccess(response);
        response = get(URL_DOMAIN_PROPERTIES);
        checkStatusForSuccess(response);
        assertTrue(isPropertyFound(getProperties(response), PROP_DOMAIN_NAME));
    }

    protected String buildPayload(List<Map<String, String>> properties) {
        if (RESPONSE_TYPE.equals(MediaType.APPLICATION_XML)) {
            return MarshallingUtils.getXmlForProperties(properties);
        } else {
            return MarshallingUtils.getJsonForProperties(properties);
        }
    }

    protected boolean isPropertyFound(List<Map<String, String>> properties, String name) {
        return getProperty(properties, name) != null;
    }

    protected Map<String, String> getProperty(List<Map<String, String>> properties, String name) {
        Map<String, String> retval = null;
        for (Map<String,String> property : properties) {
            if (name.equals(property.get("name"))) {
                retval = property;
            }
        }

        return retval;
    }
}
