package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Timeout;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Timeout} annotation an a specific method.
 * 
 * @author Jan Bernitt
 */
public final class TimeoutPolicy extends Policy {

    public final long value;
    public final ChronoUnit unit;

    public TimeoutPolicy(Method annotatedMethod, long value, ChronoUnit unit) {
        checkAtLeast(0, annotatedMethod, Timeout.class, "value", value);
        this.value = value;
        this.unit = unit;
    }

    public static TimeoutPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Timeout.class) && config.isEnabled(Timeout.class)) {
            Timeout annotation = config.getAnnotation(Timeout.class);
            return new TimeoutPolicy(context.getMethod(),
                    config.value(annotation),
                    config.unit(annotation));
        }
        return null;
    }

    public long toMillis() {
        return Duration.of(value, unit).toMillis();
    }
}
