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
import fish.payara.microprofile.faulttolerance.FaultToleranceEnvironment;
import fish.payara.microprofile.faulttolerance.interceptors.fallback.FallbackPolicy;

public abstract class BaseFaultToleranceInterceptor<T extends Annotation> {

    protected final Logger logger;
    private final String type;
    private final Class<T> annotationType;
    private final boolean throwExceptionForRetry;
    private FaultToleranceConfig config = FaultToleranceConfig.ANNOTATED;
    private FaultToleranceEnvironment execution;
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

    public FaultToleranceEnvironment getExecution() {
        return execution;
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        try {
            // Attempt to proceed the InvocationContext with FT semantics if FT is enabled for this method
            if (getConfig().isNonFallbackEnabled(context) && getConfig().isEnabled(annotationType, context)) {
                // Only increment the invocations metric if the Retry, Bulkhead, and CircuitBreaker annotations aren't present
                if (!getConfig().isAnnotationPresent(Bulkhead.class, context)
                        && !getConfig().isAnnotationPresent(Retry.class, context)
                        && !getConfig().isAnnotationPresent(CircuitBreaker.class, context)) {
                    getMetrics().incrementInvocationsTotal(context);
                }

                logger.log(Level.FINER, "Proceeding invocation with " + type + " semantics");
                resultValue = null; //TODO call FT specific method
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled, proceeding normally without "
                        + type + ".");
                resultValue = context.proceed();
            }
        } catch (Exception ex) {
            if (throwExceptionForRetry && getConfig().isAnnotationPresent(Retry.class, context)) {
                logger.log(Level.FINE, "Retry annotation found on method, propagating error upwards.");
                throw ex;
            }
            Fallback fallback = getConfig().getAnnotation(Fallback.class, context);

            // Only fall back if the annotation hasn't been disabled
            if (fallback != null && getConfig().isEnabled(Fallback.class, context)) {
                logger.log(Level.FINE, "Fallback annotation found on method, and no Retry annotation - "
                        + "falling back from " + type);
                FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, getConfig(), getExecution(), getMetrics(), context);
                resultValue = fallbackPolicy.fallback(context, ex);
            } else {
                // Increment the failure counter metric
                getMetrics().incrementInvocationsFailedTotal(context);
                throw ex;
            }
        }
        return resultValue;
    }
}
