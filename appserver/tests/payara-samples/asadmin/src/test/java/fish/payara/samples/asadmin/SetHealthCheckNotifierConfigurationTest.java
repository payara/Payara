/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.asadmin;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.samples.ServerOperations;

import java.util.function.Supplier;

import org.glassfish.embeddable.CommandResult;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies the correctness of the {@code SetHealthCheckServiceNotifierConfiguration} command.
 */
public class SetHealthCheckNotifierConfigurationTest extends AsadminTest {
    private HealthCheckServiceConfiguration config;
    private HealthCheckService service;

    private Supplier<Boolean> isLogNotifierConfigEnabled;
    private Supplier<Boolean> isLogNotifierEnabled;

    @Before
    public void setUp() {
        config = getConfigExtensionByType("server-config", HealthCheckServiceConfiguration.class);
        service = getService(HealthCheckService.class);

        this.isLogNotifierConfigEnabled = () -> config.getNotifierList().contains("log-notifier");
        this.isLogNotifierEnabled = () -> service.getEnabledNotifiers().contains("log-notifier");
    }

    @Test
    public void enabledIsOptional() {
        assertSuccess(asadmin("set-healthcheck-configuration"));
    }

    @Test
    public void notifierNamesAreAccepted() {
        final String[] names;
        if (ServerOperations.isServer()) {
            names = new String[] {"log-notifier", "jms-notifier", "cdieventbus-notifier", "eventbus-notifier"};
        } else {
            names = new String[] {"log-notifier", "cdieventbus-notifier", "eventbus-notifier"};
        }
        for (String notiferName : names) {
            CommandResult result = asadmin("set-healthcheck-configuration",
                    "--enableNotifiers", notiferName,
                    "--enabled", "true");
            assertSuccess(result); // just check the name got accepted
        }
    }

    @Test
    public void incorrectNotifierNamesAreNotAccepted() {
        CommandResult result = asadmin("set-healthcheck-configuration",
                "--enableNotifiers", "log-notifier,bad-notifier",
                "--enabled", "true");
        assertFailure(result);
    }

    @Test
    public void enabledAffectsConfigButNotService() {
        boolean logEnabled = isLogNotifierEnabled.get();
        CommandResult result = asadmin("set-healthcheck-configuration",
                "--disableNotifiers", "log-notifier",
                "--enabled", "true");
        assertSuccess(result);
        assertFalse(isLogNotifierConfigEnabled.get());
        assertUnchanged(logEnabled, isLogNotifierEnabled.get());
        result = asadmin("set-healthcheck-configuration",
                "--enableNotifiers", "log-notifier",
                "--enabled", "false");
        assertTrue(isLogNotifierConfigEnabled.get());
        assertUnchanged(logEnabled, isLogNotifierEnabled.get());
    }

    @Test
    public void enabledDynamicAffectsConfigAndService() {
        ensureHealthChecksAreEnabled();
        CommandResult result = asadmin("set-healthcheck-configuration",
                "--disableNotifiers", "log-notifier",
                "--enabled", "true",
                "--dynamic", "true");
        assertSuccess(result);
        assertFalse(isLogNotifierConfigEnabled.get());
        assertFalse(isLogNotifierEnabled.get());
        result = asadmin("set-healthcheck-configuration",
                "--enableNotifiers", "log-notifier",
                "--enabled", "false",
                "--dynamic", "true");
        assertTrue(isLogNotifierConfigEnabled.get());
        assertTrue(isLogNotifierEnabled.get());
    }

    /**
     * Dynamic changes only take effect when the health check service is enabled so we make sure it is.
     */
    private void ensureHealthChecksAreEnabled() {
        if (service.isEnabled()) {
            return; // already enabled, fine
        }
        assertSuccess(asadmin("set-healthcheck-configuration",
                "--enabled", "true",
                "--dynamic", "true"));
    }
}
