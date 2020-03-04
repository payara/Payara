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
package fish.payara.microprofile.faulttolerance.service;

import static java.lang.System.currentTimeMillis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.policy.AsynchronousPolicy;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import fish.payara.notification.requesttracing.RequestTraceSpan;

/**
 * The execution context for a FT annotated method. Each {@link Method} for each individual target {@link Object}
 * (instance of the {@link Class} defining the {@link Method}) has its corresponding instance of this
 * {@link FaultToleranceMethodContext}.
 * 
 * When the annotated {@link Method} is invoked this implementation is bound to that context by
 * {@link #in(InvocationContext, FaultTolerancePolicy)} with a fresh instance of this class. It shares all the state
 * with other invocations for the same method except the {@link InvocationContext} and the {@link FaultTolerancePolicy}
 * which are specific for each invocation. This way the full FT invocation state for each method invocation is
 * determined at the beginning of applying FT semantics and cannot change during execution (except for those counters
 * and queues that are meant to track the shared state changes of course).
 * 
 * @author Jan Bernitt
 */
public final class FaultToleranceMethodContextImpl implements FaultToleranceMethodContext {

    private static final Logger logger = Logger.getLogger(FaultToleranceMethodContextImpl.class.getName());

    private final FaultToleranceRequestTracing requestTracing;
    private final FaultToleranceMetrics metrics;
    private final ExecutorService asyncExecution;
    private final ScheduledExecutorService delayedExecution;
    private final AtomicReference<CircuitBreakerState> circuitBreakerState;
    private final AtomicReference<BlockingQueue<Thread>> concurrentExecutions;
    private final AtomicInteger queuingOrRunningPopulation;
    private final InvocationContext context;
    private final FaultTolerancePolicy policy;
    private final AtomicInteger executingThreadCount;
    private final AtomicLong lastCalled;

    public FaultToleranceMethodContextImpl(FaultToleranceRequestTracing requestTracing,
           FaultToleranceMetrics metrics, ExecutorService asyncExecution,
           ScheduledExecutorService delayedExecution) {
        this(requestTracing, metrics, asyncExecution, delayedExecution, new AtomicReference<>(),
                new AtomicReference<>(), new AtomicInteger(), null, null, new AtomicInteger(),
                new AtomicLong(currentTimeMillis()));
    }

    private FaultToleranceMethodContextImpl(FaultToleranceRequestTracing requestTracing,
            FaultToleranceMetrics metrics, ExecutorService asyncExecution,
            ScheduledExecutorService delayedExecution, AtomicReference<CircuitBreakerState> circuitBreakerState,
            AtomicReference<BlockingQueue<Thread>> concurrentExecutions,
            AtomicInteger queuingOrRunningPopulation, InvocationContext context, FaultTolerancePolicy policy, 
            AtomicInteger executingThreadCount, AtomicLong lastCalled) {
        super();
        this.requestTracing = requestTracing;
        this.metrics = metrics;
        this.asyncExecution = asyncExecution;
        this.delayedExecution = delayedExecution;
        this.circuitBreakerState = circuitBreakerState;
        this.concurrentExecutions = concurrentExecutions;
        this.queuingOrRunningPopulation = queuingOrRunningPopulation;
        this.context = context;
        this.policy = policy;
        this.executingThreadCount = executingThreadCount;
        this.lastCalled = lastCalled;
    }

    public FaultToleranceMethodContextImpl in(InvocationContext context, FaultTolerancePolicy policy) {
        return new FaultToleranceMethodContextImpl(requestTracing, metrics, asyncExecution, delayedExecution,
                circuitBreakerState, concurrentExecutions, queuingOrRunningPopulation, context, policy,
                executingThreadCount, lastCalled);
    }

    public boolean isExpired(long ttl) {
        return executingThreadCount.get() == 0 && lastCalled.get() + ttl < System.currentTimeMillis();
    }

    @Override
    public Object proceed() throws Exception {
        try {
            int in = executingThreadCount.incrementAndGet();
            if (policy.isBulkheadPresent() && in > policy.bulkhead.value) {
                logger.log(Level.WARNING, "Bulkhead appears to have been breeched, now executing {0} for method {1}",
                        new Object[] { in, context.getMethod() });
            }
            return context.proceed();
        } finally {
            executingThreadCount.decrementAndGet();
        }
    }

    @Override
    public FaultToleranceMetrics getMetrics(boolean enabled) {
        return enabled ? metrics : FaultToleranceMetrics.DISABLED;
    }

    /*
     * Execution
     */

    @Override
    public CircuitBreakerState getState(int requestVolumeThreshold) {
        return requestVolumeThreshold < 0 
                ? circuitBreakerState.get()
                : circuitBreakerState.updateAndGet(value -> value != null ? value : new CircuitBreakerState(requestVolumeThreshold));
    }

    @Override
    public BlockingQueue<Thread> getConcurrentExecutions(int maxConcurrentThreads) {
        return maxConcurrentThreads < 0
                ? concurrentExecutions.get()
                : concurrentExecutions.updateAndGet(value -> value != null ? value : new ArrayBlockingQueue<>(maxConcurrentThreads));
    }

    @Override
    public AtomicInteger getQueuingOrRunningPopulation() {
        return queuingOrRunningPopulation;
    }

    @Override
    public void delay(long delayMillis) throws InterruptedException {
        if (delayMillis <= 0) {
            return;
        }
        trace("delayRetry");
        try {
            Thread.sleep(delayMillis);
        } finally {
            endTrace();
        }
    }

    @Override
    public void runAsynchronous(CompletableFuture<Object> asyncResult, Callable<Object> task)
            throws RejectedExecutionException {
        Runnable completionTask = () -> {
            if (!asyncResult.isCancelled() && !Thread.currentThread().isInterrupted()) {
                try {
                    trace("runAsynchronous");
                    Future<?> futureResult = AsynchronousPolicy.toFuture(task.call());
                    if (!asyncResult.isCancelled()) { // could be cancelled in the meanwhile
                        if (!asyncResult.isDone()) {
                            asyncResult.complete(futureResult.get());
                        }
                    } else {
                        futureResult.cancel(true);
                    }
                } catch (Exception ex) {
                    // Note that even ExecutionException is not unpacked (intentionally)
                    asyncResult.completeExceptionally(ex); 
                } finally {
                    endTrace();
                }
            }
        };
        asyncExecution.submit(completionTask);
    }

    @Override
    public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
        return delayedExecution.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass,
            Exception ex) throws Exception {
        return CDI.current().select(fallbackClass).get().handle(
                new FaultToleranceExecutionContext(context.getMethod(), context.getParameters(), ex));
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
        requestTracing.startSpan(new RequestTraceSpan(method), context);
    }

    @Override
    public void endTrace() {
        requestTracing.endSpan();
    }

}