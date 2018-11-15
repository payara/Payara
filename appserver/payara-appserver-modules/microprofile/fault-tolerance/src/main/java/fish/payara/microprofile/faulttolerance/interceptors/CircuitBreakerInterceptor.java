/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2017-2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Pielage
 */
@Interceptor
@CircuitBreaker
@Priority(Interceptor.Priority.PLATFORM_AFTER + 15)
public class CircuitBreakerInterceptor implements Serializable {

    private static final Logger logger = Logger.getLogger(CircuitBreakerInterceptor.class.getName());

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Object proceededInvocationContext = null;

        FaultToleranceService faultToleranceService =
                Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);

        MetricRegistry metricRegistry = CDI.current().select(MetricRegistry.class).get();
        String fullMethodSignature = FaultToleranceCdiUtils.getFullAnnotatedMethodSignature(invocationContext,
                CircuitBreaker.class);

        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }

        // Attempt to proceed the invocation with CircuitBreaker semantics if Fault Tolerance is enabled for this method
        try {
            String appName = faultToleranceService.getApplicationName(invocationManager, invocationContext);

            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled
            if (faultToleranceService.isFaultToleranceEnabled(appName, config)
                    && ((Boolean) FaultToleranceCdiUtils.getEnabledOverrideValue(
                            config, CircuitBreaker.class, invocationContext)
                            .orElse(Boolean.TRUE))) {
                // Only increment the invocations metric if the Retry and Bulkhead annotations aren't present
                if (FaultToleranceCdiUtils.getAnnotation(beanManager, Bulkhead.class, invocationContext) == null
                        && FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext) == null) {
                    faultToleranceService.incrementCounterMetric(metricRegistry,
                            "ft." + fullMethodSignature + ".invocations.total", appName, config);
                }

                logger.log(Level.FINER, "Proceeding invocation with circuitbreaker semantics");
                proceededInvocationContext = circuitBreak(invocationContext);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled for {0}, proceeding normally without "
                        + "circuitbreaker.", faultToleranceService.getApplicationName(invocationManager,
                                invocationContext));
                proceededInvocationContext = invocationContext.proceed();
            }
        } catch (Exception ex) {
            Retry retry = FaultToleranceCdiUtils.getAnnotation(beanManager, Retry.class, invocationContext);

            if (retry != null) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            } else {
                Fallback fallback = FaultToleranceCdiUtils.getAnnotation(beanManager, Fallback.class, invocationContext);

                // Only fall back if the annotation hasn't been disabled
                if (fallback != null && ((Boolean) FaultToleranceCdiUtils.getEnabledOverrideValue(
                        config, Fallback.class, invocationContext)
                        .orElse(Boolean.TRUE))) {
                    logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                            + "falling back from CircuitBreaker");
                    FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, config, invocationContext);
                    proceededInvocationContext = fallbackPolicy.fallback(invocationContext, ex);
                } else {
                    // Increment the failure counter metric
                    faultToleranceService.incrementCounterMetric(metricRegistry,
                            "ft." + fullMethodSignature + ".invocations.failed.total",
                            faultToleranceService.getApplicationName(invocationManager, invocationContext),
                            config);

                    throw ex;
                }
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

        MetricRegistry metricRegistry = CDI.current().select(MetricRegistry.class).get();
        String fullMethodSignature = FaultToleranceCdiUtils.getFullAnnotatedMethodSignature(invocationContext,
                CircuitBreaker.class);

        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }

        String appName = faultToleranceService.getApplicationName(
                Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class),
                invocationContext);

        Class<? extends Throwable>[] failOn = circuitBreaker.failOn();
        try {
            Optional optionalFailOn = FaultToleranceCdiUtils.getOverrideValue(
                    config, Retry.class, "failOn", invocationContext, String.class);
            if (optionalFailOn.isPresent()) {
                String failOnString = (String) optionalFailOn.get();
                List<Class> classList = new ArrayList<>();
                // Remove any curly or square brackets from the string, as well as any spaces and ".class"es and loop
                for (String className : failOnString.replaceAll("[\\{\\[ \\]\\}]", "")
                        .replaceAll("\\.class", "").split(",")) {
                    // Get a class object
                    classList.add(Class.forName(className));
                }
                failOn = classList.toArray(failOn);
            }
        } catch (NoSuchElementException nsee) {
            logger.log(Level.FINER, "Could not find element in config", nsee);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.INFO, "Could not find class from failOn config, defaulting to annotation. "
                        + "Make sure you give the full canonical class name.", cnfe);
        }

        long delay = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "delay", invocationContext, Long.class)
                .orElse(circuitBreaker.delay());
        // Look for a String and cast to ChronoUnit - Use the Common Sense Convertor
        ChronoUnit delayUnit = (ChronoUnit) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "delayUnit", invocationContext, String.class)
                .orElse(circuitBreaker.delayUnit());
        int requestVolumeThreshold = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "requestVolumeThreshold", invocationContext, Integer.class)
                .orElse(circuitBreaker.requestVolumeThreshold());
        double failureRatio = (Double) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "failureRatio", invocationContext, Double.class)
                .orElse(circuitBreaker.failureRatio());
        int successThreshold = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "successThreshold", invocationContext, Integer.class)
                .orElse(circuitBreaker.successThreshold());

        long delayMillis = Duration.of(delay, delayUnit).toMillis();

        CircuitBreakerState circuitBreakerState = faultToleranceService.getCircuitBreakerState(appName,
                invocationContext.getTarget(),
                invocationContext.getMethod(), circuitBreaker);

        if (faultToleranceService.areFaultToleranceMetricsEnabled(appName, config)) {
            Gauge<Long> openTimeGauge = metricRegistry.getGauges()
                    .get("ft." + fullMethodSignature + ".circuitbreaker.open.total");

            Gauge<Long> halfOpenTimeGauge = metricRegistry.getGauges()
                    .get("ft." + fullMethodSignature + ".circuitbreaker.halfOpen.total");

            Gauge<Long> closedTimeGauge = metricRegistry.getGauges()
                    .get("ft." + fullMethodSignature + ".circuitbreaker.closed.total");

            registerGaugesIfNecessary(openTimeGauge, halfOpenTimeGauge, closedTimeGauge, metricRegistry, circuitBreakerState,
                    fullMethodSignature);
        }


        switch (circuitBreakerState.getCircuitState()) {
            case OPEN:
                logger.log(Level.FINER, "CircuitBreaker is Open, throwing exception");

                faultToleranceService.incrementCounterMetric(metricRegistry,
                        "ft." + fullMethodSignature + ".circuitbreaker.callsPrevented.total", appName, config);

                // If open, immediately throw an error
                throw new CircuitBreakerOpenException("CircuitBreaker for method "
                        + invocationContext.getMethod().getName() + " is in state OPEN.");
            case CLOSED:
                // If closed, attempt to proceed the invocation context
                try {
                    logger.log(Level.FINER, "Proceeding CircuitBreaker context");
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception executing CircuitBreaker context");

                    // Check if the exception is something that should record a failure
                    if (shouldFail(failOn, ex)) {
                        logger.log(Level.FINE, "Caught exception is included in CircuitBreaker failOn, "
                                + "recording failure against CircuitBreaker");
                        // Add a failure result to the queue
                        circuitBreakerState.recordClosedResult(Boolean.FALSE);

                        faultToleranceService.incrementCounterMetric(metricRegistry,
                                "ft." + fullMethodSignature + ".circuitbreaker.callsFailed.total", appName, config);

                        // Calculate the failure ratio, and if we're over it, open the circuit
                        breakCircuitIfRequired(
                                Math.round(requestVolumeThreshold * failureRatio),
                                circuitBreakerState, delayMillis, metricRegistry, fullMethodSignature,
                                faultToleranceService, appName, config);
                    }

                    // Finally, propagate the error upwards
                    throw ex;
                }

                // If everything is bon, add a success value
                circuitBreakerState.recordClosedResult(Boolean.TRUE);
                faultToleranceService.incrementCounterMetric(metricRegistry,
                        "ft." + fullMethodSignature + ".circuitbreaker.callsSucceeded.total", appName, config);

                // Calculate the failure ratio, and if we're over it, open the circuit
                breakCircuitIfRequired(
                        Math.round(requestVolumeThreshold * failureRatio),
                        circuitBreakerState, delayMillis, metricRegistry, fullMethodSignature,
                        faultToleranceService, appName, config);
                break;
            case HALF_OPEN:
                // If half-open, attempt to proceed the invocation context
                try {
                    logger.log(Level.FINER, "Proceeding half open CircuitBreaker context");
                    proceededInvocationContext = invocationContext.proceed();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Exception executing CircuitBreaker context");

                    // Check if the exception is something that should record a failure
                    if (shouldFail(failOn, ex)) {
                        logger.log(Level.FINE, "Caught exception is included in CircuitBreaker failOn, "
                                + "reopening half open circuit");

                        faultToleranceService.incrementCounterMetric(metricRegistry,
                                "ft." + fullMethodSignature + ".circuitbreaker.callsFailed.total", appName, config);

                        // Open the circuit again, and reset the half-open result counter
                        circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.OPEN);
                        circuitBreakerState.resetHalfOpenSuccessfulResultCounter();
                        scheduleHalfOpen(delayMillis, circuitBreakerState);
                    }

                    throw ex;
                }

                // If the invocation context hasn't thrown an error, record a success
                circuitBreakerState.incrementHalfOpenSuccessfulResultCounter();
                faultToleranceService.incrementCounterMetric(metricRegistry,
                        "ft." + fullMethodSignature + ".circuitbreaker.callsSucceeded.total", appName, config);

                logger.log(Level.FINER, "Number of consecutive successful circuitbreaker executions = {0}",
                        circuitBreakerState.getHalfOpenSuccessFulResultCounter());

                // If we've hit the success threshold, close the circuit
                if (circuitBreakerState.getHalfOpenSuccessFulResultCounter() == successThreshold) {
                    logger.log(Level.FINE, "Number of consecutive successful CircuitBreaker executions is above "
                            + "threshold {0}, closing circuit", successThreshold);

                    circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.CLOSED);

                    // Reset the counter for when we next need to use it
                    circuitBreakerState.resetHalfOpenSuccessfulResultCounter();

                    // Reset the rolling results window
                    circuitBreakerState.resetResults();
                }

                break;
        }

        return proceededInvocationContext;
    }

    /**
     * Helper method that schedules the CircuitBreaker state to be set to HalfOpen after the configured delay
     * @param delayMillis The number of milliseconds to wait before setting the state
     * @param circuitBreakerState The CircuitBreakerState to set the state of
     * @throws NamingException If the ManagedScheduledExecutor couldn't be found
     */
    private void scheduleHalfOpen(long delayMillis, CircuitBreakerState circuitBreakerState) throws NamingException {
        Runnable halfOpen = () -> {
            circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.HALF_OPEN);
            logger.log(Level.FINE, "Setting CircuitBreaker state to half open");
        };

        FaultToleranceService faultToleranceService = Globals.getDefaultBaseServiceLocator()
                .getService(FaultToleranceService.class);
        ManagedScheduledExecutorService managedScheduledExecutorService = faultToleranceService.
                getManagedScheduledExecutorService();

        managedScheduledExecutorService.schedule(halfOpen, delayMillis, TimeUnit.MILLISECONDS);
        logger.log(Level.FINER, "CircuitBreaker half open state scheduled in {0} milliseconds", delayMillis);
    }

    /**
     * Helper method that checks whether or not the given exception is included in the failOn parameter.
     * @param failOn The array to check for the exception in.
     * @param ex The exception to check
     * @return True if the exception is included in the array
     */
    private boolean shouldFail(Class<? extends Throwable>[] failOn, Exception ex) {
        boolean shouldFail = false;

        if (failOn[0] != Throwable.class) {
            for (Class<? extends Throwable> failureClass : failOn) {
                if (ex.getClass() == failureClass) {
                    logger.log(Level.FINER, "Exception {0} matches a Throwable in failOn",
                            ex.getClass().getSimpleName());
                    shouldFail = true;
                    break;
                } else {
                    try {
                        // If we there isn't a direct match, check if the exception is a subclass
                        ex.getClass().asSubclass(failureClass);
                        shouldFail = true;

                        logger.log(Level.FINER, "Exception {0} is a child of a Throwable in retryOn: {1}",
                                new String[]{ex.getClass().getSimpleName(), failureClass.getSimpleName()});
                        break;
                    } catch (ClassCastException cce) {
                        // Om nom nom
                    }
                }
            }
        } else {
            shouldFail = true;
        }

        return shouldFail;
    }

    private void breakCircuitIfRequired(long failureThreshold, CircuitBreakerState circuitBreakerState,
            long delayMillis, MetricRegistry metricRegistry, String fullMethodSignature,
            FaultToleranceService faultToleranceService, String appName, Config config) throws NamingException {
        // If we're over the failure threshold, open the circuit
        if (circuitBreakerState.isOverFailureThreshold(failureThreshold)) {
            logger.log(Level.FINE, "CircuitBreaker is over failure threshold {0}, opening circuit",
                    failureThreshold);

            // Update the circuit state and metric timers
            circuitBreakerState.setCircuitState(CircuitBreakerState.CircuitState.OPEN);

            // Update the opened metric counter
            faultToleranceService.incrementCounterMetric(metricRegistry,
                    "ft." + fullMethodSignature + ".circuitbreaker.opened.total", appName, config);

            // Kick off a thread that will half-open the circuit after the specified delay
            scheduleHalfOpen(delayMillis, circuitBreakerState);
        }
    }

    private void registerGaugesIfNecessary(Gauge openTimeGauge, Gauge halfOpenTimeGauge, Gauge closedTimeGauge,
            MetricRegistry metricRegistry, CircuitBreakerState circuitBreakerState, String fullMethodSignature) {

        // Register a open time gauge if there isn't one
        if (openTimeGauge == null) {
            openTimeGauge = () -> circuitBreakerState.updateAndGet(CircuitBreakerState.CircuitState.OPEN);

            metricRegistry.register("ft." + fullMethodSignature + ".circuitbreaker.open.total", openTimeGauge);
        }

        // Register a open time gauge if there isn't one
        if (halfOpenTimeGauge == null) {
            halfOpenTimeGauge = () -> circuitBreakerState.updateAndGet(CircuitBreakerState.CircuitState.HALF_OPEN);

            metricRegistry.register("ft." + fullMethodSignature + ".circuitbreaker.halfOpen.total", halfOpenTimeGauge);
        }

        // Register a open time gauge if there isn't one
        if (closedTimeGauge == null) {
            closedTimeGauge = () -> circuitBreakerState.updateAndGet(CircuitBreakerState.CircuitState.CLOSED);

            metricRegistry.register("ft." + fullMethodSignature + ".circuitbreaker.closed.total", closedTimeGauge);
        }
    }
}
