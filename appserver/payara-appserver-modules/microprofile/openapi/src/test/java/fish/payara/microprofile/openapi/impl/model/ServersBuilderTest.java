package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static java.util.Collections.singletonMap;
import static org.eclipse.microprofile.openapi.OASFactory.createServer;
import static org.eclipse.microprofile.openapi.OASFactory.createServerVariable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.servers.*}.
 */
public class ServersBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.addServer(createServer()
                .url("url")
                .description("description")
                .addExtension("x-ext", "ext-value")
                .variables(singletonMap("var1", createServerVariable()
                        .defaultValue("defaultValue")
                        .description("description")
                        .addEnumeration("enumeration1")
                        .addEnumeration("enumeration2")
                        .addExtension("x-ext", "ext-value"))));
    }

    @Test
    public void serverHasExpectedFields() {
        JsonNode server = path(getOpenAPIJson(), "servers.0");
        assertNotNull(server);
        assertEquals("url", server.get("url").textValue());
        assertEquals("description", server.get("description").textValue());
        assertEquals("ext-value", server.get("x-ext").textValue());
    }

    @Test
    public void serverVariablesHasExpectedFields() {
        JsonNode var1 = path(getOpenAPIJson(), "servers.0.variables.var1");
        assertNotNull(var1);
        assertEquals("defaultValue", var1.get("default").textValue());
        assertEquals("description", var1.get("description").textValue());
        assertEquals("ext-value", var1.get("x-ext").textValue());
        ArrayNode enumeration = (ArrayNode) var1.get("enum");
        assertNotNull(enumeration);
        assertEquals(2, enumeration.size());
        assertEquals("enumeration1", enumeration.get(0).textValue());
        assertEquals("enumeration2", enumeration.get(1).textValue());
    }
}
