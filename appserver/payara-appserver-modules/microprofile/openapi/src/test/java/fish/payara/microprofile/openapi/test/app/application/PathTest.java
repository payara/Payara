package fish.payara.microprofile.openapi.test.app.application;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that various paths are created correctly.
 */
@Path("/path")
public class PathTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @GET
    public String getRootResource() {
        return null;
    }

    @Test
    public void testRootResource() {
        TestUtils.testOperation(document, "/test/path", "getRootResource", HttpMethod.GET);
    }

    @GET
    @Path("/1")
    public String getResource() {
        return null;
    }

    @Test
    public void testResource() {
        TestUtils.testOperation(document, "/test/path/1", "getResource", HttpMethod.GET);
    }

}