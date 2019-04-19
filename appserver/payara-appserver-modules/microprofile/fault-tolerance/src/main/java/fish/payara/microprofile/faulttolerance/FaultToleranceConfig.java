package fish.payara.microprofile.faulttolerance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Encapsulates all properties extracted from FT annotations and the {@link org.eclipse.microprofile.config.Config} so
 * that the processing can be declared independent of the actual resolution mechanism.
 * 
 * The default implementations provided will extract properties plain from the given annotations.
 * 
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface FaultToleranceConfig {

    /**
     * FT behaves as stated by the present FT annotations.
     */
    static FaultToleranceConfig asAnnotated(Class<?> target, Method method) {
        return new FaultToleranceConfig() {
            @Override
            public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
                A annotation = method.getAnnotation(annotationType);
                return annotation != null ? annotation : target.getAnnotation(annotationType);
            }
        };
    }

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    /*
     * General
     */

    default boolean isNonFallbackEnabled() {
        return true;
    }

    @SuppressWarnings("unused")
    default boolean isEnabled(Class<? extends Annotation> annotationType) {
        return true;
    }

    default boolean isMetricsEnabled() {
        return true;
    }

    default boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }


    /*
     * @Retry
     */

    default int maxRetries(Retry annotation) {
        return annotation.maxRetries();
    }

    default long delay(Retry annotation) {
        return annotation.delay();
    }

    default ChronoUnit delayUnit(Retry annotation) {
        return annotation.delayUnit();
    }

    default long maxDuration(Retry annotation) {
        return annotation.maxDuration();
    }

    default ChronoUnit durationUnit(Retry annotation) {
        return annotation.durationUnit();
    }

    default long jitter(Retry annotation) {
        return annotation.jitter();
    }

    default ChronoUnit jitterDelayUnit(Retry annotation) {
        return annotation.jitterDelayUnit();
    }

    default Class<? extends Throwable>[] retryOn(Retry annotation) {
        return annotation.retryOn();
    }

    default Class<? extends Throwable>[] abortOn(Retry annotation) {
        return annotation.abortOn();
    }


    /*
     * @CircuitBreaker
     */

    default Class<? extends Throwable>[] failOn(CircuitBreaker annotation) {
        return annotation.failOn();
    }

    default long delay(CircuitBreaker annotation) {
        return annotation.delay();
    }

    default ChronoUnit delayUnit(CircuitBreaker annotation) {
        return annotation.delayUnit();
    }

    default int requestVolumeThreshold(CircuitBreaker annotation) {
        return annotation.requestVolumeThreshold();
    }

    default double failureRatio(CircuitBreaker annotation) {
        return annotation.failureRatio();
    }

    default int successThreshold(CircuitBreaker annotation) {
        return annotation.successThreshold();
    }


    /*
     * @Bulkhead
     */

    default int value(Bulkhead annotation) {
        return annotation.value();
    }

    default int waitingTaskQueue(Bulkhead annotation) {
        return annotation.waitingTaskQueue();
    }


    /*
     * @Timeout
     */

    default long value(Timeout annotation) {
        return annotation.value();
    }

    default ChronoUnit unit(Timeout annotation) {
        return annotation.unit();
    }


    /*
     * @Fallback
     */

    default Class<? extends FallbackHandler<?>> value(Fallback annotation) {
        return annotation.value();
    }

    default String fallbackMethod(Fallback annotation) {
        return annotation.fallbackMethod();
    }
}
