package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link CircuitBreaker} annotation an a specific method.
 * 
 * @author Jan Bernitt
 */
public final class CircuitBreakerPolicy extends Policy {

    static final Logger logger = Logger.getLogger(CircuitBreakerPolicy.class.getName());

    public final Class<? extends Throwable>[] failOn;
    public final long delay;
    public final ChronoUnit delayUnit;
    public final int requestVolumeThreshold;
    public final double failureRatio;
    public final int successThreshold;

    public CircuitBreakerPolicy(Method annotatedMethod, Class<? extends Throwable>[] failOn, long delay, ChronoUnit delayUnit,
            int requestVolumeThreshold, double failureRatio, int successThreshold) {
        checkAtLeast(0, annotatedMethod, CircuitBreaker.class, "delay", delay);
        checkAtLeast(1, annotatedMethod, CircuitBreaker.class, "requestVolumeThreshold", requestVolumeThreshold);
        checkAtLeast(0d, annotatedMethod, CircuitBreaker.class, "failureRatio", failureRatio);
        checkAtMost(1.0d, annotatedMethod, CircuitBreaker.class, "failureRatio", failureRatio);
        checkAtLeast(1, annotatedMethod, CircuitBreaker.class, "successThreshold", successThreshold);
        this.failOn = failOn;
        this.delay = delay;
        this.delayUnit = delayUnit;
        this.requestVolumeThreshold = requestVolumeThreshold;
        this.failureRatio = failureRatio;
        this.successThreshold = successThreshold;
    }

    public static CircuitBreakerPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(CircuitBreaker.class) && config.isEnabled(CircuitBreaker.class)) {
            CircuitBreaker annotation = config.getAnnotation(CircuitBreaker.class);
            return new CircuitBreakerPolicy(context.getMethod(),
                    config.failOn(annotation),
                    config.delay(annotation),
                    config.delayUnit(annotation),
                    config.requestVolumeThreshold(annotation),
                    config.failureRatio(annotation),
                    config.successThreshold(annotation));
        }
        return null;
    }

    /**
     * Helper method that checks whether or not the given exception is included in the failOn parameter.
     * 
     * @param ex The exception to check
     * @return True if the exception is covered by {@link #failOn} list of this policy
     */
    public boolean failOn(Exception ex) {
        return Policy.isCaught(ex, failOn);
    }
}
