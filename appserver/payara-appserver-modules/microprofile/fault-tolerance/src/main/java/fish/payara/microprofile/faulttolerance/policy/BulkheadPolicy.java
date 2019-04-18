package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Bulkhead} annotation an a specific method.
 * 
 * @author Jan Bernitt
 */
public final class BulkheadPolicy extends Policy {

    public final int value;
    public final int waitingTaskQueue;

    public BulkheadPolicy(Method annotatedMethod, int value, int waitingTaskQueue) {
        checkAtLeast(1, annotatedMethod, Bulkhead.class, "value", value);
        checkAtLeast(0, annotatedMethod, Bulkhead.class, "waitingTaskQueue", waitingTaskQueue);
        this.value = value;
        this.waitingTaskQueue = waitingTaskQueue;
    }

    public static BulkheadPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Bulkhead.class) && config.isEnabled(Bulkhead.class)) {
            Bulkhead annotation = config.getAnnotation(Bulkhead.class);
            return new BulkheadPolicy(context.getMethod(),
                    config.value(annotation), 
                    config.waitingTaskQueue(annotation));
        }
        return null;
    }
}
