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
package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.junit.Test;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.app.application.schema.TeacherDTO;

@Path("/form-params")
public class FormParamTest extends OpenApiApplicationTest {

    @POST
    @Path("/{teacherId}")
    @Consumes({ "text/csv" })
    @Produces({ "text/csv" })
    @APIResponseSchema(value = TeacherDTO.class, responseCode = "204")
    @Operation(summary = "Updates a teacher with CSV data")
    public Response updateTeacherWithCsv(
            @Parameter(
                    name = "teacherId",
                    description = "ID of teacher",
                    required = true
            ) @PathParam("teacherId") Long teacherId,
            @RequestBodySchema(TeacherDTO.class) String commaSeparatedValues) {
        return Response.ok().build();
    }

    @Test
    public void formParamIsAddedToApiMethods() {
        assertHasParameter("teacherId", true);
    }

    private void assertHasParameter(String name, boolean required) {
        assertParameter(name, required, "paths./test/form-params/{teacherId}.post.parameters");
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
