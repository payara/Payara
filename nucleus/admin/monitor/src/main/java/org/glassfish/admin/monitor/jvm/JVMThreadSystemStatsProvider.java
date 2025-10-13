/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2018-2019] Payara Foundation and/or affiliates

package org.glassfish.admin.monitor.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Base class providing the MBean to monitor JVM thread system statistics
 * <p>
 * The MBean will of the format 
 * {@code amx:pp=/mon/server-mon[server],type=thread-system-mon,name=jvm/thread-system}
 * and can be enabled by turning the Jvm monitoring level in the admin console to LOW
 * @since v2
 */
@AMXMetadata(type="thread-system-mon", group="monitoring")
@ManagedObject
@Description( "JVM Thread System Statistics" )
public class JVMThreadSystemStatsProvider {
    
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    private volatile long nanotimeBefore = System.nanoTime();
    private volatile long totalCpuNanosBefore = 0;

    private final CountStatisticImpl cpuUsage = new CountStatisticImpl("CpuUsage", "%", "CPU usage in percent");

    private final StringStatisticImpl allThreadIds = new StringStatisticImpl(
            "LiveThreads", "String", "Returns all live thread IDs" );
    private final CountStatisticImpl currentThreadCpuTime = new CountStatisticImpl(
            "CurrentThreadCpuTime", StatisticImpl.UNIT_NANOSECOND,
                "Returns the total CPU time for the current thread in nanoseconds" );
    private final CountStatisticImpl currentThreadUserTime = new CountStatisticImpl(
            "CurrentThreadUserTime", StatisticImpl.UNIT_NANOSECOND,
                "Returns the CPU time that the current thread has executed in user mode in nanoseconds" );
    private final CountStatisticImpl daemonThreadCount = new CountStatisticImpl(
            "DaemonThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the current number of live daemon threads" );
    private final StringStatisticImpl deadlockedThreads = new StringStatisticImpl(
            "DeadlockedThreads", "String",
                "Finds cycles of threads that are in deadlock waiting to acquire object monitors or ownable synchronizers" );
    private final StringStatisticImpl monitorDeadlockedThreads = new StringStatisticImpl(
            "MonitorDeadlockedThreads", "String",
                "Finds cycles of threads that are in deadlock waiting to acquire object monitors" );
    private final CountStatisticImpl peakThreadCount = new CountStatisticImpl(
            "PeakThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the peak live thread count since the Java virtual machine started or peak was reset" );
    private final CountStatisticImpl threadCount = new CountStatisticImpl(
            "ThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the current number of live threads including both daemon and non-daemon threads" );
    private final CountStatisticImpl totalStartedThreadCount = new CountStatisticImpl(
            "TotalStartedThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the total number of threads created and also started since the Java virtual machine started" );

