/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.policy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

/**
 * The {@link FaultTolerancePolicy} describes the effective aggregated policies to use for a particular {@link Method}
 * when adding fault tolerant behaviour to it.
 * 
 * The policies are extracted from FT annotations and the {@link FaultToleranceConfig}.
 * 
 * In contrast to the plain annotations the policies do consider configuration overrides and include validation of the
 * effective values.
 * 
 * The policy class also reduces the need to analyse FT annotations for each invocation and works as a consistent source
 * of truth throughout the processing of FT behaviour that is convenient to pass around as a single immutable value.
 *
 * @author Jan Bernitt
 */
public final class FaultTolerancePolicy implements Serializable {

    static final Logger logger = Logger.getLogger(FaultTolerancePolicy.class.getName());

    private static final long TTL = 60 * 1000;

    /**
     * A simple cache with a fix {@link #TTL} with a policy for each target method.
     */
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Method, FaultTolerancePolicy>> POLICY_BY_METHOD 
        = new ConcurrentHashMap<>();

    /**
     * Removes all expired policies from the cache.
     */
    public static void clean() {
        long now = System.currentTimeMillis();
        POLICY_BY_METHOD.forEachValue(Long.MAX_VALUE,
                map -> map.entrySet().removeIf(entry -> now > entry.getValue().expiresMillis));
    }

    public static FaultTolerancePolicy asAnnotated(Class<?> target, Method annotated) {
        return create(new StaticAnalysisContext(target, annotated), 
                () -> FaultToleranceConfig.asAnnotated(target, annotated));
    }

    /**
     * Returns the {@link FaultTolerancePolicy} to use for the method invoked in the current context.
     *
     * @param context       current context
     * @param configSpplier supplies the configuration (if needed, in case returned policy needs to be created with help
     *                      of the {@link FaultToleranceConfig})
     * @return the policy to apply
     * @throws FaultToleranceDefinitionException in case the effective policy contains illegal values
     */
    public static FaultTolerancePolicy get(InvocationContext context, Supplier<FaultToleranceConfig> configSpplier)
            throws FaultToleranceDefinitionException {
        return POLICY_BY_METHOD.computeIfAbsent(context.getTarget().getClass(), target -> new ConcurrentHashMap<>())
                .compute(context.getMethod(), (method, policy) -> 
                    policy != null && !policy.isExpired() ? policy : create(context, configSpplier));
    }

    private static FaultTolerancePolicy create(InvocationContext context, Supplier<FaultToleranceConfig> configSpplier) {
        FaultToleranceConfig config = configSpplier.get();
        return new FaultTolerancePolicy(
                config.isNonFallbackEnabled(),
                config.isMetricsEnabled(),
                AsynchronousPolicy.create(context, config),
                BulkheadPolicy.create(context, config),
                CircuitBreakerPolicy.create(context, config),
                FallbackPolicy.create(context, config),
                RetryPolicy.create(context, config),
                TimeoutPolicy.create(context, config));
    }

    private final long expiresMillis;
    public final boolean isPresent;
    public final boolean isNonFallbackEnabled;
    public final boolean isMetricsEnabled;
    public final AsynchronousPolicy asynchronous;
    public final BulkheadPolicy bulkhead;
    public final CircuitBreakerPolicy circuitBreaker;
    public final FallbackPolicy fallback;
    public final RetryPolicy retry;
    public final TimeoutPolicy timeout;

    public FaultTolerancePolicy(boolean isNonFallbackEnabled, boolean isMetricsEnabled, AsynchronousPolicy asynchronous,
            BulkheadPolicy bulkhead, CircuitBreakerPolicy circuitBreaker, FallbackPolicy fallback, RetryPolicy retry,
            TimeoutPolicy timeout) {
        this.expiresMillis = System.currentTimeMillis() + TTL;
        this.isNonFallbackEnabled = isNonFallbackEnabled;
        this.isMetricsEnabled = isMetricsEnabled;
        this.asynchronous = asynchronous;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.fallback = fallback;
        this.retry = retry;
        this.timeout = timeout;
        this.isPresent = isAsynchronous() || isBulkheadPresent() || isCircuitBreakerPresent()
                || isFallbackPresent() || isRetryPresent() || isTimeoutPresent();
    }

    private boolean isExpired() {
        return System.currentTimeMillis() > expiresMillis;
    }

    public boolean isAsynchronous() {
        return asynchronous != null;
    }

    public boolean isBulkheadPresent() {
        return bulkhead != null;
    }

    public boolean isCircuitBreakerPresent() {
        return circuitBreaker != null;
    }

