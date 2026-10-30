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

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

/**
 * Runs test similar to {@code org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadAsynchRetryTest} as JUnit
 * tests.
 * 
 * Uses the same test method names and tries the duplicate the test as good as possible on the
 * {@link FaultTolerancePolicy} level. For many identifiers the terminology of the TCK test is used to make them easier
 * to compare.
 */
public class BulkheadAsyncRetryTckTest extends AbstractBulkheadTest {

    @Test(timeout = 60 * 1000)
    public void testBulkheadClassAsynchronousPassiveRetry55() {
        assertExecutionResult("Success", loop(10, 5, 5));
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = { BulkheadException.class }, delay = 100, delayUnit = ChronoUnit.MILLIS, 
        maxRetries = 10, maxDuration = 1000, jitter = 0)
    public Future<?> testBulkheadClassAsynchronousPassiveRetry55_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter).toCompletableFuture();
    }

    @Test(timeout = 60 * 1000)
    public void testBulkheadMethodAsynchronousRetry55() {
        assertExecutionResult("Success", loop(20, 5, 5));
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = { BulkheadException.class }, delay = 100, delayUnit = ChronoUnit.MILLIS, 
        maxRetries = 10, maxDuration = 1000, jitter = 0)
    public Future<?> testBulkheadMethodAsynchronousRetry55_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter).toCompletableFuture();
    }

    private Thread[] loop(int iterations, int maxSimultaneousWorkers, int maxSimultaneursQueuing) {
        Thread[] callers = new Thread[iterations];
        int bulkheadCapacity = maxSimultaneousWorkers + maxSimultaneursQueuing;
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] waiters = new CompletableFuture[bulkheadCapacity];
        for (int i = 0; i < bulkheadCapacity; i++) {
            waiters[i] = new CompletableFuture<>();
            callers[i] = callMethodWithNewThreadAndWaitFor(waiters[i]);
        }
        waitUntilPermitsAquired(maxSimultaneousWorkers, maxSimultaneursQueuing);
        waitSome(100);
        assertPermitsAquired(maxSimultaneousWorkers, maxSimultaneursQueuing);
        for (int i = bulkheadCapacity; i < iterations; i++) {
            callers[i] = callMethodWithNewThreadAndWaitFor(commonWaiter);
        }
        waitSome(100);
        for (CompletableFuture<Void> w : waiters) {
            w.complete(null);
            waitSome(50);
        }
        waitSome(100);
        commonWaiter.complete(null);
        waitUntilPermitsAquired(0, 0);
        assertMaxConcurrentExecution(maxSimultaneousWorkers);
        return Arrays.copyOf(callers, bulkheadCapacity);
    }
}
