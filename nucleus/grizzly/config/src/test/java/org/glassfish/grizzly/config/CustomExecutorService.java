/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Custom {@link ExecutorService} implementation.
 * 
 * @author Alexey Stashok
 */
public class CustomExecutorService implements ExecutorService, ConfigAwareElement<ThreadPool> {
    private ExecutorService internalExecutorService;
    

    @Override
    public void configure(ServiceLocator habitat, NetworkListener networkListener,
            ThreadPool configuration) {
        internalExecutorService = new ThreadPoolExecutor(
                toInt(configuration.getMinThreadPoolSize()),
                toInt(configuration.getMaxThreadPoolSize()),
                toInt(configuration.getIdleThreadTimeoutSeconds()),
                TimeUnit.SECONDS,
                toInt(configuration.getMaxQueueSize()) >= 0 ?
                new LinkedBlockingQueue<Runnable>(toInt(configuration.getMaxQueueSize())) :
                new LinkedTransferQueue<Runnable>());
        
    }
    
    @Override
    public void shutdown() {
        internalExecutorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return internalExecutorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return internalExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return internalExecutorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return internalExecutorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return internalExecutorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return internalExecutorService.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return internalExecutorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return internalExecutorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return internalExecutorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return internalExecutorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return internalExecutorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        internalExecutorService.execute(command);
    }
    
    private static int toInt(String s) {
        return Integer.parseInt(s);
    }
}
