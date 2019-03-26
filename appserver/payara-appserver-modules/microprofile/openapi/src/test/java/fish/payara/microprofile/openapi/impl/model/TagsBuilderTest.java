package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createExternalDocumentation;
import static org.eclipse.microprofile.openapi.OASFactory.createTag;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.tags.*}.
 */
public class TagsBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.addTag(createTag()
                .name("name")
                .description("description")
                .externalDocs(createExternalDocumentation().url("url"))
                .addExtension("x-ext", "ext-value"));
    }

    @Test
    public void tagHasExpectedFields() {
        JsonNode tag = path(getOpenAPIJson(), "tags.0");
        assertNotNull(tag);
        assertEquals("name", tag.get("name").textValue());
        assertEquals("description", tag.get("description").textValue());
        assertEquals("ext-value", tag.get("x-ext").textValue());
        assertTrue(tag.get("externalDocs").isObject());
    }
}
