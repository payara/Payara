package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertNotNull;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.junit.Test;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

/**
 * Test to verify that using {@code hidden} does not cause errors as suggested by PAYARA-3259.
 */
@Path("/example")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HiddenTest extends OpenApiApplicationTest {

    @GET
    @Path("/{name}")
    @Operation(hidden = true)
    public Response hello(@PathParam("name") String name) {
        JsonObject message = Json.createObjectBuilder()
                .add("message", "hello" + name)
                .build();
        return Response.ok(message).build();
    }

    @Test
    public void hiddenPropertyDoesNotCauseErrors() {
        assertNotNull(path(getOpenAPIJson(), "paths./test/example/{name}"));
    }
}
