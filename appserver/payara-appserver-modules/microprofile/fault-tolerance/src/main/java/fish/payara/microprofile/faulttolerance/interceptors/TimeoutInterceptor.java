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
package fish.payara.microprofile.faulttolerance.interceptors;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.NamingException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Timeout
@Priority(Interceptor.Priority.PLATFORM_AFTER + 20)
public class TimeoutInterceptor {
    
    private static final Logger logger = Logger.getLogger(TimeoutInterceptor.class.getName());
    
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
                Timeout.class);
        
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
                    && (FaultToleranceCdiUtils.getEnabledOverrideValue(
                            config, Timeout.class, invocationContext)
                            .orElse(Boolean.TRUE))) {
                // Only increment the invocations metric if the Retry, Bulkhead, and CircuitBreaker annotations aren't present
                if (FaultToleranceCdiUtils.getAnnotation(beanManager, Bulkhead.class, invocationContext) == null
                        && FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext) == null
                        && FaultToleranceCdiUtils.getAnnotation(
                                beanManager, CircuitBreaker.class, invocationContext) == null) {
                    faultToleranceService.incrementCounterMetric(metricRegistry, 
                            "ft." + fullMethodSignature + ".invocations.total", appName, config);
                }
                
                logger.log(Level.FINER, "Proceeding invocation with timeout semantics");
                proceededInvocationContext = timeout(invocationContext);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled for {0}, proceeding normally without timeout.", 
                        faultToleranceService.getApplicationName(invocationManager, invocationContext));
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            Retry retry = FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext);
            
            if (retry != null) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            }
            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, 
                    invocationContext);

            // Only fall back if the annotation hasn't been disabled
            if (fallback != null && (FaultToleranceCdiUtils.getEnabledOverrideValue(
                    config, Fallback.class, invocationContext)
                    .orElse(Boolean.TRUE))) {
                logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                        + "falling back from Timeout");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                proceededInvocationContext = fallbackPolicy.fallback(invocationContext, ex);
            } else {
                // Increment the failure counter metric
                faultToleranceService.incrementCounterMetric(metricRegistry, 
                        "ft." + fullMethodSignature + ".invocations.failed.total", 
                        faultToleranceService.getApplicationName(invocationManager, invocationContext), 
                        config);
                
                throw ex;
            }
        }
        
        return proceededInvocationContext;
    }

    /**
     * Proceeds the given invocation context with Timeout semantics.
     * @param invocationContext The invocation context to proceed.
     * @return The result of the invocation context.
     * @throws Exception If the invocation context execution throws an exception
     */
    private Object timeout(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        Timeout timeout = FaultToleranceCdiUtils.getAnnotation(beanManager, Timeout.class, invocationContext);
        
        MetricRegistry metricRegistry = CDI.current().select(MetricRegistry.class).get();
        String fullMethodSignature = FaultToleranceCdiUtils.getFullAnnotatedMethodSignature(invocationContext, 
                Timeout.class);
        String appName = faultToleranceService.getApplicationName(
                Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class), 
                invocationContext);
        
        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        long value = FaultToleranceCdiUtils.getOverrideValue(
                config, Timeout.class, "value", invocationContext, Long.class)
                .orElse(timeout.value());
        // Look for a String and cast to ChronoUnit - Use the Common Sense Convertor
        ChronoUnit unit = FaultToleranceCdiUtils.getOverrideValue(
                config, Timeout.class, "unit", invocationContext, String.class).map(ChronoUnit::valueOf)
                .orElse(timeout.unit());

        Future<?> timeoutFuture = null;
        long timeoutMillis = Duration.of(value, unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutMillis;
        long executionStartTime = System.nanoTime();
        
        try {
            timeoutFuture = startTimeout(timeoutMillis);
            proceededInvocationContext = invocationContext.proceed();
            stopTimeout(timeoutFuture);

            if (System.currentTimeMillis() > timeoutTime) {
                // Record the timeout for MP Metrics
                faultToleranceService.incrementCounterMetric(metricRegistry, 
                        "ft." + fullMethodSignature + ".timeout.callsTimedOut.total", appName, config);
                
                logger.log(Level.FINE, "Execution timed out");
                throw new TimeoutException();
            }
            
            // Record the execution time for MP Metrics
            faultToleranceService.updateHistogramMetric(metricRegistry, 
                    "ft." + fullMethodSignature + ".timeout.executionDuration", 
                    System.nanoTime() - executionStartTime,
                    appName, config);
            // Record the successfuly completion for MP Metrics
            faultToleranceService.incrementCounterMetric(metricRegistry, 
                    "ft." + fullMethodSignature + ".timeout.callsNotTimedOut.total", appName, config);
        } catch (InterruptedException ie) {
            // Record the execution time for MP Metrics
            faultToleranceService.updateHistogramMetric(metricRegistry, 
                    "ft." + fullMethodSignature + ".timeout.executionDuration", 
                    System.nanoTime() - executionStartTime,
                    appName, config);
            
            if (System.currentTimeMillis() > timeoutTime) {
                // Record the timeout for MP Metrics
                faultToleranceService.incrementCounterMetric(metricRegistry, 
                        "ft." + fullMethodSignature + ".timeout.callsTimedOut.total", appName, config);
                
                logger.log(Level.FINE, "Execution timed out");
                throw new TimeoutException(ie);
            }
        } catch (Exception ex) {
            // Record the execution time for MP Metrics
            faultToleranceService.updateHistogramMetric(metricRegistry, 
                    "ft." + fullMethodSignature + ".timeout.executionDuration", 
                    System.nanoTime() - executionStartTime,
                    appName, config);
            
            // Deal with cases where someone has caught the thread.interrupt() and thrown the exception as something else
            if (ex.getCause() instanceof InterruptedException && System.currentTimeMillis() > timeoutTime) {
                // Record the timeout for MP Metrics
                faultToleranceService.incrementCounterMetric(metricRegistry, 
                        "ft." + fullMethodSignature + ".timeout.callsTimedOut.total", appName, config);
                
                logger.log(Level.FINE, "Execution timed out");
                throw new TimeoutException(ex);
            }
            
            stopTimeout(timeoutFuture);
            throw ex;
        }
        
        return proceededInvocationContext;
    }

    /**
     * Helper method that schedules a thread interrupt after a period of time.
     * @param timeoutMillis The time in milliseconds to wait before interrupting.
     * @param timedOut A threadlocal that stores whether or not the interrupt has occurred.
     * @return A future that can be cancelled if the method execution completes before the interrupt happens
     * @throws NamingException If the configured ManagedScheduledExecutorService could not be found
     */
    private static Future<?> startTimeout(long timeoutMillis) throws NamingException {
        final Thread thread = Thread.currentThread();
        
        Runnable timeoutTask = () -> {
            thread.interrupt();
        };

        FaultToleranceService faultToleranceService = Globals.getDefaultBaseServiceLocator()
                .getService(FaultToleranceService.class);
        ManagedScheduledExecutorService managedScheduledExecutorService = faultToleranceService.
                getManagedScheduledExecutorService();

        return managedScheduledExecutorService.schedule(timeoutTask, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Helper method that stops the scheduled interrupt.
     * @param timeoutFuture The scheduled interrupt to cancel.
     */
    private static void stopTimeout(Future<?> timeoutFuture) {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
        }
    }
}
