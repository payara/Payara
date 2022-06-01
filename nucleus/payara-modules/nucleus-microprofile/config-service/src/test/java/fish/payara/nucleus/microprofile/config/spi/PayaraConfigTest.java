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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link PayaraConfig} with focus on the caching aspect.
 *
 * @author Jan Bernitt
 */
public class PayaraConfigTest {

    private static final long CACHE_TTL = 50L;

    private final ConfigSource source1 = createSource("S1", 100, new HashMap<>());
    private final ConfigSource source2 = createSource("S2", 200, new HashMap<>());
    private final Config config = new PayaraConfig(asList(source1, source2), emptyMap(), CACHE_TTL);

    @Before
    public void setUp() {
        source1.getProperties().put("key1", "value1");
        source2.getProperties().put("key2", "value2");
        source1.getProperties().put("int1", "1");
        source2.getProperties().put("int2", "2");
        source2.getProperties().put("bool2", "true,false,true");
        source2.getProperties().put("brokenarr", "1,a,2");
    }

    @Test
    public void optionalBooleanArrayConversion() {
        assertArrayEquals(new Boolean[] { true, false, true }, config.getOptionalValue("bool2", Boolean[].class).get());
    }

    @Test
    public void booleanArrayConversion() {
        assertArrayEquals(new boolean[] { true, false, true }, config.getValue("bool2", boolean[].class));
    }

    @Test
    public void configValueTest() {
        ConfigValue value = config.getValue("key1", ConfigValue.class);
        assertEquals("key1", value.getName());
        assertEquals("value1", value.getRawValue());
        assertEquals("value1", value.getValue());
        assertEquals("S1", value.getSourceName());
        assertEquals(100, value.getSourceOrdinal());
    }

