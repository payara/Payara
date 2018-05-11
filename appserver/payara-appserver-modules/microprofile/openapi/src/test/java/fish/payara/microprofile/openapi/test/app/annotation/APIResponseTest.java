package fish.payara.microprofile.openapi.test.app.annotation;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that the @APIResponse annotation is handled correctly.
 */
@Path("/apiresponse")
public class APIResponseTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/default")
    @Produces(APPLICATION_XML)
    @APIResponse(description = "specified by an annotation.")
    public String defaultResponse() {
        return null;
    }

    @Test
    public void testDefaultResponse() {
        TestUtils.testOperation(document, "/test/apiresponse/default", "defaultResponse", HttpMethod.GET);
        org.eclipse.microprofile.openapi.models.responses.APIResponse response = TestUtils.testResponse(document,
                "/test/apiresponse/default", HttpMethod.GET, singletonMap(APPLICATION_XML, SchemaType.STRING));
        assertEquals("/test/apiresponse/default response had the wrong description.", "specified by an annotation.",
                response.getDescription());
    }

    @GET
    @Path("/specified")
    @Produces(APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "specified by an annotation.")
    public String specifiedResponse() {
        return null;
    }

    @Test
    public void testSpecifiedResponse() {
        TestUtils.testOperation(document, "/test/apiresponse/specified", "specifiedResponse", HttpMethod.GET);

        // Check the default response is still in tact
        org.eclipse.microprofile.openapi.models.responses.APIResponse defaultResponse = TestUtils.testResponse(document,
                "/test/apiresponse/specified", HttpMethod.GET, singletonMap(APPLICATION_JSON, SchemaType.STRING));
        assertEquals("Description was incorrect.", "Default Response.", defaultResponse.getDescription());

        // Check the new response is created
        org.eclipse.microprofile.openapi.models.responses.APIResponse successResponse = document.getPaths()
                .get("/test/apiresponse/specified").getGET().getResponses().get("200");
        assertEquals("Description was incorrect.", "specified by an annotation.", successResponse.getDescription());
    }

}