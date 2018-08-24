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
package fish.payara.microprofile.faulttolerance.interceptors;

import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import java.io.Serializable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import fish.payara.notification.requesttracing.RequestTraceSpan;
import javax.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 * Interceptor for the Fault Tolerance Bulkhead Annotation.
 * 
 * @author Andrew Pielage
 */
@Interceptor
@Bulkhead
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
public class BulkheadInterceptor implements Serializable {
    
    private static final Logger logger = Logger.getLogger(BulkheadInterceptor.class.getName());
    
    @Inject
    private BeanManager beanManager;
    
    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        
        MetricRegistry metricRegistry = CDI.current().select(MetricRegistry.class).get();
        String fullMethodSignature = FaultToleranceCdiUtils.getFullAnnotatedMethodSignature(invocationContext, 
                Bulkhead.class);
        
        Config config = null;
        
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        try {
            String appName = faultToleranceService.getApplicationName(invocationManager, invocationContext);
            
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled for this
            // method
            if (faultToleranceService.isFaultToleranceEnabled(appName, config)
                    && ((Boolean) FaultToleranceCdiUtils.getEnabledOverrideValue(
                            config, Bulkhead.class, invocationContext)
                            .orElse(Boolean.TRUE))) {
                // Only increment the invocations metric if the Retry annotation isn't present
                if (FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext) == null) {
                    metricRegistry.counter("ft." + fullMethodSignature + ".invocations.total").inc();
                }
                
                logger.log(Level.FINER, "Proceeding invocation with bulkhead semantics");
                proceededInvocationContext = bulkhead(invocationContext);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled for {0}, proceeding normally without bulkhead.", 
                        faultToleranceService.getApplicationName(invocationManager, invocationContext));
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            Retry retry = FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext);
            
            if (retry != null) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            } else {
                // If an exception was thrown, check if the method is annotated with @Fallback
                Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, 
                        invocationContext);

                // If the method was annotated with Fallback and the annotation is enabled, attempt it, otherwise just 
                // propagate the exception upwards
                if (fallback != null && ((Boolean) FaultToleranceCdiUtils.getEnabledOverrideValue(
                        config, Fallback.class, invocationContext)
                        .orElse(Boolean.TRUE))) {
                    logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                            + "falling back from Bulkhead");
                    FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                    proceededInvocationContext = fallbackPolicy.fallback(invocationContext, ex);
                } else {
                    logger.log(Level.FINE, "Fallback annotation not found on method, propagating error upwards.", ex);
                    
                    // Increment the failure counter metric
                    metricRegistry.counter("ft." + fullMethodSignature + ".invocations.failed.total").inc();
                    
                    throw ex;
                }
            }
        }
        
        return proceededInvocationContext;
    }
    
    /**
     * Proceeds the context under Bulkhead semantics.
     * @param invocationContext The context to proceed.
     * @return The outcome of the invocationContext
     * @throws Exception 
     */
    private Object bulkhead(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        Bulkhead bulkhead = FaultToleranceCdiUtils.getAnnotation(beanManager, Bulkhead.class, invocationContext);
        
        Config config = null;
        
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        int value = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, Bulkhead.class, "value", invocationContext, Integer.class)
                .orElse(bulkhead.value());
        int waitingTaskQueue = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, Bulkhead.class, "waitingTaskQueue", invocationContext, Integer.class)
                .orElse(bulkhead.waitingTaskQueue());
        
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        
        String appName = faultToleranceService.getApplicationName(invocationManager, invocationContext);
        
        
        MetricRegistry metricRegistry = CDI.current().select(MetricRegistry.class).get();
        String fullMethodSignature = FaultToleranceCdiUtils.getFullAnnotatedMethodSignature(invocationContext, 
                Bulkhead.class);
        
        Semaphore bulkheadExecutionSemaphore = faultToleranceService.getBulkheadExecutionSemaphore(appName,
                invocationContext.getMethod(), value);
        
        Gauge<Long> concurrentExecutionsGauge = metricRegistry.getGauges()
                .get("ft." + fullMethodSignature + ".bulkhead.concurrentExecutions");
        
        // Register a bulkhead concurrent executions metric if there isn't one
        if (concurrentExecutionsGauge == null) {
            concurrentExecutionsGauge = () -> getConcurrentExecutionsCount(value, bulkheadExecutionSemaphore);
            
            metricRegistry.register("ft." + fullMethodSignature + ".bulkhead.concurrentExecutions", 
                    concurrentExecutionsGauge);
        }
        
        // If the Asynchronous annotation is present, use threadpool style, otherwise use semaphore style
        if (FaultToleranceCdiUtils.getAnnotation(beanManager, Asynchronous.class, invocationContext) != null) {
            Semaphore bulkheadExecutionQueueSemaphore = faultToleranceService.getBulkheadExecutionQueueSemaphore(
                    appName, invocationContext.getMethod(), waitingTaskQueue);

            Gauge<Long> waitingQueueGauge = metricRegistry.getGauges()
                    .get("ft." + fullMethodSignature + ".bulkhead.waitingQueue.population");
        
            // Register a bulkhead queue metric if there isn't one
            if (waitingQueueGauge == null) {
                waitingQueueGauge = () -> getWaitingQueueCount(waitingTaskQueue, bulkheadExecutionQueueSemaphore);

                metricRegistry.register("ft." + fullMethodSignature + ".bulkhead.waitingQueue.population", 
                        waitingQueueGauge);
            }
            
            // Start measuring the queue duration for MP Metrics
            long queueStartTime = System.nanoTime();
            
            // Check if there are any free permits for concurrent execution
            if (!bulkheadExecutionSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
                logger.log(Level.FINER, "Attempting to acquire bulkhead queue semaphore.");
                // If there aren't any free permits, see if there are any free queue permits
                if (bulkheadExecutionQueueSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
                    logger.log(Level.FINER, "Acquired bulkhead queue semaphore.");
                    
                    // If there is a free queue permit, queue for an executor permit
                    try {
                        logger.log(Level.FINER, "Attempting to acquire bulkhead execution semaphore.");
                        faultToleranceService.startFaultToleranceSpan(new RequestTraceSpan("obtainBulkheadSemaphore"),
                                invocationManager, invocationContext);
                        try {
                            bulkheadExecutionSemaphore.acquire();
                        } finally {
                            // Make sure we end the trace right here
                            faultToleranceService.endFaultToleranceSpan();
                            
                            // Record the queue time for MP Metrics
                            metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.waiting.duration").update(
                                    System.nanoTime() - queueStartTime);
                        }
                        
                        logger.log(Level.FINER, "Acquired bulkhead queue semaphore.");
                        
                        // Release the queue permit
                        bulkheadExecutionQueueSemaphore.release();
                        
                        // Incremement the MP Metrics callsAccepted counter
                        metricRegistry.counter("ft." + fullMethodSignature + ".bulkhead.callsAccepted.total").inc();
                        
                        // Start measuring the execution duration for MP Metrics
                        long executionStartTime = System.nanoTime();
                        
                        // Proceed the invocation and wait for the response
                        try {
                            logger.log(Level.FINER, "Proceeding bulkhead context");
                            proceededInvocationContext = invocationContext.proceed();
                        } catch (Exception ex) {
                            logger.log(Level.FINE, "Exception proceeding Bulkhead context", ex);
                            
                            // Generic catch, as we need to release the semaphore permits
                            bulkheadExecutionSemaphore.release();
                            bulkheadExecutionQueueSemaphore.release();
                            
                            // Record the execution time for MP Metrics
                            metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.executionDuration").update(
                                    System.nanoTime() - executionStartTime);
                            
                            // Let the exception propagate further up - we just want to release the semaphores
                            throw ex;
                        }

                        // Record the execution time for MP Metrics
                        metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.executionDuration").update(
                                System.nanoTime() - executionStartTime);
                        
                        // Release the execution permit
                        bulkheadExecutionSemaphore.release();
                    } catch (InterruptedException ex) {
                        // Incremement the MP Metrics callsRejected counter
                        metricRegistry.counter("ft." + fullMethodSignature + ".bulkhead.callsRejected.total").inc();
                        
                        logger.log(Level.INFO, "Interrupted acquiring bulkhead semaphore", ex);
                        throw new BulkheadException(ex);
                    }
                } else {
                    // Incremement the MP Metrics callsRejected counter
                    metricRegistry.counter("ft." + fullMethodSignature + ".bulkhead.callsRejected.total").inc();
                    throw new BulkheadException("No free work or queue permits.");
                }
            } else {
                // Incremement the MP Metrics callsAccepted counter
                metricRegistry.counter("ft." + fullMethodSignature + ".bulkhead.callsAccepted.total").inc();
                
                // Record the queue time for MP Metrics
                metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.waiting.duration").update(
                        System.nanoTime() - queueStartTime);
                
                // Start measuring the execution duration for MP Metrics
                long executionStartTime = System.nanoTime();
                
                // Proceed the invocation and wait for the response
                try {
                    logger.log(Level.FINER, "Proceeding bulkhead context");
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception proceeding Bulkhead context", ex);

                    // Generic catch, as we need to release the semaphore permits
                    bulkheadExecutionSemaphore.release();
                    
                    // Record the execution time for MP Metrics
                    metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.executionDuration").update(
                            System.nanoTime() - executionStartTime);
                    
                    // Let the exception propagate further up - we just want to release the semaphores
                    throw ex;
                }
                
                // Record the execution time for MP Metrics
                metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.executionDuration").update(
                        System.nanoTime() - executionStartTime);
                
                // Release the permit
                bulkheadExecutionSemaphore.release();
            }
        } else {
            // Try to get an execution permit
            if (bulkheadExecutionSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
                // Incremement the MP Metrics callsAccepted counter
                metricRegistry.counter("ft." + fullMethodSignature + ".bulkhead.callsAccepted.total").inc();
                
                // Start measuring the execution duration for MP Metrics
                long executionStartTime = System.nanoTime();
                
                // Proceed the invocation and wait for the response
                try {
                    logger.log(Level.FINER, "Proceeding bulkhead context");
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception proceeding Bulkhead context", ex);
                    
                    // Generic catch, as we need to release the semaphore permits
                    bulkheadExecutionSemaphore.release();

                    // Record the execution time for MP Metrics
                    metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.executionDuration").update(
                            System.nanoTime() - executionStartTime);
                    
                    // Let the exception propagate further up - we just want to release the semaphores
                    throw ex;
                }
                
                // Record the execution time for MP Metrics
                metricRegistry.histogram("ft." + fullMethodSignature + ".bulkhead.executionDuration").update(
                        System.nanoTime() - executionStartTime);
                
                // Release the permit
                bulkheadExecutionSemaphore.release();
            } else {
                // Incremement the MP Metrics callsRejected counter
                metricRegistry.counter("ft." + fullMethodSignature + ".bulkhead.callsRejected.total").inc();
                
                throw new BulkheadException("No free work permits.");
            }
        }
        
        return proceededInvocationContext;
    }
    
    private Long getConcurrentExecutionsCount(int bulkheadValue, Semaphore bulkheadExecutionSemaphore) {
        return ((Number) (bulkheadValue - bulkheadExecutionSemaphore.availablePermits())).longValue();
    }
    
    private Long getWaitingQueueCount(int waitingTaskQueue, Semaphore bulkheadExecutionQueueSemaphore) {
        return ((Number) (waitingTaskQueue - bulkheadExecutionQueueSemaphore.availablePermits())).longValue();
    }
}
