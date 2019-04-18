package fish.payara.microprofile.faulttolerance.interceptors;

import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;

public abstract class BaseFaultToleranceInterceptor<T extends Annotation> {

    protected final Logger logger;
    private final String type;
    private final Class<T> annotationType;
    private final boolean throwExceptionForRetry;
    private FaultToleranceConfig config;
    private FaultToleranceService execution;
    private FaultToleranceMetrics metrics;

    protected BaseFaultToleranceInterceptor(Class<T> annotationType, boolean throwExceptionForRetry) {
        this.type = annotationType.getSimpleName();
        this.annotationType = annotationType;
        this.throwExceptionForRetry = throwExceptionForRetry;
        this.logger = Logger.getLogger(getClass().getName());
    }

    public void setConfig(FaultToleranceConfig behaviour) {
        this.config = behaviour;
    }

    public FaultToleranceConfig getConfig() {
        return config;
    }

    public FaultToleranceMetrics getMetrics() {
        return metrics;
    }

    public FaultToleranceService getExecution() {
        return execution;
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        return resultValue;
    }
}
