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

import static fish.payara.microprofile.metrics.Constants.APPENDED_SECONDS;
import static fish.payara.microprofile.metrics.Constants.NANOSECOND_CONVERSION;
import fish.payara.microprofile.metrics.Tag;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.Timer;

public class PrometheusExporter {
    
    private static final String LF = "\n";
    private static final String SPACE = " ";
    
    private static final String SUMMARY = "summary";
    private static final String COUNT_SUFFIX = "_count";
    private static final String MEAN_SUFFIX = "_mean";
    private static final String MAX_SUFFIX = "_max";
    private static final String MIN_SUFFIX = "_min";
    private static final String STDDEV_SUFFIX = "_stddev";
    private static final String TOTAL_SUFFIX = "_total";
        
    private static final String QUANTILE = "quantile";
    private static final String RATE = "_rate_";
    private static final String ONE_MIN_RATE = "_one_min_rate_";
    private static final String FIVE_MIN_RATE = "_five_min_rate_";
    private static final String FIFTEEN_MIN_RATE = "_fifteen_min_rate_";
    
    private static final Logger LOGGER = Logger.getLogger(PrometheusExporter.class.getName());
    
    public static void exportCounter(StringBuilder builder, Counter counter, String name, String description, String tags) {
        writeTypeHelpValueLine(builder, name, COUNTER.toString(), description, counter.getCount(), tags);
    }    
    
    public static void exportGauge(StringBuilder builder, Gauge<?> gauge, String name, String description, String tags, Double conversionFactor, String appendUnit) {
        Number value;
        Object gaugeValue;
        try {
            gaugeValue = gauge.getValue();
        } catch (IllegalStateException e) {
            // The forwarding gauge is unloaded
            return;
        }
        if (!Number.class.isInstance(gaugeValue)) {
            LOGGER.log(Level.FINER, "Skipping Prometheus output for Gauge: {0} of type {1}", new Object[]{name, gauge.getValue().getClass()});
            return;
        }
        value = (Number) gaugeValue;
        if (!Double.isNaN(conversionFactor)) {
            value = value.doubleValue() * conversionFactor;
        }
        writeTypeHelpValueLine(builder, name, GAUGE.toString(), description, value, tags, appendUnit);
    }

    public static void exportHistogram(StringBuilder builder, Histogram histogram, String name, String description, String tags, Double conversionFactor, String appendUnit) {
        exportSampling(builder, histogram, name, description, tags, conversionFactor, appendUnit);
    }

    public static void exportMeter(StringBuilder builder, Meter meter, String name, String description, String tags) {
        exportCounting(builder, meter, name, description, tags);
        exportMetered(builder, meter, name, description, tags);
    }

    public static void exportTimer(StringBuilder builder, Timer timer, String name, String description, String tags) {
        exportMetered(builder, timer, name, description, tags);
        exportSampling(builder, timer, name, description, tags, NANOSECOND_CONVERSION, APPENDED_SECONDS);
    }

    private static void exportCounting(StringBuilder builder, Counting counting, String name, String description, String tags) {
        writeTypeHelpValueLine(builder, name + TOTAL_SUFFIX, COUNTER.toString(), description, counting.getCount(), tags);
    }

    private static void exportMetered(StringBuilder builder, Metered metered, String name, String description, String tags) {
        writeTypeValueLine(builder, name + RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getMeanRate(), tags);
        writeTypeValueLine(builder, name + ONE_MIN_RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getOneMinuteRate(), tags);
        writeTypeValueLine(builder, name + FIVE_MIN_RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getFiveMinuteRate(), tags);
        writeTypeValueLine(builder, name + FIFTEEN_MIN_RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getFifteenMinuteRate(), tags);
    }

