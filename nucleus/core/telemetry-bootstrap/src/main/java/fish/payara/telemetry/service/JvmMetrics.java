/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.telemetry.service;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.OperatingSystemMXBean;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BatchCallback;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

class JvmMetrics implements AutoCloseable {

    private final List<MemoryPoolMXBean> memoryPoolBeans;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final List<BufferPoolMXBean> bufferPoolBeans;
    private final ThreadMXBean threadBean;
    private final ClassLoadingMXBean classLoadingBean;
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final BatchCallback batchCallback;
    private final DoubleHistogram gcHistogram;
    private final List<GcListenerEntry> gcListeners;

    JvmMetrics(Meter meter) {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.bufferPoolBeans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        java.lang.management.OperatingSystemMXBean osBeanRaw = ManagementFactory.getOperatingSystemMXBean();
        this.osBean = (osBeanRaw instanceof OperatingSystemMXBean)
                ? (OperatingSystemMXBean) osBeanRaw
                : null;

        ObservableLongMeasurement usedMemory = meter.upDownCounterBuilder("jvm.memory.used")
                .setDescription("Measure of memory used.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement committedMemory = meter.upDownCounterBuilder("jvm.memory.committed")
                .setDescription("Measure of memory committed.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement limitMemory = meter.upDownCounterBuilder("jvm.memory.limit")
                .setDescription("Measure of max obtainable memory.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement initMemory = meter.upDownCounterBuilder("jvm.memory.init")
                .setDescription("Measure of initial memory requested.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement usedMemoryTotal = meter.upDownCounterBuilder("jvm.memory.total.used")
                .setDescription("Measure of total memory used.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement committedMemoryTotal = meter.upDownCounterBuilder("jvm.memory.total.committed")
                .setDescription("Measure of total memory committed.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement limitMemoryTotal = meter.upDownCounterBuilder("jvm.memory.total.limit")
                .setDescription("Measure of total max memory that can be used.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement initMemoryTotal = meter.upDownCounterBuilder("jvm.memory.total.init")
                .setDescription("Measure of total initial memory requested.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement usedAfterLastGc = meter.upDownCounterBuilder("jvm.memory.used_after_last_gc")
                .setDescription("Measure of memory used, as measured after the most recent garbage collection event on this pool.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement threadCount = meter.upDownCounterBuilder("jvm.thread.count")
                .setDescription("Number of executing platform threads.")
                .setUnit("{thread}")
                .buildObserver();

        ObservableLongMeasurement classLoaded = meter.counterBuilder("jvm.class.loaded")
                .setDescription("Number of classes loaded since JVM start.")
                .setUnit("{class}")
                .buildObserver();

        ObservableLongMeasurement classUnloaded = meter.counterBuilder("jvm.class.unloaded")
                .setDescription("Number of classes unloaded since JVM start.")
                .setUnit("{class}")
                .buildObserver();

        ObservableLongMeasurement classCount = meter.upDownCounterBuilder("jvm.class.count")
                .setDescription("Number of classes currently loaded.")
                .setUnit("{class}")
                .buildObserver();

        // DOUBLE_SUM with unit=s — TCK requires this exact type and unit
        ObservableDoubleMeasurement cpuTime = meter.counterBuilder("jvm.cpu.time")
                .ofDoubles()
                .setDescription("CPU time used by the process as reported by the JVM.")
                .setUnit("s")
                .buildObserver();

        ObservableLongMeasurement cpuCount = meter.upDownCounterBuilder("jvm.cpu.count")
                .setDescription("Number of processors available to the Java virtual machine.")
                .setUnit("{cpu}")
                .buildObserver();

        ObservableDoubleMeasurement cpuUtilization = meter.gaugeBuilder("jvm.cpu.recent_utilization")
                .setDescription("Recent CPU utilization for the process as reported by the JVM.")
                .setUnit("1")
                .buildObserver();

        ObservableLongMeasurement bufferMemoryLimit = meter.upDownCounterBuilder("jvm.buffer.memory.limit")
                .setDescription("Measure of total memory capacity of buffers.")
                .setUnit("By")
                .buildObserver();

        ObservableLongMeasurement bufferCount = meter.upDownCounterBuilder("jvm.buffer.count")
                .setDescription("Number of buffers in the pool.")
                .setUnit("{buffer}")
                .buildObserver();

        this.batchCallback = meter.batchCallback(() -> {
                    for (MemoryPoolMXBean pool : memoryPoolBeans) {
                        Attributes attrs = Attributes.of(
                                stringKey("jvm.memory.type"), pool.getType() == MemoryType.HEAP ? "heap" : "non_heap",
                                stringKey("jvm.memory.pool.name"), pool.getName()
                        );
                        recordUsage(pool.getUsage(), attrs, usedMemory, committedMemory, initMemory, limitMemory);
                        MemoryUsage collectionUsage = pool.getCollectionUsage();
                        if (collectionUsage != null) {
                            usedAfterLastGc.record(collectionUsage.getUsed(), attrs);
                        }
                    }

                    recordUsage(memoryBean.getHeapMemoryUsage(), Attributes.of(stringKey("jvm.memory.type"), "heap"),
                            usedMemoryTotal, committedMemoryTotal, initMemoryTotal, limitMemoryTotal);
                    recordUsage(memoryBean.getNonHeapMemoryUsage(), Attributes.of(stringKey("jvm.memory.type"), "non_heap"),
                            usedMemoryTotal, committedMemoryTotal, initMemoryTotal, limitMemoryTotal);

                    recordThreads(threadCount);

                    classLoaded.record(classLoadingBean.getTotalLoadedClassCount());
                    classUnloaded.record(classLoadingBean.getUnloadedClassCount());
                    classCount.record((long) classLoadingBean.getLoadedClassCount());

                    cpuCount.record((long) Runtime.getRuntime().availableProcessors());
                    if (osBean != null) {
                        long processCpuTimeNs = osBean.getProcessCpuTime();
                        if (processCpuTimeNs != -1) {
                            // MXBean returns nanoseconds; TCK expects seconds as DOUBLE_SUM
                            cpuTime.record(processCpuTimeNs / 1_000_000_000.0);
                        }
                        double processCpuLoad = osBean.getProcessCpuLoad();
                        if (processCpuLoad >= 0) {
                            cpuUtilization.record(processCpuLoad);
                        }
                    }

                    for (BufferPoolMXBean bufferPool : bufferPoolBeans) {
                        Attributes attrs = Attributes.of(stringKey("jvm.buffer.pool.name"), bufferPool.getName());
                        bufferMemoryLimit.record(bufferPool.getTotalCapacity(), attrs);
                        bufferCount.record(bufferPool.getCount(), attrs);
                    }
                }, usedMemory, committedMemory, limitMemory, initMemory,
                usedMemoryTotal, committedMemoryTotal, limitMemoryTotal, initMemoryTotal,
                usedAfterLastGc, threadCount, classLoaded, classUnloaded, classCount,
                cpuTime, cpuCount, cpuUtilization, bufferMemoryLimit, bufferCount);

        // jvm.gc.duration must be a HISTOGRAM — push instrument driven by GC events,
        // cannot be an observable in batchCallback
        this.gcHistogram = meter.histogramBuilder("jvm.gc.duration")
                .setDescription("Duration of JVM garbage collection actions.")
                .setUnit("s")
                .build();

        this.gcListeners = new ArrayList<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean instanceof NotificationEmitter) {
                NotificationEmitter emitter = (NotificationEmitter) gcBean;
                NotificationListener listener = (notification, handback) -> {
                    if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo
                                .from((CompositeData) notification.getUserData());
                        // GcInfo.getDuration() is in milliseconds; TCK expects seconds
                        double durationSeconds = info.getGcInfo().getDuration() / 1000.0;
                        gcHistogram.record(durationSeconds,
                                Attributes.of(stringKey("jvm.gc.name"), info.getGcName()));
                    }
                };
                emitter.addNotificationListener(listener, null, null);
                gcListeners.add(new GcListenerEntry(emitter, listener));
            }
        }
    }

    private void recordUsage(MemoryUsage usage, Attributes attrs,
                             ObservableLongMeasurement used, ObservableLongMeasurement committed,
                             ObservableLongMeasurement init, ObservableLongMeasurement limit) {
        if (usage == null) return;
        used.record(usage.getUsed(), attrs);
        committed.record(usage.getCommitted(), attrs);
        long initial = usage.getInit();
        if (initial != -1) {
            init.record(initial, attrs);
        }
        long max = usage.getMax();
        if (max != -1) {
            limit.record(max, attrs);
        }
    }

    private void recordThreads(ObservableLongMeasurement threadCount) {
        long[] ids = threadBean.getAllThreadIds();
        ThreadInfo[] infos = threadBean.getThreadInfo(ids);
        Map<ThreadKey, Integer> counts = new HashMap<>();
        for (ThreadInfo info : infos) {
            if (info == null) continue;
            boolean isDaemon = isDaemonThread(info.getThreadId());
            ThreadKey key = new ThreadKey(info.getThreadState().name().toLowerCase(), isDaemon);
            counts.merge(key, 1, Integer::sum);
        }
        for (Map.Entry<ThreadKey, Integer> entry : counts.entrySet()) {
            threadCount.record(entry.getValue(), Attributes.of(
                    stringKey("jvm.thread.state"), entry.getKey().state,
                    booleanKey("jvm.thread.daemon"), entry.getKey().daemon
            ));
        }
    }

    private boolean isDaemonThread(long threadId) {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            if (t != null && t.getId() == threadId) {
                return t.isDaemon();
            }
        }
        return false;
    }

    @Override
    public void close() {
        if (batchCallback != null) {
            batchCallback.close();
        }
        for (GcListenerEntry entry : gcListeners) {
            try {
                entry.emitter.removeNotificationListener(entry.listener);
            } catch (Exception ignored) {
            }
        }
    }

    private static class GcListenerEntry {
        final NotificationEmitter emitter;
        final NotificationListener listener;

        GcListenerEntry(NotificationEmitter emitter, NotificationListener listener) {
            this.emitter = emitter;
            this.listener = listener;
        }
    }

    private static class ThreadKey {
        final String state;
        final boolean daemon;

        ThreadKey(String state, boolean daemon) {
            this.state = state;
            this.daemon = daemon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThreadKey threadKey = (ThreadKey) o;
            return daemon == threadKey.daemon && state.equals(threadKey.state);
        }

        @Override
        public int hashCode() {
            int result = state.hashCode();
            result = 31 * result + (daemon ? 1 : 0);
            return result;
        }
    }
}
