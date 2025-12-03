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

import fish.payara.internal.notification.EventLevel;
import fish.payara.nucleus.healthcheck.HealthCheckHoggingThreadsExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static fish.payara.internal.notification.TimeUtil.prettyPrintDuration;

/**
 * A "hogging thread" is a thread that uses most of the CPU during the measured period.
 *
 * @author mertcaliskan (initial version)
 * @author Jan Bernitt (consumer based and monitoring)
 */
@Service(name = "healthcheck-threads")
@RunLevel(StartupRunLevel.VAL)
public class HoggingThreadsHealthCheck extends BaseHealthCheck<HealthCheckHoggingThreadsExecutionOptions, HoggingThreadsChecker> {

    @FunctionalInterface
    private interface HoggingThreadConsumer {

        void accept(int percentage, int threshold, long totalTimeHogging, String initialMethod, ThreadInfo info);
    }

    /**
     * Book-keeping record for each thread. All times are in milliseconds.
     */
    private static final class ThreadCpuTimeRecord {

        ThreadCpuTimeRecord() {
            // make visible
        }

        /**
         * Timestamp in milliseconds from when the measuring interval started
         */
        long startOfIntervalTimestamp;
        /**
         * Total number of milliseconds spend by the thread doing CPU at the start of the measuring interval.
         * This is not zero as only ever increases from zero after the thread has been created.
         */
        long startOfIntervalCpuTime;
        /**
         * Timestamp in milliseconds from when the thread first exceeded the threshold and was identified as "hogging".
         */
        long startOfExceedingThresholdTimestamp;
        /**
         * Number of times in a row the check has identified the thread as "hogging"
         */
        int identifiedAsHoggingCount;
        /**
         * This is the method on top of the stack trace when thread first is identified as "hogging". This method is the
         * most likely candidate. Using the "current" method often is misleading as worker threads at some point get
         * back to idle in the pool which would show the parking as the last method.
         */
        String identifiedAsHoggingMethod;
    }

    private boolean supported;
    private final Map<Long, ThreadCpuTimeRecord> checkRecordsByThreadId = new ConcurrentHashMap<>();
    private final Map<Long, ThreadCpuTimeRecord> colletionRecordsByThreadId = new ConcurrentHashMap<>();

    @PostConstruct
    void postConstruct() {
        postConstruct(this, HoggingThreadsChecker.class);
        supported = ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported();
    }
    
    @Override
    public HealthCheckHoggingThreadsExecutionOptions constructOptions(HoggingThreadsChecker checker) {
        return new HealthCheckHoggingThreadsExecutionOptions(Boolean.parseBoolean(checker.getEnabled()),
                Long.parseLong(checker.getTime()), asTimeUnit(checker.getUnit()),
                Boolean.parseBoolean(checker.getAddToMicroProfileHealth()),
                Long.parseLong(checker.getThresholdPercentage()),
                Integer.parseInt(checker.getRetryCount()));
    }

    @Override
    public String getDescription() {
        return "healthcheck.description.hoggingThreads";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        HealthCheckResult result = new HealthCheckResult();
        if (!supported) {
            result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "JVM implementation or OS does" +
                    " not support getting CPU times"));
            return result;
        }
        acceptHoggingThreads(checkRecordsByThreadId,
                (percentage, threshold, totalTimeHogging, initialMethod, info) ->
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL,
                            "Thread with <id-name>: " + info.getThreadId() + "-" + info.getThreadName() +
                            " is a hogging thread for the last " +
                            prettyPrintDuration(totalTimeHogging) + "\n" + prettyPrintStackTrace(info.getStackTrace())))
                );
        return result;
    }

    private void acceptHoggingThreads(Map<Long, ThreadCpuTimeRecord> recordsById, HoggingThreadConsumer consumer) {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final long now = System.currentTimeMillis();
        final long currentThreadId = Thread.currentThread().getId();
        final int retryCount = options.getRetryCount();
        final int threshold = options.getThresholdPercentage().intValue();
        for (long threadId : bean.getAllThreadIds()) {
            if (threadId != currentThreadId) {
                final long cpuTimeInNanos = bean.getThreadCpuTime(threadId);
                if (cpuTimeInNanos == -1)
                    continue;
                long cpuTime = TimeUnit.NANOSECONDS.toMillis(cpuTimeInNanos);
                // from here all times are in millis
                ThreadCpuTimeRecord record = recordsById.get(threadId);
                if (record == null) {
                    record = new ThreadCpuTimeRecord();
                    recordsById.put(threadId, record);
                } else {
                    acceptHoggingThread(bean, now, retryCount, threshold, threadId, cpuTime, record, consumer);
                }
                record.startOfIntervalTimestamp = now;
                record.startOfIntervalCpuTime = cpuTime;
            }
        }
    }

    @Override
    protected EventLevel createNotificationEventLevel (HealthCheckResultStatus checkResult) {
        if (checkResult == HealthCheckResultStatus.FINE) {
            return EventLevel.INFO;
        }
        return EventLevel.WARNING;
    }

    private static void acceptHoggingThread(final ThreadMXBean bean, final long now, final int retryCount, final int threshold,
            long threadId, long cpuTime, ThreadCpuTimeRecord record, HoggingThreadConsumer consumer) {
        long intervalLength = now - record.startOfIntervalTimestamp;
        long intervalCpuTime = cpuTime - record.startOfIntervalCpuTime;
        if (intervalLength <= 0) {
            return;
        }
        int percentage = (int) (intervalCpuTime * 100L / intervalLength);
        if (percentage > threshold) {
            if (record.identifiedAsHoggingCount == 0) {
                record.startOfExceedingThresholdTimestamp = record.startOfIntervalTimestamp;
                record.identifiedAsHoggingMethod = getMethod(bean.getThreadInfo(threadId, 1));
            }
            record.identifiedAsHoggingCount++;
            if (record.identifiedAsHoggingCount > retryCount) {
                ThreadInfo info = bean.getThreadInfo(threadId, 1);
                long totalTimeHogging = now - record.startOfExceedingThresholdTimestamp;
                consumer.accept(percentage, threshold, totalTimeHogging, record.identifiedAsHoggingMethod, info);
            }
        } else {
            record.identifiedAsHoggingCount = 0;
        }
    }

    static String getMethod(ThreadInfo info) {
        if (info.getStackTrace().length == 0) {
            return "?";
        }
        StackTraceElement frame = info.getStackTrace()[0];
        return frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
    }
}
