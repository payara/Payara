package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.interceptor.InvocationContext;

final class StaticAnalysisMethodContext implements InvocationContext {

    private final Method annotated;

    public StaticAnalysisMethodContext(Method annotated) {
        this.annotated = annotated;
    }

    @Override
    public Object getTarget() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getTimer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Method getMethod() {
        return annotated;
    }

    @Override
    public Constructor<?> getConstructor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParameters(Object[] params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getContextData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object proceed() throws Exception {
        throw new UnsupportedOperationException();
    }

}
