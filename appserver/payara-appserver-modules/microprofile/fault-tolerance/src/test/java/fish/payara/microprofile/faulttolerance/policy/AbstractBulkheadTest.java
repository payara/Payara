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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
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

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

abstract class AbstractBulkheadTest {

    protected final FaultToleranceServiceStub service = new FaultToleranceServiceStub() {

        @Override
        public FaultToleranceMethodContext getMethodContext(InvocationContext context, FaultTolerancePolicy policy) {
            return new FaultToleranceMethodContextStub(context, state, concurrentExecutions, waitingQueuePopulation) {

                @Override
                public void delay(long delayMillis) throws InterruptedException {
                    waitSome(delayMillis);
                }
            };
        }

    };
    final AtomicReference<BlockingQueue<Thread>> concurrentExecutions = service.getConcurrentExecutionsReference();
    final AtomicInteger waitingQueuePopulation = service.getWaitingQueuePopulationReference();
    protected final CompletableFuture<Void> commonWaiter = new CompletableFuture<>();

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

    final List<Thread> caller = new ArrayList<>();

    static class InOut {
        static final Object IN = new Object();

        volatile Object result;
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
                Thread currentThread = Thread.currentThread();
                setThreadResult(currentThread, e);
                executionErrorsByThread.putIfAbsent(currentThread, e);
            }
        };
        return startDaemonThreadWith(task);
    }

    private Thread startDaemonThreadWith(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.setName(nextCallerThreadName.incrementAndGet() + "");
        caller.add(t);
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
            executionResultsByThread.put(currentThread, value == null ? null : value.toString());
            setThreadResult(currentThread, value);
        } catch (Exception e) {
            executionErrorsByThread.put(currentThread, e);
            setThreadResult(currentThread, e);
        }
    }

    private void setThreadResult(Thread currentThread, Object result) {
        int end = threadsInOut.size() - 1;
        for (int i = end; i >= 0; i--) {
            InOut inOut = threadsInOut.get(i);
            if (inOut.t == currentThread && !inOut.isIn()) {
                inOut.result = result;
                return;
            }
        }
    }

    int countExecutionResults(String expected) {
        int c = 0;
        for (String result : executionResultsByThread.values()) {
            if (Objects.equals(expected, result)) {
                c++;
            }
        }
        return c;
    }

    int countExecutionErrors(Class<? extends Exception> type) {
        int c = 0;
        for (Exception ex : executionErrorsByThread.values()) {
            if (type.isAssignableFrom(ex.getClass())) {
                c++;
            } else if (ex instanceof ExecutionException) {
                if (type.isAssignableFrom(ex.getCause().getClass())) {
                    c++;
                }
            }
        }
        return c;
    }

    void assertExecutionResult(String expected, Thread... forThreads) {
        waitUntilThreadResultIsPresent(forThreads);
        for (Thread t : forThreads) {
            String actual = executionResultsByThread.get(t);
            if (!expected.equals(actual)) {
                assertEquals("Unexpected result for thread " + t.getName() + ", processing was " + threadsInOut
                                + ", thead exception " + executionErrorsByThread.get(t),
                        expected, actual);
            }
        }
    }

    private void waitUntilThreadResultIsPresent(Thread... forThreads) {
        waitSomeUntil(() -> {
            for (Thread t : forThreads) {
                if (!executionResultsByThread.containsKey(t) && !executionErrorsByThread.containsKey(t)) {
                    return false;
                }
            }
            return true;
        });
    }

    void assertExecutionError(Exception expected, Thread... forThreads) {
        waitUntilThreadResultIsPresent(forThreads);
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
        if (Thread.currentThread().getName().endsWith(" test")) {
            fail("wrong thread");
        }
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(test.getClass(), annotatedMethod);
        StaticAnalysisContext context = new StaticAnalysisContext(test, annotatedMethod, argument);
        return policy.proceed(context, () -> service.getMethodContext(context, policy));
    }

    CompletableFuture<String> bodyWaitThenReturnSuccess(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturn(waiter, () -> CompletableFuture.completedFuture("Success"));
    }

    String bodyWaitThenReturnSuccessDirectly(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturn(waiter, () -> "Success");
    }

    <T> T bodyWaitThenReturn(Future<Void> waiter, Supplier<T> result) throws Exception {
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
            threadsInOut.add(new InOut(currentThread, concurrentExecutionsCount.getAndDecrement(), null));
        }
    }

    CompletionStage<String> bodyReturnThenWaitOnCompletionWithSuccess(Future<Void> waiter) {
        return bodyReturnThenWaitOnCompletion(waiter, () -> "Success");
    }

    <T> CompletionStage<T> bodyReturnThenWaitOnCompletion(Future<Void> waiter, Supplier<T> result) {
        Thread currentThread = Thread.currentThread();
        try {
            threadsEntered.add(currentThread);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    waiter.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw null;
                }
                return result.get();
            });
        } finally {
            threadsExited.add(currentThread);
        }
    }

    void assertFurtherThreadThrowsBulkheadException() {
        int attemptCount = 10;
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        List<Thread> attemptingCallers = new ArrayList<>();
        Map<Thread, BulkheadException> bulkheadExceptions = new ConcurrentHashMap<>();
        Map<Thread, Object> otherOutcomes = new ConcurrentHashMap<>();
        for (int i = 0; i < attemptCount; i++) { // 10 attempts to be sure
            attemptingCallers.add(startDaemonThreadWith(() -> {
                Thread currentThread = Thread.currentThread();
                try {
                    Object resultValue = proceedToResultValue(this, annotatedMethod, null);
                    if (resultValue instanceof Future) {
                        otherOutcomes.put(currentThread, ((Future<?>) resultValue).get()); // should throw the exception
                    } else {
                        otherOutcomes.put(currentThread, resultValue);
                    }
                } catch (BulkheadException ex) {
                    bulkheadExceptions.put(currentThread, ex);
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof BulkheadException) {
                        bulkheadExceptions.put(currentThread, (BulkheadException) ex.getCause());
                    }
                } catch (Exception e) {
                    otherOutcomes.put(currentThread, e);
                }
            }));
        }
        waitUntilAllThreadsDone(attemptingCallers);
        assertEquals(attemptCount, bulkheadExceptions.size());
    }

    void waitUnitAllCallersDone() {
        waitUntilAllThreadsDone(caller);
    }

    private static void waitUntilAllThreadsDone(List<Thread> threads) {
        waitSomeUntil(() -> {
            for (Thread t : threads) {
                if (t.isAlive())
                    return false;
            }
            return true;
        });
    }

    void waitUntilPermitsAquired(int concurrentExecutions, int waitingQueuePopulation) {
        waitSomeUntil(() -> {
            BlockingQueue<Thread> queue = this.concurrentExecutions.get();
            int actualConcurrentExecutions = queue == null ? 0 : queue.size();
            return concurrentExecutions == actualConcurrentExecutions
                    && waitingQueuePopulation == this.waitingQueuePopulation.get() - actualConcurrentExecutions;
        });
    }

    static void waitSomeUntil(BooleanSupplier test) {
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
        int actualConcurrentExecutions = this.concurrentExecutions.get().size();
        assertEquals(concurrentExecutions, actualConcurrentExecutions);
        int actualQueueLength = this.waitingQueuePopulation.get();
        assertEquals(waitingQueuePopulation, actualQueueLength - actualConcurrentExecutions);
    }

    private static <E> void assertSameSets(String msg, Collection<E> expected, Collection<E> actual) {
        assertEquals(msg, new HashSet<>(expected), new HashSet<>(actual));
    }

    static void assertRange(int expectedMin, int expectedMax, int actual) {
        assertThat(actual, both(greaterThanOrEqualTo(expectedMin)).and(lessThanOrEqualTo(expectedMax)));
    }
}
