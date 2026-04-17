/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState.CircuitState;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests that uses multiple concurrent callers for the method under test to see that "statistically" it behaves correctly.
 *
 * @author Jan Bernitt
 */
public class FaultToleranceStressTest implements FallbackHandler<Future<String>> {

    private static final int NUMBER_OF_CALLERS = 8;
    private static final int NUMBER_OF_CALLS = 10;

    private final AtomicInteger methodInvocationCount = new AtomicInteger();
    private final AtomicInteger methodFailedInvocationCount = new AtomicInteger();
    private final AtomicInteger methodSuccessfulInvocationCount = new AtomicInteger();

    private final AtomicInteger callerFailedInvocationCount = new AtomicInteger();
    private final AtomicInteger callerExpectedlyFailedInvocationCount = new AtomicInteger();
    private final AtomicInteger callerSuccessfulInvocationCount = new AtomicInteger();
    private final AtomicInteger callerUnexpectedOutcomeInvocationCount = new AtomicInteger();

    final AtomicInteger delayedExecutionCount = new AtomicInteger();
    final AtomicLong delayedMillis = new AtomicLong();
    final AtomicLong maxDelayMillis = new AtomicLong(Long.MIN_VALUE);
    final AtomicLong minDelayMillis = new AtomicLong(Long.MAX_VALUE);

    final AtomicInteger asyncCompletedCount = new AtomicInteger();
    final AtomicInteger asyncCompletedExceptionallyCount = new AtomicInteger();
    final AtomicInteger asyncCancelCount = new AtomicInteger();

    final AtomicInteger circuitStateAccessCount = new AtomicInteger();
    final AtomicInteger concurrentExecutionsAccessCount = new AtomicInteger();
    final AtomicInteger waitingQueuePopulationAccessCount = new AtomicInteger();

    final ExecutorService executorService = Executors.newWorkStealingPool(NUMBER_OF_CALLERS / 2);
    final FaultToleranceServiceStub service = new FaultToleranceServiceStub() {
        @Override
        protected FaultToleranceMethodContext stubMethodContext(StubContext ctx) {
            return new FaultToleranceMethodContextStub(ctx, state, concurrentExecutions, waitingQueuePopulation) {
                @Override
                public CircuitBreakerState getState() {
                    circuitStateAccessCount.incrementAndGet();
                    return super.getState();
                }

                @Override
                public BlockingQueue<Thread> getConcurrentExecutions() {
                    concurrentExecutionsAccessCount.incrementAndGet();
                    return super.getConcurrentExecutions();
                }

                @Override
                public AtomicInteger getQueuingOrRunningPopulation() {
                    waitingQueuePopulationAccessCount.incrementAndGet();
                    return super.getQueuingOrRunningPopulation();
                }

                @Override
                public void delay(long delayMillis) throws InterruptedException {
                    // we don't really wait in this test but we count waiting time
                    delayedExecutionCount.incrementAndGet();
                    maxDelayMillis.updateAndGet(value -> Math.max(value, delayMillis));
                    minDelayMillis.updateAndGet(value -> Math.min(value, delayMillis));
                    delayedMillis.addAndGet(delayMillis);
                }

                @Override
                public void runAsynchronous(AsyncFuture asyncResult,
                        Callable<Object> task) throws RejectedExecutionException {
                    Runnable completionTask = () -> {
                        if (!asyncResult.isCancelled() && !Thread.currentThread().isInterrupted()) {
                            boolean returned = false;
                            try {
                                Object res = task.call();
                                returned = true;
                                Future<?> futureResult = AsynchronousPolicy.toFuture(res);
                                if (!asyncResult.isCancelled()) {
                                    if (!asyncResult.isDone()) {
                                        asyncCompletedCount.incrementAndGet();
                                        asyncResult.complete(futureResult.get());
                                    }
                                } else {
                                    asyncCancelCount.incrementAndGet();
                                    futureResult.cancel(true);
                                }
                            } catch (Exception ex) {
                                asyncCompletedExceptionallyCount.incrementAndGet();
                                asyncResult.setExceptionThrown(!returned);
                                asyncResult.completeExceptionally(returned && ex instanceof ExecutionException ? ex.getCause() : ex);
                            }
                        }
                    };
                    executorService.execute(completionTask);
                }
            };
        }
    };
    final AtomicReference<CircuitBreakerState> state = service.getStateReference();
    final AtomicReference<BlockingQueue<Thread>> concurrentExecutions = service.getConcurrentExecutionsReference();
    final AtomicInteger waitingQueuePopulation = service.getWaitingQueuePopulationReference();

