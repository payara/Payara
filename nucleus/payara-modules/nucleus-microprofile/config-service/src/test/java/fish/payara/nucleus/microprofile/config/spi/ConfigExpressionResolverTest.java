/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021-2023] Payara Foundation and/or its affiliates. All rights reserved.
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

import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

import static fish.payara.nucleus.microprofile.config.spi.ConfigTestUtils.createSource;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ConfigExpressionResolverTest {
    
    private final ConfigSource source = createSource("S1", 100, new HashMap<>());
    private final ConfigSource sourceHigherOrdinal = createSource("SHO", 110, new HashMap<>());
    private final ConfigSource sourceEqualOrdinal = createSource("SEO", 100, new HashMap<>());
    private final ConfigSource sourceLowerOrdinal = createSource("SEO", 90, new HashMap<>());
    private final ConfigExpressionResolver resolver = new ConfigExpressionResolver(Set.of(source, sourceHigherOrdinal, sourceEqualOrdinal, sourceLowerOrdinal), "test");

    @Before
    public void configureConfigProperties() {
        source.getProperties().put("key", "value");
        source.getProperties().put("key.escaped", "a,b,c\\,d");
        source.getProperties().put("reference.escaped", "\\${key}");
        source.getProperties().put("reference.escaped2", "${not.existing:1{\\}3}");
        source.getProperties().put("reference.single", "${key}");
        source.getProperties().put("reference.double", "${reference.single}");
        source.getProperties().put("reference.concat", "1${key}${key}2");
        source.getProperties().put("reference.recursive", "${reference.recursive}");
        source.getProperties().put("reference.not.found", "${not.existing}");
        source.getProperties().put("default.value", "${not.existing:result}");
        source.getProperties().put("default.value.empty", "1${not.existing:}2");
        source.getProperties().put("default.value.reference", "${not.existing:${key}}");
        source.getProperties().put("default.key.reference", "${${not.existing:key}:not.found}");
        source.getProperties().put("%test.fish.payara.badger", "mushroom");
        source.getProperties().put("fish.payara.badger", "badger");
        source.getProperties().put("%test.fish.payara.rod", "bites");
        
        sourceHigherOrdinal.getProperties().put("fish.payara.rod", "nobites");
        sourceHigherOrdinal.getProperties().put("%test.fish.payara.profile-only", "gotcha");
        
        sourceEqualOrdinal.getProperties().put("fish.payara.badger", "i-shall-be-ignored");
        sourceLowerOrdinal.getProperties().put("fish.payara.badger", "i-shall-be-ignored");
    }

    @Test
    public void testPlainValue() {
        ConfigValue result = resolver.resolve("key");
        assertEquals("value", result.getRawValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void testEscapedValue() {
        ConfigValue result = resolver.resolve("key.escaped");
        assertEquals("a,b,c\\,d", result.getRawValue());
        assertEquals("a,b,c\\,d", result.getValue());
    }

    @Test
    public void testEscapedReference() {
        ConfigValue result = resolver.resolve("reference.escaped");
        assertEquals("\\${key}", result.getRawValue());
        assertEquals("${key}", result.getValue());

        result = resolver.resolve("reference.escaped2");
        assertEquals("${not.existing:1{\\}3}", result.getRawValue());
        assertEquals("1{}3", result.getValue());
    }

    @Test
    public void testSimpleReference() {
        ConfigValue result = resolver.resolve("reference.single");
        assertEquals("${key}", result.getRawValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void testMultipleReferences() {
        ConfigValue result = resolver.resolve("reference.double");
        assertEquals("${reference.single}", result.getRawValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void testConcatenatedReferences() {
        ConfigValue result = resolver.resolve("reference.concat");
        assertEquals("1${key}${key}2", result.getRawValue());
        assertEquals("1valuevalue2", result.getValue());
    }

    @Test
    public void testNonNullReference() {
        ConfigValueImpl configValue = resolver.resolve("reference.not.found");
        assertNotNull(configValue);
    }

    @Test
    public void testDefaultValue() {
        ConfigValue result = resolver.resolve("not.existing", null);
        assertNull(result.getRawValue());
        assertNull(result.getValue());

        result = resolver.resolve("not.existing", "value");
        assertEquals("value", result.getRawValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void testDefaultRefValue() {
        ConfigValue result = resolver.resolve("default.value");
        assertEquals("${not.existing:result}", result.getRawValue());
        assertEquals("result", result.getValue());
    }

    @Test
    public void testEmptyDefaultValue() {
        ConfigValue result = resolver.resolve("default.value.empty");
        assertEquals("1${not.existing:}2", result.getRawValue());
        assertEquals("12", result.getValue());
    }

    @Test
    public void testDefaultValueReference() {
        ConfigValue result = resolver.resolve("default.value.reference");
        assertEquals("${not.existing:${key}}", result.getRawValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void testDefaultKeyReference() {
        ConfigValue result = resolver.resolve("default.key.reference");
        assertEquals("${${not.existing:key}:not.found}", result.getRawValue());
        assertEquals("value", result.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInfinitelyRecursiveReference() {
        resolver.resolve("reference.recursive");
    }

    @Test
    public void testExpressionExpansionDisabled() {
        final ConfigExpressionResolver disabledResolver = new ConfigExpressionResolver(singleton(source), false);
        assertEquals("${key}", disabledResolver.resolve("reference.single").getValue());
    }
    
    @Test
    public void testProfiles() {
        // This test case does not only test if the profiled value is used from the same source, but also if the
        // defined equal and lower ordinal sources are ignored
        ConfigValue result = resolver.resolve("fish.payara.badger");
        assertEquals("mushroom", result.getValue());
        assertEquals("mushroom", result.getRawValue());
    }
    
    @Test
    public void testProfilesOverrideProfiledValueFromSourceWithHigherOrdinal() {
        ConfigValue result = resolver.resolve("fish.payara.rod");
        assertEquals("nobites", result.getValue());
        assertEquals("nobites", result.getRawValue());
    }
    
    @Test
    public void testProfilesDoNotFailToLookupWhenOnlyProfiledValueExists() {
        ConfigValue result = resolver.resolve("fish.payara.profile-only");
        assertEquals("gotcha", result.getValue());
        assertEquals("gotcha", result.getRawValue());
    }
    
}
