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
package fish.payara.samples.otel;

import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the OpenTelemetry-related asadmin monitoring commands:
 * {@code set-monitoring-service-configuration --otelenabled} and
 * {@code enable-monitoring --otel}.
 *
 * <p>Each test verifies:
 * <ol>
 *   <li>The set command succeeds (exit code 0).</li>
 *   <li>The value is reflected by {@code get-monitoring-service-configuration}.</li>
 *   <li>A server restart is required after toggling the flag
 *       ({@code _get-restart-required} returns {@code true}).</li>
 * </ol>
 * </p>
 *
 * <p>NOTE: toggling {@code otelEnabled} always requires a server restart for
 * the change to take effect at runtime (the OpenTelemetry SDK is only
 * initialised once at startup). The tests verify that the restart-required
 * flag is set; they do <em>not</em> restart the domain to avoid disrupting
 * the rest of the CI test suite.</p>
 */
@RunWith(Arquillian.class)
@NotMicroCompatible("Uses asadmin CLI which is not available on Payara Micro")
public class OpenTelemetryMonitoringCommandsIT {

    @Deployment
    public static WebArchive dummyDeployment() {
        // we need to deploy something so that we'd run the tests remotely
        return ShrinkWrap.create(WebArchive.class)
                .addClass(CliCommands.class);
    }

    @AfterClass
    public static void restoreDefaults() {
        // Restore otelEnabled=false (the default) so other tests are not affected.
        // NOTE: toggling otelEnabled always requires a server restart to take effect;
        // this cleanup sets the config value only and does NOT restart the domain.
        CliCommands.payaraGlassFish("set-monitoring-service-configuration", "--otelenabled", "false");
    }

    @Test
    public void setOtelEnabledViaSetMonitoringServiceConfiguration() {
        // SET
        assertEquals(0, CliCommands.payaraGlassFish(
                "set-monitoring-service-configuration", "--otelenabled", "true"));

        // GET — verify value is reflected
        List<String> output = new ArrayList<>();
        assertEquals(0, CliCommands.payaraGlassFish(
                asList("get-monitoring-service-configuration"), output));
        assertTrue("get-monitoring-service-configuration output should contain 'true'",
                output.stream().anyMatch(line -> line.contains("true")));

        // Restart is required because toggling otelEnabled re-initialises the OTel SDK
        List<String> restartOutput = new ArrayList<>();
        CliCommands.payaraGlassFish(asList("_get-restart-required"), restartOutput);
        assertTrue("Restart must be required after toggling otelEnabled via set-monitoring-service-configuration",
                restartOutput.stream().anyMatch(line -> Boolean.parseBoolean(line.trim())));
    }

    @Test
    public void setOtelEnabledViaEnableMonitoring() {
        // SET — enable-monitoring also sets monitoring-enabled=true as a side-effect,
        // which is expected in the test environment (monitoring is already enabled).
        assertEquals(0, CliCommands.payaraGlassFish("enable-monitoring", "--otel", "true"));

        // GET — verify value is reflected
        List<String> output = new ArrayList<>();
        assertEquals(0, CliCommands.payaraGlassFish(
                asList("get-monitoring-service-configuration"), output));
        assertTrue("get-monitoring-service-configuration output should contain 'true'",
                output.stream().anyMatch(line -> line.contains("true")));

        // Restart is required because toggling otelEnabled re-initialises the OTel SDK
        List<String> restartOutput = new ArrayList<>();
        CliCommands.payaraGlassFish(asList("_get-restart-required"), restartOutput);
        assertTrue("Restart must be required after toggling otelEnabled via enable-monitoring",
                restartOutput.stream().anyMatch(line -> Boolean.parseBoolean(line.trim())));
    }
}
