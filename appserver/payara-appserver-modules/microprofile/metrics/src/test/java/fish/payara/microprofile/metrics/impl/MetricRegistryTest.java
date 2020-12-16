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

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link MetricRegistry} API in most basic scenario only to check that each method has been implemented at
 * all and without using wrong constant or similar clear mistakes that are easily overlooked.
 *
 * @author Jan Bernitt
 */
public class MetricRegistryTest {

    private static final Supplier<Long> GAUGE_SUPPLIER = () -> 42L;
    private static final Function<MetricRegistryTest, Long> GAUGE_FUNCTION = test -> 42L;

    private final MetricRegistry registry = new MetricRegistryImpl(Type.APPLICATION);

    private static final MetricID COUNTER_ID = new MetricID("counter");
    private static final MetricID CONCURRENT_GAUGE_ID = new MetricID("concurrentGauge");
    private static final MetricID GAUGE_ID = new MetricID("gauge");
    private static final MetricID HISTOGRAM_ID = new MetricID("histogram");
    private static final MetricID METER_ID = new MetricID("meter");
    private static final MetricID TIMER_ID = new MetricID("timer");
    private static final MetricID SIMPLE_TIMER_ID = new MetricID("simpleTimer");

    private Counter counter;
    private ConcurrentGauge concurrentGauge;
    private Gauge<Long> gauge;
    private Histogram histogram;
    private Meter meter;
    private Timer timer;
    private SimpleTimer simpleTimer;

    @Before
    public void setUp() {
        counter = registry.counter(COUNTER_ID);
        concurrentGauge = registry.concurrentGauge(CONCURRENT_GAUGE_ID);
        gauge = registry.gauge(GAUGE_ID, GAUGE_SUPPLIER);
        histogram = registry.histogram(HISTOGRAM_ID);
        meter = registry.meter(METER_ID);
        timer = registry.timer(TIMER_ID);
        simpleTimer = registry.simpleTimer(SIMPLE_TIMER_ID);
    }

    @Test
    public void counterByMetricID() {
        assertSame(counter, registry.counter(COUNTER_ID));
    }

    @Test
    public void counterByName() {
        assertSame(counter, registry.counter(COUNTER_ID.getName()));
    }

    @Test
    public void counterByNameAndTags() {
        assertSame(counter, registry.counter(COUNTER_ID.getName(), COUNTER_ID.getTagsAsArray()));
    }

    @Test
    public void counterByMetadata() {
        assertSame(counter, registry.counter(metadataOf(COUNTER_ID)));
    }

    @Test
    public void counterByMetadataAndTags() {
        assertSame(counter, registry.counter(metadataOf(COUNTER_ID), COUNTER_ID.getTagsAsArray()));
    }

    @Test
    public void concurrentGaugeByMetricID() {
        assertSame(concurrentGauge, registry.concurrentGauge(CONCURRENT_GAUGE_ID));
    }

    @Test
    public void concurrentGaugeByName() {
        assertSame(concurrentGauge, registry.concurrentGauge(CONCURRENT_GAUGE_ID.getName()));
    }

    @Test
    public void concurrentGaugeByNameAndTags() {
        assertSame(concurrentGauge, registry.concurrentGauge(CONCURRENT_GAUGE_ID.getName(), CONCURRENT_GAUGE_ID.getTagsAsArray()));
    }

    @Test
    public void concurrentGaugeByMetadata() {
        assertSame(concurrentGauge, registry.concurrentGauge(metadataOf(CONCURRENT_GAUGE_ID)));
    }

    @Test
    public void concurrentGaugeByMetadataAndTags() {
        assertSame(concurrentGauge, registry.concurrentGauge(metadataOf(CONCURRENT_GAUGE_ID), CONCURRENT_GAUGE_ID.getTagsAsArray()));
    }

    @Test
    public void gaugeByMetricID() {
        assertSame(gauge, registry.gauge(GAUGE_ID, GAUGE_SUPPLIER));
    }

    @Test
    public void gaugeByName() {
        assertSame(gauge, registry.gauge(GAUGE_ID.getName(), GAUGE_SUPPLIER));
    }

    @Test
    public void gaugeByNameAndTags() {
        assertSame(gauge, registry.gauge(GAUGE_ID.getName(), GAUGE_SUPPLIER, GAUGE_ID.getTagsAsArray()));
    }

    @Test
    public void gaugeByMetadata() {
        assertSame(gauge, registry.gauge(metadataOf(GAUGE_ID), GAUGE_SUPPLIER));
    }

    @Test
    public void gaugeByMetadataAndTags() {
        assertSame(gauge, registry.gauge(metadataOf(GAUGE_ID), GAUGE_SUPPLIER, GAUGE_ID.getTagsAsArray()));
    }

