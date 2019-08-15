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
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.FaultToleranceServiceConfiguration;
import fish.payara.microprofile.faulttolerance.policy.AsynchronousPolicy;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
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
public class FaultToleranceServiceImpl implements EventListener, FaultToleranceService, MonitoringDataSource {

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

    private final Map<String, FaultToleranceApplicationState> stateByApplication = new ConcurrentHashMap<>();
    private ManagedScheduledExecutorService defaultScheduledExecutorService;
    private ManagedExecutorService defaultExecutorService;

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
    public void collect(MonitoringDataCollector collector) {
        MonitoringDataCollector ftCollector = collector.in("fault-tolerance");
        for (Entry<String, FaultToleranceApplicationState> appStateEntry : stateByApplication.entrySet()) {
            String appName = appStateEntry.getKey();
            FaultToleranceApplicationState appState = appStateEntry.getValue();
            collectMethodState(ftCollector, appName, "execution-semaphore",
                    appState.getBulkheadExecutionSemaphores(), FaultToleranceServiceImpl::collectBulkheadSemaphores);
            collectMethodState(ftCollector, appName, "queue-semaphore",
                    appState.getBulkheadExecutionQueueSemaphores(), FaultToleranceServiceImpl::collectBulkheadSemaphores);
            collectMethodState(ftCollector, appName, "circuit-breaker",
                    appState.getCircuitBreakerStates(), FaultToleranceServiceImpl::collectCircuitBreakerState);

        }
    }

    private static <V> void collectMethodState(MonitoringDataCollector collector, String appName, String type,
            Map<Object, Map<String, V>> semaphores, BiConsumer<MonitoringDataCollector, V> collect) {
        for (Entry<Object, Map<String, V>> targetExecutionSemaphores : semaphores.entrySet()) {
            Object target = targetExecutionSemaphores.getKey();
            String targetValue = System.identityHashCode(target) + "@" + target.getClass().getSimpleName();
            for (Entry<String, V> methodValue : targetExecutionSemaphores.getValue().entrySet()) {
                collect.accept(collector.app(appName).type(type).tag("target", targetValue)
                        .entity(methodValue.getKey()), methodValue.getValue());
            }
        }
    }

    private static void collectBulkheadSemaphores(MonitoringDataCollector collector, BulkheadSemaphore semaphore) {
        collector
            .collect("availablePermits", semaphore.availablePermits())
            .collect("acquiredPermits", semaphore.acquiredPermits());
    }

    private static void collectCircuitBreakerState(MonitoringDataCollector collector, CircuitBreakerState state) {
        collector
            .collect("halfOpenSuccessFul", state.getHalfOpenSuccessFulResultCounter())
            .collect("state", state.getCircuitState().name().charAt(0));
    }

    @Override
    public FaultToleranceConfig getConfig(InvocationContext context, Stereotypes stereotypes) {
        FaultToleranceApplicationState appState = getApplicationState(getApplicationContext(context));
        return appState.getConfig().updateAndGet(
                config -> config != null ? config : new BindableFaultToleranceConfig(stereotypes)).bindTo(context);
    }

    @Override
    public FaultToleranceMetrics getMetrics(InvocationContext context) {
        FaultToleranceApplicationState appState = getApplicationState(getApplicationContext(context));
        return appState.getMetrics().updateAndGet(
                metrics -> metrics != null ? metrics : new BindableFaultToleranceMetrics()).bindTo(context);
    }

    private ManagedExecutorService getManagedExecutorService() {
        return lookup(serviceConfig.getManagedExecutorService(), defaultExecutorService);
    }

