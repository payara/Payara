/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.glassfish.admin.rest.utils.ResourceUtil.processJvmOptions;
import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ResourceUtil#processJvmOptions(Map, boolean)} input handling.
 * 
 * @author asroth (initial version)
 * @author Jan Bernitt (extracted version)
 */
public class ResourceUtilTest {

    private static void assertJvmOptions(String expected, String key, String value) {
        assertJvmOptions(expected, key, value, false); // first not removing versioning
        assertJvmOptions(expected, key, value, true);  // then removing it
        // now add an actual version prefix
        String version = "[1.8.0u191|1.8.0u500]";
        assertJvmOptions(version + expected, version + key, value, false); // first not removing versioning
        assertJvmOptions(expected, version + key, value, true);  // then removing it
    }

    private static void assertJvmOptions(String expected, String key, String value, boolean removeVersioning) {
        assertEquals(expected, processJvmOptions(Collections.singletonMap(key, value), removeVersioning).get("id"));
        // now with some other keys
        Map<String, String> data = new LinkedHashMap<>();
        data.put("before", "bvalue");
        data.put(key, value);
        assertEquals("before=bvalue:" + expected, processJvmOptions(data, removeVersioning).get("id"));
        data.put("after", "avalue");
        assertEquals("before=bvalue:" + expected + ":after=avalue", processJvmOptions(data, removeVersioning).get("id"));
        // now with target set
        data.put("target", "thetarget");
        Map<String, String> processed = processJvmOptions(data, removeVersioning);
        assertEquals("before=bvalue:" + expected + ":after=avalue", processed.get("id"));
        assertEquals("thetarget", processed.get("target"));
        // and with profiler set
        data.put("profiler", "theprofiler");
        processed = processJvmOptions(data, removeVersioning);
        assertEquals("before=bvalue:" + expected + ":after=avalue", processed.get("id"));
        assertEquals("theprofiler", processed.get("profiler"));
        // and with an empty key in the map
        data.put("", "empty and therefore ignored");
        data.put(null, "null and therefore ignored");
        assertEquals("before=bvalue:" + expected + ":after=avalue", processJvmOptions(data, removeVersioning).get("id"));
    }

    @Test
    public void product_name_withValue(){
        String expected = "-Dproduct.name=XXX";
        assertJvmOptions(expected, "-Dproduct.name=XXX", "");
        assertJvmOptions(expected, "-Dproduct.name", "XXX");
    }

    @Test
    public void product_name_withValueEscaped(){
        String expected = "-Dproduct.name=XXX";
        assertJvmOptions(expected, "-Dproduct.name\\=XXX", "");
    }

    @Test
    public void product_name_noValue(){
        String expected = "-Dproduct.name";
        assertJvmOptions(expected, "-Dproduct.name", "");
        assertJvmOptions(expected, "-Dproduct.name=", "");
        assertJvmOptions(expected, "-Dproduct.name", null);
        assertJvmOptions(expected, "-Dproduct.name=", null);
    }

    @Test
    public void product_name_noValueEscaped(){
        String expected = "-Dproduct.name";
        assertJvmOptions(expected, "-Dproduct.name\\=", "");
        assertJvmOptions(expected, "-Dproduct.name\\", "");
    }

    @Test
    public void client(){
        String expected = "-client";
        assertJvmOptions(expected, "-client", "");
        assertJvmOptions(expected, "-client=", "");
        assertJvmOptions(expected, "-client", null);
        assertJvmOptions(expected, "-client=", null);
    }

    @Test
    public void server(){
        String expected = "-server";
        assertJvmOptions(expected, "-server", "");
        assertJvmOptions(expected, "-server=", "");
        assertJvmOptions(expected, "-server", null);
        assertJvmOptions(expected, "-server=", null);
    }

