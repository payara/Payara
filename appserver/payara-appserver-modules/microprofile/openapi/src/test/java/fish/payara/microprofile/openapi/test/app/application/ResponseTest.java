/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.app.application.schema.TeacherDTO;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response.Status;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map.Entry;

import org.junit.Test;

/**
 * A resource to test that various response types are mapped properly.
 */
@Path("/response")
@Produces({ APPLICATION_JSON, APPLICATION_XML })
public class ResponseTest extends OpenApiApplicationTest {

    @Schema(name = "Teacher", description = "Custom Response")
    public class ResponseSchema extends TeacherDTO {

    }

    @GET
    @APIResponse(responseCode = "200", description = "success", 
        content = @Content(schema = @Schema(description = "hello!")))
    @APIResponse(responseCode = "400", description = "error")
    public String getInheritedMediaType() {
        return null;
    }

    @Path("/schema")
    @GET
    @APIResponseSchema(value = ResponseSchema.class, responseCode = "202", responseDescription = "custom description")
    public void apiResponseSchema() {
    }

    @Path("/schema")
    @POST
    @APIResponseSchema(value = ResponseSchema.class)
    public void defaultPostVoidSchema() {
    }

    @Path("/schema")
    @PUT
    @APIResponseSchema(value = ResponseSchema.class)
    public void defaultPutVoidSchema() {
    }

    @Path("/schema")
    @HEAD
    @APIResponseSchema(value = ResponseSchema.class)
    public void defaultAsyncVoidSchema(final AsyncResponse async) {
    }

    @Path("/schema")
    @DELETE
    @APIResponseSchema(value = ResponseSchema.class)
    public String defaultDeleteStringSchema(final AsyncResponse async) {
        return null;
    }

    @Test
    public void inheritedMediaTypeTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response").getGET().getResponses();

        // Test the 200 response
        checkResponseCode(responses, 200, "success");
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse("200");
        checkMediaTypesExist(response, APPLICATION_JSON, APPLICATION_XML);
        checkSchemaDescriptions(response, "hello!");
    }

    @Test
    public void apiResponseSchemaTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response/schema").getGET().getResponses();

        checkResponseCode(responses, 202, "custom description");
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse("202");
        checkMediaTypesExist(response, APPLICATION_JSON, APPLICATION_XML);
        checkSchemaDescriptions(response, "Custom Response");
    }

    @Test
    public void defaultPostVoidSchemaTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response/schema").getPOST().getResponses();

        checkResponseCode(responses, 201);
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse("201");
        checkMediaTypesExist(response, APPLICATION_JSON, APPLICATION_XML);
        checkSchemaDescriptions(response, "Custom Response");
    }

    @Test
    public void defaultPutVoidSchemaTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response/schema").getPUT().getResponses();

        checkResponseCode(responses, 204);
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse("204");
        checkMediaTypesExist(response, APPLICATION_JSON, APPLICATION_XML);
        checkSchemaDescriptions(response, "Custom Response");
    }

    @Test
    public void defaultAsyncVoidSchemaTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response/schema").getHEAD().getResponses();

        checkResponseCode(responses, 200);
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse("200");
        checkMediaTypesExist(response, APPLICATION_JSON, APPLICATION_XML);
        checkSchemaDescriptions(response, "Custom Response");
    }

    @Test
    public void defaultDeleteStringSchemaTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response/schema").getDELETE().getResponses();

        checkResponseCode(responses, 200);
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse("200");
        checkMediaTypesExist(response, APPLICATION_JSON, APPLICATION_XML);
        checkSchemaDescriptions(response, "Custom Response");
    }

    // Static helper methods

    private static void checkResponseCode(APIResponses responses, int responseCode) {
        final String defaultDescription = Status.fromStatusCode(responseCode).getReasonPhrase();
        checkResponseCode(responses, responseCode, defaultDescription);
    }

    private static void checkResponseCode(APIResponses responses, int responseCode, String description) {
        final APIResponseImpl response = (APIResponseImpl) responses.getAPIResponse(Integer.toString(responseCode));
        assertNotNull("The " + responseCode + " response should have been created.", response);
        assertEquals("The " + responseCode + " response has the wrong description", description, response.getDescription());

        // Test the default response doesn't exist
        assertNull("The default response should be removed when not used.", responses.getDefaultValue());
    }

    private static void checkMediaTypesExist(APIResponseImpl response, String... mediaTypes) {
        String responseCode = ((APIResponseImpl) response).getResponseCode();
        if (responseCode == null) {
            responseCode = APIResponses.DEFAULT;
        }
        for (String mediaType : mediaTypes) {
            assertNotNull("The " + responseCode + " response should return application/json.",
                    response.getContent().getMediaType(mediaType));
        }
    }

    private static void checkSchemaDescriptions(APIResponseImpl response, String description) {
        String responseCode = response.getResponseCode();
        if (responseCode == null) {
            responseCode = APIResponses.DEFAULT;
        }
        for (Entry<String, MediaType> entry : response.getContent().getMediaTypes().entrySet()) {
            assertEquals("The " + responseCode + " response " + entry.getKey() + " should match the specified schema.",
                description, entry.getValue().getSchema().getDescription());
        }
    }

}