    /**
     * Introduced by QACI-625. Not reproducible on all machines, but this aims to catch
     * issues caused by high concurrency in the cache map
     */
    @Test
    public void concurrentConfigValueTest() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(1000);
        AtomicReference<Throwable> failure = new AtomicReference<>(null);
        for (int i = 0; i < 100000; i++) {
            exec.submit(() -> {
                try {
                    config.getConfigValue("mp.config.profile").getValue();
                } catch (Throwable t) {
                    failure.set(t);
                    throw (RuntimeException) t;
                }
            });
        }
        exec.shutdown();
        exec.awaitTermination(60, TimeUnit.SECONDS);
        assertNull("stress test failed", failure.get());
    }

    @Test
    public void stringValuesAreCached() throws InterruptedException {
        assertCachedValue(source1, "key1", String.class, "value1", "changed1");
        assertCachedValue(source2, "key2", String.class, "value2", "changed2");
    }

    @Test
    public void convertedValuesAreCached() throws InterruptedException {
        assertCachedValue(source1, "int1", Integer.class, 1, 3);
        assertCachedValue(source2, "int2", Integer.class, 2, 4);
    }

    @Test
    public void primitiveValuesAreCached() throws InterruptedException {
        assertCachedValue(source1, "int1", int.class, 1, 3);
        assertCachedValue(source2, "int2", int.class, 2, 4);
    }

    @Test
    public void enumValuesAreCached() throws InterruptedException {
        // tests automatic method converter
        source1.getProperties().put("enum", ElementType.FIELD.toString());
        assertCachedValue(source1, "enum", ElementType.class, ElementType.FIELD, ElementType.METHOD);
    }

    @Test
    public void automaticConversionValuesAreCached() throws InterruptedException {
        // tests automatic constructor converter
        source1.getProperties().put("bigInt", "42");
        assertCachedValue(source1, "bigInt", BigInteger.class, BigInteger.valueOf(42L), BigInteger.valueOf(13L));
    }

    @Test
    public void eachPropertyTypeIsCached() {
        assertEquals(1, config.getValue("int1", Integer.class).intValue());
        assertEquals("1", config.getValue("int1", String.class));
    }

    @Test
    public void cacheCanBeBypassedUsingTTLZero() {
        Config config = new PayaraConfig(asList(source1, source2), emptyMap(), 0L);
        assertEquals("value1", config.getValue("key1", String.class));
        source1.getProperties().put("key1", "changed1");
        assertEquals("TTL <= 0 is still changed", "changed1", config.getValue("key1", String.class));
    }

    @Test
    public void undefinedPropertyThrowsException() {
        assertException(NoSuchElementException.class, "Unable to find property with name undefined",
                () -> config.getValue("undefined", String.class));
    }

    @Test
    public void unknownConversionReturnsThrowsException() {
        assertException(IllegalArgumentException.class, "Unable to convert value to type java.lang.CharSequence",
                () -> config.getValue("key1", CharSequence.class));
    }

    @Test
    public void getPropertyNamesContainsPeopertiesOfAllSources() {
        HashSet<String> actual = new HashSet<>();
        config.getPropertyNames().forEach(e -> actual.add(e));
        assertEquals(new HashSet<>(asList("key1", "key2", "bool2", "int1", "int2", "brokenarr")), actual);
    }

    @Test
    public void getSourcesContainsAllSources() {
        HashSet<ConfigSource> actual = new HashSet<>();
        config.getConfigSources().forEach(s -> actual.add(s));
        assertEquals(new HashSet<>(asList(source1, source2)), actual);
    }

    @Test
    public void listValueAllowSingleElement() {
        assertEquals(asList("value1"),
                config.getValue("key1", ConfigValueResolver.class).withDefault("default").asList(String.class));
        assertEquals(asList(1),
                config.getValue("int1", ConfigValueResolver.class).withDefault("42").asList(Integer.class));
    }

    @Test
    public void listValueAllowMultipleElements() {
        source1.getProperties().put("key1", "value1,value2");
        source1.getProperties().put("int1", "1,2");
        assertEquals(asList("value1", "value2"),
                config.getValue("key1", ConfigValueResolver.class).withDefault("default").asList(String.class));
        assertEquals(asList(1, 2),
                config.getValue("int1", ConfigValueResolver.class).withDefault("42").asList(Integer.class));
    }

    @Test
    public void setValueAllowSingleElement() {
        assertEquals(new HashSet<>(asList("value1")),
                config.getValue("key1", ConfigValueResolver.class).withDefault("default").asSet(String.class));
        assertEquals(new HashSet<>(asList(1)),
                config.getValue("int1", ConfigValueResolver.class).withDefault("42").asSet(Integer.class));
    }

    @Test
    public void setValueAllowMultipleElements() {
        source1.getProperties().put("key1", "value1,value2");
        source1.getProperties().put("int1", "1,2");
        assertEquals(new HashSet<>(asList("value1", "value2")),
                config.getValue("key1", ConfigValueResolver.class).withDefault("default").asSet(String.class));
        assertEquals(new HashSet<>(asList(1, 2)),
                config.getValue("int1", ConfigValueResolver.class).withDefault("42").asSet(Integer.class));
    }

    @Test
    public void undefinedSetPropertyThrowsExcetion() {
        assertException(NoSuchElementException.class, "Unable to find property with name undefined-set",
                () -> config.getValue("undefined-set", ConfigValueResolver.class).throwOnMissingProperty().asSet(String.class));
    }

    @Test
    public void undefinedListPropertyThrowsExcetion() {
        assertException(NoSuchElementException.class, "Unable to find property with name undefined-list",
                () -> config.getValue("undefined-list", ConfigValueResolver.class).throwOnMissingProperty().asList(String.class));
    }

    @Test
    public void undefinedArrayPropertyThrowsExcetion() {
        assertException(NoSuchElementException.class, "Unable to find property with name undefined-array",
                () -> config.getValue("undefined-array", String[].class));
    }

    @Test
    public void undefinedPropertyReturnsEmptyOptional() {
        assertEquals(Optional.empty(), config.getOptionalValue("nonExisting", String.class));
    }

    @Test
    public void illegalArrayElementFailsOverallArrayConversion() {
        assertException(IllegalArgumentException.class, "Unable to convert value to type java.lang.Integer for value `a`",
                () -> config.getValue("brokenarr", Integer[].class));
    }

    @Test
    public void ttlParameterIsRespected() {
        final long ttl = 60 * 1000L;
        assertEquals(ttl, new PayaraConfig(emptyList(), emptyMap(), ttl).getCacheDurationSeconds());
    }

    private <T> void assertCachedValue(ConfigSource source, String key, Class<T> propertyType, T expectedValue1,
            T expectedValue2) throws InterruptedException {
        long cacheExpiresAt = System.currentTimeMillis() + CACHE_TTL;
        assertValue("Value not as expected before update", key, propertyType, expectedValue1);
        source.getProperties().put(key, expectedValue2.toString());
        for (int i = 0; i < 10; i++) {
            try {
                assertValue("Run " + i + ", value is not cached", key, propertyType, expectedValue1);
            } catch (AssertionError e) {
                if (System.currentTimeMillis() >= cacheExpiresAt) {
                    // we didn't manage to finish the test within timeout;
                    break;
                } else {
                    throw e;
                }
            }
        }
        Thread.sleep(CACHE_TTL);
        assertValue("Cached value still used after TTL", key, propertyType, expectedValue2);
    }

    private <T> void assertValue(String msg, String key, Class<T> propertyType, T expected) {
        assertEquals(msg, expected, config.getValue(key, propertyType));
        assertEquals(msg, expected,
                config.getValue(key, ConfigValueResolver.class).withDefault("default").as(propertyType).get());
        assertEquals(msg, expected, config.getOptionalValue(key, propertyType).get());
        // as list
        assertEquals(msg, asList(expected),
                config.getValue(key, ConfigValueResolver.class).withDefault("default").asList(propertyType));
        // as set
        assertEquals(msg, new HashSet<>(asList(expected)),
                config.getValue(key, ConfigValueResolver.class).withDefault("default").asSet(propertyType));

        if (propertyType.isPrimitive()) {
            return;
        }
        // also test as array
        Object expectedArray = Array.newInstance(propertyType, 1);
        Array.set(expectedArray, 0, expected);
        Class<?> arrayType = expectedArray.getClass();
        assertArrayEquals((Object[]) expectedArray, (Object[]) config.getValue(key, arrayType));
    }

}
