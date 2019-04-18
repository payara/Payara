package fish.payara.microprofile.faulttolerance;

import java.util.function.LongSupplier;

/**
 * Encodes the specifics of the FT metrics names using default methods while decoupling rest of the implementation from
 * the {@link org.eclipse.microprofile.metrics.MetricRegistry}.
 *
 * @author Jan Bernitt
 */
public interface FaultToleranceMetrics {

    /**
     * Can be used as NULL object when metrics are disabled so avoid testing for enabled but still do essentially NOOPs.
     */
    FaultToleranceMetrics DISABLED = new FaultToleranceMetrics() { /* does nothing */ };

    /*
     * Generic (to be implemented/overridden)
     */

    /**
     * Counters:
     *
     * @param metric full name of the metric with {@code %s} used as placeholder for the method name
     */
    default void incrementCounter(String metric) {
        //NOOP (used when metrics are disabled)
    }

    /**
     * Histogram:
     *
     * @param metric full name of the metric with {@code %s} used as placeholder for the method name
     * @param nanos amount of nanoseconds to add to the histogram (>= 0)
     */
    default void addToHistogram(String metric, long nanos) {
        //NOOP (used when metrics are disabled)
    }

    /**
     * Gauge:
     *
     * @param metric full name of the metric with{@code %s} used as placeholder for the method name
     * @param gauge the gauge function to use in case the gauge is not already linked
     */
    default void linkGauge(String metric, LongSupplier gauge) {
        //NOOP (used when metrics are disabled)
    }


    /*
     * @Retry, @Timeout, @CircuitBreaker, @Bulkhead and @Fallback
     */

    default void incrementInvocationsTotal() {
        incrementCounter("ft.%s.invocations.total");
    }

    default void incrementInvocationsFailedTotal() {
        incrementCounter("ft.%s.invocations.failed.total");
    }


    /*
     * @Retry
     */

    default void incrementRetryCallsSucceededNotRetriedTotal() {
        incrementCounter("ft.%s.retry.callsSucceededNotRetried.total");
    }

    default void incrementRetryCallsSucceededRetriedTotal() {
        incrementCounter("ft.%s.retry.callsSucceededRetried.total");
    }

    default void incrementRetryCallsFailedTotal() {
        incrementCounter("ft.%s.retry.callsFailed.total");
    }

    default void incrementRetryRetriesTotal() {
        incrementCounter("ft.%s.retry.retries.total");
    }


    /*
     * @Timeout
     */

    default void addTimeoutExecutionDuration(long duration) {
        addToHistogram("ft.%s.timeout.executionDuration", duration);
    }

    default void incrementTimeoutCallsTimedOutTotal() {
        incrementCounter("ft.%s.timeout.callsTimedOut.total");
    }

    default void incrementTimeoutCallsNotTimedOutTotal() {
        incrementCounter("ft.%s.timeout.callsNotTimedOut.total");
    }


    /*
     * @CircuitBreaker
     */

    default void incrementCircuitbreakerCallsSucceededTotal() {
        incrementCounter("ft.%s.circuitbreaker.callsSucceeded.total");
    }

    default void incrementCircuitbreakerCallsFailedTotal() {
        incrementCounter("ft.%s.circuitbreaker.callsFailed.total");
    }

    default void incrementCircuitbreakerCallsPreventedTotal() {
        incrementCounter("ft.%s.circuitbreaker.callsPrevented.total");
    }

    default void incrementCircuitbreakerOpenedTotal() {
        incrementCounter("ft.%s.circuitbreaker.opened.total");
    }

    default void linkCircuitbreakerOpenTotal(LongSupplier gauge) {
        linkGauge("ft.%s.circuitbreaker.open.total", gauge);
    }

    default void linkCircuitbreakerHalfOpenTotal(LongSupplier gauge) {
        linkGauge("ft.%s.circuitbreaker.halfOpen.total", gauge);
    }

    default void linkCircuitbreakerClosedTotal(LongSupplier gauge) {
        linkGauge("ft.%s.circuitbreaker.closed.total", gauge);
    }


    /*
     * @Bulkhead
     */

    default void incrementBulkheadCallsAcceptedTotal() {
        incrementCounter("ft.%s.bulkhead.callsAccepted.total");
    }

    default void incrementBulkheadCallsRejectedTotal() {
        incrementCounter("ft.%s.bulkhead.callsRejected.total");
    }

    default void linkBulkheadConcurrentExecutions(LongSupplier gauge) {
        linkGauge("ft.%s.bulkhead.concurrentExecutions", gauge);
    }

    default void linkBulkheadWaitingQueuePopulation(LongSupplier gauge) {
        linkGauge("ft.%s.bulkhead.waitingQueue.population", gauge);
    }

    default void addBulkheadExecutionDuration(long duration) {
        addToHistogram("ft.%s.bulkhead.executionDuration", duration);
    }

    default void addBulkheadWaitingDuration(long duration) {
        addToHistogram("ft.%s.bulkhead.waiting.duration", duration);
    }


    /*
     * @Fallback
     */

    default void incrementFallbackCallsTotal() {
        incrementCounter("ft.%s.fallback.calls.total");
    }
}
