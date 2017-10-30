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
import static fish.payara.microprofile.faulttolerance.FaultToleranceService.FAULT_TOLERANCE_ENABLED_PROPERTY;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.NamingException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Timeout
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class TimeoutInterceptor {
    
    private Future currentTimeout;
    
    @Inject
    private BeanManager beanManager;
    
    @Inject
    Config config;
    
    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        try {
            if (config.getValue(FAULT_TOLERANCE_ENABLED_PROPERTY, Boolean.class)) {
                proceededInvocationContext = timeout(invocationContext);
            } else {
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);
            
            if (fallback != null) {
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config,
                        invocationContext.getMethod().getName(), 
                        invocationContext.getMethod().getDeclaringClass().getCanonicalName());
                proceededInvocationContext = fallbackPolicy.fallback(invocationContext);
            } else {
                throw ex;
            }
        }
        
        return proceededInvocationContext;
    }
    
    private Object timeout(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        Timeout timeout = FaultToleranceCdiUtils.getAnnotation(beanManager, Timeout.class, invocationContext);
        
        long value = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Timeout.class.getName(), "value", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(timeout.value());
        ChronoUnit unit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, Timeout.class.getName(), "unit", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(timeout.unit());
        
        long timeoutMillis = Duration.of(value, unit).toMillis();
        long timeoutTime = System.currentTimeMillis() + timeoutMillis;
        
        try {
            startTimeout(timeoutMillis);
            proceededInvocationContext = invocationContext.proceed();
            stopTimeout();
            
            if (System.currentTimeMillis() > timeoutTime) {
                throw new TimeoutException();
            }
        } catch (Exception ex) {
            throw ex;
        }
        
        return proceededInvocationContext;
    }
    
    private void startTimeout(long timeoutMillis) throws NamingException {
        Runnable timeoutTask = () -> { Thread.currentThread().interrupt(); };
        
        FaultToleranceService faultToleranceService = Globals.getDefaultBaseServiceLocator()
                .getService(FaultToleranceService.class);
        ManagedScheduledExecutorService managedScheduledExecutorService = faultToleranceService.
                getManagedScheduledExecutorService();
        
        currentTimeout = managedScheduledExecutorService.schedule(timeoutTask, timeoutMillis, TimeUnit.MILLISECONDS);
    }
    
    private void stopTimeout() {
        currentTimeout.cancel(true);
    }
}
