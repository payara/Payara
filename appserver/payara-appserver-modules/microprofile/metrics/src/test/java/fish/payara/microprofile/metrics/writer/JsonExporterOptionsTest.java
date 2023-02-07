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

import java.io.StringWriter;
import java.nio.file.Paths;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.Test;

import fish.payara.microprofile.metrics.writer.JsonExporter.Mode;

/**
 * Tests the correct output generation of {@link JsonExporter} for HTTP GET requests as specified in
 * https://download.eclipse.org/microprofile/microprofile-metrics-2.3/microprofile-metrics-spec-2.3.pdf.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class JsonExporterOptionsTest {

    private final StringWriter actual = new StringWriter();
    private MetricExporter exporter = new JsonExporter(actual, Mode.OPTIONS, true);

    @Test
    public void singleMetricOption() {
        Gauge<Long> fooVal = () -> 1L;
        MetricID fooValID = new MetricID("fooVal", new Tag("store", "webshop"));
        Metadata fooValMeta = Metadata.builder()
                .withName("fooVal")
                .withDescription("The size of foo after each request")
                .withUnit(MetricUnits.MILLISECONDS)
                .build();
        export(fooValID, fooVal, fooValMeta);
        assertOutputEqualsFile("Options1.json");
    }

    @Test
    public void multipleMetricOptionsAreGroupedByName() {
        Gauge<Long> fooVal = () -> 1L;
        MetricID fooValID = new MetricID("fooVal", new Tag("store", "webshop"));
        Metadata fooValMeta = Metadata.builder()
                .withName("fooVal")
                .withDescription("The average duration of foo requests during last 5 minutes")
                .withUnit(MetricUnits.MILLISECONDS)

                .build();
        Gauge<Long> barVal1 = () -> 2L;
        Gauge<Long> barVal2 = () -> 3L;
        MetricID barValID1 = new MetricID("barVal", new Tag("store", "webshop"), new Tag("component", "backend"));
        MetricID barValID2 = new MetricID("barVal", new Tag("store", "webshop"), new Tag("component", "frontend"));
        Metadata barValMeta = Metadata.builder()
                .withName("barVal")
                .withUnit(MetricUnits.MEGABYTES)
                .build();

        export(fooValID, fooVal, fooValMeta);
        export(barValID1, barVal1, barValMeta);
        export(barValID2, barVal2, barValMeta);
        assertOutputEqualsFile("Options2.json");
    }

    /*
     * Below tests are no examples from the specification
     */

    @Test
    public void multipeRepositoriesAreGroupedByNameMetricOption() {
        exporter = exporter.in(MetricRegistry.Type.BASE);
        Gauge<Long> fooVal = () -> 1L;
        MetricID fooValID = new MetricID("fooVal", new Tag("store", "webshop"));
        Metadata fooValMeta = Metadata.builder()
                .withName("fooVal")
                .withDescription("The size of foo after each request")
                .withUnit(MetricUnits.MILLISECONDS)
                .build();
        export(fooValID, fooVal, fooValMeta);
        exporter = exporter.in(MetricRegistry.Type.APPLICATION);
        export(fooValID, fooVal, fooValMeta);
        assertOutputEqualsFile("Options3.json");
    }

    @Test
    public void gaugesThatThrowIllegalStateExceptionWhenReadDoExportMetadata() {
        Gauge<Long> gauge = () -> { throw new IllegalStateException("test"); };
        MetricID metricID = new MetricID("test4");
        Metadata metadata = Metadata.builder()
                .withName(metricID.getName())
                .build();
        assertOutputEquals("{\n" +
                "    \"test4\": {\n" +
                "        \"unit\": \"none\",\n" +
                "        \"type\": \"gauge\"\n" +
                "    }\n" +
                "}", metricID, gauge, metadata);
    }

    private void export(MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
    }

    private void assertOutputEquals(String expected, MetricID metricID, Metric metric, Metadata metadata) {
        exporter.export(metricID, metric, metadata);
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
