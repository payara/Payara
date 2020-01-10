package fish.payara.monitoring.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

/**
 * Tests basic correctness of {@link SeriesAnnotation} type.
 * 
 * @author Jan Bernitt
 */
public class SeriesAnnotationTest {

    @Test
    public void annotationAttributesMustBeGivenInPairs() {
        assertInvalidAttributes("OnlyName");
        assertInvalidAttributes("Name1", "Value1", "OnlyName2");
        assertInvalidAttributes("Name1", "Value1", "Name2", "Value2", "OnlyName2");

        assertValidAttributes();
        assertValidAttributes("Name1", "Value1");
        assertValidAttributes("Name1", "Value1", "Name2", "Value2");
        assertValidAttributes("Name1", "Value1", "Name2", "Value2", "Name3", "Value3");
    }

    private static void assertInvalidAttributes(String...attrs) {
        String attrsString = Arrays.toString(attrs);
        try {
            assertNotNull(new SeriesAnnotation(1L, Series.ANY, "instance", 1L, attrs));
            fail("Expected attributes cause exception but were accepted: " + attrsString);
        } catch (IllegalArgumentException ex) {
            assertEquals("Annotation attributes always must be given in pairs but got: " + attrsString, ex.getMessage());
        }
    }

    private static void assertValidAttributes(String...attrs) {
        assertNotNull(new SeriesAnnotation(1L, Series.ANY, "instance", 1L, attrs));
    }

}
