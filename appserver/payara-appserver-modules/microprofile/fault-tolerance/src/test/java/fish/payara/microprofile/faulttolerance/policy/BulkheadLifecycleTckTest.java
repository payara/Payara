package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;
import fish.payara.microprofile.faulttolerance.service.MethodFaultToleranceMetrics;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

/**
 * Based on MP FT TCK Test {@code org.eclipse.microprofile.fault.tolerance.tck.bulkhead.lifecycle.BulkheadLifecycleTest}.
 *
 * @author Jan Bernitt
 */
public class BulkheadLifecycleTckTest extends AbstractRecordingTest {

    static final AtomicInteger barrier = new AtomicInteger();
    MetricRegistry registry;

    static void inService(CompletableFuture<Void> waiter) throws InterruptedException, ExecutionException {
        barrier.incrementAndGet();
        waiter.get();
    }

    @Override
    protected FaultToleranceServiceStub createService() {
        // this test needs to use more advanced state per method as multiple methods are involved
        // therefore the below special setup where we have state per method as in the actual implementation
        final Map<Object, AtomicReference<BlockingQueue<Thread>>> concurrentExecutionByMethodId = new ConcurrentHashMap<>();
        final Map<Object, AtomicInteger> waitingQueuePopulationByMethodId = new ConcurrentHashMap<>();

        registry = new MetricRegistryImpl(Type.BASE);
        return new FaultToleranceServiceStub() {
            @Override
            protected FaultToleranceMethodContext stubMethodContext(StubContext ctx) {
                FaultToleranceMetrics metrics = new MethodFaultToleranceMetrics(registry, FaultToleranceUtils.getCanonicalMethodName(ctx.context));
                return new FaultToleranceMethodContextStub(ctx, state,
                        concurrentExecutionByMethodId.computeIfAbsent(ctx.key, key -> new AtomicReference<>()),
                        waitingQueuePopulationByMethodId.computeIfAbsent(ctx.key, key -> new AtomicInteger())) {

                    @Override
                    public FaultToleranceMetrics getMetrics() {
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

    /**
     * Scenario is equivalent to the TCK test of same name but not 100% identical
     */
    @Test(timeout = 3000)
    public void noSharingBetweenClasses() throws Exception {
        Method service1 = BulkheadLifecycleService1.class.getDeclaredMethod("service", CompletableFuture.class);
        Method service2 = BulkheadLifecycleService2.class.getDeclaredMethod("service", CompletableFuture.class);
        BulkheadLifecycleService1 service1a = new BulkheadLifecycleService1();
        BulkheadLifecycleService1 service1b = new BulkheadLifecycleService1();
        BulkheadLifecycleService2 service2a = new BulkheadLifecycleService2();
        BulkheadLifecycleService2 service2b = new BulkheadLifecycleService2();
        try {
            for (int i = 0; i < 4; i++) {
                callMethodWithNewThreadAndWaitFor(commonWaiter, service1, service1a);
                callMethodWithNewThreadAndWaitFor(commonWaiter, service1, service1b);
                callMethodWithNewThreadAndWaitFor(commonWaiter, service2, service2a);
                callMethodWithNewThreadAndWaitFor(commonWaiter, service2, service2b);
            }
            await().atMost(2, TimeUnit.SECONDS).until(() -> barrier.get() == 16);

            assertFurtherThreadThrowsBulkheadException(1, service1, service1a);
            assertFurtherThreadThrowsBulkheadException(1, service1, service1b);
            assertFurtherThreadThrowsBulkheadException(1, service2, service2a);
            assertFurtherThreadThrowsBulkheadException(1, service2, service2b);
        } finally {
            commonWaiter.complete(null);
        }
        waitUnitAllCallersDone();
        assertEquals(barrier.get(), 16);
    }

    static class BulkheadLifecycleService1 {

        @Bulkhead(8)
        public void service(CompletableFuture<Void> waiter) throws Exception {
            inService(waiter);
        }
    }

    static class BulkheadLifecycleService2 {

        @Bulkhead(8)
        public void service(CompletableFuture<Void> waiter) throws Exception {
            inService(waiter);
        }
    }
}
