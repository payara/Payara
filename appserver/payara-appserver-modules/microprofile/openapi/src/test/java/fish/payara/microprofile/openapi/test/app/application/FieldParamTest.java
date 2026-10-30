/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.test.app.application;

import com.fasterxml.jackson.databind.JsonNode;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * TCK lacks tests for parameter annotations being used on fields which should add such parameters to all methods of the
 * bean.
 */
@Path("/field-params")
public class FieldParamTest extends OpenApiApplicationTest {

    @PathParam("path")
    private String pathParam;

    @CookieParam("cookie")
    private String cookieParam; 

    @HeaderParam("header")
    private String headerParam;

    @QueryParam("query")
    private String queryParam;

    @GET
    public String getMethod() {
        return "";
    }

    @PUT
    public void putMethod(@SuppressWarnings("unused") @PathParam("extra") String extra) {
        // just exist to see the document for them
    }

    @Test
    public void pathParamIsAddedToApiMethods() {
        assertHasParameter("path", true);
    }

    @Test
    public void cookieParamIsAddedToApiMethods() {
        assertHasParameter("cookie", false);
    }

    @Test
    public void headerParamIsAddedToApiMethods() {
        assertHasParameter("header", false);
    }

    @Test
    public void queryParamIsAddedToApiMethods() {
        assertHasParameter("query", false);
    }

    @Test
    public void fieldParamsDoNotRemoveParameterParams() {
        assertEquals(4, path(getOpenAPIJson(), "paths./test/field-params.get.parameters").size());
        assertEquals(5, path(getOpenAPIJson(), "paths./test/field-params.put.parameters").size());
        assertParameter("extra", true, "paths./test/field-params.put.parameters");

    }

    private void assertHasParameter(String name, boolean required) {
        assertParameter(name, required, "paths./test/field-params.get.parameters");
        assertParameter(name, required, "paths./test/field-params.put.parameters");
    }

    private void assertParameter(String name, boolean required, String objectPath) {
        JsonNode parameter = parameterWithName(name, path(getOpenAPIJson(), objectPath));
        assertNotNull(parameter);
        assertRequired(required, parameter);
    }

    private static void assertRequired(boolean required, JsonNode parameter) {
        JsonNode requiredField = parameter.get("required");
        if (required) {
            assertTrue(requiredField.booleanValue());
        } else {
            assertTrue(requiredField == null || requiredField.isNull());
        }
    }

    private static JsonNode parameterWithName(String name, JsonNode parameters) {
        for (JsonNode parameter : parameters) {
            if (parameter.get("name").textValue().equals(name)) {
                return parameter;
            }
        }
        return null;
    }
}
