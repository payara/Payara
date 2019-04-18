package fish.payara.microprofile.faulttolerance.service;

import java.lang.annotation.Annotation;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.interceptor.InvocationContext;

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
final class FaultToleranceConfigFactory implements FaultToleranceConfig {

    private static final String NON_FALLBACK_ENABLED_PROPERTY = "MP_Fault_Tolerance_NonFallback_Enabled";
    private static final String METRICS_ENABLED_PROPERTY = "MP_Fault_Tolerance_Metrics_Enabled";
    private static final String INTERCEPTOR_PRIORITY_PROPERTY = "mp.fault.tolerance.interceptor.priority";

    private static final Logger logger = Logger.getLogger(FaultToleranceConfigFactory.class.getName());

    /**
     * These tree properties should only be read once at the start of the application, therefore they are cached in
     * static field.
     */
    private final AtomicReference<Boolean> nonFallbackEnabled;
    private final AtomicReference<Boolean> metricsEnabled;
    private final AtomicInteger interceptorPriority;
    private final Config config;
    private final Stereotypes sterotypes;
    private final InvocationContext context;

    public FaultToleranceConfigFactory(Stereotypes sterotypes) {
        this.sterotypes = sterotypes;
        this.config = resolveConfig();
        this.nonFallbackEnabled = new AtomicReference<>();
        this.metricsEnabled = new AtomicReference<>();
        this.interceptorPriority = new AtomicInteger(-1);
        this.context = null; // factory is unbound
    }

    private FaultToleranceConfigFactory(InvocationContext context, Stereotypes sterotypes, Config config,
            AtomicReference<Boolean> nonFallbackEnabled, AtomicReference<Boolean> metricsEnabled,
            AtomicInteger interceptorPriority) {
        this.context = context;
        this.sterotypes = sterotypes;
        this.config = config;
        this.nonFallbackEnabled = nonFallbackEnabled;
        this.metricsEnabled = metricsEnabled;
        this.interceptorPriority = interceptorPriority;
    }

    private static Config resolveConfig() {
        logger.log(Level.INFO, "Resolving Fault Tolerance Config from Provider.");
        try {
            return ConfigProvider.getConfig();
        } catch (Exception ex) {
            logger.log(Level.INFO, "No Config could be found, using annotation values only.", ex);
            return null;
        }
    }

    /*
     * Factory method
     */

    public FaultToleranceConfig bindTo(InvocationContext context) {
        return config == null
                ? FaultToleranceConfig.asAnnotated(context.getTarget().getClass(), context.getMethod())
                : new FaultToleranceConfigFactory(context, sterotypes, config, nonFallbackEnabled, metricsEnabled,
                        interceptorPriority);
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
    public int interceptorPriority() {
        return interceptorPriority.updateAndGet(priority -> priority > 0 ? priority
                        : config.getOptionalValue(INTERCEPTOR_PRIORITY_PROPERTY, Integer.class)
                                .orElse(DEFAULT_INTERCEPTOR_PRIORITY));
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
        if (classNames == null) {
            return annotationValue;
        }
        try {
            List<Class<?>> classList = new ArrayList<>();
            // Remove any curly or square brackets from the string, as well as any spaces and ".class"es
            for (String className : classNames.replaceAll("[\\{\\[ \\]\\}]", "").replaceAll("\\.class", "")
                    .split(",")) {
                classList.add(Class.forName(className));
            }
            return classList.toArray(annotationValue);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.INFO, "Could not find class from " + attributeName + " config, defaulting to annotation. "
                    + "Make sure you give the full canonical class name.", cnfe);
            return annotationValue;
        }
    }
}
