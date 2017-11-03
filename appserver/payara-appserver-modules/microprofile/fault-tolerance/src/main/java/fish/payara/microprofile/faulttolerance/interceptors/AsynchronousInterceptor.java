/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.faulttolerance.interceptors;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 * Interceptor for the Fault Tolerance Asynchronous Annotation. Also contains the wrapper class for the Future outcome.
 *
 * @author Andrew Pielage
 */
@Interceptor
@Asynchronous
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class AsynchronousInterceptor implements Serializable {

    @Inject
    BeanManager beanManager;
    
    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        // Get the configured ManagedExecutorService from the Fault Tolerance Service
        FaultToleranceService faultToleranceService = Globals.getDefaultBaseServiceLocator()
                .getService(FaultToleranceService.class);
        ManagedExecutorService managedExecutorService = faultToleranceService.getManagedExecutorService();

        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class);
        Config config = ConfigProvider.getConfig();
        
        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled
            if (faultToleranceService.isFaultToleranceEnabled(invocationManager.getCurrentInvocation().getAppName(), 
                    config)) {
                proceededInvocationContext = new FutureDelegator(managedExecutorService.submit(() -> { 
                    return invocationContext.proceed();
                }), invocationContext);
            } else {
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            // If an exception was thrown, check if the method is annotated with @Fallback
            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);
            
            // If the method was annotated with Fallback, attempt it, otherwise just propagate the exception upwards
            if (fallback != null) {
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                proceededInvocationContext = fallbackPolicy.fallback(invocationContext);
            } else {
                throw ex;
            }
        }
        
        return proceededInvocationContext;
    }

    class FutureDelegator implements Future<Object> {

        private final Future<?> future;
//        private final InvocationContext invocationContext;

        public FutureDelegator(Future<?> future, InvocationContext invocationContext) {
            this.future = future;
//            this.invocationContext = invocationContext;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            Object proceededInvocation = null;

            // Attempt to get the outcome of the InvocationContext
//            try {
                proceededInvocation = future.get();
//            } catch (InterruptedException | ExecutionException ex) {
//                // If an exception gets thrown, attempt fallback
//                proceededInvocation = attemptFallback();
//
//                // If fallback wasn't enabled, propoagate the error upwards
//                if (proceededInvocation == null) {
//                    throw ex;
//                }
//            }

            return proceededInvocation;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            Object proceededInvocation = null;

            // Attempt to get the outcome of the InvocationContext
//            try {
                proceededInvocation = future.get(timeout, unit);
//            } catch (InterruptedException | ExecutionException ex) {
//                // If an exception gets thrown, attempt fallback
//                proceededInvocation = attemptFallback();
//
//                // If fallback wasn't enabled, propoagate the error upwards
//                if (proceededInvocation == null) {
//                    throw ex;
//                }
//            }

            return proceededInvocation;
        }

//        /**
//         * Check if the method was annotated with the Fallback annotation, and attempt the fallback if so.
//         *
//         * @return An Object representing the outcome of the fallback execution, or null if fallback wasn't enabled
//         * @throws ExecutionException
//         */
//        private Object attemptFallback() throws ExecutionException {
//            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);
//            Config config = ConfigProvider.getConfig();
//            if (fallback != null) {
//                try {
//                    FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config,
//                            invocationContext.getMethod().getName(),
//                            invocationContext.getMethod().getDeclaringClass().getCanonicalName());
//
//                    try {
//                        return fallbackPolicy.fallback(invocationContext);
//                    } catch (Exception fallbackException) {
//                        throw new ExecutionException(fallbackException);
//                    }
//                } catch (ClassNotFoundException ex) {
//                    throw new ExecutionException(ex);
//                }
//            }
//            return null;
//        }
    }
}
