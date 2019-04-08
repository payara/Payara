/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance;

import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 */
@ContractsProvided(FaultToleranceExecution.class)
@Service(name = "microprofile-fault-tolerance-service")
@RunLevel(StartupRunLevel.VAL)
public class FaultToleranceService implements EventListener, FaultToleranceExecution {

    private static final Logger logger = Logger.getLogger(FaultToleranceService.class.getName());

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

    @PostConstruct
    public void postConstruct() {
        events.register(this);
        serviceConfig = serviceLocator.getService(FaultToleranceServiceConfiguration.class);
        invocationManager = serviceLocator.getService(InvocationManager.class);
        requestTracingService = serviceLocator.getService(RequestTracingService.class);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
        }
    }

    /**
     * Helper method that sets the enabled status for a given application.
     * @param applicationName The name of the application to register
     * @param serviceConfig The config to check for override values from
     */
    private void initialiseFaultToleranceObject(String applicationName) {
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has added the application");
        stateByApplication.computeIfAbsent(applicationName, key ->  new FaultToleranceApplicationState());
    }

    /**
     * Gets the configured ManagedExecutorService.
     * @return The configured ManagedExecutorService, or the default ManagedExecutorService if the configured one 
     * couldn't be found
     * @throws NamingException If the default ManagedExecutorService couldn't be found
     */
    private ManagedExecutorService getManagedExecutorService() throws NamingException {
        String managedExecutorServiceName = serviceConfig.getManagedExecutorService();
        InitialContext ctx = new InitialContext();

        ManagedExecutorService managedExecutorService;

        // If no name has been set, just get the default
        if (managedExecutorServiceName == null || managedExecutorServiceName.isEmpty()) {
            managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
        } else {
            try {
                managedExecutorService = (ManagedExecutorService) ctx.lookup(managedExecutorServiceName);
            } catch (NamingException ex) {
                logger.log(Level.INFO, "Could not find configured ManagedExecutorService, " 
                        + managedExecutorServiceName + ", so resorting to default", ex);
                managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
            } 
        }

        return managedExecutorService;
    }
    
    //TODO use the scheduler to schedule a clean of FT Info

    /**
     * Gets the configured ManagedScheduledExecutorService.
     * @return The configured ManagedExecutorService, or the default ManagedScheduledExecutorService if the configured 
     * one couldn't be found
     * @throws NamingException If the default ManagedScheduledExecutorService couldn't be found 
     */
    private ManagedScheduledExecutorService getManagedScheduledExecutorService() throws NamingException {
        String managedScheduledExecutorServiceName = serviceConfig
                .getManagedScheduledExecutorService();
        InitialContext ctx = new InitialContext();

        ManagedScheduledExecutorService managedScheduledExecutorService = null;

        // If no name has been set, just get the default
        if (managedScheduledExecutorServiceName == null || managedScheduledExecutorServiceName.isEmpty()) {
            managedScheduledExecutorService = (ManagedScheduledExecutorService) ctx.lookup(
                    "java:comp/DefaultManagedScheduledExecutorService");
        } else {
            try {
                managedScheduledExecutorService = (ManagedScheduledExecutorService) ctx.lookup(
                        managedScheduledExecutorServiceName);
            } catch (NamingException ex) {
                logger.log(Level.INFO, "Could not find configured ManagedScheduledExecutorService, " 
                        + managedScheduledExecutorServiceName + ", so resorting to default", ex);
                managedScheduledExecutorService = (ManagedScheduledExecutorService) ctx.lookup(
                        "java:comp/DefaultManagedScheduledExecutorService");
            } 
        }

        return managedScheduledExecutorService;   
    }

    /**
     * Gets the Bulkhead Execution Semaphore for a given application method, registering it to the 
     * FaultToleranceService if it hasn't already.
     * @param applicationName The name of the application
     * @param invocationTarget The target object obtained from InvocationContext.getTarget()
     * @param annotatedMethod The method that's annotated with @Bulkhead
     * @param bulkheadValue The value parameter of the Bulkhead annotation
     * @return The Semaphore for the given application method.
     */
    public BulkheadSemaphore getBulkheadExecutionSemaphore(String applicationName, Object invocationTarget, 
            Method annotatedMethod, int bulkheadValue) {
        BulkheadSemaphore bulkheadExecutionSemaphore;
        String fullMethodSignature = getFullMethodSignature(annotatedMethod);

        Map<String, BulkheadSemaphore> annotatedMethodSemaphores = null;

        try {
            annotatedMethodSemaphores = stateByApplication.get(applicationName).getBulkheadExecutionSemaphores()
                    .get(invocationTarget);
        } catch (NullPointerException npe) {
            logger.log(Level.FINE, "NPE caught trying to get semaphores for annotated method", npe);
        }

        // If there isn't a semaphore registered for this bean, register one, otherwise just return
        // the one already registered
        if (annotatedMethodSemaphores == null) {
            logger.log(Level.FINER, "No matching application or bean in bulkhead execution semaphore map, registering...");
            bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(applicationName, invocationTarget, 
                    fullMethodSignature, bulkheadValue);
        } else {
            bulkheadExecutionSemaphore = annotatedMethodSemaphores.get(fullMethodSignature);

            // If there isn't a semaphore registered for this method signature, register one, otherwise just return
            // the one already registered
            if (bulkheadExecutionSemaphore == null) {
                logger.log(Level.FINER, "No matching method signature in the bulkhead execution semaphore map, "
                        + "registering...");
                bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(applicationName, invocationTarget, 
                        fullMethodSignature, bulkheadValue);
            }
        }

        return bulkheadExecutionSemaphore;
    }

    /**
     * Helper method to create and register a Bulkhead Execution Semaphore for an annotated method
     * @param applicationName The name of the application
     * @param invocationTarget The target object obtained from InvocationContext.getTarget()
     * @param fullMethodSignature The method signature to register the semaphore against
     * @param bulkheadValue The size of the bulkhead
     * @return The Bulkhead Execution Semaphore for the given method signature and application
     */
    private synchronized BulkheadSemaphore createBulkheadExecutionSemaphore(String applicationName, Object invocationTarget, 
            String fullMethodSignature, int bulkheadValue) {

        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the application to "
                + "the bulkhead execution semaphore map");
        if (stateByApplication.get(applicationName).getBulkheadExecutionSemaphores().get(invocationTarget) == null) {
            logger.log(Level.FINER, "Registering bean to bulkhead execution semaphore map: {0}", 
                    invocationTarget);

            stateByApplication.get(applicationName).getBulkheadExecutionSemaphores().put(
                    invocationTarget, 
                    new ConcurrentHashMap<>());
        }

        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the annotated method "
                + "to the bulkhead execution semaphore map");
        if (stateByApplication.get(applicationName).getBulkheadExecutionSemaphores().get(invocationTarget)
                .get(fullMethodSignature) == null) {
            logger.log(Level.FINER, "Registering semaphore for method {0} to the bulkhead execution semaphore map", fullMethodSignature);
            stateByApplication.get(applicationName).getBulkheadExecutionSemaphores().get(invocationTarget)
            .put(fullMethodSignature, new BulkheadSemaphore(bulkheadValue));
        }

        return stateByApplication.get(applicationName).getBulkheadExecutionSemaphores().get(invocationTarget)
                .get(fullMethodSignature);
    }

    /**
     * Gets the Bulkhead Execution Queue Semaphore for a given application method, registering it to the 
     * FaultToleranceService if it hasn't already.
     * @param applicationName The name of the application
     * @param invocationTarget The target object obtained from InvocationContext.getTarget()
     * @param annotatedMethod The method that's annotated with @Bulkhead and @Asynchronous
     * @param bulkheadWaitingTaskQueue The waitingTaskQueue parameter of the Bulkhead annotation
     * @return The Semaphore for the given application method.
     */
    public BulkheadSemaphore getBulkheadExecutionQueueSemaphore(String applicationName, Object invocationTarget, 
            Method annotatedMethod, int bulkheadWaitingTaskQueue) {
        BulkheadSemaphore bulkheadExecutionQueueSemaphore;
        String fullMethodSignature = getFullMethodSignature(annotatedMethod);

        Map<String, BulkheadSemaphore> annotatedMethodExecutionQueueSemaphores = 
                stateByApplication.get(applicationName).getBulkheadExecutionQueueSemaphores().get(invocationTarget);

        // If there isn't a semaphore registered for this application name, register one, otherwise just return
        // the one already registered
        if (annotatedMethodExecutionQueueSemaphores == null) {
            logger.log(Level.FINER, "No matching application in the bulkhead execution semaphore map, registering...");
            bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(applicationName, invocationTarget, 
                    fullMethodSignature, bulkheadWaitingTaskQueue);
        } else {
            bulkheadExecutionQueueSemaphore = annotatedMethodExecutionQueueSemaphores.get(fullMethodSignature);

            // If there isn't a semaphore registered for this method signature, register one, otherwise just return
            // the one already registered
            if (bulkheadExecutionQueueSemaphore == null) {
                logger.log(Level.FINER, "No matching method signature in the bulkhead execution queue semaphore map, "
                        + "registering...");
                bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(applicationName, invocationTarget, 
                        fullMethodSignature, bulkheadWaitingTaskQueue);
            }
        }

        return bulkheadExecutionQueueSemaphore;
    }

    /**
     * Helper method to create and register a Bulkhead Execution Queue Semaphore for an annotated method
     * @param applicationName The name of the application
     * @param invocationTarget The target object obtained from InvocationContext.getTarget()
     * @param fullMethodSignature The method signature to register the semaphore against
     * @param bulkheadWaitingTaskQueue The size of the waiting task queue of the bulkhead
     * @return The Bulkhead Execution Queue Semaphore for the given method signature and application
     */
    private synchronized BulkheadSemaphore createBulkheadExecutionQueueSemaphore(String applicationName, Object invocationTarget, 
            String fullMethodSignature, int bulkheadWaitingTaskQueue) {
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the object to "
                + "the bulkhead execution queue semaphore map");
        FaultToleranceApplicationState applicationState = stateByApplication.get(applicationName);
        Map<Object, Map<String, BulkheadSemaphore>> applicationSemaphores = applicationState.getBulkheadExecutionQueueSemaphores();
        if (applicationSemaphores.get(invocationTarget) == null) {
            logger.log(Level.FINER, "Registering object to the bulkhead execution queue semaphore map: {0}", 
                    invocationTarget);
            applicationSemaphores.put(invocationTarget, new ConcurrentHashMap<>());
        }

        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the annotated method "
                + "to the bulkhead execution queue semaphore map");
        if (applicationSemaphores.get(invocationTarget).get(fullMethodSignature) == null) {
            logger.log(Level.FINER, "Registering semaphore for method {0} to the bulkhead execution semaphore map", 
                    fullMethodSignature);
            applicationSemaphores.get(invocationTarget).put(fullMethodSignature,
                    new BulkheadSemaphore(bulkheadWaitingTaskQueue));
        }

        return applicationSemaphores.get(invocationTarget).get(fullMethodSignature);
    }

    /**
     * Gets the CircuitBreakerState object for a given application name and method.If a CircuitBreakerState hasn't been 
     * registered for the given application name and method, it will register the given CircuitBreaker.
     * @param applicationName The name of the application
     * @param invocationTarget The target object obtained from InvocationContext.getTarget()
     * @param annotatedMethod The method annotated with @CircuitBreaker
     * @param circuitBreaker The @CircuitBreaker annotation from the annotated method
     * @return The CircuitBreakerState for the given application and method
     */
    private CircuitBreakerState getCircuitBreakerState(String applicationName, Object invocationTarget, 
            Method annotatedMethod, int requestVolumeThreshold) {
        CircuitBreakerState circuitBreakerState;
        String fullMethodSignature = getFullMethodSignature(annotatedMethod);

        Map<String, CircuitBreakerState> annotatedMethodCircuitBreakerStates = 
                stateByApplication.get(applicationName).getCircuitBreakerStates().get(invocationTarget);

        // If there isn't a CircuitBreakerState registered for this application name, register one, otherwise just 
        // return the one already registered
        if (annotatedMethodCircuitBreakerStates == null) {
            logger.log(Level.FINER, "No matching object in the circuit breaker states map, registering...");
            circuitBreakerState = registerCircuitBreaker(applicationName, invocationTarget, fullMethodSignature, 
                    requestVolumeThreshold);
        } else {
            circuitBreakerState = annotatedMethodCircuitBreakerStates.get(fullMethodSignature);

            // If there isn't a CircuitBreakerState registered for this method, register one, otherwise just 
            // return the one already registered
            if (circuitBreakerState == null) {
                logger.log(Level.FINER, "No matching method in the circuit breaker states map, registering...");
                circuitBreakerState = registerCircuitBreaker(applicationName, invocationTarget, fullMethodSignature, 
                        requestVolumeThreshold);
            }
        }

        return circuitBreakerState;
    }

    /**
     * Helper method to create and register a CircuitBreakerState object for an annotated method
     * @param applicationName The application name to register the CircuitBreakerState against
     * @param fullMethodSignature The method signature to register the CircuitBreakerState against
     * @param bulkheadWaitingTaskQueue The CircuitBreaker annotation of the annotated method
     * @return The CircuitBreakerState object for the given method signature and application
     */
    private synchronized CircuitBreakerState registerCircuitBreaker(String applicationName, Object invocationTarget, 
            String fullMethodSignature, int requestVolumeThreshold) {
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the object "
                + "to the circuit breaker states map");
        Map<Object, Map<String, CircuitBreakerState>> applicationStates = stateByApplication.get(applicationName).getCircuitBreakerStates();
        Map<String, CircuitBreakerState> targetStates = applicationStates.get(invocationTarget);
        if (targetStates == null) {
            logger.log(Level.FINER, "Registering application to the circuit breaker states map: {0}", 
                    invocationTarget);
            applicationStates.put(invocationTarget, new ConcurrentHashMap<>());
        }

        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the annotated method "
                + "to the circuit breaker states map");
        if (targetStates.get(fullMethodSignature) == null) {
            logger.log(Level.FINER, "Registering CircuitBreakerState for method {0} to the circuit breaker states map", 
                    fullMethodSignature);
            targetStates
            .put(fullMethodSignature, new CircuitBreakerState(requestVolumeThreshold));
        }

        return targetStates.get(fullMethodSignature);
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
    private String getApplicationName(InvocationContext context) {
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

    private void startFaultToleranceSpan(RequestTraceSpan span, InvocationContext invocationContext) {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            addGenericFaultToleranceRequestTracingDetails(span, invocationContext);
            requestTracingService.startTrace(span);
        }
    }

    private void endFaultToleranceSpan() {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            requestTracingService.endTrace();
        }
    }

    private void addGenericFaultToleranceRequestTracingDetails(RequestTraceSpan span, 
            InvocationContext invocationContext) {
        span.addSpanTag("App Name", invocationManager.getCurrentInvocation().getAppName());
        span.addSpanTag("Component ID", invocationManager.getCurrentInvocation().getComponentId());
        span.addSpanTag("Module Name", invocationManager.getCurrentInvocation().getModuleName());
        span.addSpanTag("Class Name", invocationContext.getMethod().getDeclaringClass().getName());
        span.addSpanTag("Method Name", invocationContext.getMethod().getName());
    }


    /*
     * Execution
     */

    @Override
    public CircuitBreakerState getState(int requestVolumeThreshold, InvocationContext context) {
        return getCircuitBreakerState(getApplicationName(context), context.getTarget(),
                context.getMethod(), requestVolumeThreshold);
    }

    /**
     * Helper method that schedules the CircuitBreaker state to be set to HalfOpen after the configured delay
     * @param delayMillis The number of milliseconds to wait before setting the state
     * @param circuitBreakerState The CircuitBreakerState to set the state of
     * @throws NamingException If the ManagedScheduledExecutor couldn't be found
     */
    @Override
    public void scheduleHalfOpen(long delayMillis, CircuitBreakerState circuitBreakerState) throws NamingException {
        Runnable halfOpen = () -> {
            circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.HALF_OPEN);
            logger.log(Level.FINE, "Setting CircuitBreaker state to half open");
        };
        getManagedScheduledExecutorService().schedule(halfOpen, delayMillis, TimeUnit.MILLISECONDS);
        logger.log(Level.FINER, "CircuitBreaker half open state scheduled in {0} milliseconds", delayMillis);
    }

    @Override
    public BulkheadSemaphore getExecutionSemaphoreOf(int maxConcurrentThreads, InvocationContext context) {
        return getBulkheadExecutionSemaphore(getApplicationName(context),
                context.getTarget(), context.getMethod(), maxConcurrentThreads);
    }

    @Override
    public BulkheadSemaphore getWaitingQueueSemaphoreOf(int queueCapacity, InvocationContext context) {
        return getBulkheadExecutionQueueSemaphore(getApplicationName(context),
                context.getTarget(), context.getMethod(), queueCapacity);
    }

    @Override
    public Future<?> runAsynchronous(InvocationContext context) throws Exception {
        return getManagedExecutorService().submit(() -> context.proceed());
    }

    @Override
    public Future<?> timeoutIn(long millis) throws Exception {
        final Thread thread = Thread.currentThread();
        return getManagedScheduledExecutorService().schedule(thread::interrupt, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startTrace(String method, InvocationContext context) {
        startFaultToleranceSpan(new RequestTraceSpan(method), context);
    }

    @Override
    public void endTrace() {
        endFaultToleranceSpan();
    }
}
