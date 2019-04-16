package fish.payara.microprofile.faulttolerance.policy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

    private static final ConcurrentHashMap<Method, FaultTolerancePolicy> POLICY_BY_METHOD = new ConcurrentHashMap<>();

    public static void clean() {
        long now = System.currentTimeMillis();
        POLICY_BY_METHOD.entrySet().removeIf(entry -> now > entry.getValue().expiresMillis);
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
        return POLICY_BY_METHOD.compute(context.getMethod(), (method, policy) ->
        policy != null && !policy.isExpired() ? policy : create(context, configSpplier));
    }

    private static FaultTolerancePolicy create(InvocationContext context, Supplier<FaultToleranceConfig> configSpplier) {
        FaultToleranceConfig config = configSpplier.get();
        boolean isFaultToleranceEnabled = config.isEnabled(context);
        boolean metricsEnabled = config.isMetricsEnabled(context);
        return new FaultTolerancePolicy(isFaultToleranceEnabled,
                metricsEnabled,
                AsynchronousPolicy.create(context, config),
                BulkheadPolicy.create(context, config),
                CircuitBreakerPolicy.create(context, config),
                FallbackPolicy.create(context, config),
                RetryPolicy.create(context, config),
                TimeoutPolicy.create(context, config));
    }

    private final long expiresMillis;
    public final boolean isPresent;
    public final boolean isFaultToleranceEnabled;
    public final boolean isMetricsEnabled;
    public final AsynchronousPolicy asynchronous;
    public final BulkheadPolicy bulkhead;
    public final CircuitBreakerPolicy circuitBreaker;
    public final FallbackPolicy fallback;
    public final RetryPolicy retry;
    public final TimeoutPolicy timeout;

    public FaultTolerancePolicy(boolean isFaultToleranceEnabled, boolean isMetricsEnabled, AsynchronousPolicy asynchronous,
            BulkheadPolicy bulkhead, CircuitBreakerPolicy circuitBreaker, FallbackPolicy fallback, RetryPolicy retry,
            TimeoutPolicy timeout) {
        this.expiresMillis = System.currentTimeMillis() + TTL;
        this.isFaultToleranceEnabled = isFaultToleranceEnabled;
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

    interface InvocationStage {
        Object run(FaultToleranceInvocation invocation) throws Exception;
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

        Object runStageWithWorker(InvocationStage stage) throws Exception {
            timeoutIfConcludedConcurrently();
            Thread current = Thread.currentThread();
            asyncWorkers.add(current);
            try {
                return stage.run(this);
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
        return processAsynchronousStage(context, execution);
    }

    /**
     * Stage that takes care of the {@link AsynchronousPolicy} handling.
     */
    private Object processAsynchronousStage(InvocationContext context, FaultToleranceExecution execution) throws Exception {
        FaultToleranceMetrics metrics = isMetricsEnabled ? execution.getMetrics(context) : null;
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
        };
        FaultToleranceInvocation invocation = new FaultToleranceInvocation(context, execution, metrics, asyncResult,
                asyncWorkers);
        execution.runAsynchronous(asyncResult, () -> invocation.runStageWithWorker(this::processFallbackStage));
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
            return processFallbackException(invocation, ex);
        }
    }

    private Object processFallbackException(FaultToleranceInvocation invocation, Exception ex) throws Exception {
        if (fallback.isHandlerPresent()) {
            return invocation.execution.fallbackHandle(fallback.value, invocation.context, ex);
        }
        return invocation.execution.fallbackInvoke(fallback.method, invocation.context);
    }

    /**
     * Stage that takes care of the {@link RetryPolicy} handling.
     */
    private Object processRetryStage(FaultToleranceInvocation invocation) throws Exception {
        int attemptsLeft = retry.totalAttempts();
        Long retryTimeoutTime = retry.timeoutTimeNow();
        while (attemptsLeft > 0) {
            attemptsLeft--;
            try {
                return isAsynchronous() ? processRetryAsync(invocation) : processCircuitBreakerStage(invocation, null);
            } catch (Exception ex) {
                if (attemptsLeft <= 0 
                        || !retry.retryOn(ex) 
                        || retryTimeoutTime != null && System.currentTimeMillis() >= retryTimeoutTime) {
                    throw ex;
                }
                if (retry.isDelayed()) {
                    invocation.execution.delay(retry.jitteredDelay(), invocation.context);
                }
            }
        }
        // this line should never be reached as we throw above
        throw new FaultToleranceException("Retry failed"); 
    }

    private Object processRetryAsync(FaultToleranceInvocation invocation) throws Exception {
        CompletableFuture<Object> asyncTry = new CompletableFuture<>();
        //TODO try if it works to use the asyncTry thread as inv asyncResult
        invocation.execution.runAsynchronous(asyncTry,
                () -> invocation.runStageWithWorker(inv -> processCircuitBreakerStage(inv, asyncTry)));
        try {
            asyncTry.get(); // wait and only proceed on success
            //TODO this below check should not be needed since it only could be done from a another try which only riggers if this fails with an exception
            // BUT need to remember that cancel can occur
            invocation.timeoutIfConcludedConcurrently();
            return asyncTry;
        } catch (ExecutionException ex) {
            throw (Exception) ex.getCause();
        }
    }

    /**
     * Stage that takes care of the {@link CircuitBreakerPolicy} handling.
     */
    private Object processCircuitBreakerStage(FaultToleranceInvocation invocation, CompletableFuture<Object> asyncTry) throws Exception {
        if (!isCircuitBreakerPresent()) {
            return processTimeoutStage(invocation, asyncTry);
        }
        CircuitBreakerState state = invocation.execution.getState(circuitBreaker.requestVolumeThreshold, invocation.context);
        Object resultValue = null;
        switch (state.getCircuitState()) {
        default:
        case OPEN:
            throw new CircuitBreakerOpenException();
        case HALF_OPEN:
            try {
                resultValue = processTimeoutStage(invocation, asyncTry);
            } catch (Exception ex) {
                if (circuitBreaker.failOn(ex)) {
                    state.open();
                    invocation.execution.scheduleDelayed(circuitBreaker.delay, state::halfOpen);
                }
                throw ex;
            }
            state.halfOpenSuccessful(circuitBreaker.successThreshold);
            return resultValue;
        case CLOSED:
            Exception failedOn = null;
            try {
                resultValue = processTimeoutStage(invocation, asyncTry);
                state.recordClosedResult(true);
            } catch (Exception ex) {
                if (circuitBreaker.failOn(ex)) {
                    state.recordClosedResult(false);
                }
                failedOn = ex;
            }
            if (state.isOverFailureThreshold(Math.round(circuitBreaker.requestVolumeThreshold * circuitBreaker.failureRatio))) {
                state.open();
                invocation.execution.scheduleDelayed(circuitBreaker.delay, state::halfOpen);
            }
            if (failedOn != null) {
                throw failedOn;
            }
            return resultValue;
        }
    }

    /**
     * Stage that takes care of the {@link TimeoutPolicy} handling.
     */
    private Object processTimeoutStage(FaultToleranceInvocation invocation, CompletableFuture<Object> asyncTry) throws Exception {
        if (!isTimeoutPresent()) {
            return processBulkheadStage(invocation);
        }
        long timeoutDuration = Duration.of(timeout.value, timeout.unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutDuration;
        Thread current = Thread.currentThread();
        AtomicBoolean didTimeout = new AtomicBoolean(false);
        Future<?> timeout = invocation.execution.scheduleDelayed(timeoutDuration, () -> { 
            didTimeout.set(true);
            current.interrupt();
            if (asyncTry != null) {
                // we do this since interrupting not necessarily returns directly or ever but the attempt should timeout now
                asyncTry.completeExceptionally(new TimeoutException());
            }
        });
        try {
            Object resultValue = processBulkheadStage(invocation);
            if (current.isInterrupted()) {
                Thread.interrupted(); // clear the flag
            }
            if (didTimeout.get() || System.currentTimeMillis() > timeoutTime) {
                throw new TimeoutException();
            }
            return resultValue;
        } catch (Exception ex) {
            if ((ex instanceof InterruptedException || ex.getCause() instanceof InterruptedException)
                    && System.currentTimeMillis() > timeoutTime) {
                throw new TimeoutException(ex); 
            }
            throw ex;
        } finally {
            timeout.cancel(true);
        }
    }

    /**
     * Stage that takes care of the {@link BulkheadPolicy} handling.
     */
    private Object processBulkheadStage(FaultToleranceInvocation invocation) throws Exception {
        if (!isBulkheadPresent()) {
            return processMethodCallStage(invocation);
        }
        BulkheadSemaphore executionSemaphore = invocation.execution.getExecutionSemaphoreOf(bulkhead.value, invocation.context);
        if (executionSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
            try {
                return processMethodCallStage(invocation);
            } finally {
                executionSemaphore.release();
            }
        }
        if (!isAsynchronous()) { // plain semaphore style, fail:
            throw new BulkheadException("No free work permits.");
        }
        // from here: queueing style:
        BulkheadSemaphore waitingQueueSemaphore = invocation.execution.getWaitingQueueSemaphoreOf(bulkhead.waitingTaskQueue, invocation.context);
        if (waitingQueueSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
            try {
                invocation.timeoutIfConcludedConcurrently(); // avoid execution if not needed
                executionSemaphore.acquire(); // block until execution permit becomes available
            } catch (InterruptedException ex) {
                waitingQueueSemaphore.release();
                throw new BulkheadException(ex);
            }
            waitingQueueSemaphore.release();
            try {
                return processMethodCallStage(invocation);
            } finally {
                executionSemaphore.release();
            }
        }
        throw new BulkheadException("No free work or queue permits.");
    }

    /**
     * Stage where the actual wrapped method call occurs.
     */
    private Object processMethodCallStage(FaultToleranceInvocation invocation) throws Exception {
        invocation.timeoutIfConcludedConcurrently();
        return invocation.context.proceed();
    }

}
