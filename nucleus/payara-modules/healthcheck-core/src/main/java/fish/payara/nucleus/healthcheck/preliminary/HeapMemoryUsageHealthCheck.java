/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.nucleus.healthcheck.*;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HeapMemoryUsageChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-heap")
@RunLevel(StartupRunLevel.VAL)
public class HeapMemoryUsageHealthCheck extends BaseHealthCheck implements HealthCheckConstants {

    @Inject
    protected HealthCheckService healthCheckService;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    HealthCheckServiceConfiguration configuration;

    @PostConstruct
    void postConstruct() {
        if (configuration == null) {
            return;
        }

        HeapMemoryUsageChecker checker = configuration.getCheckerByType(HeapMemoryUsageChecker.class);
        options = new HealthCheckExecutionOptions(checker.getTime(),
                asTimeUnit(checker.getUnit()),
                checker.getPropertyValue(THRESHOLD_CRITICAL, THRESHOLD_DEFAULTVAL_CRITICAL),
                checker.getPropertyValue(THRESHOLD_WARNING, THRESHOLD_DEFAULTVAL_WARNING),
                checker.getPropertyValue(THRESHOLD_GOOD, THRESHOLD_DEFAULTVAL_GOOD));
        postConstruct(checker, this, options);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean() ;
        MemoryUsage heap = memBean.getHeapMemoryUsage();

        String heapValueText = (String.format("heap: init: %s, used: %s, committed: %s, max.: %s",
                prettyPrintBytes(heap.getInit()),
                prettyPrintBytes(heap.getUsed()),
                prettyPrintBytes(heap.getCommitted()),
                prettyPrintBytes(heap.getMax())));
        Double percentage = calculatePercentage(heap);
        result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(percentage), heapValueText + "\n"
                + "heap%: " + percentage + "%"));

        return result;
    }

    @Override
    protected HealthCheckService getService() {
        return healthCheckService;
    }

    private Double calculatePercentage(MemoryUsage usage) {
        if (usage.getMax() > 0) {
            return Math.floor(((double)usage.getUsed() / (double)usage.getMax()) * 100);
        }
        return null;
    }
}

