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
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.client.Entity.entity;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;

/**
 * Test the JVM option endpoint of the REST management interface
 */
public class JvmOptionsTest extends RestManagementTest {
    /**
     * Tests that when a POST request is made to the endpoint the JVM options posted
     * are added. See CUSTCOM-234.
     */
    @Test
    @InSequence(1)
    public void when_POST_jvm_options_expect_success() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("-DtestProperty", 12345);
        properties.put("-XX\\:+PrintFlagsFinal", "");
        properties.put("--add-opens=java.fake/java.io=ALL-UNNAMED", "");

        Response result = target.path(jvmOptionsEndpoint())
            .request()
            .post(entity(properties, APPLICATION_JSON));
        assertEquals(String.format("Failed to POST JVM options. Response data: %s", result.readEntity(String.class)),
                200, result.getStatus());

        List<String> jvmOptions = getJvmOptions();

        verifyJvmOption("-DtestProperty", properties, jvmOptions);
        verifyJvmOption("-XX\\:+PrintFlagsFinal", properties, jvmOptions);
        verifyJvmOption("--add-opens=java.fake/java.io=ALL-UNNAMED", properties, jvmOptions);
    }

    /**
     * Tests that when a POST request is made the posted variable is updated. See
     * CUSTCOM-234.
     */
    @Test
    @InSequence(2)
    public void when_POST_jvm_option_expect_change_submitted() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("-DtestProperty", 67891);

        target.path(jvmOptionsEndpoint())
                .request()
                .post(entity(properties, APPLICATION_JSON));

        List<String> jvmOptions = getJvmOptions();

        verifyJvmOption("-DtestProperty", properties, jvmOptions);
    }

    private List<String> getJvmOptions() {
        return target.path(jvmOptionsEndpoint())
            .request()
            .get(JsonObject.class)
            .getJsonObject("extraProperties")
            .getJsonArray("leafList")
            .parallelStream()
            .map(value -> ((JsonString) value.asJsonObject().get("jvmOption")).getString())
            .collect(toList());
    }

    private String jvmOptionsEndpoint() {
        return format("configs/config/%s-config/java-config/jvm-options", INSTANCE_NAME);
    }

    private static void verifyJvmOption(String propertyName, Map<String, Object> localMap, List<String> jvmOptions) {
        // Unescape and finish constructing the JVM option as it will have been stored.
        String jvmOption = propertyName.replace("\\", "");
        Object jvmValue = localMap.get(jvmOption);
        if (jvmValue != null && !jvmValue.toString().isEmpty()) {
            jvmOption += "=" + jvmValue;
        }

        assertTrue(String.format("The %s JVM option wasn't found. JVM Options: %s", jvmOption, Arrays.toString(jvmOptions.toArray())),
                jvmOptions.contains(jvmOption));
    }

}