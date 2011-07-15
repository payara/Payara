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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

/* server.jvm.thread-system */
// v2 mbean: com.sun.appserv:name=thread-system,type=thread-system,category=monitor,server=server
// v3 mbean:
@AMXMetadata(type="thread-system-mon", group="monitoring")
@ManagedObject
@Description( "JVM Thread System Statistics" )
public class JVMThreadSystemStatsProvider {
    
    private ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    private StringStatisticImpl allThreadIds = new StringStatisticImpl(
            "LiveThreads", "String", "Returns all live thread IDs" );
    private CountStatisticImpl currentThreadCpuTime = new CountStatisticImpl(
            "CurrentThreadCpuTime", StatisticImpl.UNIT_NANOSECOND,
                "Returns the total CPU time for the current thread in nanoseconds" );
    private CountStatisticImpl currentThreadUserTime = new CountStatisticImpl(
            "CurrentThreadUserTime", StatisticImpl.UNIT_NANOSECOND,
                "Returns the CPU time that the current thread has executed in user mode in nanoseconds" );
    private CountStatisticImpl daemonThreadCount = new CountStatisticImpl(
            "DaemonThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the current number of live daemon threads" );
    private StringStatisticImpl deadlockedThreads = new StringStatisticImpl(
            "DeadlockedThreads", "String",
                "Finds cycles of threads that are in deadlock waiting to acquire object monitors or ownable synchronizers" );
    private StringStatisticImpl monitorDeadlockedThreads = new StringStatisticImpl(
            "MonitorDeadlockedThreads", "String",
                "Finds cycles of threads that are in deadlock waiting to acquire object monitors" );
    private CountStatisticImpl peakThreadCount = new CountStatisticImpl(
            "PeakThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the peak live thread count since the Java virtual machine started or peak was reset" );
    private CountStatisticImpl threadCount = new CountStatisticImpl(
            "ThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the current number of live threads including both daemon and non-daemon threads" );
    private CountStatisticImpl totalStartedThreadCount = new CountStatisticImpl(
            "TotalStartedThreadCount", StatisticImpl.UNIT_COUNT,
                "Returns the total number of threads created and also started since the Java virtual machine started" );

    @ManagedAttribute(id="allthreadids")
    @Description( "Returns all live thread IDs" )
    public StringStatistic getAllThreadIds() {
        long[] ids = this.threadBean.getAllThreadIds();
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (long id : ids) {
            if(first)
                first = false;
            else
                sb.append(',');

            sb.append(id);
        }
        this.allThreadIds.setCurrent(sb.toString());
        return allThreadIds;
    }

    @ManagedAttribute(id="currentthreadcputime")
    @Description( "Returns the total CPU time for the current thread in nanoseconds" )
    public CountStatistic getCurrentThreadCpuTime() {
        this.currentThreadCpuTime.setCount(threadBean.getCurrentThreadCpuTime());
        return this.currentThreadCpuTime;
    }

    @ManagedAttribute(id="currentthreadusertime")
    @Description( "Returns the CPU time that the current thread has executed in user mode in nanoseconds" )
    public CountStatistic getCurrentThreadUserTime() {
        this.currentThreadUserTime.setCount(threadBean.getCurrentThreadUserTime());
        return this.currentThreadUserTime;
    }

    @ManagedAttribute(id="daemonthreadcount")
    @Description( "Returns the current number of live daemon threads" )
    public CountStatistic getDaemonThreadCount() {
        this.daemonThreadCount.setCount(threadBean.getDaemonThreadCount());
        return this.daemonThreadCount;
    }

    @ManagedAttribute(id="deadlockedthreads")
    @Description( "Finds cycles of threads that are in deadlock waiting to acquire object monitors or ownable synchronizers" )
    public StringStatistic getDeadlockedThreads() {
        long[] threads = threadBean.findDeadlockedThreads();
        if (threads == null) {
            this.deadlockedThreads.setCurrent("None of the threads are deadlocked.");
        } else {
            StringBuffer sb = new StringBuffer();
            for (long thread : threads) {
                sb.append(thread);
                sb.append(',');
            }
            this.deadlockedThreads.setCurrent(sb.toString());
        }
        return deadlockedThreads;
    }

    @ManagedAttribute(id="monitordeadlockedthreads")
    @Description( "Finds cycles of threads that are in deadlock waiting to acquire object monitors" )
    public StringStatistic getMonitorDeadlockedThreads() {
        long[] threads = threadBean.findMonitorDeadlockedThreads();
        if (threads == null) {
            this.monitorDeadlockedThreads.setCurrent("None of the threads are monitor deadlocked.");
        } else {
            StringBuffer sb = new StringBuffer();
            for (long thread : threads) {
                sb.append(thread);
                sb.append(',');
            }
            this.monitorDeadlockedThreads.setCurrent(sb.toString());
        }
        return this.monitorDeadlockedThreads;
    }

    @ManagedAttribute(id="peakthreadcount")
    @Description( "Returns the peak live thread count since the Java virtual machine started or peak was reset" )
    public CountStatistic getPeakThreadCount() {
        this.peakThreadCount.setCount(threadBean.getPeakThreadCount());
        return this.peakThreadCount;
    }

    @ManagedAttribute(id="threadcount")
    @Description( "Returns the current number of live threads including both daemon and non-daemon threads" )
    public CountStatistic getThreadCount() {
        threadCount.setCount(threadBean.getThreadCount());
        return threadCount;
    }

    @ManagedAttribute(id="totalstartedthreadcount")
    @Description( "Returns the total number of threads created and also started since the Java virtual machine started" )
    public CountStatistic getTotalStartedThreadCount() {
        totalStartedThreadCount.setCount(threadBean.getTotalStartedThreadCount());
        return totalStartedThreadCount;
    }
}
