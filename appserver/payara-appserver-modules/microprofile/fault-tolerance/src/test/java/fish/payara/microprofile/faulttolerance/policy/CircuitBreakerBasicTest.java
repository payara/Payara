/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState.CircuitState;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests the basic correctness of {@link CircuitBreaker} handling.
 *
 * @author Jan Bernitt
 */
public class CircuitBreakerBasicTest {

    private final AtomicInteger circuitBreakerCallCounter = new AtomicInteger();
    final CompletableFuture<Void> waitBeforeHalfOpenAgain = new CompletableFuture<>();
    private final FaultToleranceServiceStub service = new FaultToleranceServiceStub() {

        @Override
        protected FaultToleranceMethodContext stubMethodContext(StubContext ctx) {
            return new FaultToleranceMethodContextStub(ctx, state, concurrentExecutions, waitingQueuePopulation) {

                @Override
                public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
                    // test method completes the future when it is ready for execution to proceed and open the circuit (task)
                    if (waitBeforeHalfOpenAgain.isDone()) {
                        task.run();
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.runAsync(() -> {
                        try {
                            waitBeforeHalfOpenAgain.get();
                        } catch (Exception e) {
                            // continue
                        }
                        task.run();
                    });
                }
            };
        }
    };

    final AtomicReference<CircuitBreakerState> state = service.getStateReference();

    @Before
    public void resetCounter() {
        circuitBreakerCallCounter.set(0);
    }

    /**
     * The simplest possible {@link CircuitBreaker} without a delay between OPEN and HALF-OPEN whereby OPEN is not
     * observable short.
     */
    @Test
    public void circuitBreakerNoDelay() throws Exception {
        assertEquals(1, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        assertEquals(3, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        assertEquals(CircuitState.HALF_OPEN, state.get().getCircuitState());
        assertEquals(5, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        // and so forth, the circuit essentially isn't observable as open since delay is zero
        assertEquals(7, proceedToResultValue().intValue()); // success will close again
        assertEquals(CircuitState.CLOSED, state.get().getCircuitState());
    }

    @CircuitBreaker(requestVolumeThreshold = 3, delay = 0)
    public int circuitBreakerNoDelay_Method() {
        return incrementAndFailingOnEveryOtherInvocationOnFirst6();
    }

    /**
     * Tests the {@link CircuitBreaker} with delay whereby OPEN state is kept for a time here mocked by waiting for the
     * {@link #waitBeforeHalfOpenAgain} future to have the test control timing.
     */
    @Test
    public void circuitBreakerWithDelay() throws Exception {
        assertEquals(1, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        assertEquals(3, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        assertEquals(CircuitState.OPEN, state.get().getCircuitState());
        assertProceedToResultValueFails(CircuitBreakerOpenException.class);
        assertProceedToResultValueFails(CircuitBreakerOpenException.class);
        assertProceedToResultValueFails(CircuitBreakerOpenException.class);
        // and it would go on until delay passed (here waiting for waitBeforeHalfOpenAgain):
        waitBeforeHalfOpenAgain.complete(null); // now transitions to half-open (async)
        waitForState(CircuitState.HALF_OPEN);
        assertEquals(CircuitState.HALF_OPEN, state.get().getCircuitState());
        assertEquals(4, circuitBreakerCallCounter.get());
        assertEquals(5, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        waitForState(CircuitState.HALF_OPEN);
        assertEquals(7, proceedToResultValue().intValue());
        assertEquals(8, proceedToResultValue().intValue()); // now we got 2 in a row: closing
        assertEquals(CircuitState.CLOSED, state.get().getCircuitState());
    }

    @CircuitBreaker(requestVolumeThreshold = 3, successThreshold = 2)
    public int circuitBreakerWithDelay_Method() {
        return incrementAndFailingOnEveryOtherInvocationOnFirst6();
    }

    @Test
    public void circuitBreakerNotFailingOn() throws Exception {
        assertEquals(1, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        assertEquals(3, proceedToResultValue().intValue());
        assertProceedToResultValueFails(IllegalStateException.class);
        assertEquals("Circuit should still be closed as the exception thrown in not matching one the circuit should fail on",
                CircuitState.CLOSED, state.get().getCircuitState());
    }

    @CircuitBreaker(requestVolumeThreshold = 3, delay = 0, failOn = NoSuchElementException.class)
    public int circuitBreakerNotFailingOn_Method() {
        return incrementAndFailingOnEveryOtherInvocationOnFirst6();
    }

    /*
     * Helpers
     */

    private void assertProceedToResultValueFails(Class<? extends Exception> expected) throws Exception {
        try {
            Integer resultValue = proceedToResultValue();
            fail("Expected the call to fail but it did return: " + resultValue);
        } catch (Exception ex) {
            assertEquals(expected, ex.getClass());
        }
    }

    private Integer proceedToResultValue(Object... methodArguments) throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(getClass(), annotatedMethod);
        StaticAnalysisContext context = new StaticAnalysisContext(this, annotatedMethod, methodArguments);
        return (Integer) policy.proceed(context, () -> service.getMethodContext(context, policy));
    }

    private int incrementAndFailingOnEveryOtherInvocationOnFirst6() {
        int resultValue = circuitBreakerCallCounter.incrementAndGet();
        if (resultValue < 7 && resultValue % 2 == 0) {
            throw new IllegalStateException("Fails every 2nd invocation");
        }
        return resultValue;
    }

    private void waitForState(CircuitState expected) {
        long delay = 4L;
        while (state.get().getCircuitState() != expected) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return; // give up
            }
            delay *= 2;
        }
    }
}
