/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
 * either the CDDL, the GPL Version 2 or, in the case of the CDDL, in
 * combination with the GPL Version 2. You may elect to receive this code under
 * either the CDDL, the GPL Version 2 or, in combination with the GPL Version 2.
 * If you are unable to obtain such a combination, then the research, development,
 * and/or production of the material is solely at your discretion and risk and
 * Payara Foundation shall not be liable for any damages which occur while making
 * use of the content.
 */
package fish.payara.samples.microprofile.config;

import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the {@code set-config-property}, {@code get-config-property}
 * and {@code delete-config-property} asadmin commands across all config sources that
 * support write operations (domain, config, server, application, cluster, jndi).
 *
 * <p>The commands work directly with the backing stores, so MP Config value caching
 * does not affect the observed results.</p>
 *
 * <p>Each test performs a full round-trip:
 * <ol>
 *   <li>set-config-property  → expect exit 0</li>
 *   <li>get-config-property  → expect exit 0 and output containing the stored value</li>
 *   <li>delete-config-property → expect exit 0</li>
 *   <li>get-config-property  → expect non-zero exit (property absent)</li>
 * </ol>
 * </p>
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible("Uses asadmin CLI which is not available on Payara Micro")
public class ConfigPropertyCommandsIT {

    private static final Logger LOG = Logger.getLogger(ConfigPropertyCommandsIT.class.getName());

    /** WAR name used both for the ShrinkWrap archive and as the --sourceName for the application source. */
    private static final String APP_NAME = "microprofile-config";

    /** Sentinel value written to every source so we can assert it is returned by get. */
    private static final String PROPERTY_VALUE = "payara-cli-test-value";

    // Unique property names per source to avoid cross-test pollution.
    // For JNDI the name is also the JNDI resource name; dots are valid in GlassFish JNDI names.
    private static final String DOMAIN_PROPERTY  = "fish.payara.test.cli.domain";
    private static final String CONFIG_PROPERTY  = "fish.payara.test.cli.config";
    private static final String SERVER_PROPERTY  = "fish.payara.test.cli.server";
    private static final String APP_PROPERTY     = "fish.payara.test.cli.application";
    private static final String CLUSTER_PROPERTY = "fish.payara.test.cli.cluster";
    private static final String JNDI_PROPERTY    = "fish.payara.test.cli.jndi";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                .addClass(TestApplication.class)
                .addClass(TestResource.class)
                .addAsManifestResource(
                        TestApplication.class.getResource("/META-INF/microprofile-config.properties"),
                        "microprofile-config.properties");
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void setup() {
        // Remove any stale properties from a previous interrupted test run.
        cleanupTestProperties();
    }

    @AfterClass
    public static void teardown() {
        // Best-effort removal of any test properties that may have been left behind
        // by a test that failed before reaching its delete step.
        cleanupTestProperties();
    }

