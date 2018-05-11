package fish.payara.microprofile.openapi.test.app.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.test.app.data.SchemaComponentTest;

@Path("/schema")
public class SchemaTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/schema1")
    @APIResponse(content = @Content(schema = @Schema(ref = "Schema1")))
    public Object getSchema1Response() {
        return null;
    }

    @Test
    public void responseSchemaTest() {
        org.eclipse.microprofile.openapi.models.responses.APIResponse response = document.getPaths()
                .get("/test/schema/schema1").getGET().getResponses().get(APIResponses.DEFAULT);
        assertNotNull("No default response found.", response);
        assertNotNull("No content found.", response.getContent());
        assertNotNull("No mediatype found.", response.getContent().get("*/*"));
        org.eclipse.microprofile.openapi.models.media.Schema schema = response.getContent().get("*/*").getSchema();
        assertNotNull("No content schema found.", schema);
        assertEquals("Incorrect reference.", "#/components/schemas/Schema1", schema.getRef());
        assertNull("Incorrect type.", schema.getType());
    }

    @POST
    @Path("/array")
    @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(description = "An array of strings.", type = SchemaType.ARRAY, format = "[item1,item2,item3]")))
    @Consumes(MediaType.APPLICATION_JSON)
    public String postStringArray(String[] value) {
        return null;
    }

    @Test
    public void newRequestSchemaTest() {
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = document.getPaths()
                .get("/test/schema/array").getPOST().getRequestBody();
        assertNotNull("No request body found.", requestBody);
        assertNotNull("No content found.", requestBody.getContent());
        assertNotNull("No mediatype found.", requestBody.getContent().get(MediaType.APPLICATION_JSON));
        org.eclipse.microprofile.openapi.models.media.Schema schema = requestBody.getContent()
                .get(MediaType.APPLICATION_JSON).getSchema();
        assertNotNull("No content schema found.", schema);
        assertEquals("Incorrect schema description.", "An array of strings.", schema.getDescription());
        assertEquals("Incorrect schema schema type.",
                org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.ARRAY, schema.getType());
        assertEquals("Incorrect schema format.", "[item1,item2,item3]", schema.getFormat());
    }

    @POST
    @Path("/component")
    public String addSchemaComponent(@Schema(ref = "TestComponent") Object component) {
        return null;
    }

    @Test
    public void parameterSchemaTest() {
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = document.getPaths()
                .get("/test/schema/component").getPOST().getRequestBody();
        assertNotNull("No request body found.", requestBody);
        assertNotNull("No content found.", requestBody.getContent());
        assertNotNull("No mediatype found.", requestBody.getContent().get("*/*"));
        org.eclipse.microprofile.openapi.models.media.Schema schema = requestBody.getContent().get("*/*").getSchema();
        assertNotNull("No content schema found.", schema);
        assertEquals("Incorrect reference.", "#/components/schemas/TestComponent", schema.getRef());
        assertNull("Incorrect type.", schema.getType());
    }

    @POST
    @Path("/component/override")
    @RequestBody(content = @Content(schema = @Schema(title = "Schema Component RequestBody", implementation = SchemaComponentTest.class)))
    public String addOverridenSchemaComponent(SchemaComponentTest component) {
        return null;
    }

    @Test
    public void overridenSchemaTest() {
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = document.getPaths()
                .get("/test/schema/component/override").getPOST().getRequestBody();
        assertNotNull("No request body found.", requestBody);
        assertNotNull("No content found.", requestBody.getContent());
        assertNotNull("No mediatype found.", requestBody.getContent().get("*/*"));
        org.eclipse.microprofile.openapi.models.media.Schema schema = requestBody.getContent().get("*/*").getSchema();
        assertNotNull("No content schema found.", schema);
        assertNull("Incorrect reference.", schema.getRef());
        assertEquals("Incorrect schema title.", "Schema Component RequestBody", schema.getTitle());
        assertEquals("Incorrect schema description.", "A component for testing the @Schema annotation.",
                schema.getDescription());
    }

}