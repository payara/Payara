package fish.payara.microprofile.openapi.test.app.application;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.MediaType.WILDCARD;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;
import fish.payara.microprofile.openapi.test.app.data.ComponentTest;

/**
 * A resource to test that various response types are mapped properly.
 */
@Path("/response")
public class ResponseTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @GET
    @Path("/none")
    public String none() {
        return null;
    }

    @Test
    public void noResponseTest() {
        TestUtils.testResponse(document, "/test/response/none", HttpMethod.GET,
                singletonMap(WILDCARD, SchemaType.STRING));
    }

    @GET
    @Path("/component")
    public ComponentTest component() {
        return null;
    }

    @Test
    public void componentResponseTest() {
        TestUtils.testResponse(document, "/test/response/component", HttpMethod.GET,
                singletonMap(WILDCARD, "#/components/schemas/ComponentTest"));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/json")
    public Object json() {
        return null;
    }

    @Test
    public void jsonResponseTest() {
        TestUtils.testResponse(document, "/test/response/json", HttpMethod.GET,
                singletonMap(APPLICATION_JSON, SchemaType.OBJECT));
    }

    @GET
    @Produces(TEXT_PLAIN)
    @Path("/text")
    public Float text() {
        return null;
    }

    @Test
    public void floatResponseTest() {
        TestUtils.testResponse(document, "/test/response/text", HttpMethod.GET,
                singletonMap(TEXT_PLAIN, SchemaType.NUMBER));
    }

    @GET
    @Produces({ APPLICATION_XML, TEXT_XML })
    @Path("/multiple")
    public Integer multiple() {
        return null;
    }

    @Test
    public void multipleResponsesTest() {
        TestUtils.testResponse(document, "/test/response/multiple", HttpMethod.GET,
                singletonMap(APPLICATION_XML, SchemaType.INTEGER));
        TestUtils.testResponse(document, "/test/response/multiple", HttpMethod.GET,
                singletonMap(TEXT_XML, SchemaType.INTEGER));
    }
}