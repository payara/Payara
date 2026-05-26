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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

/**
 * Tests the basic correctness of {@link Bulkhead} handling.
 *
 * @author Jan Bernitt
 */
public class BulkheadBasicTest extends AbstractBulkheadTest {

    private static final RuntimeException SIMULATED_METHOD_ERROR = new RuntimeException("Simulated Bulkhead method error");

    /**
     * Makes 2 concurrent request that should succeed acquiring a bulkhead permit. Further attempts fail. After
     * completing the first request the 3 attempt succeeds. Any further attempt again fails after that.
     *
     * Needs a timeout because incorrect implementation could otherwise lead to endless waiting.
     */
    @Test(timeout = 3000)
    public void bulkheadWithoutQueue() {
        callAndWait(2);
    }

    @Bulkhead(value = 2)
    public String bulkheadWithoutQueue_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    @Test(timeout = 3000)
    public void bulkheadWithoutQueueWithRetry() {
        callAndWait(4);
    }

    @Bulkhead(value = 4)
    @Retry(retryOn = { BulkheadException.class }, delay = 20, delayUnit = ChronoUnit.MILLIS,
    maxRetries = 3, maxDuration = 100, jitter = 0)
    public String bulkheadWithoutQueueWithRetry_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    @Test(timeout = 3000)
    public void bulkheadWithoutQueueNoWaiting() {
        callWithConcurrentCallers(100, 4);
    }

    @Bulkhead(value = 4)
    public String bulkheadWithoutQueueNoWaiting_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    @Test(timeout = 3000)
    public void bulkheadWithoutQueueNoWaitingWithRetry() {
        callWithConcurrentCallers(100, 4);
    }

