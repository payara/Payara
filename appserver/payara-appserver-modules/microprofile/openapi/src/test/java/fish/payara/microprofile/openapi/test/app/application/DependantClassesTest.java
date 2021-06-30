/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.app.application.schema.Partner;
import fish.payara.microprofile.openapi.test.app.application.schema.Schema1Depending;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import java.util.Iterator;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Test presence of required attribute in the generated OpenApi.
 */
@Path("/serversDependant")
public class DependantClassesTest extends OpenApiApplicationTest {

    // add multiple classes to be processed, simulate component scan
//    @Before
//    @Override
//    public void createDocument() {
////        document = ApplicationProcessedDocument.createDocument(null, getClass(), TestApplication.class, Partner.class, ShipmentData.class);
//        document = ApplicationProcessedDocument.createDocument(null, getClass(), TestApplication.class, Schema1Depending.class, Schema2Simple.class, Schema2Simple1.class, Partner.class, ShipmentData.class);
//        jsonDocument = toJson(document);
//    }

    // FIXME: the Partner classes will be removed, but for now -- they are NOT working, whils Schema* classes DO WORK!
    @Path("/partner")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save", description = "Save partner data.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successful, returning ok.")})
    public Response save(@RequestBody(description = "The request body with partner data",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Partner.class)
            )
    ) final Partner partner) {
        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successful, returning ok.")})
    public Schema1Depending save(@RequestBody(description = "The request body with partner data", required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Schema1Depending.class)
            )) Schema1Depending schema1Depending) {
        return new Schema1Depending();
    }

    /**
     * Test if the additional field description is copied to the dependant
     * schema. E.g. Schema2Simple -> Schema1Depending
     */
    @Test
    public void dependantSchemaIsFullyPopulated() {
        ObjectNode root = getOpenAPIJson();
        // verify schema
        JsonNode subpropertyId = path(root, "components.schemas.Schema1Depending.properties.schema2Simple.properties.id");
        assertEquals("ID", subpropertyId.findValue("description").asText());
        assertEquals("1", subpropertyId.findValue("example").asText());
        // verify operation's data
        // FIXME: this test crashes as for some reason, schema2Simple is refereneced, not copied.
        JsonNode operationSubpropertyId = path(root, "paths./test/serversDependant.post.requestBody.content.application/json.schema.properties.schema2Simple.properties.id");
        assertEquals("ID", subpropertyId.findValue("description").asText());
        assertEquals("1", subpropertyId.findValue("example").asText());
    }

    @Test
    public void dependantPartnerIsFullyPopulated() {
        ObjectNode root = getOpenAPIJson();
        // verify schema
        JsonNode subpropertyId = path(root, "components.schemas.Partner.properties.shipmentData.properties.salutation");
        assertEquals("Salutation of the delivery address contact person", subpropertyId.findValue("description").asText());
        assertEquals("MR", subpropertyId.findValue("example").asText());
        // verify operation's data
        // FIXME: this test crashes as for some reason, schema2Simple is refereneced, not copied.
        JsonNode operationPropertyId = path(root, "paths./test/serversDependant/partner.post.requestBody.content.application/json.schema.properties.id");
        JsonNode idDescription = operationPropertyId.findValue("description");
        assertNotNull("description is not found for id", idDescription);
        assertEquals("ID of the partner", idDescription.asText());
        assertEquals("1", operationPropertyId.findValue("example").asText());
        JsonNode operationSubpropertyId = path(root, "paths./test/serversDependant/partner.post.requestBody.content.application/json.schema.properties.shipmentData.properties.salutation");
        JsonNode salutationDescription = operationSubpropertyId.findValue("description");
        assertNotNull("description is not found for salutation", salutationDescription);
        assertEquals("Salutation of the delivery address contact person", salutationDescription.asText());
        assertEquals("MR", operationSubpropertyId.findValue("example").asText());
    }

    @Test
    public void dependantSchemaHasTwoRequiredFields() {
        // verify, that both attributes in Schema1Depending (implementation and ref) are required
        JsonNode requiredItems = path(getOpenAPIJson(), "components.schemas.Schema1Depending.required");
        assertNotNull(requiredItems);
        Iterator<JsonNode> requiredElements = requiredItems.elements();
        // verify one result
        assertTrue(requiredElements.hasNext());
        // verify the name of the required field
        assertEquals("schema2Simple", requiredElements.next().asText());
        // one more result
        assertTrue(requiredElements.hasNext());
        // verify the name of the required field
        assertEquals("schema2SimpleRef", requiredElements.next().asText());
        // no more than two results
        assertFalse(requiredElements.hasNext());
    }
}
