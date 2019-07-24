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

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.CpuUsageChecker;
import fish.payara.nucleus.healthcheck.entity.ThreadTimes;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static fish.payara.nucleus.notification.TimeHelper.prettyPrintDuration;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-cpu")
@RunLevel(StartupRunLevel.VAL)
public class CpuUsageHealthCheck
        extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, CpuUsageChecker>
        implements MonitoringDataSource {

    private volatile long timeBefore = 0;
    private volatile long totalTimeBefore = 0;
    private final Map<Long, ThreadTimes> threadTimes = new ConcurrentHashMap<>();

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (isReady()) {
            collector.in("health-check").type("checker").entity("CPUC")
                .collect("checksDone", getChecksDone())
                .collectNonZero("checksFailed", getChecksFailed())
                .collectNonZero("totalCpuTime", TimeUnit.NANOSECONDS.toMillis(getTotalCpuTime()));
        }
    }

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
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "JVM implementation or OS does not support getting CPU times"));
            return result;
        }

        final long[] ids = threadBean.getAllThreadIds();
        for (long id : ids) {
            if (id == java.lang.Thread.currentThread().getId())
                continue;
            final long c = threadBean.getThreadCpuTime(id);
            final long u = threadBean.getThreadUserTime(id);

            if (c == -1 || u == -1)
                continue;

            ThreadTimes times = threadTimes.get(id);
            if (times == null) {
                times = new ThreadTimes();
                times.setId(id);
                times.setStartCpuTime(c);
                times.setEndCpuTime(c);
                times.setStartUserTime(u);
                times.setEndUserTime(u);
                threadTimes.put(id, times);
            }
            else {
                times.setEndCpuTime(c);
                times.setEndUserTime(u);
            }
        }

        long  totalCpuTime = getTotalCpuTime();
        long time = System.nanoTime();
        double percentage = ((double)(totalCpuTime - totalTimeBefore) / (double)(time - timeBefore)) * 100;

        result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(percentage), "CPU%: " + new DecimalFormat("#.00").format(percentage)
                + ", Time CPU used: " + prettyPrintDuration(TimeUnit.NANOSECONDS.toMillis(getTotalCpuTime() - totalTimeBefore))));

        totalTimeBefore = totalCpuTime;
        timeBefore = time;

        return result;
    }

    public long getTotalCpuTime( ) {
        final Collection<ThreadTimes>  threadTimesValues = threadTimes.values();
        long time = 0L;
        for (ThreadTimes times : threadTimesValues)
            time += times.getEndCpuTime() - times.getStartCpuTime();
        return time;
    }
}
