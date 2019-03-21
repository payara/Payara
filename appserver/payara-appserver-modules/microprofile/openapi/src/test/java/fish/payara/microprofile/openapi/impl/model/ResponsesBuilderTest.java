package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponse;
import static org.eclipse.microprofile.openapi.OASFactory.createContent;
import static org.eclipse.microprofile.openapi.OASFactory.createHeader;
import static org.eclipse.microprofile.openapi.OASFactory.createLink;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.responses.*}.
 */
public class ResponsesBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().addResponse("response1", createAPIResponse()
                .description("description")
                .addHeader("header1", createHeader().ref("ref"))
                .addLink("link1", createLink().ref("ref"))
                .addExtension("x-ext", "ext-value")
                .content(createContent())
                );
    }

    @Test
    public void pathHasExpectedFields() {
        JsonNode response = path(getOpenAPIJson(), "components.responses.response1");
        assertNotNull(response);
        assertEquals("description", response.get("description").textValue());
        assertEquals("ext-value", response.get("x-ext").textValue());
        assertTrue(response.get("headers").isObject());
        assertTrue(response.get("headers").get("header1").isObject());
        assertTrue(response.get("links").isObject());
        assertTrue(response.get("links").get("link1").isObject());
    }
}
