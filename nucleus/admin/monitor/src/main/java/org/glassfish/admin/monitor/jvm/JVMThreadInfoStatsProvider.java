/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.management.ThreadInfo;
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

/* server.jvm.thread-system.thread-1 */
// v2 mbean: com.sun.appserv:name=thread-1,type=threadinfo,category=monitor,server=server
// v3 mbean:
@AMXMetadata(type="threadinfo-mon", group="monitoring")
@ManagedObject
@Description( "JVM Thread Info Statistics" )
public class JVMThreadInfoStatsProvider {
    
    private ThreadInfo threadInfo;
    
    private CountStatisticImpl blockedCount = new CountStatisticImpl(
            "BlockedCount", StatisticImpl.UNIT_COUNT,
            "Returns the total number of times that the thread associated with this ThreadInfo blocked to enter or reenter a monitor" );
    private CountStatisticImpl blockedTime = new CountStatisticImpl(
            "BlockedTime", StatisticImpl.UNIT_MILLISECOND,
                "Returns the approximate accumulated elapsed time (in milliseconds) that the thread associated with this ThreadInfo has blocked to enter or reenter a monitor since thread contention monitoring is enabled" );
    private StringStatisticImpl lockName = new StringStatisticImpl(
            "LockName", "String",
                "Returns the string representation of an object for which the thread associated with this ThreadInfo is blocked waiting" );
    private CountStatisticImpl lockOwnerId = new CountStatisticImpl(
            "LockOwnerId", "String",
                "Returns the ID of the thread which owns the object for which the thread associated with this ThreadInfo is blocked waiting" );
    private StringStatisticImpl lockOwnerName = new StringStatisticImpl(
            "LockOwnerName", "String",
                "Returns the name of the thread which owns the object for which the thread associated with this ThreadInfo is blocked waiting" );
    private StringStatisticImpl stackTrace = new StringStatisticImpl(
            "StackTrace", "String",
                "Returns the stack trace of the thread associated with this ThreadInfo" );
    private CountStatisticImpl threadId = new CountStatisticImpl(
            "ThreadId", "String",
                "Returns the ID of the thread associated with this ThreadInfo" );
    private StringStatisticImpl threadName = new StringStatisticImpl(
            "ThreadName", "String",
                "Returns the name of the thread associated with this ThreadInfo" );
    private StringStatisticImpl threadState = new StringStatisticImpl(
            "ThreadState", "String",
                "Returns the state of the thread associated with this ThreadInfo" );
    private CountStatisticImpl waitedCount = new CountStatisticImpl(
            "WaitingCount", StatisticImpl.UNIT_COUNT,
                "Returns the total number of times that the thread associated with this ThreadInfo waited for notification" );
    private CountStatisticImpl waitedTime = new CountStatisticImpl(
            "WaitingTime", StatisticImpl.UNIT_MILLISECOND,
                "Returns the approximate accumulated elapsed time (in milliseconds) that the thread associated with this ThreadInfo has waited for notification since thread contention monitoring is enabled" );

    public JVMThreadInfoStatsProvider(ThreadInfo info) {
        this.threadInfo = info;
    }

    @ManagedAttribute(id="blockedcount")
    @Description( "Returns the total number of times that the thread associated with this ThreadInfo blocked to enter or reenter a monitor" )
    public CountStatistic getBlockedCount() {
        blockedCount.setCount(threadInfo.getBlockedCount());
        return blockedCount;
    }

    @ManagedAttribute(id="blockedtime")
    @Description( "Returns the approximate accumulated elapsed time (in milliseconds) that the thread associated with this ThreadInfo has blocked to enter or reenter a monitor since thread contention monitoring is enabled" )
    public CountStatistic getBlockedTime() {
        blockedTime.setCount(threadInfo.getBlockedTime());
        return blockedTime;
    }

    @ManagedAttribute(id="lockname")
    @Description( "Returns the string representation of an object for which the thread associated with this ThreadInfo is blocked waiting" )
    public StringStatistic getLockName() {
        String name = threadInfo.getLockName();
        if (name == null) {
            lockName.setCurrent("Thread is not waiting on monitor lock.");
        } else {
            lockName.setCurrent(name);
        }
        return lockName;
    }

    @ManagedAttribute(id="lockownerid")
    @Description( "Returns the ID of the thread which owns the object for which the thread associated with this ThreadInfo is blocked waiting" )
    public CountStatistic getLockOwnerId() {
        lockOwnerId.setCount(threadInfo.getLockOwnerId());
        return lockOwnerId;
    }

    @ManagedAttribute(id="lockownername")
    @Description( "Returns the name of the thread which owns the object for which the thread associated with this ThreadInfo is blocked waiting" )
    public StringStatistic getLockOwnerName() {
        String name = threadInfo.getLockOwnerName();
        if (name == null) {
            lockOwnerName.setCurrent("None of the other threads is holding any monitors of this thread.");
        } else {
            lockOwnerName.setCurrent(name);
        }
        return lockOwnerName;
    }

    @ManagedAttribute(id="stacktrace")
    @Description( "Returns the stack trace of the thread associated with this ThreadInfo" )
    public StringStatistic getStackTrace() {
        StackTraceElement[] elements = threadInfo.getStackTrace();
        StringBuffer sb = new StringBuffer();
        for (StackTraceElement ste : elements) {
            sb.append(ste.toString());
            sb.append(',');
        }
        stackTrace.setCurrent(sb.toString());
        return stackTrace;
    }

    @ManagedAttribute(id="threadid")
    @Description( "Returns the ID of the thread associated with this ThreadInfo" )
    public CountStatistic getThreadId() {
        threadId.setCount(threadInfo.getThreadId());
        return threadId;
    }

    @ManagedAttribute(id="threadname")
    @Description( "Returns the name of the thread associated with this ThreadInfo" )
    public StringStatistic getThreadName() {
        threadName.setCurrent(threadInfo.getThreadName());
        return threadName;
    }

    @ManagedAttribute(id="threadstate")
    @Description( "Returns the state of the thread associated with this ThreadInfo" )
    public StringStatistic getThreadState() {
        threadState.setCurrent(threadInfo.getThreadState().toString());
        return threadState;
    }

    @ManagedAttribute(id="waitedcount")
    @Description( "Returns the total number of times that the thread associated with this ThreadInfo waited for notification" )
    public CountStatistic getWaitedCount() {
        waitedCount.setCount(threadInfo.getWaitedCount());
        return waitedCount;
    }

    @ManagedAttribute(id="waitedtime")
    @Description( "Returns the approximate accumulated elapsed time (in milliseconds) that the thread associated with this ThreadInfo has waited for notification since thread contention monitoring is enabled" )
    public CountStatistic getWaitedTime() {
        waitedTime.setCount(threadInfo.getWaitedTime());
        return waitedTime;
    }
}
