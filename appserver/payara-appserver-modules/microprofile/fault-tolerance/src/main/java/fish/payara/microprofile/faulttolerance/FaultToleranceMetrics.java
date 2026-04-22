/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import static fish.payara.microprofile.faulttolerance.FaultToleranceTelemetryMetricsRecorder.*;
import static java.util.Arrays.asList;
import static org.glassfish.internal.api.Globals.getDefaultHabitat;

import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextImpl;
import fish.payara.opentracing.OpenTelemetryService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

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

    enum FallbackUsage { applied, notApplied, notDefined }

    /**
     * Can be used as NULL object when metrics are disabled so avoid testing for enabled but still do essentially NOOPs.
     */
    FaultToleranceMetrics DISABLED = new FaultToleranceMetrics() {
        @Override
        public void addCircuitBreakerCallsTotal(LongCounter circuitBreakerCallsTotal) {
            
        } /* does nothing */

        @Override
        public void addCircuitBreakerOpenedTotal(LongCounter circuitBreakerOpenedTotal) {
            
        }

        @Override
        public void addFTInvocationTotalMeter(LongCounter ftInvocationTotalMeter) {
            
        }

        @Override
        public void addFTTimeoutCallsTotal(LongCounter ftTimeoutCallsTotal) {
            
        }

        @Override
        public void addFTTimeoutExecutionDuration(DoubleHistogram ftTimeoutExecutionDuration) {
            
        }

        @Override
        public LongCounter getCircuitBreakerCallsTotal() {
            return null;
        }

        @Override
        public LongCounter getCircuitBreakerOpendTotal() {
            return null;
        }

        @Override
        public LongCounter getInvocationsValueReturnedCounter() {
            return null;
        }

        @Override
        public LongCounter getTimeoutCallsCounter() {
            return null;
        }

        @Override
        public DoubleHistogram getFTTimeoutExecutionDuration() {
            return null;
        }

        @Override
        public void incrementCircuitBreakerCallsSuccessCount(LongCounter circuitBreakerCallsSuccessCount, Attributes attributes) {
            
        }

        @Override
        public void incrementCircuitBreakerCallsFailureCount(LongCounter circuitBreakerCallsFailureCount, Attributes attributes) {
            
        }

        @Override
        public void incrementCircuitBreakerCallsCircuitOpenCount(LongCounter circuitBreakerStateTotal, Attributes attributes) {
            
        }

        @Override
        public void incrementCircuitBreakerOpendTotalTelemetry(LongCounter circuitBreakerStateTotal, Attributes attributes) {
            
        }

        @Override
        public void incrementInvocationsValueReturnedCounter(LongCounter invocationsValueReturnedCounter, Attributes attributes) {
            
        }

        @Override
        public void incrementInvocationsExceptionThrownCounter(LongCounter invocationsValueReturnedCounter, Attributes attributes) {

        }

        @Override
        public void incrementTimeoutCallsCounter(LongCounter timeoutCallsCounter, Attributes attributes) {
            
        }

        @Override
        public void addTimeoutExecutionDuration(DoubleHistogram timeoutExecutionDuration, Attributes attributes, long nanos) {
            
        }

        @Override
        public void setClassAndMethodName(String classAndMethodName) {

        }

        @Override
        public String getClassAndMethodName() {
            return "";
        }
    };

    Tag[] NO_TAGS = new Tag[0];

    /**
     * @return a instance on the basis of this {@link FaultToleranceMetrics} that is properly adopted to the provided
     *         context and policy. This can be the same instance if no change is required or a new one if a change of
     *         internal state is required.
     *
     *         As part of binding to the context and policy this method also should register all metrics that make sense
     *         for the provided policy in case this has not been done already.
     */
    default FaultToleranceMetrics boundTo(FaultToleranceMethodContext context, FaultTolerancePolicy policy) {
        if (policy.isMetricsEnabled) {
            long startTime = System.nanoTime();
            OpenTelemetryService openTelemetryService = getDefaultHabitat().getService(OpenTelemetryService.class);
            String[] fallbackTag = policy.isFallbackPresent()
                    ? new String[] {"fallback", "applied", "notApplied"}
                    : new String[] {"fallback", "notDefined"};
            register(Counter.class.getTypeName(), "ft.invocations.total", new String[][]{
                {"result", "valueReturned", "exceptionThrown"}, fallbackTag});
            //place to register telemetry ft.invocations.total
            Meter currentMeter = openTelemetryService.getCurrentMeter();
            FaultToleranceMethodContextImpl faultToleranceMethodContext = (FaultToleranceMethodContextImpl) context;
            setClassAndMethodName(faultToleranceMethodContext.getClassName() + "." + faultToleranceMethodContext.getMethodName());
            addFTInvocationTotalMeter(createFTInvocationTotalMeter(getClassAndMethodName(), currentMeter, policy.isFallbackPresent()));
            
            if (policy.isRetryPresent()) {
                List<String> retryResultTag = new ArrayList<>(asList("retryResult", "valueReturned", "exceptionNotRetryable"));
                if (policy.retry.isMaxRetriesSet()) {
                    retryResultTag.add("maxRetriesReached");
                }
                if (policy.retry.isMaxDurationSet()) {
                    retryResultTag.add("maxDurationReached");
                }
                register(Counter.class.getTypeName(), "ft.retry.calls.total", new String[][]{
                    {"retried", "true", "false"}, retryResultTag.toArray(new String[0])});
                register(Counter.class.getTypeName(), "ft.retry.retries.total");
                //place to register telemetry ft.retry.calls.total and ft.retry.retries.total
                createFTRetryCallsTotal(getClassAndMethodName(), currentMeter);
                createFTRetryRetriesTotal(currentMeter);
            }
            if (policy.isTimeoutPresent()) {
                register(Counter.class.getTypeName(), "ft.timeout.calls.total", new String[][] {
                    {"timedOut", "true", "false"}});
                register(Histogram.class.getTypeName(), "ft.timeout.executionDuration");
                addFTTimeoutCallsTotal(createFTTimeoutCallsTotal(currentMeter));
                addFTTimeoutExecutionDuration(createFTTimeoutExecutionDuration(currentMeter));
            }
            if (policy.isCircuitBreakerPresent()) {
                register(Counter.class.getTypeName(), "ft.circuitbreaker.calls.total", new String[][] {
                    {"circuitBreakerResult", "success", "failure", "circuitBreakerOpen"}});
                CircuitBreakerState state = context.getState();
                register("ft.circuitbreaker.state.total", MetricUnits.NANOSECONDS, state::nanosOpen, "state", "open");
                register("ft.circuitbreaker.state.total", MetricUnits.NANOSECONDS, state::nanosHalfOpen, "state", "halfOpen");
                register("ft.circuitbreaker.state.total", MetricUnits.NANOSECONDS, state::nanosClosed, "state", "closed");
                register(Counter.class.getTypeName(), "ft.circuitbreaker.opened.total");
                addCircuitBreakerCallsTotal(createFTCircuitBreakerCallsTotal(currentMeter));
                addCircuitBreakerOpenedTotal(createFTCircuitBreakerOpenedTotal(currentMeter));
                createFTCircuitBreakerStateTotal(getClassAndMethodName(), currentMeter);
            }
            if (policy.isBulkheadPresent()) {
                register(Counter.class.getTypeName(), "ft.bulkhead.calls.total", new String[][] {
                    {"bulkheadResult", "accepted", "rejected"}});
                register(Histogram.class.getTypeName(), "ft.bulkhead.runningDuration");
                if (policy.isAsynchronous()) {
                    BlockingQueue<Thread> running = context.getConcurrentExecutions();
                    register("ft.bulkhead.executionsRunning", null, running::size);
                    AtomicInteger queuingOrRunning = context.getQueuingOrRunningPopulation();
                    register("ft.bulkhead.executionsWaiting", null, () -> Math.max(0, queuingOrRunning.get() - policy.bulkhead.value));
                    register(Histogram.class.getTypeName(), "ft.bulkhead.waitingDuration");
                } else {
                    AtomicInteger running = context.getQueuingOrRunningPopulation();
                    register("ft.bulkhead.executionsRunning", null, running::get);
                }
                createFTBulkheadCallsTotal(getClassAndMethodName(), currentMeter);
                createFTBulkheadExecutionsRunning(getClassAndMethodName(), currentMeter);
                createFTBulkheadRunningDuration(getClassAndMethodName(), currentMeter, startTime);
                createFTBulkheadExecutionWaiting(getClassAndMethodName(), currentMeter);
            }
        }
        return this;
    }

    /*
     * Generic (to be implemented/overridden)
     */


    /**
     * Gauge registration:
     *
     * @param metric name of the gauge metric
     * @param unit unit of the gauge metric (null is {@link MetricUnits#NONE})
     * @param gauge a supplier function for the gauge
     * @param tag tag name and value if the gauge uses a tag
     */
    default void register(String metric, String unit, LongSupplier gauge, String... tag) {
        //NOOP
    }

    /**
     * Registration:
     *
     * Registers a metric for each permutation of the tags. Infers unit from type.
     *
     * @param type {@link MetricType#COUNTER} (assumes no unit) or {@link MetricType#HISTOGRAM} (assumes {@link MetricUnits#NANOSECONDS})
     * @param metric name of the metric(s)
     * @param tags tag name and possible values
     */
    default void register(String metricType, String metric, String[]... tags) {
        //NOOP
    }

    /**
     * Counters:
     *
     * @param metric full name of the metric with {@code %s} used as placeholder for the method name
     * @param tags all tags to add for the metric except the {@code method} tag (which is added later internally)
     */
    default void incrementCounter(String metric, Tag... tags) {
        //NOOP (used when metrics are disabled)
    }

    default void incrementCounter(String metric) {
        incrementCounter(metric, NO_TAGS);
        
    }

    /**
     * Histogram:
     *
     * @param metric full name of the metric with {@code %s} used as placeholder for the method name
     * @param nanos amount of nanoseconds to add to the histogram (>= 0)
     * @param tags all tags to add for the metric except the {@code method} tag (which is added later internally)
     */
    default void addToHistogram(String metric, long nanos, Tag... tags) {
        //NOOP (used when metrics are disabled)
    }

    default void addToHistogram(String metric, long nanos) {
        addToHistogram(metric, nanos, NO_TAGS);
    }

    /*
     * @Retry, @Timeout, @CircuitBreaker, @Bulkhead and @Fallback
     */

    /**
     * The number of times the method was called when a value was returned.
     */
    default void incrementInvocationsValueReturned() {
        incrementCounter("ft.invocations.total",
                new Tag("result", "valueReturned"),
                new Tag("fallback", getFallbackUsage().name()));
        incrementInvocationsValueReturnedCounter(getInvocationsValueReturnedCounter(), Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                .stringKey("method"), getClassAndMethodName()).build()).put("result", "valueReturned").put("fallback", getFallbackUsage().name()).build());
    }

    /**
     * The number of times the method was called when an exception was thrown.
     */
    default void incrementInvocationsExceptionThrown() {
        incrementCounter("ft.invocations.total",
                new Tag("result", "exceptionThrown"),
                new Tag("fallback", getFallbackUsage().name()));
        incrementInvocationsExceptionThrownCounter(getInvocationsValueReturnedCounter(), Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                .stringKey("method"), getClassAndMethodName()).build()).put("result", "exceptionThrown").put("fallback", getFallbackUsage().name()).build());
    }

    /**
     * @return How fallback was involved in the current execution of the method
     */
    default FallbackUsage getFallbackUsage() {
        return FallbackUsage.notDefined;
    }


    /*
     * @Retry
     */

    /**
     * The number of times the retry logic was run. This will always be once per method call.
     *
     * When value was returned.
     */
    default void incrementRetryCallsValueReturned() {
        incrementCounter("ft.retry.calls.total",
                new Tag("retried", String.valueOf(isRetried())),
                new Tag("retryResult", "valueReturned"));
    }

    /**
     * The number of times the retry logic was run. This will always be once per method call.
     *
     * When an exception occurred that does not lead to retries.
     */
    default void incrementRetryCallsExceptionNotRetryable() {
        incrementCounter("ft.retry.calls.total",
                new Tag("retried", String.valueOf(isRetried())),
                new Tag("retryResult", "exceptionNotRetryable"));
    }

    /**
     * The number of times the retry logic was run. This will always be once per method call.
     *
     * When maximum total time for retries has been exceeded.
     */
    default void incrementRetryCallsMaxDurationReached() {
        incrementCounter("ft.retry.calls.total",
                new Tag("retried", String.valueOf(isRetried())),
                new Tag("retryResult", "maxDurationReached"));
    }

    /**
     * The number of times the retry logic was run. This will always be once per method call.
     *
     * When maximum number of retries has been reached.
     */
    default void incrementRetryCallsMaxRetriesReached() {
        incrementCounter("ft.retry.calls.total",
                new Tag("retried", String.valueOf(isRetried())),
                new Tag("retryResult", "maxRetriesReached"));
    }

    /**
     * The number of times the method was retried
     */
    default void incrementRetryRetriesTotal() {
        incrementCounter("ft.retry.retries.total");
    }

    /**
     * @return true if {@link #incrementRetryRetriesTotal()} has been called at least once, otherwise false
     */
    default boolean isRetried() {
        return false;
    }

    /*
     * @Timeout
     */

    /**
     * Histogram of execution times for the method
     *
     * @param nanos Nanoseconds
     */
    default void addTimeoutExecutionDuration(long nanos) {
        addToHistogram("ft.timeout.executionDuration", nanos);
        addTimeoutExecutionDuration(this.getFTTimeoutExecutionDuration(), Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                .stringKey("method"), getClassAndMethodName()).build()).build(), nanos);
    }


    /**
     * The number of times the timeout logic was run. This will usually be once per method call, but may be zero times
     * if the circuit breaker prevents execution or more than once if the method is retried.
     *
     * When method call timed out
     */
    default void incrementTimeoutCallsTimedOutTotal() {
        incrementCounter("ft.timeout.calls.total",
                new Tag("timedOut", "true"));
        incrementTimeoutCallsCounter(getTimeoutCallsCounter(), Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                .stringKey("method"), getClassAndMethodName()).build()).put("timeOut", "true").build());
    }
    

    /**
     * The number of times the timeout logic was run. This will usually be once per method call, but may be zero times
     * if the circuit breaker prevents execution or more than once if the method is retried.
     *
     * When method call did not time out
     */
    default void incrementTimeoutCallsNotTimedOutTotal() {
        incrementCounter("ft.timeout.calls.total",
                new Tag("timedOut", "false"));
        incrementTimeoutCallsCounter(getTimeoutCallsCounter(), Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                .stringKey("method"), getClassAndMethodName()).build()).put("timeOut", "false").build());
    }

    


    /*
     * @CircuitBreaker
     */

    /**
     * The number of times the circuit breaker logic was run. This will usually be once per method call, but may be more
     * than once if the method call is retried.
     *
     * The method ran and was successful
     */
    default void incrementCircuitbreakerCallsSucceededTotal() {
        incrementCounter("ft.circuitbreaker.calls.total",
                new Tag("circuitBreakerResult", "success"));
        incrementCircuitBreakerCallsSuccessCount(getCircuitBreakerCallsTotal(), 
                Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                        .stringKey("method"), getClassAndMethodName()).build()).put("circuitBreakerResult", "success").build());
    }
    
    /**
     * The number of times the circuit breaker logic was run. This will usually be once per method call, but may be more
     * than once if the method call is retried.
     *
     * The method ran and failed
     */
    default void incrementCircuitbreakerCallsFailedTotal() {
        incrementCounter("ft.circuitbreaker.calls.total",
                new Tag("circuitBreakerResult", "failure"));
        incrementCircuitBreakerCallsFailureCount(getCircuitBreakerCallsTotal(), 
                Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                        .stringKey("method"), getClassAndMethodName()).build()).put("circuitBreakerResult", "failure").build());
    }

    /**
     * The number of times the circuit breaker logic was run. This will usually be once per method call, but may be more
     * than once if the method call is retried.
     *
     * The method did not run because the circuit breaker was in open state.
     */
    default void incrementCircuitbreakerCallsPreventedTotal() {
        incrementCounter("ft.circuitbreaker.calls.total",
                new Tag("circuitBreakerResult", "circuitBreakerOpen"));
        incrementCircuitBreakerCallsCircuitOpenCount(getCircuitBreakerCallsTotal(), 
                Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                        .stringKey("method"), getClassAndMethodName()).build()).put("circuitBreakerResult", "circuitBreakerOpen").build());
    }

    /**
     * Number of times the circuit breaker has moved from closed state to open state
     */
    default void incrementCircuitbreakerOpenedTotal() {
        incrementCounter("ft.circuitbreaker.opened.total");
        incrementCircuitBreakerOpendTotalTelemetry(getCircuitBreakerOpendTotal(), Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                .stringKey("method"), getClassAndMethodName()).build()).build());
    }

    /*
     * @Bulkhead
     */

    /**
     * The number of times the bulkhead logic was run. This will usually be once per method call, but may be zero times
     * if the circuit breaker prevented execution or more than once if the method call is retried.
     *
     * Bulkhead allowed the method call to run.
     */
    default void incrementBulkheadCallsAcceptedTotal() {
        incrementCounter("ft.bulkhead.calls.total",
                new Tag("bulkheadResult", "accepted"));
    }

    /**
     * The number of times the bulkhead logic was run. This will usually be once per method call, but may be zero times
     * if the circuit breaker prevented execution or more than once if the method call is retried.
     *
     * Bulkhead did not allow the method call to run.
     */
    default void incrementBulkheadCallsRejectedTotal() {
        incrementCounter("ft.bulkhead.calls.total",
                new Tag("bulkheadResult", "rejected"));
    }

    /**
     * Histogram of the time that method executions spent running
     *
     * @param nanos Nanoseconds
     */
    default void addBulkheadExecutionDuration(long nanos) {
        addToHistogram("ft.bulkhead.runningDuration", nanos);
    }

    /**
     * Histogram of the time that method executions spent waiting in the queue
     *
     * Only added if the method is also annotated with {@link Asynchronous}
     *
     * @param nanos Nanoseconds
     */
    default void addBulkheadWaitingDuration(long nanos) {
        addToHistogram("ft.bulkhead.waitingDuration", nanos);
    }


    /*
     * @Fallback
     */

    default void incrementFallbackCallsTotal() {
        //NOOP
    }
    
    public void addCircuitBreakerCallsTotal(LongCounter circuitBreakerCallsTotal);
    
    public void addCircuitBreakerOpenedTotal(LongCounter circuitBreakerOpenedTotal);
    
    public void addFTInvocationTotalMeter(LongCounter ftInvocationTotalMeter);

    public void addFTTimeoutCallsTotal(LongCounter ftTimeoutCallsTotal);

    public void addFTTimeoutExecutionDuration(DoubleHistogram ftTimeoutExecutionDuration);
    
    public LongCounter getCircuitBreakerCallsTotal();
    
    public LongCounter getCircuitBreakerOpendTotal();
    
    public LongCounter getInvocationsValueReturnedCounter();
    
    public LongCounter getTimeoutCallsCounter();
    
    public DoubleHistogram getFTTimeoutExecutionDuration();
    
    public void incrementCircuitBreakerCallsSuccessCount(LongCounter circuitBreakerCallsSuccessCount, Attributes attributes);
    
    public void incrementCircuitBreakerCallsFailureCount(LongCounter circuitBreakerCallsFailureCount, Attributes attributes);
    
    public void incrementCircuitBreakerCallsCircuitOpenCount(LongCounter circuitBreakerStateTotal, Attributes attributes);
    
    public void incrementCircuitBreakerOpendTotalTelemetry(LongCounter circuitBreakerOpendTotal, Attributes attributes);
    
    public void incrementInvocationsValueReturnedCounter(LongCounter invocationsValueReturnedCounter, Attributes attributes);
    
    public void incrementInvocationsExceptionThrownCounter(LongCounter invocationsValueReturnedCounter, Attributes attributes);
    
    public void incrementTimeoutCallsCounter(LongCounter timeoutCallsCounter, Attributes attributes);

    public void addTimeoutExecutionDuration(DoubleHistogram timeoutExecutionDuration, Attributes attributes, long nanos);
    
    public void setClassAndMethodName(String classAndMethodName);
    
    public String getClassAndMethodName();
}