    private static void exportSampling(StringBuilder builder, Sampling sampling, String name, String description, String tags, Double conversionFactor, String appendUnit) {
        double mean = sampling.getSnapshot().getMean();
        double max = sampling.getSnapshot().getMax();
        double min = sampling.getSnapshot().getMin();
        double stdDev = sampling.getSnapshot().getStdDev();
        double median = sampling.getSnapshot().getMedian();
        double percentile75th = sampling.getSnapshot().get75thPercentile();
        double percentile95th = sampling.getSnapshot().get95thPercentile();
        double percentile98th = sampling.getSnapshot().get98thPercentile();
        double percentile99th = sampling.getSnapshot().get99thPercentile();
        double percentile999th = sampling.getSnapshot().get999thPercentile();

        if (!Double.isNaN(conversionFactor)) {
            mean *= conversionFactor;
            max *= conversionFactor;
            min *= conversionFactor;
            stdDev *= conversionFactor;
            median *= conversionFactor;
            percentile75th *= conversionFactor;
            percentile95th *= conversionFactor;
            percentile98th *= conversionFactor;
            percentile99th *= conversionFactor;
            percentile999th *= conversionFactor;
        }

        writeTypeValueLine(builder, name + MEAN_SUFFIX, GAUGE.toString(), mean, tags, appendUnit);
        writeTypeValueLine(builder, name + MAX_SUFFIX, GAUGE.toString(), max, tags, appendUnit);
        writeTypeValueLine(builder, name + MIN_SUFFIX, GAUGE.toString(), min, tags, appendUnit);
        writeTypeValueLine(builder, name + STDDEV_SUFFIX, GAUGE.toString(), stdDev, tags, appendUnit);

        writeTypeLine(builder, name, SUMMARY, appendUnit);
        writeHelpLine(builder, name, description, appendUnit);
        if (Counting.class.isInstance(sampling)) {
            writeValueLine(builder, name, ((Counting) sampling).getCount(), tags, appendUnit == null ? COUNT_SUFFIX : appendUnit + COUNT_SUFFIX);
        }
        
        writeValueLine(builder, name, median, tags, new Tag(QUANTILE, "0.5"), appendUnit);
        writeValueLine(builder, name, percentile75th, tags, new Tag(QUANTILE, "0.75"), appendUnit);
        writeValueLine(builder, name, percentile95th, tags, new Tag(QUANTILE, "0.95"), appendUnit);
        writeValueLine(builder, name, percentile98th, tags, new Tag(QUANTILE, "0.98"), appendUnit);
        writeValueLine(builder, name, percentile99th, tags, new Tag(QUANTILE, "0.99"), appendUnit);
        writeValueLine(builder, name, percentile999th, tags, new Tag(QUANTILE, "0.999"), appendUnit);
    }

    private static void writeTypeHelpValueLine(StringBuilder builder, String name, String type, String description, Number value, String tags) {
        writeTypeHelpValueLine(builder, name, type, description, value, tags, null);
    }
        
    private static void writeTypeHelpValueLine(StringBuilder builder, String name, String type, String description, Number value, String tags, String appendUnit) {
        writeTypeLine(builder, name, type, appendUnit);
        writeHelpLine(builder, name, description, appendUnit);
        writeValueLine(builder, name, value, tags, appendUnit);
    }
    
    private static void writeTypeValueLine(StringBuilder builder, String name, String type, Number value, String tags) {
        writeTypeValueLine(builder, name, type, value, tags, null);
    }
    
    private static void writeTypeValueLine(StringBuilder builder, String name, String type, Number value, String tags, String appendUnit) {
        writeTypeLine(builder, name, type, appendUnit);
        writeValueLine(builder, name, value, tags, appendUnit);
    }
    
    private static void writeValueLine(StringBuilder builder, String name, Number value, String tags, Tag quantile, String appendUnit) {
        String qunatileKeyValue = quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        tags = tags == null || tags.isEmpty() ? qunatileKeyValue : tags + "," + qunatileKeyValue;
        writeValueLine(builder, name, value, tags, appendUnit);
    }

    private static void writeValueLine(StringBuilder builder, String name, Number value, String tags, String appendUnit) {
        builder.append(sanitizeMetricName(name));
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        if (tags != null && tags.length() > 0) {
            builder.append("{").append(tags).append("}");
        }
        builder.append(SPACE);
        builder.append(value);
        builder.append(LF);
    }

    private static void writeHelpLine(StringBuilder builder, String name, String description, String appendUnit) {
        if (description != null && !description.isEmpty()) {
            builder.append("# HELP ");
            builder.append(sanitizeMetricName(name));
            if (appendUnit != null) {
                builder.append(appendUnit);
            }
            builder.append(SPACE);
            builder.append(description);
            builder.append(LF);
        }
    }

    private static void writeTypeLine(StringBuilder builder, String name, String type, String appendUnit) {
        builder.append("# TYPE ");
        builder.append(sanitizeMetricName(name));
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        builder.append(SPACE);
        builder.append(type);
        builder.append(LF);
    }

    private static String sanitizeMetricName(String name) {
        String out = name.replaceAll("(?<!^|:)(\\p{Upper})(?=\\p{Lower})", "_$1");
        out = out.replaceAll("(?<=\\p{Lower})(\\p{Upper})", "_$1").toLowerCase();
        out = out.replaceAll("[-_.\\s]+", "_");
        out = out.replaceAll("^_*(.*?)_*$", "$1");
        return out;
    }
}
