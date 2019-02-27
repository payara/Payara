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
package fish.payara.admin.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import fish.payara.micro.ClusterCommandResult;
import fish.payara.test.domain.BaseHealthCheck;
import fish.payara.test.domain.Checker;
import fish.payara.test.domain.HealthCheckService;
import fish.payara.test.domain.HealthCheckServiceConfiguration;

/**
 * Verifies the correctness of the {@code SetHealthCheckServiceConfiguration} command.
 */
public class SetHealthCheckServiceConfigurationTest extends AsAdminIntegrationTest {

    private HealthCheckServiceConfiguration config;
    private HealthCheckService service;
    private BaseHealthCheck garbageCollection;
    private BaseHealthCheck hoggingThreads;
    private BaseHealthCheck stuckThreads;

    @Before
    public void setUp() {
        config = HealthCheckServiceConfiguration.from(server);
        service = HealthCheckService.from(server);
        garbageCollection = BaseHealthCheck.from(server, "GARBAGE_COLLECTOR");
        hoggingThreads = BaseHealthCheck.from(server, "HOGGING_THREADS");
        stuckThreads = BaseHealthCheck.from(server, "STUCK_THREAD");
    }

    @Test
    public void setHealthCheckServiceConfiguration_EnabledIsMandatory() {
        assertMissingParameter("enabled", asadmin("set-healthcheck-service-configuration", 
                "--service", "gc"));
    }

    @Test
    public void setHealthCheckServiceConfiguration_ServiceIsMandatory() {
        assertMissingParameter("serviceName", asadmin("set-healthcheck-service-configuration", 
                "--enabled", "true"));
    }

    @Test
    public void setHealthCheckServiceConfiguration_ShortNames() {
        String[] shortNames = { "cp", "cu", "gc", "hmu", "ht", "mmu", "st", "mh" };
        setHealthCheckServiceConfigurationNames(shortNames);
    }

    @Test
    public void setHealthCheckServiceConfiguration_FullNames() {
        String[] fullNames = { "CONNECTION_POOL", "CPU_USAGE", "GARBAGE_COLLECTOR", "HEAP_MEMORY_USAGE",
                "HOGGING_THREADS", "MACHINE_MEMORY_USAGE", "STUCK_THREAD", "MP_HEALTH" };
        setHealthCheckServiceConfigurationNames(fullNames);
    }

