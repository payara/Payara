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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
 * of truth throughout the execution of FT behaviour that is convenient to pass around as a single immutable value.
 *
 * @author Jan Bernitt
 */
public final class FaultTolerancePolicy implements Serializable {

    private static final long TTL = 60 * 1000;

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Method, FaultTolerancePolicy>> POLICY_BY_METHOD = new ConcurrentHashMap<>();

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
     * @return the result of {@link InvocationContext#proceed()} wrapped with FT behaviour
     * @throws Exception as thrown by the wrapped invocation or a {@link FaultToleranceException}
     */
    public Object proceed(InvocationContext context, FaultToleranceService service) throws Exception {
        if (!isPresent) {
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
        Set<Thread> workers = ConcurrentHashMap.newKeySet();
        CompletableFuture<Object> asyncResult = new CompletableFuture<Object>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (super.cancel(mayInterruptIfRunning)) {
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
        service.runAsynchronous(asyncResult,
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
        try {
            return processRetryStage(invocation);
        } catch (Exception ex) {
            invocation.metrics.incrementFallbackCallsTotal();
            if (fallback.isHandlerPresent()) {
                return invocation.service.fallbackHandle(fallback.value, invocation.context, ex);
            }
            return invocation.service.fallbackInvoke(fallback.method, invocation.context);
        }
    }

    /**
     * Stage that takes care of the {@link RetryPolicy} handling.
     */
    private Object processRetryStage(FaultToleranceInvocation invocation) throws Exception {
        int totalAttempts = retry.totalAttempts();
        int attemptsLeft = totalAttempts;
        Long retryTimeoutTime = retry.timeoutTimeNow();
        while (attemptsLeft > 0) {
            attemptsLeft--;
            try {
                boolean firstAttempt = attemptsLeft == totalAttempts - 1;
                if (!firstAttempt) {
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
                if (attemptsLeft <= 0 
                        || !retry.retryOn(ex) 
                        || retryTimeoutTime != null && System.currentTimeMillis() >= retryTimeoutTime) {
                    invocation.metrics.incrementRetryCallsFailedTotal();
                    throw ex;
                }
                if (retry.isDelayed()) {
                    invocation.service.delay(retry.jitteredDelay(), invocation.context);
                }
            }
        }
        // this line should never be reached as we throw above
        throw new FaultToleranceException("Retry failed"); 
    }

    private Object processRetryAsync(FaultToleranceInvocation invocation) throws Exception {
        CompletableFuture<Object> asyncAttempt = new CompletableFuture<>();
        invocation.service.runAsynchronous(asyncAttempt,
                () -> invocation.runStageWithWorker(() -> processCircuitBreakerStage(invocation, asyncAttempt)));
        try {
            asyncAttempt.get(); // wait and only proceed on success
            invocation.timeoutIfConcludedConcurrently();
            return asyncAttempt;
        } catch (ExecutionException ex) { // this ExecutionException is from calling get() above in case completed exceptionally
            if (ex.getCause() instanceof ExecutionException) { 
                // this cause ExecutionException is caused by annotated method returned a Future that completed exceptionally
                if (asynchronous.isSuccessWhenCompletedExceptionally()) {
                    return new CompletableFuture<>().completeExceptionally(ex.getCause()); // only unwrap level added by async processing
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
        CircuitBreakerState state = invocation.service.getState(circuitBreaker.requestVolumeThreshold, invocation.context);
        if (isMetricsEnabled) {
            invocation.metrics.insertCircuitbreakerOpenTotal(state::nanosOpen);
            invocation.metrics.insertCircuitbreakerHalfOpenTotal(state::nanosHalfOpen);
            invocation.metrics.insertCircuitbreakerClosedTotal(state::nanosClosed);
        }
        Object resultValue = null;
        switch (state.getCircuitState()) {
        default:
        case OPEN:
            invocation.metrics.incrementCircuitbreakerCallsPreventedTotal();
            throw new CircuitBreakerOpenException();
        case HALF_OPEN:
            try {
                resultValue = processTimeoutStage(invocation, asyncAttempt);
            } catch (Exception ex) {
                invocation.metrics.incrementCircuitbreakerCallsFailedTotal();
                if (circuitBreaker.failOn(ex)) {
                    invocation.metrics.incrementCircuitbreakerOpenedTotal();
                    state.open();
                    invocation.service.scheduleDelayed(circuitBreaker.delay, state::halfOpen);
                }
                throw ex;
            }
            state.halfOpenSuccessful(circuitBreaker.successThreshold);
            invocation.metrics.incrementCircuitbreakerCallsSucceededTotal();
            return resultValue;
        case CLOSED:
            Exception failedOn = null;
            try {
                resultValue = processTimeoutStage(invocation, asyncAttempt);
                state.recordClosedResult(true);
            } catch (Exception ex) {
                invocation.metrics.incrementCircuitbreakerCallsFailedTotal();
                if (circuitBreaker.failOn(ex)) {
                    state.recordClosedResult(false);
                }
                failedOn = ex;
            }
            if (state.isOverFailureThreshold(circuitBreaker.requestVolumeThreshold, circuitBreaker.failureRatio)) {
                invocation.metrics.incrementCircuitbreakerOpenedTotal();
                state.open();
                invocation.service.scheduleDelayed(circuitBreaker.delay, state::halfOpen);
            }
            if (failedOn != null) {
                throw failedOn;
            }
            invocation.metrics.incrementCircuitbreakerCallsSucceededTotal();
            return resultValue;
        }
    }

    /**
     * Stage that takes care of the {@link TimeoutPolicy} handling.
     */
    private Object processTimeoutStage(FaultToleranceInvocation invocation, CompletableFuture<Object> asyncAttempt) throws Exception {
        if (!isTimeoutPresent()) {
            return processBulkheadStage(invocation);
        }
        long timeoutDuration = Duration.of(timeout.value, timeout.unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutDuration;
        Thread current = Thread.currentThread();
        AtomicBoolean didTimeout = new AtomicBoolean(false);
        Future<?> timeout = invocation.service.scheduleDelayed(timeoutDuration, () -> { 
            didTimeout.set(true);
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
            if (didTimeout.get() || System.currentTimeMillis() > timeoutTime) {
                throw new TimeoutException();
            }
            invocation.metrics.incrementTimeoutCallsNotTimedOutTotal();
            return resultValue;
        } catch (Exception ex) {
            if ((ex instanceof InterruptedException || ex.getCause() instanceof InterruptedException)
                    && System.currentTimeMillis() > timeoutTime) {
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
        InvocationContext context = invocation.context;
        BulkheadSemaphore concurrentExecutions = invocation.service.getConcurrentExecutions(bulkhead.value, context);
        BulkheadSemaphore waitingQueuePopulation = !isAsynchronous() ? null
                : invocation.service.getWaitingQueuePopulation(bulkhead.waitingTaskQueue, context);
        if (isMetricsEnabled) {
            invocation.metrics.insertBulkheadConcurrentExecutions(concurrentExecutions::acquiredPermits);
            if (waitingQueuePopulation != null) {
                invocation.metrics.insertBulkheadWaitingQueuePopulation(waitingQueuePopulation::acquiredPermits);
            }
        }
        long executionStartTime = System.nanoTime();
        if (concurrentExecutions.tryAcquire(0, TimeUnit.SECONDS)) {
            invocation.metrics.incrementBulkheadCallsAcceptedTotal();
            if (isAsynchronous()) {
                invocation.metrics.addBulkheadWaitingDuration(0L); // we did not wait but need to factor in the invocation for histogram quartiles
            }
            try {
                return proceed(invocation);
            } finally {
                invocation.metrics.addBulkheadExecutionDuration(System.nanoTime() - executionStartTime);
                concurrentExecutions.release();
            }
        }
        if (waitingQueuePopulation == null) { // plain semaphore style, fail:
            invocation.metrics.incrementBulkheadCallsRejectedTotal();
            throw new BulkheadException("No free work permits.");
        }
        // from here: queueing style:
        if (waitingQueuePopulation.tryAcquire(0, TimeUnit.SECONDS)) {
            invocation.metrics.incrementBulkheadCallsAcceptedTotal();
            long queueStartTime = System.nanoTime();
            try {
                concurrentExecutions.acquire(); // block until execution permit becomes available
            } catch (InterruptedException ex) {
                waitingQueuePopulation.release();
                throw new BulkheadException(ex);
            } finally {
                invocation.metrics.addBulkheadWaitingDuration(System.nanoTime() - queueStartTime);
            }
            waitingQueuePopulation.release();
            try {
                return proceed(invocation);
            } finally {
                concurrentExecutions.release();
            }
        }
        invocation.metrics.incrementBulkheadCallsRejectedTotal();
        throw new BulkheadException("No free work or queue permits.");
    }

    /**
     * Final stage where the actual wrapped method call occurs.
     */
    private static Object proceed(FaultToleranceInvocation invocation) throws Exception {
        invocation.timeoutIfConcludedConcurrently();
        return invocation.context.proceed();
    }

}
