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

import fish.payara.nucleus.healthcheck.HealthCheckHoggingThreadsExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.entity.ThreadTimes;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static fish.payara.nucleus.notification.TimeHelper.prettyPrintDuration;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-threads")
@RunLevel(StartupRunLevel.VAL)
public class HoggingThreadsHealthCheck
        extends BaseHealthCheck<HealthCheckHoggingThreadsExecutionOptions, HoggingThreadsChecker>
        implements MonitoringDataSource {

    private final Map<Long, ThreadTimes> threadTimes = new ConcurrentHashMap<>();
    private final AtomicInteger hoggingThreads = new AtomicInteger();

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (isReady()) {
            collector.in("health-check").type("checker").entity("HOGT")
                .collect("checksDone", getChecksDone())
                .collectNonZero("checksFailed", getChecksFailed())
                .collectNonZero("hoggingThreads", hoggingThreads.get());
        }
    }

    @PostConstruct
    void postConstruct() {
        postConstruct(this, HoggingThreadsChecker.class);
    }

    @Override
    public HealthCheckHoggingThreadsExecutionOptions constructOptions(HoggingThreadsChecker checker) {
        return new HealthCheckHoggingThreadsExecutionOptions(Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()), asTimeUnit(checker.getUnit()), 
                Long.parseLong(checker.getThresholdPercentage()), 
                Integer.parseInt(checker.getRetryCount()));
    }

    @Override
    public String getDescription() {
        return "healthcheck.description.hoggingThreads";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        hoggingThreads.set(0);
        HealthCheckResult result = new HealthCheckResult();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "JVM implementation or OS does" +
                    " not support getting CPU times"));
            return result;
        }

        final long[] ids = threadBean.getAllThreadIds();
        for (long id : ids) {
            if (id == Thread.currentThread().getId())
                continue;
            final long c = threadBean.getThreadCpuTime(id);
            final long u = threadBean.getThreadUserTime(id);
            ThreadInfo threadInfo = threadBean.getThreadInfo(id);

            if (c == -1 || u == -1)
                continue;

            ThreadTimes times = threadTimes.get(id);
            if (times == null) {
                times = new ThreadTimes();
                times.setId(id);
                times.setName(threadInfo.getThreadName());
                times.setStartCpuTime(c);
                times.setEndCpuTime(c);
                times.setStartUserTime(u);
                times.setEndUserTime(u);
                threadTimes.put(id, times);
            } else {
                times.setStartCpuTime(times.getEndCpuTime());
                times.setStartUserTime(times.getEndUserTime());
                times.setEndCpuTime(c);
                times.setEndUserTime(u);

                long checkTime = getOptions().getUnit().toMillis(getOptions().getTime());
                long duration = times.getEndCpuTime() - times.getStartCpuTime();
                double percentage = ((double) (TimeUnit.NANOSECONDS.toMillis(duration)) / (double) (checkTime)) * 100;

                if (percentage > options.getThresholdPercentage()) {
                    if (times.getRetryCount() == 0) {
                        times.setInitialStartCpuTime(System.nanoTime());
                        times.setInitialStartUserTime(System.nanoTime());
                    }
                    if (times.getRetryCount() >= options.getRetryCount()) {
                        hoggingThreads.incrementAndGet();
                        result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL,
                                "Thread with <id-name>: " + id + "-" + times.getName() +
                                        " is a hogging thread for the last " +
                                        prettyPrintDuration(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - times
                                                .getInitialStartCpuTime())) + "\n" + prettyPrintStackTrace(threadInfo.getStackTrace())));
                    }
                    times.setRetryCount(times.getRetryCount() + 1);
                }
            }
        }

        return result;
    }
}
