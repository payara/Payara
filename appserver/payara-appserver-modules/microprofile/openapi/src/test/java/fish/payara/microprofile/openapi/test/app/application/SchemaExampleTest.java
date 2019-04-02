package fish.payara.microprofile.openapi.test.app.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.visitor.OpenApiWalker;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.util.JsonUtils;

/**
 * This is a minimal example extracted from a failing TCK test where the {@link Schema} details of a bean field were
 * missing from the output.
 * 
 * These should occur as {@link SchemaImpl#getProperties()}.
 */
@Path("/users")
public class SchemaExampleTest extends OpenApiApplicationTest {

    @Schema(maxProperties = 1024, minProperties = 1, requiredProperties = { "password" })
    public static class User {
        @Schema(example = "bobSm37")
        String password;
    }

    /**
     * Interestingly adding this method that uses the {@link User} class could cause the properties of the
     * {@link User#password} schema to disappear from the model and output. This is worked around by the
     * {@link OpenApiWalker} processing {@link Schema} annotation twice.
     */
    @GET
    public User getUser(@SuppressWarnings("unused") @PathParam("id") String id) {
        return new User();
    }

    @Test
    public void fieldSchemaExampleIsRendered() {
        JsonNode passwordProperties = JsonUtils.path(getOpenAPIJson(), "components.schemas.User.properties.password");
        assertNotNull(passwordProperties);
        assertEquals("string", passwordProperties.get("type").textValue());
        assertEquals("bobSm37", passwordProperties.get("example").textValue());
    }
}
