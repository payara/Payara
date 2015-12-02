/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.
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
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-cpu")
@RunLevel(StartupRunLevel.VAL)
public class CpuUsageHealthCheck extends BaseHealthCheck {

    private long timeBefore = 0;
    private long totalTimeBefore = 0;
    private HashMap<Long, ThreadTimes> threadTimes = new HashMap<Long, ThreadTimes>();

    @Inject
    protected HealthCheckService healthCheckService;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HealthCheckServiceConfiguration configuration;

    @PostConstruct
    void postConstruct() {
        CpuUsageChecker checker = configuration.getCheckerByType(CpuUsageChecker.class);
            options = new HealthCheckExecutionOptions(checker.getTime(),
                    asTimeUnit(checker.getUnit()),
                    checker.getPropertyValue(THRESHOLD_CRITICAL, THRESHOLD_DEFAULTVAL_CRITICAL),
                    checker.getPropertyValue(THRESHOLD_WARNING, THRESHOLD_DEFAULTVAL_WARNING),
                    checker.getPropertyValue(THRESHOLD_GOOD, THRESHOLD_DEFAULTVAL_GOOD));
            postConstruct(checker, this, options);
    }

    @Override
    public HealthCheckResult doCheck() {
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
                times.id = id;
                times.startCpuTime  = c;
                times.endCpuTime = c;
                times.startUserTime = u;
                times.endUserTime = u;
                threadTimes.put(id, times);
            }
            else {
                times.endCpuTime  = c;
                times.endUserTime = u;
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

    public long getTotalUserTime( ) {
        final Collection<ThreadTimes> threadTimesValues = threadTimes.values();
        long time = 0L;
        for (ThreadTimes times : threadTimesValues)
            time += times.getEndUserTime() - times.getStartUserTime();
        return time;
    }

    @Override
    protected HealthCheckService getService() {
        return healthCheckService;
    }

    private static class ThreadTimes {
        private long id;
        private long startCpuTime;
        private long startUserTime;
        private long endCpuTime;
        private long endUserTime;

        public long getEndCpuTime() {
            return endCpuTime;
        }

        public long getEndUserTime() {
            return endUserTime;
        }

        public long getStartCpuTime() {
            return startCpuTime;
        }

        public long getStartUserTime() {
            return startUserTime;
        }

        @Override
        public String toString() {
            return "Times{" +
                    "id=" + id +
                    ", startCpuTime=" + startCpuTime +
                    ", endCpuTime=" + endCpuTime +
                    ", startUserTime=" + startUserTime +
                    ", endUserTime=" + endUserTime +
                    '}';
        }
    }
}
