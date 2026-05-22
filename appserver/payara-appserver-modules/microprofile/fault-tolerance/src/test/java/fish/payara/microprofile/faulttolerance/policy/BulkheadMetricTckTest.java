/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Future;

import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.Assert;
import org.junit.Test;

/**
 * Based on MP FT TCK Test {@code org.eclipse.microprofile.fault.tolerance.tck.metrics.BulkheadMetricTest}.
 *
 * @author Jan Bernitt
 */
public class BulkheadMetricTckTest extends AbstractMetricTest {

    /**
     * Scenario is equivalent to the TCK test of same name but not 100% identical
     */
    @Test(timeout = 3000)
    public void bulkheadMetricAsyncTest() {
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 0);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);

        assertFurtherThreadThrowsBulkheadException(1);

        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);

        String methodName = "fish.payara.microprofile.faulttolerance.policy.BulkheadMetricTckTest.bulkheadMetricAsyncTest_Method";
        Counter successfulInvocations = registry.getCounter(new MetricID("ft.invocations.total",
                new Tag("method", methodName),
                new Tag("fallback", "notDefined"),
                new Tag("result", "valueReturned")));
        Counter failedInvocations = registry.getCounter(new MetricID("ft.invocations.total",
                new Tag("method", methodName),
                new Tag("fallback", "notDefined"),
                new Tag("result", "exceptionThrown")));
        Assert.assertEquals(4, successfulInvocations.getCount());
        Assert.assertEquals(1, failedInvocations.getCount());
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<String> bulkheadMetricAsyncTest_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }

    /**
     * Scenario is equivalent to the TCK test of same name but not 100% identical
     */
    @Test(timeout = 3000)
    public void bulkheadMetricHistogramTest() {
        final long delayMs = 100;
        long beforeInitTimestamp = System.nanoTime();
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        // time spent during initialization of , it needs to be added to the expected time
        waitUntilPermitsAquired(2, 0);

        assertFurtherThreadThrowsBulkheadException(1);

        long waitTimeForOneMethod = TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - beforeInitTimestamp)) / 2 + delayMs;

        waitSome(delayMs);
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);

        Histogram executionTimes = registry.getHistogram(new MetricID("ft.bulkhead.runningDuration",
                new Tag("method", "fish.payara.microprofile.faulttolerance.policy.BulkheadMetricTckTest.bulkheadMetricHistogramTest_Method")));
        Snapshot snap = executionTimes.getSnapshot();
        Snapshot.PercentileValue[] percentile = snap.percentileValues();
        Optional<Snapshot.PercentileValue> optPercentileMedian = Arrays.stream(percentile).filter(p -> p.getPercentile() == 0.5).findAny();
        Assert.assertNotNull(executionTimes);
        Assert.assertEquals(2, executionTimes.getCount());
        Assert.assertTrue(optPercentileMedian.isPresent());
        assertApproxMillis("Testing median, should be " + delayMs + " - " + waitTimeForOneMethod, delayMs, waitTimeForOneMethod, Math.round(optPercentileMedian.get().getValue()));
        assertApproxMillis("Testing mean of 2, should be " + delayMs + " - " + waitTimeForOneMethod, delayMs, waitTimeForOneMethod, Math.round(snap.getMean()));

        // Now let's put some quick results through the bulkhead
        callMethodDirectly(null);
        callMethodDirectly(null);

        Assert.assertEquals(4, executionTimes.getCount());
        snap = executionTimes.getSnapshot();
        assertApproxMillis("Testing mean of 4, should be " + (delayMs / 2) + " - " + (waitTimeForOneMethod / 2), delayMs / 2, waitTimeForOneMethod / 2, Math.round(snap.getMean()));
    }

    @Bulkhead(2)
    public String bulkheadMetricHistogramTest_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    /**
     * Scenario is equivalent to the TCK test of same name but not 100% identical
     */
    @Test(timeout = 3000)
    public void bulkheadMetricRejectionTest() {
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 0);
        assertFurtherThreadThrowsBulkheadException(1);

        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);

        String methodName = "fish.payara.microprofile.faulttolerance.policy.BulkheadMetricTckTest.bulkheadMetricRejectionTest_Method";
        @SuppressWarnings("unchecked")
        Gauge<Long> excutionsRunning = (Gauge<Long>) registry.getGauge(new MetricID("ft.bulkhead.executionsRunning",
                new Tag("method", methodName)));
        Assert.assertNotNull(excutionsRunning);
        Assert.assertEquals(0, excutionsRunning.getValue().intValue());
        Counter acceptedCalls = registry.getCounter(new MetricID("ft.bulkhead.calls.total",
                new Tag("method", methodName),
                new Tag("bulkheadResult", "accepted")));
        Assert.assertNotNull(acceptedCalls);
        Assert.assertEquals(2, acceptedCalls.getCount());
        Counter rejectedCalls = registry.getCounter(new MetricID("ft.bulkhead.calls.total",
                new Tag("method", methodName),
                new Tag("bulkheadResult", "rejected")));
        Assert.assertNotNull(rejectedCalls);
        Assert.assertEquals(1, rejectedCalls.getCount());
    }

    @Bulkhead(2)
    public String bulkheadMetricRejectionTest_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    private static void assertApproxMillis(String msg, long expectedMillis, long expectedMillisWithWait, long actualNanos) {
        long millis = actualNanos / 1_000_000;
        long max = Math.round(expectedMillisWithWait * 1.3);
        assertThat(msg, millis, allOf(greaterThan(expectedMillis), lessThan(max)));
    }
}
