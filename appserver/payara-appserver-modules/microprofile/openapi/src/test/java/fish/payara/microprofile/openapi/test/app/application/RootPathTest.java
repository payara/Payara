package fish.payara.microprofile.openapi.test.app.application;

import java.util.Collections;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that a resource at the context root is mapped correctly.
 */
@Path("/")
public class RootPathTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @GET
    public String getRoot() {
        return null;
    }

    @Test
    public void testRoot() {
        TestUtils.testOperation(document, "/test", "getRoot", HttpMethod.GET);
        TestUtils.testResponse(document, "/test", HttpMethod.GET, Collections.singletonMap("*/*", SchemaType.STRING));
        TestUtils.testParameter(document, "/test", HttpMethod.GET, null);
    }
}