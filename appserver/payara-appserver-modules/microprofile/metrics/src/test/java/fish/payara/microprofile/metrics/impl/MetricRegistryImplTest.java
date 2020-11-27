/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.metrics.impl;

import static java.util.Arrays.asList;
import static org.eclipse.microprofile.metrics.Metadata.builder;
import static org.eclipse.microprofile.metrics.MetricType.HISTOGRAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.junit.Before;
import org.junit.Test;

import fish.payara.microprofile.metrics.TestConfig;
import fish.payara.microprofile.metrics.impl.CounterImpl;
import fish.payara.microprofile.metrics.impl.GaugeImpl;
import fish.payara.microprofile.metrics.impl.HistogramImpl;
import fish.payara.microprofile.metrics.impl.MeterImpl;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.impl.TimerImpl;

/**
 * Tests the basic correctness of the {@link MetricRegistryImpl}.
 * This does not include and form of thread-safety testing.
 *
 * @author Jan Bernitt
 */
public class MetricRegistryImplTest {

    private static final AtomicInteger nextNameId = new AtomicInteger();

    private static final Tag SOME_TAG = new Tag("some", "tag");
    private static final Metadata FILLED = builder()
            .withDisplayName("display")
            .withDescription("description")
            .withUnit("unit")
            .withName("name").build();

    private static Metadata withName(String name) {
        return withNameAnd(name).build();
    }

    private static MetadataBuilder withNameAnd(String name) {
        return Metadata.builder(FILLED).withName(name);
    }

    private final MetricRegistry registry = new MetricRegistryImpl(Type.APPLICATION);

    @Before
    public void setUp() {
        TestConfig.resetConfig();
    }

    @Test
    public void counterByNameUsesExistingMetadataForSameName() {
        assertExistingMetadataIsUsed(
            name -> registry.counter(withName(name)),
            name -> registry.counter(name));
    }

    @Test
    public void counterByNameCreatesNonExistingCounter() {
        String name = nextName();
        Counter counter = registry.counter(name);
        assertNotNull(counter);
        Metadata metadata = registry.getMetadata(name);
        assertEquals(name, metadata.getName());
        assertEquals(name, metadata.getDisplayName());
        assertEquals(MetricType.COUNTER, metadata.getTypeRaw());
    }

    @Test
    public void counterByNameReceivesExistingCounter() {
        String name = nextName();
        Counter counter = registry.counter(name);
        assertSame(counter, registry.counter(name));
        assertNotSame(counter, registry.counter(nextName()));
    }

    @Test
    public void counterByMetadataCreatesNonExistingCounter() {
        String name = nextName();
        Metadata metadata = withNameAnd(name).withType(MetricType.COUNTER).build();
        Counter counter = registry.counter(metadata);
        assertNotNull(counter);
        Metadata actualMetadata = registry.getMetadata(name);
        assertEquals(name, actualMetadata.getName());
        assertEquals(MetricType.COUNTER, actualMetadata.getTypeRaw());
        assertEquals(metadata, actualMetadata);
    }

    @Test
    public void counterByMetadataReceivesExistingCounter() {
        String name = nextName();
        Metadata metadata = withNameAnd(name).withType(MetricType.COUNTER).build();
        Counter counter = registry.counter(metadata);
        assertSame(counter, registry.counter(metadata));
        assertNotSame(counter, registry.counter(metadata, SOME_TAG));
    }

    @Test
    public void counterByMetaThrowsExceptionWhenMetadataIsDifferent() {
        assertException("Tried to lookup a metric with conflicting metadata, looup is ...",
            name -> registry.counter(withName(name)),
            name -> registry.counter(withNameAnd(name).withDisplayName("other").build()));
    }

    @Test
    public void findOrCreateThrowsExceptionWhenExistingMetadataIsNotSameType() {
        assertException("Metric ['%s'] type['histogram'] does not match with existing type['counter']",
            name -> registry.counter(withName(name)),
            name -> registry.histogram(withName(name), SOME_TAG));
    }

    @Test
    public void registerByNameUsesExistingMetadataForSameName() {
        assertExistingMetadataIsUsed(
            name -> registry.register(withName(name), new CounterImpl(), SOME_TAG),
            name -> registry.register(name, new CounterImpl()));
    }

    @Test
    public void registerByNameThrowsExceptionWhenNameIsEmpty() {
        assertException("Metric name must not be null or empty",
            () -> registry.register("", new CounterImpl()));
    }

    @Test
    public void registerByNameThrowsExceptionWhenNameIsNull() {
        assertException("Metric name must not be null or empty",
            () -> registry.register((String) null, new CounterImpl()));
    }