    @Test
    public void occasionallyFailingService() throws InterruptedException {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        Runnable callerTask = () -> callServiceMethod(annotatedMethod);
        List<Thread> callers = startCallers(NUMBER_OF_CALLERS, callerTask);
        waitForCallersToFinish(callers);
        assertTrue("All callers should be done and removed by now", callers.isEmpty());

        // check the counts make sense
        assertEquals("No unexpected outcome should occur", 0, callerUnexpectedOutcomeInvocationCount.get());
        int totalSuccesses = callerSuccessfulInvocationCount.get();
        int totalFailures = callerFailedInvocationCount.get();
        int totalExpectedCalls = NUMBER_OF_CALLERS * NUMBER_OF_CALLS;
        int minimumExpectedRetries = totalExpectedCalls / 3;
        assertEquals(totalExpectedCalls, totalSuccesses + totalFailures);
        assertThat("Retry should lead one successful attempt and at least one failure attempt per failure outcome",
                methodInvocationCount.get(), greaterThanOrEqualTo(totalSuccesses + minimumExpectedRetries));
        assertThat("Every 5th attempt should return with a Future complected exceptionally",
                callerExpectedlyFailedInvocationCount.get(), greaterThan(0)); // open circuit makes this mostly unpredictable
        assertThat("Some atempt should end up waiting",
                waitingQueuePopulationAccessCount.get(), greaterThan(0));
        assertThat("Most attempts should go through bulkhead execution", // due to open circuit some might never reach bulkhead
                concurrentExecutionsAccessCount.get(), greaterThanOrEqualTo(totalExpectedCalls / 2)); // conservative assumption: half of normal case number
        assertThat("Each attempt should use cuircuit breaker state",
                circuitStateAccessCount.get(), greaterThanOrEqualTo(totalExpectedCalls));
        assertThat("Each successful attempt should have been asyncronous",
                asyncCompletedCount.get(), greaterThan(totalExpectedCalls));
        assertThat("Each failing attempt should have been asyncronous",
                asyncCompletedExceptionallyCount.get(), greaterThanOrEqualTo(totalFailures));
        assertEquals("Cancel should not have occured", 0, asyncCancelCount.get());
        assertThat("Most attempt throwing an exception should cause a delay", // not all since retry can be cancelled due to concurrent completion
                delayedExecutionCount.get(), greaterThanOrEqualTo(minimumExpectedRetries / 2)); // conservative assumption: half of normal case number
        long totalDelayMillis = delayedMillis.get();
        assertThat("Some attempts should have tried to retry with a delay",
                totalDelayMillis, greaterThan(0L));
        assertThat("Total delay should be less than the sum of maximal jitter per retry",
                totalDelayMillis, lessThan(methodInvocationCount.get() * 200L)); // 200ms being the jitter
        assertThat("All attempts should use a non negative delay",
                maxDelayMillis.get(), greaterThanOrEqualTo(0L));
        assertThat("All attempts should use a delay not larger than the jitter",
                maxDelayMillis.get(), lessThanOrEqualTo(200L));

        // now check that the state makes sense
        assertEquals("No execution should ongo", 0, concurrentExecutions.get().size());
        assertEquals("No queueing should ongo", 0, waitingQueuePopulation.get());
        assertThat("Circuit should not be open (any more)",
                state.get().getCircuitState(), oneOf(CircuitState.HALF_OPEN, CircuitState.CLOSED));
    }

