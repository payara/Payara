/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.ejb.containers;

import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import javax.transaction.Status;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class EjbThreadPoolExecutor extends ThreadPoolExecutor {
    public EjbThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, BlockingQueue<Runnable> workQueue, String threadPoolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue, 
                new ThreadFactoryImpl(threadPoolName));
    }

    /**
     * Ensure that we give out our EjbFutureTask as opposed to JDK's FutureTask
     * @param callable
     * @return a RunnableFuture
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        if (callable instanceof EjbAsyncTask) {
            return ((EjbAsyncTask) callable).getFutureTask();
        }
        return super.newTaskFor(callable);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EjbThreadPoolExecutor with ");
        sb.append(RuntimeTagNames.THREAD_CORE_POOL_SIZE).append(" ").append(getCorePoolSize()).append(" ");
        sb.append(RuntimeTagNames.THREAD_MAX_POOL_SIZE).append(" ").append(getMaximumPoolSize()).append(" ");
        sb.append(RuntimeTagNames.THREAD_KEEP_ALIVE_SECONDS).append(" ").append(getKeepAliveTime(TimeUnit.SECONDS)).append(" ");
        sb.append(RuntimeTagNames.THREAD_QUEUE_CAPACITY).append(" ").append(getQueue().remainingCapacity()).append(" ");
        sb.append(RuntimeTagNames.ALLOW_CORE_THREAD_TIMEOUT).append(" ").append(allowsCoreThreadTimeOut()).append(" ");
        return sb.toString();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        try {
            JavaEETransactionManager tm = EjbContainerUtilImpl.getInstance().getTransactionManager();
            if (tm.getTransaction() != null) {
                int st = tm.getStatus();
                Logger logger = EjbContainerUtilImpl.getLogger();
                logger.warning("NON-NULL TX IN AFTER_EXECUTE. TX STATUS: " + st);
                if (st == Status.STATUS_ROLLEDBACK || st == Status.STATUS_COMMITTED ||
                        st == Status.STATUS_UNKNOWN) {
                    tm.clearThreadTx();
                } else {
                    tm.rollback();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class ThreadFactoryImpl implements ThreadFactory {
        private AtomicInteger threadId = new AtomicInteger(0);
        private String threadPoolName;

        public ThreadFactoryImpl(String threadPoolName) {
            this.threadPoolName = threadPoolName;
        }

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, threadPoolName + threadId.incrementAndGet());
            th.setDaemon(true);
            th.setContextClassLoader(null); //Prevent any app classloader being set as CCL
            return th;
        }
    }
}
