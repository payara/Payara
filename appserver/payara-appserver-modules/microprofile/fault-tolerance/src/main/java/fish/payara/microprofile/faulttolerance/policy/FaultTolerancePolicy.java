package fish.payara.microprofile.faulttolerance.policy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceExecution;
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

    private static final Logger logger = Logger.getLogger(FaultTolerancePolicy.class.getName());

    private static final long TTL = 60 * 1000;

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Method, FaultTolerancePolicy>> POLICY_BY_METHOD = new ConcurrentHashMap<>();

    public static void clean() {
        long now = System.currentTimeMillis();
        POLICY_BY_METHOD.forEachValue(Long.MAX_VALUE,
                map -> map.entrySet().removeIf(entry -> now > entry.getValue().expiresMillis));
    }

    public static FaultTolerancePolicy asAnnotated(Method annotated) {
        return create(new StaticAnalysisMethodContext(annotated), () -> FaultToleranceConfig.ANNOTATED);
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
                config.isNonFallbackEnabled(context),
                config.isMetricsEnabled(context),
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
        final FaultToleranceExecution execution;
        final FaultToleranceMetrics metrics;
        final CompletableFuture<Object> asyncResult;
        final Set<Thread> asyncWorkers;

        FaultToleranceInvocation(InvocationContext context, FaultToleranceExecution execution, FaultToleranceMetrics metrics,
                CompletableFuture<Object> asyncResult, Set<Thread> asyncWorkers) {
            this.context = context;
            this.execution = execution;
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

    public Object proceed(InvocationContext context, FaultToleranceExecution execution) throws Exception {
        if (!isPresent) {
            return context.proceed();
        }
        FaultToleranceMetrics metrics = isMetricsEnabled 
                ? execution.getMetrics(context)
                : FaultToleranceMetrics.DISABLED;
        try {
            metrics.incrementInvocationsTotal(context);
            return processAsynchronousStage(context, execution, metrics);
        } catch (Exception e) {
            metrics.incrementInvocationsFailedTotal(context);
            throw e;
        }
    }

    /**
     * Stage that takes care of the {@link AsynchronousPolicy} handling.
     */
    private Object processAsynchronousStage(InvocationContext context, FaultToleranceExecution execution,
            FaultToleranceMetrics metrics) throws Exception {
        if (!isAsynchronous()) {
            return processFallbackStage(new FaultToleranceInvocation(context, execution, metrics, null, null));
        }
        Set<Thread> asyncWorkers = ConcurrentHashMap.newKeySet();
        CompletableFuture<Object> asyncResult = new CompletableFuture<Object>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (super.cancel(mayInterruptIfRunning)) {
                    if (mayInterruptIfRunning) {
                        asyncWorkers.forEach(worker -> worker.interrupt());
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
                    metrics.incrementInvocationsFailedTotal(context);
                    return super.completeExceptionally(ex.getCause());
                } else if (!asynchronous.isSuccessWhenCompletedExceptionally()) {
                    metrics.incrementInvocationsFailedTotal(context);
                }
                return super.completeExceptionally(ex);
            }
        };
        FaultToleranceInvocation invocation = new FaultToleranceInvocation(context, execution, metrics, asyncResult,
                asyncWorkers);
        execution.runAsynchronous(asyncResult,
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
            invocation.metrics.incrementFallbackCallsTotal(invocation.context);
            if (fallback.isHandlerPresent()) {
                return invocation.execution.fallbackHandle(fallback.value, invocation.context, ex);
            }
            return invocation.execution.fallbackInvoke(fallback.method, invocation.context);
        }
    }

    /**
     * Stage that takes care of the {@link RetryPolicy} handling.
     */
    private Object processRetryStage(FaultToleranceInvocation invocation) throws Exception {
        int totalAttempts = retry.totalAttempts();
        int attemptsLeft = totalAttempts;
        Long retryTimeoutTime = retry.timeoutTimeNow();
        InvocationContext context = invocation.context;
        while (attemptsLeft > 0) {
            attemptsLeft--;
            try {
                boolean firstAttempt = attemptsLeft == totalAttempts - 1;
                if (!firstAttempt) {
                    invocation.metrics.incrementRetryRetriesTotal(context);
                }
                Object resultValue = isAsynchronous() 
                        ? processRetryAsync(invocation)
                        : processCircuitBreakerStage(invocation, null);
                if (firstAttempt) {
                    invocation.metrics.incrementRetryCallsSucceededNotRetriedTotal(context);
                } else {
                    invocation.metrics.incrementRetryCallsSucceededRetriedTotal(context);
                }
                return resultValue;
            } catch (Exception ex) {
                if (attemptsLeft <= 0 
                        || !retry.retryOn(ex) 
                        || retryTimeoutTime != null && System.currentTimeMillis() >= retryTimeoutTime) {
                    invocation.metrics.incrementRetryCallsFailedTotal(context);
                    throw ex;
                }
                if (retry.isDelayed()) {
                    invocation.execution.delay(retry.jitteredDelay(), context);
                }
            }
        }
        // this line should never be reached as we throw above
        throw new FaultToleranceException("Retry failed"); 
    }

    private Object processRetryAsync(FaultToleranceInvocation invocation) throws Exception {
        CompletableFuture<Object> asyncAttempt = new CompletableFuture<>();
        //TODO try if it works to use the asyncTry thread as inv asyncResult
        invocation.execution.runAsynchronous(asyncAttempt,
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
        InvocationContext context = invocation.context;
        CircuitBreakerState state = invocation.execution.getState(circuitBreaker.requestVolumeThreshold, context);
        if (isMetricsEnabled) {
            invocation.metrics.insertCircuitbreakerOpenTotal(state::nanosOpen, context);
            invocation.metrics.insertCircuitbreakerHalfOpenTotal(state::nanosHalfOpen, context);
            invocation.metrics.insertCircuitbreakerClosedTotal(state::nanosClosed, context);
        }
        Object resultValue = null;
        switch (state.getCircuitState()) {
        default:
        case OPEN:
            invocation.metrics.incrementCircuitbreakerCallsPreventedTotal(context);
            throw new CircuitBreakerOpenException();
        case HALF_OPEN:
            try {
                resultValue = processTimeoutStage(invocation, asyncAttempt);
            } catch (Exception ex) {
                invocation.metrics.incrementCircuitbreakerCallsFailedTotal(context);
                if (circuitBreaker.failOn(ex)) {
                    invocation.metrics.incrementCircuitbreakerOpenedTotal(context);
                    state.open();
                    invocation.execution.scheduleDelayed(circuitBreaker.delay, state::halfOpen);
                }
                throw ex;
            }
            state.halfOpenSuccessful(circuitBreaker.successThreshold);
            invocation.metrics.incrementCircuitbreakerCallsSucceededTotal(context);
            return resultValue;
        case CLOSED:
            Exception failedOn = null;
            try {
                resultValue = processTimeoutStage(invocation, asyncAttempt);
                state.recordClosedResult(true);
            } catch (Exception ex) {
                invocation.metrics.incrementCircuitbreakerCallsFailedTotal(context);
                if (circuitBreaker.failOn(ex)) {
                    state.recordClosedResult(false);
                }
                failedOn = ex;
            }
            if (state.isOverFailureThreshold(circuitBreaker.requestVolumeThreshold, circuitBreaker.failureRatio)) {
                invocation.metrics.incrementCircuitbreakerOpenedTotal(context);
                state.open();
                invocation.execution.scheduleDelayed(circuitBreaker.delay, state::halfOpen);
            }
            if (failedOn != null) {
                throw failedOn;
            }
            invocation.metrics.incrementCircuitbreakerCallsSucceededTotal(context);
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
        InvocationContext context = invocation.context;
        Future<?> timeout = invocation.execution.scheduleDelayed(timeoutDuration, () -> { 
            didTimeout.set(true);
            current.interrupt();
            invocation.metrics.incrementTimeoutCallsTimedOutTotal(context);
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
            invocation.metrics.incrementTimeoutCallsNotTimedOutTotal(context);
            return resultValue;
        } catch (Exception ex) {
            if ((ex instanceof InterruptedException || ex.getCause() instanceof InterruptedException)
                    && System.currentTimeMillis() > timeoutTime) {
                throw new TimeoutException(ex); 
            }
            throw ex;
        } finally {
            invocation.metrics.addTimeoutExecutionDuration(System.nanoTime() - executionStartTime, context);
            timeout.cancel(true);
        }
    }

    /**
     * Stage that takes care of the {@link BulkheadPolicy} handling.
     */
    private Object processBulkheadStage(FaultToleranceInvocation invocation) throws Exception {
        if (!isBulkheadPresent()) {
            return processContextProceedStage(invocation);
        }
        InvocationContext context = invocation.context;
        BulkheadSemaphore executionSemaphore = invocation.execution.getExecutionSemaphoreOf(bulkhead.value, context);
        if (isMetricsEnabled) {
            invocation.metrics.insertBulkheadConcurrentExecutions(executionSemaphore::acquiredPermits, context);
        }
        long executionStartTime = System.nanoTime();
        if (executionSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
            invocation.metrics.incrementBulkheadCallsAcceptedTotal(context);
            if (isAsynchronous()) {
                invocation.metrics.addBulkheadWaitingDuration(0L, context); // we did not wait but need to factor in the invocation for histogram quartiles
            }
            try {
                return processContextProceedStage(invocation);
            } finally {
                invocation.metrics.addBulkheadExecutionDuration(System.nanoTime() - executionStartTime, context);
                executionSemaphore.release();
            }
        }
        if (!isAsynchronous()) { // plain semaphore style, fail:
            invocation.metrics.incrementBulkheadCallsRejectedTotal(context);
            throw new BulkheadException("No free work permits.");
        }
        // from here: queueing style:
        BulkheadSemaphore waitingQueueSemaphore = invocation.execution.getWaitingQueueSemaphoreOf(bulkhead.waitingTaskQueue, context);
        if (isMetricsEnabled) {
            invocation.metrics.insertBulkheadWaitingQueuePopulation(waitingQueueSemaphore::acquiredPermits, context);
        }
        if (waitingQueueSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
            invocation.metrics.incrementBulkheadCallsAcceptedTotal(context);
            long queueStartTime = System.nanoTime();
            try {
                executionSemaphore.acquire(); // block until execution permit becomes available
            } catch (InterruptedException ex) {
                waitingQueueSemaphore.release();
                throw new BulkheadException(ex);
            } finally {
                invocation.metrics.addBulkheadWaitingDuration(System.nanoTime() - queueStartTime, context);
            }
            waitingQueueSemaphore.release();
            try {
                return processContextProceedStage(invocation);
            } finally {
                executionSemaphore.release();
            }
        }
        invocation.metrics.incrementBulkheadCallsRejectedTotal(context);
        throw new BulkheadException("No free work or queue permits.");
    }

    /**
     * Stage where the actual wrapped method call occurs.
     */
    private Object processContextProceedStage(FaultToleranceInvocation invocation) throws Exception {
        invocation.timeoutIfConcludedConcurrently();
        return invocation.context.proceed();
    }

}
