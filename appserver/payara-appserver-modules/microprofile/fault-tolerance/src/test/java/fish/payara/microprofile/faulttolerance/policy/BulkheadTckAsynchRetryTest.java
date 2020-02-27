package fish.payara.microprofile.faulttolerance.policy;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

public class BulkheadTckAsynchRetryTest extends AbstractBulkheadTest {

    @Test
    public void testBulkheadClassAsynchronousPassiveRetry55() {
        assertExecutionResult("Success", loop(10, 5, 5));
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = { BulkheadException.class }, delay = 1, delayUnit = ChronoUnit.SECONDS, 
        maxRetries = 10, maxDuration = 999999)
    public Future<?> testBulkheadClassAsynchronousPassiveRetry55_Method(Future<Void> waiter) throws InterruptedException {
        return waitThenReturnSuccess(waiter).toCompletableFuture();
    }

    @Test
    public void testBulkheadMethodAsynchronousRetry55() {
        assertExecutionResult("Success", loop(20, 5, 5));
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn =
    { BulkheadException.class }, delay = 1, delayUnit = ChronoUnit.SECONDS, maxRetries = 10, maxDuration=999999)
    public Future<?> testBulkheadMethodAsynchronousRetry55_Method(Future<Void> waiter) throws InterruptedException {
        return waitThenReturnSuccess(waiter).toCompletableFuture();
    }

    private Thread[] loop(int iterations, int maxSimultaneousWorkers, int maxSimultaneursQueuing) {
        Thread[] callers = new Thread[iterations];
        int bulkheadCapacity = maxSimultaneousWorkers + maxSimultaneursQueuing;
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] waiters = new CompletableFuture[bulkheadCapacity];
        for (int i = 0; i < bulkheadCapacity; i++) {
            waiters[i] = new CompletableFuture<>();
            callers[i] = callBulkheadWithNewThreadAndWaitFor(waiters[i]);
        }
        waitUntilPermitsAquired(maxSimultaneousWorkers, maxSimultaneursQueuing);
        waitSome(100);
        assertPermitsAquired(maxSimultaneousWorkers, maxSimultaneursQueuing);
        for (int i = bulkheadCapacity; i < iterations; i++) {
            callers[i] = callBulkheadWithNewThreadAndWaitFor(waiter);
        }
        waitSome(100);
        for (int i = 0; i < waiters.length; i++) {
            waiters[i].complete(null);
            waitSome(50);
        }
        waitUntilPermitsAquired(0, 0);
        Thread[] expectedExecutingCallers = Arrays.copyOf(callers, bulkheadCapacity);
        assertCompletedExecutionLimitedTo(maxSimultaneousWorkers, expectedExecutingCallers);
        return expectedExecutingCallers;
    }
}
