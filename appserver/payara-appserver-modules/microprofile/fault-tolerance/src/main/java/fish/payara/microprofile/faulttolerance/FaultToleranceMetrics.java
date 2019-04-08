package fish.payara.microprofile.faulttolerance;

import java.lang.annotation.Annotation;
import java.util.function.LongSupplier;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Encodes the specifics of the FT metrics names using default methods while decoupling
 * {@link org.eclipse.microprofile.metrics.MetricRegistry}.
 *
 * @author Jan Bernitt
 */
public interface FaultToleranceMetrics {

    /*
     * Generic (to be implemented)
     */

    /**
     * Counters:
     *
     * @param metric
     * @param annotationType
     * @param context
     */
    void increment(String metric, Class<? extends Annotation> annotationType, InvocationContext context);

    /**
     * Histogram:
     *
     * @param metric
     * @param nanos
     * @param annotationType
     * @param context
     */
    void add(String metric, long nanos, Class<? extends Annotation> annotationType, InvocationContext context);

    /**
     * Gauge:
     *
     * @param metric
     * @param gauge
     * @param annotationType
     * @param context
     */
    void insert(String metric, LongSupplier gauge, Class<? extends Annotation> annotationType, InvocationContext context);


    /*
     * @Retry, @Timeout, @CircuitBreaker, @Bulkhead and @Fallback
     */

    default void incrementInvocationsTotal(Class<? extends Annotation> annotationType, InvocationContext context) {
        increment("ft.%s.invocations.total", annotationType, context);
    }

    default void incrementInvocationsFailedTotal(Class<? extends Annotation> annotationType, InvocationContext context) {
        increment("ft.%s.invocations.failed.total", annotationType, context);
    }


    /*
     * @Retry
     */

    default void incrementRetryCallsSucceededNotRetriedTotal(InvocationContext context) {
        increment("ft.%s.retry.callsSucceededNotRetried.total", Retry.class, context);
    }

    default void incrementRetryCallsSucceededRetriedTotal(InvocationContext context) {
        increment("ft.%s.retry.callsSucceededRetried.total", Retry.class, context);
    }

    default void incrementRetryCallsFailedTotal(InvocationContext context) {
        increment("ft.%s.retry.callsFailed.total", Retry.class, context);
    }

    default void incrementRetryRetriesTotal(InvocationContext context) {
        increment("ft.%s.retry.retries.total", Retry.class, context);
    }


    /*
     * @Timeout
     */

    default void addTimeoutExecutionDuration(long duration, InvocationContext context) {
        add("ft.%s.timeout.executionDuration", duration, Timeout.class, context);
    }

    default void incrementTimeoutCallsTimedOutTotal(InvocationContext context) {
        increment("ft.%s.timeout.callsTimedOut.total", Timeout.class, context);
    }

    default void incrementTimeoutCallsNotTimedOutTotal(InvocationContext context) {
        increment("ft.%s.timeout.callsNotTimedOut.total", Timeout.class, context);
    }


    /*
     * @CircuitBreaker
     */

    default void incrementCircuitbreakerCallsSucceededTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.callsSucceeded.total", CircuitBreaker.class, context);
    }

    default void incrementCircuitbreakerCallsFailedTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.callsFailed.total", CircuitBreaker.class, context);
    }

    default void incrementCircuitbreakerCallsPreventedTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.callsPrevented.total", CircuitBreaker.class, context);
    }

    default void incrementCircuitbreakerOpenedTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.opened.total", CircuitBreaker.class, context);
    }

    default void insertCircuitbreakerOpenTotal(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.circuitbreaker.open.total", gauge, CircuitBreaker.class, context);
    }

    default void insertCircuitbreakerHalfOpenTotal(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.circuitbreaker.halfOpen.total", gauge, CircuitBreaker.class, context);
    }

    default void insertCircuitbreakerClosedTotal(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.circuitbreaker.closed.total", gauge, CircuitBreaker.class, context);
    }


    /*
     * @Bulkhead
     */

    default void incrementBulkheadCallsAcceptedTotal(InvocationContext context) {
        increment("ft.%s.bulkhead.callsAccepted.total", Bulkhead.class, context);
    }

    default void incrementBulkheadCallsRejectedTotal(InvocationContext context) {
        increment("ft.%s.bulkhead.callsRejected.total", Bulkhead.class, context);
    }

    default void insertBulkheadConcurrentExecutions(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.bulkhead.concurrentExecutions", gauge, Bulkhead.class, context);
    }

    default void insertBulkheadWaitingQueuePopulation(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.bulkhead.waitingQueue.population", gauge, Bulkhead.class, context);
    }

    default void addBulkheadExecutionDuration(long duration, InvocationContext context) {
        add("ft.%s.bulkhead.executionDuration", duration, Bulkhead.class, context);
    }

    default void addBulkheadWaitingDuration(long duration, InvocationContext context) {
        add("ft.%s.bulkhead.waiting.duration", duration, Bulkhead.class, context);
    }


    /*
     * @Fallback
     */

    default void incrementFallbackCallsTotal(InvocationContext context) {
        increment("ft.%s.fallback.calls.total", Fallback.class, context);
    }
}