    public boolean isFallbackPresent() {
        return fallback != null;
    }

    public boolean isRetryPresent() {
        return retry != null;
    }

    public boolean isTimeoutPresent() {
        return timeout != null;
    }

    static final class FaultToleranceInvocation {
        final InvocationContext context;
        final FaultToleranceService service;
        final FaultToleranceMetrics metrics;
        final CompletableFuture<Object> asyncResult;
        final Set<Thread> asyncWorkers;

        FaultToleranceInvocation(InvocationContext context, FaultToleranceService service, FaultToleranceMetrics metrics,
                CompletableFuture<Object> asyncResult, Set<Thread> asyncWorkers) {
            this.context = context;
            this.service = service;
            this.metrics = metrics;
            this.asyncResult = asyncResult;
            this.asyncWorkers = asyncWorkers;
        }

        Object runStageWithWorker(Callable<Object> stage) throws Exception {
            timeoutIfConcludedConcurrently();
            Thread current = Thread.currentThread();
            asyncWorkers.add(current);
            try {
                return stage.call();
            } finally {
                asyncWorkers.remove(current);
            }
        }

        void timeoutIfConcludedConcurrently() throws TimeoutException {
            if (asyncResult != null && asyncResult.isDone() || Thread.currentThread().isInterrupted()) {
                throw new TimeoutException("Computation already concluded in a concurrent attempt");
            }
        }

        void trace(String method) {
            service.trace(method, context);
        }

        void endTrace() {
            service.endTrace();
        }
    }

    /**
     * Wraps {@link InvocationContext#proceed()} with fault tolerance behaviour.
     * 
     * Processing has 6 stages:
     * <pre>
     * 1) Asynchronous 
     * 2) Fallback 
     * 3) Retry 
     * 4) Circuit Breaker 
     * 5) Timeout 
     * 6) Bulkhead
     * </pre>
     * The call chain goes from 1) down to 6) skipping stages that are not requested by this policy.
     * 
     * Asynchronous execution branches to new threads in stage 1) and 3) each executed by the
     * {@link FaultToleranceService#runAsynchronous(CompletableFuture, Callable)}.
     * 
     * @param context intercepted call context
     * @param service the environment used to execute the FT behaviour
     * @return the result of {@link InvocationContext#proceed()} after applying FT behaviour
     * @throws Exception as thrown by the wrapped invocation or a {@link FaultToleranceException}
     */
    public Object proceed(InvocationContext context, FaultToleranceService service) throws Exception {
        if (!isPresent) {
            logger.log(Level.FINER, "Fault Tolerance not enabled, proceeding normally.");
            return context.proceed();
        }
        FaultToleranceMetrics metrics = isMetricsEnabled 
                ? service.getMetrics(context)
                : FaultToleranceMetrics.DISABLED;
        try {
            metrics.incrementInvocationsTotal();
            return processAsynchronousStage(context, service, metrics);
        } catch (Exception e) {
            metrics.incrementInvocationsFailedTotal();
            throw e;
        }
    }

    /**
     * Stage that takes care of the {@link AsynchronousPolicy} handling.
     */
    private Object processAsynchronousStage(InvocationContext context, FaultToleranceService service,
            FaultToleranceMetrics metrics) throws Exception {
        if (!isAsynchronous()) {
            return processFallbackStage(new FaultToleranceInvocation(context, service, metrics, null, null));
        }
        logger.log(Level.FINER, "Proceeding invocation with asynchronous semantics");
        Set<Thread> workers = ConcurrentHashMap.newKeySet();
        CompletableFuture<Object> asyncResult = new CompletableFuture<Object>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (super.cancel(mayInterruptIfRunning)) {
                    logger.log(Level.FINE, "Asynchronous computation was cancelled by caller.");
                    if (mayInterruptIfRunning) {
                        workers.forEach(worker -> worker.interrupt());
                    }
                    return true;
                }
                return false;
            }

