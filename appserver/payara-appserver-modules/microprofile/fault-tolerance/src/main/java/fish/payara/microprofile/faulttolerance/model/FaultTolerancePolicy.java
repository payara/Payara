package fish.payara.microprofile.faulttolerance.model;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The {@link FaultTolerancePolicy} describes the effective aggregated policies to use for a particular {@link Method}
 * when adding fault tolerant behaviour to it.
 * 
 * The policies are extracted from FT annotations and the {@link FaultToleranceConfig}.
 * 
 * In contrast to the plain annotations the policies do consider configuration overrides and include validation of the
 * effective values.
 * 
 * The policy class also reduces the need to analyse FT annotations for each invocation and works as a consistent source
 * of truth throughout the execution of FT behaviour that is convenient to pass around as a single immutable value.
 *
 * @author Jan Bernitt
 */
public final class FaultTolerancePolicy implements Serializable {

    private static final long TTL = 60 * 1000;

    private static final ConcurrentHashMap<Method, FaultTolerancePolicy> POLICY_BY_METHOD = new ConcurrentHashMap<>();

    public static void clean() {
        long now = System.currentTimeMillis();
        POLICY_BY_METHOD.entrySet().removeIf(entry -> now > entry.getValue().expiresMillis);
    }

    /**
     * Returns the {@link FaultTolerancePolicy} to use for the method invoked in the current context.
     *
     * @param context       current context
     * @param configSpplier supplies the configuration (if needed, in case returned policy needs to be created with help
     *                      of the {@link FaultToleranceConfig})
     * @return the policy to apply
     * @throws FaultToleranceDefinitionException in case the effective policy contains illegal values
     */
    public static FaultTolerancePolicy get(InvocationContext context, Supplier<FaultToleranceConfig> configSpplier)
            throws FaultToleranceDefinitionException {
        return POLICY_BY_METHOD.compute(context.getMethod(), (method, info) ->
            info != null && !info.isExpired() ? info : create(context, configSpplier));
    }

    private static FaultTolerancePolicy create(InvocationContext context, Supplier<FaultToleranceConfig> configSpplier) {
        FaultToleranceConfig config = configSpplier.get();
        boolean isFaultToleranceEnabled = config.isEnabled(context);
        if (!isFaultToleranceEnabled) {
            return new FaultTolerancePolicy(false, false, null, null, null, null, null, null);
        }
        return new FaultTolerancePolicy(isFaultToleranceEnabled,
                config.isMetricsEnabled(context),
                AsynchronousPolicy.create(context, config),
                BulkheadPolicy.create(context, config),
                CircuitBreakerPolicy.create(context, config),
                FallbackPolicy.create(context, config),
                RetryPolicy.create(context, config),
                TimeoutPolicy.create(context, config));
    }

    private final long expiresMillis;
    public final boolean isFaultToleranceEnabled;
    public final boolean isMetricsEnabled;
    public final AsynchronousPolicy asynchronous;
    public final BulkheadPolicy bulkhead;
    public final CircuitBreakerPolicy circuitBreaker;
    public final FallbackPolicy fallback;
    public final RetryPolicy retry;
    public final TimeoutPolicy timeout;

    public FaultTolerancePolicy(boolean isFaultToleranceEnabled, boolean isMetricsEnabled, AsynchronousPolicy asynchronous,
            BulkheadPolicy bulkhead, CircuitBreakerPolicy circuitBreaker, FallbackPolicy fallback, RetryPolicy retry,
            TimeoutPolicy timeout) {
        this.expiresMillis = System.currentTimeMillis() + TTL;
        this.isFaultToleranceEnabled = isFaultToleranceEnabled;
        this.isMetricsEnabled = isMetricsEnabled;
        this.asynchronous = asynchronous;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.fallback = fallback;
        this.retry = retry;
        this.timeout = timeout;
    }

    private boolean isExpired() {
        return System.currentTimeMillis() > expiresMillis;
    }

    public boolean isAsynchronous() {
        return asynchronous != null;
    }

    public boolean isBulkheadPresent() {
        return bulkhead != null;
    }

    public boolean isCircuitBreakerPresent() {
        return circuitBreaker != null;
    }

    public boolean isFallbackPresent() {
        return fallback != null;
    }

    public boolean isRetryPresent() {
        return retry != null;
    }

    public boolean isTimeoutPresent() {
        return timeout != null;
    }

}
