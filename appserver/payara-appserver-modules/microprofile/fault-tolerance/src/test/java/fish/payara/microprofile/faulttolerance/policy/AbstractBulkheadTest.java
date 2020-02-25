package fish.payara.microprofile.faulttolerance.policy;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

abstract class AbstractBulkheadTest {

    final AtomicReference<BulkheadSemaphore> concurrentExecutions = new AtomicReference<>();
    final AtomicReference<BulkheadSemaphore> waitingQueuePopulation = new AtomicReference<>();
    protected final FaultToleranceService service = new FaultToleranceServiceStub() {
        @Override
        public BulkheadSemaphore getConcurrentExecutions(int maxConcurrentThreads, InvocationContext context) {
            return concurrentExecutions.updateAndGet(value -> 
                value != null ? value : new BulkheadSemaphore(maxConcurrentThreads));
        }

        @Override
        public BulkheadSemaphore getWaitingQueuePopulation(int queueCapacity, InvocationContext context) {
            return waitingQueuePopulation.updateAndGet(value -> 
                value != null ? value : new BulkheadSemaphore(queueCapacity));
        }
    };

    protected final CompletableFuture<Void> waiter = new CompletableFuture<>();

    /*
     * For Verification:
     */

    /**
     * Number of times the annotated bulkhead method has been called in total
     */
    final AtomicInteger bulkheadMethodCallCount = new AtomicInteger();

    /**
     * Current number of concurrent threads executing the annotated method
     */
    final AtomicInteger concurrentExecutionsCount = new AtomicInteger();

    /**
     * The maximum number of concurrent threads executing the annotated method
     */
    final AtomicInteger maxConcurrentExecutionsCount = new AtomicInteger();

    /**
     * The order in which threads entered the annotated bulkhead method
     */
    final List<Thread> threadsEntered = new CopyOnWriteArrayList<>();

    /**
     * The order in which threads exited the annotated bulkhead method
     */
    final List<Thread> threadsExited = new CopyOnWriteArrayList<>();

    private final AtomicInteger nextCallerThreadName = new AtomicInteger();

    /*
     * Helpers 
     */

    Thread callBulkheadWithNewThreadAndWaitFor(CompletableFuture<Void> waiter) {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        Runnable task = () ->  proceedToResultValueOrFail(this, annotatedMethod, waiter);
        Thread t = new Thread(task);
        t.setName(nextCallerThreadName.incrementAndGet() + "");
        t.start();
        return t;
    }

    void assertEnteredAndExited(int entered, int exited) {
        for (int i = 0; i < 5; i++) {
            try {
                assertEquals(entered, threadsEntered.size());
                assertEquals(exited, threadsExited.size());
                return;
            } catch (AssertionError e) {

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    throw e;
                }
            }
        }
    }

    void assertCompletedExecution(int expectedMaxConcurrentExecutions, Thread... expectedHaveExecuted) {
        assertEquals(expectedHaveExecuted.length, bulkheadMethodCallCount.get());
        assertEquals(0, concurrentExecutionsCount.get());
        assertRange(1, expectedMaxConcurrentExecutions, maxConcurrentExecutionsCount.get());
        assertSameSets(asList(expectedHaveExecuted), threadsEntered);
        assertSameSets(asList(expectedHaveExecuted), threadsExited);
    }

    @SafeVarargs
    final void assertExecutionGroups(List<Thread>... expectedConcurrentThreadGroups) {
        int startIndex = 0;
        for (List<Thread> group : expectedConcurrentThreadGroups) {
            int length = group.size();
            assertSameSets(group, threadsEntered.subList(startIndex, startIndex + length));
            assertSameSets(group, threadsExited.subList(startIndex, startIndex + length));
            startIndex += length;
        }
    }

    Object proceedToResultValueOrFail(Object test, Method annotatedMethod, Future<Void> argument) {
        try {
            return proceedToResultValue(test, annotatedMethod, argument);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    Object proceedToResultValue(Object test, Method annotatedMethod, Future<Void> argument) throws Exception {
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(test.getClass(), annotatedMethod);
        return policy.proceed(new StaticAnalysisContext(test, annotatedMethod, argument), service);
    }

    CompletionStage<String> waitThenReturnSuccess(Future<Void> waiter) throws AssertionError {
        maxConcurrentExecutionsCount.accumulateAndGet(concurrentExecutionsCount.incrementAndGet(), Integer::max);
        bulkheadMethodCallCount.incrementAndGet();
        threadsEntered.add(Thread.currentThread());
        try {
            waiter.get();
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            threadsExited.add(Thread.currentThread());
            concurrentExecutionsCount.decrementAndGet();
        }
        return CompletableFuture.completedFuture("Success");
    }

    void assertFurtherThreadThrowsBulkheadException() throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        for (int i = 0; i < 10; i++) { // 10 attempts to be sure
            assertProceedingThrowsBulkheadException1(annotatedMethod);
        }
    }

    void assertProceedingThrowsBulkheadException1(Method annotatedMethod)
            throws Exception, InterruptedException {
        try {
            Object resultValue = proceedToResultValue(this, annotatedMethod, null);
            if (resultValue instanceof Future) {
                ((Future<?>) resultValue).get(); // should throw the exception
            }
            fail("Expected to fail with a BulkheadException");
        } catch (BulkheadException ex) {
            // as expected for non asyncronous
        } catch (ExecutionException ex) {
            assertEquals(BulkheadException.class, ex.getCause().getClass());
        }
    }

    void waitUntilPermitsAquired(int concurrentExecutions, int waitingQueuePopulation) {
        long delayMs = 4;
        while (    !equalAcquiredPermits(concurrentExecutions, this.concurrentExecutions.get())
                || !equalAcquiredPermits(waitingQueuePopulation, this.waitingQueuePopulation.get())) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                return; // give up (test was cancelled)
            }
            delayMs *= 2;
        }
    }

    static boolean equalAcquiredPermits(int expected, BulkheadSemaphore actual) {
        return actual == null ? expected == 0 : actual.acquiredPermits() == expected;
    }

    static <E> void assertSameSets(Collection<E> expected, Collection<E> actual) {
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }

    static void assertRange(int expectedMin, int expectedMax, int actual) {
        assertThat(actual, both(greaterThanOrEqualTo(expectedMin)).and(lessThanOrEqualTo(expectedMax)));
    }    
}
