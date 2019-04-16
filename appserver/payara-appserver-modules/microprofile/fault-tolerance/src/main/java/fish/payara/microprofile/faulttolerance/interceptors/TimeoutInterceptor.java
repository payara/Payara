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

import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Timeout
@Priority(Interceptor.Priority.PLATFORM_AFTER + 20)
public class TimeoutInterceptor extends BaseFaultToleranceInterceptor<Timeout> {

    public TimeoutInterceptor() {
        super(Timeout.class, true);
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled for this
            // method
            if (getConfig().isNonFallbackEnabled(context) && getConfig().isEnabled(Timeout.class, context)) {
                // Only increment the invocations metric if the Retry, Bulkhead, and CircuitBreaker annotations aren't present
                if (getConfig().getAnnotation(Bulkhead.class, context) == null
                        && getConfig().getAnnotation(Retry.class, context) == null
                        && getConfig().getAnnotation(CircuitBreaker.class, context) == null) {
                    getMetrics().incrementInvocationsTotal(Timeout.class, context);
                }

                logger.log(Level.FINER, "Proceeding invocation with timeout semantics");
                resultValue = timeout(context);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled, proceeding normally without timeout.");
                resultValue = context.proceed();
            }
        } catch (Exception ex) {
            Retry retry = getConfig().getAnnotation(Retry.class, context);

            if (retry != null) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            }
            Fallback fallback = getConfig().getAnnotation(Fallback.class, context);

            // Only fall back if the annotation hasn't been disabled
            if (fallback != null && getConfig().isEnabled(Fallback.class, context)) {
                logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                        + "falling back from Timeout");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, getConfig(), getExecution(), getMetrics(), context);
                resultValue = fallbackPolicy.fallback(context, ex);
            } else {
                // Increment the failure counter metric
                getMetrics().incrementInvocationsFailedTotal(Timeout.class, context);
                throw ex;
            }
        }
        return resultValue;
    }

    /**
     * Proceeds the given invocation context with Timeout semantics.
     * @param context The invocation context to proceed.
     * @return The result of the invocation context.
     * @throws Exception If the invocation context execution throws an exception
     */
    private Object timeout(InvocationContext context) throws Exception {
        Object resultValue = null;

        Timeout timeout = getConfig().getAnnotation(Timeout.class, context);

        long value = getConfig().value(timeout, context);
        ChronoUnit unit = getConfig().unit(timeout, context);

        Future<?> timeoutFuture = null;
        long timeoutMillis = Duration.of(value, unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutMillis;
        long executionStartTime = System.nanoTime();

        try {
            timeoutFuture = getExecution().scheduleDelayed(timeoutMillis, Thread.currentThread()::interrupt);
            resultValue = context.proceed();
            stopTimeout(timeoutFuture);

            if (System.currentTimeMillis() > timeoutTime) {
                // Record the timeout for MP Metrics
                getMetrics().incrementTimeoutCallsTimedOutTotal(context);
                logger.log(Level.FINE, "Execution timed out");
                throw new TimeoutException();
            }

            // Record the execution time for MP Metrics
            getMetrics().addTimeoutExecutionDuration(System.nanoTime() - executionStartTime, context);
            // Record the successfuly completion for MP Metrics
            getMetrics().incrementTimeoutCallsNotTimedOutTotal(context);
        } catch (InterruptedException ie) {
            // Record the execution time for MP Metrics
            getMetrics().addTimeoutExecutionDuration(System.nanoTime() - executionStartTime, context);

            if (System.currentTimeMillis() > timeoutTime) {
                // Record the timeout for MP Metrics
                getMetrics().incrementTimeoutCallsTimedOutTotal(context);
                logger.log(Level.FINE, "Execution timed out");
                throw new TimeoutException(ie);
            }
        } catch (Exception ex) {
            // Record the execution time for MP Metrics
            getMetrics().addTimeoutExecutionDuration(System.nanoTime() - executionStartTime, context);

            // Deal with cases where someone has caught the thread.interrupt() and thrown the exception as something else
            if (ex.getCause() instanceof InterruptedException && System.currentTimeMillis() > timeoutTime) {
                // Record the timeout for MP Metrics
                getMetrics().incrementTimeoutCallsTimedOutTotal(context);
                logger.log(Level.FINE, "Execution timed out");
                throw new TimeoutException(ex);
            }

            stopTimeout(timeoutFuture);
            throw ex;
        }

        return resultValue;
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
