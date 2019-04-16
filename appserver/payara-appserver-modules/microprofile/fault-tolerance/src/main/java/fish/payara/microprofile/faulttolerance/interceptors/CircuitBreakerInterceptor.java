/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2017-2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@CircuitBreaker
@Priority(Interceptor.Priority.PLATFORM_AFTER + 15)
public class CircuitBreakerInterceptor extends BaseFaultToleranceInterceptor<CircuitBreaker> implements Serializable {

    public CircuitBreakerInterceptor() {
        super(CircuitBreaker.class, true);
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        // Attempt to proceed the invocation with CircuitBreaker semantics if Fault Tolerance is enabled for this method
        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled
            if (getConfig().isNonFallbackEnabled(context) && getConfig().isEnabled(CircuitBreaker.class, context)) {
                // Only increment the invocations metric if the Retry and Bulkhead annotations aren't present
                if (getConfig().getAnnotation(Bulkhead.class, context) == null
                        && getConfig().getAnnotation(Retry.class, context) == null) {
                    getMetrics().incrementInvocationsTotal(CircuitBreaker.class, context);
                }

                logger.log(Level.FINER, "Proceeding invocation with circuitbreaker semantics");
                resultValue = circuitBreak(context);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled, proceeding normally without circuitbreaker.");
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
                        + "falling back from CircuitBreaker");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, getConfig(), getExecution(), getMetrics(), context);
                resultValue = fallbackPolicy.fallback(context, ex);
            } else {
                // Increment the failure counter metric
                getMetrics().incrementInvocationsFailedTotal(CircuitBreaker.class, context);
                throw ex;
            }
        }

        return resultValue;
    }

    private Object circuitBreak(InvocationContext context) throws Exception {
        Object resultValue = null;

        CircuitBreaker circuitBreaker = getConfig().getAnnotation(CircuitBreaker.class, context);

        Class<? extends Throwable>[] failOn = getConfig().failOn(circuitBreaker, context);
        long delay = getConfig().delay(circuitBreaker, context);
        ChronoUnit delayUnit = getConfig().delayUnit(circuitBreaker, context);
        int requestVolumeThreshold = getConfig().requestVolumeThreshold(circuitBreaker, context);
        double failureRatio = getConfig().failureRatio(circuitBreaker, context);
        int successThreshold = getConfig().successThreshold(circuitBreaker, context);

        long delayMillis = Duration.of(delay, delayUnit).toMillis();

        CircuitBreakerState circuitBreakerState = getExecution().getState(requestVolumeThreshold, context);

        if (getConfig().isMetricsEnabled(context)) {
            getMetrics().insertCircuitbreakerOpenTotal(circuitBreakerState::isOpen, context);
            getMetrics().insertCircuitbreakerHalfOpenTotal(circuitBreakerState::isHalfOpen, context);
            getMetrics().insertCircuitbreakerClosedTotal(circuitBreakerState::isClosed, context);
        }

        switch (circuitBreakerState.getCircuitState()) {
            case OPEN:
                logger.log(Level.FINER, "CircuitBreaker is Open, throwing exception");
                getMetrics().incrementCircuitbreakerCallsPreventedTotal(context);

                // If open, immediately throw an error
                throw new CircuitBreakerOpenException("CircuitBreaker for method "
                        + context.getMethod().getName() + " is in state OPEN.");
            case CLOSED:
                // If closed, attempt to proceed the invocation context
                try {
                    logger.log(Level.FINER, "Proceeding CircuitBreaker context");
                    resultValue = context.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception executing CircuitBreaker context");

                    // Check if the exception is something that should record a failure
                    if (shouldFail(failOn, ex)) {
                        logger.log(Level.FINE, "Caught exception is included in CircuitBreaker failOn, "
                                + "recording failure against CircuitBreaker");
                        // Add a failure result to the queue
                        circuitBreakerState.recordClosedResult(Boolean.FALSE);
                        getMetrics().incrementCircuitbreakerCallsFailedTotal(context);

                        // Calculate the failure ratio, and if we're over it, open the circuit
                        breakCircuitIfRequired(
                                Math.round(requestVolumeThreshold * failureRatio),
                                circuitBreakerState, delayMillis, context);
                    }

                    // Finally, propagate the error upwards
                    throw ex;
                }

                // If everything is bon, add a success value
                circuitBreakerState.recordClosedResult(Boolean.TRUE);
                getMetrics().incrementCircuitbreakerCallsSucceededTotal(context);

                // Calculate the failure ratio, and if we're over it, open the circuit
                breakCircuitIfRequired(
                        Math.round(requestVolumeThreshold * failureRatio),
                        circuitBreakerState, delayMillis, context);
                break;
            case HALF_OPEN:
                // If half-open, attempt to proceed the invocation context
                try {
                    logger.log(Level.FINER, "Proceeding half open CircuitBreaker context");
                    resultValue = context.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception executing CircuitBreaker context");

                    // Check if the exception is something that should record a failure
                    if (shouldFail(failOn, ex)) {
                        logger.log(Level.FINE, "Caught exception is included in CircuitBreaker failOn, "
                                + "reopening half open circuit");

                        getMetrics().incrementCircuitbreakerCallsFailedTotal(context);

                        // Open the circuit again, and reset the half-open result counter
                        circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.OPEN);
                        circuitBreakerState.resetHalfOpenSuccessfulResultCounter();
                        getExecution().scheduleDelayed(delayMillis, circuitBreakerState::halfOpen);
                    }

                    throw ex;
                }

                // If the invocation context hasn't thrown an error, record a success
                circuitBreakerState.incrementHalfOpenSuccessfulResultCounter();
                getMetrics().incrementCircuitbreakerCallsSucceededTotal(context);

                logger.log(Level.FINER, "Number of consecutive successful circuitbreaker executions = {0}",
                        circuitBreakerState.getHalfOpenSuccessFulResultCounter());

                // If we've hit the success threshold, close the circuit
                if (circuitBreakerState.getHalfOpenSuccessFulResultCounter() == successThreshold) {
                    logger.log(Level.FINE, "Number of consecutive successful CircuitBreaker executions is above "
                            + "threshold {0}, closing circuit", successThreshold);

                    circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.CLOSED);

                    // Reset the counter for when we next need to use it
                    circuitBreakerState.resetHalfOpenSuccessfulResultCounter();

                    // Reset the rolling results window
                    circuitBreakerState.resetResults();
                }

                break;
        }

        return resultValue;
    }

    /**
     * Helper method that checks whether or not the given exception is included in the failOn parameter.
     * @param failOn The array to check for the exception in.
     * @param ex The exception to check
     * @return True if the exception is included in the array
     */
    private boolean shouldFail(Class<? extends Throwable>[] failOn, Exception ex) {
        boolean shouldFail = false;

        if (failOn[0] != Throwable.class) {
            for (Class<? extends Throwable> failureClass : failOn) {
                if (ex.getClass() == failureClass) {
                    logger.log(Level.FINER, "Exception {0} matches a Throwable in failOn",
                            ex.getClass().getSimpleName());
                    shouldFail = true;
                    break;
                }
                try {
                    // If we there isn't a direct match, check if the exception is a subclass
                    ex.getClass().asSubclass(failureClass);
                    shouldFail = true;

                    logger.log(Level.FINER, "Exception {0} is a child of a Throwable in retryOn: {1}",
                            new String[]{ex.getClass().getSimpleName(), failureClass.getSimpleName()});
                    break;
                } catch (ClassCastException cce) {
                    // Om nom nom
                }
            }
        } else {
            shouldFail = true;
        }

        return shouldFail;
    }

    private void breakCircuitIfRequired(long failureThreshold, CircuitBreakerState circuitBreakerState,
            long delayMillis, InvocationContext context) throws Exception {
        // If we're over the failure threshold, open the circuit
        if (circuitBreakerState.isOverFailureThreshold(failureThreshold)) {
            logger.log(Level.FINE, "CircuitBreaker is over failure threshold {0}, opening circuit",
                    failureThreshold);

            // Update the circuit state and metric timers
            circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.OPEN);

            // Update the opened metric counter
            getMetrics().incrementCircuitbreakerOpenedTotal(context);

            // Kick off a thread that will half-open the circuit after the specified delay
            getExecution().scheduleDelayed(delayMillis, circuitBreakerState::halfOpen);
        }
    }

}
