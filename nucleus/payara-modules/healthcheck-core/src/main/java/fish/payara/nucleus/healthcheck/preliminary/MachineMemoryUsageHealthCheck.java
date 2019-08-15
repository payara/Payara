/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.MachineMemoryUsageChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-machinemem")
@RunLevel(StartupRunLevel.VAL)
public class MachineMemoryUsageHealthCheck
        extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, MachineMemoryUsageChecker>
        implements MonitoringDataSource {

    private static final String MEMTOTAL = "MemTotal:";
    private static final String MEMFREE = "MemFree:";
    private static final String MEMAVAILABLE = "MemAvailable:";
    private static final String ACTIVEFILE = "Active(file):";
    private static final String INACTIVEFILE = "Inactive(file):";
    private static final String RECLAIMABLE = "SReclaimable:";
    private static final String KB = "kB";

    private volatile long memTotal;
    private volatile long memAvailable;
    private volatile long memUsed;
    private volatile double usedPercentage;

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (isReady()) {
            collector.in("health-check").type("checker").entity("MEMM")
                .collect("checksDone", getChecksDone())
                .collectNonZero("checksFailed", getChecksFailed())
                .collectNonZero("memBytesTotal", memTotal)
                .collectNonZero("memBytesAvailable", memAvailable)
                .collectNonZero("memBytesUsed", memUsed)
                .collect("usedPercentage", usedPercentage);
        }
    }

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
    protected HealthCheckResult doCheckInternal() {
        if (isLinux()) {
            return doCheckLinux();
        }
        return doCheckNonLinux();
    }

    private HealthCheckResult doCheckLinux() {
        HealthCheckResult result = new HealthCheckResult();
        long memTotal = 0;
        try {
            List<String> lines = Files.readAllLines(Paths.get("/proc/meminfo"), StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return result;
            }
            long memAvailable = 0;
            long memFree = 0;
            long memActiveFile = 0;
            long memInactiveFile = 0;
            long memReclaimable= 0;
            boolean memAvailableFound = false;

            for (String line : lines) {
                String[] parts = line.split("\\s+");

                if (parts.length > 1) {
                    String part = parts[0];
                    if (MEMAVAILABLE.equals(part)) {
                        memAvailable = parseMemInfo(parts);
                        memAvailableFound = true;
                    }
                    if (MEMFREE.equals(part)) {
                        memFree = parseMemInfo(parts);
                    }
                    if (MEMTOTAL.equals(part)) {
                        memTotal = parseMemInfo(parts);
                    }
                    if (ACTIVEFILE.equals(part)) {
                        memActiveFile = parseMemInfo(parts);
                    }
                    if (INACTIVEFILE.equals(part)) {
                        memInactiveFile = parseMemInfo(parts);
                    }
                    if (RECLAIMABLE.equals(part)) {
                        memReclaimable = parseMemInfo(parts);
                    }
                }
            }

            if (!memAvailableFound) {
                memAvailable = memFree + memActiveFile + memInactiveFile + memReclaimable;
            }

            double usedPercentage = memTotal == 0L ? 0d :((double) (memTotal - memAvailable) / memTotal) * 100;

            this.memTotal = memTotal;
            this.memAvailable = memAvailable;
            this.usedPercentage = usedPercentage;
            this.memUsed = memTotal - memAvailable;

            result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(usedPercentage),
                    "Physical Memory Used: " + prettyPrintBytes(memTotal - memAvailable) + " - " +
                            "Total Physical Memory: " + prettyPrintBytes(memTotal) + " - " +
                            "Memory Used%: " + new DecimalFormat("#.00").format(usedPercentage) + "%"));

        } catch (IOException exception) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                    "Memory information cannot be read for retrieving physical memory usage values",
                    exception));
        } catch (ArithmeticException exception) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                    "Error occurred while calculating memory usage values. Total memory is " + memTotal,
                    exception));
        }
        return result;
    }

    private HealthCheckResult doCheckNonLinux() {
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

    private static boolean isLinux() {
        String osName = System.getProperty("os.name");
        return osName.startsWith("Linux") ||
                osName.startsWith("FreeBSD") ||
                osName.startsWith("OpenBSD") ||
                osName.startsWith("gnu") ||
                osName.startsWith("netbsd");
    }

    private static long parseMemInfo(String[] parts) {
        long memory = 0;

        if (parts.length >= 2) {
            memory = Long.parseLong(parts[1]);
            if (parts.length > 2 && KB.equals(parts[2])) {
                memory *= 1024;
            }
        }
        return memory;
    }

    private static Long invokeMethodFor(OperatingSystemMXBean osBean, String methodName) throws Exception {
        Method m = osBean.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return (Long) m.invoke(osBean);
    }
}

