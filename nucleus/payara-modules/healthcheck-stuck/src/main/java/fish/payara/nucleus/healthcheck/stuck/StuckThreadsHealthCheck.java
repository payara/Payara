/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.stuck;

import fish.payara.internal.notification.EventLevel;
import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.MonitoringWatchCollector;
import fish.payara.monitoring.collect.MonitoringWatchSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckStatsProvider;
import fish.payara.nucleus.healthcheck.HealthCheckStuckThreadExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.StuckThreadsChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 4.1.2.173
 * @author jonathan coustick (initial)
 * @author Jan Bernitt (consumer based and monitoring)
 */
@Service(name = "healthcheck-stuck")
@RunLevel(StartupRunLevel.VAL)
public class StuckThreadsHealthCheck extends
        BaseHealthCheck<HealthCheckStuckThreadExecutionOptions, StuckThreadsChecker>
        implements MonitoringDataSource, MonitoringWatchSource, HealthCheckStatsProvider {

    private final Map<String, Number> stuckThreadResult = new ConcurrentHashMap<>();
    private static final String STUCK_THREAD_COUNT = "count";
    private static final String STUCK_THREAD_MAX_DURATION = "maxDuration";
    private static final Set<String> VALID_SUB_ATTRIBUTES = Set.of(STUCK_THREAD_COUNT, STUCK_THREAD_MAX_DURATION);

    @FunctionalInterface
    private interface StuckThreadConsumer {
        void accept(long workStartedTime, long timeWorkingInMillis, long thresholdInMillis, ThreadInfo stuck);
    }

    @Inject
    private StuckThreadsStore stuckThreadsStore;

    @Inject
    private StuckThreadsChecker checker;

    @PostConstruct
    void postConstruct() {
        postConstruct(this, StuckThreadsChecker.class);
    }

    @Override
    public Object getValue(Class type, String attributeName, String subAttributeName) {
        if (subAttributeName == null) {
            throw new IllegalArgumentException("sub-attribute name is required");
        }
        if (!VALID_SUB_ATTRIBUTES.contains(subAttributeName)) {
            throw new IllegalArgumentException("Invalid sub-attribute name, supported sub-attributes are " + VALID_SUB_ATTRIBUTES);
        }
        if (!Number.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("sub-attribute type must be number");
        }
        return stuckThreadResult.getOrDefault(subAttributeName, 0);
    }

    @Override
    public Set<String> getAttributes() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getSubAttributes() {
        return VALID_SUB_ATTRIBUTES;
    }

    @Override
    public boolean isEnabled() {
       return this.getOptions() != null ? this.getOptions().isEnabled() : false;
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        HealthCheckResult result = new HealthCheckResult();
        acceptStuckThreads((workStartedTime, timeWorkingInMillis, thresholdInMillis, info) ->
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.WARNING, "Stuck Thread: " + info.toString())));
        return result;
    }

    @Override
    protected EventLevel createNotificationEventLevel (HealthCheckResultStatus checkResult) {
        if (checkResult == HealthCheckResultStatus.FINE) {
            return EventLevel.INFO;
        }
        return EventLevel.WARNING;
    }

    @Override
    @MonitoringData(ns = "health", intervalSeconds = 4)
    public void collect(MonitoringDataCollector collector) {
        if (options == null || !options.isEnabled()) {
            return;
        }
        AtomicInteger count = new AtomicInteger(0);
        AtomicLong maxDuration = new AtomicLong(0L);
        acceptStuckThreads((workStartedTime, timeWorkingInMillis, thresholdInMillis, info) -> {
            String thread = info.getThreadName();
            if (thread == null || thread.isEmpty()) {
                thread = String.valueOf(info.getThreadId());
            }
            collector.annotate("StuckThreadDuration", timeWorkingInMillis, true, //
                    "Thread", thread, // OBS! must be the first attribute as it is the key.
                    "Started", String.valueOf(workStartedTime), //
                    "Threshold", String.valueOf(thresholdInMillis), //
                    "Locked", Boolean.toString(info.getLockInfo() != null), //
                    "Suspended", String.valueOf(info.isSuspended()), //
                    "State", composeStateText(info));
            count.incrementAndGet();
            maxDuration.updateAndGet(value -> Math.max(value, timeWorkingInMillis));
        });
        collector.collect("StuckThreadDuration", maxDuration);
        collector.collect("StuckThreadCount", count);
        stuckThreadResult.put(STUCK_THREAD_MAX_DURATION, maxDuration.get());
        stuckThreadResult.put(STUCK_THREAD_COUNT, count.get());
    }

    @Override
    public void collect(MonitoringWatchCollector collector) {
        if (options == null || !options.isEnabled()) {
            return;
        }
        long thresholdInMillis = getThresholdInMillis();
        collector.watch("ns:health StuckThreadDuration", "Stuck Threads", "ms")
            .red(thresholdInMillis, -30000L, false, null, null, false)
            .green(-thresholdInMillis, 1, false, null, null, false);
    }

    private static String composeStateText(ThreadInfo info) {
        if (info.getLockInfo() == null) {
            return "Running";
        }
        Thread.State state = info.getThreadState();
        return composeActionText(state) + info.getLockInfo().toString();
    }

    private static String composeActionText(Thread.State state) {
        switch(state) {
        case BLOCKED:
            return "Blocked on ";
        case WAITING:
        case TIMED_WAITING:
            return "Waiting on ";
        default: return "Running ";
        }
    }

    private void acceptStuckThreads(StuckThreadConsumer consumer) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long thresholdInMillis = getThresholdInMillis();
        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, Long> threads = stuckThreadsStore.getThreads();
        String[] blacklist = checker.getBlacklistPatterns().split(",");
        for (Entry<Long, Long> thread : threads.entrySet()) {
            Long threadId = thread.getKey();
            long workStartedTime = thread.getValue();
            long timeWorkingInMillis = now - workStartedTime;
            if (timeWorkingInMillis > thresholdInMillis){
                ThreadInfo info = bean.getThreadInfo(threadId, Integer.MAX_VALUE);
                if (info != null && !isInBlacklist(info.getThreadName(), blacklist)){ //check thread hasn't died already
                    consumer.accept(workStartedTime, timeWorkingInMillis, thresholdInMillis, info);
                }
            }
        }
    }

    private boolean isInBlacklist(String threadName, String[] blacklistPatterns) {
        return Arrays.stream(blacklistPatterns).anyMatch(threadName::matches);
    }

    private long getThresholdInMillis() {
        return Math.max(1, TimeUnit.MILLISECONDS.convert(options.getTimeStuck(), options.getUnitStuck()));
    }


    @Override
    public HealthCheckStuckThreadExecutionOptions constructOptions(StuckThreadsChecker checker) {
        return new HealthCheckStuckThreadExecutionOptions(Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()), asTimeUnit(checker.getUnit()), Boolean.valueOf(checker.getAddToMicroProfileHealth()),
                Long.parseLong(checker.getThreshold()), asTimeUnit(checker.getThresholdTimeUnit()), checker.getBlacklistPatterns());
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.stuckThreads";
    }

}
