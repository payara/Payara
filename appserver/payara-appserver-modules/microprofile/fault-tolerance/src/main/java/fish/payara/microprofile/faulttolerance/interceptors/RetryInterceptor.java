/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.interceptors;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import fish.payara.notification.requesttracing.RequestTraceSpan;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@Retry
@Priority(Interceptor.Priority.PLATFORM_AFTER + 5)
public class RetryInterceptor {
    
    private static final Logger logger = Logger.getLogger(RetryInterceptor.class.getName());
    
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
                logger.log(Level.FINER, "Proceeding invocation with retry semantics");
                proceededInvocationContext = retry(invocationContext);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled for {0}, proceeding normally without retry.", 
                        faultToleranceService.getApplicationName(invocationManager, invocationContext));
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);
            
            if (fallback != null) {
                logger.log(Level.FINE, "Fallback annotation found on method - falling back from Retry");
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                proceededInvocationContext = fallbackPolicy.fallback(invocationContext);
            } else {
                throw ex;
            }
        }
        
        return proceededInvocationContext;
    }
    
    /**
     * Proceeds the given invocation context with Retry semantics.
     * @param invocationContext The invocation context to proceed
     * @return The proceeded invocation context
     * @throws Exception If the invocation throws an exception that shouldn't be retried, or if all retry attempts are 
     * expended
     */
    private Object retry(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;
        Retry retry = FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext);
        
        FaultToleranceService faultToleranceService = 
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        
        try {
            proceededInvocationContext = invocationContext.proceed();
        } catch (Exception ex) {
            Config config = null;
            try {
                config = ConfigProvider.getConfig();
            } catch (IllegalArgumentException iae) {
                logger.log(Level.INFO, "No config could be found", ex);
            }

            Class<? extends Throwable>[] retryOn = retry.retryOn();
            try {
                String retryOnString = ((String) FaultToleranceCdiUtils.getOverrideValue(
                        config, Retry.class, "retryOn", invocationContext, String.class).get());
                
                List<Class> classList = new ArrayList<>();
            
                // Remove any curly or square brackets from the string, as well as any spaces and ".class"es
                for (String className : retryOnString.replaceAll("[\\{\\[ \\]\\}]", "")
                        .replaceAll("\\.class", "").split(",")) {
                    classList.add(Class.forName(className));
                }
                
                retryOn = classList.toArray(retryOn);
            } catch (NoSuchElementException nsee) {
                logger.log(Level.FINER, "Could not find element in config", nsee);
            } catch (ClassNotFoundException cnfe) {
                logger.log(Level.INFO, "Could not find class from retryOn config, defaulting to annotation. "
                        + "Make sure you give the full canonical class name.", cnfe);
            }
            
            Class<? extends Throwable>[] abortOn = retry.abortOn();
            try {
                String abortOnString = (String) FaultToleranceCdiUtils.getOverrideValue(
                        config, Retry.class, "abortOn", invocationContext, String.class).get();

                List<Class> classList = new ArrayList<>();

                // Remove any curly or square brackets from the string, as well as any spaces and ".class"es
                for (String className : abortOnString.replaceAll("[\\{\\[ \\]\\}]", "")
                        .replaceAll("\\.class", "").split(",")) {
                    classList.add(Class.forName(className));
                }
                
                abortOn = classList.toArray(abortOn);
            } catch (NoSuchElementException nsee) {
                logger.log(Level.FINER, "Could not find element in config", nsee);
            } catch (ClassNotFoundException cnfe) {
                logger.log(Level.INFO, "Could not find class from abortOn config, defaulting to annotation. "
                        + "Make sure you give the full canonical class name.", cnfe);
            }

            if (!shouldRetry(retryOn, abortOn, ex)) {
                logger.log(Level.FINE, "Exception is contained in retryOn or abortOn, not retrying.", ex);
                throw ex;
            }
            
            int maxRetries = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "maxRetries", invocationContext, Integer.class)
                .orElse(retry.maxRetries());
            long delay = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "delay", invocationContext, Long.class)
                .orElse(retry.delay());
            ChronoUnit delayUnit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "delayUnit", invocationContext, ChronoUnit.class)
                .orElse(retry.delayUnit());
            long maxDuration = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "maxDuration", invocationContext, Long.class)
                .orElse(retry.maxDuration());
            ChronoUnit durationUnit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "durationUnit", invocationContext, ChronoUnit.class)
                .orElse(retry.durationUnit());
            long jitter = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "jitter", invocationContext, Long.class)
                .orElse(retry.jitter());
            ChronoUnit jitterDelayUnit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "jitterDelayUnit", invocationContext, ChronoUnit.class)
                .orElse(retry.jitterDelayUnit());
            
            long delayMillis = Duration.of(delay, delayUnit).toMillis();
            long jitterMillis = Duration.of(jitter, jitterDelayUnit).toMillis();
            long timeoutTime = System.currentTimeMillis() + Duration.of(maxDuration, durationUnit).toMillis();
            
            Exception retryException = ex;
            
            faultToleranceService.startFaultToleranceSpan(new RequestTraceSpan("retryMethod"), invocationManager,
                    invocationContext);
            try {
                if (maxRetries == -1 && maxDuration > 0) {
                    logger.log(Level.FINER, "Retrying until maxDuration is breached.");

                    while (System.currentTimeMillis() < timeoutTime) {
                        try {
                            proceededInvocationContext = invocationContext.proceed();
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                faultToleranceService.startFaultToleranceSpan(new RequestTraceSpan("delayRetry"), 
                                        invocationManager, invocationContext);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    faultToleranceService.endFaultToleranceSpan();
                                }
                            }
                        }
                    }
                } else if (maxRetries == -1 && maxDuration == 0) {
                    logger.log(Level.INFO, "Retrying potentially forever!");
                    while (true) {
                        try {
                            proceededInvocationContext = invocationContext.proceed();
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                faultToleranceService.startFaultToleranceSpan(new RequestTraceSpan("delayRetry"), 
                                        invocationManager, invocationContext);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    faultToleranceService.endFaultToleranceSpan();
                                }
                            }
                        }
                    }
                } else if (maxRetries != -1 && maxDuration > 0) {
                    logger.log(Level.INFO, 
                            "Retrying as long as maxDuration ({0}ms) isn''t breached, and no more than {1} times", 
                            new Object[]{Duration.of(maxDuration, durationUnit).toMillis(), maxRetries});
                    while (maxRetries > 0 && System.currentTimeMillis() < timeoutTime) {
                        try {
                            proceededInvocationContext = invocationContext.proceed();
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                faultToleranceService.startFaultToleranceSpan(new RequestTraceSpan("delayRetry"), 
                                        invocationManager, invocationContext);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    faultToleranceService.endFaultToleranceSpan();
                                }
                            }

                            maxRetries--;
                        }
                    }
                } else {
                    logger.log(Level.INFO, "Retrying no more than {0} times", maxRetries);
                    while (maxRetries > 0) {
                        try {
                            proceededInvocationContext = invocationContext.proceed();
                            break;
                        } catch (Exception caughtException) {
                            retryException = caughtException;
                            if (!shouldRetry(retryOn, abortOn, caughtException)) {
                                break;
                            }

                            if (delayMillis > 0 || jitterMillis > 0) {
                                faultToleranceService.startFaultToleranceSpan(new RequestTraceSpan("delayRetry"), 
                                        invocationManager, invocationContext);
                                try {
                                    Thread.sleep(delayMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis));
                                } finally {
                                    faultToleranceService.endFaultToleranceSpan();
                                }
                            }

                            maxRetries--;
                        }
                    }
                }
            } finally {
                faultToleranceService.endFaultToleranceSpan();
            }

            if (proceededInvocationContext == null) {
                throw retryException;
            }
        }
        
        return proceededInvocationContext;
    }
    
    /**
     * Helper method that determines whether or not a retry should be attempted for the given exception.
     * @param retryOn The exceptions to retry on.
     * @param abortOn The exceptions to abort on.
     * @param ex The caught exception
     * @return True if retry should be attempted.
     */
    private boolean shouldRetry(Class<? extends Throwable>[] retryOn, Class<? extends Throwable>[] abortOn, 
            Exception ex) {
        boolean shouldRetry = false;
            
        // If the first value in the array is just "Exception", just set retry to true, otherwise check the exceptions
        if (retryOn[0] != Exception.class) {
            for (Class<? extends Throwable> throwable : retryOn) {
                if (ex.getClass() == throwable) {
                    logger.log(Level.FINER, "Exception {0} matches a Throwable in retryOn", 
                            ex.getClass().getSimpleName());
                    shouldRetry = true;
                    break;
                }  else {
                    try {
                        // If we there isn't a direct match, check if the exception is a subclass
                        ex.getClass().asSubclass(throwable);
                        shouldRetry = true;
                        
                        logger.log(Level.FINER, "Exception {0} is a child of a Throwable in retryOn: {1}", 
                                new String[]{ex.getClass().getSimpleName(), throwable.getSimpleName()});
                        break;
                    } catch (ClassCastException cce) {
                        // Om nom nom
                    }
                }
            }
        } else {
            shouldRetry = true;
        }

        // If we should retry, check if the exception is one we shoukd abort on
        if (shouldRetry && abortOn != null) {
            for (Class<? extends Throwable> throwable : abortOn) {
                if (ex.getClass() == throwable) {
                    logger.log(Level.FINER, "Exception {0} matches a Throwable in abortOn", 
                            ex.getClass().getSimpleName());
                    shouldRetry = false;
                    break;
                } else {
                    try {
                        // If we there isn't a direct match, check if the exception is a subclass
                        ex.getClass().asSubclass(throwable);
                        shouldRetry = false;
                        
                        logger.log(Level.FINER, "Exception {0} is a child of a Throwable in abortOn: {1}", 
                                new String[]{ex.getClass().getSimpleName(), throwable.getSimpleName()});
                        break;
                    } catch (ClassCastException cce) {
                        // Om nom nom
                    }
                }
            }
        }

        return shouldRetry;
    }
}