    /**
     * The method under tests fails every 3rd call whereby in a window of 4 there can be 2 failed calls opening the
     * circuit. As delay is just recorded but not enforced there is only a minimal chance another caller does an attempt
     * while the circuit is open but occasionally this happens.
     *
     * Every 5th call returns a failed Future that should be handed to the caller as is.
     *
     * Every 3rd call fails causing a retry. Together with open circuits this might even cause fallback handler to be
     * used which will also fail the result as it only rethrows the error.
     */
    @Asynchronous
    @Fallback(FaultToleranceStressTest.class)
    @Retry(maxRetries = 1)
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    @CircuitBreaker(successThreshold = 2, delay = 0, requestVolumeThreshold = 4)
    public Future<String> occasionallyFailingService_Method() throws IOException {
        int called = methodInvocationCount.incrementAndGet();
        if (called % 3 == 0) {
            // this causes a retry
            methodFailedInvocationCount.incrementAndGet();
            throw new IOException("Failed");
        }
        if (called % 5 == 0) {
            // this does not cause a retry, its simply a Future that completes with a failure
            CompletableFuture<String> failedValue = new CompletableFuture<>();
            failedValue.completeExceptionally(new IOException("Failed"));
            return failedValue;
        }
        methodSuccessfulInvocationCount.incrementAndGet();
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    public Future<String> handle(ExecutionContext context) {
        if (context.getFailure() instanceof FaultToleranceException) {
            throw (FaultToleranceException) context.getFailure();
        }
        throw new UncheckedIOException((IOException) context.getFailure());
    }

    private void callServiceMethod(Method annotatedMethod) {
        Object test = this;
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(test.getClass(), annotatedMethod);
        for (int i = 0; i < NUMBER_OF_CALLS; i++) {
            try {
                StaticAnalysisContext context = new StaticAnalysisContext(test, annotatedMethod);
                @SuppressWarnings("unchecked")
                Future<String> resultValue = (Future<String>)
                        policy.proceed(context, () -> service.getMethodContext(context, policy));
                assertEquals("Success", resultValue.get());
                callerSuccessfulInvocationCount.incrementAndGet();
            } catch (ExecutionException ex) {
                callerFailedInvocationCount.incrementAndGet();
                if (ex.getCause() instanceof UncheckedIOException) {
                    // last retry after exception(s) threw exception which comes back from fallback handler as UncheckedIOException
                    assertEquals("Failed", ex.getCause().getCause().getMessage());
                } else if (ex.getCause() instanceof IOException) {
                    // failed since Future completed with an exception
                    assertEquals("Failed", ex.getCause().getMessage());
                    callerExpectedlyFailedInvocationCount.incrementAndGet();
                } else if (ex.getCause() instanceof FaultToleranceException) {
                    // circuit open or bulkhead queue full
                } else {
                    callerUnexpectedOutcomeInvocationCount.incrementAndGet();
                }
            } catch (Exception ex) {
                callerUnexpectedOutcomeInvocationCount.incrementAndGet();
            } catch (AssertionError err) {
                callerUnexpectedOutcomeInvocationCount.incrementAndGet();
            }
        }
    }

    private static List<Thread> startCallers(int count, Runnable callerTask) {
        List<Thread> callers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread caller = new Thread(callerTask);
            callers.add(caller);
            caller.start();
        }
        return callers;
    }

    private static void waitForCallersToFinish(List<Thread> callers) throws InterruptedException {
        Iterator<Thread> iter = callers.iterator();
        while (iter.hasNext()) {
            Thread caller = iter.next();
            iter.remove();
            try {
                caller.join();
            } catch (InterruptedException ex) {
                for (Thread c : callers) {
                    c.interrupt();
                }
                throw ex;
            }
        }
    }
}
