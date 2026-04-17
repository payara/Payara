/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import java.util.List;

/**
 * Class to define methods to register Telemetry Metrics for FaultTolerance
 */
public class FaultToleranceTelemetryMetricsRecorder {
    
    //invocations telemetry metrics properties
    private static final String FT_INVOCATIONS_TOTAL = "ft.invocations.total";
    private static final String FT_INVOCATIONS_TOTAL_DESCRIPTION = 
            """
            The number of times the method was called.
            """;
    private static final String FALLBACK_NAME = "fallback";
    private static final String METHOD_ATTRIBUTE_NAME = "method";
    
    //retry telemetry metrics properties
    private static final String FT_RETRY_CALLS_TOTAL = "ft.retry.calls.total";
    private static final String FT_RETRY_CALLS_TOTAL_DESCRIPTION = 
            """
            The number of times the retry was run.
            """;
    private static final String FT_RETRY_RETRIES_TOTAL = "ft.retry.retries.total";
    private static final String FT_RETRY_RETRIES_TOTAL_DESCRIPTION = """
            The number of time the method was retried.
            """;
    
    //timeout telemetry metrics properties
    private static final String  FT_TIMEOUT_CALLS_TOTAL = "ft.timeout.calls.total";
    private static final String FT_TIMEOUT_CALLS_TOTAL_DESCRIPTION = """
            The number of times the timeout logic was run.
            """;
    
    private static final String FT_TIMEOUT_EXECUTION_DURATION = "ft.timeout.executionDuration";
    private static final String FT_TIMEOUT_EXECUTION_DURATION_DESCRIPTION = """
            Histogram of the execution duration of the method.
            """;
    private static final List<Double> HISTOGRAM_BUCKETS = List.of(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0);
    