    @Test
    public void registerByNameThrowsExceptionWhenTypeIsDifferent() {
        assertException("Metric ['%s'] type['histogram'] does not match with existing type['counter']",
            name -> registry.register(withName(name), new CounterImpl(), SOME_TAG),
            name -> registry.register(name, new HistogramImpl()));
    }

    @Test
    public void registerByMetadataAcceptsInstancesWithSameNameButDifferentTags() {
        EnumSet<MetricType> types = EnumSet.allOf(MetricType.class);
        types.remove(MetricType.INVALID); // obviously
        types.remove(MetricType.GAUGE); // gauges cannot be created
        for (MetricType type : types) {
            String name = nextName();
            for (int i = 0; i < 10; i++) {
                final int n = i;
                MetadataBuilder metadata = withNameAnd(name).withType(type);
                Runnable register = () -> registry.register(metadata.build(), null, new Tag("a", "b" + n));
                register.run(); // first time should work
                register.run(); // works again as all metrics are reusable
            }
        }
    }

    @Test
    public void registerByMetadataAcceptsGaugeWithSameNameButDifferentTags() {
        String name = nextName();
        Gauge<Long> gauge1 = () -> 1L;
        registry.register(withNameAnd(name).build(), gauge1, new Tag("a", "b"));
        Gauge<Long> gauge2 = () -> 2L;
        registry.register(withNameAnd(name).build(), gauge2, new Tag("a", "c"));
    }

    @Test
    public void registerByMetadataAllowsToRegisterMetricsOfSameNameWithDifferentTagsButSameType() {
        String name = nextName();
        Tag ab = new Tag("a", "b");
        Tag ac = new Tag("a", "c");
        MetricID metricAb = new MetricID(name, ab);
        MetricID metricAc = new MetricID(name, ac);
        registry.register(withName(name), new TimerImpl(), ab);
        registry.register(withName(name), new TimerImpl(), ac);
        assertEquals(2, registry.getTimers().size());
        Map<MetricID, Metric> metrics = registry.getMetrics((metricID, metric) -> metricID.getName().equals(name));
        assertEquals(2, metrics.size());
        assertTrue(metrics.containsKey(metricAb));
        assertTrue(metrics.containsKey(metricAc));
        assertEquals(new TreeSet<>(asList(metricAb, metricAc)), registry.getMetricIDs());
        assertEquals(new TreeSet<>(asList(name)), registry.getNames());
        assertEquals(1, registry.getTimers((id, metric) -> id.equals(metricAc)).size());
    }

    @Test
    public void registerByMetadataAllowsToReuseAsLongAsMetadataIsSame() {
        Histogram h1 = new HistogramImpl();
        assertExistingMetadataIsUsed(
            name -> registry.register(withName(name), h1),
            name -> registry.register(withName(name), new HistogramImpl()));
        assertSame(h1, registry.getHistograms().values().iterator().next());
    }

    @Test
    public void registerByMetadataThrowsExceptionWhenNameIsEmpty() {
        assertException("Name must not be empty",
            () -> registry.register(withName(""), new MeterImpl()));
    }

    @Test
    public void registerByMetadataThrowsExceptionWhenExistingMetadataForSameNameIsDifferent_NotSameType() {
        assertException("Metric ['%s'] type['histogram'] does not match with existing type['counter']",
            name -> registry.register(withName(name), new CounterImpl()),
            name -> registry.register(withName(name), new HistogramImpl()));
        assertException("Metric ['%s'] type['histogram'] does not match with existing type['counter']",
            name -> registry.register(withName(name), new CounterImpl()),
            name -> registry.register(withNameAnd(name).withType(HISTOGRAM).build(), new CounterImpl()));
    }

    @Test
    public void registerByMetadataThrowsExceptionWhenExistingMetadataForSameNameIsDifferent_Gauge() {
        Method dummy = getClass().getMethods()[0];
        Gauge<?> gauge1 = new GaugeImpl<>(dummy, this);
        Gauge<?> gauge2 = new GaugeImpl<>(dummy, this);
        String name = nextName();
        registry.register(withName(name), gauge1);
        assertSame(gauge1, registry.register(withName(name), gauge2));
    }

    @Test
    public void registerByMetadataThrowsExceptionWhenExistingMetadataForSameNameIsDifferent_NotSameMetadata() {
        assertException("Metadata ['DefaultMetadata{name='%s', type=counter, unit='unit', description='description', displayName='display'}'] already registered, does not match provided ['DefaultMetadata{name='%s', type=counter, unit='unit', description='description', displayName='other'}']",
            name -> registry.register(withName(name), new CounterImpl()),
            name -> registry.register(withNameAnd(name).withDisplayName("other").build(), new CounterImpl()));
    }

