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

package com.sun.enterprise.v3.services.impl.monitor;

import com.sun.enterprise.v3.services.impl.monitor.stats.ConnectionQueueStatsProvider;
import com.sun.enterprise.v3.services.impl.monitor.stats.ThreadPoolStatsProvider;
import org.glassfish.grizzly.threadpool.AbstractThreadPool;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;

/**
 *
 * @author oleksiys
 */
public class ThreadPoolMonitor implements ThreadPoolProbe {
    private final GrizzlyMonitoring grizzlyMonitoring;
    private final String monitoringId;

    public ThreadPoolMonitor(GrizzlyMonitoring grizzlyMonitoring,
            String monitoringId, ThreadPoolConfig config) {
        this.grizzlyMonitoring = grizzlyMonitoring;
        this.monitoringId = monitoringId;

        if (grizzlyMonitoring != null) {
            final ThreadPoolStatsProvider threadPoolStatsProvider =
                    grizzlyMonitoring.getThreadPoolStatsProvider(monitoringId);
            if (threadPoolStatsProvider != null) {
                threadPoolStatsProvider.setStatsObject(config);
                threadPoolStatsProvider.reset();
            }

            final ConnectionQueueStatsProvider connectionQueueStatsProvider =
                    grizzlyMonitoring.getConnectionQueueStatsProvider(monitoringId);
            if (connectionQueueStatsProvider != null) {
                connectionQueueStatsProvider.setStatsObject(config);
                connectionQueueStatsProvider.reset();
            }
        }
    }

    @Override
    public void onThreadPoolStartEvent(AbstractThreadPool threadPool) {
    }

    @Override
    public void onThreadPoolStopEvent(AbstractThreadPool threadPool) {
    }

    @Override
    public void onThreadAllocateEvent(AbstractThreadPool threadPool, Thread thread) {
        grizzlyMonitoring.getThreadPoolProbeProvider().threadAllocatedEvent(
                monitoringId, threadPool.getConfig().getPoolName(),
                thread.getId());
    }

    @Override
    public void onThreadReleaseEvent(AbstractThreadPool threadPool, Thread thread) {
        grizzlyMonitoring.getThreadPoolProbeProvider().threadReleasedEvent(
                monitoringId, threadPool.getConfig().getPoolName(),
                thread.getId());
    }

    @Override
    public void onMaxNumberOfThreadsEvent(AbstractThreadPool threadPool, int maxNumberOfThreads) {
        grizzlyMonitoring.getThreadPoolProbeProvider().maxNumberOfThreadsReachedEvent(
                monitoringId, threadPool.getConfig().getPoolName(),
                maxNumberOfThreads);
    }

    @Override
    public void onTaskDequeueEvent(AbstractThreadPool threadPool, Runnable task) {
        grizzlyMonitoring.getThreadPoolProbeProvider().threadDispatchedFromPoolEvent(
                monitoringId, threadPool.getConfig().getPoolName(),
                Thread.currentThread().getId());
        grizzlyMonitoring.getConnectionQueueProbeProvider().onTaskDequeuedEvent(
                monitoringId, task.getClass().getName());
    }

    @Override
    public void onTaskCancelEvent(AbstractThreadPool threadPool, Runnable task) {
        // when dequeued task is cancelled - we have to "return" the thread, that
        // we marked as dispatched from the pool
        grizzlyMonitoring.getThreadPoolProbeProvider().threadReturnedToPoolEvent(
                monitoringId, threadPool.getConfig().getPoolName(),
                Thread.currentThread().getId());
    }
    
    @Override
    public void onTaskCompleteEvent(AbstractThreadPool threadPool, Runnable task) {
        grizzlyMonitoring.getThreadPoolProbeProvider().threadReturnedToPoolEvent(
                monitoringId, threadPool.getConfig().getPoolName(),
                Thread.currentThread().getId());
    }

    @Override
    public void onTaskQueueEvent(AbstractThreadPool threadPool, Runnable task) {
        grizzlyMonitoring.getConnectionQueueProbeProvider().onTaskQueuedEvent(
                monitoringId, task.getClass().getName());
    }

    @Override
    public void onTaskQueueOverflowEvent(AbstractThreadPool threadPool) {
        grizzlyMonitoring.getConnectionQueueProbeProvider().onTaskQueueOverflowEvent(
                monitoringId);
    }
}
