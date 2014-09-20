/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.nucleus.admin.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class JvmOptionsTest extends RestTestBase {
    protected static final String URL_SERVER_JVM_OPTIONS = "/domain/configs/config/server-config/java-config/jvm-options";
    protected static final String URL_DEFAULT_JVM_OPTIONS = "/domain/configs/config/default-config/java-config/jvm-options";

    protected static final String URL_SERVER_CONFIG_CREATE_PROFILER = "/domain/configs/config/server-config/java-config/create-profiler";
    protected static final String URL_SERVER_CONFIG_DELETE_PROFILER = "/domain/configs/config/server-config/java-config/profiler/delete-profiler";
    protected static final String URL_SERVER_CONFIG_PROFILER_JVM_OPTIONS = "/domain/configs/config/server-config/java-config/profiler/jvm-options";

    protected static final String URL_DEFAULT_CONFIG_CREATE_PROFILER = "/domain/configs/config/default-config/java-config/create-profiler";
    protected static final String URL_DEFAULT_CONFIG_DELETE_PROFILER = "/domain/configs/config/default-config/java-config/profiler/delete-profiler";
    protected static final String URL_DEFAULT_CONFIG_PROFILER_JVM_OPTIONS = "/domain/configs/config/default-config/java-config/profiler/jvm-options";

    private ConfigTest configTest;
    private String testConfigName;
    private String URL_TEST_CONFIG;
    private String URL_TEST_CONFIG_JVM_OPTIONS;

    @BeforeMethod
    public void createConfig() {
        if (configTest == null) {
            configTest = getTestClass(ConfigTest.class);
        }

        testConfigName = "config-" + generateRandomString();
        MultivaluedMap formData = new MultivaluedHashMap() {{
            add("id", "default-config");
            add("id", testConfigName);
        }};
        configTest.createAndVerifyConfig(testConfigName, formData);

        URL_TEST_CONFIG = "/domain/configs/config/" + testConfigName;
        URL_TEST_CONFIG_JVM_OPTIONS = URL_TEST_CONFIG + "/java-config/jvm-options";
    }

    @AfterMethod
    public void deleteConfig() {
        configTest.deleteAndVerifyConfig(testConfigName);
    }


    @Test
    public void getJvmOptions() {
        Response response = get(URL_SERVER_JVM_OPTIONS);
        assertTrue(isSuccess(response));
        Map<String, Object> responseMap = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        List<String> jvmOptions = (List<String>)((Map)responseMap.get("extraProperties")).get("leafList");
        assertTrue(jvmOptions.size() > 0);
    }

    @Test
    public void createAndDeleteOptions() {
        final String option1Name = "-Doption" + generateRandomString();
        Map<String, String> newOptions = new HashMap<String, String>() {{
            put(option1Name, "someValue");
        }};

        Response response = post(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        List<String> jvmOptions = getJvmOptions(response);
        assertTrue(jvmOptions.contains(option1Name+"=someValue"));

        response = delete(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        jvmOptions = getJvmOptions(response);
        assertFalse(jvmOptions.contains(option1Name+"=someValue"));
    }

    // http://java.net/jira/browse/GLASSFISH-19069
    @Test
    public void createAndDeleteOptionsWithBackslashes() {
        final String optionName = "-Dfile" + generateRandomString();
        final String optionValue = "C:\\ABC\\DEF\\";
        Map<String, String> newOptions = new HashMap<String, String>() {{
            put(optionName, escape(optionValue));
        }};

        Response response = post(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        List<String> jvmOptions = getJvmOptions(response);
        assertTrue(jvmOptions.contains(optionName+"="+optionValue));

        response = delete(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        jvmOptions = getJvmOptions(response);
        assertFalse(jvmOptions.contains(optionName+"="+optionValue));
    }

    @Test
    public void createAndDeleteOptionsWithoutValues() {
        final String option1Name = "-Doption" + generateRandomString();
        final String option2Name = "-Doption" + generateRandomString();
        Map<String, String> newOptions = new HashMap<String, String>() {{
            put(option1Name, "");
            put(option2Name, "");
        }};

        Response response = post(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        List<String> jvmOptions = getJvmOptions(response);
        assertTrue(jvmOptions.contains(option1Name));
        assertFalse(jvmOptions.contains(option1Name+"="));
        assertTrue(jvmOptions.contains(option2Name));
        assertFalse(jvmOptions.contains(option2Name+"="));

        response = delete(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        jvmOptions = getJvmOptions(response);
        assertFalse(jvmOptions.contains(option1Name));
        assertFalse(jvmOptions.contains(option2Name));
    }

    @Test
    public void testIsolatedOptionsCreationOnNewConfig() {
        final String optionName = "-Doption" + generateRandomString();

        Map<String, String> newOptions = new HashMap<String, String>() {{
            put(optionName, "");
            put("target", testConfigName);
        }};

        // Test new config to make sure option is there
        Response response = post(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        List<String> jvmOptions = getJvmOptions(response);
        assertTrue(jvmOptions.contains(optionName));

        // Test server-config to make sure the options are NOT there
        response = get(URL_SERVER_JVM_OPTIONS);
        jvmOptions = getJvmOptions(response);
        assertFalse(jvmOptions.contains(optionName));
    }

    @Test
    public void testProfilerJvmOptions() {
        final String profilerName = "profiler" + generateRandomString();
        final String optionName = "-Doption" + generateRandomString();
        Map<String, String> attrs = new HashMap<String, String>() {{
            put("name", profilerName);
            put("target", testConfigName);
        }};
        Map<String, String> newOptions = new HashMap<String, String>() {{
//            put("target", testConfigName);
//            put("profiler", "true");
            put(optionName, "");
        }};

        deleteProfiler(URL_TEST_CONFIG + "/java-config/profiler/delete-profiler", testConfigName, false);

        Response response = post(URL_TEST_CONFIG + "/java-config/create-profiler", attrs);
        assertTrue(isSuccess(response));

        response = post(URL_TEST_CONFIG + "/java-config/profiler/jvm-options", newOptions);
        assertTrue(isSuccess(response));

        response = get(URL_TEST_CONFIG + "/java-config/profiler/jvm-options");
        List<String> jvmOptions = getJvmOptions(response);
        assertTrue(jvmOptions.contains(optionName));

        deleteProfiler(URL_TEST_CONFIG + "/java-config/profiler/delete-profiler", testConfigName, true);
    }

    @Test
    public void testJvmOptionWithColon() {
        final String optionName = "-XX:MaxPermSize";
        final String optionValue = "152m";
        Map<String, String> newOptions = new HashMap<String, String>() {{
            put(escape(optionName), optionValue);
        }};

        Response response = post(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
//        assertTrue(isSuccess(response));
        List<String> jvmOptions = getJvmOptions(response);
        assertTrue(jvmOptions.contains(optionName+"="+optionValue));

        response = delete(URL_TEST_CONFIG_JVM_OPTIONS, newOptions);
        assertTrue(isSuccess(response));
        response = get(URL_TEST_CONFIG_JVM_OPTIONS);
        jvmOptions = getJvmOptions(response);
        assertFalse(jvmOptions.contains(optionName+"="+optionValue));
    }

    protected void deleteProfiler(final String url, final String target, final boolean failOnError) {
        Response response = delete (url, new HashMap() {{ put ("target", target); }});
        if (failOnError) {
            assertTrue(isSuccess(response));
        }
    }

    protected List<String> getJvmOptions(Response response) {
        Map<String, Object> responseMap = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        List<String> jvmOptions = (List<String>)((Map)responseMap.get("extraProperties")).get("leafList");

        return jvmOptions;
    }
  
    protected String escape(String part) {
        String changed = part
                .replace("\\", "\\\\")
                .replace(":", "\\:");
        return changed;
    }
}
