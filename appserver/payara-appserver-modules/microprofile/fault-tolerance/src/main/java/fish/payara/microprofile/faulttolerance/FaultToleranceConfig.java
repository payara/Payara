/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Encapsulates all properties extracted from FT annotations and the {@link org.eclipse.microprofile.config.Config} so
 * that the processing can be declared independent of the actual resolution mechanism.
 * 
 * A configuration is bound to a specific invocation context which is not an argument to each of the provided methods
 * but passed to the implementation upon construction. For another invocation another configuration instance is bound.
 * 
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface FaultToleranceConfig {

    /**
     * A payara specific feature that allows to specify a list of annotation classes that have a similar effect as
     * {@link Asynchronous}.
     */
    String ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY = "MP_Fault_Tolerance_Alternative_Asynchronous_Annotations";

    @SuppressWarnings("unchecked")
    Class<? extends Annotation>[] NO_ALTERNATIVE_ANNOTATIONS = new Class[0];

    /**
     * @return A {@link FaultToleranceConfig} that behaves as stated by the present FT annotations. {@link Method}
     *         annotations take precedence over the {@link Class} level annotations.
     */
    static FaultToleranceConfig asAnnotated(Class<?> target, Method method) {
        return new FaultToleranceConfig() {
            @Override
            public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
                A annotation = method.getAnnotation(annotationType);
                return annotation != null ? annotation : target.getAnnotation(annotationType);
            }
        };
    }

    /**
     * Returns the value of the given annotation type for the invocation context was bound to upon construction.
     * 
     * @param annotationType type to lookup
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    /*
     * General
     */

    /**
     * Check global generic annotation switch.
     * 
     * @return true if (in addition to {@link Fallback}, which is always enabled) the other FT annotations are as well
     *         (default). Mostly used to disable these which will not disable {@link Fallback}.
     */
    default boolean isNonFallbackEnabled() {
        return true;
    }

    /**
     * Check global annotation specific annotation switch.
     * 
     * @param annotationType the annotation type to check
     * @return true if the given annotation type is globally enabled, false if it is globally disabled
     */
    default boolean isEnabled(Class<? extends Annotation> annotationType) {
        return true;
    }

    /**
     * Check for global metrics switch.
     * 
     * @return true if metrics are enabled, false otherwise.
     */
    default boolean isMetricsEnabled() {
        return true;
    }

    /**
     * @see #getAnnotation(Class)
     */
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    default boolean isAlternativeAsynchronousAnnoationPresent() {
        return false;
    }


    /*
     * @Retry
     */

    default int maxRetries(Retry annotation) {
        return annotation.maxRetries();
    }

    default long delay(Retry annotation) {
        return annotation.delay();
    }

    default ChronoUnit delayUnit(Retry annotation) {
        return annotation.delayUnit();
    }

    default long maxDuration(Retry annotation) {
        return annotation.maxDuration();
    }

    default ChronoUnit durationUnit(Retry annotation) {
        return annotation.durationUnit();
    }

    default long jitter(Retry annotation) {
        return annotation.jitter();
    }

    default ChronoUnit jitterDelayUnit(Retry annotation) {
        return annotation.jitterDelayUnit();
    }

    default Class<? extends Throwable>[] retryOn(Retry annotation) {
        return annotation.retryOn();
    }

    default Class<? extends Throwable>[] abortOn(Retry annotation) {
        return annotation.abortOn();
    }


    /*
     * @CircuitBreaker
     */

    default Class<? extends Throwable>[] failOn(CircuitBreaker annotation) {
        return annotation.failOn();
    }

    default Class<? extends Throwable>[] skipOn(CircuitBreaker annotation) {
        return annotation.skipOn();
    }

    default long delay(CircuitBreaker annotation) {
        return annotation.delay();
    }

    default ChronoUnit delayUnit(CircuitBreaker annotation) {
        return annotation.delayUnit();
    }

    default int requestVolumeThreshold(CircuitBreaker annotation) {
        return annotation.requestVolumeThreshold();
    }

    default double failureRatio(CircuitBreaker annotation) {
        return annotation.failureRatio();
    }

    default int successThreshold(CircuitBreaker annotation) {
        return annotation.successThreshold();
    }


    /*
     * @Bulkhead
     */

    default int value(Bulkhead annotation) {
        return annotation.value();
    }

    default int waitingTaskQueue(Bulkhead annotation) {
        return annotation.waitingTaskQueue();
    }


    /*
     * @Timeout
     */

    default long value(Timeout annotation) {
        return annotation.value();
    }

    default ChronoUnit unit(Timeout annotation) {
        return annotation.unit();
    }


    /*
     * @Fallback
     */

    default Class<? extends FallbackHandler<?>> value(Fallback annotation) {
        return annotation.value();
    }

    default String fallbackMethod(Fallback annotation) {
        return annotation.fallbackMethod();
    }

    default Class<? extends Throwable>[] applyOn(Fallback annotation) {
        return annotation.applyOn();
    }

    default Class<? extends Throwable>[] skipOn(Fallback annotation) {
        return annotation.skipOn();
    }
}
