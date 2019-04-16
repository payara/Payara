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
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

/**
 * Interceptor for the Fault Tolerance Bulkhead Annotation.
 * 
 * @author Andrew Pielage
 * @author Jan Bernitt (2.0 update)
 */
@Interceptor
@Bulkhead
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
public class BulkheadInterceptor extends BaseFaultToleranceInterceptor<Bulkhead> implements Serializable {

    public BulkheadInterceptor() {
        super(Bulkhead.class, true);
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled for this
            // method
            if (getConfig().isNonFallbackEnabled(context) && getConfig().isEnabled(Bulkhead.class, context)) {
                if (getConfig().isMetricsEnabled(context)) {
                    // Only increment the invocations metric if the Retry annotation isn't present
                    if (getConfig().getAnnotation(Retry.class, context) == null) {
                        getMetrics().incrementInvocationsTotal(Bulkhead.class, context);
                    }
                }


                logger.log(Level.FINER, "Proceeding invocation with bulkhead semantics");
                resultValue = bulkhead(context);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled, proceeding normally without bulkhead.");
                resultValue = context.proceed();
            }
        } catch (Exception ex) {
            Retry retry = getConfig().getAnnotation(Retry.class, context);

            if (retry != null) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            }
            // If an exception was thrown, check if the method is annotated with @Fallback
            Fallback fallback = getConfig().getAnnotation(Fallback.class, context);

            // If the method was annotated with Fallback and the annotation is enabled, attempt it, otherwise just 
            // propagate the exception upwards
            if (fallback != null && getConfig().isEnabled(Fallback.class, context)) {
                logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                        + "falling back from Bulkhead");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, getConfig(), getExecution(), getMetrics(), context);
                resultValue = fallbackPolicy.fallback(context, ex);
            } else {
                logger.log(Level.FINE, "Fallback annotation not found on method, propagating error upwards.", ex);

                // Increment the failure counter metric
                getMetrics().incrementInvocationsFailedTotal(Bulkhead.class, context);
                throw ex;
            }
        }

        return resultValue;
    }

    /**
     * Proceeds the context under Bulkhead semantics.
     * @param context The context to proceed.
     * @return The outcome of the invocationContext
     * @throws Exception 
     */
    private Object bulkhead(InvocationContext context) throws Exception {
        Object resultValue = null;

        Bulkhead bulkhead = getConfig().getAnnotation(Bulkhead.class, context);

        BulkheadSemaphore bulkheadExecutionSemaphore = getExecution().getExecutionSemaphoreOf(getConfig().value(bulkhead, context), context);

        if (getConfig().isMetricsEnabled(context)) {
            getMetrics().insertBulkheadConcurrentExecutions(bulkheadExecutionSemaphore::acquiredPermits, context);
        }

        // If the Asynchronous annotation is present, use threadpool style, otherwise use semaphore style
        if (getConfig().getAnnotation(Asynchronous.class, context) != null) {
            BulkheadSemaphore bulkheadExecutionQueueSemaphore = getExecution().getWaitingQueueSemaphoreOf(getConfig().waitingTaskQueue(bulkhead, context), context);

            if (getConfig().isMetricsEnabled(context)) {
                getMetrics().insertBulkheadWaitingQueuePopulation(bulkheadExecutionQueueSemaphore::acquiredPermits, context);
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
                        getExecution().startTrace("obtainBulkheadSemaphore", context);
                        try {
                            bulkheadExecutionSemaphore.acquire();
                        } finally {
                            // Make sure we end the trace right here
                            getExecution().endTrace();

                            // Record the queue time for MP Metrics
                            getMetrics().addBulkheadWaitingDuration(System.nanoTime() - queueStartTime, context);
                        }

                        logger.log(Level.FINER, "Acquired bulkhead queue semaphore.");

                        // Release the queue permit
                        bulkheadExecutionQueueSemaphore.release();

                        // Incremement the MP Metrics callsAccepted counter
                        getMetrics().incrementBulkheadCallsAcceptedTotal(context);

                        // Start measuring the execution duration for MP Metrics
                        long executionStartTime = System.nanoTime();

                        // Proceed the invocation and wait for the response
                        try {
                            logger.log(Level.FINER, "Proceeding bulkhead context");
                            resultValue = context.proceed();
                        } catch (Exception ex) {
                            logger.log(Level.FINE, "Exception proceeding Bulkhead context", ex);

                            // Generic catch, as we need to release the semaphore permits
                            bulkheadExecutionSemaphore.release();
                            bulkheadExecutionQueueSemaphore.release();

                            // Record the execution time for MP Metrics              
                            getMetrics().addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);

                            // Let the exception propagate further up - we just want to release the semaphores
                            throw ex;
                        }

                        // Record the execution time for MP Metrics
                        getMetrics().addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);

                        // Release the execution permit
                        bulkheadExecutionSemaphore.release();
                    } catch (InterruptedException ex) {
                        // Incremement the MP Metrics callsRejected counter
                        getMetrics().incrementBulkheadCallsRejectedTotal(context);

                        logger.log(Level.INFO, "Interrupted acquiring bulkhead semaphore", ex);
                        throw new BulkheadException(ex);
                    }
                } else {
                    // Incremement the MP Metrics callsRejected counter
                    getMetrics().incrementBulkheadCallsRejectedTotal(context);

                    throw new BulkheadException("No free work or queue permits.");
                }
            } else {
                // Incremement the MP Metrics callsAccepted counter
                getMetrics().incrementBulkheadCallsAcceptedTotal(context);

                // Record the queue time for MP Metrics
                getMetrics().addBulkheadWaitingDuration(System.nanoTime() - queueStartTime, context);

                // Start measuring the execution duration for MP Metrics
                long executionStartTime = System.nanoTime();

                // Proceed the invocation and wait for the response
                try {
                    logger.log(Level.FINER, "Proceeding bulkhead context");
                    resultValue = context.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception proceeding Bulkhead context", ex);

                    // Generic catch, as we need to release the semaphore permits
                    bulkheadExecutionSemaphore.release();

                    // Record the execution time for MP Metrics
                    getMetrics().addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);

                    // Let the exception propagate further up - we just want to release the semaphores
                    throw ex;
                }

                // Record the execution time for MP Metrics
                getMetrics().addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);

                // Release the permit
                bulkheadExecutionSemaphore.release();
            }
        } else {
            // Try to get an execution permit
            if (bulkheadExecutionSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
                // Incremement the MP Metrics callsAccepted counter
                getMetrics().incrementBulkheadCallsAcceptedTotal(context);

                // Start measuring the execution duration for MP Metrics
                long executionStartTime = System.nanoTime();

                // Proceed the invocation and wait for the response
                try {
                    logger.log(Level.FINER, "Proceeding bulkhead context");
                    resultValue = context.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception proceeding Bulkhead context", ex);

                    // Generic catch, as we need to release the semaphore permits
                    bulkheadExecutionSemaphore.release();

                    // Record the execution time for MP Metrics
                    getMetrics().addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);

                    // Let the exception propagate further up - we just want to release the semaphores
                    throw ex;
                }

                // Record the execution time for MP Metrics
                getMetrics().addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);

                // Release the permit
                bulkheadExecutionSemaphore.release();
            } else {
                // Incremement the MP Metrics callsRejected counter
                getMetrics().incrementBulkheadCallsRejectedTotal(context);

                throw new BulkheadException("No free work permits.");
            }
        }

        return resultValue;
    }
}
