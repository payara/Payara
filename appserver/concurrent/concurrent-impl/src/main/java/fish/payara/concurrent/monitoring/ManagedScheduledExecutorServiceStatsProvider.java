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
package fish.payara.concurrent.monitoring;

import org.glassfish.concurrent.config.ManagedScheduledExecutorService;
import org.glassfish.concurrent.runtime.ConcurrentRuntime;
import org.glassfish.concurrent.runtime.deployer.ManagedScheduledExecutorServiceConfig;
import org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Class that provides monitoring stats for the ManagedScheduledExecutorService
 * @author Andrew Pielage
 */
@AMXMetadata(type="managed-scheduled-executor-service-mon", group="monitoring", 
        isSingleton=false)
@ManagedObject
@Description("ManagedScheduledExecutorService Statistics")
public class ManagedScheduledExecutorServiceStatsProvider
{
    private final String name;
    private boolean registered = false;
    private final ManagedScheduledExecutorServiceImpl 
            managedScheduledExecutorServiceImpl;
    
    private CountStatisticImpl completedTaskCount = new CountStatisticImpl(
            "CompletedTaskCount", "count", 
            "Number of tasks completed");
    private CountStatisticImpl taskCount = new CountStatisticImpl(
            "TaskCount", "count",
            "Total number of tasks ever scheduled");
    
    private CountStatisticImpl activeCount = new CountStatisticImpl(
            "ActiveCount", "count",
            "The approximate number of active threads");
    
    private CountStatisticImpl largestPoolSize = new CountStatisticImpl(
            "LargestPoolSize", "count",
            "The largest number of threads that have ever simultaneously "
                    + "been in the pool.");
    
    private CountStatisticImpl poolSize = new CountStatisticImpl(
            "PoolSize", "count",
            "The current number of threads in the pool.");
    
    public ManagedScheduledExecutorServiceStatsProvider(
            ManagedScheduledExecutorService managedScheduledExecutorService) { 
        
        ManagedScheduledExecutorServiceConfig 
                managedScheduledExecutorServiceConfig = new 
                        ManagedScheduledExecutorServiceConfig(
                                managedScheduledExecutorService);  
        ConcurrentRuntime concurrentRuntime = ConcurrentRuntime.getRuntime();
        
        managedScheduledExecutorServiceImpl = concurrentRuntime.
                getManagedScheduledExecutorService(null, 
                        managedScheduledExecutorServiceConfig);
        name = this.managedScheduledExecutorServiceImpl.getName();     
    }
    
    public void register() {
        String node = ConcurrentMonitoringUtils.registerSingleComponent(
                name, this);
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
    
    @ManagedAttribute(id="CompletedTaskCount")
    @Description( "Number of tasks completed")
    public CountStatistic getCompletedTaskCount() {
        completedTaskCount.setCount(
                managedScheduledExecutorServiceImpl.getCompletedTaskCount());
        return completedTaskCount;
    }
    
    @ManagedAttribute(id="TaskCount")
    @Description( "Total number of tasks ever scheduled")
    public CountStatistic getTaskCount() {
        taskCount.setCount(managedScheduledExecutorServiceImpl.getTaskCount());
        return taskCount;
    }
    
    @ManagedAttribute(id="ActiveCount")
    @Description("The approximate number of active threads")
    public CountStatistic getActiveCount() {
        activeCount.setCount(managedScheduledExecutorServiceImpl.
                getActiveCount());
        return activeCount;
    }
    
    @ManagedAttribute(id="LargestPoolSize")
    @Description("The largest number of threads that have ever simultaneously "
                    + "been in the pool")
    public CountStatistic getLargestPoolSize() {
        largestPoolSize.setCount(managedScheduledExecutorServiceImpl.
                getLargestPoolSize());
        return largestPoolSize;
    }
    
    @ManagedAttribute(id="PoolSize")
    @Description("The current number of threads in the pool")
    public CountStatistic getPoolSize() {
        poolSize.setCount(managedScheduledExecutorServiceImpl.getPoolSize());
        return poolSize;
    }
}