    @Test
    public void gaugeByMetricIDFunction() {
        assertSame(gauge, registry.gauge(GAUGE_ID, this, GAUGE_FUNCTION));
    }

    @Test
    public void gaugeByNameFunction() {
        assertSame(gauge, registry.gauge(GAUGE_ID.getName(), this, GAUGE_FUNCTION));
    }

    @Test
    public void gaugeByNameAndTagsFunction() {
        assertSame(gauge, registry.gauge(GAUGE_ID.getName(), this, GAUGE_FUNCTION, GAUGE_ID.getTagsAsArray()));
    }

    @Test
    public void gaugeByMetadataFunction() {
        assertSame(gauge, registry.gauge(metadataOf(GAUGE_ID), this, GAUGE_FUNCTION));
    }

    @Test
    public void gaugeByMetadataAndTagsFunction() {
        assertSame(gauge, registry.gauge(metadataOf(GAUGE_ID), this, GAUGE_FUNCTION, GAUGE_ID.getTagsAsArray()));
    }

    @Test
    public void histogramByMetricID() {
        assertSame(histogram, registry.histogram(HISTOGRAM_ID));
    }

    @Test
    public void histogramByName() {
        assertSame(histogram, registry.histogram(HISTOGRAM_ID.getName()));
    }

    @Test
    public void histogramByNameAndTags() {
        assertSame(histogram, registry.histogram(HISTOGRAM_ID.getName(), HISTOGRAM_ID.getTagsAsArray()));
    }

    @Test
    public void histogramByMetadata() {
        assertSame(histogram, registry.histogram(metadataOf(HISTOGRAM_ID)));
    }

    @Test
    public void histogramByMetadataAndTags() {
        assertSame(histogram, registry.histogram(metadataOf(HISTOGRAM_ID), HISTOGRAM_ID.getTagsAsArray()));
    }

    @Test
    public void timerByMetricID() {
        assertSame(timer, registry.timer(TIMER_ID));
    }

    @Test
    public void timerByName() {
        assertSame(timer, registry.timer(TIMER_ID.getName()));
    }

    @Test
    public void timerByNameAndTags() {
        assertSame(timer, registry.timer(TIMER_ID.getName(), TIMER_ID.getTagsAsArray()));
    }

    @Test
    public void timerByMetadata() {
        assertSame(timer, registry.timer(metadataOf(TIMER_ID)));
    }

    @Test
    public void timerByMetadataAndTags() {
        assertSame(timer, registry.timer(metadataOf(TIMER_ID), TIMER_ID.getTagsAsArray()));
    }

    @Test
    public void simpleTimerByMetricID() {
        assertSame(simpleTimer, registry.simpleTimer(SIMPLE_TIMER_ID));
    }

    @Test
    public void simpleTimerByName() {
        assertSame(simpleTimer, registry.simpleTimer(SIMPLE_TIMER_ID.getName()));
    }

    @Test
    public void simpleTimerByNameAndTags() {
        assertSame(simpleTimer, registry.simpleTimer(SIMPLE_TIMER_ID.getName(), SIMPLE_TIMER_ID.getTagsAsArray()));
    }

    @Test
    public void simpleTimerByMetadata() {
        assertSame(simpleTimer, registry.simpleTimer(metadataOf(SIMPLE_TIMER_ID)));
    }

    @Test
    public void simpleTimerByMetadataAndTags() {
        assertSame(simpleTimer, registry.simpleTimer(metadataOf(SIMPLE_TIMER_ID), SIMPLE_TIMER_ID.getTagsAsArray()));
    }

    @Test
    public void getMetricByMetricId() {
        assertSame(counter, registry.getMetric(COUNTER_ID));
        assertSame(concurrentGauge, registry.getMetric(CONCURRENT_GAUGE_ID));
        assertSame(gauge, registry.getMetric(GAUGE_ID));
        assertSame(histogram, registry.getMetric(HISTOGRAM_ID));
        assertSame(meter, registry.getMetric(METER_ID));
        assertSame(timer, registry.getMetric(TIMER_ID));
        assertSame(simpleTimer, registry.getMetric(SIMPLE_TIMER_ID));
        assertNull(registry.getMetric(new MetricID("does-not-exist")));
    }

    @Test
    public void getCounterByMetricID() {
        assertSame(counter, registry.getCounter(COUNTER_ID));
        assertNull(registry.getCounter(new MetricID("does-not-exist")));
    }

    @Test
    public void getConcurrentGaugeByMetricID() {
        assertSame(concurrentGauge, registry.getConcurrentGauge(CONCURRENT_GAUGE_ID));
        assertNull(registry.getConcurrentGauge(new MetricID("does-not-exist")));
    }

    @Test
    public void getGaugeByMetricID() {
        assertSame(gauge, registry.getGauge(GAUGE_ID));
        assertNull(registry.getGauge(new MetricID("does-not-exist")));
    }

