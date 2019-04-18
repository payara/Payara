package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.interceptor.InvocationContext;

/**
 * A {@link InvocationContext} used during static analysis.
 * 
 * @author Jan Bernitt
 */
final class StaticAnalysisContext implements InvocationContext {

    private final Class<?> targetClass;
    private final Method annotated;
    private transient Object target;

    public StaticAnalysisContext(Class<?> targetClass, Method annotated) {
        this.targetClass = targetClass;
        this.annotated = annotated;
    }

    @Override
    public Object getTarget() {
        if (target == null) {
            try {
                target = targetClass.newInstance();
            } catch (Exception e) {
                target = new UnsupportedOperationException();
            }
        }
        if (target instanceof UnsupportedOperationException) {
            throw (UnsupportedOperationException) target;
        }
        return target;
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
