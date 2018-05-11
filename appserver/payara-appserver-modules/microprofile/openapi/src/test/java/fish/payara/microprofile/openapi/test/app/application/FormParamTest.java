package fish.payara.microprofile.openapi.test.app.application;

import static java.util.Collections.singletonMap;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that various types of form parameters are successfully
 * generalised into one data type.
 */
@Path("/form")
public class FormParamTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @POST
    @Path("/stringint")
    public String getStringAndInt(@FormParam("stringParam") String stringParam, @FormParam("intParam") int intParam) {
        return null;
    }

    @Test
    public void stringIntTest() {
        TestUtils.testRequestBody(document, "/test/form/stringint", HttpMethod.POST,
                singletonMap("*/*", SchemaType.STRING));
    }

    @POST
    @Path("/intstring")
    public String getIntAndString(@FormParam("intParam") int intParam, @FormParam("stringParam") String stringParam) {
        return null;
    }

    @Test
    public void intStringTest() {
        TestUtils.testRequestBody(document, "/test/form/intstring", HttpMethod.POST,
                singletonMap("*/*", SchemaType.STRING));
    }

    @POST
    @Path("/intint")
    public String getIntAndInt(@FormParam("intParam") int intParam, @FormParam("stringParam") int intParam2) {
        return null;
    }

    @Test
    public void intIntTest() {
        TestUtils.testRequestBody(document, "/test/form/intint", HttpMethod.POST,
                singletonMap("*/*", SchemaType.INTEGER));
    }

}