    /**
     * Silently attempts to delete every test property from every source.
     * Exit codes are intentionally ignored: a non-zero exit simply means the
     * property was not present, which is fine.
     */
    private static void cleanupTestProperties() {
        CliCommands.payaraGlassFish("delete-config-property",
                "--source", "domain", "--propertyName", DOMAIN_PROPERTY);
        CliCommands.payaraGlassFish("delete-config-property",
                "--source", "config", "--sourceName", "server-config", "--propertyName", CONFIG_PROPERTY);
        CliCommands.payaraGlassFish("delete-config-property",
                "--source", "server", "--sourceName", "server", "--propertyName", SERVER_PROPERTY);
        CliCommands.payaraGlassFish("delete-config-property",
                "--source", "application", "--sourceName", APP_NAME, "--propertyName", APP_PROPERTY);
        CliCommands.payaraGlassFish("delete-config-property",
                "--source", "cluster", "--propertyName", CLUSTER_PROPERTY);
        CliCommands.payaraGlassFish("delete-config-property",
                "--source", "jndi", "--propertyName", JNDI_PROPERTY);
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * domain config source — properties are stored in domain.xml at the domain level,
     * no --sourceName required.
     */
    @Test
    public void testDomainSource() {
        assertRoundTrip("domain", DOMAIN_PROPERTY);
    }

    /**
     * config config source — properties are stored in the named GlassFish configuration
     * (e.g. server-config).  Requires --sourceName.
     */
    @Test
    public void testConfigSource() {
        assertRoundTrip("config", CONFIG_PROPERTY, "--sourceName", "server-config");
    }

    /**
     * server config source — properties are stored on a named server instance.
     * Requires --sourceName.
     */
    @Test
    public void testServerSource() {
        assertRoundTrip("server", SERVER_PROPERTY, "--sourceName", "server");
    }

    /**
     * application config source — properties are stored against the deployed application
     * in domain.xml.  Requires --sourceName matching the application name (WAR name
     * without the .war extension).
     */
    @Test
    public void testApplicationSource() {
        assertRoundTrip("application", APP_PROPERTY, "--sourceName", APP_NAME);
    }

    /**
     * cluster config source — properties are stored in the shared Hazelcast clustered
     * map ("payara.microprofile.config").  Hazelcast starts automatically with Payara Server,
     * so no extra setup is needed.
     */
    @Test
    public void testClusterSource() {
        assertRoundTrip("cluster", CLUSTER_PROPERTY);
    }

    /**
     * jndi config source — each property is backed by a GlassFish Custom Resource
     * whose JNDI name equals the MP Config property name.  The value is served by
     * {@code PrimitivesAndStringFactory} and looked up via {@code InitialContext.lookup()}.
     *
     * <p>Note: {@code set-config-property --source jndi} refuses to overwrite an
     * already-existing JNDI resource; the pre-test cleanup in {@link #setup()} removes
     * any stale resource left from a previous interrupted run.</p>
     */
    @Test
    public void testJndiSource() {
        assertRoundTrip("jndi", JNDI_PROPERTY);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Executes the full set → get (value present) → delete → get (value absent) cycle
     * for the given source and property name.
     *
     * @param source     the --source value (e.g. "domain", "cluster", …)
     * @param property   the --propertyName value
     * @param extraArgs  any additional arguments required by this source (e.g. --sourceName)
     */
    private static void assertRoundTrip(String source, String property, String... extraArgs) {
        LOG.info("Testing config source: " + source + ", property: " + property);

        // 1. set
        List<String> setCmd = buildCmd("set-config-property", source, property, extraArgs);
        setCmd.add("--propertyValue");
        setCmd.add(PROPERTY_VALUE);
        assertEquals("set-config-property should succeed [source=" + source + "]",
                0, CliCommands.payaraGlassFish(setCmd));

        // 2. get — value must be present in the command output
        List<String> output = new ArrayList<>();
        assertEquals("get-config-property should succeed after set [source=" + source + "]",
                0, CliCommands.payaraGlassFish(buildCmd("get-config-property", source, property, extraArgs), output));
        assertTrue("get-config-property output must contain '" + PROPERTY_VALUE + "' [source=" + source + "]",
                output.stream().anyMatch(line -> line.contains(PROPERTY_VALUE)));

        // 3. delete
        assertEquals("delete-config-property should succeed [source=" + source + "]",
                0, CliCommands.payaraGlassFish(buildCmd("delete-config-property", source, property, extraArgs)));

        // 4. get after delete — property must be absent, command must fail
        assertNotEquals("get-config-property must fail after deletion [source=" + source + "]",
                0, CliCommands.payaraGlassFish(buildCmd("get-config-property", source, property, extraArgs)));
    }

    /**
     * Builds a command list from the fixed arguments common to all three config property
     * commands plus any extra (source-specific) arguments.
     */
    private static List<String> buildCmd(String command, String source, String propertyName, String... extraArgs) {
        List<String> cmd = new ArrayList<>(asList(command,
                "--source", source,
                "--propertyName", propertyName));
        cmd.addAll(asList(extraArgs));
        return cmd;
    }
}