    @Bulkhead(value = 4)
    @Retry(retryOn = { BulkheadException.class }, delay = 20, delayUnit = ChronoUnit.MILLIS,
            maxRetries = 3, maxDuration = 100, jitter = 0)
    public String bulkheadWithoutQueueNoWaitingWithRetry_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    /**
     * First two request can acquire a bulkhead permit.
     * Following two request can acquire a queue permit.
     * Fifth request fails.
     *
     * Needs a timeout because incorrect implementation could otherwise lead to endless waiting.
     */
    @Test(timeout = 3000)
    public void bulkheadWithQueue() {
        Thread exec1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread exec2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 0);
        assertEnteredAndExited(2, 0);
        Thread queueAndExec1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread queueAndExec2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);
        assertFurtherThreadThrowsBulkheadException10();
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEnteredAndExited(4, 4);
        assertCompletedExecutionLimitedTo(2, exec1, exec2, queueAndExec1, queueAndExec2);
        assertExecutionGroups(threadsEntered, asList(exec1, exec2), asList(queueAndExec1, queueAndExec2));
        assertExecutionResult("Success", exec1, exec2, queueAndExec1, queueAndExec2);
        assertExecutionError(null);
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public CompletionStage<String> bulkheadWithQueue_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }

    /**
     * Similar to {@link #bulkheadWithQueue()} just that we interrupt the queueing threads and expect their permits to
     * be released.
     */
    @Test(timeout = 3000)
    public void bulkheadWithQueueInterruptQueueing() {
        Thread exec1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread exec2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        // must wait here to ensure these two threads actually are the ones getting permits
        waitUntilPermitsAquired(2, 0);
        assertEnteredAndExited(2, 0);
        Thread queueing1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread queueing2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);
        assertEnteredAndExited(2, 0);
        assertEnteredSoFar(exec1, exec2);
        queueing1.interrupt();
        waitUntilPermitsAquired(2, 1);
        queueing2.interrupt();
        waitUntilPermitsAquired(2, 0);
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEnteredAndExited(2, 2);
        assertCompletedExecutionLimitedTo(2, exec1, exec2);
        assertExecutionResult("Success", exec1, exec2);
        assertExecutionError(new ExecutionException(new InterruptedException()), queueing1, queueing2);
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<String> bulkheadWithQueueInterruptQueueing_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }

    /**
     * Similar to {@link #bulkheadWithQueue()} just that we interrupt the executing threads and expect their permits to
     * be released and waiting threads to become executing.
     */
    @Test(timeout = 3000)
    public void bulkheadWithQueueInterruptExecuting() {
        CompletableFuture<Void> exec2Waiter = new CompletableFuture<>();
        Thread exec1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread exec2 = callMethodWithNewThreadAndWaitFor(exec2Waiter);
        // must wait here to ensure these two threads actually are the ones getting permits
        waitUntilPermitsAquired(2, 0);
        Thread queueing1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread queueing2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);
        assertEnteredAndExited(2, 0);
        assertEnteredSoFar(exec1, exec2);
        exec1.interrupt(); // should cause exit of bulkhead
        waitUntilPermitsAquired(2, 1);
        assertEnteredAndExited(3, 1);
        assertSame(exec1, threadsExited.get(0));
        exec2Waiter.complete(null); // exec2 is done, exit of bulkhead
        waitUntilPermitsAquired(2, 0);
        assertEnteredAndExited(4, 2);
        assertEquals(asList(exec1, exec2), threadsExited);
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEnteredAndExited(4, 4);
        assertCompletedExecutionLimitedTo(2, exec1, exec2, queueing1, queueing2);
        assertExecutionResult("Success", exec2, queueing1, queueing2);
        assertExecutionError(new ExecutionException(new InterruptedException()), exec1);
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<String> bulkheadWithQueueInterruptExecuting_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter);
    }

    /**
     * Similar to {@link #bulkheadWithQueue()} but one thread executing fails by completing the {@link CompletionStage}
     * with an exception. This should exit the bulkhead and allow another thread to run.
     */
    @Test(timeout = 3000)
    public void bulkheadWithQueueCompleteWithException() {
        CompletableFuture<Void> exec1Waiter = new CompletableFuture<>();
        Thread exec1 = callMethodWithNewThreadAndWaitFor(exec1Waiter);
        Thread exec2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        // must wait here to ensure these two threads actually are the ones getting permits
        waitUntilPermitsAquired(2, 0);
        assertEnteredAndExited(2, 0);
        Thread queueing1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread queueing2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);
        exec1Waiter.complete(null);
        waitUntilPermitsAquired(2, 1);
        assertEnteredAndExited(3, 1);
        assertExitedSoFar(exec1);
        commonWaiter.complete(null); //
        waitUntilPermitsAquired(0, 0);
        assertCompletedExecutionLimitedTo(2, exec1, exec2, queueing1, queueing2);
        assertExecutionResult("Success", exec2, queueing1, queueing2);
        assertExecutionError(new ExecutionException(SIMULATED_METHOD_ERROR), exec1);
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<String> bulkheadWithQueueCompleteWithException_Method(Future<Void> waiter) throws Exception {
        if (waiter == this.commonWaiter)
            return bodyWaitThenReturnSuccess(waiter);
        return bodyWaitThenReturn(waiter, () -> {
            CompletableFuture<String> res = new CompletableFuture<>();
            res.completeExceptionally(SIMULATED_METHOD_ERROR);
            return res;
        });
    }

    /**
     * Similar to {@link #bulkheadWithQueue()} but one thread executing throws an exception which should exist the
     * bulkhead and allow another queueing thread to run.
     */
    @Test(timeout = 3000)
    public void bulkheadWithQueueThrowsException() {
        CompletableFuture<Void> exec1Waiter = new CompletableFuture<>();
        Thread exec1 = callMethodWithNewThreadAndWaitFor(exec1Waiter);
        Thread exec2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        // must wait here to ensure these two threads actually are the ones getting permits
        waitUntilPermitsAquired(2, 0);
        assertEnteredAndExited(2, 0);
        Thread queueing1 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        Thread queueing2 = callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);
        exec1Waiter.complete(null); // now throws an exception
        waitUntilPermitsAquired(2, 1);
        assertEnteredAndExited(3, 1);
        assertExitedSoFar(exec1);
        commonWaiter.complete(null); //
        waitUntilPermitsAquired(0, 0);
        assertCompletedExecutionLimitedTo(2, exec1, exec2, queueing1, queueing2);
        assertExecutionResult("Success", exec2, queueing1, queueing2);
        assertExecutionError(new ExecutionException(SIMULATED_METHOD_ERROR), exec1);
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<String> bulkheadWithQueueThrowsException_Method(Future<Void> waiter) throws Exception {
        if (waiter == this.commonWaiter) {
            return bodyWaitThenReturnSuccess(waiter);
        }
        return bodyWaitThenReturn(waiter, () -> {
            throw SIMULATED_METHOD_ERROR;
        });
    }

    @Test(timeout = 3000)
    public void bulkheadWithoutQueueWithAsyncCompletionStageExitsOnCompletion() {
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 0);
        assertFurtherThreadThrowsBulkheadException10();
        assertEquals(2, threadsEntered.size());
        assertEquals(0, threadsExited.size());
        waitSome(50);
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEquals(2, threadsExited.size());
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 0)
    public CompletionStage<String> bulkheadWithoutQueueWithAsyncCompletionStageExitsOnCompletion_Method(
            Future<Void> waiter) throws Exception {
        return bodyReturnThenWaitOnCompletionWithSuccess(waiter);
    }

    @Test(timeout = 3000)
    public void bulkheadWithQueueWithAsyncCompletionStageExitsOnCompletion() {
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(2, 2);
        assertFurtherThreadThrowsBulkheadException10();
        assertEquals(2, threadsEntered.size());
        assertEquals(0, threadsExited.size());
        waitSome(50);
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEquals(4, threadsEntered.size());
        assertEquals(4, threadsExited.size());
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public CompletionStage<String> bulkheadWithQueueWithAsyncCompletionStageExitsOnCompletion_Method(
            Future<Void> waiter) throws Exception {
        return bodyReturnThenWaitOnCompletionWithSuccess(waiter);
    }

    @Test(timeout = 3000)
    public void bulkheadWithoutQueueSingleCapacity() {
        callMethodWithNewThreadAndWaitFor(commonWaiter);
        waitUntilPermitsAquired(1, 0);
        assertFurtherThreadThrowsBulkheadException10();
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        CompletableFuture<Void> waiter = new CompletableFuture<>();
        callMethodWithNewThreadAndWaitFor(waiter);
        waitUntilPermitsAquired(1, 0);
        assertFurtherThreadThrowsBulkheadException10();
        waiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        callWithConcurrentCallers(100, 1);
    }

    @Bulkhead(1)
    public String bulkheadWithoutQueueSingleCapacity_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(waiter);
    }

    private void callAndWait(int expectedMaxConcurrentExecutions) {
        CompletableFuture<Void> waiterExec1 = new CompletableFuture<>();
        List<Thread> execs = new ArrayList<>();
        execs.add(callMethodWithNewThreadAndWaitFor(waiterExec1));
        for (int i = 1; i < expectedMaxConcurrentExecutions; i++) {
            execs.add(callMethodWithNewThreadAndWaitFor(commonWaiter));
        }
        waitUntilPermitsAquired(expectedMaxConcurrentExecutions, 0);
        assertEnteredAndExited(expectedMaxConcurrentExecutions, 0);
        assertFurtherThreadThrowsBulkheadException10();
        waiterExec1.complete(null);
        execs.add(callMethodWithNewThreadAndWaitFor(commonWaiter));
        assertEnteredAndExited(expectedMaxConcurrentExecutions + 1, 1);
        assertFurtherThreadThrowsBulkheadException10();
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEnteredAndExited(expectedMaxConcurrentExecutions + 1, expectedMaxConcurrentExecutions + 1);
        Thread[] expectedSuccessful = execs.toArray(new Thread[0]);
        assertCompletedExecutionLimitedTo(expectedMaxConcurrentExecutions, expectedSuccessful);
        assertExecutionResult("Success", expectedSuccessful);
        assertExecutionError(null);
    }

    private void callWithConcurrentCallers(int numberOfCallers, int expectedMaxConcurrentCallers) {
        int success0 = countExecutionResults("Success");
        int errors0 = countExecutionErrors(BulkheadException.class);
        for (int i = 0; i < numberOfCallers; i++) {
            callMethodWithNewThreadAndWaitFor(null);
        }
        waitUnitAllCallersDone();
        assertMaxConcurrentExecution(expectedMaxConcurrentCallers);
        int success = countExecutionResults("Success") - success0;
        int failedWithBulkheadException = countExecutionErrors(BulkheadException.class) - errors0;
        assertTrue(success > 0);
        assertEquals(numberOfCallers, success + failedWithBulkheadException);
    }
}
