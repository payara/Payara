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
import static jakarta.ws.rs.client.Entity.entity;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

/**
 * Test the REST management interface for it's basic HTTP responses
 */
public class HttpSemanticsTest extends RestManagementTest {

    /**
     * Tests that when deleting a non-existent resource a 404 is returned instead of
     * a 415. See CUSTCOM-13 for details.
     */
    @Test
    public void when_DELETE_non_existent_resource_expect_404() {
        Response response = target.path("servers/server/server/application-ref/non-existent-application-ref")
                .request()
                .delete();
        assertEquals("A non-existent resource should return a 404.", 404, response.getStatus());
    }

    /**
     * Tests that when invoking an asadmin command using the REST management
     * interface with a JSON request, the parameters are unwrapped correctly (and
     * without quotation marks). See CUSTCOM-199 for details.
     */
    @Test
    public void when_JSON_parameters_in_request_expect_correct_parsing() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("node", "invalid-node");

        JsonObject response = target.path("create-instance")
                .request()
                .post(entity(parameters, APPLICATION_JSON))
                .readEntity(JsonObject.class);

        String commandLog = response
                .getJsonObject("extraProperties")
                .getJsonArray("commandLog")
                .toString();

        assertTrue(format("The \"node\" parameter was unwrapped incorrectly. command log: %s", commandLog),
            commandLog.contains("--node invalid-node"));
    }

}