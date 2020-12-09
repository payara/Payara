/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.faulttolerance.service;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.FaultToleranceServiceConfiguration;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;

import static java.lang.Integer.parseInt;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.control.RequestContextController;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Base Service for MicroProfile Fault Tolerance.
 *
 * @author Andrew Pielage
 * @author Jan Bernitt (2.0)
 */
@ContractsProvided(FaultToleranceService.class)
@Service(name = "microprofile-fault-tolerance-service")
@RunLevel(StartupRunLevel.VAL)
public class FaultToleranceServiceImpl
        implements EventListener, FaultToleranceService, MonitoringDataSource, FaultToleranceRequestTracing {

    private static final Logger logger = Logger.getLogger(FaultToleranceServiceImpl.class.getName());

    private InvocationManager invocationManager;
    private FaultToleranceServiceConfiguration config;

    @Inject
    private RequestTracingService requestTracingService;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Events events;

    @Inject
    private MetricsService metricsService;

    private final ConcurrentMap<String, ConcurrentMap<String, FaultToleranceMethodContextImpl>> contextByAppNameAndMethodId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BindableFaultToleranceConfig> configByAppName = new ConcurrentHashMap<>();
    private ThreadPoolExecutor asyncExecutorService;
    private ScheduledExecutorService delayExecutorService;

    @PostConstruct
    public void postConstruct() {
        events.register(this);
        invocationManager = serviceLocator.getService(InvocationManager.class);
        requestTracingService = serviceLocator.getService(RequestTracingService.class);
        config = serviceLocator.getService(FaultToleranceServiceConfiguration.class);
        delayExecutorService = Executors.newScheduledThreadPool(getMaxDelayPoolSize());
        asyncExecutorService = new ThreadPoolExecutor(0, getMaxAsyncPoolSize(), getAsyncPoolKeepAliveInSeconds(),
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(true)); // a fair queue => FIFO
        int interval = getCleanupIntervalInMinutes();
        delayExecutorService.scheduleAtFixedRate(this::cleanMethodContexts, interval, interval, TimeUnit.MINUTES);
        if (config != null) {
            if (!"concurrent/__defaultManagedExecutorService".equals(config.getManagedExecutorService())) {
                logger.log(Level.WARNING,
                        "Fault tolerance executor service was configured to managed executor service {0}. This option has been replaced by 'async-max-pool-size' to set the maximum size of a fixed Fault Tolerance pool.",
                        config.getManagedExecutorService());
            }
            if (!"concurrent/__defaultManagedScheduledExecutorService".equals(config.getManagedScheduledExecutorService())) {
                logger.log(Level.WARNING,
                        "Fault tolerance scheduled executor service was configured to managed scheduled executor service {0}. This option has been replaced by 'delay-max-pool-size' to set the maximum size of a fixed Fault Tolerance pool.",
                        config.getManagedScheduledExecutorService());
            }
        }
    }

    /**
     * Since {@link Map#compute(Object, java.util.function.BiFunction)} locks the key entry for
     * {@link ConcurrentHashMap} it is safe to remove the entry in case
     * {@link FaultToleranceMethodContextImpl#isExpired(long)} as concurrent call to
     * {@link Map#computeIfAbsent(Object, java.util.function.Function)} are going to wait for the completion of
     * {@link Map#compute(Object, java.util.function.BiFunction)}.
     */
    private void cleanMethodContexts() {
        final long ttl = TimeUnit.MINUTES.toMillis(1);
        int cleaned = 0;
        for (Map<String, FaultToleranceMethodContextImpl> appEntry : contextByAppNameAndMethodId.values()) {
            for (String key : new HashSet<>(appEntry.keySet())) {
                try {
                    Object newValue = appEntry.compute(key,
                            (k, methodContext) -> methodContext.isExpired(ttl) ? null : methodContext);
                    if (newValue == null) {
                        cleaned++;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to clean FT method context for " + key, e);
                }
            }
        }
        if (cleaned > 0) {
            String allClean = contextByAppNameAndMethodId.isEmpty() ? ".All clean." : ".";
            logger.log(Level.INFO, "Cleaned {0} expired FT method contexts" + allClean, cleaned);
        }
    }

    private int getMaxDelayPoolSize() {
        return config == null ? 20 : parseInt(config.getDelayMaxPoolSize());
    }

    private int getMaxAsyncPoolSize() {
        return config == null ? 2000 : parseInt(config.getAsyncMaxPoolSize());
    }

    private int getAsyncPoolKeepAliveInSeconds() {
        return config == null ? 60 : parseInt(config.getAsyncPoolKeepAliveInSeconds());
    }

    private int getCleanupIntervalInMinutes() {
        return config == null ? 1 : parseInt(config.getCleanupIntervalInMinutes());
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
            FaultTolerancePolicy.clean();
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            if (asyncExecutorService != null) {
                asyncExecutorService.shutdownNow();
            }
            if (delayExecutorService != null) {
                delayExecutorService.shutdownNow();
            }
        }
    }

    @Override
    @MonitoringData(ns = "ft")
    public void collect(MonitoringDataCollector collector) {
        for (Entry<String, ConcurrentMap<String, FaultToleranceMethodContextImpl>> appEntry : contextByAppNameAndMethodId.entrySet()) {
            String app = appEntry.getKey();
            for (Entry<String, FaultToleranceMethodContextImpl> methodEntry  : appEntry.getValue().entrySet()) {
                MonitoringDataCollector methodCollector = collector.group(methodEntry.getKey()).tag("app", app);
                FaultToleranceMethodContext context = methodEntry.getValue();
                BlockingQueue<Thread> concurrentExecutions = context.getConcurrentExecutions();
                if (concurrentExecutions != null) {
                    collectBulkheadSemaphores(methodCollector, concurrentExecutions);
                    collectBulkheadSemaphores(methodCollector, concurrentExecutions, context.getQueuingOrRunningPopulation());
                }
                collectCircuitBreakerState(methodCollector, context.getState());
            }
        }
    }

    private static void collectBulkheadSemaphores(MonitoringDataCollector collector,
            BlockingQueue<Thread> concurrentExecutions) {
        collector
            .collect("RemainingConcurrentExecutionsCapacity", concurrentExecutions.remainingCapacity())
            .collect("ConcurrentExecutions", concurrentExecutions.size());
    }

    private static void collectBulkheadSemaphores(MonitoringDataCollector collector,
            BlockingQueue<Thread> concurrentExecutions, AtomicInteger queuingOrRunningPopulation) {
        collector
            .collect("WaitingQueuePopulation", queuingOrRunningPopulation.get() - concurrentExecutions.size());
    }

    private static void collectCircuitBreakerState(MonitoringDataCollector collector, CircuitBreakerState state) {
        if (state == null) {
            return;
        }
        collector
            .collect("circuitBreakerHalfOpenSuccessful", state.getHalfOpenSuccessfulResultCounter())
            .collect("circuitBreakerState", state.getCircuitState().name().charAt(0));
    }

    @Override
    public FaultToleranceConfig getConfig(InvocationContext context, Stereotypes stereotypes) {
        return configByAppName.computeIfAbsent(getAppName(context),
                key -> new BindableFaultToleranceConfig(stereotypes)).bindTo(context);
    }

    private MetricRegistry getBaseMetricRegistry() {
        try {
            return metricsService.getContext(true).getBaseRegistry();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Removes an application from the enabled map, CircuitBreaker map, and bulkhead maps
     * @param applicationName The name of the application to remove
     */
    private void deregisterApplication(String applicationName) {
        configByAppName.remove(applicationName);
        contextByAppNameAndMethodId.remove(applicationName);
    }

    /**
     * Gets the application name from the invocation manager. Failing that, it will use the module name, component name,
     * or method signature (in that order).
     * @param invocationManager The invocation manager to get the application name from
     * @param context The context of the current invocation
     * @return The application name
     */
    private String getAppName(InvocationContext context) {
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        String appName = currentInvocation.getAppName();
        return appName != null ? appName : "common";
    }

    @Override
    public void startSpan(RequestTraceSpan span, InvocationContext context) {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            addGenericFaultToleranceRequestTracingDetails(span, context);
            requestTracingService.startTrace(span);
        }
    }

    @Override
    public void endSpan() {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            requestTracingService.endTrace();
        }
    }

    private void addGenericFaultToleranceRequestTracingDetails(RequestTraceSpan span,
            InvocationContext context) {
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        span.addSpanTag("App Name", currentInvocation.getAppName());
        span.addSpanTag("Component ID", currentInvocation.getComponentId());
        span.addSpanTag("Module Name", currentInvocation.getModuleName());
        span.addSpanTag("Class Name", context.getMethod().getDeclaringClass().getName());
        span.addSpanTag("Method Name", context.getMethod().getName());
    }

    @Override
    public FaultToleranceMethodContext getMethodContext(InvocationContext context, FaultTolerancePolicy policy,
            RequestContextController requestContextController) {
        return contextByAppNameAndMethodId.computeIfAbsent(getAppName(context), appId -> new ConcurrentHashMap<>())
                    .computeIfAbsent(getTargetMethodId(context),
                        methodId -> createMethodContext(methodId, context, requestContextController)).boundTo(context, policy);
    }

    private FaultToleranceMethodContextImpl createMethodContext(String methodId, InvocationContext context,
            RequestContextController requestContextController) {
        MetricRegistry metricRegistry = getBaseMetricRegistry();
        FaultToleranceMetrics metrics = metricRegistry == null
                ? FaultToleranceMetrics.DISABLED
                : new MethodFaultToleranceMetrics(metricRegistry, FaultToleranceUtils.getCanonicalMethodName(context));
        asyncExecutorService.setMaximumPoolSize(getMaxAsyncPoolSize()); // lazy update of max size
        asyncExecutorService.setKeepAliveTime(getAsyncPoolKeepAliveInSeconds(), TimeUnit.SECONDS);
        logger.log(Level.INFO, "Creating FT method context for {0}", methodId);
        return new FaultToleranceMethodContextImpl(requestContextController, this, metrics, asyncExecutorService,
                delayExecutorService, context.getTarget());
    }

    /**
     * It is essential that the computed signature is referring to the {@link Method} as defined by the target
     * {@link Object} class not its declaring {@link Class} as this could be different when called via an abstract
     * {@link Method} implemented or overridden by the target {@link Class}.
     *
     * Since MP FT 3.0 all instances of a class share same state object for the same method. Or in other words the FT
     * context is not specific to an instance but to the annotated class and method.
     */
    public static String getTargetMethodId(InvocationContext context) {
        Object target = context.getTarget();
        Method method = context.getMethod();
        StringBuilder methodId = new StringBuilder();
        methodId.append(target.getClass().getName()).append('.').append(method.getName());
        if (method.getParameterCount() > 0) {
            methodId.append('(');
            for (Class<?> param : method.getParameterTypes()) {
                methodId.append(param.getName()).append(' ');
            }
            methodId.append(')');
        }
        return methodId.toString();
    }

}
