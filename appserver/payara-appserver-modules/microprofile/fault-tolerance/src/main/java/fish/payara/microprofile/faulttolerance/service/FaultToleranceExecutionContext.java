package fish.payara.microprofile.faulttolerance.service;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

/**
 * Default implementation class for the Fault Tolerance ExecutionContext interface
 */
final class FaultToleranceExecutionContext implements ExecutionContext {

    private final Method method;
    private final Object[] parameters;
    private final Throwable failure;

    FaultToleranceExecutionContext(Method method, Object[] parameters, Throwable failure) {
        this.method = method;
        this.parameters = parameters;
        this.failure = failure;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public Throwable getFailure() {
        return failure;
    }
}