/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance;

import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "microprofile-fault-tolerance-service")
@RunLevel(StartupRunLevel.VAL)
public class FaultToleranceService implements EventListener {
    
    public final static String FAULT_TOLERANCE_ENABLED_PROPERTY = "MP_Fault_Tolerance_NonFallback_Enabled";
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    FaultToleranceServiceConfiguration faultToleranceServiceConfiguration;
    
    @Inject
    ServiceLocator habitat;
    
    private final Map<String, Boolean> enabledMap;
    private final Map<String, Map<Method, Semaphore>> bulkheadExecutionSemaphores;
    private final Map<String, Map<Method, Semaphore>> bulkheadExecutionQueueSemaphores;
    private final Map<String, Map<Method, CircuitBreakerState>> circuitBreakerStates;
    
    public FaultToleranceService() {
        bulkheadExecutionSemaphores = new ConcurrentHashMap();
        bulkheadExecutionQueueSemaphores = new ConcurrentHashMap();
        circuitBreakerStates = new ConcurrentHashMap();
        enabledMap = new ConcurrentHashMap();
    }
    
    @PostConstruct
    public void postConstruct() {
        faultToleranceServiceConfiguration = habitat.getService(FaultToleranceServiceConfiguration.class);
    }
    
    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterCircuitBreaker(info.getName());
        }
    }
    
    public Boolean isFaultToleranceEnabled(String applicationName, Config config) {
        if (enabledMap.containsKey(applicationName)) {
            return enabledMap.get(applicationName);
        } else {
            setFaultToleranceEnabled(applicationName, config);
            return enabledMap.get(applicationName);
        }
    }
    
    private void setFaultToleranceEnabled(String applicationName, Config config) {
        enabledMap.put(applicationName, 
                config.getOptionalValue(FAULT_TOLERANCE_ENABLED_PROPERTY, Boolean.class).orElse(Boolean.TRUE));
    }
    
    public ManagedExecutorService getManagedExecutorService() throws NamingException {
        String managedExecutorServiceName = faultToleranceServiceConfiguration.getManagedExecutorService();
        InitialContext ctx = new InitialContext();
        
        ManagedExecutorService managedExecutorService = null;
        
        if (managedExecutorServiceName == null || managedExecutorServiceName.isEmpty()) {
            managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
        } else {
            try {
                managedExecutorService = (ManagedExecutorService) ctx.lookup(managedExecutorServiceName);
            } catch (NamingException ex) {
                managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
            } 
        }
        
        return managedExecutorService;
    }
    
    public ManagedScheduledExecutorService getManagedScheduledExecutorService() throws NamingException {
        String managedScheduledExecutorServiceName = faultToleranceServiceConfiguration.getManagedScheduledExecutorService();
        InitialContext ctx = new InitialContext();
        
        ManagedScheduledExecutorService managedScheduledExecutorService = null;
        
        if (managedScheduledExecutorServiceName == null || managedScheduledExecutorServiceName.isEmpty()) {
            managedScheduledExecutorService = (ManagedScheduledExecutorService) ctx.lookup(
                    "java:comp/DefaultManagedScheduledExecutorService");
        } else {
            try {
                managedScheduledExecutorService = (ManagedScheduledExecutorService) ctx.lookup(
                        managedScheduledExecutorServiceName);
            } catch (NamingException ex) {
                managedScheduledExecutorService = (ManagedScheduledExecutorService) ctx.lookup(
                        "java:comp/DefaultManagedScheduledExecutorService");
            } 
        }
        
        return managedScheduledExecutorService;
        
    }
    
    public Semaphore getBulkheadExecutionSemaphore(String applicationName, Method annotatedMethod, int bulkheadValue) {
        Semaphore bulkheadExecutionSemaphore;
        
        Map<Method, Semaphore> annotatedMethodSemaphores = bulkheadExecutionSemaphores.get(applicationName);
        
        if (annotatedMethodSemaphores == null) {
            bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(applicationName, annotatedMethod,
                    bulkheadValue);
        } else {
            bulkheadExecutionSemaphore = annotatedMethodSemaphores.get(annotatedMethod);
        
            if (bulkheadExecutionSemaphore == null) {
                bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(applicationName, annotatedMethod,
                        bulkheadValue);
            }
        }
        
        return bulkheadExecutionSemaphore;
    }
    
    private Semaphore createBulkheadExecutionSemaphore(String applicationName, Method annotatedMethod, 
            int bulkheadValue) {
        Semaphore bulkheadExecutionSemaphore = new Semaphore(bulkheadValue);
        
        Map annotatedMethodSemaphores = new ConcurrentHashMap();
        annotatedMethodSemaphores.put(annotatedMethod, bulkheadExecutionSemaphore);
        
        bulkheadExecutionSemaphores.put(applicationName, annotatedMethodSemaphores);
        return bulkheadExecutionSemaphore;
    }
    
    public Semaphore getBulkheadExecutionQueueSemaphore(String applicationName, Method annotatedMethod,
            int bulkheadWaitingTaskQueue) {
        Semaphore bulkheadExecutionQueueSemaphore;
        
        Map<Method, Semaphore> annotatedMethodExecutionQueueSemaphores = 
                bulkheadExecutionQueueSemaphores.get(applicationName);
        
        if (annotatedMethodExecutionQueueSemaphores == null) {
            bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(applicationName, annotatedMethod,
                    bulkheadWaitingTaskQueue);
        } else {
            bulkheadExecutionQueueSemaphore = annotatedMethodExecutionQueueSemaphores.get(annotatedMethod);
        
            if (bulkheadExecutionQueueSemaphore == null) {
                bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(applicationName, 
                        annotatedMethod, bulkheadWaitingTaskQueue);
            }
        }
        
        return bulkheadExecutionQueueSemaphore;
    }
    
    private Semaphore createBulkheadExecutionQueueSemaphore(String applicationName, Method annotatedMethod,
            int bulkheadWaitingTaskQueue) {
        Semaphore bulkheadExecutionQueueSemaphore = new Semaphore(bulkheadWaitingTaskQueue);
        
        Map annotatedMethodExecutionQueueSemaphores = new ConcurrentHashMap();
        annotatedMethodExecutionQueueSemaphores.put(annotatedMethod, bulkheadExecutionQueueSemaphore);
        
        bulkheadExecutionQueueSemaphores.put(applicationName, annotatedMethodExecutionQueueSemaphores);
        return bulkheadExecutionQueueSemaphore;
    }
    
    public CircuitBreakerState getCircuitBreakerState(String applicationName, Method annotatedMethod, 
            CircuitBreaker circuitBreaker) {
        CircuitBreakerState circuitBreakerState;
        
        Map<Method, CircuitBreakerState> annotatedMethodCircuitBreakerStates = 
                circuitBreakerStates.get(applicationName);

        if (annotatedMethodCircuitBreakerStates == null) {
            circuitBreakerState = registerCircuitBreaker(applicationName, annotatedMethod, circuitBreaker);
        } else {
            circuitBreakerState = annotatedMethodCircuitBreakerStates.get(annotatedMethod);
        
            if (circuitBreakerState == null) {
                circuitBreakerState = registerCircuitBreaker(applicationName, annotatedMethod, circuitBreaker);
            }
        }
        
        return circuitBreakerState;
    }
    
    private CircuitBreakerState registerCircuitBreaker(String applicationName, Method annotatedMethod, 
            CircuitBreaker circuitBreaker) {
        CircuitBreakerState circuitBreakerState = 
                new CircuitBreakerState(circuitBreaker.requestVolumeThreshold());
        
        Map annotatedMethodCircuitBreakerStates = new ConcurrentHashMap();
        annotatedMethodCircuitBreakerStates.put(annotatedMethod, circuitBreakerState);
        
        circuitBreakerStates.put(applicationName, annotatedMethodCircuitBreakerStates);
        return circuitBreakerState;
    }

    private void deregisterCircuitBreaker(String applicationName) {
        circuitBreakerStates.remove(applicationName);
    }
}
