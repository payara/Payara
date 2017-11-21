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

// Portions Copyright [2016] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.services.impl.monitor.stats;

import java.util.Set;
import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.annotations.Reset;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Server wide Thread Pool statistics
 * 
 * @author Amy Roh
 */
@AMXMetadata(type = "thread-pool-mon", group = "monitoring")
@ManagedObject
@Description("Thread Pool Statistics")
public class ThreadPoolStatsProviderGlobal extends ThreadPoolStatsProvider {
    
    public ThreadPoolStatsProviderGlobal(String name) {
        super(name);
    }
    
    @ManagedAttribute(id = "currentthreadcount")
    @Description("Provides the number of request processing threads currently in the listener thread pool")
    @Override
    public CountStatistic getCurrentThreadCount() {
        countThreadsInThreadPools();
        return currentThreadCount;
    }
    
    @ProbeListener("glassfish:kernel:thread-pool:setMaxThreadsEvent")
    @Override
    public void setMaxThreadsEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("maxNumberOfThreads") int maxNumberOfThreads) {

        maxThreadsCount.setCount(maxNumberOfThreads);
    }

    @ProbeListener("glassfish:kernel:thread-pool:setCoreThreadsEvent")
    @Override
    public void setCoreThreadsEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("coreNumberOfThreads") int coreNumberOfThreads) {

        coreThreadsCount.setCount(coreNumberOfThreads);
    }
    
    @ProbeListener("glassfish:kernel:thread-pool:threadDispatchedFromPoolEvent")
    @Override
    public void threadDispatchedFromPoolEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadId") long threadId) {
        currentThreadsBusy.increment();
    }
    
    @ProbeListener("glassfish:kernel:thread-pool:threadReturnedToPoolEvent")
    @Override
    public void threadReturnedToPoolEvent(
            @ProbeParam("monitoringId") String monitoringId,
            @ProbeParam("threadId") long threadId) {

        totalExecutedTasksCount.increment();
        
        // Stop it from decrementing to negative values
        if (currentThreadsBusy.getCount() > 0) {
            currentThreadsBusy.decrement();
        }
    }
    
    /**
     * Counts the threads in all thread pools by querying the JVM. Also 
     * counts the number of threads that are running.
     */
    private void countThreadsInThreadPools() {     
        // Set to 0 as we want to reset them
        currentThreadCount.setCount(0);
        
        // Get all the threads currently in the JVM
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        
        for (Thread thread : threads) {
            String threadName = thread.getName();
            for (String threadPoolName : threadPoolNames) {
                if (thread.isAlive() && threadName.contains(threadPoolName + "(")) {
                    currentThreadCount.increment();                   
                    break;
                }
            }
        }
    }
    
    public void subtractBusyThreads(long busyThreads) {
        long busyThreadsCount = currentThreadsBusy.getCount() - busyThreads;
        if (busyThreadsCount > 0) {
            currentThreadsBusy.setCount(currentThreadsBusy.getCount() - busyThreads);
        } else {
            currentThreadsBusy.setCount(0);
        }
    }
    
    @Reset
    @Override
    public void reset() {
        // Do nothing
    }
}
