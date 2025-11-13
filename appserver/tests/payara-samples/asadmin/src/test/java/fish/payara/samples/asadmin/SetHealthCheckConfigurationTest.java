/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.glassfish.embeddable.CommandResult;
import org.junit.Before;
import org.junit.Test;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;

/**
 * Verifies the correctness of the {@code SetHealthCheckConfiguration} command.
 */
public class SetHealthCheckConfigurationTest extends AsadminTest {

    private HealthCheckServiceConfiguration config;
    private HealthCheckService service;

    @Before
    public void setUp() {
        config = getConfigExtensionByType("server-config", HealthCheckServiceConfiguration.class);
        service = getService(HealthCheckService.class);
    }

    @Test
    public void enabledIsOptional() {
        assertSuccess(asadmin("set-healthcheck-configuration"));
    }

    @Test
    public void enabledAffectsConfigButNotService() {
        boolean enabled = service.isEnabled();
        asadmin("set-healthcheck-configuration", 
                "--enabled", "false");
        assertFalse(config.getEnabled());
        CommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true");
        assertSuccess(result);
        assertTrue(config.getEnabled());
        assertUnchanged(enabled, service.isEnabled());
        result = asadmin("set-healthcheck-configuration", 
                "--enabled", "false");
        assertSuccess(result);
        assertFalse(config.getEnabled());
        assertUnchanged(enabled, service.isEnabled());
    }

    @Test
    public void enabledDynamicAffectsConfigAndService() {
        CommandResult result = asadmin("set-healthcheck-configuration", 
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
    public void logNotifierEnabledByDefault() {
        assertTrue(config.getNotifierList().contains("log-notifier"));
        assertTrue(service.getEnabledNotifiers().contains("log-notifier"));
    }

    @Test
    public void historicalTraceEnabledAffectsConfigButNotService() {
        boolean historicalTraceEnabled = service.isHistoricalTraceEnabled();
        CommandResult result = asadmin("set-healthcheck-configuration", 
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
    public void historicalTraceEnabledDynamicAffectsConfigAndService() {
        CommandResult result = asadmin("set-healthcheck-configuration", 
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
    public void historicalTraceStoreSizeAffectsConfigButNotService() {
        Integer historicalTraceStoreSize = service.getHistoricalTraceStoreSize();
        CommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-size", "13");
        assertSuccess(result);
        assertEquals(13, Integer.parseInt(config.getHistoricalTraceStoreSize()));
        assertEquals(historicalTraceStoreSize, service.getHistoricalTraceStoreSize());
    }

    @Test
    public void historicalTraceStoreSizeDynamicAffectsConfigAndService() {
        CommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-size", "13",
                "--dynamic", "true");
        assertSuccess(result);
        assertEquals(13, Integer.parseInt(config.getHistoricalTraceStoreSize()));
        assertEquals(13, service.getHistoricalTraceStoreSize().intValue());
    }

    @Test
    public void historicalTraceStoreTimeoutAffectsConfigButNotService() {
        Long historicalTraceStoreTimeout = getHistoricalTraceStoreTimeout();
        CommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-timeout", "42");
        assertSuccess(result);
        assertEquals(42, Integer.parseInt(config.getHistoricalTraceStoreTimeout()));
        assertEquals(historicalTraceStoreTimeout, getHistoricalTraceStoreTimeout());
    }

    @Test
    public void historicalTraceStoreTimeoutDynamicAffectsConfigAndService() {
        CommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-timeout", "42",
                "--dynamic", "true");
        assertSuccess(result);
        assertEquals(42, Integer.parseInt(config.getHistoricalTraceStoreTimeout()));
        assertEquals(42, getHistoricalTraceStoreTimeout().longValue());
    }

    @Test
    public void historicalTraceStoreSizeBelowMinimumCausesError() {
        CommandResult result = asadmin("set-healthcheck-configuration", 
                "--enabled", "true", 
                "--historical-trace-store-size", "0");
        assertUnacceptableParameter("historicalTraceStoreSize", result);
        assertTrue(result.getOutput().contains("Store size must be greater than 0"));
    }

    private Long getHistoricalTraceStoreTimeout() { // no getter available
        try {
            Field field = service.getClass().getDeclaredField("historicalTraceStoreTimeout");
            field.setAccessible(true);
            return (Long)field.get(service);
        } catch (Exception e) {
            fail("Expected field to exist");
            return null;
        }
    }

}
