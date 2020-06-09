package fish.payara.nucleus.microprofile.config.spi;

import static fish.payara.nucleus.microprofile.config.spi.ConfigTestUtils.assertException;
import static fish.payara.nucleus.microprofile.config.spi.ConfigTestUtils.createSource;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests formal correctness of the {@link ConfigValueResolver} API.
 *
 * Any property can be resolved as {@link ConfigValueResolver} using {@link Config#getValue(String, Class)} with
 * {@link ConfigValueResolver} as target type. The returned instance can be used to resolve the value of the property
 * with more control over the failure behaviour and used defaults.
 *
 * @author Jan Bernitt
 */
public class ConfigValueResolverTest {

    private static final long CACHE_TTL = 50L;

    private final ConfigSource source1 = createSource("S1", 100, new HashMap<>());
    private final Config config = new PayaraConfig(asList(source1), emptyMap(), CACHE_TTL);

    @Before
    public void setUp() {
        String[][] source =  {
                {"string1", "str"},
                {"long1", "42"},
                {"bool1", "true" },
                {"int1", "13"},
                {"brokenlong", "fourtytwo"},
                {"brokenint", "one" },

        };
        Map<String, String> properties = source1.getProperties();
        for (String[] pair : source) {
            properties.put(pair[0], pair[1]);
        }
    }

    private ConfigValueResolver resolve(String propertyName) {
        return config.getValue(propertyName, ConfigValueResolver.class);
    }

    @Test
    public void asString() {
        assertEquals("str", resolve("string1").as(String.class).get());
    }

    @Test
    public void asLong() {
        assertEquals(Long.valueOf(42L), resolve("long1").as(Long.class).get());
    }

    @Test
    public void asBoolean() {
        assertEquals(Boolean.TRUE, resolve("bool1").as(Boolean.class).get());
    }

    @Test
    public void asPrimitiveBoolean() {
        assertTrue(resolve("bool1").as(boolean.class).get().booleanValue());
    }


    /*
     * Ineffective Raw and Typed Defaults
     */

    @Test
    public void asStringWithTypedDefault() {
        assertEquals("str", resolve("string1").as(String.class, ""));
    }

    @Test
    public void asLongWithTypedDefault() {
        assertEquals(42L, resolve("long1").as(long.class, -1L).longValue());
    }

    @Test
    public void asBooleanWithTypedDefault() {
        assertEquals(Boolean.TRUE, resolve("bool1").as(Boolean.class, false));
    }

    @Test
    public void asPrimitiveIntWithTypedDefault() {
        assertEquals(13, resolve("int1").as(int.class, -1).intValue());
    }

    @Test
    public void asPrimitiveBooleanWithTypedDefault() {
        assertTrue(resolve("bool1").as(Boolean.class, false).booleanValue());
    }


    /*
     * Effective Raw and Typed Defaults
     */

    @Test
    public void asStringReturnsProvidedTypedDefault() {
        assertEquals("fallback", resolve("nosuchstr").as(String.class, "fallback"));
    }

    @Test
    public void asStringReturnsProvidedRawDefault() {
        assertEquals("fallback", resolve("nosuchstr").withDefault("fallback").as(String.class).get());
    }

    @Test
    public void asStringReturnsProvidedRawDefaultOverProvidedTypedDefault() {
        assertEquals("fallback", resolve("nosuchstr").withDefault("fallback").as(String.class, "notneeded"));
    }

    @Test
    public void asPrimitiveIntReturnsProvidedTypedDefault() {
        assertEquals(22, resolve("nosuchint").as(int.class, 22).intValue());
    }

    @Test
    public void asPrimitiveIntReturnsProvidedRawDefault() {
        assertEquals(22, resolve("nosuchint").withDefault("22").as(int.class).get().intValue());
    }

    @Test
    public void asPrimitiveIntReturnsProvidedRawDefaultOverProvidedTypedDefault() {
        assertEquals(22, resolve("nosuchint").withDefault("22").as(int.class, 66).intValue());
    }

    @Test
    public void asLongReturnsProvidedTypedDefault() {
        assertEquals(44L, resolve("nosuchlong").as(Long.class, 44L).longValue());
    }

    @Test
    public void asLongReturnsProvidedRawDefault() {
        assertEquals(44L, resolve("nosuchlong").withDefault("44").as(Long.class).get().longValue());
    }

    @Test
    public void asLongReturnsProvidedRawDefaultOverProvidedTypedDefault() {
        assertEquals(44L, resolve("nosuchlong").withDefault("44").as(Long.class, 66L).longValue());
    }


    /*
     * Arrays with Effective Raw and Typed Defaults
     */

    @Test
    public void asStringArrayReturnsProvidedTypedDefault() {
        assertArrayEquals(new String[] { "fallback" }, resolve("nosuchstr").as(String[].class, new String[] { "fallback" }));
    }

    @Test
    public void asStringArrayReturnsProvidedRawDefault() {
        assertArrayEquals(new String[] { "fallback" }, resolve("nosuchstr").withDefault("fallback").as(String[].class).get());
    }

    @Test
    public void asStringArrayReturnsProvidedRawDefaultOverProvidedTypedDefault() {
        assertArrayEquals(new String[] { "fallback" }, resolve("nosuchstr").withDefault("fallback").as(String[].class, new String[] { "notneeded" }));
    }

    @Test
    public void asPrimitiveIntArrayReturnsProvidedTypedDefault() {
        assertArrayEquals(new int[] { 22 }, resolve("nosuchint").as(int[].class, new int[] { 22 }));
    }

    @Test
    public void asPrimitiveIntArrayReturnsProvidedRawDefault() {
        assertArrayEquals(new int[] { 22 }, resolve("nosuchint").withDefault("22").as(int[].class).get());
    }

    @Test
    public void asPrimitiveIntArrayReturnsProvidedRawDefaultOverProvidedTypedDefault() {
        assertArrayEquals(new int[] { 22 }, resolve("nosuchint").withDefault("22").as(int[].class, null));
    }

    @Test
    public void asLongArrayReturnsProvidedTypedDefault() {
        assertArrayEquals(new Long[] { 11L, 44L }, resolve("nosuchlong").as(Long[].class, new Long[] { 11L, 44L }));
    }

    @Test
    public void asLongArrayReturnsProvidedRawDefault() {
        assertArrayEquals(new Long[] { 11L, 44L }, resolve("nosuchlong").withDefault("11,44").as(Long[].class).get());
    }

    @Test
    public void asLongArrayReturnsProvidedRawDefaultOverProvidedTypedDefault() {
        assertArrayEquals(new Long[] { 11L, 44L }, resolve("nosuchlong").withDefault("11,44").as(Long[].class, null));
    }


    /*
     * Throwing exceptions on missing properties
     */

    @Test
    public void asStringThrowsMissingException() {
        assertThrowsMissingProperty("nosuchstr",
                () ->  resolve("nosuchstr").throwOnMissingProperty().as(String.class));
    }

    @Test
    public void asLongThrowsMissingException() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().as(Long.class));
    }

    @Test
    public void asBooleanThrowsMissingException() {
        assertThrowsMissingProperty("nosuchbool",
                () -> resolve("nosuchbool").throwOnMissingProperty().as(Boolean.class));
    }

    @Test
    public void asPrimitiveIntThrowsMissingException() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().as(int.class));
    }

    @Test
    public void asStringThrowsMissingExceptionWithTypedDefault() {
        assertThrowsMissingProperty("nosuchstr",
                () -> resolve("nosuchstr").throwOnMissingProperty().as(String.class, "fallback"));
    }

    @Test
    public void asStringThrowsMissingExceptioWithRawDefault() {
        assertThrowsMissingProperty("nosuchstr",
                () -> resolve("nosuchstr").throwOnMissingProperty().withDefault("fallback").as(String.class));
    }

    @Test
    public void asStringThrowsMissingExceptioWithRawAndTypedDefault() {
        assertThrowsMissingProperty("nosuchstr",
                () -> resolve("nosuchstr").throwOnMissingProperty().withDefault("fallback").as(String.class, "notneeded"));
    }

    @Test
    public void asPrimitiveIntThrowsMissingExceptioWithTypedDefault() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().as(int.class, 22));
    }

    @Test
    public void asPrimitiveIntThrowsMissingExceptioWithRawDefault() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().withDefault("22").as(int.class));
    }

    @Test
    public void asPrimitiveIntThrowsMissingExceptioWithRawAndTypedDefault() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().withDefault("22").as(int.class, 66));
    }

    @Test
    public void asLongThrowsMissingExceptioWithTypedDefault() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().as(Long.class, 44L));
    }

    @Test
    public void asLongThrowsMissingExceptioWithRawDefault() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().withDefault("44").as(Long.class));
    }

    @Test
    public void asLongThrowsMissingExceptioWithRawAndTypedDefault() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().withDefault("44").as(Long.class, 66L));
    }

    @Test
    public void asStringArrayThrowsMissingExceptioWithTypedDefault() {
        assertThrowsMissingProperty("nosuchstr",
                () ->  resolve("nosuchstr").throwOnMissingProperty().as(String[].class, new String[] { "fallback" }));
    }

    @Test
    public void asStringArrayThrowsMissingExceptioWithRawDefault() {
        assertThrowsMissingProperty("nosuchstr",
                () -> resolve("nosuchstr").throwOnMissingProperty().withDefault("fallback").as(String[].class));
    }

    @Test
    public void asStringArrayThrowsMissingExceptioWithRawAndTypedDefault() {
        assertThrowsMissingProperty("nosuchstr",
                () -> resolve("nosuchstr").throwOnMissingProperty().withDefault("fallback").as(String[].class, new String[] { "notneeded" }));
    }

    @Test
    public void asPrimitiveIntArrayThrowsMissingExceptioWithTypedDefault() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().as(int[].class, new int[] { 22 }));
    }

    @Test
    public void asPrimitiveIntArrayThrowsMissingExceptioWithRawDefault() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().withDefault("22").as(int[].class));
    }

    @Test
    public void asPrimitiveIntArrayThrowsMissingExceptioWithRawAndTypedDefault() {
        assertThrowsMissingProperty("nosuchint",
                () -> resolve("nosuchint").throwOnMissingProperty().withDefault("22").as(int[].class, null));
    }

    @Test
    public void asLongArrayThrowsMissingExceptioWithTypedDefault() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().as(Long[].class, new Long[] { 11L, 44L }));
    }

    @Test
    public void asLongArrayThrowsMissingExceptioWithRawDefault() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().withDefault("11,44").as(Long[].class));
    }

    @Test
    public void asLongArrayThrowsMissingExceptioWithRawDefaultAndTypedDefault() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().withDefault("11,44").as(Long[].class, null));
    }


    /*
     * Not throwing exceptions on failed conversion
     */

    @Test
    public void asLongReturnsProvidedTypedDefaultOnConversionFailure() {
        assertEquals(Long.valueOf(13L), resolve("brokenlong").as(Long.class, 13L));
    }

    @Test
    public void asPrimitiveIntReturnsProvidedTypedDefaultOnConversionFailure() {
        assertEquals(567, resolve("brokenint").as(int.class, 567).intValue());
    }

    @Test
    public void asLongArrayReturnsProvidedTypedDefaultOnConversionFailure() {
        assertArrayEquals(new Long[] { 13L, 14L }, resolve("string1").as(Long[].class, new Long[] { 13L, 14L }));
    }

    @Test
    public void asPrimitiveIntArrayReturnsTypedProvidedDefaultOnConversionFailure() {
        assertArrayEquals(new int[] { 1,2,3 }, resolve("string1").as(int[].class, new int[] { 1,2,3 }));
    }

    @Test
    public void asLongReturnsProvidedRawDefaultOnConversionFailure() {
        assertEquals(Long.valueOf(13L), resolve("brokenlong").withDefault("13").as(Long.class).get());
    }

    @Test
    public void asPrimitiveIntReturnsProvidedRawDefaultOnConversionFailure() {
        assertEquals(567, resolve("brokenint").withDefault("567").as(int.class).get().intValue());
    }

    @Test
    public void asLongArrayReturnsProvidedRawDefaultOnConversionFailure() {
        assertArrayEquals(new Long[] { 13L, 14L }, resolve("brokenlong").withDefault("13,14").as(Long[].class).get());
    }

    @Test
    public void asPrimitiveIntArrayReturnsRawProvidedDefaultOnConversionFailure() {
        assertArrayEquals(new int[] { 1,2,3 }, resolve("brokenint").withDefault("1,2,3").as(int[].class).get());
    }

    @Test
    public void asLongReturnsProvidedTypedDefaultOnConversionFailureOfRawDefault() {
        assertEquals(13L, resolve("brokenlong").withDefault("notalong").as(Long.class, 13L).longValue());
    }

    /*
     * Throwing exceptions on failed conversion
     */


    private static void assertThrowsMissingProperty(String propertyName, Runnable test) {
        assertException(NoSuchElementException.class, "Unable to find property with name " + propertyName, test);
    }
}
