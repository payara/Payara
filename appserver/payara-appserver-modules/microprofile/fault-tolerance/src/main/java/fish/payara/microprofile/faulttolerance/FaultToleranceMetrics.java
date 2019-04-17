package fish.payara.microprofile.faulttolerance;

import java.util.function.LongSupplier;

import javax.interceptor.InvocationContext;

/**
 * Encodes the specifics of the FT metrics names using default methods while decoupling
 * {@link org.eclipse.microprofile.metrics.MetricRegistry}.
 *
 * @author Jan Bernitt
 */
public interface FaultToleranceMetrics {

    FaultToleranceMetrics DISABLED = new FaultToleranceMetrics() { /* does nothing */ };

    /*
     * Generic (to be implemented/overridden)
     */

    /**
     * Counters:
     *
     * @param metric
     * @param annotationType
     * @param context
     */
    default void increment(String metric, InvocationContext context) {
        //NOOP (used when metrics are disabled)
    }

    /**
     * Histogram:
     *
     * @param metric
     * @param nanos
     * @param annotationType
     * @param context
     */
    default void add(String metric, long nanos, InvocationContext context) {
        //NOOP (used when metrics are disabled)
    }

    /**
     * Gauge:
     *
     * @param metric
     * @param gauge
     * @param annotationType
     * @param context
     */
    default void insert(String metric, LongSupplier gauge, InvocationContext context) {
        //NOOP (used when metrics are disabled)
    }


    /*
     * @Retry, @Timeout, @CircuitBreaker, @Bulkhead and @Fallback
     */

    default void incrementInvocationsTotal(InvocationContext context) {
        increment("ft.%s.invocations.total", context);
    }

    default void incrementInvocationsFailedTotal(InvocationContext context) {
        increment("ft.%s.invocations.failed.total", context);
    }


    /*
     * @Retry
     */

    default void incrementRetryCallsSucceededNotRetriedTotal(InvocationContext context) {
        increment("ft.%s.retry.callsSucceededNotRetried.total", context);
    }

    default void incrementRetryCallsSucceededRetriedTotal(InvocationContext context) {
        increment("ft.%s.retry.callsSucceededRetried.total", context);
    }

    default void incrementRetryCallsFailedTotal(InvocationContext context) {
        increment("ft.%s.retry.callsFailed.total", context);
    }

    default void incrementRetryRetriesTotal(InvocationContext context) {
        increment("ft.%s.retry.retries.total", context);
    }


    /*
     * @Timeout
     */

    default void addTimeoutExecutionDuration(long duration, InvocationContext context) {
        add("ft.%s.timeout.executionDuration", duration, context);
    }

    default void incrementTimeoutCallsTimedOutTotal(InvocationContext context) {
        increment("ft.%s.timeout.callsTimedOut.total", context);
    }

    default void incrementTimeoutCallsNotTimedOutTotal(InvocationContext context) {
        increment("ft.%s.timeout.callsNotTimedOut.total", context);
    }


    /*
     * @CircuitBreaker
     */

    default void incrementCircuitbreakerCallsSucceededTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.callsSucceeded.total", context);
    }

    default void incrementCircuitbreakerCallsFailedTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.callsFailed.total", context);
    }

    default void incrementCircuitbreakerCallsPreventedTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.callsPrevented.total", context);
    }

    default void incrementCircuitbreakerOpenedTotal(InvocationContext context) {
        increment("ft.%s.circuitbreaker.opened.total", context);
    }

    default void insertCircuitbreakerOpenTotal(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.circuitbreaker.open.total", gauge, context);
    }

    default void insertCircuitbreakerHalfOpenTotal(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.circuitbreaker.halfOpen.total", gauge, context);
    }

    default void insertCircuitbreakerClosedTotal(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.circuitbreaker.closed.total", gauge, context);
    }


    /*
     * @Bulkhead
     */

    default void incrementBulkheadCallsAcceptedTotal(InvocationContext context) {
        increment("ft.%s.bulkhead.callsAccepted.total", context);
    }

    default void incrementBulkheadCallsRejectedTotal(InvocationContext context) {
        increment("ft.%s.bulkhead.callsRejected.total", context);
    }

    default void insertBulkheadConcurrentExecutions(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.bulkhead.concurrentExecutions", gauge, context);
    }

    default void insertBulkheadWaitingQueuePopulation(LongSupplier gauge, InvocationContext context) {
        insert("ft.%s.bulkhead.waitingQueue.population", gauge, context);
    }

    default void addBulkheadExecutionDuration(long duration, InvocationContext context) {
        add("ft.%s.bulkhead.executionDuration", duration, context);
    }

    default void addBulkheadWaitingDuration(long duration, InvocationContext context) {
        add("ft.%s.bulkhead.waiting.duration", duration, context);
    }


    /*
     * @Fallback
     */

    default void incrementFallbackCallsTotal(InvocationContext context) {
        increment("ft.%s.fallback.calls.total", context);
    }
}
