/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package fish.payara.microprofile.metrics.writer;

import static fish.payara.microprofile.metrics.Constants.GIBIBIT_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.GIGABIT_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.KIBIBIT_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.KILOBIT_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.MEBIBIT_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.MEGABIT_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.REGISTRY_NAMES_LIST;
import fish.payara.microprofile.metrics.MetricsHelper;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import static org.eclipse.microprofile.metrics.MetricUnits.BYTES;
import static org.eclipse.microprofile.metrics.MetricUnits.DAYS;
import static org.eclipse.microprofile.metrics.MetricUnits.GIBIBITS;
import static org.eclipse.microprofile.metrics.MetricUnits.GIGABITS;
import static org.eclipse.microprofile.metrics.MetricUnits.GIGABYTES;
import static org.eclipse.microprofile.metrics.MetricUnits.HOURS;
import static org.eclipse.microprofile.metrics.MetricUnits.KIBIBITS;
import static org.eclipse.microprofile.metrics.MetricUnits.KILOBITS;
import static org.eclipse.microprofile.metrics.MetricUnits.KILOBYTES;
import static org.eclipse.microprofile.metrics.MetricUnits.MEBIBITS;
import static org.eclipse.microprofile.metrics.MetricUnits.MEGABITS;
import static org.eclipse.microprofile.metrics.MetricUnits.MEGABYTES;
import static org.eclipse.microprofile.metrics.MetricUnits.MICROSECONDS;
import static org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS;
import static org.eclipse.microprofile.metrics.MetricUnits.MINUTES;
import static org.eclipse.microprofile.metrics.MetricUnits.NANOSECONDS;
import static org.eclipse.microprofile.metrics.MetricUnits.NONE;
import static org.eclipse.microprofile.metrics.MetricUnits.PERCENT;
import static org.eclipse.microprofile.metrics.MetricUnits.SECONDS;
import org.eclipse.microprofile.metrics.Timer;
import static fish.payara.microprofile.metrics.Constants.MINUTE_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.HOUR_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.SECOND_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.DAY_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.BYTE_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.KILOBYTE_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.MEGABYTE_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.GIGABYTE_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.MILLISECOND_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.MICROSECOND_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.NANOSECOND_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.APPENDED_SECONDS;
import static fish.payara.microprofile.metrics.Constants.APPENDED_PERCENT;
import static fish.payara.microprofile.metrics.Constants.APPENDED_BYTES;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrometheusMetricWriter implements OutputWriter {

    private final Writer writer;
    private final MetricsHelper helper;
    
    private static final Logger LOGGER = Logger.getLogger(PrometheusMetricWriter.class.getName());
    
    public PrometheusMetricWriter(Writer writer, MetricsHelper helper) {
        this.writer = writer;
        this.helper = helper;
    }

    @Override
    public void write(String registryName, String metricName) throws NoSuchMetricException, NoSuchRegistryException, IOException {
        StringBuilder builder = new StringBuilder();
        writeMetrics(builder, registryName, metricName);
        serialize(builder);
    }

    @Override
    public void write(String registryName) throws NoSuchRegistryException, IOException {
        StringBuilder builder = new StringBuilder();
        writeMetrics(builder, registryName);
        serialize(builder);
    }

    @Override
    public void write() throws IOException {
        StringBuilder builder = new StringBuilder();
        REGISTRY_NAMES_LIST.forEach(registryName -> {
            try {
                writeMetrics(builder, registryName);
            } catch (NoSuchRegistryException e) { // Ignore
            }
        });
        serialize(builder);
    }

    private void writeMetrics(StringBuilder builder, String registryName) throws NoSuchRegistryException {
        writeMetricMap(
                builder,
                registryName,
                helper.getMetricsAsMap(registryName),
                helper.getMetadataAsMap(registryName)
        );
    }

    private void writeMetrics(StringBuilder builder, String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        writeMetricMap(
                builder,
                registryName,
                helper.getMetricsAsMap(registryName, metricName),
                helper.getMetadataAsMap(registryName)
        );
    }

    private void writeMetricMap(StringBuilder builder, String registryName, Map<String, Metric> metricMap, Map<String, Metadata> metricMetadataMap) {
        for (Entry<String, Metric> entry : metricMap.entrySet()) {
            String name = registryName + ":" + entry.getKey();
            Metric metric = entry.getValue();
            String entryName = entry.getKey();

            Metadata metricMetaData = metricMetadataMap.get(entryName);

            String description = metricMetaData.getDescription() == null || metricMetaData.getDescription().trim().isEmpty() ? "" : metricMetaData.getDescription();

            String tags = metricMetaData.getTagsAsString();

            String unit = metricMetaData.getUnit();

            double conversionFactor;
            String appendUnit;

            if (unit == null || unit.trim().isEmpty() || unit.equals(NONE)) {
                conversionFactor = Double.NaN;
                appendUnit = null;
            } else {
                switch (unit) {
                    case NANOSECONDS:
                        conversionFactor = NANOSECOND_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    case MICROSECONDS:
                        conversionFactor = MICROSECOND_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    case SECONDS:
                        conversionFactor = SECOND_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    case MINUTES:
                        conversionFactor = MINUTE_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    case HOURS:
                        conversionFactor = HOUR_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    case DAYS:
                        conversionFactor = DAY_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    case PERCENT:
                        conversionFactor = Double.NaN;
                        appendUnit = APPENDED_PERCENT;
                        break;
                    case BYTES:
                        conversionFactor = BYTE_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case KILOBYTES:
                        conversionFactor = KILOBYTE_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case MEGABYTES:
                        conversionFactor = MEGABYTE_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case GIGABYTES:
                        conversionFactor = GIGABYTE_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case KILOBITS:
                        conversionFactor = KILOBIT_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case MEGABITS:
                        conversionFactor = MEGABIT_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case GIGABITS:
                        conversionFactor = GIGABIT_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case KIBIBITS:
                        conversionFactor = KIBIBIT_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case MEBIBITS:
                        conversionFactor = MEBIBIT_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case GIBIBITS:
                        conversionFactor = GIBIBIT_CONVERSION;
                        appendUnit = APPENDED_BYTES;
                        break;
                    case MILLISECONDS:
                        conversionFactor = MILLISECOND_CONVERSION;
                        appendUnit = APPENDED_SECONDS;
                        break;
                    default:
                        conversionFactor = Double.NaN;
                        appendUnit = "_" + unit;
                        break;
                }
            }

            if (Counter.class.isInstance(metric)) {
                PrometheusExporter.exportCounter(builder, (Counter) metric, name, description, tags);
            } else if (Gauge.class.isInstance(metric)) {
                PrometheusExporter.exportGauge(builder, (Gauge) metric, name, description, tags, conversionFactor, appendUnit);
            } else if (Timer.class.isInstance(metric)) {
                PrometheusExporter.exportTimer(builder, (Timer) metric, name, description, tags);
            } else if (Histogram.class.isInstance(metric)) {
                PrometheusExporter.exportHistogram(builder, (Histogram) metric, name, description, tags, conversionFactor, appendUnit);
            } else if (Meter.class.isInstance(metric)) {
                PrometheusExporter.exportMeter(builder, (Meter) metric, name, description, tags);
            } else {
                LOGGER.log(Level.WARNING, "Metric type '{0} for {1} is invalid", new Object[]{metric.getClass(), entryName});
            }
        }
    }

    private void serialize(StringBuilder builder) throws IOException {
        try {
            writer.write(builder.toString());
        } finally {
            writer.close();
        }
    }
}
