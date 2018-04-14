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

import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 * Base Service for MicroProfile Fault Tolerance.
 * 
 * @author Andrew Pielage
 */
@Service(name = "microprofile-fault-tolerance-service")
@RunLevel(StartupRunLevel.VAL)
public class FaultToleranceService implements EventListener {
    
    public static final String FAULT_TOLERANCE_ENABLED_PROPERTY = "MP_Fault_Tolerance_NonFallback_Enabled";
    public static final String FALLBACK_HANDLER_METHOD_NAME = "handle";
    
    private static final Logger logger = Logger.getLogger(FaultToleranceService.class.getName());
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    FaultToleranceServiceConfiguration faultToleranceServiceConfiguration;
    
    @Inject
    RequestTracingService requestTracingService;
    
    @Inject
    ServiceLocator habitat;
    
    @Inject
    Events events;
    
    private final Map<String, Boolean> enabledMap;
    private final Map<String, Map<String, Semaphore>> bulkheadExecutionSemaphores;
    private final Map<String, Map<String, Semaphore>> bulkheadExecutionQueueSemaphores;
    private final Map<String, Map<String, CircuitBreakerState>> circuitBreakerStates;
    
    public FaultToleranceService() {
        enabledMap = new ConcurrentHashMap<>();
        bulkheadExecutionSemaphores  = new ConcurrentHashMap<>();
        bulkheadExecutionQueueSemaphores = new ConcurrentHashMap<>();
        circuitBreakerStates = new ConcurrentHashMap<>();
    }
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
        faultToleranceServiceConfiguration = habitat.getService(FaultToleranceServiceConfiguration.class);
        requestTracingService = habitat.getService(RequestTracingService.class);
    }
    
    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
        }
    }
    
    /**
     * Checks whether fault tolerance is enabled for a given application, setting its enabled value if it doesn't
     * already have one.
     * @param applicationName The application to check if Fault Tolerance is enabled for.
     * @param config The application config to check for any override values when setting the enabled status for an
     * unregistered application.
     * @return True if Fault Tolerance is enabled for the given application name
     */
    public Boolean isFaultToleranceEnabled(String applicationName, Config config) {
        try {
            if (enabledMap.containsKey(applicationName)) {
                return enabledMap.get(applicationName);
            } else {
                setFaultToleranceEnabled(applicationName, config);
                return enabledMap.get(applicationName);
            }
        } catch (NullPointerException npe) {
            setFaultToleranceEnabled(applicationName, config);
            return enabledMap.get(applicationName);
        }
        
    }
    
    /**
     * Helper method that sets the enabled status for a given application.
     * @param applicationName The name of the application to register
     * @param config The config to check for override values from
     */
    private synchronized void setFaultToleranceEnabled(String applicationName, Config config) {
        try {
            // Double lock as multiple methods can get inside the calling if at the same time
            logger.log(Level.FINER, "Checking double lock to see if something else has added the application");
            if (!enabledMap.containsKey(applicationName)) {
                if (config != null) {
                    // Set the enabled value to the override value from the config, or true if it isn't configured
                    enabledMap.put(applicationName, 
                        config.getOptionalValue(FAULT_TOLERANCE_ENABLED_PROPERTY, Boolean.class).orElse(Boolean.TRUE));
                } else {
                    logger.log(Level.FINE, "No config found, so enabling fault tolerance for application: {0}",
                            applicationName);
                    enabledMap.put(applicationName, Boolean.TRUE);
                }
            }
        } catch (NullPointerException npe) {
            logger.log(Level.FINE, "Caught null pointer attempting to register application: " + applicationName, npe);
            if (config != null) {
                // Set the enabled value to the override value from the config, or true if it isn't configured
                enabledMap.put(applicationName, 
                    config.getOptionalValue(FAULT_TOLERANCE_ENABLED_PROPERTY, Boolean.class).orElse(Boolean.TRUE));
            } else {
                logger.log(Level.FINE, "No config found, so enabling fault tolerance for application: {0}",
                        applicationName);
                enabledMap.put(applicationName, Boolean.TRUE);
            }
        }
    }
    
    /**
     * Gets the configured ManagedExecutorService.
     * @return The configured ManagedExecutorService, or the default ManagedExecutorService if the configured one 
     * couldn't be found
     * @throws NamingException If the default ManagedExecutorService couldn't be found
     */
    public ManagedExecutorService getManagedExecutorService() throws NamingException {
        String managedExecutorServiceName = faultToleranceServiceConfiguration.getManagedExecutorService();
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
    
    /**
     * Gets the configured ManagedScheduledExecutorService.
     * @return The configured ManagedExecutorService, or the default ManagedScheduledExecutorService if the configured 
     * one couldn't be found
     * @throws NamingException If the default ManagedScheduledExecutorService couldn't be found 
     */
    public ManagedScheduledExecutorService getManagedScheduledExecutorService() throws NamingException {
        String managedScheduledExecutorServiceName = faultToleranceServiceConfiguration
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
     * @param annotatedMethod The method that's annotated with @Bulkhead
     * @param bulkheadValue The value parameter of the Bulkhead annotation
     * @return The Semaphore for the given application method.
     */
    public Semaphore getBulkheadExecutionSemaphore(String applicationName, Method annotatedMethod, int bulkheadValue) {
        Semaphore bulkheadExecutionSemaphore;
        String fullMethodSignature = getFullMethodSignature(annotatedMethod);
        
        Map<String, Semaphore> annotatedMethodSemaphores = bulkheadExecutionSemaphores.get(applicationName);
        
        // If there isn't a semaphore registered for this application name, register one, otherwise just return
        // the one already registered
        if (annotatedMethodSemaphores == null) {
            logger.log(Level.FINER, "No matching application in the bulkhead execution semaphore map, registering...");
            bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(applicationName, fullMethodSignature,
                    bulkheadValue);
        } else {
            bulkheadExecutionSemaphore = annotatedMethodSemaphores.get(fullMethodSignature);
        
            // If there isn't a semaphore registered for this method signature, register one, otherwise just return
            // the one already registered
            if (bulkheadExecutionSemaphore == null) {
                logger.log(Level.FINER, "No matching method signature in the bulkhead execution semaphore map, "
                        + "registering...");
                bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(applicationName, fullMethodSignature,
                        bulkheadValue);
            }
        }
        
        return bulkheadExecutionSemaphore;
    }
    
    /**
     * Helper method to create and register a Bulkhead Execution Semaphore for an annotated method
     * @param applicationName The application name to register the semaphore against
     * @param fullMethodSignature The method signature to register the semaphore against
     * @param bulkheadValue The size of the bulkhead
     * @return The Bulkhead Execution Semaphore for the given method signature and application
     */
    private synchronized Semaphore createBulkheadExecutionSemaphore(String applicationName, String fullMethodSignature, 
            int bulkheadValue) {
        
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the application to "
                + "the bulkhead execution semaphore map");
        if (bulkheadExecutionSemaphores.get(applicationName) == null) {
            logger.log(Level.FINER, "Registering application to the bulkhead execution semaphore map: {0}", 
                    applicationName);
            bulkheadExecutionSemaphores.put(applicationName, new ConcurrentHashMap<>());
        }
        
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the annotated method "
                + "to the bulkhead execution semaphore map");
        if (bulkheadExecutionSemaphores.get(applicationName).get(fullMethodSignature) == null) {
            logger.log(Level.FINER, "Registering semaphore for method {0} to the bulkhead execution semaphore map for "
                    + "application {1}", new String[]{fullMethodSignature, applicationName});
            bulkheadExecutionSemaphores.get(applicationName).put(fullMethodSignature, new Semaphore(bulkheadValue, 
                    true));
        }

        return bulkheadExecutionSemaphores.get(applicationName).get(fullMethodSignature);
    }
    
    /**
     * Gets the Bulkhead Execution Queue Semaphore for a given application method, registering it to the 
     * FaultToleranceService if it hasn't already.
     * @param applicationName The name of the application
     * @param annotatedMethod The method that's annotated with @Bulkhead and @Asynchronous
     * @param bulkheadWaitingTaskQueue The waitingTaskQueue parameter of the Bulkhead annotation
     * @return The Semaphore for the given application method.
     */
    public Semaphore getBulkheadExecutionQueueSemaphore(String applicationName, Method annotatedMethod,
            int bulkheadWaitingTaskQueue) {
        Semaphore bulkheadExecutionQueueSemaphore;
        String fullMethodSignature = getFullMethodSignature(annotatedMethod);
        
        Map<String, Semaphore> annotatedMethodExecutionQueueSemaphores = 
                bulkheadExecutionQueueSemaphores.get(applicationName);
        
        // If there isn't a semaphore registered for this application name, register one, otherwise just return
        // the one already registered
        if (annotatedMethodExecutionQueueSemaphores == null) {
            logger.log(Level.FINER, "No matching application in the bulkhead execution semaphore map, registering...");
            bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(applicationName, 
                    fullMethodSignature, bulkheadWaitingTaskQueue);
        } else {
            bulkheadExecutionQueueSemaphore = annotatedMethodExecutionQueueSemaphores.get(fullMethodSignature);
        
            // If there isn't a semaphore registered for this method signature, register one, otherwise just return
            // the one already registered
            if (bulkheadExecutionQueueSemaphore == null) {
                logger.log(Level.FINER, "No matching method signature in the bulkhead execution queue semaphore map, "
                        + "registering...");
                bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(applicationName, 
                        fullMethodSignature, bulkheadWaitingTaskQueue);
            }
        }
        
        return bulkheadExecutionQueueSemaphore;
    }
    
    /**
     * Helper method to create and register a Bulkhead Execution Queue Semaphore for an annotated method
     * @param applicationName The application name to register the semaphore against
     * @param fullMethodSignature The method signature to register the semaphore against
     * @param bulkheadWaitingTaskQueue The size of the waiting task queue of the bulkhead
     * @return The Bulkhead Execution Queue Semaphore for the given method signature and application
     */
    private synchronized Semaphore createBulkheadExecutionQueueSemaphore(String applicationName, 
            String fullMethodSignature, int bulkheadWaitingTaskQueue) {
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the application to "
                + "the bulkhead execution queue semaphore map");
        if (bulkheadExecutionQueueSemaphores.get(applicationName) == null) {
            logger.log(Level.FINER, "Registering application to the bulkhead execution queue semaphore map: {0}", 
                    applicationName);
            bulkheadExecutionQueueSemaphores.put(applicationName, new ConcurrentHashMap<>());
        }
        
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the annotated method "
                + "to the bulkhead execution queue semaphore map");
        if (bulkheadExecutionQueueSemaphores.get(applicationName).get(fullMethodSignature) == null) {
            logger.log(Level.FINER, "Registering semaphore for method {0} to the bulkhead execution semaphore map for "
                    + "application {1}", new String[]{fullMethodSignature, applicationName});
            bulkheadExecutionQueueSemaphores.get(applicationName)
                    .put(fullMethodSignature, new Semaphore(bulkheadWaitingTaskQueue, true));
        }

        return bulkheadExecutionQueueSemaphores.get(applicationName).get(fullMethodSignature);
    }
    
    /**
     * Gets the CircuitBreakerState object for a given application name and method. If a CircuitBreakerState hasn't
     * been registered for the given application name and method, it will register the given CircuitBreaker.
     * @param applicationName The application name
     * @param annotatedMethod The method annotated with @CircuitBreaker
     * @param circuitBreaker The @CircuitBreaker annotation from the annotated method
     * @return The CircuitBreakerState for the given application and method
     */
    public CircuitBreakerState getCircuitBreakerState(String applicationName, Method annotatedMethod, 
            CircuitBreaker circuitBreaker) {
        CircuitBreakerState circuitBreakerState;
        String fullMethodSignature = getFullMethodSignature(annotatedMethod);
        
        Map<String, CircuitBreakerState> annotatedMethodCircuitBreakerStates = 
                circuitBreakerStates.get(applicationName);

        // If there isn't a CircuitBreakerState registered for this application name, register one, otherwise just 
        // return the one already registered
        if (annotatedMethodCircuitBreakerStates == null) {
            logger.log(Level.FINER, "No matching application in the circuit breaker states map, registering...");
            circuitBreakerState = registerCircuitBreaker(applicationName, fullMethodSignature, circuitBreaker);
        } else {
            circuitBreakerState = annotatedMethodCircuitBreakerStates.get(fullMethodSignature);
        
            // If there isn't a CircuitBreakerState registered for this method, register one, otherwise just 
            // return the one already registered
            if (circuitBreakerState == null) {
                logger.log(Level.FINER, "No matching method in the circuit breaker states map, registering...");
                circuitBreakerState = registerCircuitBreaker(applicationName, fullMethodSignature, circuitBreaker);
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
    private synchronized CircuitBreakerState registerCircuitBreaker(String applicationName, String fullMethodSignature, 
            CircuitBreaker circuitBreaker) {
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the application "
                + "to the circuit breaker states map");
        if (circuitBreakerStates.get(applicationName) == null) {
            logger.log(Level.FINER, "Registering application to the circuit breaker states map: {0}", 
                    applicationName);
            circuitBreakerStates.put(applicationName, new ConcurrentHashMap<>());
        }
        
        // Double lock as multiple methods can get inside the calling if at the same time
        logger.log(Level.FINER, "Checking double lock to see if something else has already added the annotated method "
                + "to the circuit breaker states map");
        if (circuitBreakerStates.get(applicationName).get(fullMethodSignature) == null) {
            logger.log(Level.FINER, "Registering CircuitBreakerState for method {0} to the circuit breaker states map "
                    + "for application {1}", new String[]{fullMethodSignature, applicationName});
            circuitBreakerStates.get(applicationName)
                    .put(fullMethodSignature, new CircuitBreakerState(circuitBreaker.requestVolumeThreshold()));
        }

        return circuitBreakerStates.get(applicationName).get(fullMethodSignature);
    }

    /**
     * Removes an application from the enabled map, CircuitBreaker map, and bulkhead maps
     * @param applicationName The name of the application to remove
     */
    private void deregisterApplication(String applicationName) {
        enabledMap.remove(applicationName);
        deregisterCircuitBreaker(applicationName);
        deregisterBulkhead(applicationName);
    }
    
    /**
     * Removes the given application name from the circuit breaker states map
     * @param applicationName The name of the application to remove
     */
    private void deregisterCircuitBreaker(String applicationName) {
        circuitBreakerStates.remove(applicationName);
    }
    
    /**
     * Removes the given application name from the bulkhead maps
     * @param applicationName The name of the application to remove
     */
    private void deregisterBulkhead(String applicationName) {
        bulkheadExecutionSemaphores.remove(applicationName);
        bulkheadExecutionQueueSemaphores.remove(applicationName);
    }
    
    /**
     * Gets the application name from the invocation manager. Failing that, it will use the module name, component name,
     * or method signature (in that order).
     * @param invocationManager The invocation manager to get the application name from
     * @param invocationContext The context of the current invocation
     * @return The application name
     */
    public String getApplicationName(InvocationManager invocationManager, InvocationContext invocationContext) {
        String appName = invocationManager.getCurrentInvocation().getAppName();
        if (appName == null) {
            appName = invocationManager.getCurrentInvocation().getModuleName();

            if (appName == null) {
                appName = invocationManager.getCurrentInvocation().getComponentId();

                if (appName == null) {
                    appName = getFullMethodSignature(invocationContext.getMethod());
                }
            }
        }
        
        return appName;
    }
    
    /**
     * Helper method to generate a full method signature consisting of canonical class name, method name, 
     * parameter types, and return type.
     * @param annotatedMethod The annotated Method to generate the signature for
     * @return A String in the format of CanonicalClassName#MethodName({ParameterTypes})>ReturnType
     */
    private String getFullMethodSignature(Method annotatedMethod) {
        return annotatedMethod.getDeclaringClass().getCanonicalName() 
                + "#" + annotatedMethod.getName() 
                + "(" + Arrays.toString(annotatedMethod.getParameterTypes()) + ")"
                + ">" + annotatedMethod.getReturnType().getSimpleName();
    }
    
    public void startFaultToleranceSpan(RequestTraceSpan span, InvocationManager invocationManager,
            InvocationContext invocationContext) {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            addGenericFaultToleranceRequestTracingDetails(span, invocationManager, invocationContext);
            requestTracingService.startTrace(span);
        }
    }
    
    public void endFaultToleranceSpan() {
        if (requestTracingService != null && requestTracingService.isRequestTracingEnabled()) {
            requestTracingService.endTrace();
        }
    }
    
    private void addGenericFaultToleranceRequestTracingDetails(RequestTraceSpan span, 
            InvocationManager invocationManager, InvocationContext invocationContext) {
        span.addSpanTag("App Name", invocationManager.getCurrentInvocation().getAppName());
        span.addSpanTag("Component ID", invocationManager.getCurrentInvocation().getComponentId());
        span.addSpanTag("Module Name", invocationManager.getCurrentInvocation().getModuleName());
        span.addSpanTag("Class Name", invocationContext.getMethod().getDeclaringClass().getName());
        span.addSpanTag("Method Name", invocationContext.getMethod().getName());
    }
}
