package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

/**
 * TCK lacks tests for parameter annotations being used on fields which should add such parameters to all methods of the
 * bean.
 */
@Path("/field-params")
public class FieldParamTest extends OpenApiApplicationTest {

    @PathParam("path")
    private String pathParam;

    @CookieParam("cookie")
    private String cookieParam; 

    @HeaderParam("header")
    private String headerParam;

    @QueryParam("query")
    private String queryParam;

    @GET
    public String getMethod() {
        return "";
    }

    @PUT
    public void putMethod(@SuppressWarnings("unused") @PathParam("extra") String extra) {
        // just exist to see the document for them
    }

    @Test
    public void pathParamIsAddedToApiMethods() {
        assertHasParameter("path", true);
    }

    @Test
    public void cookieParamIsAddedToApiMethods() {
        assertHasParameter("cookie", false);
    }

    @Test
    public void headerParamIsAddedToApiMethods() {
        assertHasParameter("header", false);
    }

    @Test
    public void queryParamIsAddedToApiMethods() {
        assertHasParameter("query", false);
    }

    @Test
    public void fieldParamsDoNotRemoveParameterParams() {
        assertEquals(4, path(getOpenAPIJson(), "paths./test/field-params.get.parameters").size());
        assertEquals(5, path(getOpenAPIJson(), "paths./test/field-params.put.parameters").size());
        assertParameter("extra", true, "paths./test/field-params.put.parameters");

    }

    private void assertHasParameter(String name, boolean required) {
        assertParameter(name, required, "paths./test/field-params.get.parameters");
        assertParameter(name, required, "paths./test/field-params.put.parameters");
    }

    private void assertParameter(String name, boolean required, String objectPath) {
        JsonNode parameter = parameterWithName(name, path(getOpenAPIJson(), objectPath));
        assertNotNull(parameter);
        assertRequired(required, parameter);
    }

    private static void assertRequired(boolean required, JsonNode parameter) {
        JsonNode requiredField = parameter.get("required");
        if (required) {
            assertTrue(requiredField.booleanValue());
        } else {
            assertTrue(requiredField == null || requiredField.isNull());
        }
    }

    private static JsonNode parameterWithName(String name, JsonNode parameters) {
        for (JsonNode parameter : parameters) {
            if (parameter.get("name").textValue().equals(name)) {
                return parameter;
            }
        }
        return null;
    }
}
