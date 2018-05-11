package fish.payara.microprofile.openapi.test.app.application;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test the results of multiple methods sharing the same endpoint.
 */
@Path("/merge")
public class MethodMergeTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_XML)
    public Object jsonToXml(Object data) {
        return data;
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Object formToJson(@FormParam("paramName") String name, @FormParam("paramValue") int value) {
        return null;
    }

    @Test
    public void testMergedEndpoint() {
        TestUtils.testOperation(document, "/test/merge", null, HttpMethod.POST);
        Operation operation = document.getPaths().get("/test/merge").getPOST();
        if ("jsonToXml".equals(operation.getOperationId())) {
            TestUtils.testRequestBody(document, "/test/merge", HttpMethod.POST,
                    singletonMap(APPLICATION_JSON, SchemaType.OBJECT));
            TestUtils.testResponse(document, "/test/merge", HttpMethod.POST,
                    singletonMap(APPLICATION_XML, SchemaType.OBJECT));
        }
        if ("formToJson".equals(operation.getOperationId())) {
            TestUtils.testRequestBody(document, "/test/merge", HttpMethod.POST,
                    singletonMap(APPLICATION_FORM_URLENCODED, SchemaType.STRING));
            TestUtils.testResponse(document, "/test/merge", HttpMethod.POST,
                    singletonMap(APPLICATION_JSON, SchemaType.OBJECT));
        }
    }

}