/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.MachineMemoryUsageChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.PostConstruct;
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
extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, MachineMemoryUsageChecker> {

    private static final String MEMTOTAL = "MemTotal:";
    private static final String MEMFREE = "MemFree:";
    private static final String MEMAVAILABLE = "MemAvailable:";
    private static final String ACTIVEFILE = "Active(file):";
    private static final String INACTIVEFILE = "Inactive(file):";
    private static final String RECLAIMABLE = "SReclaimable:";
    private static final String KB = "kB";

    private final PysicalMemoryUsage stats = new PysicalMemoryUsage();

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
        HealthCheckResult result = new HealthCheckResult();
        long memTotal = 0;
        try {
            double usedPercentage = stats.usedPercentage();
            result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(usedPercentage),
                    "Physical Memory Used: " + prettyPrintBytes(stats.getUsedMemory()) + " - " +
                            "Total Physical Memory: " + prettyPrintBytes(stats.getTotalMemory()) + " - " +
                            "Memory Used%: " + new DecimalFormat("#.00").format(usedPercentage) + "%"));

        } catch (IOException exception) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                    "Memory information cannot be read for retrieving physical memory usage values",
                    exception));
        } catch (ArithmeticException exception) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                    "Error occurred while calculating memory usage values. Total memory is " + memTotal,
                    exception));
        } catch (Exception exception) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                    "Operating system methods cannot be invoked for retrieving physical memory usage values",
                    exception));
        }
        return result;
    }

    private static final class PysicalMemoryUsage {
        private volatile long totalMemory;
        private volatile long availableMemory;

        PysicalMemoryUsage() {
            // make visible
        }

        long getTotalMemory() {
            return totalMemory;
        }

        long getUsedMemory() {
            return totalMemory - availableMemory;
        }

        double usedPercentage() throws Exception {
            if (isLinux()) {
                updateLinux();
            } else {
                updateNonLinux();
            }
            long usedMemory = totalMemory - availableMemory;
            return totalMemory == 0L ? 0d : 100d * usedMemory / totalMemory;
        }

        private void updateLinux() throws IOException {
            List<String> lines = Files.readAllLines(Paths.get("/proc/meminfo"), StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                totalMemory = 0;
                availableMemory = 0;
                return;
            }
            long otherAvailableMemory = 0;
            availableMemory = 0;
            for (String line : lines) {
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    switch(parts[0]) {
                    case MEMAVAILABLE: 
                        availableMemory = parseMemInfo(parts);
                        break;
                    case MEMTOTAL:
                        totalMemory = parseMemInfo(parts);
                        break;
                    case INACTIVEFILE:
                    case ACTIVEFILE:
                    case RECLAIMABLE:
                    case MEMFREE:
                        otherAvailableMemory += parseMemInfo(parts); 
                        break;
                    default:
                        // NOOP
                    }
                }
            }
            if (availableMemory == 0) {
                availableMemory = otherAvailableMemory;
            }
        }

        private void updateNonLinux() throws Exception {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            totalMemory = invokeMethodFor(osBean, "getTotalPhysicalMemorySize");
            availableMemory = invokeMethodFor(osBean, "getFreePhysicalMemorySize");
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


}

