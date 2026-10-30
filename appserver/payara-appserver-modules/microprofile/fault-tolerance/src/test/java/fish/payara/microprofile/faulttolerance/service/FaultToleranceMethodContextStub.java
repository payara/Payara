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
package fish.payara.microprofile.faulttolerance.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.policy.AsynchronousPolicy;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

public class FaultToleranceMethodContextStub implements FaultToleranceMethodContext {

    private final InvocationContext context;
    private final FaultTolerancePolicy policy;
    private final AtomicReference<CircuitBreakerState> state;
    private final AtomicReference<BlockingQueue<Thread>> concurrentExecutions;
    private final AtomicInteger queuingOrRunningPopulation;
    private final BiFunction<InvocationContext, FaultTolerancePolicy, FaultToleranceMethodContext> binder;

    public FaultToleranceMethodContextStub(FaultToleranceServiceStub.StubContext ctx,
                                           AtomicReference<CircuitBreakerState> state,
                                           AtomicReference<BlockingQueue<Thread>> concurrentExecutions,
                                           AtomicInteger queuingOrRunningPopulation) {
        this.context = ctx.context;
        this.policy = ctx.policy;
        this.state = state;
        this.concurrentExecutions = concurrentExecutions;
        this.queuingOrRunningPopulation = queuingOrRunningPopulation;
        this.binder = ctx.binder;
    }

    @Override
    public FaultToleranceMethodContext boundTo(InvocationContext context, FaultTolerancePolicy policy) {
        return binder.apply(context, policy);
    }

    @Override
    public Object proceed() throws Exception {
        return context.proceed();
    }

    @Override
    public FaultToleranceMetrics getMetrics() {
        return FaultToleranceMetrics.DISABLED;
    }

    @Override
    public CircuitBreakerState getState() {
        if (state == null) {
            throw new UnsupportedOperationException();
        }
        return policy.circuitBreaker.requestVolumeThreshold < 0
                ? state.get()
                : state.updateAndGet(
                    value -> value != null ? value :
                        new CircuitBreakerState(policy.circuitBreaker.requestVolumeThreshold, policy.circuitBreaker.failureRatio));
    }

    @Override
    public BlockingQueue<Thread> getConcurrentExecutions() {
        if (concurrentExecutions == null) {
            throw new UnsupportedOperationException();
        }
        int maxConcurrentThreads = policy.bulkhead.value;
        return maxConcurrentThreads < 0
                ? concurrentExecutions.get()
                : concurrentExecutions.updateAndGet(
                    value -> value != null ? value : new ArrayBlockingQueue<>(maxConcurrentThreads));
    }

    @Override
    public AtomicInteger getQueuingOrRunningPopulation() {
        if (queuingOrRunningPopulation == null) {
            throw new UnsupportedOperationException();
        }
        return queuingOrRunningPopulation;
    }

    @Override
    public void delay(long delayMillis) throws InterruptedException {
        throw new UnsupportedOperationException("delay: Override for test case");
    }

    @Override
    public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
        throw new UnsupportedOperationException("runDelayed: Override for test case");
    }

    @Override
    public void runAsynchronous(AsyncFuture asyncResult, Callable<Object> task)
            throws RejectedExecutionException {
        boolean returned = false;
        try {
            Object res = task.call();
            returned = true;
            Object futureResult = AsynchronousPolicy.toFuture(res).get();
            asyncResult.complete(futureResult);
        } catch (Exception ex) {
            asyncResult.setExceptionThrown(!returned);
            asyncResult.completeExceptionally(returned && ex instanceof ExecutionException ? ex.getCause() : ex);
        }
    }

    @Override
    public Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, Throwable ex) throws Exception {
        return fallbackClass.newInstance()
                .handle(new FaultToleranceExecutionContext(context.getMethod(), context.getParameters(), ex));
    }

    @Override
    public Object fallbackInvoke(Method fallbackMethod) throws Exception {
        try {
            fallbackMethod.setAccessible(true);
            return fallbackMethod.invoke(context.getTarget(), context.getParameters());
        } catch (InvocationTargetException e) {
            throw (Exception) e.getTargetException();
        } catch (IllegalAccessException e) {
            throw new FaultToleranceDefinitionException(e); // should not happen as we validated
        }
    }

    @Override
    public void trace(String method) {
        //NOOP, tracing not supported
    }

    @Override
    public void endTrace() {
        //NOOP, tracing not supported
    }
}