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
package fish.payara.microprofile.faulttolerance;

import java.util.function.LongSupplier;

/**
 * Encodes the specifics of the FT metrics names using default methods while decoupling rest of the implementation from
 * the {@link org.eclipse.microprofile.metrics.MetricRegistry}.
 * 
 * The metrics are bound to a specific invocation context which is not an argument to each of the provided methods but
 * passed to the implementation upon construction. For another invocation another metrics instance is bound.
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
