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
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

/**
 * Tests that bulkhead method is not entered by more callers than its capacity.
 */
public class BulkheadStressTest extends AbstractBulkheadTest {

    @Test
    public void bulkheadWithQueueAndRetry_55_100() {
        loop(5, 100, 5);
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = {
            BulkheadException.class }, delay = 100, delayUnit = ChronoUnit.MILLIS, maxRetries = 10, maxDuration = 999999)
    public Future<?> bulkheadWithQueueAndRetry_55_100_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(waiter).toCompletableFuture();
    }

    @Test
    public void bulkheadWithQueueAndRetry_55_100_NoDelay() {
        loop(1, 100, 5);
    }

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = {
            BulkheadException.class }, delay = 100, delayUnit = ChronoUnit.MILLIS, maxRetries = 10, maxDuration = 999999)
    public Future<?> bulkheadWithQueueAndRetry_55_100_NoDelay_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccess(null).toCompletableFuture();
    }

    @Test
    public void bulkheadWithoutQueueSingleCapacity() {
        loop(0, 100, 3);
        assertMaxConcurrentExecution(1);
    }

    @Bulkhead(1)
    public String bulkheadWithoutQueueSingleCapacity_Method(Future<Void> waiter) throws Exception {
        return bodyWaitThenReturnSuccessDirectly(null);
    }

    private void loop(int speed, int concurrentCallers, int maxSimultaneousWorkers) {
        int bulkheadMethodReturned = 0;
        Random rng = new Random();
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] waiters = new CompletableFuture[concurrentCallers];
        Thread[] callers = new Thread[concurrentCallers];
        for (int i = 0; i < concurrentCallers; i++) {
            waiters[i] = new CompletableFuture<>();
            callers[i] = callMethodWithNewThreadAndWaitFor(waiters[i]);
            if (i > maxSimultaneousWorkers) {
                for (int j = 0; j < i / maxSimultaneousWorkers; j++) {
                    if (speed > 0)
                        waitSome(rng.nextInt(speed*4));
                    int completedWaiter = rng.nextInt(i);
                    CompletableFuture<Void> completedCall = waiters[completedWaiter];
                    switch (rng.nextInt(4)) {
                    default:
                    case 0:
                        if (completedCall.complete(null)) {
                            bulkheadMethodReturned++;
                        }
                        break;
                    case 1:
                        completedCall.cancel(true);
                        break;
                    case 2:
                        completedCall.cancel(false);
                        break;
                    case 3:
                        callers[completedWaiter].interrupt();
                    }
                }
            }
        }
        for (int i = 0; i < waiters.length; i++) {
            waiters[i].cancel(true);
        }
        waitUntilPermitsAquired(0, 0);
        // setting the minimum to max - 1 is not logically correct (could be 1) but we expect the bulkhead to be used fully
        // so it really should be its capacity or 1 less in a bad corner case
        if (speed > 0)
            assertRange(speed == 1 ? 1 : maxSimultaneousWorkers - 1, maxSimultaneousWorkers, maxConcurrentExecutionsCount.get());
        assertMaxConcurrentExecution(maxSimultaneousWorkers);
        // the number of threads that ever entered the method should be between the number of threads that completed waiting
        // and all threads
        assertRange(bulkheadMethodReturned - 1, concurrentCallers, annotatedMethodCallCount.get());
    }
}
