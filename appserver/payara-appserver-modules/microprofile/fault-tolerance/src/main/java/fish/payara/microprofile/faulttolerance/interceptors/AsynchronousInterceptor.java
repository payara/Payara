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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
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

    private static final Logger logger = Logger.getLogger(AsynchronousInterceptor.class.getName());
    
    @Inject
    BeanManager beanManager;
    
    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        // Get the configured ManagedExecutorService from the Fault Tolerance Service
        FaultToleranceService faultToleranceService = Globals.getDefaultBaseServiceLocator()
                .getService(FaultToleranceService.class);
        ManagedExecutorService managedExecutorService = faultToleranceService.getManagedExecutorService();

        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        
        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled
            if (faultToleranceService.isFaultToleranceEnabled(faultToleranceService.getApplicationName(
                    invocationManager, invocationContext), config)) {                
                Callable callable = () -> {
                    return invocationContext.proceed();
                };
                logger.log(Level.FINER, "Proceeding invocation asynchronously");
                proceededInvocationContext = new FutureDelegator(managedExecutorService.submit(callable));
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled for {0}, proceeding normally without asynchronous.", 
                        faultToleranceService.getApplicationName(invocationManager, invocationContext));
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            // If an exception was thrown, check if the method is annotated with @Fallback
            // We should only get here if executing synchronously, as the exception wouldn't get thrown in this thread
            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);
            
            // If the method was annotated with Fallback, attempt it, otherwise just propagate the exception upwards
            if (fallback != null) {
                logger.log(Level.FINE, "Fallback annotation found on method - falling back from Asynchronous");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                proceededInvocationContext = fallbackPolicy.fallback(invocationContext);
            } else {
                throw ex;
            }
        }
        
        return proceededInvocationContext;
    }

    /**
     * Wrapper class for the Future object
     */
    class FutureDelegator implements Future<Object> {

        private final Future<?> future;

        public FutureDelegator(Future<?> future) {
            this.future = future;
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
            Object proceededInvocation;

            try {
                proceededInvocation = future.get();
                
                // If the result of future.get() is still a future, get it again
                if (proceededInvocation instanceof Future) {
                    Future tempFuture = (Future) proceededInvocation;
                    proceededInvocation = tempFuture.get();
                }
            } catch (InterruptedException | ExecutionException ex) {
                if (ex.getCause() instanceof FaultToleranceException) {
                    throw (FaultToleranceException) ex.getCause();
                } else {
                    throw ex;
                }
            }
            
            return proceededInvocation;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            Object proceededInvocation;

            try {
                proceededInvocation = future.get(timeout, unit);
                
                // If the result of future.get() is still a future, get it again
                if (proceededInvocation instanceof Future) {
                    Future tempFuture = (Future) proceededInvocation;
                    proceededInvocation = tempFuture.get(timeout, unit);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                if (ex.getCause() instanceof FaultToleranceException) {
                    throw (FaultToleranceException) ex.getCause();
                } else {
                    throw ex;
                }
            }
            
            return proceededInvocation;
        }
    }
}
