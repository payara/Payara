package fish.payara.microprofile.faulttolerance.policy;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

abstract class AbstractBulkheadTest {

    final AtomicReference<BulkheadSemaphore> concurrentExecutions = new AtomicReference<>();
    final AtomicReference<BulkheadSemaphore> waitingQueuePopulation = new AtomicReference<>();
    protected final FaultToleranceService service = new FaultToleranceServiceStub(new AtomicReference<>(),
            concurrentExecutions, waitingQueuePopulation) {

        @Override
        public void delay(long delayMillis) throws InterruptedException {
            waitSome(delayMillis);
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

    final List<InOut> threadsInOut = new CopyOnWriteArrayList<>();

    static class InOut {
        static final Object IN = new Object();

        final Object result;
        final Thread t;
        final int inCount;

        InOut(Thread t, int inCount) {
            this(t, inCount, IN);
        }

        InOut(Thread t, int inCount, Object result) {
            this.t = t;
            this.inCount = inCount;
            this.result = result;
        }

        @Override
        public String toString() {
            return "No" + t.getName() + (isIn() ? " in (" : " out (") + inCount + ")"
                    + (isIn() ? "" : " [" + result + "]");
        }

        boolean isIn() {
            return result == IN;
        }
    }

    private final AtomicInteger nextCallerThreadName = new AtomicInteger();

    private final Map<Thread, String> executionResultsByThread = new ConcurrentHashMap<>();
    private final Map<Thread, Exception> executionErrorsByThread = new ConcurrentHashMap<>();

    /*
     * Helpers 
     */

    Thread callBulkheadWithNewThreadAndWaitFor(CompletableFuture<Void> waiter) {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        Runnable task = () ->  {
            try {
                Object res = proceedToResultValue(this, annotatedMethod, waiter);
                recordCallerResult(res);
            } catch (Exception e) {
                executionErrorsByThread.putIfAbsent(Thread.currentThread(), e);
            }
        };
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.setName(nextCallerThreadName.incrementAndGet() + "");
        t.start();
        return t;
    }

    private void recordCallerResult(Object res) throws AssertionError {
        Thread currentThread = Thread.currentThread();
        try {
            Object value = res;
            if (res instanceof CompletionStage<?>) {
                value = ((CompletionStage<?>) res).toCompletableFuture().get();
            } else if (res instanceof Future<?>) {
                value = ((Future<?>) res).get();
            } 
            if (value != null) {
                executionResultsByThread.put(currentThread, value.toString());
                threadsInOut.add(new InOut(currentThread, concurrentExecutionsCount.get(), value));
            }
        } catch (Exception e) {
            executionErrorsByThread.put(currentThread, e);
            threadsInOut.add(new InOut(currentThread, concurrentExecutionsCount.get(), e));
        }
    }

    void assertExecutionResult(String expected, Thread... forThreads) {
        for (Thread t : forThreads) {
            String actual = executionResultsByThread.get(t);
            if (!expected.equals(actual)) {
                assertEquals("Unexpected result for thread " + t.getName() + ", processing was " + threadsInOut,
                        expected, actual);
            }
        }
    }

    void assertExecutionError(Exception expected, Thread... forThreads) {
        for (Thread t : forThreads) {
            assertEqualExceptions(expected, executionErrorsByThread.get(t));
        }
        assertEquals("There were more threads with errors", executionErrorsByThread.size(), forThreads.length);
    }

    private void assertEqualExceptions(Throwable expected, Throwable actual) {
        assertSame(expected.getClass(), actual.getClass());
        assertEquals(expected.getMessage(), actual.getMessage());
        if (expected.getCause() != null) {
            assertNotNull(actual.getCause());
            assertEqualExceptions(expected.getCause(), actual.getCause());
        } else {
            assertNull(actual.getCause());
        }
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

    void assertCompletedExecutionLimitedTo(int expectedMaxConcurrentExecutions, Thread... expectedHaveExecuted) {
        assertEquals(0, concurrentExecutionsCount.get());
        assertEnteredSoFar(expectedHaveExecuted);
        assertExitedSoFar(expectedHaveExecuted);
        assertMaxConcurrentExecution(expectedMaxConcurrentExecutions);
    }

    void assertMaxConcurrentExecution(int expectedMaxConcurrentExecutions) {
        assertRange(1, expectedMaxConcurrentExecutions, maxConcurrentExecutionsCount.get());
        for (InOut inOut : threadsInOut) {
            assertRange(inOut.isIn() ? 1 : 0, expectedMaxConcurrentExecutions, inOut.inCount);
        }
    }

    void assertExitedSoFar(Thread... expectedSet) {
        assertSameSets("exited in unexpected order", asList(expectedSet), threadsExited);
    }

    void assertEnteredSoFar(Thread... expectedSet) {
        assertSameSets("entered in unexpected order", asList(expectedSet), threadsEntered);
    }

    @SafeVarargs
    final void assertExecutionGroups(List<Thread> actual, List<Thread>... expectedConcurrentThreadGroups) {
        int startIndex = 0;
        for (List<Thread> group : expectedConcurrentThreadGroups) {
            int length = group.size();
            assertSameSets((actual == threadsEntered ? "entered" : "exited") + " in unexpected order", group,
                    actual.subList(startIndex, startIndex + length));
            startIndex += length;
        }
    }

    Object proceedToResultValue(Object test, Method annotatedMethod, Future<Void> argument) throws Exception {
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(test.getClass(), annotatedMethod);
        StaticAnalysisContext context = new StaticAnalysisContext(test, annotatedMethod, argument);
        return policy.proceed(context, () -> service.getMethodContext(context));
    }

    CompletionStage<String> bodyWaitThenReturnSuccess(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturn(waiter, () -> CompletableFuture.completedFuture("Success"));
    }

    CompletionStage<String> bodyWaitThenReturn(Future<Void> waiter, Supplier<CompletionStage<String>> result) throws Exception {
        maxConcurrentExecutionsCount.accumulateAndGet(concurrentExecutionsCount.incrementAndGet(), Integer::max);
        bulkheadMethodCallCount.incrementAndGet();
        Thread currentThread = Thread.currentThread();
        threadsEntered.add(currentThread);
        threadsInOut.add(new InOut(currentThread, concurrentExecutionsCount.get()));
        try {
            if (waiter != null) {
                waiter.get();
            }
            return result.get();
        } finally {
            threadsExited.add(currentThread);
            concurrentExecutionsCount.decrementAndGet();
        }
    }

    void assertFurtherThreadThrowsBulkheadException() {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        for (int i = 0; i < 10; i++) { // 10 attempts to be sure
            try {
                assertProceedingThrowsBulkheadException1(annotatedMethod);
            } catch (Exception e) {
                throw new AssertionError("Did not throw BulkheadException but: ", e);
            }
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
        waitSomeUnit(() -> 
            equalAcquiredPermits(concurrentExecutions, this.concurrentExecutions.get())
            && equalAcquiredPermits(waitingQueuePopulation, this.waitingQueuePopulation.get()));
    }

    static void waitSomeUnit(BooleanSupplier test) {
        long delayMs = 4;
        while (!test.getAsBoolean()) {
            waitSome(delayMs);
            delayMs *= 2;
        }
    }

    static void waitSome(long delayMs) {
        if (delayMs <= 0)
            return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            return; // give up (test was cancelled)
        }
    }

    void assertPermitsAquired(int concurrentExecutions, int waitingQueuePopulation) {
        assertEquals(concurrentExecutions, this.concurrentExecutions.get().acquiredPermits());
        assertEquals(waitingQueuePopulation, this.waitingQueuePopulation.get().acquiredPermits());
    }

    static boolean equalAcquiredPermits(int expected, BulkheadSemaphore actual) {
        return actual == null ? expected == 0 : actual.acquiredPermits() == expected;
    }

    private static <E> void assertSameSets(String msg, Collection<E> expected, Collection<E> actual) {
        assertEquals(msg, new HashSet<>(expected), new HashSet<>(actual));
    }

    static void assertRange(int expectedMin, int expectedMax, int actual) {
        assertThat(actual, both(greaterThanOrEqualTo(expectedMin)).and(lessThanOrEqualTo(expectedMax)));
    }
}
