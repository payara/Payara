/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.admin.cli.healthcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import fish.payara.admin.cli.AsAdminIntegrationTest;
import fish.payara.micro.ClusterCommandResult;
import fish.payara.test.domain.healthcheck.HealthCheckService;
import fish.payara.test.domain.healthcheck.HealthCheckServiceConfiguration;
import fish.payara.test.domain.notification.Notifier;
import fish.payara.test.domain.notification.NotifierExecutionOptions;

/**
 * Verifies the correctness of the {@code SetHealthCheckConfiguration} command.
 */
public class SetHealthCheckConfigurationTest extends AsAdminIntegrationTest {

    private HealthCheckServiceConfiguration config;
    private HealthCheckService service;
    private Class<?> logNotifierType;

    @Before
    public void setUp() {
        config = HealthCheckServiceConfiguration.from(server);
        service = HealthCheckService.from(server);
        logNotifierType = server.getClass(Notifier.LOG_NOTIFIER_CLASS_NAME);
    }

    @Test
    public void setHealthcheckConfiguration_EnabledIsMandatory() {
        assertMissingParameter("enabled", asadmin("set-healthcheck-configuration"));
    }

    @Test
    public void setHealthcheckConfiguration_Enabled() {
        boolean enabled = service.isEnabled();
        asadmin("set-healthcheck-configuration", 
                "--enabled", "false");
        assertFalse(config.getEnabled());
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true");
        assertSuccess(result);
        assertEquals("log.enabled was false set to true\n", result.getOutput());
        assertTrue(config.getEnabled());
        assertUnchanged(enabled, service.isEnabled());
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "false");
        assertSuccess(result);
        assertEquals("log.enabled was true set to false\n", result.getOutput());
        assertFalse(config.getEnabled());
        assertUnchanged(enabled, service.isEnabled());
    }

    @Test
    public void setHealthcheckConfiguration_EnabledDynamic() {
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--dynamic", "true");
        assertSuccess(result);
        assertTrue(service.isEnabled());
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "false", 
                "--dynamic", "true");
        assertSuccess(result);
        assertFalse(service.isEnabled());
    }

    @Test
    public void setHealthcheckConfiguration_EnabledAppliesToLogNotifier() {
        boolean logEnabled = getLogNotifierOptions().isEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "false");
        assertSuccess(result);
        Notifier logNotifier = config.getNotifierByType(logNotifierType);
        assertFalse(logNotifier.getEnabled());
        assertUnchanged(logEnabled, getLogNotifierOptions().isEnabled());
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true");
        assertSuccess(result);
        assertTrue(logNotifier.getEnabled());
        assertUnchanged(logEnabled, getLogNotifierOptions().isEnabled());
    }

    @Test
    public void setHealthcheckConfiguration_EnabledDynamicAppliesToLogNotifier() {
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "false",
                "--dynamic", "true");
        assertSuccess(result);
        Notifier logNotifier = config.getNotifierByType(logNotifierType);
        assertFalse(logNotifier.getEnabled());
        // can't verify the active options since disabled service will not update these
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true",
                "--dynamic", "true");
        assertSuccess(result);
        assertTrue(logNotifier.getEnabled());
        assertTrue(getLogNotifierOptions().isEnabled());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceEnabled() {
        boolean historicalTraceEnabled = service.isHistoricalTraceEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-enabled", "false");
        assertSuccess(result);
        assertUnchanged(historicalTraceEnabled, service.isHistoricalTraceEnabled());
        assertFalse(config.getHistoricalTraceEnabled());
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-enabled", "true");
        assertSuccess(result);
        assertTrue(config.getHistoricalTraceEnabled());
        assertUnchanged(historicalTraceEnabled, service.isHistoricalTraceEnabled());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceEnabledDynamic() {
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true",
                "--historical-trace-enabled", "true", 
                "--dynamic", "true");
        assertSuccess(result);
        assertTrue(service.isHistoricalTraceEnabled());
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-enabled", "false", 
                "--dynamic", "true");
        assertSuccess(result);
        assertFalse(service.isHistoricalTraceEnabled());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceStoreSize() {
        Integer historicalTraceStoreSize = service.getHistoricalTraceStoreSize();
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-size", "13");
        assertSuccess(result);
        assertEquals(13, config.getHistoricalTraceStoreSize());
        assertEquals(historicalTraceStoreSize, service.getHistoricalTraceStoreSize());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceStoreSizeDynamic() {
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-size", "13",
                "--dynamic", "true");
        assertSuccess(result);
        assertEquals(13, config.getHistoricalTraceStoreSize());
        assertEquals(13, service.getHistoricalTraceStoreSize().intValue());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceStoreTimeout() {
        Long historicalTraceStoreTimeout = service.getHistoricalTraceStoreTimeout();
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-timeout", "42");
        assertSuccess(result);
        assertEquals(42, config.getHistoricalTraceStoreTimeout());
        assertEquals(historicalTraceStoreTimeout, service.getHistoricalTraceStoreTimeout());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceStoreTimeoutDynamic() {
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-timeout", "42",
                "--dynamic", "true");
        assertSuccess(result);
        assertEquals(42, config.getHistoricalTraceStoreTimeout());
        assertEquals(42, service.getHistoricalTraceStoreTimeout().longValue());
    }

    @Test
    public void setHealthcheckConfiguration_HistoricalTraceStoreSizeBelowMinimum() {
        ClusterCommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-size", "0");
        assertUnacceptableParameter("historicalTraceStoreSize", result);
        assertTrue(result.getOutput().contains("Store size must be greater than 0"));
    }

    private NotifierExecutionOptions getLogNotifierOptions() {
        return service.getNotifierExecutionOptions("LOG");
    }
}
