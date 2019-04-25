/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests the basic correctness of {@link Bulkhead} handling.
 * 
 * @author Jan Bernitt
 */
public class BulkheadBasicTest {

    final AtomicReference<BulkheadSemaphore> concurrentExecutions = new AtomicReference<>();
    final AtomicReference<BulkheadSemaphore> waitingQueuePopulation = new AtomicReference<>();
    final AtomicInteger bulkheadWithoutQueueCallCount = new AtomicInteger();
    final AtomicInteger bulkheadWithQueueCallCount = new AtomicInteger();
    final AtomicInteger bulkheadWithQueueInterruptedCallCount = new AtomicInteger();

    private final FaultToleranceService service = new FaultToleranceServiceStub() {
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

    /**
     * Makes 2 concurrent request that should succeed acquiring a bulkhead permit.
     * The 3 attempt fails as no queue is in place without {@link Asynchronous}.
     * 
     * Needs a timeout because incorrect implementation could otherwise lead to endless waiting.
     */
    @Test(timeout = 500)
    public void bulkheadWithoutQueue() throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        CompletableFuture<Void> waiter = new CompletableFuture<>();
        Runnable task = () ->  proceedToResultValueOrFail(this, annotatedMethod, waiter);
        new Thread(task).start();
        new Thread(task).start();
        waitUntilPermitsAquired(2, 0);
        assertProceedingThrowsBulkheadException(annotatedMethod); 
        waiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEquals(2, bulkheadWithoutQueueCallCount.get());
    }

    @Bulkhead(value = 2)
    public CompletionStage<String> bulkheadWithoutQueue_Method(Future<Void> waiter) {
        bulkheadWithoutQueueCallCount.incrementAndGet();
        return waitThenReturnSuccess(waiter);
    }

    /**
     * First two request can acquire a bulkhead permit.
     * Following two request can acquire a queue permit.
     * Fifth request fails.
     * 
     * Needs a timeout because incorrect implementation could otherwise lead to endless waiting.
     */
    @Test(timeout = 500)
    public void bulkheadWithQueue() throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        CompletableFuture<Void> waiter = new CompletableFuture<>();
        Runnable task = () ->  proceedToResultValueOrFail(this, annotatedMethod, waiter);
        new Thread(task).start();
        new Thread(task).start();
        new Thread(task).start();
        new Thread(task).start();
        waitUntilPermitsAquired(2, 2);
        assertProceedingThrowsBulkheadException(annotatedMethod); 
        waiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEquals(4, bulkheadWithQueueCallCount.get());
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public CompletionStage<String> bulkheadWithQueue_Method(Future<Void> waiter) {
        bulkheadWithQueueCallCount.incrementAndGet();
        return waitThenReturnSuccess(waiter);
    }

    /**
     * Similar to {@link #bulkheadWithQueue()} just that we interrupt the queueing threads and expect their permits to
     * be released.
     */
    @Test(timeout = 500)
    public void bulkheadWithQueueInterrupted() throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        CompletableFuture<Void> waiter = new CompletableFuture<>();
        Runnable task = () ->  proceedToResultValueOrFail(this, annotatedMethod, waiter);
        new Thread(task).start();
        new Thread(task).start();
        // must wait here to ensure these two threads actually are the ones getting permits
        waitUntilPermitsAquired(2, 0); 
        Thread queueing1 = new Thread(task);
        queueing1.start();
        Thread queueing2 = new Thread(task);
        queueing2.start();
        waitUntilPermitsAquired(2, 2);
        queueing1.interrupt();
        waitUntilPermitsAquired(2, 1);
        queueing2.interrupt();
        waitUntilPermitsAquired(2, 0);
        waiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertEquals(2, bulkheadWithQueueInterruptedCallCount.get());
    }

    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public CompletionStage<String> bulkheadWithQueueInterrupted_Method(Future<Void> waiter) {
        bulkheadWithQueueInterruptedCallCount.incrementAndGet();
        return waitThenReturnSuccess(waiter);
    }

    /*
     * Helpers 
     */

    private Object proceedToResultValueOrFail(Object test, Method annotatedMethod, Future<Void> argument) {
        try {
            return proceedToResultValue(test, annotatedMethod, argument);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Object proceedToResultValue(Object test, Method annotatedMethod, Future<Void> argument) throws Exception {
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(test.getClass(), annotatedMethod);
        return policy.proceed(new StaticAnalysisContext(test, annotatedMethod, argument), service);
    }

    private static CompletionStage<String> waitThenReturnSuccess(Future<Void> waiter) throws AssertionError {
        try {
            waiter.get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return CompletableFuture.completedFuture("Success");
    }

    private void assertProceedingThrowsBulkheadException(Method annotatedMethod) throws Exception {
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

    private void waitUntilPermitsAquired(int concurrentExecutions, int waitingQueuePopulation) {
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

    private static boolean equalAcquiredPermits(int expected, BulkheadSemaphore actual) {
        return actual == null ? expected == 0 : actual.acquiredPermits() == expected;
    }
}
