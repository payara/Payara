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
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.CpuUsageChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import static fish.payara.internal.notification.TimeUtil.prettyPrintDuration;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-cpu")
@RunLevel(StartupRunLevel.VAL)
public class CpuUsageHealthCheck
extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, CpuUsageChecker> {

    private final CpuUsage healthCheck = new CpuUsage();
    private final CpuUsage collect = new CpuUsage();

    @PostConstruct
    void postConstruct() {
        postConstruct(this, CpuUsageChecker.class);
    }

    @Override
    public HealthCheckWithThresholdExecutionOptions constructOptions(CpuUsageChecker checker) {
        return super.constructThresholdOptions(checker);
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.cpu";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        HealthCheckResult result = new HealthCheckResult();
        try {
            double percentage = healthCheck.percentage();
            result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(percentage), 
                    "CPU%: " + new DecimalFormat("#.00").format(percentage)
                    + ", Time CPU used: " + prettyPrintDuration(TimeUnit.NANOSECONDS.toMillis(healthCheck.getLastTimeDelta()))));
        } catch (UnsupportedOperationException ex) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "JVM implementation or OS does not support getting CPU times"));
            return result;
        }
        return result;
    }

    private static final class CpuUsage {

        private volatile long nanotimeBefore = System.nanoTime();
        private volatile long totalCpuNanosBefore = 0;
        private volatile long timeDelta;

        CpuUsage() {
            // make visible
        }

        double percentage() {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            if (!threadBean.isCurrentThreadCpuTimeSupported()) {
                throw new UnsupportedOperationException("CurrentThreadCpuTimeSupported not supported.");
            }
            long currentThreadId = Thread.currentThread().getId();
            long totalCpuNanos = 0L;
            for (long id : threadBean.getAllThreadIds()) {
                if (id != currentThreadId) {
                    final long threadCpuTime = threadBean.getThreadCpuTime(id);
                    if (threadCpuTime >= 0L) {
                        totalCpuNanos += threadCpuTime;
                    }
                }
            }
            long nanotime = System.nanoTime();
            timeDelta = nanotime - nanotimeBefore;
            long cpuTimeDelta = totalCpuNanos - totalCpuNanosBefore;
            double percentage = Math.max(0d, Math.min(100d, 100d * cpuTimeDelta / timeDelta));
            totalCpuNanosBefore = totalCpuNanos;
            nanotimeBefore = nanotime;
            return percentage;
        }

        long getLastTimeDelta() {
            return timeDelta;
        }
    }

}
