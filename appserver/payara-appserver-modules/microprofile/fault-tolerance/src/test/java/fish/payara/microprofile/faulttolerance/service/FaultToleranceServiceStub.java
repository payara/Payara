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
package fish.payara.microprofile.faulttolerance.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.policy.AsynchronousPolicy;
import fish.payara.microprofile.faulttolerance.service.Stereotypes;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

/**
 * A stub of {@link FaultToleranceService} that can be used in tests as a basis.
 * 
 * Most methods need to be overridden with test behaviour.
 * 
 * The {@link #runAsynchronous(CompletableFuture, InvocationContext, Callable)} does run the task synchronous for
 * deterministic tests behaviour.
 * 
 * @author Jan Bernitt
 *
 */
public class FaultToleranceServiceStub implements FaultToleranceService {

    @Override
    public FaultToleranceConfig getConfig(InvocationContext context, Stereotypes stereotypes) {
        return FaultToleranceConfig.asAnnotated(context.getTarget().getClass(), context.getMethod());
    }

    @Override
    public FaultToleranceMetrics getMetrics(InvocationContext context) {
        return FaultToleranceMetrics.DISABLED;
    }

    @Override
    public CircuitBreakerState getState(int requestVolumeThreshold, InvocationContext context) {
        throw new UnsupportedOperationException("Override for test case");
    }

    @Override
    public BulkheadSemaphore getConcurrentExecutions(int maxConcurrentThreads, InvocationContext context) {
        throw new UnsupportedOperationException("Override for test case");
    }

    @Override
    public BulkheadSemaphore getWaitingQueuePopulation(int queueCapacity, InvocationContext context) {
        throw new UnsupportedOperationException("Override for test case");
    }

    @Override
    public void delay(long delayMillis, InvocationContext context) throws InterruptedException {
        throw new UnsupportedOperationException("Override for test case");        
    }

    @Override
    public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
        throw new UnsupportedOperationException("Override for test case");
    }

    @Override
    public void runAsynchronous(CompletableFuture<Object> asyncResult, InvocationContext context, Callable<Object> task)
            throws RejectedExecutionException {
        try {
            asyncResult.complete(AsynchronousPolicy.toFuture(task.call()).get());
        } catch (Exception e) {
            asyncResult.completeExceptionally(e);
        }
    }

    @Override
    public Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, InvocationContext context,
            Exception ex) throws Exception {
        return fallbackClass.newInstance()
                .handle(new FaultToleranceExecutionContext(context.getMethod(), context.getParameters(), ex));
    }

    @Override
    public Object fallbackInvoke(Method fallbackMethod, InvocationContext context) throws Exception {
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
    public void trace(String method, InvocationContext context) {
        //NOOP, tracing not supported
    }

    @Override
    public void endTrace() {
        //NOOP, tracing not supported
    }

}
