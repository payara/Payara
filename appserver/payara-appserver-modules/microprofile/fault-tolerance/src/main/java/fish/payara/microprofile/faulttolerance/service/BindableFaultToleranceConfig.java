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

import java.lang.annotation.Annotation;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * A {@link FaultToleranceConfig} using {@link Config} to resolve overrides.
 * The {@link Config} is resolved using the {@link ConfigProvider} if needed.
 *
 * @author Jan Bernitt
 */
final class BindableFaultToleranceConfig implements FaultToleranceConfig {

    static final String NON_FALLBACK_ENABLED_PROPERTY = "MP_Fault_Tolerance_NonFallback_Enabled";
    static final String METRICS_ENABLED_PROPERTY = "MP_Fault_Tolerance_Metrics_Enabled";

    private static final Logger logger = Logger.getLogger(BindableFaultToleranceConfig.class.getName());

    /**
     * These tree properties should only be read once at the start of the application, therefore they are initialised
     * once.
     */
    private final AtomicReference<Boolean> nonFallbackEnabled;
    private final AtomicReference<Boolean> metricsEnabled;
    private final AtomicReference<Class<? extends Annotation>[]> alternativeAsynchronousAnnotations;
    private final Config config;
    private final Stereotypes sterotypes;
    private final InvocationContext context;

    public BindableFaultToleranceConfig(Stereotypes sterotypes) {
        this(resolveConfig(), sterotypes);
    }

    public BindableFaultToleranceConfig(Config config, Stereotypes sterotypes) {
        this(null, config, sterotypes, new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>());
    }

    private BindableFaultToleranceConfig(InvocationContext context, Config config, Stereotypes sterotypes,
            AtomicReference<Boolean> nonFallbackEnabled, AtomicReference<Boolean> metricsEnabled,
            AtomicReference<Class<? extends Annotation>[]> alternativeAsynchronousAnnotations) {
        this.context = context;
        this.sterotypes = sterotypes;
        this.config = config;
        this.nonFallbackEnabled = nonFallbackEnabled;
        this.metricsEnabled = metricsEnabled;
        this.alternativeAsynchronousAnnotations = alternativeAsynchronousAnnotations;
    }

