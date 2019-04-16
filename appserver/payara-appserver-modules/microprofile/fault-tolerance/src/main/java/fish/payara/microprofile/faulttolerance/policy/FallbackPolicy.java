package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import javassist.Modifier;

/**
 * The resolved "cached" information of a {@link Fallback} annotation an a specific method.
 */
public final class FallbackPolicy extends Policy {

    public final Class<? extends FallbackHandler<?>> value;
    public final String fallbackMethod;
    public final Method method;

    public FallbackPolicy(Method annotated, Class<? extends FallbackHandler<?>> value, String fallbackMethod) {
        checkUnambiguous(annotated, value, fallbackMethod);
        this.value = value;
        this.fallbackMethod = fallbackMethod;
        if (fallbackMethod != null && !fallbackMethod.isEmpty()) {
            method = MethodLookupUtils.findMethodWithMatchingNameAndArguments(fallbackMethod, annotated);
            if (method == null) {
                throw new FaultToleranceDefinitionException(describe(annotated, Fallback.class, "fallbackMethod")
                        + "value referring to a method that is not defined or has a incompatible method signature.");
            }
            checkReturnsSameAs(annotated, Fallback.class, "fallbackMethod", method);
            checkAccessible(annotated, method);
        } else {
            method = null;
        }
        if (isHandlerPresent()) {
            checkReturnsSameAs(annotated, Fallback.class, "value", value, "handle", ExecutionContext.class);
        }
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

    private static void checkUnambiguous(Method annotated, Class<? extends FallbackHandler<?>> value, String fallbackMethod) {
        if (fallbackMethod != null && !fallbackMethod.isEmpty() && value != null && value != Fallback.DEFAULT.class) {
            throw new FaultToleranceDefinitionException(
                    describe(annotated, Fallback.class, "") + "defined both a fallback handler and a fallback method.");
        }
    }

    private static void checkAccessible(Method annotated, Method fallback) {
        boolean samePackage = fallback.getDeclaringClass().getPackage().equals(annotated.getDeclaringClass().getPackage());
        boolean sameClass = fallback.getDeclaringClass().equals(annotated.getDeclaringClass());
        if (Modifier.isPackage(fallback.getModifiers()) && !samePackage
                || Modifier.isPrivate(fallback.getModifiers()) && !sameClass) {
            throw new FaultToleranceDefinitionException(describe(annotated, Fallback.class, "fallbackMethod")
                    + "value referring to a method that is not accessible.");
        }
    }

    public boolean isHandlerPresent() {
        return value != null && value != Fallback.DEFAULT.class;
    }

}
