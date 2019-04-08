package fish.payara.microprofile.faulttolerance.model;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Fallback} annotation an a specific method.
 */
public final class FallbackPolicy extends Policy {

    public final Class<? extends FallbackHandler<?>> value;
    public final String fallbackMethod;

    public FallbackPolicy(Method method, Class<? extends FallbackHandler<?>> value, String fallbackMethod) {
        checkUnambiguous(value, fallbackMethod);
        if (fallbackMethod != null && !fallbackMethod.isEmpty()) {
            checkReturnsSameAs(method, Fallback.class, "fallbackMethod", method.getDeclaringClass(), fallbackMethod, 
                    method.getParameterTypes());
        }
        if (value != null && value != Fallback.DEFAULT.class) {
            checkReturnsSameAs(method, Fallback.class, "value", value, "handle", ExecutionContext.class);
        }
        this.value = value;
        this.fallbackMethod = fallbackMethod;
    }

    public static FallbackPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Fallback.class, context) && config.isEnabled(Fallback.class, context)) {
            Fallback annotation = config.getAnnotation(Fallback.class, context);
            return new FallbackPolicy(context.getMethod(),
                    config.value(annotation, context),
                    config.fallbackMethod(annotation, context));
        }
        return null;
    }

    private static void checkUnambiguous(Class<? extends FallbackHandler<?>> value, String fallbackMethod) {
        if (fallbackMethod != null && !fallbackMethod.isEmpty() && value != null && value != Fallback.DEFAULT.class) {
                throw new FaultToleranceDefinitionException("Both a fallback class and method have been set.");
        }
    }
}
