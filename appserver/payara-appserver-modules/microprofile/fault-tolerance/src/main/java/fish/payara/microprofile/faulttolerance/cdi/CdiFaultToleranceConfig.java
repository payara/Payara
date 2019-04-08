package fish.payara.microprofile.faulttolerance.cdi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils.Stereotypes;

/**
 * A {@link FaultToleranceConfig} using {@link Config} to resolve overrides.
 * The {@link Config} is resolved using the {@link ConfigProvider} if needed.
 *
 * @author Jan Bernitt
 */
public class CdiFaultToleranceConfig implements FaultToleranceConfig, Serializable {

    public static final String FAULT_TOLERANCE_ENABLED_PROPERTY = "MP_Fault_Tolerance_NonFallback_Enabled";
    public static final String METRICS_ENABLED_PROPERTY = "MP_Fault_Tolerance_Metrics_Enabled";

    private static final Logger logger = Logger.getLogger(CdiFaultToleranceConfig.class.getName());

    private final Stereotypes sterotypes;
    private transient Config config;

    public CdiFaultToleranceConfig(Config config, Stereotypes sterotypes) {
        this.sterotypes = sterotypes;
        this.config = config;
    }

    private Config getConfig() {
        if (config == null) {
            logger.log(Level.INFO, "Resolving Fault Tolerance Config from Provider.");
            try {
                config = ConfigProvider.getConfig();
            } catch (IllegalArgumentException ex) {
                logger.log(Level.INFO, "No config could be found", ex);
            }
        }
        return config;
    }


    /*
     * General
     */

    @Override
    public boolean isEnabled(InvocationContext context) {
        return getConfig().getOptionalValue(FAULT_TOLERANCE_ENABLED_PROPERTY, Boolean.class).orElse(true);
    }

    @Override
    public boolean isEnabled(Class<? extends Annotation> annotationType, InvocationContext context) {
        return FaultToleranceCdiUtils.getEnabledOverrideValue(getConfig(), annotationType, context).orElse(true);
    }

    @Override
    public boolean isMetricsEnabled(InvocationContext context) {
        return getConfig().getOptionalValue(METRICS_ENABLED_PROPERTY, Boolean.class).orElse(true);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, InvocationContext context) {
        return FaultToleranceCdiUtils.getAnnotation(sterotypes, annotationType, context);
    }

    /*
     * Retry
     */

    @Override
    public int maxRetries(Retry annotation, InvocationContext context) {
        return intValue(Retry.class, "maxRetries", context, annotation.maxRetries());
    }

    @Override
    public long delay(Retry annotation, InvocationContext context) {
        return longValue(Retry.class, "delay", context, annotation.delay());
    }

    @Override
    public ChronoUnit delayUnit(Retry annotation, InvocationContext context) {
        return chronoUnitValue(Retry.class, "delayUnit", context, annotation.delayUnit());
    }

    @Override
    public long maxDuration(Retry annotation, InvocationContext context) {
        return longValue(Retry.class, "maxDuration", context, annotation.maxDuration());
    }

    @Override
    public ChronoUnit durationUnit(Retry annotation, InvocationContext context) {
        return chronoUnitValue(Retry.class, "durationUnit", context, annotation.durationUnit());
    }

    @Override
    public long jitter(Retry annotation, InvocationContext context) {
        return longValue(Retry.class, "jitter", context, annotation.jitter());
    }

    @Override
    public ChronoUnit jitterDelayUnit(Retry annotation, InvocationContext context) {
        return chronoUnitValue(Retry.class, "jitterDelayUnit", context, annotation.jitterDelayUnit());
    }

    @Override
    public Class<? extends Throwable>[] retryOn(Retry annotation, InvocationContext context) {
        return getClassArrayValue(Retry.class, "retryOn", context, annotation.retryOn());
    }

    @Override
    public Class<? extends Throwable>[] abortOn(Retry annotation, InvocationContext context) {
        return getClassArrayValue(Retry.class, "abortOn", context, annotation.abortOn());
    }


    /*
     * Circuit-Breaker
     */

    @Override
    public Class<? extends Throwable>[] failOn(CircuitBreaker annotation, InvocationContext context) {
        return getClassArrayValue(CircuitBreaker.class, "failOn", context, annotation.failOn());
    }

    @Override
    public long delay(CircuitBreaker annotation, InvocationContext context) {
        return longValue(CircuitBreaker.class, "delay", context, annotation.delay());
    }

