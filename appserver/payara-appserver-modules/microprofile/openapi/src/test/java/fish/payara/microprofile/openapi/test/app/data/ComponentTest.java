package fish.payara.microprofile.openapi.test.app.data;

import static org.junit.Assert.assertNull;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A test component to be added to the model.
 */
@SuppressWarnings("unused")
public class ComponentTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new ApplicationProcessedDocument();
    }

    @Test
    public void ignoreTransientFieldTest() {
        assertNull(document.getComponents().getSchemas().get("ComponentTest").getProperties().get("document"));
    }

    // Should be INTEGER type
    private int id;
    private Integer id2;

    @Test
    public void testIntegers() {
        TestUtils.testComponentProperty(document, "ComponentTest", "id", SchemaType.INTEGER);
        TestUtils.testComponentProperty(document, "ComponentTest", "id2", SchemaType.INTEGER);
    }

    // Should be NUMBER type
    private float $ref;
    private Float $ref2;
    private double $ref3;
    private Double $ref4;

    @Test
    public void testNumbers() {
        TestUtils.testComponentProperty(document, "ComponentTest", "$ref", SchemaType.NUMBER);
        TestUtils.testComponentProperty(document, "ComponentTest", "$ref2", SchemaType.NUMBER);
        TestUtils.testComponentProperty(document, "ComponentTest", "$ref3", SchemaType.NUMBER);
        TestUtils.testComponentProperty(document, "ComponentTest", "$ref4", SchemaType.NUMBER);
    }

    // Should be String type
    private String this$0;

    @Test
    public void testStrings() {
        TestUtils.testComponentProperty(document, "ComponentTest", "this$0", SchemaType.STRING);
    }

    // Should be BOOLEAN type
    private boolean bool;
    private Boolean bool2;

    @Test
    public void testBooleans() {
        TestUtils.testComponentProperty(document, "ComponentTest", "bool", SchemaType.BOOLEAN);
        TestUtils.testComponentProperty(document, "ComponentTest", "bool2", SchemaType.BOOLEAN);
    }

    // Should be OBJECT type
    private Object _var_;

    @Test
    public void testObjects() {
        TestUtils.testComponentProperty(document, "ComponentTest", "_var_", SchemaType.OBJECT);
    }

    // Should be ARRAY type
    private String[] array;
    private Object[] array2;
    private int[][] array3;

    @Test
    public void testArrays() {
        TestUtils.testComponentProperty(document, "ComponentTest", "array", SchemaType.ARRAY, SchemaType.STRING);
        TestUtils.testComponentProperty(document, "ComponentTest", "array2", SchemaType.ARRAY, SchemaType.OBJECT);
        TestUtils.testComponentProperty(document, "ComponentTest", "array3", SchemaType.ARRAY, SchemaType.ARRAY,
                SchemaType.INTEGER);
    }

}