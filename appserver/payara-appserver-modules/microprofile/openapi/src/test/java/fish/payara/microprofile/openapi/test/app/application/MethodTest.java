package fish.payara.microprofile.openapi.test.app.application;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test each of the JAX-RS methods.
 */
@Path("/method")
public class MethodTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @GET
    public String getResource() {
        return null;
    }

    @Test
    public void testGet() {
        TestUtils.testOperation(document, "/test/method", "getResource", HttpMethod.GET);
    }

    @POST
    public String postResource() {
        return null;
    }

    @Test
    public void testPost() {
        TestUtils.testOperation(document, "/test/method", "postResource", HttpMethod.POST);
    }

    @PUT
    public String putResource() {
        return null;
    }

    @Test
    public void testPut() {
        TestUtils.testOperation(document, "/test/method", "putResource", HttpMethod.PUT);
    }

    @DELETE
    public String deleteResource() {
        return null;
    }

    @Test
    public void testDelete() {
        TestUtils.testOperation(document, "/test/method", "deleteResource", HttpMethod.DELETE);
    }

    @HEAD
    public String headResource() {
        return null;
    }

    @Test
    public void testHead() {
        TestUtils.testOperation(document, "/test/method", "headResource", HttpMethod.HEAD);
    }

    @OPTIONS
    public String optionsResource() {
        return null;
    }

    @Test
    public void testOptions() {
        TestUtils.testOperation(document, "/test/method", "optionsResource", HttpMethod.OPTIONS);
    }

    @PATCH
    public String patchResource() {
        return null;
    }

    @Test
    public void testPatch() {
        TestUtils.testOperation(document, "/test/method", "patchResource", HttpMethod.PATCH);
    }
}