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
import fish.payara.nucleus.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.MachineMemoryUsageChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-machinemem")
@RunLevel(StartupRunLevel.VAL)
public class MachineMemoryUsageHealthCheck extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions,
        MachineMemoryUsageChecker> {

    @PostConstruct
    void postConstruct() {
        postConstruct(this, MachineMemoryUsageChecker.class);
    }

    @Override
    public HealthCheckWithThresholdExecutionOptions constructOptions(MachineMemoryUsageChecker checker) {
        return super.constructThresholdOptions(checker);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Long totalPhysicalMemSize = invokeMethodFor(osBean, "getTotalPhysicalMemorySize");
            Long freePhysicalMemSize = invokeMethodFor(osBean, "getFreePhysicalMemorySize");

            double usedPercentage = ((double) (totalPhysicalMemSize - freePhysicalMemSize) / totalPhysicalMemSize) *
                    100;

            result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(usedPercentage),
                    "Physical Memory Used: " + prettyPrintBytes((totalPhysicalMemSize - freePhysicalMemSize)) + " - " +
                            "Total Physical Memory: " + prettyPrintBytes(totalPhysicalMemSize) + " - " +
                            "Memory Used%: " + new DecimalFormat("#.00").format(usedPercentage) + "%"));

        } catch (Exception exception) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                    "Operating system methods cannot be invoked for retrieving physical memory usage values",
                    exception));
        }

        return result;
    }

    private Long invokeMethodFor(OperatingSystemMXBean osBean, String methodName) throws Exception {
        Method m = osBean.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return (Long) m.invoke(osBean);
    }
}