    /**
     * Returns all live thread IDs
     * @return a {@link StringStatistic} with a comma separated list of all live thread IDs
     */
    @ManagedAttribute(id="allthreadids")
    @Description( "Returns all live thread IDs" )
    public StringStatistic getAllThreadIds() {
        long[] ids = this.threadBean.getAllThreadIds();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (long id : ids) {
            if(first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(id);
        }
        this.allThreadIds.setCurrent(sb.toString());
        return allThreadIds;
    }

    /**
     * Returns the total CPU time for the current thread in nanoseconds
     * @return a {@link CountStatistic} with the time in nanoseconds
     */
    @ManagedAttribute(id="currentthreadcputime")
    @Description( "Returns the total CPU time for the current thread in nanoseconds" )
    public CountStatistic getCurrentThreadCpuTime() {
        this.currentThreadCpuTime.setCount(threadBean.getCurrentThreadCpuTime());
        return this.currentThreadCpuTime;
    }

    /**
     * Returns the CPU time that the current thread has executed in user mode in nanoseconds
     * @return a {@link CountStatistic} with the time in nanoseconds
     */
    @ManagedAttribute(id="currentthreadusertime")
    @Description( "Returns the CPU time that the current thread has executed in user mode in nanoseconds" )
    public CountStatistic getCurrentThreadUserTime() {
        this.currentThreadUserTime.setCount(threadBean.getCurrentThreadUserTime());
        return this.currentThreadUserTime;
    }

    /**
     * Returns the current number of live daemon threads
     * @return a {@link CountStatistic} with thenumber of threads
     */
    @ManagedAttribute(id="daemonthreadcount")
    @Description( "Returns the current number of live daemon threads" )
    public CountStatistic getDaemonThreadCount() {
        this.daemonThreadCount.setCount(threadBean.getDaemonThreadCount());
        return this.daemonThreadCount;
    }

    /**
     * Finds cycles of threads that are in deadlock waiting to acquire object monitors or ownable synchronizers
     * @return A {@link StringStatistic} with a comma separated list of deadlocked threads or the string "{@code None of the threads are deadlocked.}"
     * if there are no deadlocked threads
     */
    @ManagedAttribute(id="deadlockedthreads")
    @Description( "Finds cycles of threads that are in deadlock waiting to acquire object monitors or ownable synchronizers" )
    public StringStatistic getDeadlockedThreads() {
        long[] threads = threadBean.findDeadlockedThreads();
        if (threads == null) {
            this.deadlockedThreads.setCurrent("None of the threads are deadlocked.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (long thread : threads) {
                sb.append(thread);
                sb.append(',');
            }
            this.deadlockedThreads.setCurrent(sb.toString());
        }
        return deadlockedThreads;
    }

    /**
     * Finds cycles of threads that are in deadlock waiting to acquire object monitors
     * @return A {@link StringStatistic} with a comma separated list of deadlocked threads or the string
     * "{@code None of the threads are monitor deadlocked.}"
     * if there are no deadlocked threads
     */
    @ManagedAttribute(id="monitordeadlockedthreads")
    @Description( "Finds cycles of threads that are in deadlock waiting to acquire object monitors" )
    public StringStatistic getMonitorDeadlockedThreads() {
        long[] threads = threadBean.findMonitorDeadlockedThreads();
        if (threads == null) {
            this.monitorDeadlockedThreads.setCurrent("None of the threads are monitor deadlocked.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (long thread : threads) {
                sb.append(thread);
                sb.append(',');
            }
            this.monitorDeadlockedThreads.setCurrent(sb.toString());
        }
        return this.monitorDeadlockedThreads;
    }

    /**
     * Returns the peak live thread count since the Java virtual machine started or peak was reset
     * @return a {@link CountStatistic} with the highest number of threads
     */
    @ManagedAttribute(id="peakthreadcount")
    @Description( "Returns the peak live thread count since the Java virtual machine started or peak was reset" )
    public CountStatistic getPeakThreadCount() {
        this.peakThreadCount.setCount(threadBean.getPeakThreadCount());
        return this.peakThreadCount;
    }

    /**
     * Returns the current number of live threads including both daemon and non-daemon threads
     * @return A {@link CountStatistic} with the current number of threads
     */
    @ManagedAttribute(id="threadcount")
    @Description( "Returns the current number of live threads including both daemon and non-daemon threads" )
    public CountStatistic getThreadCount() {
        threadCount.setCount(threadBean.getThreadCount());
        return threadCount;
    }

    /**
     * Returns the total number of threads created and also started since the Java virtual machine started
     * @return a {@link CountStatistic} with the total number of threads
     */
    @ManagedAttribute(id="totalstartedthreadcount")
    @Description( "Returns the total number of threads created and also started since the Java virtual machine started" )
    public CountStatistic getTotalStartedThreadCount() {
        totalStartedThreadCount.setCount(threadBean.getTotalStartedThreadCount());
        return totalStartedThreadCount;
    }

    @ManagedAttribute(id="cpuusage")
    @Description( "Returns the CPU usage in percent." )
    public CountStatistic getCpuUsage() {
        long totalCpuNanos = 0L;
        for (long id : threadBean.getAllThreadIds()) {
            final long threadCpuTime = threadBean.getThreadCpuTime(id);
            if (threadCpuTime >= 0L) {
                totalCpuNanos += threadCpuTime;
            }
        }
        long nanotime = System.nanoTime();
        long timeDelta = nanotime - nanotimeBefore;
        long cpuTimeDelta = totalCpuNanos - totalCpuNanosBefore;
        long percentage = Math.max(0L, Math.min(100L, 100L * cpuTimeDelta / timeDelta));
        cpuUsage.setCount(percentage);
        nanotimeBefore = nanotime;
        totalCpuNanosBefore = totalCpuNanos;
        return cpuUsage;
    }
}
