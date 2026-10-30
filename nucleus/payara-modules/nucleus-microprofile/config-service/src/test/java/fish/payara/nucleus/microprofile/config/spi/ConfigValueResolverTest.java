/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.nucleus.microprofile.config.spi;

import static fish.payara.nucleus.microprofile.config.spi.ConfigTestUtils.assertException;
import static fish.payara.nucleus.microprofile.config.spi.ConfigTestUtils.createSource;
import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolver.ElementPolicy.FAIL;
import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolver.ElementPolicy.NULL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
                {"string2", " str "},
                {"long1", "42"},
                {"long2", " 42 "},
                {"bool1", "true" },
                {"int1", "13"},
                {"brokenlong", "fourtytwo"},
                {"brokenint", "one" },
                {"array1", "12,one,13" },
                {"array2", "a, b, c" },

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

    @Test
    public void asStringWithUnconfiguredRawDefault() {
        assertEquals("winner",
                resolve("nosuchstr").withDefault(ConfigProperty.UNCONFIGURED_VALUE).as(String.class, "winner"));
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

    @Test
    public void asPredicateReturnsProvidedTypedDefaultWithMissingConverter() {
        Predicate<Long> p = val -> true;
        assertEquals(p, resolve("long1").as(Predicate.class, p));
    }

    /*
     * Throwing exceptions on failed conversion
     */

    @Test
    public void asLongThrowsConversionFailure() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `fourtytwo`",
                () -> resolve("brokenlong").throwOnFailedConversion().as(Long.class));
    }

    @Test
    public void asPrimitiveIntThrowsConversionFailure() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Integer for value `one`",
                () -> resolve("brokenint").throwOnFailedConversion().as(int.class));
    }

    @Test
    public void asLongArrayThrowsConversionFailure() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `fourtytwo`",
                () -> resolve("brokenlong").throwOnFailedConversion().as(Long[].class));
    }

    @Test
    public void asPrimitiveIntArrayThrowsConversionFailure() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Integer for value `one`",
                () -> resolve("brokenint").throwOnFailedConversion().as(int[].class));
    }

    @Test
    public void asLongThrowsConversionFailureWithTypedDefault() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `fourtytwo`",
                () -> resolve("brokenlong").throwOnFailedConversion().as(Long.class, 13L));
    }

    @Test
    public void asPrimitiveIntThrowsConversionFailureWithTypedDefault() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Integer for value `one`",
                () -> resolve("brokenint").throwOnFailedConversion().as(int.class, 567));
    }

    @Test
    public void asLongArrayThrowsConversionFailureWithTypedDefault() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `str`",
                () -> resolve("string1").throwOnFailedConversion().as(Long[].class, new Long[] { 13L, 14L }));
    }

    @Test
    public void asPrimitiveIntArrayThrowsConversionFailureWithTypedDefault() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Integer for value `str`",
                () -> resolve("string1").throwOnFailedConversion().as(int[].class, new int[] { 1,2,3 }));
    }

    @Test
    public void asLongThrowsConversionFailureWithTypedDefaultAndRawDefault() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `notalong`",
                () -> resolve("brokenlong").throwOnFailedConversion().withDefault("notalong").as(Long.class, 13L));
    }

    @Test
    public void asFunctionThrowsConversionFailureDueToMissingConverter() {
        assertThrowsFailedConversion("Unable to find converter for property long1 with value 42.",
                () -> resolve("long1").throwOnFailedConversion().as(Function.class));
    }


    /*
     * Ad-hoc Conversion
     */

    @Test
    public void asConvertedBy() {
        assertEquals(42L, resolve("long1").asConvertedBy(Long::valueOf, -1L).longValue());
    }

    @Test
    public void asConvertedByWithTypedDefault() {
        assertEquals(42L, resolve("long1").asConvertedBy(Long::valueOf, 12L).longValue());
        assertEquals(12L, resolve("nosuchlong").asConvertedBy(Long::valueOf, 12L).longValue());
        assertEquals(14L, resolve("brokenlong").asConvertedBy(Long::valueOf, 14L).longValue());
    }

    @Test
    public void asConvertedByWithRawDefault() {
        assertEquals(42L, resolve("long1").withDefault("12").asConvertedBy(Long::valueOf, -1L).longValue());
        assertEquals(12L, resolve("nosuchlong").withDefault("12").asConvertedBy(Long::valueOf, -1L).longValue());
        assertEquals(14L, resolve("brokenlong").withDefault("14").asConvertedBy(Long::valueOf, -1L).longValue());
    }

    @Test
    public void asConvertedByWithRawAndTypedDefault() {
        assertEquals(42L, resolve("long1").withDefault("12").asConvertedBy(Long::valueOf, -1L).longValue());
        assertEquals(12L, resolve("nosuchlong").withDefault("12").asConvertedBy(Long::valueOf, -1L).longValue());
        assertEquals(15L, resolve("brokenlong").withDefault("notalong").asConvertedBy(Long::valueOf, 15L).longValue());
    }

    @Test
    public void asConvertedByWithTypedDefaultThrowsFailedConversion() {
        assertThrowsFailedConversion("java.lang.NumberFormatException: For input string: \"fourtytwo\"",
                () -> resolve("brokenlong").throwOnFailedConversion().asConvertedBy(Long::valueOf, -1L));
    }

    @Test
    public void asConvertedByWithRawDefaultThrowsFailedConversion() {
        assertThrowsFailedConversion("java.lang.NumberFormatException: For input string: \"notalong\"",
                () -> resolve("nosuchlong").throwOnFailedConversion().withDefault("notalong").asConvertedBy(Long::valueOf, -1L));
        assertThrowsFailedConversion("java.lang.NumberFormatException: For input string: \"fourtytwo\"",
                () -> resolve("brokenlong").throwOnFailedConversion().withDefault("notalong").asConvertedBy(Long::valueOf, -1L));
    }

    @Test
    public void asConvertedByWithTypedDefaultThrowsMissingProperty() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().asConvertedBy(Long::valueOf, -1L));
    }

    @Test
    public void asConvertedByWithRawDefaultThrowsMissingProperty() {
        assertThrowsMissingProperty("nosuchlong",
                () -> resolve("nosuchlong").throwOnMissingProperty().withDefault("notalong").asConvertedBy(Long::valueOf, -1L));
    }


    /*
     * Lists
     */

    @Test
    public void asListOfString() {
        assertEquals(asList("str"), resolve("string1").asList(String.class));
        assertEquals(asList("str"), resolve("string1").withDefault("ignored").asList(String.class));
    }

    @Test
    public void asListOfLong() {
        assertEquals(asList(42L), resolve("long1").asList(Long.class));
        assertEquals(asList(42L), resolve("long1").withDefault("ignored").asList(Long.class));
    }

    @Test
    public void asListOfPrimitiveInt() {
        assertEquals(asList(13), resolve("int1").asList(int.class));
        assertEquals(asList(13), resolve("int1").withDefault("ignored").asList(int.class));
    }

    @Test
    public void asListOfPrimitiveBool() {
        assertEquals(asList(true), resolve("bool1").asList(boolean.class));
        assertEquals(asList(true), resolve("bool1").withDefault("ignored").asList(boolean.class));
    }

    @Test
    public void asListOfStringWithRawDefault() {
        assertEquals(asList("str1", "str2", "str3", "str4"),
                resolve("nosuchlist").withDefault("str1,str2,str3,str4").asList(String.class));
    }

    @Test
    public void asListOfLongWithRawDefault() {
        assertEquals(asList(42L, 45L, 67L), resolve("nosuchlist").withDefault("42,45,67").asList(Long.class));
    }

    @Test
    public void asListOfPrimitiveIntWithRawDefault() {
        assertEquals(asList(13,14), resolve("nosuchlist").withDefault("13,14").asList(int.class));
    }

    @Test
    public void asListDefaultIsEmptyForMissingProperties() {
        assertEquals(emptyList(), resolve("nosuchlist").asList(String.class));
    }

    @Test
    public void asListWithDefaultForMissingProperties() {
        assertEquals(asList("default"), resolve("nosuchlist").asList(String.class, asList("default")));
    }

    @Test
    public void asListWithEmptyDefaultThrowsOnMissingProperty() {
        assertThrowsMissingProperty("nosuchlist",
                () -> resolve("nosuchlist").throwOnMissingProperty().asList(String.class));
    }

    @Test
    public void asListWithDefaultThrowsOnMissingProperty() {
        assertThrowsMissingProperty("nosuchlist",
                () -> resolve("nosuchlist").throwOnMissingProperty().asList(String.class, asList("default")));
    }

    @Test
    public void asListOfLongWithEmptyDefaultThrowsOnFailedConversion() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `str`",
                () -> resolve("string1").throwOnFailedConversion().asList(Long.class));
    }

    @Test
    public void asListOfLongWithDefaultThrowsOnFailedConversion() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `fourtytwo`",
                () -> resolve("brokenlong").throwOnFailedConversion().asList(Long.class, asList(42L)));
    }


    /*
     * Sets
     */

    @Test
    public void asSetOfString() {
        assertEquals(new HashSet<>(asList("str")), resolve("string1").asSet(String.class));
        assertEquals(new HashSet<>(asList("str")), resolve("string1").withDefault("ignored").asSet(String.class));
    }

    @Test
    public void asSetOfLong() {
        assertEquals(new HashSet<>(asList(42L)), resolve("long1").asSet(Long.class));
        assertEquals(new HashSet<>(asList(42L)), resolve("long1").withDefault("ignored").asSet(Long.class));
    }

    @Test
    public void asSetOfPrimitiveInt() {
        assertEquals(new HashSet<>(asList(13)), resolve("int1").asSet(int.class));
        assertEquals(new HashSet<>(asList(13)), resolve("int1").withDefault("ignored").asSet(int.class));
    }

    @Test
    public void asSetOfPrimitiveBool() {
        assertEquals(new HashSet<>(asList(true)), resolve("bool1").asSet(boolean.class));
        assertEquals(new HashSet<>(asList(true)), resolve("bool1").withDefault("ignored").asSet(boolean.class));
    }

    @Test
    public void asSetOfStringWithRawDefault() {
        assertEquals(new HashSet<>(asList("str1", "str2", "str3", "str4")),
                resolve("nosuchlist").withDefault("str1,str2,str3,str4").asSet(String.class));
    }

    @Test
    public void asSetOfLongWithRawDefault() {
        assertEquals(new HashSet<>(asList(42L, 45L, 67L)),
                resolve("nosuchlist").withDefault("42,45,67").asSet(Long.class));
    }

    @Test
    public void asSetOfPrimitiveIntWithRawDefault() {
        assertEquals(new HashSet<>(asList(13,14)), resolve("nosuchlist").withDefault("13,14").asSet(int.class));
    }

    @Test
    public void asSetDefaultIsEmptyForMissingProperties() {
        assertEquals(emptySet(), resolve("nosuchlist").asSet(String.class));
    }

    @Test
    public void asSetWithDefaultForMissingProperties() {
        assertEquals(new HashSet<>(asList("default")),
                resolve("nosuchlist").asSet(String.class, new HashSet<>(asList("default"))));
    }

    @Test
    public void asSetWithEmptyDefaultThrowsOnMissingProperty() {
        assertThrowsMissingProperty("nosuchlist",
                () -> resolve("nosuchlist").throwOnMissingProperty().asSet(String.class));
    }

    @Test
    public void asSetWithDefaultThrowsOnMissingProperty() {
        assertThrowsMissingProperty("nosuchlist",
                () -> resolve("nosuchlist").throwOnMissingProperty().asSet(String.class, new HashSet<>(asList("default"))));
    }

    @Test
    public void asSetOfLongWithEmptyDefaultThrowsOnFailedConversion() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `str`",
                () -> resolve("string1").throwOnFailedConversion().asSet(Long.class));
    }

    @Test
    public void asSetOfLongWithDefaultThrowsOnFailedConversion() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Long for value `fourtytwo`",
                () -> resolve("brokenlong").throwOnFailedConversion().asSet(Long.class, new HashSet<>(asList(42L))));
    }


    /*
     * Array Special Cases
     */

    @Test
    public void asArraySkipsElementsWithConversionFailure() {
        assertArrayEquals(new int[] { 12,  13 }, resolve("array1").as(int[].class, null));
        // is equivalent to (as long as only some elements fail):
        assertArrayEquals(new int[] { 12,  13 }, resolve("array1").throwOnFailedConversion().as(int[].class, null));
    }

    @Test
    public void asArrayThrowsConversionFailureWhenNoElementCanBeConverted() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Integer for value `c`",
                () -> resolve("nosucharray").throwOnFailedConversion().withDefault("a,b,c").as(int[].class, null));
    }

    @Test
    public void asArrayFailsWithConversionFailureAndFailElementPolicy() {
        assertThrowsFailedConversion("Unable to convert value to type java.lang.Integer for value `one`",
                () -> resolve("array1").withPolicy(FAIL).throwOnFailedConversion().as(int[].class, null));
    }

    @Test
    public void asArrayUsesNullWithConversionFailureAndNullElementPolicy() {
        assertArrayEquals(new Integer[] { 12, null, 13 },
                resolve("array1").withPolicy(NULL).throwOnFailedConversion().as(Integer[].class, null));
    }

    @Test
    public void asArrayFailsWithConversionFailureAndNullElementPolicyForPrimitiveArrays() {
        assertArrayEquals(new int[] { 1 },
                resolve("array1").withPolicy(NULL).as(int[].class, new int[] { 1 }));
    }

    @Test
    public void asListSkipsElementsWithConversionFailure() {
        assertEquals(asList(12,  13), resolve("array1").asList(Integer.class));
    }

    @Test
    public void asListTrimsElements() {
        assertEquals(asList("a", "b", "c"), resolve("array2").asList(String.class));
    }

    @Test
    public void asListDoesNotTrimElementsWhenDisabled() {
        assertEquals(asList("a", " b", " c"), resolve("array2").withTrimming(false).asList(String.class));
    }

    /*
     * Trimming
     */

    @Test
    public void asStringByDefaultAppliesTrimming() {
        assertEquals("str", resolve("string2").as(String.class, null));
    }

    @Test
    public void asStringOptionalByDefaultAppliesTrimming() {
        assertEquals("str", resolve("string2").as(String.class).get());
    }

    @Test
    public void asLongByDefaultAppliesTrimming() {
        assertEquals(42L, resolve("long2").as(Long.class, null).longValue());
    }

    @Test
    public void asLongOptionalByDefaultAppliesTrimming() {
        assertEquals(42L, resolve("long2").as(Long.class).get().longValue());
    }

    @Test
    public void asLongPrimitiveByDefaultAppliesTrimming() {
        assertEquals(42L, resolve("long2").as(long.class, null).longValue());
    }

    @Test
    public void asLongPrimitiveOptionalByDefaultAppliesTrimming() {
        assertEquals(42L, resolve("long2").as(long.class).get().longValue());
    }

    @Test
    public void asStringDoesNoTrimmingWhenDisabled() {
        assertEquals(" str ", resolve("string2").withTrimming(false).as(String.class, null));
    }

    /*
     * Custom TTLs
     */

    @Test
    public void customTtlUsesSeparateCacheEntry() {
        Map<String, String> properties = source1.getProperties();
        properties.put("newkey", "42");
        assertEquals(42, resolve("newkey").withTTL(1000).as(int.class, -1).intValue());
        // update the value in source
        properties.put("newkey", "142");
        // resolve with different TTL
        assertEquals(142, resolve("newkey").withTTL(50).as(int.class, -1).intValue());
        // while when resolved with TTL 1000 (1 sec) it still is the old value (as it did not take more then a sec to get here)
        assertEquals(42, resolve("newkey").withTTL(1000).as(int.class, -1).intValue());
    }

    private static void assertThrowsFailedConversion(String msg, Runnable test) {
        assertException(IllegalArgumentException.class, msg, test);
    }

    private static void assertThrowsMissingProperty(String propertyName, Runnable test) {
        assertException(NoSuchElementException.class, "Unable to find property with name " + propertyName, test);
    }
}
