package fish.payara.microprofile.faulttolerance.policy;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;
import fish.payara.microprofile.faulttolerance.service.MethodFaultToleranceMetrics;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * Based on MP FT TCK Test {@code org.eclipse.microprofile.fault.tolerance.tck.metrics.BulkheadMetricTest}.
 *
 * @author Jan Bernitt
 */
public class BulkheadTckMetricTest extends AbstractBulkheadTest {

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
                };
            }
        };
    }

    @Test(timeout = 3000)
    public void bulkheadMetricAsyncTest() {
        callBulkheadWithNewThreadAndWaitFor(commonWaiter);
        callBulkheadWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 0);
        callBulkheadWithNewThreadAndWaitFor(commonWaiter);
        callBulkheadWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);

        assertFurtherThreadThrowsBulkheadException(1);

        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);

        String methodName = "fish.payara.microprofile.faulttolerance.policy.BulkheadTckMetricTest.bulkheadMetricAsyncTest_Method";
        Counter successfulInvocations = registry.getCounter(new MetricID("ft.invocations.total",
                new Tag("method", methodName),
                new Tag("fallback", "notDefined"),
                new Tag("result", "valueReturned")));
        Counter failedInvocations = registry.getCounter(new MetricID("ft.invocations.total",
                new Tag("method", methodName),
                new Tag("fallback", "notDefined"),
                new Tag("result", "exceptionThrown")));
        assertEquals(4, successfulInvocations.getCount());
        assertEquals(1, failedInvocations.getCount());
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<String> bulkheadMetricAsyncTest_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }

}
