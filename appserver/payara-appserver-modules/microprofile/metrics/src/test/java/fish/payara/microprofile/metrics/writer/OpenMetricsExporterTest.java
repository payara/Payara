/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.metrics.writer;

import static java.nio.file.Files.readAllBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.junit.Test;

/**
 * Test the general correctness of the {@link OpenMetricsExporter} by creating the {@link Metric} instance that matches
 * the example given in the specification and compare the exported output with the example output given in the
 * specification.
 *
 * Note that the {@link Timer} example is modified slightly because of double value formatting.
 *
 * https://download.eclipse.org/microprofile/microprofile-metrics-2.3/microprofile-metrics-spec-2.3.pdf
 */
public class OpenMetricsExporterTest {

    /**
     * The actual output as written by the {@link OpenMetricsExporter}
     */
    private final StringWriter actual = new StringWriter();
    private final MetricExporter exporter = new OpenMetricsExporter(actual).in(Type.APPLICATION);

    @Test
    public void exportCounter() {
        Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(80L);
        MetricID metricID = new MetricID("visitors");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("The number of unique visitors")
                .build();
        assertOutputEqualsFile("Counter.txt", metricID, counter, metadata);
    }

    @Test
    public void exportConcurrentGauge() {
        ConcurrentGauge gauge = mock(ConcurrentGauge.class);
        when(gauge.getCount()).thenReturn(80L);
        when(gauge.getMin()).thenReturn(20L);
        when(gauge.getMax()).thenReturn(100L);
        MetricID metricID = new MetricID("method_a_invocations");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("The number of parallel invocations of methodA()")
                .build();
        assertOutputEqualsFile("ConcurrentGauge.txt", metricID, gauge, metadata);
    }

    @Test
    public void exportMeter() {
        Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(29382L);
        when(meter.getMeanRate()).thenReturn(12.223d);
        when(meter.getOneMinuteRate()).thenReturn(12.563d);
        when(meter.getFiveMinuteRate()).thenReturn(12.364d);
        when(meter.getFifteenMinuteRate()).thenReturn(12.126d);
        MetricID metricID = new MetricID("requests");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("Tracks the number of requests to the server")
                .build();
        assertOutputEqualsFile("Meter.txt", metricID, meter, metadata);
    }

