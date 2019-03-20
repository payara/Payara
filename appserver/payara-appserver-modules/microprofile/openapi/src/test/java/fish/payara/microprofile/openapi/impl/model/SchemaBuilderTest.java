package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.toJson;
import static org.eclipse.microprofile.openapi.OASFactory.createObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.spec.OpenApiValidator;

public class SchemaBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().getSchemas().put("SimpleMap", createObject(Schema.class)
                    .type(SchemaType.OBJECT)
                    .additionalPropertiesSchema(createObject(Schema.class).type(SchemaType.STRING)));
    }

    @Test
    public void isValidJsonDocument() {
        OpenApiValidator.validate(toJson(getDocument()));
    }

    @Test
    public void schemaAdditionalPropertiesStructure() {
        ObjectNode openAPI = toJson(getDocument());
        JsonNode mapEntry = path(openAPI, "components.schemas.SimpleMap");
        assertNotNull(mapEntry);
        assertEquals("object", mapEntry.get("type").textValue());
        JsonNode additionalProperties = mapEntry.get("additionalProperties");
        assertNotNull(additionalProperties);
        assertEquals("string", additionalProperties.get("type").textValue());
    }
}
