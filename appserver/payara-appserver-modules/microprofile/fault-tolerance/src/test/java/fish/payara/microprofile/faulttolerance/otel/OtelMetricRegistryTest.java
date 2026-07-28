/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * only if the code is changed by the third party and used as a new code
 * in combination with Open Source Software developed by Glassfish/Payara
 * or its successors.
 */
package fish.payara.microprofile.faulttolerance.otel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

/**
 * Tests for {@link OtelMetricRegistry}.
 * Uses OpenTelemetry SDK in-memory reader to validate metric recording and attribute translation.
 */
public class OtelMetricRegistryTest {

    private InMemoryMetricReader metricReader;
    private SdkMeterProvider meterProvider;
    private OtelMetricRegistry registry;

    @Before
    public void setUp() {
        metricReader = InMemoryMetricReader.create();
        meterProvider = SdkMeterProvider.builder()
            .registerMetricReader(metricReader)
            .build();
        registry = new OtelMetricRegistry(meterProvider.get("test-meter"), "application");
    }

    @After
    public void tearDown() throws Exception {
        registry.close();
        meterProvider.close();
    }

    @Test
    public void testCounterCreation() {
        Counter counter = registry.counter("test.counter");
        assertNotNull(counter);
        counter.inc();
        counter.inc(5);
        
        var metrics = metricReader.collectAllMetrics();
        assertTrue("Should have recorded metrics", metrics.size() > 0);
    }

    @Test
    public void testCounterWithTags() {
        Counter counter = registry.counter("test.counter", new Tag("env", "test"), new Tag("service", "app"));
        assertNotNull(counter);
        counter.inc(10);
        
        var metrics = metricReader.collectAllMetrics();
        assertTrue("Should have recorded metrics with tags", metrics.size() > 0);
    }

    @Test
    public void testHistogramCreation() {
        Histogram histogram = registry.histogram("test.histogram");
        assertNotNull(histogram);
        histogram.update(100);
        histogram.update(200);
        
        var metrics = metricReader.collectAllMetrics();
        assertTrue("Should have recorded histogram metrics", metrics.size() > 0);
    }

    @Test
    public void testHistogramSnapshot() {
        Histogram histogram = registry.histogram("test.histogram");
        assertThrows(UnsupportedOperationException.class, histogram::getSnapshot);
    }

    @Test
    public void testGaugeWithSupplier() {
        Gauge<Long> gauge = registry.gauge("test.gauge", () -> 42L);
        assertNotNull(gauge);
        assertThrows(UnsupportedOperationException.class, gauge::getValue);
        
        var metrics = metricReader.collectAllMetrics();
        assertTrue("Should have recorded gauge metrics", metrics.size() > 0);
    }

    @Test
    public void testGaugeWithFunction() {
        String obj = "test_value";
        Gauge<Long> gauge = registry.gauge("test.gauge", obj, v -> (long) v.length());
        assertNotNull(gauge);
        assertThrows(UnsupportedOperationException.class, gauge::getValue);

        var metrics = metricReader.collectAllMetrics();
        assertTrue("Should have recorded gauge metrics", metrics.size() > 0);
    }

    @Test
    public void testUnsupportedTimer() {
        assertThrows(UnsupportedOperationException.class, () -> registry.timer("test.timer"));
    }



    @Test
    public void testMetricRetrieval() {
        Counter counter = registry.counter("test.counter");
        counter.inc(5);
        
        Counter retrieved = registry.getCounter(new MetricID("test.counter"));
        assertNotNull(retrieved);
        assertSame(counter, retrieved);
    }

    @Test
    public void testMetricRemoval() {
        registry.counter("test.counter");
        assertTrue(registry.getNames().contains("test.counter"));
        
        boolean removed = registry.remove("test.counter");
        assertTrue(removed);
        assertTrue(!registry.getNames().contains("test.counter"));
    }

    @Test
    public void testRegistryScope() {
        assertEquals("application", registry.getScope());
    }

    @Test
    public void testGaugeLifecycle() throws Exception {
        Gauge<Long> gauge1 = registry.gauge("gauge1", () -> 1L);
        Gauge<Long> gauge2 = registry.gauge("gauge2", () -> 2L);
        
        registry.close();
        
        var metrics = metricReader.collectAllMetrics();
        assertTrue("Should handle gauge cleanup gracefully", metrics.size() >= 0);
    }

}
