package fish.payara.microprofile.faulttolerance.policy;

import static org.awaitility.Awaitility.await;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Based on MP FT TCK Test {@code org.eclipse.microprofile.fault.tolerance.tck.bulkhead.lifecycle.BulkheadLifecycleTest}.
 *
 * @author Jan Bernitt
 */
public class BulkheadLifecycleTckTest extends AbstractMetricTest {

    static final AtomicInteger barrier = new AtomicInteger();

    /**
     * Scenario is equivalent to the TCK test of same name but not 100% identical
     */
    @Ignore
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
            //TODO issue is that the same queue is used because of the stub context => use real impl?
            await().atMost(2, TimeUnit.SECONDS).until(() -> barrier.get() == 16);

            assertFurtherThreadThrowsBulkheadException(1, service1, service1a);
            assertFurtherThreadThrowsBulkheadException(1, service1, service1b);
            assertFurtherThreadThrowsBulkheadException(1, service2, service2a);
            assertFurtherThreadThrowsBulkheadException(1, service2, service2b);
        } finally {
            commonWaiter.complete(null);
        }
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

    static void inService(CompletableFuture<Void> waiter) throws InterruptedException, ExecutionException {
        barrier.incrementAndGet();
        waiter.get();
    }
}
