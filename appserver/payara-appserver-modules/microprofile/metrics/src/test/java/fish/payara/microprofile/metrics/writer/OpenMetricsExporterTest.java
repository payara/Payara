/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.writer;

import static java.nio.file.Files.readAllBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
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
    private final MetricExporter exporter = new OpenMetricsExporter(actual).in(MetricRegistry.APPLICATION_SCOPE);

    @Test
    public void exportCounter() {
        Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(80L);
        MetricID metricID = new MetricID("visitors");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("The number of unique visitors")
                .build();
        assertOutputEqualsContent("Counter.txt", metricID, counter, metadata);
    }





    @Test
    public void exportHistogram() {
        Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(2037L);
        when(histogram.getSum()).thenReturn(45678L);
        Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);
        when(snapshot.percentileValues()).thenReturn(getPercentilesArray());
        MetricID metricID = new MetricID("file_sizes");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("Users file size")
                .withUnit(MetricUnits.BYTES)
                .build();
        assertOutputEqualsContent("Histogram.txt", metricID, histogram, metadata);
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
        assertOutputEqualsContent("Gauge.txt", metricID, gauge, metadata);
    }

    @Test
    public void exportTimer() {
        Timer timer = mock(Timer.class);
        when(timer.getElapsedTime()).thenReturn(Duration.ofMillis(23L));
        when(timer.getCount()).thenReturn(80L);

        Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);

        when(snapshot.getMean()).thenReturn(415041d);
        when(snapshot.percentileValues()).thenReturn(getPercentilesArray());

        MetricID metricID = new MetricID("response_time");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withDescription("Server response time for /index.html")
                .withUnit(MetricUnits.NANOSECONDS)
                .build();
        assertOutputEqualsContent("Timer.txt", metricID, timer, metadata);
    }

    public Snapshot.PercentileValue[] getPercentilesArray() {
        Snapshot.PercentileValue[] percentileValues = null;
        double[] percentiles = {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};
        percentileValues = new Snapshot.PercentileValue[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            percentileValues[i] = new Snapshot.PercentileValue(percentiles[i], 0);
        }
        return percentileValues;
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
        MetricExporter base = exporter.in(MetricRegistry.APPLICATION_SCOPE);
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
        assertEquals("# TYPE common gauge\n" +
                "# HELP common description\n" +
                "common{a=\"b\"} 1\n" +
                "common{some=\"other\"} 2\n", actual.getBuffer().toString());
    }

    @Test
    public void quantilesAreAppendedOtherTags() {
        Histogram histogram = mock(Histogram.class);
        Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);
        when(snapshot.percentileValues()).thenReturn(getPercentilesArray());
        MetricID metricID = new MetricID("test6", new Tag("custom", "tag-value"));
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.MILLISECONDS)
                .build();
        exporter.export(metricID, histogram, metadata);
        String actualOutput = actual.getBuffer().toString();
        assertTrue(actualOutput.contains("test6_seconds{custom=\"tag-value\",quantile=\"0.5\"} 0"));
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
        assertOutputEquals("# TYPE test5_total counter\n" +
                "# HELP test5_total \n"+"test5_total{key=\"escape\\\\and\\\"and\\n\"} 13.0\n", metricID, counter, metadata);
    }

    @Test
    public void namesAreadyEndingWithSuffixAvoidAppendingSuffixAgain() {
        Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(13L);
        MetricID metricID = new MetricID("my_total");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("# TYPE my_total counter\n" +
                "# HELP my_total \n"+"my_total 13.0\n", metricID, counter, metadata);
    }

    @Test
    public void unitPerSecondUsesUnscaledValue() {
        Gauge<Double> perSec = () -> 2.3d;
        MetricID metricID = new MetricID("test7");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.PER_SECOND)
                .build();
        assertOutputEquals("# TYPE test7_per_second gauge\n" +
                "# HELP test7_per_second \n"+"test7_per_second 2.3\n", metricID, perSec, metadata);
    }

    @Test
    public void unitPercentMappedToRatioWithUnscaledValue() {
        Gauge<Double> perSec = () -> 2.3d;
        MetricID metricID = new MetricID("test8");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.PERCENT)
                .build();
        assertOutputEquals("# TYPE test8_ratio gauge\n" +
                "# HELP test8_ratio \n"+"test8_ratio 2.3\n", metricID, perSec, metadata);
    }

    @Test
    public void unitOfStyleA_Per_BAreKeptWithUnscaledValue() {
        Gauge<Double> aPerB = () -> 2.3d;
        MetricID metricID = new MetricID("test9");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit("meter_per_sec")
                .build();
        assertOutputEquals("# TYPE test9_meter_per_sec gauge\n" +
                "# HELP test9_meter_per_sec \n"+"test9_meter_per_sec 2.3\n", metricID, aPerB, metadata);
    }

    @Test
    public void unitNoneShouldNotBeAppendedToName() {
        Gauge<Long> gauge = () -> 13L;
        MetricID metricID = new MetricID("test2");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .withUnit(MetricUnits.NONE)
                .build();
        assertOutputEquals("# TYPE test2 gauge\n" +
                "# HELP test2 \n"+"test2 13\n", metricID, gauge, metadata);
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
        assertOutputEquals("# TYPE test1_total counter\n" +
                "# HELP test1_total \n"+"test1_total 13.0\n", metricID, counter, metadata);
    }

    @Test
    public void unitAnyBitsHasBaseUnitBytes() {
        assertUnitConversion(MetricUnits.BITS, 1, "bytes", "0.125");
        assertUnitConversion(MetricUnits.BITS, 64, "bytes", "8.0");

        // those that scale with 1000
        assertUnitConversion(MetricUnits.KILOBITS, 1, "bytes", "125.0");
        assertUnitConversion(MetricUnits.KILOBITS, 1000, "bytes", "125000.0");
        assertUnitConversion(MetricUnits.KILOBITS, 1024, "bytes", "128000.0");
        assertUnitConversion(MetricUnits.KILOBITS, 1000, "bytes", "125000.0");
        assertUnitConversion(MetricUnits.KILOBITS, 999, "bytes", "124875.0");
        assertUnitConversion(MetricUnits.MEGABITS, 5, "bytes", "625000.0");
        assertUnitConversion(MetricUnits.MEGABITS, 1024, "bytes", "1.28E8");
        assertUnitConversion(MetricUnits.GIGABITS, 2, "bytes", "2.5E8");

        // those that scale with 1024
        assertUnitConversion(MetricUnits.MEBIBITS, 1, "bytes", "131072.0");
        assertUnitConversion(MetricUnits.MEBIBITS, 23, "bytes", "3014656.0");
        assertUnitConversion(MetricUnits.GIBIBITS, 1, "bytes", "1.34217728E8");
        assertUnitConversion(MetricUnits.GIBIBITS, 42, "bytes", "5.637144576E9");
    }

    @Test
    public void unitAnyBytesHasBaseUnitBytes() {
        assertUnitConversion(MetricUnits.BYTES, 1, "bytes", "1");
        assertUnitConversion(MetricUnits.BYTES, 555, "bytes", "555");
        assertUnitConversion(MetricUnits.KILOBYTES, 1, "bytes", "1000.0");
        assertUnitConversion(MetricUnits.KILOBYTES, 23, "bytes", "23000.0");
        assertUnitConversion(MetricUnits.MEGABYTES, 1, "bytes", "1000000.0");
        assertUnitConversion(MetricUnits.MEGABYTES, 0.5d, "bytes", "500000.0");
    }

    @Test
    public void unitAnyTimeHasBaseUnitSeconds() {
        assertUnitConversion(MetricUnits.NANOSECONDS, 50000000, "seconds", "0.05");
        assertUnitConversion(MetricUnits.MICROSECONDS, 50400000, "seconds", "50.4");
        assertUnitConversion(MetricUnits.MILLISECONDS, 123, "seconds", "0.123");
        assertUnitConversion(MetricUnits.SECONDS, 42, "seconds", "42");
        assertUnitConversion(MetricUnits.MINUTES, 1, "seconds", "60.0");
        assertUnitConversion(MetricUnits.HOURS, 2, "seconds", "7200.0");
        assertUnitConversion(MetricUnits.DAYS, 1, "seconds", "86400.0");
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
        assertOutputEquals("# TYPE " + name + "_" + expectedUnit + " gauge\n" +
                "# HELP "+ name + "_" + expectedUnit+" \n" + name + "_" + expectedUnit + " " + expectedValue + "\n", metricID, gauge, metadata);
        actual.getBuffer().setLength(0); // clean output so far to allow multiple usages of this in a single test
    }

    private void assertOutputEquals(String expected, MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
        assertEquals(expected, actual.getBuffer().toString());
    }

    private void assertOutputEqualsContent(String expectedFile, MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
        assertOutputEqualsContent(expectedFile);
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

    private void assertOutputEqualsContent(String expectedFile) {
        List<String> lines = readLines("/examples/" + expectedFile);
        String resultingContent = actual.getBuffer().toString();
        for (String l : lines) {
            assertTrue(resultingContent.contains(l));
        }
    }

    private List<String> readLines(String file) {
        try {
            return Files.lines(Paths.get(getClass().getResource(file).toURI())).collect(Collectors.toList());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}

