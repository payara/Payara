/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2022 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.service;

import fish.payara.microprofile.faulttolerance.*;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private final ConcurrentMap<MethodKey, FaultToleranceMethodContextImpl> contextByMethod = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BindableFaultToleranceConfig> configByAppName = new ConcurrentHashMap<>();
    private ExecutorService asyncExecutorService;
    private ScheduledExecutorService delayExecutorService;

    @PostConstruct
    public void postConstruct() {
        try {
            events.register(this);
            invocationManager = serviceLocator.getService(InvocationManager.class);
            requestTracingService = serviceLocator.getService(RequestTracingService.class);
            config = serviceLocator.getService(FaultToleranceServiceConfiguration.class);
            InitialContext context = new InitialContext();
            asyncExecutorService = (ManagedExecutorService) context.lookup(config.getManagedExecutorService());
            delayExecutorService = (ManagedScheduledExecutorService) context.lookup(config.getManagedScheduledExecutorService());
        } catch (NamingException namingException) {
            throw new RuntimeException("Error initialising Fault Tolerance Service: could not perform lookup for configured managed-executor-service or managed-scheduled-executor-service.", namingException);
        }
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info);
            FaultTolerancePolicy.clean(info.getAppClassLoader());
        }
    }

    @Override
    @MonitoringData(ns = "ft")
    public void collect(MonitoringDataCollector collector) {
        for (Entry<MethodKey, FaultToleranceMethodContextImpl> methodEntry : contextByMethod.entrySet()) {
            MonitoringDataCollector methodCollector = collector.group(methodEntry.getKey().getMethodId())
                    .tag("app", methodEntry.getValue().getAppName());
            FaultToleranceMethodContext context = methodEntry.getValue();
            BlockingQueue<Thread> concurrentExecutions = context.getConcurrentExecutions();
            if (concurrentExecutions != null) {
                collectBulkheadSemaphores(methodCollector, concurrentExecutions);
                collectBulkheadSemaphores(methodCollector, concurrentExecutions, context.getQueuingOrRunningPopulation());
            }
            collectCircuitBreakerState(methodCollector, context.getState());
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

    private MetricsService.MetricsContext getMetricsContext() {
        try {
            return metricsService.getContext(true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Removes an application from the enabled map, CircuitBreaker map, and bulkhead maps
     *
     * @param appInfo The name of the application to remove
     */
    private void deregisterApplication(ApplicationInfo appInfo) {
        configByAppName.remove(appInfo.getName());
        contextByMethod.keySet().removeIf(methodKey ->
                methodKey.targetClass.getClassLoader().equals(appInfo.getAppClassLoader()));
    }

    /**
     * Gets the application name from the invocation manager. Failing that, it will use the module name, component name,
     * or method signature (in that order).
     *
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
        return contextByMethod.computeIfAbsent(new MethodKey(context),
                methodKey -> createMethodContext(methodKey, context, requestContextController)).boundTo(context, policy);
    }

    private FaultToleranceMethodContextImpl createMethodContext(MethodKey methodKey, InvocationContext context,
            RequestContextController requestContextController) {
        MetricsService.MetricsContext metricsContext = getMetricsContext();
        MetricRegistry metricRegistry = metricsContext != null ? metricsContext.getBaseRegistry() : null;
        String appName = metricsContext != null ? metricsContext.getName() : "";
        FaultToleranceMetrics metrics = metricRegistry == null
                ? FaultToleranceMetrics.DISABLED
                : new MethodFaultToleranceMetrics(metricRegistry, FaultToleranceUtils.getCanonicalMethodName(context));
        logger.log(Level.FINE, "Creating FT method context for {0}", methodKey);
        return new FaultToleranceMethodContextImpl(requestContextController, this, metrics, asyncExecutorService,
                delayExecutorService, appName);
    }
}
