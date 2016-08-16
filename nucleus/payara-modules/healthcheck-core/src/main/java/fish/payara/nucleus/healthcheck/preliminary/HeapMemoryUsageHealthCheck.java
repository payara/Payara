/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
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

import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.HeapMemoryUsageChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-heap")
@RunLevel(StartupRunLevel.VAL)
public class HeapMemoryUsageHealthCheck extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, HeapMemoryUsageChecker> {

    @PostConstruct
    void postConstruct() {
        postConstruct(this, HeapMemoryUsageChecker.class);
    }

    @Override
    public HealthCheckWithThresholdExecutionOptions constructOptions(HeapMemoryUsageChecker checker) {
        return super.constructThresholdOptions(checker);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean() ;
        MemoryUsage heap = memBean.getHeapMemoryUsage();

        String heapValueText = String.format("heap: init: %s, used: %s, committed: %s, max.: %s",
                prettyPrintBytes(heap.getInit()),
                prettyPrintBytes(heap.getUsed()),
                prettyPrintBytes(heap.getCommitted()),
                prettyPrintBytes(heap.getMax()));
        Double percentage = calculatePercentage(heap);
        result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(percentage), heapValueText + "heap%: " + percentage + "%"));

        return result;
    }

    private Double calculatePercentage(MemoryUsage usage) {
        if (usage.getMax() > 0) {
            return Math.floor(((double)usage.getUsed() / (double)usage.getMax()) * 100);
        }
        return null;
    }
}

