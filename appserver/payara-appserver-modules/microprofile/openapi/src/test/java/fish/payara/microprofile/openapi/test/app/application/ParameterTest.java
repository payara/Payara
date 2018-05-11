package fish.payara.microprofile.openapi.test.app.application;

import java.util.Collections;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test various types of parameters.
 */
@Path("/parameter")
public class ParameterTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    // Recognise parameter name

    @GET
    @Path("/name")
    public String name(@QueryParam("name1") String blarg, @QueryParam("name2") int glarg) {
        return null;
    }

    @Test
    public void testParameterName() {
        TestUtils.testParameter(document, "/test/parameter/name", HttpMethod.GET,
                Collections.singletonMap("name1", In.QUERY));
        TestUtils.testParameter(document, "/test/parameter/name", HttpMethod.GET,
                Collections.singletonMap("name2", In.QUERY));
    }

    // Recognise different types of parameters

    @GET
    @Path("/query")
    public String query(@QueryParam("param") String param) {
        return null;
    }

    @Test
    public void testQueryParameter() {
        TestUtils.testParameter(document, "/test/parameter/query", HttpMethod.GET,
                Collections.singletonMap("param", In.QUERY));
    }

    @GET
    @Path("/path/{param}")
    public String path(@PathParam("param") String param) {
        return null;
    }

    @Test
    public void testPathParameter() {
        TestUtils.testParameter(document, "/test/parameter/path/{param}", HttpMethod.GET,
                Collections.singletonMap("param", In.PATH));
    }

    @GET
    @Path("/cookie")
    public String cookie(@CookieParam("param") String param) {
        return null;
    }

    @Test
    public void testCookieParameter() {
        TestUtils.testParameter(document, "/test/parameter/cookie", HttpMethod.GET,
                Collections.singletonMap("param", In.COOKIE));
    }

    @GET
    @Path("/header")
    public String header(@HeaderParam("param") String param) {
        return null;
    }

    @Test
    public void testHeaderParameter() {
        TestUtils.testParameter(document, "/test/parameter/header", HttpMethod.GET,
                Collections.singletonMap("param", In.HEADER));
    }

    // PARAMETERS EXPECTED TO BE IGNORED

    @GET
    @Path("/fake")
    public String fake(/* Not a parameter */ String param) {
        return null;
    }

    @Test
    public void testFakeParameter() {
        TestUtils.testParameter(document, "/test/parameter/fake", HttpMethod.GET, null);
    }

    @GET
    @Path("/form")
    public String form(@FormParam("param") String param) {
        return null;
    }

    @Test
    public void testFormParameter() {
        TestUtils.testParameter(document, "/test/parameter/form", HttpMethod.GET, null);
    }

    @GET
    @Path("/matrix")
    public String matrix(@MatrixParam("param") String param) {
        return null;
    }

    @Test
    public void testMatrixParameter() {
        TestUtils.testParameter(document, "/test/parameter/matrix", HttpMethod.GET, null);
    }

    @GET
    @Path("/context")
    public String context(@Context UriInfo uriInfo) {
        return null;
    }

    @Test
    public void testContextParameter() {
        TestUtils.testParameter(document, "/test/parameter/context", HttpMethod.GET, null);
    }
}