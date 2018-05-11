package fish.payara.microprofile.openapi.impl.model.util;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModelUtilsTest {

    private static final String CURRENT_VALUE = "whatever";
    private static final String NEW_VALUE = "something else";
    private static final String NULL = null;

    /**
     * Tests that the function overrides properties correctly.
     */
    @Test
    public void mergePropertyTest() {
        assertEquals(NEW_VALUE, mergeProperty(CURRENT_VALUE, NEW_VALUE, true));
        assertEquals(CURRENT_VALUE, mergeProperty(CURRENT_VALUE, NEW_VALUE, false));
        assertEquals(CURRENT_VALUE, mergeProperty(CURRENT_VALUE, NULL, true));
        assertEquals(CURRENT_VALUE, mergeProperty(CURRENT_VALUE, NULL, false));
        assertEquals(NEW_VALUE, mergeProperty(NULL, NEW_VALUE, true));
        assertEquals(NEW_VALUE, mergeProperty(NULL, NEW_VALUE, false));
        assertEquals(NULL, mergeProperty(NULL, NULL, true));
        assertEquals(NULL, mergeProperty(NULL, NULL, false));
    }

}