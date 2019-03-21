package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.examples.*}.
 */
public class ExamplesBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents()
            .addExample("example1", createExample()
                    .description("description")
                    .summary("summary")
                    .value("value")
                    .externalValue("externalValue")
                    .addExtension("x-ext", "ext-value")
                    )
            .addExample("example2", createExample()
                    .value(new ObjectMapper().createArrayNode().add("a").add("b"))
                    )
            .addExample("example3", createExample()
                    .value(Arrays.asList("a", "b")));
    }

    @Test
    public void exampleHasExpectedFields() {
        JsonNode example1 = path(getOpenAPIJson(), "components.examples.example1");
        assertNotNull(example1);
        assertEquals("summary", example1.get("summary").textValue());
        assertEquals("description", example1.get("description").textValue());
        assertEquals("externalValue", example1.get("externalValue").textValue());
        assertEquals("value", example1.get("value").textValue());
        assertEquals("ext-value", example1.get("x-ext").textValue());
    }

    @Test
    public void exampleCanUseJsonValue() {
        JsonNode example2 = path(getOpenAPIJson(), "components.examples.example2");
        assertNotNull(example2);
        assertTrue(example2.get("value").isArray());
        assertEquals("a", example2.get("value").get(0).textValue());
        assertEquals("b", example2.get("value").get(1).textValue());
    }

    @Test
    public void exampleCanUseCollectionValue() {
        JsonNode example3 = path(getOpenAPIJson(), "components.examples.example3");
        assertNotNull(example3);
        assertTrue(example3.get("value").isArray());
        assertEquals("a", example3.get("value").get(0).textValue());
        assertEquals("b", example3.get("value").get(1).textValue());
    }
}