    private void setHealthCheckServiceConfigurationNames(String[] serviceNames) {
        for (String serviceName : serviceNames) {
            ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                    "--service", serviceName, 
                    "--enabled", "true");
            assertSuccess(result); // just check the name got accepted
        }
    }

    @Test
    public void setHealthCheckServiceConfiguration_Enabled() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc", 
                "--enabled", "true");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertTrue(gcConfig.getEnabled());
        result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc", 
                "--enabled", "false");
        assertFalse(gcConfig.getEnabled());
    }

    @Test
    public void setHealthCheckServiceConfiguration_EnabledDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc", 
                "--enabled", "true",
                "--dynamic", "true");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        BaseHealthCheck check = service.getCheck(gcConfig.getName());
        assertNotNull(check);
        assertTrue(check.isEnabled());
        result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc", 
                "--enabled", "false",
                "--dynamic", "true");
        assertFalse(check.isEnabled());
    }

    @Test
    public void setHealthCheckServiceConfiguration_Time() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--time", "42");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(42L, gcConfig.getTime().longValue());
        assertNotEquals(42L, service.getCheck(gcConfig.getName()).getTime());
    }

    @Test
    public void setHealthCheckServiceConfiguration_TimeDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--time", "42",
                "--dynamic", "true");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(42L, gcConfig.getTime().longValue());
        assertEquals(42L, service.getCheck(gcConfig.getName()).getTime());
    }

    @Test
    public void setHealthCheckServiceConfiguration_TimeUnitUnknownName() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--time-unit", "YEARS");
        assertUnacceptableParameter("time-unit", result);
        assertContains("DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_TimeUnit() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--time-unit", TimeUnit.DAYS.name());
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(TimeUnit.DAYS, gcConfig.getUnit());
        assertNotEquals(TimeUnit.DAYS, service.getCheck(gcConfig.getName()).getUnit());
    }

    @Test
    public void setHealthCheckServiceConfiguration_TimeUnitDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--time-unit", TimeUnit.HOURS.name(),
                "--dynamic", "true"
                );
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(TimeUnit.HOURS, gcConfig.getUnit());
        assertEquals(TimeUnit.HOURS, service.getCheck(gcConfig.getName()).getUnit());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsThresholdBelowMinumum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-threshold", "-1");
        assertUnacceptableParameter("hogginThreadsThreshold", result);
        assertContains("Hogging threads threshold is a percentage so must be greater than zero", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsThresholdAboveMaximum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-threshold", "101");
        assertUnacceptableParameter("hogginThreadsThreshold", result);
        assertContains("Hogging threads threshold is a percentage so must be less than 100", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsThreshold() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-threshold", "33");
        assertSuccess(result);
        Checker htConfig = config.getCheckerByType(hoggingThreads.getCheckerType());
        assertEquals(33, htConfig.getThresholdPercentage().intValue());
        assertNotEquals(Long.valueOf(33), service.getCheck(htConfig.getName()).getThresholdPercentage());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsThresholdDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-threshold", "42",
                "--dynamic", "true");
        assertSuccess(result);
        Checker htConfig = config.getCheckerByType(hoggingThreads.getCheckerType());
        assertEquals(42, htConfig.getThresholdPercentage().intValue());
        assertEquals(Long.valueOf(42), service.getCheck(htConfig.getName()).getThresholdPercentage());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsRetryCountBelowMinimum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-retry-count", "0");
        assertUnacceptableParameter("hogginThreadsRetryCount", result);
        assertContains("Hogging threads retry count must be 1 or more", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsRetryCount() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-retry-count", "13");
        assertSuccess(result);
        Checker htConfig = config.getCheckerByType(hoggingThreads.getCheckerType());
        assertEquals(13, htConfig.getRetryCount().intValue());
        assertNotEquals(Integer.valueOf(13), service.getCheck(htConfig.getName()).getRetryCount());
    }

    @Test
    public void setHealthCheckServiceConfiguration_HogginThreadsRetryCountDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "ht",
                "--enabled", "true",
                "--hogging-threads-retry-count", "24",
                "--dynamic", "true");
        assertSuccess(result);
        Checker htConfig = config.getCheckerByType(hoggingThreads.getCheckerType());
        assertEquals(24, htConfig.getRetryCount().intValue());
        assertEquals(Integer.valueOf(24), service.getCheck(htConfig.getName()).getRetryCount());
    }

    @Test
    public void setHealthCheckServiceConfiguration_StuckThreadsThresholdBelowMinumum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "st",
                "--enabled", "true",
                "--stuck-threads-threshold", "0");
        assertUnacceptableParameter("stuckThreadsThreshold", result);
    }

    @Test
    public void setHealthCheckServiceConfiguration_StuckThreadsThreshold() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "st",
                "--enabled", "true",
                "--stuck-threads-threshold", "13");
        assertSuccess(result);
        Checker stConfig = config.getCheckerByType(stuckThreads.getCheckerType());
        assertEquals(13, stConfig.getThreshold().intValue());
        BaseHealthCheck activeService = service.getCheck(stConfig.getName());
        if (activeService != null) { 
            assertNotEquals(Long.valueOf(13), activeService.getTimeStuck());
        }
    }

    @Test
    public void setHealthCheckServiceConfiguration_StuckThreadsThresholdDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "st",
                "--enabled", "true",
                "--stuck-threads-threshold", "17",
                "--dynamic", "true");
        assertSuccess(result);
        Checker stConfig = config.getCheckerByType(stuckThreads.getCheckerType());
        assertEquals(17, stConfig.getThreshold().intValue());
        assertEquals(Long.valueOf(17), service.getCheck(stConfig.getName()).getTimeStuck());
    }

    @Test
    public void setHealthCheckServiceConfiguration_StuckThreadsThresholdUnitUnknownValue() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "st",
                "--enabled", "true",
                "--stuck-threads-threshold-unit", "YEARS");
        assertUnacceptableParameter("stuck-threads-threshold-unit", result);
    }

    @Test
    public void setHealthCheckServiceConfiguration_StuckThreadsThresholdUnit() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "st",
                "--enabled", "true",
                "--stuck-threads-threshold-unit", TimeUnit.DAYS.name());
        assertSuccess(result);
        Checker stConfig = config.getCheckerByType(stuckThreads.getCheckerType());
        assertEquals(TimeUnit.DAYS, stConfig.getThresholdTimeUnit());
        BaseHealthCheck activeService = service.getCheck(stConfig.getName());
        if (activeService != null) { 
            assertNotEquals(TimeUnit.DAYS, activeService.getUnitStuck());
        }
    }

    @Test
    public void setHealthCheckServiceConfiguration_StuckThreadsThresholdUnitDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "st",
                "--enabled", "true",
                "--stuck-threads-threshold-unit", TimeUnit.HOURS.name(),
                "--dynamic", "true");
        assertSuccess(result);
        Checker stConfig = config.getCheckerByType(stuckThreads.getCheckerType());
        assertEquals(TimeUnit.HOURS, stConfig.getThresholdTimeUnit());
        assertEquals(TimeUnit.HOURS, service.getCheck(stConfig.getName()).getUnitStuck());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdCriticalBelowMinimum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-critical", "-1");
        assertUnacceptableParameter("thresholdCritical", result);
        assertContains("Critical threshold is a percentage so must be greater than zero", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdCriticalAboveMaximum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-critical", "101");
        assertUnacceptableParameter("thresholdCritical", result);
        assertContains("Critical threshold is a percentage so must be less than 100", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdCritical() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-critical", "99");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(99, gcConfig.getThresholdCritical().intValue());
        assertNotEquals(99, service.getCheck(gcConfig.getName()).getThresholdCritical());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdCriticalDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-critical", "88",
                "--dynamic", "true");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(88, gcConfig.getThresholdCritical().intValue());
        assertEquals(88, service.getCheck(gcConfig.getName()).getThresholdCritical());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdWarningBelowMinimum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-warning", "-1");
        assertUnacceptableParameter("thresholdWarning", result);
        assertContains("Warning threshold is a percentage so must be greater than zero", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdWarningAboveMaximum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-warning", "101");
        assertUnacceptableParameter("thresholdWarning", result);
        assertContains("Wanring threshold is a percentage so must be less than 100", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdWarning() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-warning", "99");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(99, gcConfig.getThresholdWarning().intValue());
        assertNotEquals(99, service.getCheck(gcConfig.getName()).getThresholdWarning());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdWarningDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-warning", "88",
                "--dynamic", "true");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(88, gcConfig.getThresholdWarning().intValue());
        assertEquals(88, service.getCheck(gcConfig.getName()).getThresholdWarning());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdGoodBelowMinimum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-good", "-1");
        assertUnacceptableParameter("thresholdGood", result);
        assertContains("Good threshold is a percentage so must be greater than zero", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdGoodAboveMaximum() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-good", "101");
        assertUnacceptableParameter("thresholdGood", result);
        assertContains("Good threshold is a percentage so must be less than 100", result.getOutput());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdGood() {
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-good", "33");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(33, gcConfig.getThresholdGood().intValue());
        assertNotEquals(33, service.getCheck(gcConfig.getName()).getThresholdGood());
    }

    @Test
    public void setHealthCheckServiceConfiguration_ThresholdGoodDynamic() {
        ensureHealthChecksAreEnabled();
        ClusterCommandResult result = asadmin("set-healthcheck-service-configuration", 
                "--service", "gc",
                "--enabled", "true",
                "--threshold-good", "22",
                "--dynamic", "true");
        assertSuccess(result);
        Checker gcConfig = config.getCheckerByType(garbageCollection.getCheckerType());
        assertEquals(22, gcConfig.getThresholdGood().intValue());
        assertEquals(22, service.getCheck(gcConfig.getName()).getThresholdGood());
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
