/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Optional;
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

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private FaultToleranceServiceConfiguration serviceConfig;

    private InvocationManager invocationManager;

    @Inject
    private RequestTracingService requestTracingService;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Events events;

    @Inject
    private MetricsService metricsService;

    private static final class ApplicationState {

        final AtomicReference<BindableFaultToleranceConfig> config = new AtomicReference<>();
        final Map<Object, Map<String, FaultToleranceMethodContext>> methodByTargetObjectAndName = new ConcurrentHashMap<>();

    }

    private final Map<String, ApplicationState> stateByApplication = new ConcurrentHashMap<>();
    private ManagedScheduledExecutorService defaultScheduledExecutorService;
    private ManagedExecutorService defaultExecutorService;
    private ExecutorService asyncExecutorService;

    @PostConstruct
    public void postConstruct() throws NamingException {
        events.register(this);
        serviceConfig = serviceLocator.getService(FaultToleranceServiceConfiguration.class);
        invocationManager = serviceLocator.getService(InvocationManager.class);
        requestTracingService = serviceLocator.getService(RequestTracingService.class);
        InitialContext context = new InitialContext();
        defaultExecutorService = (ManagedExecutorService) context.lookup("java:comp/DefaultManagedExecutorService");
        defaultScheduledExecutorService = (ManagedScheduledExecutorService) context
                .lookup("java:comp/DefaultManagedScheduledExecutorService");
        asyncExecutorService = Executors.newCachedThreadPool();
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
            FaultTolerancePolicy.clean();
        }
    }

    @Override
    @MonitoringData(ns = "ft")
    public void collect(MonitoringDataCollector collector) {
        for (Entry<String, ApplicationState> appStateEntry : stateByApplication.entrySet()) {
            collectMethodState(collector, appStateEntry.getKey(), appStateEntry.getValue().methodByTargetObjectAndName);
        }
    }

    private static void collectMethodState(MonitoringDataCollector collector, String appName,
            Map<Object, Map<String, FaultToleranceMethodContext>> entries) {
        for (Entry<Object, Map<String, FaultToleranceMethodContext>> entry : entries.entrySet()) {
            Object target = entry.getKey();
            String targetValue = System.identityHashCode(target) + "@" + target.getClass().getSimpleName();
            for (Entry<String, FaultToleranceMethodContext> methodValue : entry.getValue().entrySet()) {
                String group = appName + "-" + targetValue + "-" + methodValue.getKey();
                MonitoringDataCollector methodCollector = collector.group(group);
                FaultToleranceMethodContext context = methodValue.getValue();
                collectBulkheadSemaphores(methodCollector, "execution", context.getConcurrentExecutions(-1));
                collectBulkheadSemaphores(methodCollector, "queue", context.getWaitingQueuePopulation(-1));
                collectCircuitBreakerState(methodCollector, context.getState(-1));
            }
        }
    }

    private static void collectBulkheadSemaphores(MonitoringDataCollector collector, String type, BulkheadSemaphore semaphore) {
        if (semaphore == null) {
            return;
        }
        collector
            .collect(type + "AvailablePermits", semaphore.availablePermits())
            .collect(type + "AcquiredPermits", semaphore.acquiredPermits());
    }

    private static void collectCircuitBreakerState(MonitoringDataCollector collector, CircuitBreakerState state) {
        if (state == null) {
            return;
        }
        collector
            .collect("circuitBreakerHalfOpenSuccessFul", state.getHalfOpenSuccessFulResultCounter())
            .collect("circuitBreakerState", state.getCircuitState().name().charAt(0));
    }

    @Override
    public FaultToleranceConfig getConfig(InvocationContext context, Stereotypes stereotypes) {
        ApplicationState appState = getApplicationState(getApplicationContext(context));
        return appState.config.updateAndGet(
                config -> config != null ? config : new BindableFaultToleranceConfig(stereotypes)).bindTo(context);
    }

    private MetricRegistry getApplicationMetricRegistry() {
        try {
            return metricsService.getApplicationRegistry();
        } catch (Exception e) {
            return null;
        }
    }

    private ManagedExecutorService getManagedExecutorService() {
        return lookup(serviceConfig.getManagedExecutorService(), defaultExecutorService);
    }

    ManagedScheduledExecutorService getManagedScheduledExecutorService() {
        return lookup(serviceConfig.getManagedScheduledExecutorService(), defaultScheduledExecutorService);
    }

    @SuppressWarnings("unchecked")
    private static <T> T lookup(String name, T defaultInstance) {
        // If no name has been set, just get the default
        if (name == null || name.isEmpty()) {
            return defaultInstance; 
        }
        try {
            return (T) new InitialContext().lookup(name);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Could not find configured , " + name + ", so resorting to default", ex);
            return defaultInstance;
        }
    }

    /**
     * Removes an application from the enabled map, CircuitBreaker map, and bulkhead maps
     * @param applicationName The name of the application to remove
     */
    private void deregisterApplication(String applicationName) {
        stateByApplication.remove(applicationName);
    }

    /**
     * Gets the application name from the invocation manager. Failing that, it will use the module name, component name,
     * or method signature (in that order).
     * @param invocationManager The invocation manager to get the application name from
     * @param context The context of the current invocation
     * @return The application name
     */
    private String getApplicationContext(InvocationContext context) {
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        String appName = currentInvocation.getAppName();
        if (appName != null) {
            return appName;
        }
        appName = currentInvocation.getModuleName();
        if (appName != null) {
            return appName;
        }
        appName = currentInvocation.getComponentId();
        // If we've found a component name, check if there's an application registered with the same name
        if (appName != null) {
            // If it's not directly in the registry, it's possible due to how the componentId is constructed
            if (serviceLocator.getService(ApplicationRegistry.class).get(appName) == null) {
                // The application name should be the first component
                return appName.split("_/")[0];
            }
        }
        // If we still don't have a name - just construct it from the method signature
        return getFullMethodSignature(context.getMethod());
    }

    private ApplicationState getApplicationState(String applicationName) {
        return stateByApplication.computeIfAbsent(applicationName, key -> new ApplicationState());
    }

    /**
     * Helper method to generate a full method signature consisting of canonical class name, method name, 
     * parameter types, and return type.
     * @param annotatedMethod The annotated Method to generate the signature for
     * @return A String in the format of CanonicalClassName#MethodName({ParameterTypes})>ReturnType
     */
    private static String getFullMethodSignature(Method annotatedMethod) {
        return annotatedMethod.getDeclaringClass().getCanonicalName() 
                + "#" + annotatedMethod.getName() 
                + "(" + Arrays.toString(annotatedMethod.getParameterTypes()) + ")"
                + ">" + annotatedMethod.getReturnType().getSimpleName();
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
        span.addSpanTag("App Name", invocationManager.getCurrentInvocation().getAppName());
        span.addSpanTag("Component ID", invocationManager.getCurrentInvocation().getComponentId());
        span.addSpanTag("Module Name", invocationManager.getCurrentInvocation().getModuleName());
        span.addSpanTag("Class Name", context.getMethod().getDeclaringClass().getName());
        span.addSpanTag("Method Name", context.getMethod().getName());
    }

    @Override
    public FaultToleranceMethodContext getMethodContext(InvocationContext context) {
        ApplicationState appState = getApplicationState(getApplicationContext(context));
        FaultToleranceMethodContext methodContext = appState.methodByTargetObjectAndName //
                .computeIfAbsent(context.getTarget(), key -> new ConcurrentHashMap<>()) //
                .computeIfAbsent(getFullMethodSignature(context.getMethod()), key -> {
                    FaultToleranceMetrics metrics = new MethodFaultToleranceMetrics(getApplicationMetricRegistry(),
                            FaultToleranceUtils.getCanonicalMethodName(context));
                    return new FaultToleranceMethodContextImpl(this, metrics, asyncExecutorService,
                            getManagedScheduledExecutorService());
                });
        return ((FaultToleranceMethodContextImpl) methodContext).in(context);
    }

}
