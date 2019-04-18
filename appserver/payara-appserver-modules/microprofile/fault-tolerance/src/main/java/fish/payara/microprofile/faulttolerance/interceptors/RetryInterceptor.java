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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Retry
@Priority(Interceptor.Priority.PLATFORM_AFTER + 5)
public class RetryInterceptor extends BaseFaultToleranceInterceptor<Retry> {

    public RetryInterceptor() {
        super(Retry.class, false);
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled for this
            // method
            if (getConfig().isNonFallbackEnabled() && getConfig().isEnabled(Retry.class)) {
                // Increment the invocations metric
                getMetrics().incrementInvocationsTotal();

                logger.log(Level.FINER, "Proceeding invocation with retry semantics");
                resultValue = retry(context);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled, proceeding normally without retry.");
                resultValue = context.proceed();
            }
        } catch (Exception ex) {
            Fallback fallback = getConfig().getAnnotation(Fallback.class);

            // Only fall back if the annotation hasn't been disabled
            if (fallback != null && getConfig().isEnabled(Fallback.class)) {
                logger.log(Level.FINE, "Fallback annotation found on method - falling back from Retry");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, getConfig(), getExecution(), getMetrics(), context);
                resultValue = fallbackPolicy.fallback(context, ex);
            } else {
                // Increment the failure counter metric
                getMetrics().incrementInvocationsFailedTotal();
                throw ex;
            }
        }

        return resultValue;
    }

    /**
     * Proceeds the given invocation context with Retry semantics.
     * @param context The invocation context to proceed
     * @return The proceeded invocation context
     * @throws Exception If the invocation throws an exception that shouldn't be retried, or if all retry attempts are
     * expended
     */
    private Object retry(InvocationContext context) throws Exception {
        Object resultValue = null;
        Retry retry = getConfig().getAnnotation(Retry.class);

        try {
            resultValue = context.proceed();
            getMetrics().incrementRetryCallsSucceededNotRetriedTotal();
        } catch (Exception ex) {
            final Class<? extends Throwable>[] retryOn = getConfig().retryOn(retry);
            final Class<? extends Throwable>[] abortOn = getConfig().abortOn(retry);

            if (!shouldRetry(retryOn, abortOn, ex)) {
                logger.log(Level.FINE, "Exception is contained in retryOn or abortOn, not retrying.", ex);
                throw ex;
            }

            int maxRetries = getConfig().maxRetries(retry);
            long delay = getConfig().delay(retry);
            ChronoUnit delayUnit = getConfig().delayUnit(retry);
            long maxDuration = getConfig().maxDuration(retry);
            ChronoUnit durationUnit = getConfig().durationUnit(retry);
            long jitter = getConfig().jitter(retry);
            ChronoUnit jitterDelayUnit = getConfig().jitterDelayUnit(retry);

            long delayMillis = Duration.of(delay, delayUnit).toMillis();
            long jitterMillis = Duration.of(jitter, jitterDelayUnit).toMillis();
            long timeoutTime = System.currentTimeMillis() + Duration.of(maxDuration, durationUnit).toMillis();

            Exception retryException = ex;

            getExecution().trace("retryMethod", context);

            boolean succeeded = false;

            try {
                if (maxRetries == -1 && maxDuration > 0) {
                    logger.log(Level.FINER, "Retrying until maxDuration is breached.");

                    while (System.currentTimeMillis() < timeoutTime) {
                        getMetrics().incrementRetryRetriesTotal();
                        try {
                            resultValue = context.proceed();
                            succeeded = true;
                            getMetrics().incrementRetryCallsSucceededRetriedTotal();
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                getExecution().trace("delayRetry", context);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    getExecution().endTrace();
                                }
                            }
                        }
                    }
                } else if (maxRetries == -1 && maxDuration == 0) {
                    logger.log(Level.INFO, "Retrying potentially forever!");
                    while (true) {
                        getMetrics().incrementRetryRetriesTotal();
                        try {
                            resultValue = context.proceed();
                            getMetrics().incrementRetryCallsSucceededRetriedTotal();
                            succeeded = true;
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                getExecution().trace("delayRetry", context);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    getExecution().endTrace();
                                }
                            }
                        }
                    }
                } else if (maxRetries != -1 && maxDuration > 0) {
                    logger.log(Level.INFO,
                            "Retrying as long as maxDuration ({0}ms) isn''t breached, and no more than {1} times",
                            new Object[]{Duration.of(maxDuration, durationUnit).toMillis(), maxRetries});
                    while (maxRetries > 0 && System.currentTimeMillis() < timeoutTime) {
                        getMetrics().incrementRetryRetriesTotal();
                        try {
                            resultValue = context.proceed();
                            getMetrics().incrementRetryCallsSucceededRetriedTotal();
                            succeeded = true;
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                getExecution().trace("delayRetry", context);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    getExecution().endTrace();
                                }
                            }

                            maxRetries--;
                        }
                    }
                } else {
                    logger.log(Level.INFO, "Retrying no more than {0} times", maxRetries);
                    while (maxRetries > 0) {
                        getMetrics().incrementRetryRetriesTotal();
                        try {
                            resultValue = context.proceed();
                            getMetrics().incrementRetryCallsSucceededRetriedTotal();
                            succeeded = true;
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                getExecution().trace("delayRetry", context);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    getExecution().endTrace();
                                }
                            }

                            maxRetries--;
                        }
                    }
                }
            } finally {
                getExecution().endTrace();
            }

            if (!succeeded) {
                getMetrics().incrementRetryCallsFailedTotal();
                throw retryException;
            }
        }

        return resultValue;
    }

    /**
     * Helper method that determines whether or not a retry should be attempted for the given exception.
     * @param retryOn The exceptions to retry on.
     * @param abortOn The exceptions to abort on.
     * @param ex The caught exception
     * @return True if retry should be attempted.
     */
    private boolean shouldRetry(Class<? extends Throwable>[] retryOn, Class<? extends Throwable>[] abortOn,
            Exception ex) {
        boolean shouldRetry = false;

        // If the first value in the array is just "Exception", just set retry to true, otherwise check the exceptions
        if (retryOn[0] != Exception.class) {
            for (Class<? extends Throwable> throwable : retryOn) {
                if (ex.getClass() == throwable) {
                    logger.log(Level.FINER, "Exception {0} matches a Throwable in retryOn",
                            ex.getClass().getSimpleName());
                    shouldRetry = true;
                    break;
                }
                try {
                    // If we there isn't a direct match, check if the exception is a subclass
                    ex.getClass().asSubclass(throwable);
                    shouldRetry = true;

                    logger.log(Level.FINER, "Exception {0} is a child of a Throwable in retryOn: {1}",
                            new String[]{ex.getClass().getSimpleName(), throwable.getSimpleName()});
                    break;
                } catch (ClassCastException cce) {
                    // Om nom nom
                }
            }
        } else {
            shouldRetry = true;
        }

        // If we should retry, check if the exception is one we shoukd abort on
        if (shouldRetry && abortOn != null) {
            for (Class<? extends Throwable> throwable : abortOn) {
                if (ex.getClass() == throwable) {
                    logger.log(Level.FINER, "Exception {0} matches a Throwable in abortOn",
                            ex.getClass().getSimpleName());
                    shouldRetry = false;
                    break;
                }
                try {
                    // If we there isn't a direct match, check if the exception is a subclass
                    ex.getClass().asSubclass(throwable);
                    shouldRetry = false;

                    logger.log(Level.FINER, "Exception {0} is a child of a Throwable in abortOn: {1}",
                            new String[]{ex.getClass().getSimpleName(), throwable.getSimpleName()});
                    break;
                } catch (ClassCastException cce) {
                    // Om nom nom
                }
            }
        }

        return shouldRetry;
    }
}