    private ManagedScheduledExecutorService getManagedScheduledExecutorService() {
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

    private FaultToleranceApplicationState getApplicationState(String applicationName) {
        return stateByApplication.computeIfAbsent(applicationName, key -> new FaultToleranceApplicationState());
    }

    private BulkheadSemaphore getBulkheadExecutionSemaphore(String applicationName, Object invocationTarget, 
            Method annotatedMethod, int bulkheadValue) {
        return getApplicationState(applicationName).getBulkheadExecutionSemaphores()
                .computeIfAbsent(invocationTarget, key -> new ConcurrentHashMap<>())
                .computeIfAbsent( getFullMethodSignature(annotatedMethod), key -> new BulkheadSemaphore(bulkheadValue));
    }

    private BulkheadSemaphore getBulkheadExecutionQueueSemaphore(String applicationName, Object invocationTarget, 
            Method annotatedMethod, int bulkheadWaitingTaskQueue) {
        return getApplicationState(applicationName).getBulkheadExecutionQueueSemaphores()
                .computeIfAbsent(invocationTarget, key -> new ConcurrentHashMap<>())
                .computeIfAbsent( getFullMethodSignature(annotatedMethod), key -> new BulkheadSemaphore(bulkheadWaitingTaskQueue));
    }

    private CircuitBreakerState getCircuitBreakerState(String applicationName, Object invocationTarget, 
            Method annotatedMethod, int requestVolumeThreshold) {
        return getApplicationState(applicationName).getCircuitBreakerStates()
                .computeIfAbsent(invocationTarget, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(getFullMethodSignature(annotatedMethod), key -> new CircuitBreakerState(requestVolumeThreshold));
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

    private void startFaultToleranceSpan(RequestTraceSpan span, InvocationContext context) {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            addGenericFaultToleranceRequestTracingDetails(span, context);
            requestTracingService.startTrace(span);
        }
    }

    private void endFaultToleranceSpan() {
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


    /*
     * Execution
     */

    @Override
    public CircuitBreakerState getState(int requestVolumeThreshold, InvocationContext context) {
        return getCircuitBreakerState(getApplicationContext(context), context.getTarget(),
                context.getMethod(), requestVolumeThreshold);
    }

    @Override
    public BulkheadSemaphore getConcurrentExecutions(int maxConcurrentThreads, InvocationContext context) {
        return getBulkheadExecutionSemaphore(getApplicationContext(context),
                context.getTarget(), context.getMethod(), maxConcurrentThreads);
    }

    @Override
    public BulkheadSemaphore getWaitingQueuePopulation(int queueCapacity, InvocationContext context) {
        return getBulkheadExecutionQueueSemaphore(getApplicationContext(context),
                context.getTarget(), context.getMethod(), queueCapacity);
    }

    @Override
    public void delay(long delayMillis, InvocationContext context) throws InterruptedException {
        if (delayMillis <= 0) {
            return;
        }
        trace("delayRetry", context);
        try {
            Thread.sleep(delayMillis);
        } finally {
            endTrace();
        }
    }

    @Override
    public void runAsynchronous(CompletableFuture<Object> asyncResult, InvocationContext context, Callable<Object> task)
            throws RejectedExecutionException {
        Runnable completionTask = () -> {
            if (!asyncResult.isCancelled() && !Thread.currentThread().isInterrupted()) {
                try {
                    trace("runAsynchronous", context);
                    Future<?> futureResult = AsynchronousPolicy.toFuture(task.call());
                    if (!asyncResult.isCancelled()) { // could be cancelled in the meanwhile
                        if (!asyncResult.isDone()) {
                            asyncResult.complete(futureResult.get());
                        }
                    } else {
                        futureResult.cancel(true);
                    }
                } catch (Exception ex) {
                    // Note that even ExecutionException is not unpacked (intentionally)
                    asyncResult.completeExceptionally(ex); 
                } finally {
                    endTrace();
                }
            }
        };
        getManagedExecutorService().submit(completionTask);
    }

    @Override
    public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
        return getManagedScheduledExecutorService().schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, InvocationContext context,
            Exception ex) throws Exception {
        return CDI.current().select(fallbackClass).get().handle(
                new FaultToleranceExecutionContext(context.getMethod(), context.getParameters(), ex));
    }

    @Override
    public Object fallbackInvoke(Method fallbackMethod, InvocationContext context) throws Exception {
        try {
            fallbackMethod.setAccessible(true);
            return fallbackMethod.invoke(context.getTarget(), context.getParameters());
        } catch (InvocationTargetException e) {
            throw (Exception) e.getTargetException();
        } catch (IllegalAccessException e) {
            throw new FaultToleranceDefinitionException(e); // should not happen as we validated
        }
    }

    @Override
    public void trace(String method, InvocationContext context) {
        startFaultToleranceSpan(new RequestTraceSpan(method), context);
    }

    @Override
    public void endTrace() {
        endFaultToleranceSpan();
    }
}
