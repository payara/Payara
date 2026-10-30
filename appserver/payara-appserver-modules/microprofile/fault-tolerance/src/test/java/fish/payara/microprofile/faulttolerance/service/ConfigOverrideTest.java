/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.service;

import static fish.payara.microprofile.faulttolerance.test.TestUtils.getAnnotatedMethod;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.policy.StaticAnalysisContext;
import fish.payara.microprofile.faulttolerance.test.ConfigOverrides;

/**
 * Tests that properties can be used to override annotation attributes.
 * 
 * Also in response to:
 * 
 * - https://github.com/payara/Payara/issues/3762 
 * - https://github.com/payara/Payara/issues/3821
 * 
 * @author Jan Bernitt
 */
public class ConfigOverrideTest {

    private ConfigOverrides overrides = new ConfigOverrides();
    private BindableFaultToleranceConfig configFactory = new BindableFaultToleranceConfig(overrides, null);

    /*
     * Tests
     */

    /**
     * Tests a int/{@link Integer} value override
     */
    @Test
    public void circuitBreakerRequestVolumeThresholdOverride() {
        assertOverridden(CircuitBreaker.class, "requestVolumeThreshold", 42, 13,
                (config, annotation) -> config.requestVolumeThreshold(annotation));
    }

    @CircuitBreaker(requestVolumeThreshold = 42)
    public String circuitBreakerRequestVolumeThresholdOverride_Method() {
        return "test";
    }

    /**
     * Tests a {@link ChronoUnit} value override
     */
    @Test
    public void timeoutUnitOverride() {
        assertOverridden(Timeout.class, "unit", ChronoUnit.DECADES, ChronoUnit.HOURS,
                (config, annotation) -> config.unit(annotation));
    }

    @Timeout(unit = ChronoUnit.DECADES)
    public String timeoutUnitOverride_Method() {
        return "test";
    }

    /**
     * Tests a {@link Class} value override
     */
    @Test
    public void fallbackValueOverride() {
        assertOverridden(Fallback.class, "value", MyFallbackHandler.class, MyOtherFallbackHandler.class,
                (config, annotation) -> config.value(annotation));
    }

    @Fallback(value = MyFallbackHandler.class)
    public String fallbackValueOverride_Method() {
        return "test";
    }

    /**
     * Tests a {@link String} value override
     */
    @Test
    public void fallbackFallbackMethodOverride() {
        assertOverridden(Fallback.class, "fallbackMethod", "annotatedFallbackMethod", "overriddenFallbackMethod",
                (config, annotation) -> config.fallbackMethod(annotation));
    }

    @Fallback(fallbackMethod = "annotatedFallbackMethod")
    public String fallbackFallbackMethodOverride_Method() {
        return "test";
    }

    /**
     * Tests a long/{@link Long} value override
     */
    @Test
    public void retryDelayOverride() {
        assertOverridden(Retry.class, "delay", 54321L, 12345L,
                (config, annotation) -> config.delay(annotation));
    }

    @Retry(delay = 54321)
    public String retryDelayOverride_Method() {
        return "test";
    }

    /**
     * Tests a {@link Class[]} value override
     */
    @Test
    public void retryRetryOnOverride() {
        assertOverridden(Retry.class, "retryOn",
                new Class[] { IllegalStateException.class, IllegalArgumentException.class },
                new Class[] { NoSuchElementException.class, UnsupportedOperationException.class },
                (config, annotation) -> config.retryOn(annotation));
    }

    @Retry(retryOn = { IllegalStateException.class, IllegalArgumentException.class })
    public String retryRetryOnOverride_Method() {
        return "test";
    }

    private <T, A extends Annotation> void assertOverridden(Class<A> annotationType, String propertyName, T annotated,
            T overridden, BiFunction<FaultToleranceConfig, A, T> property) {
        Method annotatedMethod = getAnnotatedMethod();
        FaultToleranceConfig config = configFactory.bindTo(new StaticAnalysisContext(getClass(), annotatedMethod));
        A annotation = config.getAnnotation(annotationType);
        // check we get the expected annotated value
        T actual = property.apply(config, annotation);
        assertEqualValue(annotated, actual);
        // make the override
        overrides.override(annotatedMethod, annotationType, propertyName, overridden);
        // now check that we get the expected overridden value
        actual = property.apply(config, annotation);
        assertEqualValue(overridden, actual);
    }

    private static <T> void assertEqualValue(T expected, T actual) {
        if (actual instanceof Object[]) {
            assertArrayEquals((Object[]) expected, (Object[])actual);
        } else {
            assertEquals(expected, actual);
        }
    }

    interface MyFallbackHandler extends FallbackHandler<String> {/* just for config override test */}
    interface MyOtherFallbackHandler extends FallbackHandler<String> {/* just for config override test */}
}
