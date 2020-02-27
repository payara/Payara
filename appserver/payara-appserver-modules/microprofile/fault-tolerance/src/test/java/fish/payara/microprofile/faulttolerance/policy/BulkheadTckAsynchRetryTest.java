package fish.payara.microprofile.faulttolerance.policy;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

public class BulkheadTckAsynchRetryTest extends AbstractBulkheadTest {

    @Test
    public void testBulkheadClassAsynchronousPassiveRetry55() {
        int iterations = 10;
        Thread[] callers = new Thread[iterations];
        for (int i = 0; i < iterations; i++) {
            callers[i] = callBulkheadWithNewThreadAndWaitFor(waiter);
        }
        waitUntilPermitsAquired(5, 5);
        waiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertCompletedExecutionLimitedTo(5, callers);
        assertExecutionResult("Success", callers);
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = { BulkheadException.class }, delay = 1, delayUnit = ChronoUnit.SECONDS, 
        maxRetries = 10, maxDuration = 999999)
    public Future<?> testBulkheadClassAsynchronousPassiveRetry55_Method(Future<Void> waiter) throws InterruptedException {
        return waitThenReturnSuccess(waiter).toCompletableFuture();
    }
}
