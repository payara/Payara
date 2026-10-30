/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

public interface FaultToleranceMethodContext {

    class AsyncFuture extends CompletableFuture<Object> {

        private volatile boolean exceptionThrown;

        public void setExceptionThrown(boolean exceptionThrown) {
            this.exceptionThrown = exceptionThrown;
        }

        public boolean isExceptionThrown() {
            return exceptionThrown;
        }
    }

    FaultToleranceMethodContext boundTo(InvocationContext context, FaultTolerancePolicy policy);

    /**
     * Returns the {@link FaultToleranceMetrics} to use.
     *
     * @return the {@link FaultToleranceMetrics} to use, {@link FaultToleranceMetrics#DISABLED} when not enabled.
     */
    FaultToleranceMetrics getMetrics();

    /*
     * State
     */

    /**
     * Get or create the {@link CircuitBreakerState}.
     *
     * @return the created or existing state, or null if non existed and requestVolumeThreshold was null
     */
    CircuitBreakerState getState();

    /**
     * Get or create the {@link BlockingQueue} for bulkhead.
     *
     * @return the created or existing queue, or null if non existed and requestVolumeThreshold was null
     */
    BlockingQueue<Thread> getConcurrentExecutions();

    /**
     * Get the bulkhead thread count.
     *
     * @return This are number of threads that are either waiting or running in the bulkhead.
     */
    AtomicInteger getQueuingOrRunningPopulation();

    /*
     * Processing
     */

    /**
     * Proceeds execution to the annotated method body.
     *
     * @return result returned by the annotated method
     * @throws Exception in case the annotated method threw an {@link Exception}.
     */
    Object proceed() throws Exception;

    /**
     * Delays the current thread by the given duration. The delay is traced.
     *
     * @param delayMillis the time to sleep in milliseconds
     * @param context     current context delayed
     * @throws InterruptedException In case waiting is interrupted
     */
    void delay(long delayMillis) throws InterruptedException;

    /**
     * Runs a given task after a certain waiting time.
     *
     * @param delayMillis time to wait in milliseconds before running the given task
     * @param task        operation to run
     * @return A future that can be cancelled if the operation should no longer be run
     */
    Future<?> runDelayed(long delayMillis, Runnable task) throws Exception;

    /**
     * Runs the task asynchronously and completes the given asyncResult with the its outcome.
     *
     * @param asyncResult a not yet completed {@link CompletableFuture} that should receive the result of the operation
     *                    when it is executed
     * @param context     the currently processed context (for e.g. tracing)
     * @param task        an operation that must compute a value of type {@link Future} or {@link CompletionStage}.
     * @throws RejectedExecutionException In case the task could not be accepted for execution. Usually due to too many
     *                                    work in progress.
     */
    void runAsynchronous(AsyncFuture asyncResult, Callable<Object> task)
            throws RejectedExecutionException;

    /**
     * Invokes the instance of the given {@link FallbackHandler} {@link Class} defined in the given context to handle
     * the given {@link Exception}.
     *
     * @param fallbackClass the type of {@link FallbackHandler} to resolve or instantiate and use
     * @param context       the currently processed context to use for arguments
     * @param ex            the {@link Exception} thrown by the FT processing to handle by the {@link FallbackHandler}
     * @return the result returned by the invoked {@link FallbackHandler}
     * @throws Exception in case resolving, instantiating or invoking the handler method fails
     */
    Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, Throwable ex)
            throws Exception;

    /**
     * Invokes the given fallback {@link Method} in the given context.
     *
     * @param fallbackMethod the {@link Method} to invoke
     * @param context        the currently processed context to use for target instance and method arguments
     * @return the result returned by the invoked fallback method
     * @throws Exception in case invoking the method fails or the invoked method threw an {@link Exception}
     */
    Object fallbackInvoke(Method fallbackMethod) throws Exception;

    /*
     * Tracing
     */

    /**
     * Starts tracing the given context named with the given method label.
     *
     * @param method  the label to use for the trace
     * @param context the currently processed context
     */
    void trace(String method);

    /**
     * Ends the innermost trace.
     */
    void endTrace();
}
