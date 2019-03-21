package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.eclipse.microprofile.openapi.OASFactory.createXML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.media.*}.
 */
public class MediaBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().addSchema("SimpleMap", createSchema()
                .type(SchemaType.OBJECT)
                .additionalPropertiesSchema(createSchema().type(SchemaType.STRING)));

        XML xml = createXML()
                .name("name")
                .namespace("namespace")
                .prefix("prefix")
                .attribute(true)
                .wrapped(true)
                .addExtension("x-ext", "ext-value");

        document.getComponents().addSchema("XML", createSchema()
                .xml(xml));

    }

    @Test
    public void simpleMapSchemaHasExpectedFields() {
        JsonNode mapEntry = path(getOpenAPIJson(), "components.schemas.SimpleMap");
        assertNotNull(mapEntry);
        assertEquals("object", mapEntry.get("type").textValue());
        JsonNode additionalProperties = mapEntry.get("additionalProperties");
        assertNotNull(additionalProperties);
        assertEquals("string", additionalProperties.get("type").textValue());
    }

    @Test
    public void xmlHasExpectedFields() {
        JsonNode xml = path(getOpenAPIJson(), "components.schemas.XML.xml");
        assertNotNull(xml);
        assertEquals("name", xml.get("name").textValue());
        assertEquals("namespace", xml.get("namespace").textValue());
        assertEquals("prefix", xml.get("prefix").textValue());
        assertEquals("ext-value", xml.get("x-ext").textValue());
        assertTrue(xml.get("attribute").booleanValue());
        assertTrue(xml.get("wrapped").booleanValue());
    }
}
