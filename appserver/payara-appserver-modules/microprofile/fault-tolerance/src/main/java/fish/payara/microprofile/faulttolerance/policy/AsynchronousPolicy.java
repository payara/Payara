package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
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

    private static final AsynchronousPolicy FUTURE = new AsynchronousPolicy();
    private static final AsynchronousPolicy COMPLETION_STAGE = new AsynchronousPolicy();

    private AsynchronousPolicy() {
        // hide
    }

    public static AsynchronousPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Asynchronous.class, context) && config.isEnabled(Asynchronous.class, context)) {
            checkReturnsFutureOrCompletionStage(context.getMethod());
            return context.getMethod().getReturnType() == Future.class ? FUTURE : COMPLETION_STAGE;
        }
        return null;
    }

    static void checkReturnsFutureOrCompletionStage(Method annotated) {
        Class<?> returnType = annotated.getReturnType();
        if (returnType != Future.class && returnType != CompletionStage.class) {
            throw new FaultToleranceDefinitionException(describe(annotated, Asynchronous.class, "")
                    + "does not return a Future or CompletionStage but: " + returnType.getName());
        }
    }

    public static Future<?> toFuture(Object asyncResult) {
        return asyncResult instanceof CompletionStage
                ? ((CompletionStage<?>) asyncResult).toCompletableFuture()
                : (Future<?>) asyncResult;
    }

    public boolean isSuccessWhenCompletedExceptionally() {
        return this == FUTURE;
    }
}