    @Test
    public void exportHistogram() {
        Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(2037L);
        when(histogram.getSum()).thenReturn(45678L);
        Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMin()).thenReturn(180L);
        when(snapshot.getMax()).thenReturn(31716L);
        when(snapshot.getMean()).thenReturn(4738.231d);
        when(snapshot.getStdDev()).thenReturn(1054.7343037063602d);
        when(snapshot.getMedian()).thenReturn(4201d);
        when(snapshot.get75thPercentile()).thenReturn(6175d);
        when(snapshot.get95thPercentile()).thenReturn(13560d);
        when(snapshot.get98thPercentile()).thenReturn(29643d);
        when(snapshot.get99thPercentile()).thenReturn(31716d);
        when(snapshot.get999thPercentile()).thenReturn(31716d);
        MetricID metricID = new MetricID("file_sizes");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("Users file size")
                .withUnit(MetricUnits.BYTES)
                .build();
        assertOutputEqualsFile("Histogram.txt", metricID, histogram, metadata);
    }

    @Test
    public void exportSimpleTimer() {
        SimpleTimer timer = mock(SimpleTimer.class);
        when(timer.getCount()).thenReturn(12L);
        when(timer.getElapsedTime()).thenReturn(Duration.ofMillis(12300L));
        when(timer.getMaxTimeDuration()).thenReturn(Duration.ofMillis(3231L));
        when(timer.getMinTimeDuration()).thenReturn(Duration.ofNanos(25600000L));
        MetricID metricID = new MetricID("response_time");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("The number of calls to this REST endpoint #(1)")
                .build();
        assertOutputEqualsFile("SimpleTimer.txt", metricID, timer, metadata);
    }

    @Test
    public void exportGauge() {
        @SuppressWarnings("unchecked")
        Gauge<Long> gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(80L);
        MetricID metricID = new MetricID("cost");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("The running cost of the server in dollars.")
                .withUnit("dollars")
                .build();
        assertOutputEqualsFile("Gauge.txt", metricID, gauge, metadata);
    }

    @Test
    public void exportTimer() {
        Timer timer = mock(Timer.class);
        when(timer.getElapsedTime()).thenReturn(Duration.ofMillis(23L));
        when(timer.getCount()).thenReturn(80L);
        when(timer.getMeanRate()).thenReturn(0.004292520715985437d);
        when(timer.getOneMinuteRate()).thenReturn(2.794076465421066E-14d);
        when(timer.getFiveMinuteRate()).thenReturn(4.800392614619373E-4d);
        when(timer.getFifteenMinuteRate()).thenReturn(0.01063191047532505d);
        Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMin()).thenReturn(169916L);
        when(snapshot.getMax()).thenReturn(560869L);
        when(snapshot.getMean()).thenReturn(415041d);
        when(snapshot.getStdDev()).thenReturn(652907d);
        when(snapshot.getMedian()).thenReturn(293324d);
        when(snapshot.get75thPercentile()).thenReturn(344914d);
        when(snapshot.get95thPercentile()).thenReturn(543647d);
        when(snapshot.get98thPercentile()).thenReturn(2706543d);
        when(snapshot.get99thPercentile()).thenReturn(5608694d);
        when(snapshot.get999thPercentile()).thenReturn(5608694d);
        MetricID metricID = new MetricID("response_time");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("Server response time for /index.html")
                .withUnit(MetricUnits.NANOSECONDS)
                .build();
        assertOutputEqualsFile("Timer.txt", metricID, timer, metadata);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void exportTags() {
        Gauge<Long> fooVal = mock(Gauge.class);
        when(fooVal.getValue()).thenReturn(12345L);
        MetricID fooValID = new MetricID("fooVal", new Tag("store", "webshop"));
        Metadata fooValMetadata = Metadata.builder()
                .withName(fooValID.getName())
                .withDescription("The average duration of foo requests during last 5 minutes")
                .withUnit(MetricUnits.MILLISECONDS)
                .build();
        MetricExporter base = exporter.in(Type.BASE);
        base.export(fooValID, fooVal, fooValMetadata);
        Gauge<Long> barVal = mock(Gauge.class);
        when(barVal.getValue()).thenReturn(42L);
        MetricID barValID = new MetricID("barVal", new Tag("component", "backend"), new Tag("store", "webshop"));
        Metadata barValMetadata = Metadata.builder()
                .withName(barValID.getName())
                .withUnit(MetricUnits.KILOBYTES)
                .build();
        base.export(barValID, barVal, barValMetadata);
        Gauge<Long> barVal2 = mock(Gauge.class);
        when(barVal2.getValue()).thenReturn(63L);
        MetricID barVal2ID = new MetricID("barVal", new Tag("component", "frontend"), new Tag("store", "webshop"));
        base.export(barVal2ID, barVal2, barValMetadata);
        assertOutputEqualsFile("GaugeTags.txt");
    }

    /*
     * Below tests are no examples from the specification
     */

    @Test
    public void eachTypeAndHelpLineOccursOnlyOnceForEachOpenMetricsName() {
        Gauge<Long> g1 = () -> 1L;
        MetricID g1ID = new MetricID("common", new Tag("a", "b"));
        Gauge<Long> g2 = () -> 2L;
        MetricID g2ID = new MetricID("common", new Tag("some", "other"));
        Metadata metadata = Metadata.builder()
                .withName("common")
                .withDescription("description")
                .build();
        exporter.export(g1ID, g1, metadata);
        exporter.export(g2ID, g2, metadata);
        assertEquals("# TYPE application_common gauge\n" +
                "# HELP application_common description\n" +
                "application_common{a=\"b\"} 1\n" +
                "application_common{some=\"other\"} 2\n", actual.getBuffer().toString());
    }

    @Test
    public void quantilesAreAppendedOtherTags() {
        Histogram histogram = mock(Histogram.class);
        Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);
        MetricID metricID = new MetricID("test6", new Tag("custom", "tag-value"));
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.MILLISECONDS)
                .build();
        exporter.export(metricID, histogram, metadata);
        String actualOutput = actual.getBuffer().toString();
        assertTrue(actualOutput.contains("application_test6_seconds{custom=\"tag-value\",quantile=\"0.5\"} 0"));
    }

    @Test
    public void gaugesWithNonNumberValuesAreNotExported() {
        Gauge<String> gauge = () -> "hello world";
        MetricID metricID = new MetricID("test3");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("", metricID, gauge, metadata);
    }

    @Test
    public void gaugesThatThrowIllegalStateExceptionWhenReadAreNotExported() {
        Gauge<Long> gauge = () -> { throw new IllegalStateException("test"); };
        MetricID metricID = new MetricID("test4");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("", metricID, gauge, metadata);
    }

    @Test
    public void tagValuesAreEscaped() {
        Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(13L);
        MetricID metricID = new MetricID("test5", new Tag("key", "escape\\and\"and\n"));
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("# TYPE application_test5_total counter\n" +
                "application_test5_total{key=\"escape\\\\and\\\"and\\n\"} 13\n", metricID, counter, metadata);
    }

    @Test
    public void namesAreadyEndingWithSuffixAvoidAppendingSuffixAgain() {
        Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(13L);
        MetricID metricID = new MetricID("my_total");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("# TYPE application_my_total counter\n" +
                "application_my_total 13\n", metricID, counter, metadata);
    }

    @Test
    public void unitPerSecondUsesUnscaledValue() {
        Gauge<Double> perSec = () -> 2.3d;
        MetricID metricID = new MetricID("test7");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.PER_SECOND)
                .build();
        assertOutputEquals("# TYPE application_test7_per_second gauge\n" +
                "application_test7_per_second 2.3\n", metricID, perSec, metadata);
    }

    @Test
    public void unitPercentMappedToRatioWithUnscaledValue() {
        Gauge<Double> perSec = () -> 2.3d;
        MetricID metricID = new MetricID("test8");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.PERCENT)
                .build();
        assertOutputEquals("# TYPE application_test8_ratio gauge\n" +
                "application_test8_ratio 2.3\n", metricID, perSec, metadata);
    }

    @Test
    public void unitOfStyleA_Per_BAreKeptWithUnscaledValue() {
        Gauge<Double> aPerB = () -> 2.3d;
        MetricID metricID = new MetricID("test9");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit("meter_per_sec")
                .build();
        assertOutputEquals("# TYPE application_test9_meter_per_sec gauge\n" +
                "application_test9_meter_per_sec 2.3\n", metricID, aPerB, metadata);
    }

    @Test
    public void unitNoneShouldNotBeAppendedToName() {
        Gauge<Long> gauge = () -> 13L;
        MetricID metricID = new MetricID("test2");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.NONE)
                .build();
        assertOutputEquals("# TYPE application_test2 gauge\n" +
                "application_test2 13\n", metricID, gauge, metadata);
    }

    @Test
    public void emptyDescriptionDoesNotPrintHelpLine() {
        Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(13L);
        MetricID metricID = new MetricID("test1");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("")
                .build();
        assertOutputEquals("# TYPE application_test1_total counter\n" +
                "application_test1_total 13\n", metricID, counter, metadata);
    }

    @Test
    public void unitAnyBitsHasBaseUnitBytes() {
        assertUnitConversion(MetricUnits.BITS, 1, "bytes", "0.125");
        assertUnitConversion(MetricUnits.BITS, 64, "bytes", "8");

        // those that scale with 1000
        assertUnitConversion(MetricUnits.KILOBITS, 1, "bytes", "125");
        assertUnitConversion(MetricUnits.KILOBITS, 1000, "bytes", "125000");
        assertUnitConversion(MetricUnits.KILOBITS, 1024, "bytes", "128000");
        assertUnitConversion(MetricUnits.KILOBITS, 1000, "bytes", "125000");
        assertUnitConversion(MetricUnits.KILOBITS, 999, "bytes", "124875");
        assertUnitConversion(MetricUnits.MEGABITS, 5, "bytes", "625000");
        assertUnitConversion(MetricUnits.MEGABITS, 1024, "bytes", "1.28E8");
        assertUnitConversion(MetricUnits.GIGABITS, 2, "bytes", "2.5E8");

        // those that scale with 1024
        assertUnitConversion(MetricUnits.KIBIBITS, 1, "bytes", "128");
        assertUnitConversion(MetricUnits.KIBIBITS, 55, "bytes", "7040");
        assertUnitConversion(MetricUnits.MEBIBITS, 1, "bytes", "131072");
        assertUnitConversion(MetricUnits.MEBIBITS, 23, "bytes", "3014656");
        assertUnitConversion(MetricUnits.GIBIBITS, 1, "bytes", "1.34217728E8");
        assertUnitConversion(MetricUnits.GIBIBITS, 42, "bytes", "5.637144576E9");
    }

    @Test
    public void unitAnyBytesHasBaseUnitBytes() {
        assertUnitConversion(MetricUnits.BYTES, 1, "bytes", "1");
        assertUnitConversion(MetricUnits.BYTES, 555, "bytes", "555");
        assertUnitConversion(MetricUnits.KILOBYTES, 1, "bytes", "1000");
        assertUnitConversion(MetricUnits.KILOBYTES, 23, "bytes", "23000");
        assertUnitConversion(MetricUnits.MEGABYTES, 1, "bytes", "1000000");
        assertUnitConversion(MetricUnits.MEGABYTES, 0.5d, "bytes", "500000");
        assertUnitConversion(MetricUnits.GIGABYTES, 1, "bytes", "1.0E9");
        assertUnitConversion(MetricUnits.GIGABYTES, 0.025d, "bytes", "2.5E7");
    }

    @Test
    public void unitAnyTimeHasBaseUnitSeconds() {
        assertUnitConversion(MetricUnits.NANOSECONDS, 50000000, "seconds", "0.05");
        assertUnitConversion(MetricUnits.MICROSECONDS, 50400000, "seconds", "50.4");
        assertUnitConversion(MetricUnits.MILLISECONDS, 123, "seconds", "0.123");
        assertUnitConversion(MetricUnits.SECONDS, 42, "seconds", "42");
        assertUnitConversion(MetricUnits.MINUTES, 1, "seconds", "60");
        assertUnitConversion(MetricUnits.HOURS, 2, "seconds", "7200");
        assertUnitConversion(MetricUnits.DAYS, 1, "seconds", "86400");
    }

    private static final AtomicInteger nextNameId = new AtomicInteger(10);
    private void assertUnitConversion(String inputUnit, Number inputValue, String expectedUnit, String expectedValue) {
        Gauge<Number> gauge = () -> inputValue;
        String name = "test" + nextNameId.incrementAndGet();
        MetricID metricID = new MetricID(name);
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(inputUnit)
                .build();
        assertOutputEquals("# TYPE application_" + name + "_" + expectedUnit + " gauge\n" +
                "application_" + name + "_" + expectedUnit + " " + expectedValue + "\n", metricID, gauge, metadata);
        actual.getBuffer().setLength(0); // clean output so far to allow multiple usages of this in a single test
    }

    private void assertOutputEquals(String expected, MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
        assertEquals(expected, actual.getBuffer().toString());
    }

    private void assertOutputEqualsFile(String expectedFile, MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
        assertOutputEqualsFile(expectedFile);
    }

    private void assertOutputEqualsFile(String expectedFile) {
        assertEquals(readFile("/examples/" + expectedFile), actual.getBuffer().toString());
    }

    private String readFile(String file) {
        try {
            return new String(readAllBytes(Paths.get(getClass().getResource(file).toURI())));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}