    @Test
    public void getHistogramByMetricID() {
        assertSame(histogram, registry.getHistogram(HISTOGRAM_ID));
        assertNull(registry.getHistogram(new MetricID("does-not-exist")));
    }

    @Test
    public void getMeterByMetricID() {
        assertSame(meter, registry.getMeter(METER_ID));
        assertNull(registry.getMeter(new MetricID("does-not-exist")));
    }

    @Test
    public void getTimerByMetricID() {
        assertSame(timer, registry.getTimer(TIMER_ID));
        assertNull(registry.getTimer(new MetricID("does-not-exist")));
    }

    @Test
    public void getSimpleTimer() {
        assertSame(simpleTimer, registry.getSimpleTimer(SIMPLE_TIMER_ID));
        assertNull(registry.getSimpleTimer(new MetricID("does-not-exist")));
    }

    @Test
    public void getMetadataByName() {
        assertNotNull(registry.getMetadata(COUNTER_ID.getName()));
        assertNotNull(registry.getMetadata(CONCURRENT_GAUGE_ID.getName()));
        assertNotNull(registry.getMetadata(GAUGE_ID.getName()));
        assertNotNull(registry.getMetadata(HISTOGRAM_ID.getName()));
        assertNotNull(registry.getMetadata(METER_ID.getName()));
        assertNotNull(registry.getMetadata(TIMER_ID.getName()));
        assertNotNull(registry.getMetadata(SIMPLE_TIMER_ID.getName()));
        assertNull(registry.getMetadata("does-not-exist"));
    }

    @Test
    public void getNames() {
        assertEquals(7, registry.getNames().size());
    }

    @Test
    public void getMetricIDs() {
        assertEquals(7, registry.getMetricIDs().size());
    }

    @Test
    public void getCounters() {
        assertEquals(singletonMap(COUNTER_ID, counter), registry.getCounters());
    }

    @Test
    public void getConcurrentGauges() {
        assertEquals(singletonMap(CONCURRENT_GAUGE_ID, concurrentGauge), registry.getConcurrentGauges());
    }

    @Test
    public void getGauges() {
        assertEquals(singletonMap(GAUGE_ID, gauge), registry.getGauges());
    }

    @Test
    public void getHistograms() {
        assertEquals(singletonMap(HISTOGRAM_ID, histogram), registry.getHistograms());
    }

    @Test
    public void getMeters() {
        assertEquals(singletonMap(METER_ID, meter), registry.getMeters());
    }

    @Test
    public void getTimers() {
        assertEquals(singletonMap(TIMER_ID, timer), registry.getTimers());
    }

    @Test
    public void getSimpleTimers() {
        assertEquals(singletonMap(SIMPLE_TIMER_ID, simpleTimer), registry.getSimpleTimers());
    }

    @Test
    public void getMetricsByType() {
        assertEquals(singletonMap(COUNTER_ID, counter), registry.getMetrics(Counter.class, MetricFilter.ALL));
        assertEquals(singletonMap(CONCURRENT_GAUGE_ID, concurrentGauge), registry.getMetrics(ConcurrentGauge.class, MetricFilter.ALL));
        assertEquals(singletonMap(GAUGE_ID, gauge), registry.getMetrics(Gauge.class, MetricFilter.ALL));
        assertEquals(singletonMap(HISTOGRAM_ID, histogram), registry.getMetrics(Histogram.class, MetricFilter.ALL));
        assertEquals(singletonMap(METER_ID, meter), registry.getMetrics(Meter.class, MetricFilter.ALL));
        assertEquals(singletonMap(TIMER_ID, timer), registry.getMetrics(Timer.class, MetricFilter.ALL));
        assertEquals(singletonMap(SIMPLE_TIMER_ID, simpleTimer), registry.getMetrics(SimpleTimer.class, MetricFilter.ALL));
        assertEquals(0, registry.getMetrics(Counter.class, (metricID, metric) -> false).size());
    }

    @Test
    public void getMetrics() {
        assertEquals(7, registry.getMetrics().size());
    }

    @Test
    public void getMetricsByFilter() {
        assertEquals(7, registry.getMetrics(MetricFilter.ALL).size());
        assertEquals(0, registry.getMetrics((metricID, metric) -> false).size());
    }

    @Test
    public void getMetadata() {
        assertEquals(7, registry.getMetadata().size());
    }

    @Test
    public void getType() {
        assertEquals(Type.APPLICATION, registry.getType());
    }

