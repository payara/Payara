/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.util;

import com.sun.corba.ee.spi.threadpool.NoSuchWorkQueueException;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.BoundedRangeStatistic;
import org.glassfish.external.statistics.RangeStatistic;

import org.glassfish.external.statistics.impl.BoundedRangeStatisticImpl;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import com.sun.corba.ee.spi.threadpool.ThreadPool;
import com.sun.corba.ee.spi.threadpool.WorkQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.Description;

/**
 * This is the implementation for the ThreadPoolStats
 * and provides the implementation required to get the statistics
 * for a threadpool
 *
 * @author Pramod Gopinath
 */
@ManagedObject
@Description("The implementation for the ThreadPoolStats")
public class ThreadPoolStatsImpl
        extends ORBCommonStatsImpl
        implements ThreadPoolStats {

    private ThreadPool threadPool;
    private WorkQueue workQueue;
    private String workQueueName;
    private CountStatisticImpl numberOfBusyThreads;
    private CountStatisticImpl numberOfAvailableThreads;
    private BoundedRangeStatisticImpl currentNumberOfThreads;
    private BoundedRangeStatisticImpl averageWorkCompletionTime;
    private CountStatisticImpl totalWorkItemsAdded;
    private BoundedRangeStatisticImpl numberOfWorkItemsInQueue;
    private BoundedRangeStatisticImpl averageTimeInQueue;
    private static final String stringNumberOfBusyThreads =
            MonitoringConstants.THREADPOOL_NUMBER_OF_BUSY_THREADS;
    private static final String stringNumberOfAvailableThreads =
            MonitoringConstants.THREADPOOL_NUMBER_OF_AVAILABLE_THREADS;
    private static final String stringCurrentNumberOfThreads =
            MonitoringConstants.THREADPOOL_CURRENT_NUMBER_OF_THREADS;
    private static final String stringAverageWorkCompletionTime =
            MonitoringConstants.THREADPOOL_AVERAGE_WORK_COMPLETION_TIME;
    private static final String stringTotalWorkItemsAdded =
            MonitoringConstants.WORKQUEUE_TOTAL_WORK_ITEMS_ADDED;
    private static final String stringNumberOfWorkItemsInQueue =
            MonitoringConstants.WORKQUEUE_WORK_ITEMS_IN_QUEUE;
    private static final String stringAverageTimeInQueue =
            MonitoringConstants.WORKQUEUE_AVERAGE_TIME_IN_QUEUE;

    public ThreadPoolStatsImpl(ThreadPool threadPool) throws NoSuchWorkQueueException {
        this.threadPool = threadPool;

        getWorkQueueForThreadPool();

        initializeStats();
    }

    private void getWorkQueueForThreadPool() {
        try {
            workQueue = threadPool.getWorkQueue(0);
            workQueueName = workQueue.getName();
        } catch (NoSuchWorkQueueException ex) {

            Logger.getLogger(workQueueName).log(Level.SEVERE, workQueueName);
            throw new RuntimeException(ex);

        }
    }

    private void initializeStats() throws NoSuchWorkQueueException {
        super.initialize("org.glassfish.enterprise.iiop.util.ThreadPoolStats");

        final long time = System.currentTimeMillis();

        numberOfBusyThreads =
                new CountStatisticImpl(threadPool.numberOfBusyThreads(), stringNumberOfBusyThreads, "COUNT",
                threadPool.getWorkQueue(0).toString(),
                time, time);

        numberOfAvailableThreads =
                new CountStatisticImpl(
                threadPool.numberOfAvailableThreads(), stringNumberOfAvailableThreads, "count",
                threadPool.getWorkQueue(0).toString(),
                time, time);

        currentNumberOfThreads =
                new BoundedRangeStatisticImpl(
                threadPool.currentNumberOfThreads(), threadPool.maximumNumberOfThreads(), threadPool.minimumNumberOfThreads(), java.lang.Long.MAX_VALUE, 0,
                stringCurrentNumberOfThreads, "count",
                threadPool.getWorkQueue(0).toString(),
                time, time);

        averageWorkCompletionTime =
                new BoundedRangeStatisticImpl(
                threadPool.averageWorkCompletionTime(), 0, 0, java.lang.Long.MAX_VALUE, 0,
                stringAverageWorkCompletionTime, "Milliseconds",
                threadPool.getWorkQueue(0).toString(),
                time, time);

        // WorkQueue workItems = threadPool.getWorkQueue(0);

        totalWorkItemsAdded =
                new CountStatisticImpl(
                workQueue.totalWorkItemsAdded(), stringTotalWorkItemsAdded, "count",
                workQueue.getName(),
                time, time);

        numberOfWorkItemsInQueue =
                new BoundedRangeStatisticImpl(
                workQueue.workItemsInQueue(), 0, 0, java.lang.Long.MAX_VALUE, 0,
                stringNumberOfWorkItemsInQueue, "count",
                workQueue.getName(),
                time, time);

        averageTimeInQueue =
                new BoundedRangeStatisticImpl(
                workQueue.averageTimeInQueue(), 0, 0, java.lang.Long.MAX_VALUE, 0,
                stringAverageTimeInQueue, "Milliseconds",
                workQueue.getName(),
                time, time);

    }

    @ManagedAttribute(id = "currentbusythreads")
    @Description("Total number of busy threads")
    public synchronized CountStatistic getNumberOfBusyThreads() {
        int numBusyThreads = threadPool.numberOfBusyThreads();
        numberOfBusyThreads.setCount(numBusyThreads);
        return (CountStatistic) numberOfBusyThreads;
    }

    @ManagedAttribute
    @Description("Total number of available threads")
    public synchronized CountStatistic getNumberOfAvailableThreads() {
        long numAvailableThreads = (long) threadPool.numberOfAvailableThreads();

        numberOfAvailableThreads.setCount(numAvailableThreads);

        return (CountStatistic) numberOfAvailableThreads;
    }

    @ManagedAttribute
    @Description("Total number of current threads")
    public synchronized BoundedRangeStatistic getCurrentNumberOfThreads() {
        int numCurrentThreads = threadPool.currentNumberOfThreads();
        currentNumberOfThreads.setCurrent(numCurrentThreads);
        return (BoundedRangeStatistic) currentNumberOfThreads;
    }

    @ManagedAttribute
    @Description("Average time to complete work")
    public synchronized RangeStatistic getAverageWorkCompletionTime() {
        long avgWorkCompletionTime = threadPool.averageWorkCompletionTime();
        averageWorkCompletionTime.setCurrent(avgWorkCompletionTime);
        return (RangeStatistic) averageWorkCompletionTime;
    }

    @ManagedAttribute
    @Description("Total number of work items added to the queue")
    public synchronized CountStatistic getTotalWorkItemsAdded() {
        long totWorkItemsAdded = workQueue.totalWorkItemsAdded();
        totalWorkItemsAdded.setCount(totWorkItemsAdded);
        return (CountStatistic) totalWorkItemsAdded;
    }

    @ManagedAttribute
    @Description("Total number of work items in the queue")
    public synchronized BoundedRangeStatistic getNumberOfWorkItemsInQueue() {
        int totWorkItemsInQueue = workQueue.workItemsInQueue();
        numberOfWorkItemsInQueue.setCurrent(totWorkItemsInQueue);
        return (BoundedRangeStatistic) numberOfWorkItemsInQueue;
    }

    @ManagedAttribute
    @Description("Average time in queue")
    public synchronized RangeStatistic getAverageTimeInQueue() {
        long avgTimeInQueue = workQueue.averageTimeInQueue();
        averageTimeInQueue.setCurrent(avgTimeInQueue);

        return (RangeStatistic) averageTimeInQueue;
    }
} //ThreadPoolStatsImpl{ }

