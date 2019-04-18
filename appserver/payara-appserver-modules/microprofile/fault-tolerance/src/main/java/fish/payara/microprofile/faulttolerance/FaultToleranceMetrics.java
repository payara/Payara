package fish.payara.microprofile.faulttolerance;

import java.util.function.LongSupplier;

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
    default void increment(String metric) {
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
    default void add(String metric, long nanos) {
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
    default void insert(String metric, LongSupplier gauge) {
        //NOOP (used when metrics are disabled)
    }


    /*
     * @Retry, @Timeout, @CircuitBreaker, @Bulkhead and @Fallback
     */

    default void incrementInvocationsTotal() {
        increment("ft.%s.invocations.total");
    }

    default void incrementInvocationsFailedTotal() {
        increment("ft.%s.invocations.failed.total");
    }


    /*
     * @Retry
     */

    default void incrementRetryCallsSucceededNotRetriedTotal() {
        increment("ft.%s.retry.callsSucceededNotRetried.total");
    }

    default void incrementRetryCallsSucceededRetriedTotal() {
        increment("ft.%s.retry.callsSucceededRetried.total");
    }

    default void incrementRetryCallsFailedTotal() {
        increment("ft.%s.retry.callsFailed.total");
    }

    default void incrementRetryRetriesTotal() {
        increment("ft.%s.retry.retries.total");
    }


    /*
     * @Timeout
     */

    default void addTimeoutExecutionDuration(long duration) {
        add("ft.%s.timeout.executionDuration", duration);
    }

    default void incrementTimeoutCallsTimedOutTotal() {
        increment("ft.%s.timeout.callsTimedOut.total");
    }

    default void incrementTimeoutCallsNotTimedOutTotal() {
        increment("ft.%s.timeout.callsNotTimedOut.total");
    }


    /*
     * @CircuitBreaker
     */

    default void incrementCircuitbreakerCallsSucceededTotal() {
        increment("ft.%s.circuitbreaker.callsSucceeded.total");
    }

    default void incrementCircuitbreakerCallsFailedTotal() {
        increment("ft.%s.circuitbreaker.callsFailed.total");
    }

    default void incrementCircuitbreakerCallsPreventedTotal() {
        increment("ft.%s.circuitbreaker.callsPrevented.total");
    }

    default void incrementCircuitbreakerOpenedTotal() {
        increment("ft.%s.circuitbreaker.opened.total");
    }

    default void insertCircuitbreakerOpenTotal(LongSupplier gauge) {
        insert("ft.%s.circuitbreaker.open.total", gauge);
    }

    default void insertCircuitbreakerHalfOpenTotal(LongSupplier gauge) {
        insert("ft.%s.circuitbreaker.halfOpen.total", gauge);
    }

    default void insertCircuitbreakerClosedTotal(LongSupplier gauge) {
        insert("ft.%s.circuitbreaker.closed.total", gauge);
    }


    /*
     * @Bulkhead
     */

    default void incrementBulkheadCallsAcceptedTotal() {
        increment("ft.%s.bulkhead.callsAccepted.total");
    }

    default void incrementBulkheadCallsRejectedTotal() {
        increment("ft.%s.bulkhead.callsRejected.total");
    }

    default void insertBulkheadConcurrentExecutions(LongSupplier gauge) {
        insert("ft.%s.bulkhead.concurrentExecutions", gauge);
    }

    default void insertBulkheadWaitingQueuePopulation(LongSupplier gauge) {
        insert("ft.%s.bulkhead.waitingQueue.population", gauge);
    }

    default void addBulkheadExecutionDuration(long duration) {
        add("ft.%s.bulkhead.executionDuration", duration);
    }

    default void addBulkheadWaitingDuration(long duration) {
        add("ft.%s.bulkhead.waiting.duration", duration);
    }


    /*
     * @Fallback
     */

    default void incrementFallbackCallsTotal() {
        increment("ft.%s.fallback.calls.total");
    }
}
