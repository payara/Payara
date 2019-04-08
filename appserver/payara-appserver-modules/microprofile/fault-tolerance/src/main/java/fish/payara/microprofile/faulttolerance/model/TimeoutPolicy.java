package fish.payara.microprofile.faulttolerance.model;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Timeout;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Timeout} annotation an a specific method.
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
        if (config.isAnnotationPresent(Timeout.class, context) && config.isEnabled(Timeout.class, context)) {
            Timeout annotation = config.getAnnotation(Timeout.class, context);
            return new TimeoutPolicy(context.getMethod(),
                    config.value(annotation, context),
                    config.unit(annotation, context));
        }
        return null;
    }
}
