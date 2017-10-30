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

import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.glassfish.internal.api.Globals;

/**
 * Interceptor for the Fault Tolerance Bulkhead Annotation.
 * 
 * @author Andrew Pielage
 */
@Interceptor
@Bulkhead
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class BulkheadInterceptor implements Serializable {
    
    @Inject
    private BeanManager beanManager;
    
    @Inject
    Config config;
    
    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        // Attempt to proceed the InvocationContext with Bulkhead semantics
        try {
            proceededInvocationContext = bulkhead(invocationContext);
        } catch (Exception ex) {
            // If an exception was thrown, check if the method is annotated with @Fallback
            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);
            
            // If the method was annotated with Fallback, attempt it, otherwise just propagate the exception upwards
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
    
    
    private Object bulkhead(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        
        Bulkhead bulkhead = FaultToleranceCdiUtils.getAnnotation(beanManager, Bulkhead.class, invocationContext);
        
        int value = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, Bulkhead.class.getName(), "value", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(bulkhead.value());
        int waitingTaskQueue = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, Bulkhead.class.getName(), "waitingTaskQueue", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(bulkhead.waitingTaskQueue());
        
        Semaphore bulkheadExecutionSemaphore = faultToleranceService.getBulkheadExecutionSemaphore(bulkhead, value);
        
        // If the Asynchronous annotation is present, use threadpool style, otherwise use semaphore style
        if (FaultToleranceCdiUtils.getAnnotation(beanManager, Asynchronous.class, invocationContext) != null) {
            Semaphore bulkheadExecutionQueueSemaphore = 
                    faultToleranceService.getBulkheadExecutionQueueSemaphore(bulkhead, waitingTaskQueue);
            
            // Check if there are any free permits for concurrent execution
            if (!bulkheadExecutionSemaphore.tryAcquire()) {
                // If there aren't any free permits, see if there are any free queue permits
                if (bulkheadExecutionQueueSemaphore.tryAcquire()) {
                    // If there is a free queue permit, queue for an executor permit
                    bulkheadExecutionSemaphore.acquire();
                    
                    // Proceed the invocation and wait for the response
                    try {
                        proceededInvocationContext = invocationContext.proceed();
                    } catch (Exception ex) {
                        // Generic catch, as we need to release the semaphore permits
                        bulkheadExecutionSemaphore.release();
                        bulkheadExecutionQueueSemaphore.release();
                        
                        // Let the exception propagate further up - we just want to release the semaphores
                        throw ex;
                    }
                    
                    // Release both permits
                    bulkheadExecutionSemaphore.release();
                    bulkheadExecutionQueueSemaphore.release();
                } else {
                    throw new BulkheadException("No free work or queue permits.");
                }
            } else {
                // Proceed the invocation and wait for the response
                    try {
                        proceededInvocationContext = invocationContext.proceed();
                    } catch (Exception ex) {
                        // Generic catch, as we need to release the semaphore permits
                        bulkheadExecutionSemaphore.release();
                        
                        // Let the exception propagate further up - we just want to release the semaphores
                        throw ex;
                    }
                    
                // Release the permit
                bulkheadExecutionSemaphore.release();
            }
        } else {
            // Try to get an execution permit
            if (bulkheadExecutionSemaphore.tryAcquire()) {
                // Proceed the invocation and wait for the response
                try {
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    // Generic catch, as we need to release the semaphore permits
                    bulkheadExecutionSemaphore.release();

                    // Let the exception propagate further up - we just want to release the semaphores
                    throw ex;
                }
                
                // Release the permit
                bulkheadExecutionSemaphore.release();
            } else {
                throw new BulkheadException("No free work permits.");
            }
        }
        
        return proceededInvocationContext;
    }
}
