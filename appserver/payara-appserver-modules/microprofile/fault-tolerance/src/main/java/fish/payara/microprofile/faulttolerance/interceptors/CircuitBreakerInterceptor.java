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
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@CircuitBreaker
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class CircuitBreakerInterceptor implements Serializable {
    
    @Inject
    private BeanManager beanManager;
    
    @Inject
    Config config;
    
    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        try {
            proceededInvocationContext = circuitBreak(invocationContext);
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
    
    private Object circuitBreak(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        CircuitBreaker circuitBreaker = FaultToleranceCdiUtils.getAnnotation(beanManager, CircuitBreaker.class, 
                invocationContext);
        
        Class<? extends Throwable>[] failOn = (Class<? extends Throwable>[]) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class.getName(), "failOn", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(circuitBreaker.failOn());
        long delay = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class.getName(), "value", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(circuitBreaker.delay());
        ChronoUnit delayUnit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class.getName(), "delayUnit", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(circuitBreaker.delayUnit());
        int requestVolumeThreshold = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class.getName(), "requestVolumeThreshold", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(circuitBreaker.requestVolumeThreshold());
        double failureRatio = (Double) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class.getName(), "failureRatio", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(circuitBreaker.failureRatio());
        int successThreshold = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class.getName(), "successThreshold", invocationContext.getMethod().getName(), 
                invocationContext.getMethod().getDeclaringClass().getCanonicalName())
                .orElse(circuitBreaker.successThreshold());

        delay = Duration.of(delay, delayUnit).toMillis();
        
        CircuitBreakerState circuitBreakerState = faultToleranceService.getCircuitBreakerState(circuitBreaker);
        
        switch (circuitBreakerState.getCircuitState()) {
            case OPEN:
                // If open, immediately throw an error
                throw new CircuitBreakerOpenException("CircuitBreaker for method " 
                        + invocationContext.getMethod().getName() + "is in state OPEN.");
            case CLOSED:
                // If closed, attempt to proceed the invocation context
                try {
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    // Generic catch, as we want to register that the method failed somehow
                    // Add a failure result to the queue
                    circuitBreakerState.recordClosedResult(Boolean.FALSE);
                    
                    // Calculate the failure threshold
                    long failureThreshold = Math.round(requestVolumeThreshold * failureRatio);

                    // If we're over the failure threshold, open the circuit
                    if (circuitBreakerState.isOverFailureThreshold(failureThreshold)) {
                        circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.OPEN);
                        
                        // Kick off a thread that will half-open the circuit after the specified delay
                        scheduleHalfOpen(delay, circuitBreakerState);
                    }
                    
                    // Finally, propagate the error upwards
                    throw ex;
                }
                
                // If everything is bon, just add a success value
                circuitBreakerState.recordClosedResult(Boolean.TRUE);
                break;
            case HALF_OPEN:
                // If half-open, attempt to proceed the invocation context
                try {
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    // Generic catch, as we want to register that something has gone wrong
                    // Open the circuit again, and reset the half-open result counter
                    circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.OPEN);
                    circuitBreakerState.resetHalfOpenSuccessfulResultCounter();
                    scheduleHalfOpen(delay, circuitBreakerState);
                    throw ex;
                }
                
                // If the invocation context hasn't thrown an error, record a success
                circuitBreakerState.incrementHalfOpenSuccessfulResultCounter();
                
                // If we've hit the success threshold, close the circuit
                if (circuitBreakerState.getHalfOpenSuccessFulResultCounter() == successThreshold) {
                    circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.CLOSED);
                    
                    // Reset the counter for when we next need to use it
                    circuitBreakerState.resetHalfOpenSuccessfulResultCounter();

                    // We want to keep the rolling results, so fill the queue with success values
                    for (int i = 0; i < requestVolumeThreshold; i++) {
                        circuitBreakerState.recordClosedResult(Boolean.TRUE);
                    }
                }
                    
                break;
        }
        
        return proceededInvocationContext;
    }
    
    private void scheduleHalfOpen(long delay, CircuitBreakerState circuitBreakerState) {
        Runnable halfOpen = () -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Logger.getLogger(CircuitBreakerInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            }
            circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.HALF_OPEN);
        };
        
        new Thread(halfOpen).start();
    }
}
