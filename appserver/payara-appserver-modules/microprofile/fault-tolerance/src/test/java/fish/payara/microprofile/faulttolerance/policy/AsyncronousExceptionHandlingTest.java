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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Checks that {@link Asynchronous} methods exception handling use proper semantics based on the return type of the
 * annotated method.
 * 
 * A {@link CompletionStage} is only successful if it is completed successful (that is with a value).
 * In contrast to {@link CompletionStage} a method returning a {@link Future} is successful if it returns a
 * {@link Future} instance independent of if that {@link Future} later completes with an exception.
 * 
 * @author Jan Bernitt
 */
public class AsyncronousExceptionHandlingTest {

    final AtomicInteger asyncFutureWithRetryCallCount = new AtomicInteger();
    final AtomicInteger asyncFutureWithRetryThrowsExceptionCallCount = new AtomicInteger();

    final AtomicInteger asyncCompletionStageWithRetryCallCount = new AtomicInteger();
    final AtomicInteger asyncCompletionStageWithRetryThrowsExceptionCallCount = new AtomicInteger();
    final AtomicInteger asyncCompletionStageWithRetrySuccessOnRetryCallCount = new AtomicInteger();

    /**
     * When returning a {@link Future} there is only 1 attempt made since a {@link Future} instance is returned on first
     * attempt.
     */
    @Test
    public void asyncFutureWithRetry() throws Exception {
        Future<?> result = proceedToDoneFuture();
        assertEquals("no retry attempt should occur", 1, asyncFutureWithRetryCallCount.get());
        assertOriginalException(result);
    }

    @Retry(jitter = 0L)
    @Asynchronous
    public Future<String> asyncFutureWithRetry_Method() {
        asyncFutureWithRetryCallCount.incrementAndGet();
        return completedExceptionally(new IllegalStateException("Original exception"));
    }

    /**
     * When the method returning a {@link Future} throws an {@link Exception} the {@link Retry} is applied and further
     * attempts are made.
     */
    @Test
    public void asyncFutureWithRetryThrowsException() throws Exception {
        Future<?> result = proceedToDoneFuture();
        assertEquals("all retry attempts should occur", 4, asyncFutureWithRetryThrowsExceptionCallCount.get());
        assertOriginalException(result);
    }

    @Retry(jitter = 0L)
    @Asynchronous
    public Future<String> asyncFutureWithRetryThrowsException_Method() {
        asyncFutureWithRetryThrowsExceptionCallCount.incrementAndGet();
        throw new IllegalStateException("Originally thrown exception");
    }

    /**
     * A method returning {@link CompletionStage} must complete successful otherwise {@link Retry} is effective and
     * further attempts are made.
     */
    @Test
    public void asyncCompletionStageWithRetry() throws Exception {
        Future<?> result = proceedToDoneFuture();
        assertEquals("all retry attempts should occur", 3, asyncCompletionStageWithRetryCallCount.get());
        assertOriginalException(result);
    }

    @Retry(maxRetries = 2, jitter = 0L)
    @Asynchronous
    public CompletionStage<String> asyncCompletionStageWithRetry_Method() {
        asyncCompletionStageWithRetryCallCount.incrementAndGet();
        return completedExceptionally(new IllegalStateException("Original exception"));
    }

    /**
     * For a method returning {@link CompletionStage} throwing an exception is similarly handled to returning a value
     * that later completes exceptionally. Therefore retry attempts should occur.
     */
    @Test
    public void asyncCompletionStageWithRetryThrowsException() throws Exception {
        Future<?> result = proceedToDoneFuture();
        assertEquals("all retry attempts should occur", 3, asyncCompletionStageWithRetryThrowsExceptionCallCount.get());
        assertOriginalException(result);
    }

    @Retry(maxRetries = 2, jitter = 0L)
    @Asynchronous
    public CompletionStage<String> asyncCompletionStageWithRetryThrowsException_Method() {
        asyncCompletionStageWithRetryThrowsExceptionCallCount.incrementAndGet();
        throw new IllegalStateException("Originally thrown exception");
    }

    /**
     * A {@link CompletionStage} that first completes exceptionally should make further {@link Retry} attempts and
     * complete successful in this scenario.
     */
    @Test
    public void asyncCompletionStageWithRetrySuccessOnRetry() throws Exception {
        Future<?> result = proceedToDoneFuture();
        assertEquals("one retry should lead to success", 2, asyncCompletionStageWithRetrySuccessOnRetryCallCount.get());
        assertEquals("Success", result.get());
    }

    @Retry(maxRetries = 2, jitter = 0L)
    @Asynchronous
    public CompletionStage<String> asyncCompletionStageWithRetrySuccessOnRetry_Method() {
        int callNo = asyncCompletionStageWithRetrySuccessOnRetryCallCount.incrementAndGet();
        return callNo < 2
                ? completedExceptionally(new IllegalStateException("Original exception"))
                : CompletableFuture.completedFuture("Success");
    }

    private Future<?> proceedToDoneFuture() throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(getClass(), annotatedMethod);
        StaticAnalysisContext context = new StaticAnalysisContext(this, annotatedMethod);
        Future<?> result = AsynchronousPolicy.toFuture(
                policy.proceed(context, () -> new FaultToleranceServiceStub().getMethodContext(context, policy)));
        assertTrue(result.isDone());
        return result;
    }

    private static void assertOriginalException(Future<?> result) throws InterruptedException {
        try {
            result.get();
            fail("Should have completed exceptionally");
        } catch (ExecutionException ex) {
            assertEquals("Did not preseve the exception the returned future was completed with", 
                    IllegalStateException.class, ex.getCause().getClass());
            assertTrue(ex.getCause().getMessage().startsWith("Original"));
        }
    }

    private static CompletableFuture<String> completedExceptionally(Exception ex) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }
}
