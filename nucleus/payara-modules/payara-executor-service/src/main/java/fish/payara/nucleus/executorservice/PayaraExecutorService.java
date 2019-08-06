/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2017-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.executorservice;

import static java.util.logging.Level.SEVERE;
import static org.glassfish.api.admin.ProcessEnvironment.ProcessType.ACC;

import java.beans.PropertyChangeEvent;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.config.serverbeans.Config;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "payara-executor-service")
public class PayaraExecutorService implements ConfigListener, EventListener {

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    PayaraExecutorServiceConfiguration payaraExecutorServiceConfiguration;

    @Inject
    Transactions transactions;

    @Inject
    ServerEnvironment serverEnvironment;
    
    @Inject
    private ProcessEnvironment processEnvironment;

    @Inject
    private Events events;

    private ThreadPoolExecutor threadPoolExecutor;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @PostConstruct
    public void postConstruct() {
        if (events != null) {
            events.register(this);
        }

        if (payaraExecutorServiceConfiguration == null) {
            payaraExecutorServiceConfiguration = Globals.get(PayaraExecutorServiceConfiguration.class);
        }

        if (transactions != null) {
            transactions.addListenerForType(PayaraExecutorServiceConfiguration.class, this);
        }

        initialiseThreadPools();
    }

    /**
     *
     * @param event
     */
    @Override
    public void event(Event event) {
        if (event.is(Deployment.ALL_APPLICATIONS_LOADED)) {
            // there is awkward point in embedded EJBContainer initialization, where same server
            // instance is started twice. In such case, we need to reinitialize.
            if (threadPoolExecutor.isShutdown()) {
                initialiseThreadPools();
            }
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            threadPoolExecutor.shutdown();
            scheduledThreadPoolExecutor.shutdown();
        }
    }

    private void initialiseThreadPools() {
        if (isACC()) {
            
            // The application client container doesn't use the executor service locally for now.
            // But create a "mock" executor to be absolutely sure.
            threadPoolExecutor = new ThreadPoolExecutor(
                0,
                1,
                0,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(), 
                r -> new Thread(r, "payara-executor-service-task"));
            threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            
            scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                    0, r -> {
                        Thread t = new Thread(r, "payara-executor-service-scheduled-task");
                        t.setUncaughtExceptionHandler( (thread, thrwbl) -> 
                            Logger.getLogger(PayaraExecutorService.class.getName()).log(SEVERE, "Uncaught exception in Payara Scheduled Executor thread ", thrwbl));
                        return t;
            });
            scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
            
            
        } else {
            int threadPoolExecutorQueueSize = Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorQueueSize());
            
