package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

/**
 * In response to {@link https://github.com/payara/Payara/issues/3832} this test should make sure the
 * {@link org.eclipse.microprofile.openapi.models.media.Schema} is constructed and rendered correctly for the example
 * given.
 */
@Path("/servers")
public class SchemaReferenceTest extends OpenApiApplicationTest {

    @Schema(name = "Server")
    public class Server {
        @Schema(name = "name", required = true)
        private String name;

        public Server(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Server> getServers() {
        return Arrays.asList(new Server("Server A"), new Server("Server B"));
    }

    @Test
    public void test() {
        JsonNode items = path(getOpenAPIJson(), 
                "paths./test/servers.get.responses.default.content.application/json.schema.items");
        assertNotNull(items);
        assertEquals("#/components/schemas/Server", items.get("$ref").textValue());
    }
}
