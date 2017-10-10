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
import java.io.Serializable;
import java.util.concurrent.Semaphore;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Bulkhead
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class BulkheadInterceptor implements Serializable {
    
    @AroundInvoke
    public Object bulkhead(InvocationContext invocationContext) throws Exception {
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        
        Bulkhead bulkhead = invocationContext.getMethod().getAnnotation(Bulkhead.class);
        Semaphore bulkheadExecutionSemaphore = faultToleranceService.getBulkheadExecutionSemaphore(bulkhead);
        
        Object proceededInvocationContext = null;
        
        if (invocationContext.getMethod().getAnnotation(Asynchronous.class) != null) {
            Semaphore bulkheadExecutionQueueSemaphore = 
                    faultToleranceService.getBulkheadExecutionQueueSemaphore(bulkhead);
            
            // Check if there are any free permits for concurrent execution
            if (!bulkheadExecutionSemaphore.tryAcquire()) {
                // If there aren't any free permits, see if there are any free queue permits
                if (bulkheadExecutionQueueSemaphore.tryAcquire()) {
                    // If there is a free queue permit, queue for an executor permit
                    bulkheadExecutionSemaphore.acquire();
                    
                    // Proceed the invocation and wait for the response
                    proceededInvocationContext = invocationContext.proceed();
                    
                    // Release both permits
                    bulkheadExecutionSemaphore.release();
                    bulkheadExecutionQueueSemaphore.release();
                } else {
                    throw new BulkheadException();
                }
            } else {
                // Proceed the invocation and wait for the response
                proceededInvocationContext = invocationContext.proceed();
                    
                // Release both permits
                bulkheadExecutionSemaphore.release();
                bulkheadExecutionQueueSemaphore.release();
            }
        } else {
            // Try to get an execution permit
            if (bulkheadExecutionSemaphore.tryAcquire()) {
                // Proceed the invocation and wait for the response
                proceededInvocationContext = invocationContext.proceed();
                
                // Release the permit
                bulkheadExecutionSemaphore.release();
            } else {
                throw new BulkheadException();
            }
        }

        // Check we actually have a value to return
        if (proceededInvocationContext == null) {
            throw new BulkheadException();
        }
        
        return proceededInvocationContext;
    }
}
