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

import fish.payara.nucleus.healthcheck.*;
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
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static fish.payara.nucleus.notification.TimeHelper.prettyPrintDuration;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-cpu")
@RunLevel(StartupRunLevel.VAL)
public class CpuUsageHealthCheck extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, CpuUsageChecker> {

    private long timeBefore = 0;
    private long totalTimeBefore = 0;
    private HashMap<Long, ThreadTimes> threadTimes = new HashMap<Long, ThreadTimes>();

    @PostConstruct
    void postConstruct() {
        postConstruct(this, CpuUsageChecker.class);
    }

    @Override
    public HealthCheckWithThresholdExecutionOptions constructOptions(CpuUsageChecker checker) {
        return super.constructThresholdOptions(checker);
    }

    @Override
    public HealthCheckResult doCheck() {
        if (!getOptions().isEnabled()) {
            return null;
        }
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
