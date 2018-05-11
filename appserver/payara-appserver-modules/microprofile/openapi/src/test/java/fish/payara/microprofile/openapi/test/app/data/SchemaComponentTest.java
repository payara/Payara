package fish.payara.microprofile.openapi.test.app.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;

@Schema(name = "TestComponent", title = "Test Component", description = "A component for testing the @Schema annotation.")
public class SchemaComponentTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @Test
    public void schemaCreationTest() {
        org.eclipse.microprofile.openapi.models.media.Schema component = document.getComponents().getSchemas()
                .get("TestComponent");
        assertNotNull("TestComponent was not found.", component);
        assertEquals("TestComponent had the wrong title.", "Test Component", component.getTitle());
        assertEquals("TestComponent had the wrong description.", "A component for testing the @Schema annotation.",
                component.getDescription());
    }

    @Schema(name = "componentHeight", title = "The height of the component.", example = "50m")
    public int height;

    @Test
    public void heightCreationTest() {
        org.eclipse.microprofile.openapi.models.media.Schema component = document.getComponents().getSchemas()
                .get("TestComponent").getProperties().get("componentHeight");
        assertNotNull("TestComponent had no height property.", component);
        assertEquals("TestComponent height had the wrong type.",
                org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.INTEGER, component.getType());
        assertEquals("TestComponent height had the wrong example.", "50m", component.getExample());
        assertEquals("TestComponent height had the wrong title.", "The height of the component.", component.getTitle());
    }

    @Schema(title = "An ignored reference to Schema1", ref = "Schema1")
    public Object reference;

    @Test
    public void referenceCreationTest() {
        org.eclipse.microprofile.openapi.models.media.Schema component = document.getComponents().getSchemas()
                .get("TestComponent").getProperties().get("reference");
        assertNotNull("TestComponent had no reference property.", component);
        assertNull("TestComponent reference had a type.", component.getType());
        assertNull("TestComponent title should be empty.", component.getTitle());
        assertEquals("TestComponent reference had the wrong reference.", "#/components/schemas/Schema1",
                component.getRef());
    }

}