package fish.payara.microprofile.faulttolerance.service;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Identifier of method-related data in Fault Tolerance.
 * It is essential that the computed signature is referring to the {@link Method} as defined by the target
 * {@link Object} class not its declaring {@link Class} as this could be different when called via an abstract
 * {@link Method} implemented or overridden by the target {@link Class}.
 *
 * Since MP FT 3.0 all instances of a class share same state object for the same method. Or in other words the FT
 * context is not specific to an instance but to the annotated class and method.
 */
final class MethodKey {
    final Class<?> targetClass;
    final Method method;
    private String methodId;

    MethodKey(InvocationContext ctx) {
        this.targetClass = ctx.getTarget().getClass();
        this.method = ctx.getMethod();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodKey methodKey = (MethodKey) o;
        return targetClass.equals(methodKey.targetClass) && method.equals(methodKey.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetClass, method);
    }

    String getMethodId() {
        if (methodId != null) {
            return methodId;
        }
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(targetClass.getName()).append('.').append(method.getName());
        if (method.getParameterCount() > 0) {
            idBuilder.append('(');
            for (Class<?> param : method.getParameterTypes()) {
                idBuilder.append(param.getName()).append(' ');
            }
            idBuilder.append(')');
        }
        methodId = idBuilder.toString();
        return methodId;
    }

    @Override
    public String toString() {
        return getMethodId();
    }
}