    @Test
    public void removeByNameRemovesAllExistingMetricsWithThatName() {
        String name = nextName();
        registry.meter(withName(name));
        registry.meter(withName(name), SOME_TAG);
        String otherName = nextName();
        Meter remaining = registry.meter(withName(otherName));
        assertEquals(3, registry.getMeters().size());
        assertEquals(new TreeSet<>(asList(name, otherName)), registry.getNames());
        assertTrue(registry.remove(name));
        assertEquals(1, registry.getMeters().size());
        assertTrue(registry.getMeters().values().contains(remaining));
        assertEquals(new TreeSet<>(asList(otherName)), registry.getNames());
        assertFalse(registry.remove(name));
    }

    @Test
    public void removeByNameIgnoresNamesWithoutAnyMetric() {
        assertFalse(registry.remove("does not exist"));
    }

    @Test
    public void removeByMetricIdRemovesOnlyMetricsWithThatId() {
        String name = nextName();
        registry.concurrentGauge(withName(name));
        ConcurrentGauge remaining1 = registry.concurrentGauge(withName(name), SOME_TAG);
        String otherName = nextName();
        ConcurrentGauge remaining2 = registry.concurrentGauge(withName(otherName));
        assertEquals(3, registry.getConcurrentGauges().size());
        assertEquals(new TreeSet<>(asList(name, otherName)), registry.getNames());
        assertTrue(registry.remove(new MetricID(name)));
        assertEquals(2, registry.getConcurrentGauges().size());
        assertEquals(new HashSet<>(asList(remaining1, remaining2)), new HashSet<>(registry.getConcurrentGauges().values()));
        assertEquals(new TreeSet<>(asList(name, otherName)), registry.getNames());
        assertTrue(registry.remove(new MetricID(name, SOME_TAG)));
        assertEquals(1, registry.getConcurrentGauges().size());
        assertEquals(new TreeSet<>(asList(otherName)), registry.getNames());
    }

    @Test
    public void removeByMetricIdIgnoresNamesWithoutAnyMetric() {
        assertFalse(registry.remove(new MetricID("does not exist")));
    }


    @Test
    public void removeMatchingRemovesAllMatchingMetrics() {
        registry.concurrentGauge(nextName(), new Tag("x", "y"));
        registry.timer(nextName(), new Tag("x", "z"));
        registry.histogram(nextName(), new Tag("x", "y"));
        registry.meter(nextName(), new Tag("z", "a"));
        registry.timer(nextName(), new Tag("h", "i"));
        assertEquals(5, registry.getNames().size());
        registry.removeMatching((id, metric) -> id.getTags().containsValue("y"));
        assertEquals(3, registry.getNames().size());
        assertEquals(2, registry.getTimers().size());
        assertEquals(1, registry.getMeters().size());
        registry.removeMatching((id, metric) -> id.getTags().containsKey("x"));
        assertEquals(2, registry.getNames().size());
        registry.removeMatching(MetricFilter.ALL);
        assertEquals(0, registry.getNames().size());
    }

    @Test
    public void getMetricById() {
        String name = nextName();
        Counter c1 = registry.counter(name);
        assertSame(c1, registry.getMetric(new MetricID(name), Counter.class));
    }

    @Test
    public void getMetricByIdThrowsExceptionWhenWrongTypeIsRequested() {
        String name = nextName();
        registry.counter(name);
        assertException("Invalid metric type : interface org.eclipse.microprofile.metrics.Histogram",
                () -> registry.getMetric(new MetricID(name), Histogram.class));
    }

    private static void assertException(String expectedMsg, Runnable test) {
        try {
            test.run();
            fail("Expected exception did not occur, message should have been: " + expectedMsg);
        } catch (IllegalArgumentException ex) {
            if (expectedMsg.endsWith("...")) {
                int startsWithLength = expectedMsg.length() - 3;
                assertEquals(expectedMsg.substring(0, startsWithLength), ex.getMessage().substring(0, startsWithLength));
            } else {
                assertEquals(expectedMsg, ex.getMessage());
            }
        }
    }

    private static void assertException(String expectedMsg, Consumer<String> setup, Consumer<String> test) {
        String name = nextName();
        setup.accept(name);
        if (expectedMsg.contains("%s")) {
            expectedMsg = expectedMsg.replaceAll("%s", name);
        }
        assertException(expectedMsg, () -> test.accept(name));
    }

    private void assertExistingMetadataIsUsed(Consumer<String> setup, Consumer<String> test) {
        String name = nextName();
        setup.accept(name);
        Metadata expected = registry.getMetadata(name);
        test.accept(name);
        assertSame(expected, registry.getMetadata(name));
    }

    private static String nextName() {
        return "metric" + nextNameId.incrementAndGet();
    }
}
