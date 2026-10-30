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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.interceptor.InvocationContext;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
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

    private final ConcurrentMap<MethodKey, FaultToleranceMethodContext> contextByMethodId = new ConcurrentHashMap<>();

    protected final AtomicReference<CircuitBreakerState> state = new AtomicReference<>();
    protected final AtomicReference<BlockingQueue<Thread>> concurrentExecutions = new AtomicReference<>();
    protected final AtomicInteger waitingQueuePopulation = new AtomicInteger();

    protected class StubContext {
        public final InvocationContext context;
        public final FaultTolerancePolicy policy;
        private final MethodKey methodKey;
        public final Object key;
        public BiFunction<InvocationContext, FaultTolerancePolicy, FaultToleranceMethodContext> binder;

        protected StubContext(MethodKey key, InvocationContext context, FaultTolerancePolicy policy) {
            this.methodKey = key;
            this.key = key;
            this.context = context;
            this.policy = policy;
            this.binder = (i, p) -> createMethodContext(methodKey, i, p);
        }

    }

    @Override
    public FaultToleranceConfig getConfig(InvocationContext context, Stereotypes stereotypes) {
        return FaultToleranceConfig.asAnnotated(context.getTarget().getClass(), context.getMethod());
    }

    @Override
    public final FaultToleranceMethodContext getMethodContext(InvocationContext context, FaultTolerancePolicy policy) {
        return getMethodContext(context, policy, null);
    }

    @Override
    public final FaultToleranceMethodContext getMethodContext(InvocationContext context, FaultTolerancePolicy policy,
            RequestContextController requestContextController) {
        return contextByMethodId.computeIfAbsent(new MethodKey(context),
                key -> createMethodContext(key, context, policy)).boundTo(context, policy);
    }

    @SuppressWarnings("unused")
    protected final FaultToleranceMethodContext createMethodContext(MethodKey key, InvocationContext context,
                                                              FaultTolerancePolicy policy) {
        return stubMethodContext(new StubContext(key, context, policy));
    }

    protected FaultToleranceMethodContext stubMethodContext(StubContext ctx) {
        return new FaultToleranceMethodContextStub(ctx, state, concurrentExecutions, waitingQueuePopulation);
    }

    public AtomicReference<CircuitBreakerState> getStateReference() {
        return state;
    }

    public AtomicReference<BlockingQueue<Thread>> getConcurrentExecutionsReference() {
        return concurrentExecutions;
    }

    public AtomicInteger getWaitingQueuePopulationReference() {
        return waitingQueuePopulation;
    }
}
