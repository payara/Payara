package fish.payara.microprofile.openapi.test.app.annotation;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that the @RequestBody annotation is handled correctly.
 */
@Path("/requestbody")
public class RequestBodyTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/empty")
    @RequestBody
    public String emptyRequestBody() {
        return null;
    }

    @Test
    public void testEmptyRequestBody() {
        TestUtils.testOperation(document, "/test/requestbody/empty", "emptyRequestBody", HttpMethod.GET);
        TestUtils.testRequestBody(document, "/test/requestbody/empty", HttpMethod.GET, null);
    }

    @GET
    @Path("/specified")
    @Consumes(APPLICATION_XML)
    @RequestBody(description = "specified by an annotation.", required = true)
    public String specifiedRequestBody(String test, @Context UriInfo info) {
        return null;
    }

    @Test
    public void testSpecifiedRequestBody() {
        TestUtils.testOperation(document, "/test/requestbody/specified", "specifiedRequestBody", HttpMethod.GET);
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = TestUtils.testRequestBody(document,
                "/test/requestbody/specified", HttpMethod.GET, singletonMap(APPLICATION_XML, SchemaType.STRING));
        assertEquals("/test/requestbody/specified requestBody had the wrong description.",
                "specified by an annotation.", requestBody.getDescription());
        assertEquals("/test/requestbody/specified requestBody had the wrong required value.", true,
                requestBody.getRequired());
    }

    @POST
    @Path("/override")
    @RequestBody(name = "ignored", description = "ignore me", required = true)
    public String overrideRequestBody(
            @RequestBody(name = "override", description = "overrides method.", required = false) String input) {
        return null;
    }

    @Test
    public void testOverridenRequestBody() {
        TestUtils.testOperation(document, "/test/requestbody/override", "overrideRequestBody", HttpMethod.POST);
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = TestUtils.testRequestBody(document,
                "/test/requestbody/override", HttpMethod.POST, singletonMap(WILDCARD, SchemaType.STRING));
        assertEquals("/test/requestbody/override requestBody had the wrong description.", "overrides method.",
                requestBody.getDescription());
        assertEquals("/test/requestbody/override requestBody had the wrong required value.", false,
                requestBody.getRequired());
    }

}