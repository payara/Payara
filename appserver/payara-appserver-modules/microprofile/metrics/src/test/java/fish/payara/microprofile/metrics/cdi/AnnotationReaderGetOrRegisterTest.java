/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.microprofile.metrics.cdi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.spi.InjectionPoint;

import java.util.function.Function;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.junit.Test;

import fish.payara.microprofile.metrics.impl.Clock;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.test.TestUtils;

/**
 * Specifically tests the
 * {@link AnnotationReader#getOrRegister(jakarta.enterprise.inject.spi.InjectionPoint, Class, org.eclipse.microprofile.metrics.MetricRegistry)}
 * method.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class AnnotationReaderGetOrRegisterTest {

    private final MetricRegistry registry = new MetricRegistryImpl(
            MetricRegistry.APPLICATION_SCOPE, this::tick);
    private long clockTime;

    /**
     * Advances {@link Clock} by 1 sec each time it is called.
     */
    private long tick() {
        clockTime += TimeUnit.SECONDS.toNanos(1);
        return clockTime;
    }

    @Test
    public void counterWithoutAnnotation() {
        assertGetsOrRegisters(AnnotationReader.COUNTED, Counter.class, 0);
    }

    @Test
    @Counted(tags = { "a=b", "c=d" })
    public void counterFromName() {
        assertGetsOrRegisters(AnnotationReader.COUNTED, Counter.class, 2);
    }

    @Test
    @Counted(tags = { "a=b", "c=d" })
    public void counterFromMetdadata() {
        assertGetsOrRegisters(AnnotationReader.COUNTED, Counter.class, 2);
    }


    @Test
    public void timerWithoutAnnotation() {
        assertGetsOrRegisters(AnnotationReader.TIMED, Timer.class, 0);
    }

    @Test
    @Timed(tags = { "a=b", "c=d" })
    public void timerFromName() {
        assertGetsOrRegisters(AnnotationReader.TIMED, Timer.class, 2);
    }

    @Test
    @Timed(tags = { "a=b", "c=d" }, unit = MetricUnits.HOURS, description = "description")
    public void timerFromMetdadata() {
        assertGetsOrRegisters(AnnotationReader.TIMED, Timer.class, 2);
    }


    @Test
    public void gaugeWithoutAnnotation() {
        String name = getClass().getCanonicalName() + ".gaugeWithoutAnnotation";
        org.eclipse.microprofile.metrics.Gauge<Long> gauge = () -> 1L;
        Double d = new Double(gauge.getValue());
        Function<Double, Number> func = v -> d.doubleValue();
        org.eclipse.microprofile.metrics.Gauge<Number> g = registry.gauge(name, d , func);
        InjectionPoint point = testMethodAsInjectionPoint();
        org.eclipse.microprofile.metrics.Gauge<?> gauge2 = AnnotationReader.GAUGE.getOrRegister(point,
                org.eclipse.microprofile.metrics.Gauge.class, registry);
        assertSame(g, gauge2);
    }

    @Test
    @Gauge(tags = { "a=b", "c=d" }, unit = MetricUnits.HOURS, description = "description")
    public void gaugeFromMetdadata() {
        Metadata annoated = AnnotationReader.GAUGE.metadata(getClass(), TestUtils.getTestMethod());
        String name = getClass().getCanonicalName() + ".gaugeFromMetdadata";
        Metadata expected = Metadata.builder(annoated).withName(name).build();
        org.eclipse.microprofile.metrics.Gauge<Long> gauge = () -> 1L;
        Double d = new Double(gauge.getValue());
        Function<Double, Number> func = v -> d.doubleValue();
        org.eclipse.microprofile.metrics.Gauge<Number> g = registry
                .gauge(expected, d, func, new Tag("a", "b"), new Tag("c", "d"));
        InjectionPoint point = testMethodAsInjectionPoint();
        org.eclipse.microprofile.metrics.Gauge<?> gauge2 = AnnotationReader.GAUGE.getOrRegister(point,
                org.eclipse.microprofile.metrics.Gauge.class, registry);
        assertNotNull(gauge2);
        assertSame(g, gauge2);
    }

    private <T extends Metric, A extends Annotation> void assertGetsOrRegisters(AnnotationReader<A> reader,
            Class<T> metric, int expectedTags) {
        InjectionPoint point = testMethodAsInjectionPoint();
        T c1 = reader.getOrRegister(point, metric, registry);
        assertNotNull(c1);
        T c2 = reader.getOrRegister(point, metric, registry);
        assertNotNull(c2);
        assertSame(c1, c2);
        Set<MetricID> metricIDs = registry.getMetricIDs();
        assertEquals(1, metricIDs.size());
        MetricID metricID = metricIDs.iterator().next();
        assertEquals(expectedTags, metricID.getTagsAsArray().length);
        if (expectedTags == 2) {
            assertArrayEquals(new Tag[] { new Tag("a", "b"), new Tag("c", "d") }, metricID.getTagsAsArray());
        }
        Metadata metadata = registry.getMetadata(metricID.getName());
        assertNotNull(metadata);
        A annotation = null;
        try {
            annotation = reader.annotation(point);
        } catch (IllegalArgumentException ex) {
            // with no annotation, done
            return;
        }
        String description = reader.description(annotation);
        if (description.isEmpty()) {
            assertFalse(metadata.description().isPresent());
        } else {
            assertEquals(description, metadata.getDescription());
        }
        String unit = reader.unit(annotation);
        if (unit.isEmpty()) {
            assertFalse(metadata.unit().isPresent());
        } else {
            assertEquals(unit, metadata.getUnit());
        }
    }

    private static InjectionPoint testMethodAsInjectionPoint() {
        Method m = TestUtils.getTestMethod();
        return TestUtils.fakeInjectionPointFor(m, m);
    }
}
