/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;
import fish.payara.microprofile.faulttolerance.service.MethodFaultToleranceMetrics;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * Based on MP FT TCK Test {@code org.eclipse.microprofile.fault.tolerance.tck.metrics.AllMetricsTest}.
 *
 * @author Jan Bernitt
 */
public class AllMetricsTckTest extends AbstractRecordingTest {

    MetricRegistry registry;

    @Override
    protected FaultToleranceServiceStub createService() {
        registry = new MetricRegistryImpl(Type.BASE);
        return new FaultToleranceServiceStub() {

            @Override
            public FaultToleranceMethodContext getMethodContext(InvocationContext context, FaultTolerancePolicy policy) {
                FaultToleranceMetrics metrics = new MethodFaultToleranceMetrics(registry, FaultToleranceUtils.getCanonicalMethodName(context));
                return new FaultToleranceMethodContextStub(context, state, concurrentExecutions, waitingQueuePopulation) {

                    @Override
                    public FaultToleranceMetrics getMetrics(boolean enabled) {
                        return metrics;
                    }

                    @Override
                    public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
                        return CompletableFuture.completedFuture(null);
                    }
                };
            }
        };
    }

    private enum FTMetrics {

        INVOCATIONS_TOTAL("ft.invocations.total", Counter.class, null),
        RETRY_CALLS_TOTAL("ft.retry.calls.total", Counter.class, null),
        TIMEOUT_CALLS_TOTAL("ft.timeout.calls.total", Counter.class, null),
        RETRY_RETRIES_TOTAL("ft.retry.retries.total", Counter.class, null),
        TIMEOUT_EXECUTIONDURATION("ft.timeout.executionDuration", Histogram.class, MetricUnits.NANOSECONDS),
        CIRCUITBREAKER_CALLS_TOTAL("ft.circuitbreaker.calls.total", Counter.class, null),
        CIRCUITBREAKER_STATE_TOTAL("ft.circuitbreaker.state.total", Gauge.class, MetricUnits.NANOSECONDS),
        CIRCUITBREAKER_OPENED_TOTAL("ft.circuitbreaker.opened.total", Counter.class, null),
        BULKHEAD_CALLS_TOTAL("ft.bulkhead.calls.total", Counter.class, null),
        BULKHEAD_EXECUTIONSRUNNING("ft.bulkhead.executionsRunning", Gauge.class, null),
        BULKHEAD_EXECUTIONSWAITING("ft.bulkhead.executionsWaiting", Gauge.class, null),
        BULKHEAD_RUNNINGDURATION("ft.bulkhead.runningDuration", Histogram.class, MetricUnits.NANOSECONDS),
        BULKHEAD_WAITINGDURATION("ft.bulkhead.waitingDuration", Histogram.class, MetricUnits.NANOSECONDS);

        final String name;
        final Class<? extends Metric> type;
        final String unit;

        FTMetrics(String name, Class<? extends Metric> type, String unit) {
            this.name = name;
            this.type= type;
            this.unit = unit == null ? MetricUnits.NONE : unit;
        }
    }

    @Test
    public void testMetricUnits() throws Exception  {
        Future<?> res = (Future<?>) callMethodDirectly(null);
        assertEquals("Success", res.get());

        for (FTMetrics metric : FTMetrics.values()) {
            String name = metric.name;
            Metadata metadata = registry.getMetadata(name);
            assertNotNull("Missing metadata for metric " + name, metadata);
            assertEquals("Incorrect type for metric " + name, MetricType.from(metric.type), metadata.getTypeRaw());
            assertEquals("Incorrect unit for metric" + name, metric.unit, metadata.getUnit());
        }
    }

    @Retry(maxRetries = 5)
    @Bulkhead(3)
    @Timeout(value = 1, unit = ChronoUnit.MINUTES)
    @CircuitBreaker(failureRatio = 1.0, requestVolumeThreshold = 20)
    @Fallback(fallbackMethod = "doFallback")
    @Asynchronous
    public Future<String> testMetricUnits_Method(CompletableFuture<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }

    public Future<String> doFallback(CompletableFuture<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }
}
