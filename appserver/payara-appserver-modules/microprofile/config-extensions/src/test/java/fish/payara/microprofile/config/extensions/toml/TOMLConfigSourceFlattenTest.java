/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.extensions.toml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the TOML flattening logic in {@link TOMLConfigSource}.
 *
 * The {@code flattenToml} and {@code flattenTomlArray} methods are private, but the
 * module-info.java for fish.payara.microprofile.config.extensions declares
 * {@code opens fish.payara.microprofile.config.extensions.toml}, which permits
 * reflective access from the unnamed module (classpath) where tests run.
 *
 * These tests exercise the key flattening scenarios without requiring a live HK2
 * container or any server infrastructure.
 */
public class TOMLConfigSourceFlattenTest {

    private TOMLConfigSource source;
    private Method flattenToml;
    private Method flattenTomlArray;

    @Before
    public void setUp() throws Exception {
        source = new TOMLConfigSource();

        flattenToml = TOMLConfigSource.class.getDeclaredMethod(
                "flattenToml", Map.class, String.class, Map.class, int.class, int.class);
        flattenToml.setAccessible(true);

        flattenTomlArray = TOMLConfigSource.class.getDeclaredMethod(
                "flattenTomlArray", ArrayList.class, String.class, Map.class, int.class, int.class);
        flattenTomlArray.setAccessible(true);
    }

    @Test
    public void flatScalarValues_areKeyedDirectly() throws Exception {
        Map<String, Object> toml = new LinkedHashMap<>();
        toml.put("host", "localhost");
        toml.put("port", 5432);
        toml.put("enabled", true);

        Map<String, String> result = new LinkedHashMap<>();
        flattenToml.invoke(source, toml, "", result, 0, 10);

        assertEquals("localhost", result.get("host"));
        assertEquals("5432", result.get("port"));
        assertEquals("true", result.get("enabled"));
    }

    @Test
    public void nestedTable_isFlattened_withDotSeparator() throws Exception {
        Map<String, Object> database = new LinkedHashMap<>();
        database.put("host", "db.example.com");
        database.put("port", 5432);

        Map<String, Object> toml = new LinkedHashMap<>();
        toml.put("database", database);

        Map<String, String> result = new LinkedHashMap<>();
        flattenToml.invoke(source, toml, "", result, 0, 10);

        assertEquals("db.example.com", result.get("database.host"));
        assertEquals("5432", result.get("database.port"));
    }

    @Test
    public void deeplyNested_isFlattened_recursively() throws Exception {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("value", "deep");

        Map<String, Object> mid = new LinkedHashMap<>();
        mid.put("inner", inner);

        Map<String, Object> toml = new LinkedHashMap<>();
        toml.put("a", mid);

        Map<String, String> result = new LinkedHashMap<>();
        flattenToml.invoke(source, toml, "", result, 0, 10);

        assertEquals("deep", result.get("a.inner.value"));
    }

    @Test
    public void prefix_isPrependedToAllKeys() throws Exception {
        Map<String, Object> toml = new LinkedHashMap<>();
        toml.put("timeout", 30);

        Map<String, String> result = new LinkedHashMap<>();
        flattenToml.invoke(source, toml, "server", result, 0, 10);

        assertEquals("30", result.get("server.timeout"));
    }

    @Test
    public void arrayOfScalars_isIndexed() throws Exception {
        ArrayList<Object> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        Map<String, String> result = new LinkedHashMap<>();
        flattenTomlArray.invoke(source, list, "tags", result, 0, 10);

        assertEquals("a", result.get("tags[0]"));
        assertEquals("b", result.get("tags[1]"));
        assertEquals("c", result.get("tags[2]"));
    }

    @Test
    public void arrayOfTables_isFlattened_withIndexedPrefix() throws Exception {
        Map<String, Object> server1 = new LinkedHashMap<>();
        server1.put("host", "s1.example.com");
        Map<String, Object> server2 = new LinkedHashMap<>();
        server2.put("host", "s2.example.com");

        ArrayList<Object> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        Map<String, String> result = new LinkedHashMap<>();
        flattenTomlArray.invoke(source, servers, "servers", result, 0, 10);

        assertEquals("s1.example.com", result.get("servers[0].host"));
        assertEquals("s2.example.com", result.get("servers[1].host"));
    }

    @Test
    public void nullValue_isSkipped() throws Exception {
        Map<String, Object> toml = new LinkedHashMap<>();
        toml.put("present", "yes");
        toml.put("missing", null);

        Map<String, String> result = new LinkedHashMap<>();
        flattenToml.invoke(source, toml, "", result, 0, 10);

        assertTrue(result.containsKey("present"));
        assertFalse(result.containsKey("missing"));
    }

    @Test(expected = java.lang.reflect.InvocationTargetException.class)
    public void exceedingMaxDepth_throwsIllegalArgument() throws Exception {
        Map<String, Object> toml = new LinkedHashMap<>();
        toml.put("key", "value");

        Map<String, String> result = new LinkedHashMap<>();
        // depth=5 > maxDepth=4, should throw IllegalArgumentException wrapped in InvocationTargetException
        flattenToml.invoke(source, toml, "", result, 5, 4);
    }
}