            /**
             * Note that the exception is expected to be the exception thrown when trying to resolve the future returned
             * by the annotated method.
             */
            @Override
            public boolean completeExceptionally(Throwable ex) {
                logger.log(Level.FINE, "Asynchronous computation completed with exception", ex);
                if (ex instanceof ExecutionException) {
                    metrics.incrementInvocationsFailedTotal();
                    return super.completeExceptionally(ex.getCause());
                } else if (ex instanceof FaultToleranceException || !asynchronous.isSuccessWhenCompletedExceptionally()) {
                    metrics.incrementInvocationsFailedTotal();
                }
                return super.completeExceptionally(ex);
            }
        };
        FaultToleranceInvocation invocation = new FaultToleranceInvocation(context, service, metrics, asyncResult, workers);
        service.runAsynchronous(asyncResult, context,
                () -> invocation.runStageWithWorker(() -> processFallbackStage(invocation)));
        return asyncResult;
    }

    /**
     * Stage that takes care of the {@link FallbackPolicy} handling.
     */
    private Object processFallbackStage(FaultToleranceInvocation invocation) throws Exception {
        if (!isFallbackPresent()) {
            return processRetryStage(invocation);
        }
        logger.log(Level.FINER, "Proceeding invocation with fallback semantics");
        invocation.trace("executeFallbackMethod");
        try {
            return processRetryStage(invocation);
        } catch (Exception ex) {
            invocation.metrics.incrementFallbackCallsTotal();
            if (fallback.isHandlerPresent()) {
                logger.log(Level.FINE, "Using fallback class: {0}", fallback.value.getName());
                return invocation.service.fallbackHandle(fallback.value, invocation.context, ex);
            }
            logger.log(Level.FINE, "Using fallback method: {0}", fallback.method.getName());
            return invocation.service.fallbackInvoke(fallback.method, invocation.context);
        } finally {
            invocation.endTrace();
        }
    }

    /**
     * Stage that takes care of the {@link RetryPolicy} handling.
     */
    private Object processRetryStage(FaultToleranceInvocation invocation) throws Exception {
        if (!retry.isNone()) {
            logger.log(Level.FINER, "Proceeding invocation with retry semantics");
        }
        int totalAttempts = retry.totalAttempts();
        int attemptsLeft = totalAttempts;
        Long retryTimeoutTime = retry.timeoutTimeNow();
        while (attemptsLeft > 0) {
            attemptsLeft--;
            try {
                boolean firstAttempt = attemptsLeft == totalAttempts - 1;
                if (!firstAttempt) {
                    logger.log(Level.FINER, "Attempting retry.");
                    invocation.metrics.incrementRetryRetriesTotal();
                }
                Object resultValue = isAsynchronous() 
                        ? processRetryAsync(invocation)
                        : processCircuitBreakerStage(invocation, null);
                if (firstAttempt) {
                    invocation.metrics.incrementRetryCallsSucceededNotRetriedTotal();
                } else {
                    invocation.metrics.incrementRetryCallsSucceededRetriedTotal();
                }
                return resultValue;
            } catch (Exception ex) {
                boolean timedOut = retryTimeoutTime != null && System.currentTimeMillis() >= retryTimeoutTime;
                if (attemptsLeft <= 0 || !retry.retryOn(ex) || timedOut) {
                    logger.log(Level.FINE, "Retry attemp failed. Giving up{0}", timedOut ? " due to time-out." : ".");
                    invocation.metrics.incrementRetryCallsFailedTotal();
                    throw ex;
                }
                logger.log(Level.FINE, "Retry attempt failed. {0} attempts left.", attemptsLeft);
                if (retry.isDelayed()) {
                    invocation.service.delay(retry.jitteredDelay(), invocation.context);
                }
            }
        }
        // this line should never be reached as we throw above
        throw new FaultToleranceException("Retry failed"); 
    }

    private CompletableFuture<Object> processRetryAsync(FaultToleranceInvocation invocation) throws Exception {
        CompletableFuture<Object> asyncAttempt = new CompletableFuture<>();
        invocation.service.runAsynchronous(asyncAttempt, invocation.context,
                () -> invocation.runStageWithWorker(() -> processCircuitBreakerStage(invocation, asyncAttempt)));
        try {
            asyncAttempt.get(); // wait and only proceed on success
            invocation.timeoutIfConcludedConcurrently();
            return asyncAttempt;
        } catch (ExecutionException ex) { // this ExecutionException is from calling get() above in case completed exceptionally
            if (ex.getCause() instanceof ExecutionException) { 
                // this cause ExecutionException is caused by annotated method returned a Future that completed exceptionally
                if (asynchronous.isSuccessWhenCompletedExceptionally()) {
                    CompletableFuture<Object> exceptionalResult = new CompletableFuture<>();
                    exceptionalResult.completeExceptionally(ex.getCause().getCause()); // unwrap
                    return exceptionalResult;
                }
                throw (Exception) ex.getCause().getCause(); // for retry handling return plain cause
            }
            throw (Exception) ex.getCause();
        }
    }

    /**
     * Stage that takes care of the {@link CircuitBreakerPolicy} handling.
     */
    private Object processCircuitBreakerStage(FaultToleranceInvocation invocation, CompletableFuture<Object> asyncAttempt) throws Exception {
        if (!isCircuitBreakerPresent()) {
            return processTimeoutStage(invocation, asyncAttempt);
        }
        logger.log(Level.FINER, "Proceeding invocation with circuitbreaker semantics");
        CircuitBreakerState state = invocation.service.getState(circuitBreaker.requestVolumeThreshold, invocation.context);
        if (isMetricsEnabled) {
            invocation.metrics.linkCircuitbreakerOpenTotal(state::nanosOpen);
            invocation.metrics.linkCircuitbreakerHalfOpenTotal(state::nanosHalfOpen);
            invocation.metrics.linkCircuitbreakerClosedTotal(state::nanosClosed);
        }
        Object resultValue = null;
        switch (state.getCircuitState()) {
        default:
        case OPEN:
            logger.log(Level.FINER, "CircuitBreaker is open, throwing exception");
            invocation.metrics.incrementCircuitbreakerCallsPreventedTotal();
            throw new CircuitBreakerOpenException();
        case HALF_OPEN:
            logger.log(Level.FINER, "Proceeding half open CircuitBreaker context");
            try {
                resultValue = processTimeoutStage(invocation, asyncAttempt);
            } catch (Exception ex) {
                invocation.metrics.incrementCircuitbreakerCallsFailedTotal();
                if (circuitBreaker.failOn(ex)) {
                    logger.log(Level.FINE, "Exception causes CircuitBreaker to transit: half-open => open");
                    openCircuit(invocation, state);
                }
                throw ex;
            }
            if (state.halfOpenSuccessfulClosedCircuit(circuitBreaker.successThreshold)) {
                logger.log(Level.FINE, "Success threshold causes CircuitBreaker to transit: half-open => closed");
            }
            invocation.metrics.incrementCircuitbreakerCallsSucceededTotal();
            return resultValue;
        case CLOSED:
            logger.log(Level.FINER, "Proceeding closed CircuitBreaker context");
            Exception failedOn = null;
            try {
                resultValue = processTimeoutStage(invocation, asyncAttempt);
                state.recordClosedOutcome(true);
            } catch (Exception ex) {
                if (circuitBreaker.failOn(ex)) {
                    state.recordClosedOutcome(false);
                    invocation.metrics.incrementCircuitbreakerCallsFailedTotal();
                } else {
                    invocation.metrics.incrementCircuitbreakerCallsSucceededTotal();
                }
                failedOn = ex;
            }
            if (state.isOverFailureThreshold(circuitBreaker.requestVolumeThreshold, circuitBreaker.failureRatio)) {
                logger.log(Level.FINE, "Failure threshold causes CircuitBreaker to transit: closed => open");
                openCircuit(invocation, state);
            }
            if (failedOn != null) {
                throw failedOn;
            }
            invocation.metrics.incrementCircuitbreakerCallsSucceededTotal();
            return resultValue;
        }
    }

    private void openCircuit(FaultToleranceInvocation invocation, CircuitBreakerState state) throws Exception {
        invocation.metrics.incrementCircuitbreakerOpenedTotal();
        state.open();
        if (circuitBreaker.delay == 0L) {
            state.halfOpen();
        } else {
            invocation.service.runDelayed(circuitBreaker.delay, state::halfOpen);
        }
    }

    /**
     * Stage that takes care of the {@link TimeoutPolicy} handling.
     */
    private Object processTimeoutStage(FaultToleranceInvocation invocation, CompletableFuture<Object> asyncAttempt) throws Exception {
        if (!isTimeoutPresent()) {
            return processBulkheadStage(invocation);
        }
        logger.log(Level.FINER, "Proceeding invocation with timeout semantics");
        long timeoutDuration = Duration.of(timeout.value, timeout.unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutDuration;
        Thread current = Thread.currentThread();
        AtomicBoolean timedOut = new AtomicBoolean(false);
        Future<?> timeout = invocation.service.runDelayed(timeoutDuration, () -> { 
            logger.log(Level.FINE, "Interrupting attempt due to timeout.");
            timedOut.set(true);
            current.interrupt();
            invocation.metrics.incrementTimeoutCallsTimedOutTotal();
            if (asyncAttempt != null) {
                // we do this since interrupting not necessarily returns directly or ever but the attempt should timeout now
                asyncAttempt.completeExceptionally(new TimeoutException());
            }
        });
        long executionStartTime = System.nanoTime();
        try {
            Object resultValue = processBulkheadStage(invocation);
            if (current.isInterrupted()) {
                Thread.interrupted(); // clear the flag
            }
            if (timedOut.get() || System.currentTimeMillis() > timeoutTime) {
                throw new TimeoutException();
            }
            invocation.metrics.incrementTimeoutCallsNotTimedOutTotal();
            return resultValue;
        } catch (TimeoutException ex) {
            logger.log(Level.FINE, "Execution timed out.");
            throw ex;
        } catch (Exception ex) {
            if (timedOut.get() || System.currentTimeMillis() > timeoutTime) {
                logger.log(Level.FINE, "Execution timed out.");
                throw new TimeoutException(ex);
            }
            throw ex;
        } finally {
            invocation.metrics.addTimeoutExecutionDuration(System.nanoTime() - executionStartTime);
            timeout.cancel(true);
        }
    }

    /**
     * Stage that takes care of the {@link BulkheadPolicy} handling.
     */
    private Object processBulkheadStage(FaultToleranceInvocation invocation) throws Exception {
        if (!isBulkheadPresent()) {
            return proceed(invocation);
        }
        logger.log(Level.FINER, "Proceeding invocation with bulkhead semantics");
        InvocationContext context = invocation.context;
        BulkheadSemaphore concurrentExecutions = invocation.service.getConcurrentExecutions(bulkhead.value, context);
        BulkheadSemaphore waitingQueuePopulation = !isAsynchronous() ? null
                : invocation.service.getWaitingQueuePopulation(bulkhead.waitingTaskQueue, context);
        if (isMetricsEnabled) {
            invocation.metrics.linkBulkheadConcurrentExecutions(concurrentExecutions::acquiredPermits);
            if (waitingQueuePopulation != null) {
                invocation.metrics.linkBulkheadWaitingQueuePopulation(waitingQueuePopulation::acquiredPermits);
            }
        }
        logger.log(Level.FINER, "Attempting to acquire bulkhead execution permit.");
        if (concurrentExecutions.tryAcquireFair()) {
            logger.log(Level.FINE, "Acquired bulkhead execution permit.");
            invocation.metrics.incrementBulkheadCallsAcceptedTotal();
            if (isAsynchronous()) {
                // we did not wait but need to factor in the invocation for histogram quartiles
                invocation.metrics.addBulkheadWaitingDuration(1L); // using 1ns because 0 leads to flaky test
            }
            return processBulkheadExecution(invocation, concurrentExecutions);
        }
        if (waitingQueuePopulation == null) { // plain semaphore style, fail:
            invocation.metrics.incrementBulkheadCallsRejectedTotal();
            throw new BulkheadException("No free work permits.");
        }
        // from here: queueing style:
        logger.log(Level.FINER, "Attempting to acquire bulkhead queue permit.");
        if (waitingQueuePopulation.tryAcquireFair()) {
            logger.log(Level.FINE, "Acquired bulkhead queue permit.");
            invocation.metrics.incrementBulkheadCallsAcceptedTotal();
            long waitingSince = System.nanoTime();
            try {
                invocation.trace("obtainBulkheadSemaphore");
                concurrentExecutions.acquire(); // block until execution permit becomes available
                waitingQueuePopulation.release();
            } catch (InterruptedException ex) {
                logger.log(Level.FINE, "Interrupted acquiring bulkhead permit", ex);
                invocation.metrics.incrementBulkheadCallsRejectedTotal();
                waitingQueuePopulation.release();
                throw new BulkheadException(ex);
            } finally {
                invocation.endTrace();
                invocation.metrics.addBulkheadWaitingDuration(System.nanoTime() - waitingSince);
            }
            return processBulkheadExecution(invocation, concurrentExecutions);
        }
        invocation.metrics.incrementBulkheadCallsRejectedTotal();
        throw new BulkheadException("No free work or queue permits.");
    }

    private static Object processBulkheadExecution(FaultToleranceInvocation invocation, BulkheadSemaphore concurrentExecutions)
            throws Exception {
        long executionSince = System.nanoTime();
        try {
            return proceed(invocation);
        } finally {
            invocation.metrics.addBulkheadExecutionDuration(System.nanoTime() - executionSince);
            concurrentExecutions.release();
        }
    }

    /**
     * Final stage where the actual wrapped method call occurs.
     */
    private static Object proceed(FaultToleranceInvocation invocation) throws Exception {
        invocation.timeoutIfConcludedConcurrently();
        logger.log(Level.FINER, "Proceeding invocation chain");
        return invocation.context.proceed();
    }

}