            if (threadPoolExecutorQueueSize > 0) {
                threadPoolExecutor = new ThreadPoolExecutor(
                        Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorCorePoolSize()),
                        Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorMaxPoolSize()),
                        Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorKeepAliveTime()),
                        TimeUnit.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorKeepAliveTimeUnit()),
                        new LinkedBlockingQueue<>(threadPoolExecutorQueueSize),
                        (Runnable r) -> new Thread(r, "payara-executor-service-task"));
            } else {
                threadPoolExecutor = new ThreadPoolExecutor(
                        Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorCorePoolSize()),
                        Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorMaxPoolSize()),
                        Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorKeepAliveTime()),
                        TimeUnit.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorKeepAliveTimeUnit()),
                        new SynchronousQueue<>(), (Runnable r) -> new Thread(r, "payara-executor-service-task"));
            }
            
            threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                    Integer.valueOf(payaraExecutorServiceConfiguration.getScheduledThreadPoolExecutorCorePoolSize()), (Runnable r) -> {
                        Thread t = new Thread(r, "payara-executor-service-scheduled-task");
                        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread thread, Throwable thrwbl) {
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Uncaught exception in Payara Scheduled Executor thread ",thrwbl);
                            }
                        });
                        return t;
            });
            scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        }
        
    }

    public <T> Future<T> submit(Callable<T> task) {
        return threadPoolExecutor.submit(task);
    }

    public Future<?> submit(Runnable task) {
        return threadPoolExecutor.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return threadPoolExecutor.submit(task, result);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduledThreadPoolExecutor.schedule(callable, delay, unit);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledThreadPoolExecutor.schedule(command, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledThreadPoolExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduledThreadPoolExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public ExecutorService getUnderlyingExecutorService() {
        return threadPoolExecutor;
    }

    public ScheduledExecutorService getUnderlyingScheduledExecutorService() {
        return scheduledThreadPoolExecutor;
    }

    public int getExecutorThreadPoolSize() {
        return Integer.valueOf(payaraExecutorServiceConfiguration.getThreadPoolExecutorMaxPoolSize());
    }

    public int getScheduledExecutorThreadPoolSize() {
        return Integer.valueOf(payaraExecutorServiceConfiguration.getScheduledThreadPoolExecutorCorePoolSize());
    }


    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        List<UnprocessedChangeEvent> unprocessedChanges = new ArrayList<>();

        boolean keepAliveChanged = false;
        for (PropertyChangeEvent propertyChangeEvent : propertyChangeEvents) {
            switch (propertyChangeEvent.getPropertyName()) {
                case "thread-pool-executor-core-pool-size":
                    if (isCurrentInstanceMatchTarget(propertyChangeEvent)
                            && !propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                        threadPoolExecutor.setCorePoolSize((Integer) propertyChangeEvent.getNewValue());
                    }
                    break;
                case "thread-pool-executor-max-pool-size":
                    if (isCurrentInstanceMatchTarget(propertyChangeEvent)
                            && !propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                        threadPoolExecutor.setMaximumPoolSize((Integer) propertyChangeEvent.getNewValue());
                    }
                    break;
                case "thread-pool-executor-keep-alive-time":
                    if (isCurrentInstanceMatchTarget(propertyChangeEvent)
                            && !propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                        if (!keepAliveChanged) {
                            for (PropertyChangeEvent pce : propertyChangeEvents) {
                                if (isCurrentInstanceMatchTarget(pce)
                                        && pce.getPropertyName().equals("thread-pool-executor-keep-alive-time-unit")) {
                                    if (!pce.getOldValue().equals(pce.getNewValue())) {
                                        keepAliveChanged = true;
                                        threadPoolExecutor.setKeepAliveTime(
                                                Long.valueOf((String) propertyChangeEvent.getNewValue()),
                                                TimeUnit.valueOf((String) pce.getNewValue()));
                                    } else {
                                        threadPoolExecutor.setKeepAliveTime((Integer) propertyChangeEvent.getNewValue(),
                                                TimeUnit.valueOf(payaraExecutorServiceConfiguration
                                                        .getThreadPoolExecutorKeepAliveTimeUnit()));
                                    }
                                }
                            }

                            if (!keepAliveChanged) {
                                threadPoolExecutor.setKeepAliveTime(
                                        Long.valueOf((String) propertyChangeEvent.getNewValue()),
                                        TimeUnit.valueOf(payaraExecutorServiceConfiguration
                                                .getThreadPoolExecutorKeepAliveTimeUnit()));
                            }
                        }
                    }
                    break;
                case "thread-pool-executor-keep-alive-time-unit":
                    if (isCurrentInstanceMatchTarget(propertyChangeEvent)
                            && !propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                        if (!keepAliveChanged) {
                            for (PropertyChangeEvent pce : propertyChangeEvents) {
                                if (isCurrentInstanceMatchTarget(pce)
                                        && pce.getPropertyName().equals("thread-pool-executor-keep-alive-time")) {
                                    if (!pce.getOldValue().equals(pce.getNewValue())) {
                                        keepAliveChanged = true;
                                        threadPoolExecutor.setKeepAliveTime(Long.valueOf((String) pce.getNewValue()),
                                                TimeUnit.valueOf((String) propertyChangeEvent.getNewValue()));
                                    } else {
                                        threadPoolExecutor.setKeepAliveTime(Long.valueOf((String)
                                                payaraExecutorServiceConfiguration
                                                        .getThreadPoolExecutorKeepAliveTime()),
                                                TimeUnit.valueOf((String) propertyChangeEvent.getNewValue()));
                                    }
                                }
                            }

                            if (!keepAliveChanged) {
                                threadPoolExecutor.setKeepAliveTime(Long.valueOf((String)
                                        payaraExecutorServiceConfiguration.getThreadPoolExecutorKeepAliveTime()),
                                        TimeUnit.valueOf((String) propertyChangeEvent.getNewValue()));
                            }
                        }
                    }
                    break;
                case "thread-pool-executor-queue-size":
                    if (isCurrentInstanceMatchTarget(propertyChangeEvent)
                            && !propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                        unprocessedChanges.add(new UnprocessedChangeEvent(propertyChangeEvent,
                                "Payara Executor Service requires restarting"));
                    }
                    break;
                case "scheduled-thread-pool-executor-core-pool-size":
                    if (isCurrentInstanceMatchTarget(propertyChangeEvent)
                            && !propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                        scheduledThreadPoolExecutor.setCorePoolSize((Integer) propertyChangeEvent.getNewValue());
                    }
                    break;
            }
        }

        if (unprocessedChanges.isEmpty()) {
            return null;
        } else {
            return new UnprocessedChangeEvents(unprocessedChanges);
        }
    }

    private boolean isCurrentInstanceMatchTarget(PropertyChangeEvent pe) {
        if (serverEnvironment.isInstance()) {
            return true;
        }

        ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
        while (proxy != null && !(proxy instanceof Config)) {
            proxy = proxy.getParent();
        }

        if (proxy != null) {
            return ((Config)proxy).isDas();
        }

        return false;
    }
    
    public boolean isACC() {
        return processEnvironment.getProcessType().equals(ACC);
    }
}