    @Test
    public void newRatio_noValue(){
        String expected = "-XX:NewRatio";
        assertJvmOptions(expected, "-XX:NewRatio", null);
        assertJvmOptions(expected, "-XX:NewRatio=", null);
        assertJvmOptions(expected, "-XX:NewRatio", "");
        assertJvmOptions(expected, "-XX:NewRatio=", "");
    }

    @Test
    public void newRatio_withValue(){
        String expected = "-XX:NewRatio=2";
        assertJvmOptions(expected, "-XX:NewRatio", "2");
        assertJvmOptions(expected, "-XX:NewRatio=", "2");
        assertJvmOptions(expected, "-XX:NewRatio=2", "");
        assertJvmOptions(expected, "-XX:NewRatio=2", null);
    }

    @Test
    public void xmx512m() {
        String expected = "-Xmx512m";
        assertJvmOptions(expected, "-Xmx512m", "");
        assertJvmOptions(expected, "-Xmx512m=", "");
        assertJvmOptions(expected, "-Xmx512m", null);
        assertJvmOptions(expected, "-Xmx512m=", null);
    }

    @Test
    public void system_property_withValue() {
        String expected = "-Dsystem.property=X";
        assertJvmOptions(expected, "-Dsystem.property", "X");
        assertJvmOptions(expected, "-Dsystem.property=", "X");
        assertJvmOptions(expected, "-Dsystem.property=X", "");
        assertJvmOptions(expected, "-Dsystem.property=X", null);
    }

    @Test
    public void system_property_noValue() {
        String expected = "-Dsystem.property";
        assertJvmOptions(expected, "-Dsystem.property", "");
        assertJvmOptions(expected, "-Dsystem.property=", "");
        assertJvmOptions(expected, "-Dsystem.property", null);
        assertJvmOptions(expected, "-Dsystem.property=", null);
    }

    @Test
    public void addOpens_withValue() {
        String expected = "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED";
        assertJvmOptions(expected, "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED");
        assertJvmOptions(expected, "--add-opens=", "java.base/jdk.internal.loader=ALL-UNNAMED");
        assertJvmOptions(expected, "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED", "");
        assertJvmOptions(expected, "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED", null);
    }

    @Test
    public void xXUnlockDiagnosticVMOptions() {
        String expected = "-XX:+UnlockDiagnosticVMOptions";
        assertJvmOptions(expected, "-XX:+UnlockDiagnosticVMOptions", "");
        assertJvmOptions(expected, "-XX:+UnlockDiagnosticVMOptions=", "");
        assertJvmOptions(expected, "-XX:+UnlockDiagnosticVMOptions", null);
        assertJvmOptions(expected, "-XX:+UnlockDiagnosticVMOptions=", null);
    }

    @Test
    public void xXUnlockDiagnosticVMOptions_escaped() {
        String expected = "-XX\\:+UnlockDiagnosticVMOptions";
        assertJvmOptions(expected, "-XX\\:+UnlockDiagnosticVMOptions", "");
        assertJvmOptions(expected, "-XX\\:+UnlockDiagnosticVMOptions\\=", "");
        assertJvmOptions(expected, "-XX\\:+UnlockDiagnosticVMOptions", null);
        assertJvmOptions(expected, "-XX\\:+UnlockDiagnosticVMOptions\\=", null);
    }

    @Test
    public void xbootclasspath() {
        String expected = "-Xbootclasspath/a:${com.sun.aas.installRoot}/lib/grizzly-npn-api.jar";
        assertJvmOptions(expected, "-Xbootclasspath/a:${com.sun.aas.installRoot}/lib/grizzly-npn-api.jar", "");
        assertJvmOptions(expected, "-Xbootclasspath/a:${com.sun.aas.installRoot}/lib/grizzly-npn-api.jar=", "");
        assertJvmOptions(expected, "-Xbootclasspath/a:${com.sun.aas.installRoot}/lib/grizzly-npn-api.jar", null);
        assertJvmOptions(expected, "-Xbootclasspath/a:${com.sun.aas.installRoot}/lib/grizzly-npn-api.jar=", null);
    }
}
