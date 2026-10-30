/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.weld.services;

import com.sun.enterprise.util.Utility;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import org.jboss.weld.executor.AbstractExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;

/**
 * Implementation of the Weld Executor Services SPI which uses the Payara Executor
 * Service for its 
 * @author steve
 */
public class ExecutorServicesImpl extends AbstractExecutorServices implements ExecutorServices {

    private final ExecutorService taskExecutor;
    private final ContextualTimerExecutor timerExecutor;
    private final ManagedExecutorService executor;
    private final org.glassfish.concurrent.config.ManagedExecutorService config;

    private static final String DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE = "java:comp/DefaultManagedScheduledExecutorService";
    private static final String DEFAULT_MANAGED_EXECUTOR_SERVICE = "java:comp/DefaultManagedExecutorService";
    private static final String DEFAULT_MANAGED_EXECUTOR_SERVICE_PHYS = "concurrent/__defaultManagedExecutorService";

    public ExecutorServicesImpl() throws NamingException {
        InitialContext ctx = new InitialContext();
        executor = (ManagedExecutorService) ctx.lookup(DEFAULT_MANAGED_EXECUTOR_SERVICE);
        taskExecutor = new ContextualTaskExecutor();
        timerExecutor = new ContextualTimerExecutor();
        this.config = getManagedExecutorServiceConfig(DEFAULT_MANAGED_EXECUTOR_SERVICE_PHYS);
    }

    @Override
    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }

    @Override
    public ScheduledExecutorService getTimerExecutor() {
        return timerExecutor;
    }

    @Override
    public void cleanup() {
    }

    @Override
    protected int getThreadPoolSize() {
        return Integer.parseInt(this.config.getMaximumPoolSize());
    }

    @Override
    public <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        // needs to set the context classloader based on the current context classloader and then reset it back
        ClassLoader TCCL = Thread.currentThread().getContextClassLoader();
        List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrapped.add(inContextClassloader(TCCL, task));
        }
        return wrapped;
    }
    
    
    private static <V> Callable<V> inContextClassloader(ClassLoader contextClassLoader, Callable<V> task) {
        return () -> {
            ClassLoader old = Utility.setContextClassLoader(contextClassLoader);
            try {
                return task.call();
            } finally {
                Utility.setContextClassLoader(old);
            }
        };
    }

    private static Runnable inContextClassloader(ClassLoader contextClassLoader, Runnable task) {
        return () -> {
            ClassLoader old = Utility.setContextClassLoader(contextClassLoader);
            try {
                task.run();
            } finally {
                Utility.setContextClassLoader(old);
            }
        };
    }

    private static <V> Callable<V> inCurrentContextClassloader(Callable<V> task) {
        return inContextClassloader(Thread.currentThread().getContextClassLoader(), task);
    }

    private static Runnable inCurrentContextClassloader(Runnable task) {
        return inContextClassloader(Thread.currentThread().getContextClassLoader(), task);
    }

    class ContextualTaskExecutor implements ExecutorService {
        private final ExecutorService delegate;

        ContextualTaskExecutor() {
            this.delegate = executor;
        }

        @Override
        public void shutdown() {
            throw new IllegalStateException("Downstream service cannot request shutdown");
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new IllegalStateException("Downstream service cannot request shutdown");
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate().awaitTermination(timeout, unit);
        }

        @Override
        public boolean isShutdown() {
            return delegate().isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate().isTerminated();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate().submit(inCurrentContextClassloader(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate().submit(inCurrentContextClassloader(task), result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate().submit(inCurrentContextClassloader(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate().invokeAll(wrap(tasks));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return delegate().invokeAll(wrap(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate().invokeAny(wrap(tasks));
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate().invokeAny(wrap(tasks), timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate().execute(inCurrentContextClassloader(command));
        }

        protected ExecutorService delegate() {
            return delegate;
        }
    }

    class ContextualTimerExecutor extends ContextualTaskExecutor implements ScheduledExecutorService {
        private final ScheduledExecutorService delegate;

        ContextualTimerExecutor() throws NamingException {
            InitialContext ctx = new InitialContext();
            this.delegate = (ManagedScheduledExecutorService) ctx.lookup(DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE);
        }

        @Override
        protected ScheduledExecutorService delegate() {
            return this.delegate;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return delegate().schedule(inCurrentContextClassloader(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return delegate().schedule(inCurrentContextClassloader(callable), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return delegate().scheduleAtFixedRate(inCurrentContextClassloader(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return delegate().scheduleWithFixedDelay(inCurrentContextClassloader(command), initialDelay, delay, unit);
        }
    }

    private org.glassfish.concurrent.config.ManagedExecutorService getManagedExecutorServiceConfig(String jndiName) {
        for (org.glassfish.concurrent.config.ManagedExecutorService service : Globals.getDefaultHabitat()
                .getAllServices(org.glassfish.concurrent.config.ManagedExecutorService.class)) {
            if(service.getJndiName().equals(jndiName)) {
                return service;
            }
        }
        throw new IllegalStateException(jndiName + " executor service config not found.");
    }

}
