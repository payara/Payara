package fish.payara.microprofile.openapi.test.app.application;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that various types of request body are created properly.
 */
@Path("/request")
public class RequestTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @GET
    @Path("/none")
    public String untyped(Object data, @QueryParam("ignoreme") String param) {
        return null;
    }

    @Test
    public void testUntyped() {
        TestUtils.testRequestBody(document, "/test/request/none", HttpMethod.GET,
                singletonMap("*/*", SchemaType.OBJECT));
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Path("/form")
    public String form(@FormParam("test") String test, @QueryParam("ignoreme") String param) {
        return null;
    }

    @Test
    public void testForm() {
        TestUtils.testRequestBody(document, "/test/request/form", HttpMethod.POST,
                singletonMap(APPLICATION_FORM_URLENCODED, SchemaType.STRING));
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Path("/json")
    public String json(String[] data) {
        return null;
    }

    @Test
    public void testArray() {
        TestUtils.testRequestBody(document, "/test/request/json", HttpMethod.GET,
                singletonMap(APPLICATION_JSON, SchemaType.ARRAY));
    }

    @GET
    @Consumes({ APPLICATION_XML, TEXT_PLAIN })
    @Path("/multiple")
    public String multiple(Boolean data) {
        return null;
    }

    @Test
    public void testMultiple() {
        TestUtils.testRequestBody(document, "/test/request/multiple", HttpMethod.GET,
                singletonMap(APPLICATION_XML, SchemaType.BOOLEAN));
        TestUtils.testRequestBody(document, "/test/request/multiple", HttpMethod.GET,
                singletonMap(TEXT_PLAIN, SchemaType.BOOLEAN));
    }
}