package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createLink;
import static org.eclipse.microprofile.openapi.OASFactory.createServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class LinksBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().addLink("link1", createLink()
                .operationId("operationId")
                .operationRef("operationRef")
                .description("description")
                .requestBody("requestBody")
                .server(createServer().url("url"))
                .addParameter("param1", "expr1")
                .addParameter("param2", "expr2")
                );
    }

    @Test
    public void linkHasExpectedFields() {
        JsonNode link = path(getOpenAPIJson(), "components.links.link1");
        assertNotNull(link);
        assertEquals("operationRef", link.get("operationRef").textValue());
        assertEquals("operationId", link.get("operationId").textValue());
        assertEquals("requestBody", link.get("requestBody").textValue());
        assertEquals("description", link.get("description").textValue());
        assertTrue(link.get("server").isObject());
        assertTrue(link.get("parameters").isObject());
        assertEquals("expr1", link.get("parameters").get("param1").textValue());
        assertEquals("expr2", link.get("parameters").get("param2").textValue());
    }
}