    @Test
    public void removeByName() {
       assertTrue(registry.remove(COUNTER_ID.getName()));
       assertNull(registry.getCounter(COUNTER_ID));
       assertEquals(6, registry.getNames().size());
       assertFalse(registry.remove(COUNTER_ID.getName()));

       assertTrue(registry.remove(CONCURRENT_GAUGE_ID.getName()));
       assertNull(registry.getConcurrentGauge(CONCURRENT_GAUGE_ID));
       assertEquals(5, registry.getNames().size());
       assertFalse(registry.remove(CONCURRENT_GAUGE_ID.getName()));

       assertTrue(registry.remove(GAUGE_ID.getName()));
       assertNull(registry.getGauge(GAUGE_ID));
       assertEquals(4, registry.getNames().size());
       assertFalse(registry.remove(GAUGE_ID.getName()));

       assertTrue(registry.remove(HISTOGRAM_ID.getName()));
       assertNull(registry.getHistogram(HISTOGRAM_ID));
       assertEquals(3, registry.getNames().size());
       assertFalse(registry.remove(HISTOGRAM_ID.getName()));

       assertTrue(registry.remove(METER_ID.getName()));
       assertNull(registry.getMeter(METER_ID));
       assertEquals(2, registry.getNames().size());
       assertFalse(registry.remove(METER_ID.getName()));

       assertTrue(registry.remove(TIMER_ID.getName()));
       assertNull(registry.getTimer(TIMER_ID));
       assertEquals(1, registry.getNames().size());
       assertFalse(registry.remove(TIMER_ID.getName()));

       assertTrue(registry.remove(SIMPLE_TIMER_ID.getName()));
       assertNull(registry.getSimpleTimer(SIMPLE_TIMER_ID));
       assertEquals(0, registry.getNames().size());
       assertFalse(registry.remove(SIMPLE_TIMER_ID.getName()));
    }

    @Test
    public void removeByMetricID() {
       assertTrue(registry.remove(COUNTER_ID));
       assertNull(registry.getCounter(COUNTER_ID));
       assertEquals(6, registry.getNames().size());
       assertFalse(registry.remove(COUNTER_ID));

       assertTrue(registry.remove(CONCURRENT_GAUGE_ID));
       assertNull(registry.getConcurrentGauge(CONCURRENT_GAUGE_ID));
       assertEquals(5, registry.getNames().size());
       assertFalse(registry.remove(CONCURRENT_GAUGE_ID));

       assertTrue(registry.remove(GAUGE_ID));
       assertNull(registry.getGauge(GAUGE_ID));
       assertEquals(4, registry.getNames().size());
       assertFalse(registry.remove(GAUGE_ID));

       assertTrue(registry.remove(HISTOGRAM_ID));
       assertNull(registry.getHistogram(HISTOGRAM_ID));
       assertEquals(3, registry.getNames().size());
       assertFalse(registry.remove(HISTOGRAM_ID));

       assertTrue(registry.remove(METER_ID));
       assertNull(registry.getMeter(METER_ID));
       assertEquals(2, registry.getNames().size());
       assertFalse(registry.remove(METER_ID));

       assertTrue(registry.remove(TIMER_ID));
       assertNull(registry.getTimer(TIMER_ID));
       assertEquals(1, registry.getNames().size());
       assertFalse(registry.remove(TIMER_ID));

       assertTrue(registry.remove(SIMPLE_TIMER_ID));
       assertNull(registry.getSimpleTimer(SIMPLE_TIMER_ID));
       assertEquals(0, registry.getNames().size());
       assertFalse(registry.remove(SIMPLE_TIMER_ID));
    }

    @Test
    public void removeMatchingByFilter() {
        registry.removeMatching((metricID, metric) -> metricID.equals(COUNTER_ID));
        assertEquals(6, registry.getMetricIDs().size());
        assertNull(registry.getMetric(COUNTER_ID));
        assertNotNull(registry.getMetric(CONCURRENT_GAUGE_ID));
        assertNotNull(registry.getMetric(GAUGE_ID));
        assertNotNull(registry.getMetric(HISTOGRAM_ID));
        assertNotNull(registry.getMetric(METER_ID));
        assertNotNull(registry.getMetric(TIMER_ID));
        assertNotNull(registry.getMetric(SIMPLE_TIMER_ID));
    }

    @Test
    public void removeMatchingByFilterALL() {
        registry.removeMatching(MetricFilter.ALL);
        assertEquals(0, registry.getMetricIDs().size());
        assertNull(registry.getMetric(COUNTER_ID));
        assertNull(registry.getMetric(CONCURRENT_GAUGE_ID));
        assertNull(registry.getMetric(GAUGE_ID));
        assertNull(registry.getMetric(HISTOGRAM_ID));
        assertNull(registry.getMetric(METER_ID));
        assertNull(registry.getMetric(TIMER_ID));
        assertNull(registry.getMetric(SIMPLE_TIMER_ID));
    }

    private static Metadata metadataOf(MetricID metricID) {
        return Metadata.builder().withName(metricID.getName()).build();
    }
}
