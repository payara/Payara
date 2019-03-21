package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().getSchemas().put("SimpleMap", createSchema()
                    .type(SchemaType.OBJECT)
                    .additionalPropertiesSchema(createSchema().type(SchemaType.STRING)));
    }

    @Test
    public void schemaHasExpectedFields() {
        ObjectNode openAPI = getOpenAPIJson();
        JsonNode mapEntry = path(openAPI, "components.schemas.SimpleMap");
        assertNotNull(mapEntry);
        assertEquals("object", mapEntry.get("type").textValue());
        JsonNode additionalProperties = mapEntry.get("additionalProperties");
        assertNotNull(additionalProperties);
        assertEquals("string", additionalProperties.get("type").textValue());
    }
}
