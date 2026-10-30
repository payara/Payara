/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Retry;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Retry} annotation an a specific method.
 *
 * @author Jan Bernitt
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
        if (config.isAnnotationPresent(Retry.class) && config.isEnabled(Retry.class)) {
            Retry annotation = config.getAnnotation(Retry.class);
            return new RetryPolicy(context.getMethod(),
                    config.maxRetries(annotation),
                    config.delay(annotation),
                    config.delayUnit(annotation),
                    config.maxDuration(annotation),
                    config.durationUnit(annotation),
                    config.jitter(annotation),
                    config.jitterDelayUnit(annotation),
                    config.retryOn(annotation),
                    config.abortOn(annotation));
        }
        return NONE;
    }

    public boolean isNone() {
        return this != NONE;
    }

    /**
     * Should a retry occur then the given {@link Throwable} is thrown?
     *
     * Relevant section from {@link Retry} javadocs:
     * <blockquote>
     * When a method returns and the retry policy is present, the following rules are applied:
     * <ol>
     * <li>If the method returns normally (doesn't throw), the result is simply returned.
     * <li>Otherwise, if the thrown object is assignable to any value in the {@link #abortOn()} parameter, the thrown object is rethrown.
     * <li>Otherwise, if the thrown object is assignable to any value in the {@link #retryOn()} parameter, the method call is retried.
     * <li>Otherwise the thrown object is rethrown.
     * </ol>
     * </blockquote>
     *
     * @param ex an {@link Error} or an {@link Exception}
     * @return true, if a retry should occur, else false.
     */
    public boolean retryOn(Throwable ex) {
        return !isCaught(ex, abortOn) && isCaught(ex, retryOn);
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

    public boolean isMaxRetriesSet() {
        return maxRetries >= 0;
    }

    public boolean isMaxDurationSet() {
        return maxDuration > 0L;
    }
}