    @Override
    public ChronoUnit delayUnit(CircuitBreaker annotation, InvocationContext context) {
        return chronoUnitValue(CircuitBreaker.class, "delayUnit", context, annotation.delayUnit());
    }

    @Override
    public int requestVolumeThreshold(CircuitBreaker annotation, InvocationContext context) {
        return intValue(CircuitBreaker.class, "requestVolumeThreshold", context, annotation.requestVolumeThreshold());
    }

    @Override
    public double failureRatio(CircuitBreaker annotation, InvocationContext context) {
        return value(CircuitBreaker.class, "failureRatio", context, Double.class, annotation.failureRatio());
    }

    @Override
    public int successThreshold(CircuitBreaker annotation, InvocationContext context) {
        return intValue(CircuitBreaker.class, "successThreshold", context, annotation.successThreshold());
    }


    /*
     * Bulkhead
     */

    @Override
    public int value(Bulkhead annotation, InvocationContext context) {
        return intValue(Bulkhead.class, "value", context, annotation.value());
    }

    @Override
    public int waitingTaskQueue(Bulkhead annotation, InvocationContext context) {
        return intValue(Bulkhead.class, "waitingTaskQueue", context, annotation.waitingTaskQueue());
    }


    /*
     * Timeout
     */

    @Override
    public long value(Timeout annotation, InvocationContext context) {
        return longValue(Timeout.class, "value", context, annotation.value());
    }

    @Override
    public ChronoUnit unit(Timeout annotation, InvocationContext context) {
        return chronoUnitValue(Timeout.class, "unit", context, annotation.unit());
    }


    /*
     * Fallback
     */

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends FallbackHandler<?>> value(Fallback annotation, InvocationContext context) {
        Optional<String> className = FaultToleranceCdiUtils.getOverrideValue(getConfig(), Fallback.class, "value", 
                context, String.class);
        if (className.isPresent()) {
            try {
                return (Class<? extends FallbackHandler<?>>) Thread.currentThread().getContextClassLoader()
                        .loadClass(className.get());
            } catch (ClassNotFoundException e) {
                // fall through
            }
        }
        return annotation.value();
    }

    @Override
    public String fallbackMethod(Fallback annotation, InvocationContext context) {
        return value(Fallback.class, "fallbackMethod", context, String.class, annotation.fallbackMethod());
    }


    /*
     * Helpers
     */

    private long longValue(Class<? extends Annotation> annotationType, String attribute, InvocationContext context,
            long annotationValue) {
        return value(annotationType, attribute, context, Long.class, annotationValue);
    }

    private int intValue(Class<? extends Annotation> annotationType, String attribute, InvocationContext context,
            int annotationValue) {
        return value(annotationType, attribute, context, Integer.class, annotationValue);
    }

    private ChronoUnit chronoUnitValue(Class<? extends Annotation> annotationType, String attribute,
            InvocationContext context, ChronoUnit annotationValue) {
        return value(annotationType, attribute, context, ChronoUnit.class, annotationValue);
    }

    private <T> T value(Class<? extends Annotation> annotationType, String attribute,
            InvocationContext context, Class<T> valueType, T annotationValue) {
        return FaultToleranceCdiUtils.getOverrideValue(getConfig(), annotationType, attribute, context, valueType) 
                .orElse(annotationValue);
    }

    private Class<? extends Throwable>[] getClassArrayValue(Class<? extends Annotation> annotationType,
            String attributeName, InvocationContext context, Class<? extends Throwable>[] annotationValue) {
        try {
            Optional<String> classNames = FaultToleranceCdiUtils.getOverrideValue(
                    getConfig(), annotationType, attributeName, context, String.class);
            if (classNames.isPresent()) {
                List<Class<?>> classList = new ArrayList<>();
                // Remove any curly or square brackets from the string, as well as any spaces and ".class"es
                for (String className : classNames.get().replaceAll("[\\{\\[ \\]\\}]", "")
                        .replaceAll("\\.class", "").split(",")) {
                    classList.add(Class.forName(className));
                }
                return classList.toArray(annotationValue);
            }
        } catch (NoSuchElementException nsee) {
            logger.log(Level.FINER, "Could not find element in config", nsee);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.INFO, "Could not find class from " + attributeName + " config, defaulting to annotation. "
                    + "Make sure you give the full canonical class name.", cnfe);
        }
        return annotationValue;
    }
}
