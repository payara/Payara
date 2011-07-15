/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.services.impl.monitor.stats;

import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.annotations.Reset;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Thread Pool statistics
 * 
 * @author Alexey Stashok
 */
@AMXMetadata(type = "thread-pool-mon", group = "monitoring")
@ManagedObject
@Description("Thread Pool Statistics")
public class ThreadPoolStatsProvider implements StatsProvider {

    private final String name;
    protected final CountStatisticImpl maxThreadsCount = new CountStatisticImpl("MaxThreads", "count", "Maximum number of threads allowed in the thread pool");
    protected final CountStatisticImpl coreThreadsCount = new CountStatisticImpl("CoreThreads", "count", "Core number of threads in the thread pool");
    
    protected final CountStatisticImpl totalExecutedTasksCount = new CountStatisticImpl("TotalExecutedTasksCount", "count", "Provides the total number of tasks, which were executed by the thread pool");
    protected final CountStatisticImpl currentThreadCount = new CountStatisticImpl("CurrentThreadCount", "count", "Provides the number of request processing threads currently in the listener thread pool");
    protected final CountStatisticImpl currentThreadsBusy = new CountStatisticImpl("CurrentThreadsBusy", "count", "Provides the number of request processing threads currently in use in the listener thread pool serving requests");

    protected volatile ThreadPoolConfig threadPoolConfig;

    public ThreadPoolStatsProvider(String name) {
        this.name = name;
    }

    @Override
    public Object getStatsObject() {
        return threadPoolConfig;
    }

    @Override
    public void setStatsObject(Object object) {
        if (object instanceof ThreadPoolConfig) {
            threadPoolConfig = (ThreadPoolConfig) object;
        } else {
            threadPoolConfig = null;
        }
    }

    @ManagedAttribute(id = "maxthreads")
    @Description("Maximum number of threads allowed in the thread pool")
    public CountStatistic getMaxThreadsCount() {
        return maxThreadsCount;
    }

    @ManagedAttribute(id = "corethreads")
    @Description("Core number of threads in the thread pool")
    public CountStatistic getCoreThreadsCount() {
        return coreThreadsCount;
    }

    @ManagedAttribute(id = "totalexecutedtasks")
    @Description("Provides the total number of tasks, which were executed by the thread pool")
    public CountStatistic getTotalExecutedTasksCount() {
        return totalExecutedTasksCount;
    }

    @ManagedAttribute(id = "currentthreadcount")
    @Description("Provides the number of request processing threads currently in the listener thread pool")
    public CountStatistic getCurrentThreadCount() {
        return currentThreadCount;
    }

    @ManagedAttribute(id = "currentthreadsbusy")
    @Description("Provides the number of request processing threads currently in use in the listener thread pool serving requests.")
    public CountStatistic getCurrentThreadsBusy() {
        return currentThreadsBusy;
    }

    @ProbeListener("glassfish:kernel:thread-pool:setMaxThreadsEvent")
    public void setMaxThreadsEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadPoolName") String threadPoolName,
            @ProbeParam("maxNumberOfThreads") int maxNumberOfThreads) {

        if (name.equals(monitoringId)) {
            maxThreadsCount.setCount(maxNumberOfThreads);
        }
    }

    @ProbeListener("glassfish:kernel:thread-pool:setCoreThreadsEvent")
    public void setCoreThreadsEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadPoolName") String threadPoolName,
            @ProbeParam("coreNumberOfThreads") int coreNumberOfThreads) {

        if (name.equals(monitoringId)) {
            coreThreadsCount.setCount(coreNumberOfThreads);
        }
    }

    @ProbeListener("glassfish:kernel:thread-pool:threadAllocatedEvent")
    public void threadAllocatedEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadPoolName") String threadPoolName,
            @ProbeParam("threadId") long threadId) {

        if (name.equals(monitoringId)) {
            currentThreadCount.increment();
        }
    }

    @ProbeListener("glassfish:kernel:thread-pool:threadReleasedEvent")
    public void threadReleasedEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadPoolName") String threadPoolName,
            @ProbeParam("threadId") long threadId) {

        if (name.equals(monitoringId)) {
            currentThreadCount.decrement();
        }
    }

    @ProbeListener("glassfish:kernel:thread-pool:threadDispatchedFromPoolEvent")
    public void threadDispatchedFromPoolEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadPoolName") String threadPoolName,
            @ProbeParam("threadId") long threadId) {

        if (name.equals(monitoringId)) {
            currentThreadsBusy.increment();
        }
    }

    @ProbeListener("glassfish:kernel:thread-pool:threadReturnedToPoolEvent")
    public void threadReturnedToPoolEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadPoolName") String threadPoolName,
            @ProbeParam("threadId") long threadId) {

        if (name.equals(monitoringId)) {
            totalExecutedTasksCount.increment();
            currentThreadsBusy.decrement();
        }
    }

    @Reset
    public void reset() {
        if (threadPoolConfig != null) {
            maxThreadsCount.setCount(threadPoolConfig.getMaxPoolSize());
            coreThreadsCount.setCount(threadPoolConfig.getCorePoolSize());
            currentThreadCount.setCount(0);
            currentThreadsBusy.setCount(0);
        }

        totalExecutedTasksCount.setCount(0);
    }
}
