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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.time.Duration;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Test;

import fish.payara.microprofile.metrics.writer.JsonExporter.Mode;

/**
 * Tests the correct output generation of {@link JsonExporter} for HTTP GET requests as specified in
 * https://download.eclipse.org/microprofile/microprofile-metrics-2.3/microprofile-metrics-spec-2.3.pdf.
 *
 * Note however, that the specification contains a handful of errors where examples contradict the rules given. For
 * example tags that do not occur in alphabetical order as demanded. Such mistakes were corrected in the expected output
 * examples extracted from the specification.
 *
 * Note also that the formatting differs from the specification. The indent is 4 instead of 2 spaces. Output starts with
 * an empty line (for some reason). Sometimes the specification has extra spaces (inconsistent) which do not occur in
 * actual output.
 *
 * @author Jan Bernitt
 */
public class JsonExporterGetTest {

    private final StringWriter actual = new StringWriter();
    private MetricExporter exporter = new JsonExporter(actual, Mode.GET, true);

    @Test
    public void exportCounter() {
        Counter hitCount = mock(Counter.class);
        when(hitCount.getCount()).thenReturn(45L);
        Counter hitCount2 = mock(Counter.class);
        when(hitCount2.getCount()).thenReturn(3L);
        Counter hitCount3 = mock(Counter.class);
        when(hitCount3.getCount()).thenReturn(4L);
        export(new MetricID("hitCount"), hitCount);
        export(new MetricID("hitCount", new Tag("servlet", "two")), hitCount2);
        export(new MetricID("hitCount", new Tag("store", "webshop"), new Tag("servlet", "three")), hitCount3);
        assertOutputEqualsFile("Counter.json");
    }

    @Test
    public void exportGauge() {
        Gauge<Double> responsePercentage1 =() -> 48.45632d;
        Gauge<Double> responsePercentage2 =() -> 26.23654d;
        Gauge<Double> responsePercentage3 =() -> 29.24554d;
        export(new MetricID("responsePercentage"), responsePercentage1);
        export(new MetricID("responsePercentage", new Tag("servlet", "two")), responsePercentage2);
        export(new MetricID("responsePercentage", new Tag("store", "webshop"), new Tag("servlet", "three")), responsePercentage3);
        assertOutputEqualsFile("Gauge.json");
    }



    @Test
    public void exportHistogram() {
        Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(2L);
        when(histogram.getSum()).thenReturn(42L);
        Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMean()).thenReturn(-799.0d);
        // example uses same values for both histograms so we can get away with just one
        // but conceptually those should be two different histogram instances
        export(new MetricID("daily_value_changes"), histogram);
        export(new MetricID("daily_value_changes", new Tag("servlet", "two")), histogram);
        assertOutputEqualsFile("Histogram.json");
    }

    @Test
    public void exportTimer() {
        Timer timer = mock(Timer.class);
        when(timer.getElapsedTime()).thenReturn(Duration.ofMillis(45678L));
        when(timer.getCount()).thenReturn(29382L);
        Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMean()).thenReturn(415041.00024926325d);
        // example uses same values for both timers so we can get away with just one
        // but conceptually those should be two different timer instances
        export(new MetricID("responseTime"), timer);
        export(new MetricID("responseTime", new Tag("servlet", "two")), timer);
        assertOutputEqualsFile("Timer.json");
    }



    /*
     * Below tests are no examples from the specification
     */

    @Test
    public void gaugesWithNonNumberValuesAreNotExported() {
        Gauge<Double> gauge = () -> 12.23;
        MetricID metricID = new MetricID("test3");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("{\n}", metricID, gauge, metadata);
    }

    @Test
    public void gaugesThatThrowIllegalStateExceptionWhenReadAreNotExported() {
        Gauge<Long> gauge = () -> { throw new IllegalStateException("test"); };
        MetricID metricID = new MetricID("test4");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("{\n}", metricID, gauge, metadata);
    }

    @Test
    public void multipeRepositoriesAreGroupedByNameMetricOption() {
        exporter = exporter.in(MetricRegistry.BASE_SCOPE);
        Gauge<Long> fooVal = () -> 1L;
        MetricID fooValID = new MetricID("fooVal", new Tag("store", "webshop"));
        export(fooValID, fooVal);
        exporter = exporter.in(MetricRegistry.APPLICATION_SCOPE);
        export(fooValID, fooVal);
        assertOutputEquals("{\n" +
                "    \"base\": {\n" +
                "        \"fooVal;store=webshop\": 1\n" +
                "    },\n" +
                "    \"application\": {\n" +
                "        \"fooVal;store=webshop\": 1\n" +
                "    }\n" +
                "}");
    }

    private void export(MetricID metricID, Metric metric) {
        exporter.export(metricID, metric, Metadata.builder().withName(metricID.getName()).build());
    }

    private void assertOutputEquals(String expected, MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
        assertOutputEquals(expected);
    }

    private void assertOutputEquals(String expected) {
        exporter.exportComplete();
        assertEquals(expected, actual.getBuffer().toString());
    }

    private void assertOutputEqualsFile(String expectedFile) {
        exporter.exportComplete();
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
