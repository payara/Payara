/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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
    public String getDescription() {
        return "healthcheck.description.machineMemory";
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

