/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package com.sun.ejb.monitoring.stats;

import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Class that provides monitoring stats for the ThreadPoolExecutor
 * @author Andrew Pielage
 */
@AMXMetadata(type="exec-pool-mon", group="monitoring", isSingleton=false)
@ManagedObject
@Description("ThreadPoolExecutor Statistics")
public class EjbThreadPoolExecutorStatsProvider
{
    
    private final ThreadPoolExecutor threadPoolExecutor;
    private final String poolName;
    private final EjbContainerUtil ejbContainerUtilImpl;
    private boolean registered = false;
    
    private CountStatisticImpl NumActiveThreads = new CountStatisticImpl(
            "ActiveNumThreads", "count", 
            "Number of active threads in the associated pool");
    private CountStatisticImpl NumTasksCompleted = new CountStatisticImpl(
            "NumTasksCompleted", "count", 
            "Number of tasks completed in the associated pool");
    private CountStatisticImpl corePoolSize = new CountStatisticImpl(
            "CoreNumThreads", "count", 
            "Core number of threads in the associated pool");
    private CountStatisticImpl keepAliveTime = new CountStatisticImpl(
            "KeepAliveTime", "Milliseconds", 
            "Keep-Alive time for threads in the associated pool");
    private CountStatisticImpl largestPoolSize = new CountStatisticImpl(
            "LargestNumThreads", "count", 
            "Largest number of simultaneous threads in the associated pool");
    private CountStatisticImpl maxPoolSize = new CountStatisticImpl(
            "MaxNumThreads", "count", 
            "Maximum number of threads in the associated pool");
    private CountStatisticImpl poolSize = new CountStatisticImpl(
            "NumThreads", "count", 
            "Current number of threads in the associated pool");
    private CountStatisticImpl tasksCreated = new CountStatisticImpl(
            "TotalTasksCreated", "count", 
            "Number of tasks created in the associated pool");
    
    public EjbThreadPoolExecutorStatsProvider(String poolName) {      
        if (poolName != null) {
            this.poolName = poolName;
        } else {
            this.poolName = "default-exec-pool";
        }
        
        ejbContainerUtilImpl = EjbContainerUtilImpl.getInstance();
        threadPoolExecutor = ejbContainerUtilImpl.getThreadPoolExecutor(
                poolName);
    }
    
    public void register() {
        String node = EjbMonitoringUtils.registerSingleComponent(poolName, 
                this);
        if (node != null) {
            registered = true;
        }
    }

    public void unregister() {
        if (registered) {
            registered = false;
            StatsProviderManager.unregister(this);
        }
    }
    
    @ManagedAttribute(id="activenumthreads")
    @Description( "Number of active threads in the associated pool")
    public CountStatistic getActiveThreads() {
        NumActiveThreads.setCount(threadPoolExecutor.getActiveCount());
        return NumActiveThreads;
    }
    
    @ManagedAttribute(id="numtaskscompleted")
    @Description( "Number of tasks completed in the associated pool")
    public CountStatistic getTasksCompleted() {
        NumTasksCompleted.setCount(threadPoolExecutor.getCompletedTaskCount());
        return NumTasksCompleted;
    }
    
    @ManagedAttribute(id="corenumthreads")
    @Description( "Core number of threads in the associated pool")
    public CountStatistic getCorePoolSize() {
        corePoolSize.setCount(threadPoolExecutor.getCorePoolSize());
        return corePoolSize;
    }
    
    @ManagedAttribute(id="keepalivetime")
    @Description( "Keep-Alive time for threads in the associated pool")
    public CountStatistic getKeepAlive() {
        keepAliveTime.setCount(threadPoolExecutor.getKeepAliveTime(
                TimeUnit.MILLISECONDS));
        return keepAliveTime;
    }
    
    @ManagedAttribute(id="largestnumthreads")
    @Description( "Largest number of simultaneous threads in the associated "
            + "pool")
    public CountStatistic getLargestPoolSize() {
        largestPoolSize.setCount(threadPoolExecutor.getLargestPoolSize());
        return largestPoolSize;
    }
    
    @ManagedAttribute(id="maxnumthreads")
    @Description( "Maximum number of threads in the associated pool")
    public CountStatistic getMaxPoolSize() {
        maxPoolSize.setCount(threadPoolExecutor.getMaximumPoolSize());
        return maxPoolSize;
    }
    
    @ManagedAttribute(id="numthreads")
    @Description( "Current number of threads in the associated pool")
    public CountStatistic getPoolSize() {
        poolSize.setCount(threadPoolExecutor.getPoolSize());
        return poolSize;
    }
    
    @ManagedAttribute(id="totaltaskscreated")
    @Description( "Number of tasks created in the associated pool")
    public CountStatistic getTasksCreated() {
        tasksCreated.setCount(threadPoolExecutor.getTaskCount());
        return tasksCreated;
    }
}
