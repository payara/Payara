/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.rest.management;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static jakarta.ws.rs.client.Entity.entity;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;

/**
 * Test the system property endpoint of the REST management interface
 */
public class SystemPropertyTest extends RestManagementTest {
    /**
     * Tests that when a POST request is made to the endpoint the system properties
     * posted are added.
     */
    @Test
    @InSequence(1)
    public void when_POST_system_properties_expect_success() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("ASADMIN_LISTENER_PORT", 12345);
        properties.put("HTTP_LISTENER_PORT", 23456);
        properties.put("HTTP_SSL_LISTENER_PORT", 34567);

        target.path(systemPropertiesEndpoint())
            .request()
            .post(entity(properties, APPLICATION_JSON));

        Map<String, String> systemProperties = getSystemProperties();

        verifySystemProperty("ASADMIN_LISTENER_PORT", properties, systemProperties);
        verifySystemProperty("HTTP_LISTENER_PORT", properties, systemProperties);
        verifySystemProperty("HTTP_SSL_LISTENER_PORT", properties, systemProperties);
    }

    /**
     * Tests that when a PUT request is made all other system properties are
     * overwritten. See CUSTCOM-75 for details.
     */
    @Test
    @InSequence(2)
    public void when_PUT_system_property_expect_others_are_deleted() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("HTTP_LISTENER_PORT", 45678);
        properties.put("HTTP_SSL_LISTENER_PORT", 56789);

        target.path(systemPropertiesEndpoint())
            .request()
            .put(entity(properties, APPLICATION_JSON));

        Map<String, String> systemProperties = getSystemProperties();

        verifySystemProperty("HTTP_LISTENER_PORT", properties, systemProperties);
        verifySystemProperty("HTTP_SSL_LISTENER_PORT", properties, systemProperties);
        assertEquals("No other system properties should exist after a PUT.", 2, systemProperties.size());
    }

    /**
     * Tests that when a POST request is made the posted variables are updated and
     * the others remain. See CUSTCOM-75 for details.
     */
    @Test
    @InSequence(3)
    public void when_POST_system_property_expect_only_change_submitted() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("HTTP_LISTENER_PORT", 67891);

        target.path(systemPropertiesEndpoint())
                .request()
                .post(entity(properties, APPLICATION_JSON));

        Map<String, String> systemProperties = getSystemProperties();

        verifySystemProperty("HTTP_LISTENER_PORT", properties, systemProperties);
        assertEquals("All other system properties should be maintained after a POST.", 2, systemProperties.size());
    }

    private Map<String, String> getSystemProperties() {
        return target.path(systemPropertiesEndpoint())
            .request()
            .get(JsonObject.class)
            .getJsonObject("extraProperties")
            .getJsonArray("systemProperties")
            .parallelStream()
            .filter(value -> value.asJsonObject().get("value") != null)
            .collect(toMap(
                    value -> ((JsonString) value.asJsonObject().get("name")).getString(),
                    value -> ((JsonString) value.asJsonObject().get("value")).getString()));
    }

    private String systemPropertiesEndpoint() {
        return format("servers/server/%s/system-properties", INSTANCE_NAME);
    }

    private static void verifySystemProperty(String propertyName, Map<String, ? extends Object> localMap, Map<String, ? extends Object> systemProperties) {
        assertTrue(String.format("The %s system property wasn't found.", propertyName),
                systemProperties.containsKey(propertyName));
        assertEquals(String.format("The %s system property was incorrect.", propertyName),
                localMap.get(propertyName).toString(),
                systemProperties.get(propertyName).toString());
    }

}
