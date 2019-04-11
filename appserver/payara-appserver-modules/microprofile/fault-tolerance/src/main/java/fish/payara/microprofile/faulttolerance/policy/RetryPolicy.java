package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Retry;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Retry} annotation an a specific method.
 */
public final class RetryPolicy extends Policy {

    @SuppressWarnings("unchecked")
    private static final RetryPolicy NONE = new RetryPolicy(null, 0, 0, ChronoUnit.SECONDS, 0, ChronoUnit.SECONDS, 0,
            ChronoUnit.SECONDS, new Class[0], new Class[0]);

    public final int maxRetries;
    public final long delay;
    public final ChronoUnit delayUnit;
    public final long maxDuration;
    public final ChronoUnit durationUnit;
    public final long jitter;
    public final ChronoUnit jitterDelayUnit;
    public final Class<? extends Throwable>[] retryOn;
    public final Class<? extends Throwable>[] abortOn;

    public RetryPolicy(Method annotatedMethod, int maxRetries, long delay, ChronoUnit delayUnit, long maxDuration, ChronoUnit durationUnit,
            long jitter, ChronoUnit jitterDelayUnit, Class<? extends Throwable>[] retryOn,
            Class<? extends Throwable>[] abortOn) {
        if (annotatedMethod != null) {
            checkAtLeast(-1, annotatedMethod, Retry.class, "maxRetries", maxRetries);
            checkAtLeast(0, annotatedMethod, Retry.class, "delay", delay);
            checkAtLeast(0, annotatedMethod, Retry.class, "maxDuration", maxDuration);
            checkAtLeast("delay", delay + 1, annotatedMethod, Retry.class, "maxDuration", maxDuration);
            checkAtLeast(0, annotatedMethod, Retry.class, "jitter", jitter);
        }
        this.maxRetries = maxRetries;
        this.delay = delay;
        this.delayUnit = delayUnit;
        this.maxDuration = maxDuration;
        this.durationUnit = durationUnit;
        this.jitter = jitter;
        this.jitterDelayUnit = jitterDelayUnit;
        this.retryOn = retryOn;
        this.abortOn = abortOn;
    }

    public static RetryPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Retry.class, context) && config.isEnabled(Retry.class, context)) {
            Retry annotation = config.getAnnotation(Retry.class, context);
            return new RetryPolicy(context.getMethod(),
                    config.maxRetries(annotation, context),
                    config.delay(annotation, context),
                    config.delayUnit(annotation, context),
                    config.maxDuration(annotation, context),
                    config.durationUnit(annotation, context),
                    config.jitter(annotation, context),
                    config.jitterDelayUnit(annotation, context),
                    config.retryOn(annotation, context),
                    config.abortOn(annotation, context));
        }
        return NONE;
    }

    public boolean retryOn(Exception ex) {
        return isCaught(ex, retryOn) && !isCaught(ex, abortOn);
    }

    public Long timeoutTimeNow() {
        return maxDuration == 0L ? null : System.currentTimeMillis() + Duration.of(maxDuration, durationUnit).toMillis();
    }

    public boolean isDelayed() {
        return delay > 0L || jitter > 0L;
    }

    public long jitteredDelay() {
        long duration = Duration.of(delay, delayUnit).toMillis();
        return jitter == 0L 
                ? duration
                : duration + ThreadLocalRandom.current().nextLong(0, Duration.of(jitter, jitterDelayUnit).toMillis());
    }

    public int totalAttempts() {
        return maxRetries < 0 ? Integer.MAX_VALUE : maxRetries + 1;
    }
}
