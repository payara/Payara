package fish.payara.microprofile.faulttolerance;

import java.lang.annotation.Annotation;
import java.time.temporal.ChronoUnit;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jvnet.hk2.annotations.Contract;

import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

/**
 * Encapsulates all properties extracted from FT annotations and the {@link org.eclipse.microprofile.config.Config} so
 * that the processing can be declared independent of the actual resolution mechanism.
 * 
 * The default implementations provided will extract properties plain from the given annotations.
 */
@SuppressWarnings("unused")
@Contract
public interface FaultToleranceConfig {

    /**
     * FT behaves as stated by the present FT annotations.
     */
    FaultToleranceConfig ANNOTATED = new FaultToleranceConfig() {
        // uses default methods
    };

    /*
     * General
     */

    default boolean isNonFallbackEnabled(InvocationContext context) {
        return true;
    }

    default boolean isEnabled(Class<? extends Annotation> annotationType, InvocationContext context) {
        return true;
    }

    default boolean isMetricsEnabled(InvocationContext context) {
        return true;
    }

    default <A extends Annotation> A getAnnotation(Class<A> annotationType, InvocationContext context) {
        A annotation = context.getMethod().getAnnotation(annotationType);
        return annotation != null ? annotation : context.getMethod().getDeclaringClass().getAnnotation(annotationType);
    }

    default boolean isAnnotationPresent(Class<? extends Annotation> annotationType, InvocationContext context) {
        return getAnnotation(annotationType, context) != null;
    }

    default int interceptorPriority() {
        return Interceptor.Priority.PLATFORM_AFTER + 15;
    }


    /*
     * Retry
     */

    default int maxRetries(Retry annotation, InvocationContext context) {
        return annotation.maxRetries();
    }

    default long delay(Retry annotation, InvocationContext context) {
        return annotation.delay();
    }

    default ChronoUnit delayUnit(Retry annotation, InvocationContext context) {
        return annotation.delayUnit();
    }

    default long maxDuration(Retry annotation, InvocationContext context) {
        return annotation.maxDuration();
    }

    default ChronoUnit durationUnit(Retry annotation, InvocationContext context) {
        return annotation.durationUnit();
    }

    default long jitter(Retry annotation, InvocationContext context) {
        return annotation.jitter();
    }

    default ChronoUnit jitterDelayUnit(Retry annotation, InvocationContext context) {
        return annotation.jitterDelayUnit();
    }

    default Class<? extends Throwable>[] retryOn(Retry annotation, InvocationContext context) {
        return annotation.retryOn();
    }

    default Class<? extends Throwable>[] abortOn(Retry annotation, InvocationContext context) {
        return annotation.abortOn();
    }


    /*
     * Circuit-Breaker
     */

    default Class<? extends Throwable>[] failOn(CircuitBreaker annotation, InvocationContext context) {
        return annotation.failOn();
    }

    default long delay(CircuitBreaker annotation, InvocationContext context) {
        return annotation.delay();
    }

    default ChronoUnit delayUnit(CircuitBreaker annotation, InvocationContext context) {
        return annotation.delayUnit();
    }

    default int requestVolumeThreshold(CircuitBreaker annotation, InvocationContext context) {
        return annotation.requestVolumeThreshold();
    }

    default double failureRatio(CircuitBreaker annotation, InvocationContext context) {
        return annotation.failureRatio();
    }

    default int successThreshold(CircuitBreaker annotation, InvocationContext context) {
        return annotation.successThreshold();
    }


    /*
     * Bulkhead
     */

    default int value(Bulkhead annotation, InvocationContext context) {
        return annotation.value();
    }

    default int waitingTaskQueue(Bulkhead annotation, InvocationContext context) {
        return annotation.waitingTaskQueue();
    }


    /*
     * Timeout
     */

    default long value(Timeout annotation, InvocationContext context) {
        return annotation.value();
    }

    default ChronoUnit unit(Timeout annotation, InvocationContext context) {
        return annotation.unit();
    }


    /*
     * Fallback
     */

    default Class<? extends FallbackHandler<?>> value(Fallback annotation, InvocationContext context) {
        return annotation.value();
    }

    default String fallbackMethod(Fallback annotation, InvocationContext context) {
        return annotation.fallbackMethod();
    }
}
