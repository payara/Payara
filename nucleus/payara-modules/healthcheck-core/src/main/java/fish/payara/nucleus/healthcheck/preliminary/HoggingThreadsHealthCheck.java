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

import fish.payara.nucleus.healthcheck.HealthCheckHoggingThreadsExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.entity.ThreadTimes;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static fish.payara.nucleus.notification.TimeHelper.prettyPrintDuration;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-threads")
@RunLevel(StartupRunLevel.VAL)
public class HoggingThreadsHealthCheck extends BaseHealthCheck<HealthCheckHoggingThreadsExecutionOptions,
        HoggingThreadsChecker> {

    private HashMap<Long, ThreadTimes> threadTimes = new HashMap<Long, ThreadTimes>();

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
    public HealthCheckResult doCheck() {
        if (!getOptions().isEnabled()) {
            return null;
        }
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