    private static Config resolveConfig() {
        logger.log(Level.FINE, "Resolving Fault Tolerance Config from Provider.");
        try {
            return ConfigProvider.getConfig();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "No Config could be found, using annotation values only.", ex);
            return null;
        }
    }

    /*
     * Factory method
     */

    /**
     * Creates a {@link FaultToleranceConfig} that is bound to the given {@link InvocationContext} that is currently
     * processed.
     * 
     * Implementation note: If this configuration template has no {@link Config} available it falls back to pure
     * annotation lookup.
     * 
     * @param context the currently processed context to bind to
     * @return A {@link FaultToleranceConfig} bound to the given context
     */
    public FaultToleranceConfig bindTo(InvocationContext context) {
        return config == null
                ? FaultToleranceConfig.asAnnotated(context.getTarget().getClass(), context.getMethod())
                : new BindableFaultToleranceConfig(context, config, sterotypes, nonFallbackEnabled, metricsEnabled,
                        alternativeAsynchronousAnnotations);
    }

    /*
     * General
     */

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return FaultToleranceUtils.getAnnotation(sterotypes, annotationType, context);
    }

    @Override
    public boolean isNonFallbackEnabled() {
        if (nonFallbackEnabled.get() == null) {
            nonFallbackEnabled.compareAndSet(null,
                    config.getOptionalValue(NON_FALLBACK_ENABLED_PROPERTY, Boolean.class).orElse(true));
        }
        return nonFallbackEnabled.get().booleanValue();
    }

    @Override
    public boolean isMetricsEnabled() {
        if (metricsEnabled.get() == null) {
            metricsEnabled.compareAndSet(null,
                    config.getOptionalValue(METRICS_ENABLED_PROPERTY, Boolean.class).orElse(true));
        }
        return metricsEnabled.get().booleanValue();
    }

    @Override
    public boolean isEnabled(Class<? extends Annotation> annotationType) {
        return FaultToleranceUtils.getEnabledOverrideValue(config, annotationType, context,
                        annotationType == Fallback.class || isNonFallbackEnabled());
    }

    @Override
    public boolean isAlternativeAsynchronousAnnoationPresent() {
        if (alternativeAsynchronousAnnotations.get() == null) {
            alternativeAsynchronousAnnotations.compareAndSet(null, FaultToleranceUtils.toClassArray(
                    config.getOptionalValue(ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, String.class).orElse(null),
                    ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, NO_ALTERNATIVE_ANNOTATIONS));
        }
        for (Class<? extends Annotation> alterantiveAnnotation : alternativeAsynchronousAnnotations.get()) {
            if (isAnnotationPresent(alterantiveAnnotation)) {
                return true;
            }
        }
        return false;
    }


    /*
     * Retry
     */

    @Override
    public int maxRetries(Retry annotation) {
        return intValue(Retry.class, "maxRetries", annotation.maxRetries());
    }

    @Override
    public long delay(Retry annotation) {
        return longValue(Retry.class, "delay", annotation.delay());
    }

    @Override
    public ChronoUnit delayUnit(Retry annotation) {
        return chronoUnitValue(Retry.class, "delayUnit", annotation.delayUnit());
    }

    @Override
    public long maxDuration(Retry annotation) {
        return longValue(Retry.class, "maxDuration", annotation.maxDuration());
    }

    @Override
    public ChronoUnit durationUnit(Retry annotation) {
        return chronoUnitValue(Retry.class, "durationUnit", annotation.durationUnit());
    }

    @Override
    public long jitter(Retry annotation) {
        return longValue(Retry.class, "jitter", annotation.jitter());
    }

    @Override
    public ChronoUnit jitterDelayUnit(Retry annotation) {
        return chronoUnitValue(Retry.class, "jitterDelayUnit", annotation.jitterDelayUnit());
    }

    @Override
    public Class<? extends Throwable>[] retryOn(Retry annotation) {
        return getClassArrayValue(Retry.class, "retryOn", annotation.retryOn());
    }

    @Override
    public Class<? extends Throwable>[] abortOn(Retry annotation) {
        return getClassArrayValue(Retry.class, "abortOn", annotation.abortOn());
    }


    /*
     * Circuit-Breaker
     */

    @Override
    public Class<? extends Throwable>[] failOn(CircuitBreaker annotation) {
        return getClassArrayValue(CircuitBreaker.class, "failOn", annotation.failOn());
    }

    @Override
    public Class<? extends Throwable>[] skipOn(CircuitBreaker annotation) {
        return getClassArrayValue(CircuitBreaker.class, "skipOn", annotation.skipOn());
    }

    @Override
    public long delay(CircuitBreaker annotation) {
        return longValue(CircuitBreaker.class, "delay", annotation.delay());
    }

    @Override
    public ChronoUnit delayUnit(CircuitBreaker annotation) {
        return chronoUnitValue(CircuitBreaker.class, "delayUnit", annotation.delayUnit());
    }

    @Override
    public int requestVolumeThreshold(CircuitBreaker annotation) {
        return intValue(CircuitBreaker.class, "requestVolumeThreshold", annotation.requestVolumeThreshold());
    }

    @Override
    public double failureRatio(CircuitBreaker annotation) {
        return value(CircuitBreaker.class, "failureRatio", Double.class, annotation.failureRatio());
    }

    @Override
    public int successThreshold(CircuitBreaker annotation) {
        return intValue(CircuitBreaker.class, "successThreshold", annotation.successThreshold());
    }


    /*
     * Bulkhead
     */

    @Override
    public int value(Bulkhead annotation) {
        return intValue(Bulkhead.class, "value", annotation.value());
    }

    @Override
    public int waitingTaskQueue(Bulkhead annotation) {
        return intValue(Bulkhead.class, "waitingTaskQueue", annotation.waitingTaskQueue());
    }


    /*
     * Timeout
     */

    @Override
    public long value(Timeout annotation) {
        return longValue(Timeout.class, "value", annotation.value());
    }

    @Override
    public ChronoUnit unit(Timeout annotation) {
        return chronoUnitValue(Timeout.class, "unit", annotation.unit());
    }


    /*
     * Fallback
     */

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends FallbackHandler<?>> value(Fallback annotation) {
        String className = FaultToleranceUtils.getOverrideValue(config, Fallback.class, "value", 
                context, String.class, null);
        if (className == null) {
            return annotation.value();
        }
        try {
            return (Class<? extends FallbackHandler<?>>) Thread.currentThread().getContextClassLoader()
                    .loadClass(className);
        } catch (ClassNotFoundException e) {
            return annotation.value();
        }
    }

    @Override
    public String fallbackMethod(Fallback annotation) {
        return value(Fallback.class, "fallbackMethod", String.class, annotation.fallbackMethod());
    }

    @Override
    public Class<? extends Throwable>[] applyOn(Fallback annotation) {
        return getClassArrayValue(Fallback.class, "applyOn", annotation.applyOn());
    }

    @Override
    public Class<? extends Throwable>[] skipOn(Fallback annotation) {
        return getClassArrayValue(Fallback.class, "skipOn", annotation.skipOn());
    }


    /*
     * Helpers
     */

    private long longValue(Class<? extends Annotation> annotationType, String attribute,
            long annotationValue) {
        return value(annotationType, attribute, Long.class, annotationValue);
    }

    private int intValue(Class<? extends Annotation> annotationType, String attribute,
            int annotationValue) {
        return value(annotationType, attribute, Integer.class, annotationValue);
    }

    private ChronoUnit chronoUnitValue(Class<? extends Annotation> annotationType, String attribute,
            ChronoUnit annotationValue) {
        return value(annotationType, attribute, ChronoUnit.class, annotationValue);
    }

    private <T> T value(Class<? extends Annotation> annotationType, String attribute, Class<T> valueType,
            T annotationValue) {
        return FaultToleranceUtils.getOverrideValue(config, annotationType, attribute, context, valueType, annotationValue);
    }

    private Class<? extends Throwable>[] getClassArrayValue(Class<? extends Annotation> annotationType,
            String attributeName, Class<? extends Throwable>[] annotationValue) {
        String classNames = FaultToleranceUtils.getOverrideValue(config, annotationType, attributeName, context,
                String.class, null);
        return FaultToleranceUtils.toClassArray(classNames, attributeName, annotationValue);
    }
}
