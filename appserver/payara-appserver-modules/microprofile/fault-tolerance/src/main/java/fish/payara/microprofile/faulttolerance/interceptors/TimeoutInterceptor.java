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
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.NamingException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Timeout
@Priority(Interceptor.Priority.PLATFORM_AFTER + 20)
public class TimeoutInterceptor {
    
    private static final Logger logger = Logger.getLogger(TimeoutInterceptor.class.getName());
    
    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        
        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        try {
            if (faultToleranceService.isFaultToleranceEnabled(faultToleranceService.getApplicationName(
                    invocationManager, invocationContext), config)) {
                logger.log(Level.FINER, "Proceeding invocation with timeout semantics");
                proceededInvocationContext = timeout(invocationContext);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled for {0}, proceeding normally without timeout.", 
                        faultToleranceService.getApplicationName(invocationManager, invocationContext));
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            Retry retry = FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext);
            
            if (retry != null) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            } else {
                Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, 
                        invocationContext);

                if (fallback != null) {
                    logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                            + "falling back from Timeout");
                    FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                    proceededInvocationContext = fallbackPolicy.fallback(invocationContext);
                } else {
                    throw ex;
                }
            }
        }
        
        return proceededInvocationContext;
    }

    /**
     * Proceeds the given invocation context with Timeout semantics.
     * @param invocationContext The invocation context to proceed.
     * @return The result of the invocation context.
     * @throws Exception If the invocation context execution throws an exception
     */
    private Object timeout(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        Timeout timeout = FaultToleranceCdiUtils.getAnnotation(beanManager, Timeout.class, invocationContext);
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        
        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        long value = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Timeout.class, "value", invocationContext, Long.class)
                .orElse(timeout.value());
        ChronoUnit unit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, Timeout.class, "unit", invocationContext, ChronoUnit.class)
                .orElse(timeout.unit());

        Future timeoutFuture = null;
        ThreadLocal<Boolean> timedOut = new ThreadLocal<>();
        timedOut.set(false);
        long timeoutMillis = Duration.of(value, unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutMillis;
        
        try {
            timeoutFuture = startTimeout(timeoutMillis, timedOut);
            proceededInvocationContext = invocationContext.proceed();
            stopTimeout(timeoutFuture);

            if (System.currentTimeMillis() > timeoutTime || timedOut.get()) {
                logger.log(Level.FINE, "Execution timed out");
                RequestEvent requestEvent = new RequestEvent("FaultTolerance-TimeoutException");
                faultToleranceService.traceFaultToleranceEvent(requestEvent, invocationManager, invocationContext);
                throw new TimeoutException();
            }
        } catch (Exception ex) {
            stopTimeout(timeoutFuture);
            RequestEvent requestEvent = new RequestEvent("FaultTolerance-TimeoutException");
            faultToleranceService.traceFaultToleranceEvent(requestEvent, invocationManager, invocationContext);
            throw ex;
        }
        
        return proceededInvocationContext;
    }

    /**
     * Helper method that schedules a thread interrupt after a period of time.
     * @param timeoutMillis The time in milliseconds to wait before interrupting.
     * @param timedOut A threadlocal that stores whether or not the interrupt has occurred.
     * @return A future that can be cancelled if the method execution completes before the interrupt happens
     * @throws NamingException If the configured ManagedScheduledExecutorService could not be found
     */
    private Future startTimeout(long timeoutMillis, ThreadLocal<Boolean> timedOut) throws NamingException {
        final Thread thread = Thread.currentThread();
        
        Runnable timeoutTask = () -> {
            thread.interrupt();
            timedOut.set(true);
        };

        FaultToleranceService faultToleranceService = Globals.getDefaultBaseServiceLocator()
                .getService(FaultToleranceService.class);
        ManagedScheduledExecutorService managedScheduledExecutorService = faultToleranceService.
                getManagedScheduledExecutorService();

        return managedScheduledExecutorService.schedule(timeoutTask, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Helper method that stops the scheduled interrupt.
     * @param timeoutFuture The scheduled interrupt to cancel.
     */
    private void stopTimeout(Future timeoutFuture) {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
        }
    }
}
