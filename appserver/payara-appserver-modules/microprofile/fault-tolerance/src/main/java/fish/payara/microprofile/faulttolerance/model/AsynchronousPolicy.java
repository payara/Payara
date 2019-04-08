package fish.payara.microprofile.faulttolerance.model;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Asynchronous} annotation an a specific method.
 */
public final class AsynchronousPolicy extends Policy {

    private static final AsynchronousPolicy IS_ASYNCHRONOUS = new AsynchronousPolicy();

    private AsynchronousPolicy() {
        // hide
    }

    public static AsynchronousPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Asynchronous.class, context) && config.isEnabled(Asynchronous.class, context)) {
            checkReturnsFutureOrCompletionStage(context);
            return IS_ASYNCHRONOUS;
        }
        return null;
    }

    private static void checkReturnsFutureOrCompletionStage(InvocationContext context) {
        Class<?> returnType = context.getMethod().getReturnType();
        if (returnType != Future.class && returnType != CompletionStage.class) {
            throw new FaultToleranceDefinitionException(describe(context.getMethod(), Asynchronous.class, "")
                    + "does not return a Future or CompletionStage but: " + returnType.getName());
        }
    }
}