    //circuitBreaker telemetry metrics properties
    private static final String FT_CIRCUIT_BREAKER_CALLS_TOTAL = "ft.circuitbreaker.calls.total";
    private static final String FT_CIRCUIT_BREAKER_CALLS_TOTAL_DESCRIPTION = """
            The number of times the circuit breaker logic was run.
            """;
    private static final String FT_CIRCUIT_BREAKER_STATE_TOTAL= "ft.circuitbreaker.state.total";
    private static final String FT_CIRCUIT_BREAKER_STATE_TOTAL_DESCRIPTION = """
            Amount of time the circuit breaker spent in each state.
            """;
    private static final String FT_CIRCUIT_BREAKER_OPENED_TOTAL = "ft.circuitbreaker.opened.total";
    private static final String FT_CIRCUIT_BREAKER_OPENED_TOTAL_DESCRIPTION = """
            Number of times the circuit breaker moved from closed state to open state.
            """;
    //bulkhead telemetry metrics properties
    private static final String FT_BULKHEAD_CALLS_TOTAL = "ft.bulkhead.calls.total";
    private static final String FT_BULKHEAD_CALLS_TOTAL_DESCRIPTION = """
            The number of times the bulkhead logic was run.
            """;
    private static final String FT_BULKHEAD_EXECUTIONS_RUNNING = "ft.bulkhead.executionsRunning";
    private static final String FT_BULKHEAD_EXECUTIONS_RUNNING_DESCRIPTION = """
            Number of currently running executions.
            """;
    private static final String FT_BULKHEAD_RUNNING_DURATION = "ft.bulkhead.runningDuration";
    private static final String FT_BULKHEAD_RUNNING_DURATION_DESCRIPTION = """
            Histogram of the time that method executions spent running.
            """;
    private static final String FT_BULKHEAD_EXECUTION_WAITING = "ft.bulkhead.executionsWaiting";
    private static final String FT_BULKHEAD_EXECUTION_WAITING_DESCRIPTION = """
            Histogram of the time that method executions spent waiting.
            """;
    /**
     * this method will help to report ft.invocations.total metric for Fault Tolerance using Telemetry api
     * @param currentMeter
     */
    public static LongCounter createFTInvocationTotalMeter(String classAndMethodName, Meter currentMeter, boolean isFallback) {
        LongCounter longCounter = currentMeter.counterBuilder(FT_INVOCATIONS_TOTAL).setDescription(FT_INVOCATIONS_TOTAL_DESCRIPTION).build();
        if (isFallback) {
            longCounter.add(0, Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                            .stringKey("method"), classAndMethodName).build()).put(AttributeKey.stringKey("result"), "valueReturned")
                    .put(AttributeKey.stringKey("fallback"), "notApplied").build());
            longCounter.add(0, Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                            .stringKey("method"), classAndMethodName).build()).put(AttributeKey.stringKey("result"), "valueReturned")
                    .put(AttributeKey.stringKey("fallback"), "applied").build());
            longCounter.add(0, Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                            .stringKey("method"), classAndMethodName).build()).put(AttributeKey.stringKey("result"), "exceptionThrown")
                    .put(AttributeKey.stringKey("fallback"), "notApplied").build());
            longCounter.add(0, Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                            .stringKey("method"), classAndMethodName).build()).put(AttributeKey.stringKey("result"), "exceptionThrown")
                    .put(AttributeKey.stringKey("fallback"), "applied").build());

        } else {
            longCounter.add(0, Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                            .stringKey("method"), classAndMethodName).build()).put(AttributeKey.stringKey("result"), "valueReturned")
                    .put(AttributeKey.stringKey("fallback"), "notDefined").build());
            longCounter.add(0, Attributes.builder().putAll(Attributes.builder().put(AttributeKey
                            .stringKey("method"), classAndMethodName).build()).put(AttributeKey.stringKey("result"), "exceptionThrown")
                    .put(AttributeKey.stringKey("fallback"), "notDefined").build());
        }
        return longCounter;
    }

    /**
     * this method will help to report ft.retry.calls.total metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTRetryCallsTotal(String classAndMethodName, Meter currentMeter) {
        LongCounter longCounter = currentMeter.counterBuilder(FT_RETRY_CALLS_TOTAL).setDescription(FT_RETRY_CALLS_TOTAL_DESCRIPTION).build();
        AttributeKey<String> key = AttributeKey.stringKey("retryResult");
        AttributeKey<String> retriedKey = AttributeKey.stringKey("retried");
        Attributes attributes = Attributes.builder().putAll(getMethodAttribute(classAndMethodName)).put(key, "valueReturned").put(retriedKey, "false").build();
        longCounter.add(1, attributes);
    }

    /**
     * this method will help to report ft.retry.retries.total metric for Fault Tolerance using Telemetry api
     * @param currentMeter
     */
    public static void createFTRetryRetriesTotal(Meter currentMeter) {
        currentMeter.counterBuilder(FT_RETRY_RETRIES_TOTAL).setDescription(FT_RETRY_RETRIES_TOTAL_DESCRIPTION).build();
    }

    /**
     * this method will help to report ft.timeout.calls.total metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTTimeoutCallsTotal(String classAndMethodName, Meter currentMeter) {
        LongCounter longCounter = currentMeter.counterBuilder(FT_TIMEOUT_CALLS_TOTAL).setDescription(FT_TIMEOUT_CALLS_TOTAL_DESCRIPTION).build();
        AttributeKey<String> key = AttributeKey.stringKey("timedOut");
        Attributes attributes = Attributes.builder().putAll(getMethodAttribute(classAndMethodName)).put(key, "false").build();
        longCounter.add(1, attributes);
    }

    /**
     * this method will help to report ft.timeout.executionDuration metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTTimeoutExecutionDuration(String classAndMethodName, Meter currentMeter, long startTime) {
        DoubleHistogram doubleHistogram = currentMeter.histogramBuilder(FT_TIMEOUT_EXECUTION_DURATION).setDescription(FT_TIMEOUT_EXECUTION_DURATION_DESCRIPTION)
                .setUnit("seconds").setExplicitBucketBoundariesAdvice(HISTOGRAM_BUCKETS).build();
        double seconds = System.nanoTime() - startTime;
        doubleHistogram.record(seconds / 1_000_000_000d, getMethodAttribute(classAndMethodName));
    }

    /**
     * this method will help to report ft.circuitbreaker.calls.total metric for Fault Tolerance using Telemetry api
     * @param currentMeter
     */
    public static LongCounter createFTCircuitBreakerCallsTotal(Meter currentMeter) {
        return currentMeter.counterBuilder(FT_CIRCUIT_BREAKER_CALLS_TOTAL).setDescription(FT_CIRCUIT_BREAKER_CALLS_TOTAL_DESCRIPTION).build();
    }

    /**
     * this method will help to report ft.circuitbreaker.opened.total metric for Fault Tolerance using Telemetry api
     * @param currentMeter
     */
    public static LongCounter createFTCircuitBreakerOpenedTotal(Meter currentMeter) {
        return currentMeter.counterBuilder(FT_CIRCUIT_BREAKER_OPENED_TOTAL).setDescription(FT_CIRCUIT_BREAKER_OPENED_TOTAL_DESCRIPTION).build();
    }
    
    public static void createFTCircuitBreakerStateTotal(String classAndMethodName, Meter currentMeter) {
        LongCounter longCounterClosed = currentMeter.counterBuilder(FT_CIRCUIT_BREAKER_STATE_TOTAL).setUnit("nanoseconds")
                .setDescription(FT_CIRCUIT_BREAKER_STATE_TOTAL_DESCRIPTION).build();
        LongCounter longCounterHalfOpen = currentMeter.counterBuilder(FT_CIRCUIT_BREAKER_STATE_TOTAL).setUnit("nanoseconds")
                .setDescription(FT_CIRCUIT_BREAKER_STATE_TOTAL_DESCRIPTION).build();
        LongCounter longCounterOpen = currentMeter.counterBuilder(FT_CIRCUIT_BREAKER_STATE_TOTAL).setUnit("nanoseconds")
                .setDescription(FT_CIRCUIT_BREAKER_STATE_TOTAL_DESCRIPTION).build();
        longCounterClosed.add(1, Attributes.builder().putAll(getMethodAttribute(classAndMethodName)).put("state", "closed").build());
        longCounterHalfOpen.add(0, Attributes.builder().putAll(getMethodAttribute(classAndMethodName)).put("state", "halfOpen").build()); 
        longCounterOpen.add(0, Attributes.builder().putAll(getMethodAttribute(classAndMethodName)).put("state", "open").build());
    }

    /**
     * this method will help to report ft.bulkhead.calls.total metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTBulkheadCallsTotal(String classAndMethodName, Meter currentMeter) {
        LongCounter longCounter = currentMeter.counterBuilder(FT_BULKHEAD_CALLS_TOTAL).setDescription(FT_BULKHEAD_CALLS_TOTAL_DESCRIPTION).build();
        AttributeKey<String> key = AttributeKey.stringKey("bulkheadResult");
        Attributes attributes = Attributes.builder().putAll(getMethodAttribute(classAndMethodName)).put(key, "accepted").build();
        longCounter.add(1, attributes);      
    }

    /**
     * this method will help to report ft.bulkhead.runningDuration metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTBulkheadExecutionsRunning(String classAndMethodName, Meter currentMeter) {
        LongUpDownCounter longUpDownCounter = currentMeter.upDownCounterBuilder(FT_BULKHEAD_EXECUTIONS_RUNNING).setDescription(FT_BULKHEAD_EXECUTIONS_RUNNING_DESCRIPTION).build();
        longUpDownCounter.add(0, getMethodAttribute(classAndMethodName));
    }

    /**
     * this method will help to report ft.bulkhead.runningDuration metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTBulkheadRunningDuration(String classAndMethodName, Meter currentMeter, long startTime) {
        DoubleHistogram doubleHistogram = currentMeter.histogramBuilder(FT_BULKHEAD_RUNNING_DURATION).setDescription(FT_BULKHEAD_RUNNING_DURATION_DESCRIPTION)
                .setUnit("seconds").setExplicitBucketBoundariesAdvice(HISTOGRAM_BUCKETS).build();
        double seconds = System.nanoTime() - startTime;
        doubleHistogram.record(seconds / 1_000_000_000d, getMethodAttribute(classAndMethodName));
    }

    /**
     * this method will help to report ft.bulkhead.executionWaiting metric for Fault Tolerance using Telemetry api
     * @param classAndMethodName
     * @param currentMeter
     */
    public static void createFTBulkheadExecutionWaiting(String classAndMethodName, Meter currentMeter) {
        LongUpDownCounter longUpDownCounter = currentMeter.upDownCounterBuilder(FT_BULKHEAD_EXECUTION_WAITING).setDescription(FT_BULKHEAD_EXECUTION_WAITING_DESCRIPTION).build();
        longUpDownCounter.add(0, getMethodAttribute(classAndMethodName));
    }
    

    public static Attributes getMethodAttribute(String classAndMethodName) {
        return Attributes.builder().put(AttributeKey.stringKey(METHOD_ATTRIBUTE_NAME), classAndMethodName).build();
    }
    
}
