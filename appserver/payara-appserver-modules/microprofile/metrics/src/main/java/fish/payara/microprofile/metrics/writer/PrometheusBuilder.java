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
import static fish.payara.microprofile.metrics.Constants.FIFTEEN_MIN_RATE;
import static fish.payara.microprofile.metrics.Constants.FIVE_MIN_RATE;
import static fish.payara.microprofile.metrics.Constants.NANOSECOND_CONVERSION;
import static fish.payara.microprofile.metrics.Constants.ONE_MIN_RATE;
import static fish.payara.microprofile.metrics.Constants.QUANTILE;
import static fish.payara.microprofile.metrics.Constants.RATE;
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

public class PrometheusBuilder {

    private static final Logger LOGGER = Logger.getLogger(PrometheusBuilder.class.getName());
    
    public static void buildGauge(StringBuilder builder, String name, Gauge<?> gauge, String description, Double conversionFactor, String tags, String appendUnit) {
        // Skip non number values
        Number gaugeValNumber;
        Object gaugeValue;
        try {
            gaugeValue = gauge.getValue();
        } catch (IllegalStateException e) {
            // The forwarding gauge is likely unloaded
            return;
        }
        if (!Number.class.isInstance(gaugeValue)) {
            LOGGER.log(Level.FINER, "Skipping Prometheus output for Gauge: {0} of type {1}", new Object[]{name, gauge.getValue().getClass()});
            return;
        }
        gaugeValNumber = (Number) gaugeValue;
        if (!(Double.isNaN(conversionFactor))) {
            gaugeValNumber = gaugeValNumber.doubleValue() * conversionFactor;
        }
        getPromTypeLine(builder, name, GAUGE.toString(), appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        getPromValueLine(builder, name, gaugeValNumber, tags, appendUnit);
    }

    public static void buildCounter(StringBuilder builder, String name, Counter counter, String description, String tags) {
        getPromTypeLine(builder, name, COUNTER.toString());
        getPromHelpLine(builder, name, description);
        getPromValueLine(builder, name, counter.getCount(), tags);
    }

    public static void buildTimer(StringBuilder builder, String name, Timer timer, String description, String tags) {
        buildMetered(builder, name, timer, description, tags);
        buildSampling(builder, name, timer, description, NANOSECOND_CONVERSION, tags, APPENDED_SECONDS);
    }

    public static void buildHistogram(StringBuilder builder, String name, Histogram histogram, String description, Double conversionFactor, String tags,
            String appendUnit) {
        buildSampling(builder, name, histogram, description, conversionFactor, tags, appendUnit);
    }

    public static void buildMeter(StringBuilder builder, String name, Meter meter, String description, String tags) {
        buildCounting(builder, name, meter, description, tags);
        buildMetered(builder, name, meter, description, tags);
    }

    private static void buildSampling(StringBuilder builder, String name, Sampling sampling, String description, Double conversionFactor, String tags,
            String appendUnit) {

        double meanVal = sampling.getSnapshot().getMean();
        double maxVal = sampling.getSnapshot().getMax();
        double minVal = sampling.getSnapshot().getMin();
        double stdDevVal = sampling.getSnapshot().getStdDev();
        double medianVal = sampling.getSnapshot().getMedian();
        double percentile75th = sampling.getSnapshot().get75thPercentile();
        double percentile95th = sampling.getSnapshot().get95thPercentile();
        double percentile98th = sampling.getSnapshot().get98thPercentile();
        double percentile99th = sampling.getSnapshot().get99thPercentile();
        double percentile999th = sampling.getSnapshot().get999thPercentile();

        if (!(Double.isNaN(conversionFactor))) {
            meanVal = sampling.getSnapshot().getMean() * conversionFactor;
            maxVal = sampling.getSnapshot().getMax() * conversionFactor;
            minVal = sampling.getSnapshot().getMin() * conversionFactor;
            stdDevVal = sampling.getSnapshot().getStdDev() * conversionFactor;
            medianVal = sampling.getSnapshot().getMedian() * conversionFactor;
            percentile75th = sampling.getSnapshot().get75thPercentile() * conversionFactor;
            percentile95th = sampling.getSnapshot().get95thPercentile() * conversionFactor;
            percentile98th = sampling.getSnapshot().get98thPercentile() * conversionFactor;
            percentile99th = sampling.getSnapshot().get99thPercentile() * conversionFactor;
            percentile999th = sampling.getSnapshot().get999thPercentile() * conversionFactor;
        }

        String lineName = name + "_mean";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, meanVal, tags, appendUnit);
        lineName = name + "_max";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, maxVal, tags, appendUnit);
        lineName = name + "_min";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, minVal, tags, appendUnit);
        lineName = name + "_stddev";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, stdDevVal, tags, appendUnit);

        getPromTypeLine(builder, name, "summary", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        if (Counting.class.isInstance(sampling)) {
            getPromValueLine(builder, name, ((Counting) sampling).getCount(), tags, appendUnit == null ? "_count" : appendUnit + "_count");
        }
        getPromValueLine(builder, name, medianVal, tags, new Tag(QUANTILE, "0.5"), appendUnit);
        getPromValueLine(builder, name, percentile75th, tags, new Tag(QUANTILE, "0.75"), appendUnit);
        getPromValueLine(builder, name, percentile95th, tags, new Tag(QUANTILE, "0.95"), appendUnit);
        getPromValueLine(builder, name, percentile98th, tags, new Tag(QUANTILE, "0.98"), appendUnit);
        getPromValueLine(builder, name, percentile99th, tags, new Tag(QUANTILE, "0.99"), appendUnit);
        getPromValueLine(builder, name, percentile999th, tags, new Tag(QUANTILE, "0.999"), appendUnit);
    }

    private static void buildCounting(StringBuilder builder, String name, Counting counting, String description, String tags) {
        String lineName = name + "_total";
        getPromTypeLine(builder, lineName, COUNTER.toString());
        getPromHelpLine(builder, lineName, description);
        getPromValueLine(builder, lineName, counting.getCount(), tags);
    }

    private static void buildMetered(StringBuilder builder, String name, Metered metered, String description, String tags) {
        String lineName = name + RATE + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getMeanRate(), tags);

        lineName = name + ONE_MIN_RATE + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getOneMinuteRate(), tags);

        lineName = name + FIVE_MIN_RATE + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getFiveMinuteRate(), tags);

        lineName = name + FIFTEEN_MIN_RATE + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getFifteenMinuteRate(), tags);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags) {
        getPromValueLine(builder, name, value, tags, null);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, Tag quantile, String appendUnit) {

        if (tags == null || tags.isEmpty()) {
            tags = quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        } else {
            tags = tags + "," + quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        }
        getPromValueLine(builder, name, value, tags, appendUnit);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, String appendUnit) {

        String metricName = getPrometheusMetricName(name);

        builder.append(metricName);

        if (appendUnit != null) {
            builder.append(appendUnit);
        }

        if (tags != null && tags.length() > 0) {
            builder.append("{").append(tags).append("}");
        }

        builder.append(" ").append(value).append('\n');
    }

    private static void getPromHelpLine(StringBuilder builder, String name, String description) {
        getPromHelpLine(builder, name, description, null);
    }

    private static void getPromHelpLine(StringBuilder builder, String name, String description, String appendUnit) {
        String metricName = getPrometheusMetricName(name);
        if (description != null && !description.isEmpty()) {
            builder.append("# HELP ").append(metricName);

            if (appendUnit != null) {
                builder.append(appendUnit);
            }
            builder.append(" ").append(description).append("\n");
        }
    }

    private static void getPromTypeLine(StringBuilder builder, String name, String type) {
        getPromTypeLine(builder, name, type, null);
    }

    private static void getPromTypeLine(StringBuilder builder, String name, String type, String appendUnit) {

        String metricName = getPrometheusMetricName(name);
        builder.append("# TYPE ").append(metricName);
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        builder.append(" ").append(type).append("\n");
    }

    /*
     * Create the Prometheus metric name by sanitizing some characters
     */
    private static String getPrometheusMetricName(String name) {

        String out = name.replaceAll("(?<!^|:)(\\p{Upper})(?=\\p{Lower})", "_$1");
        out = out.replaceAll("(?<=\\p{Lower})(\\p{Upper})", "_$1").toLowerCase();
        out = out.replaceAll("[-_.\\s]+", "_");
        out = out.replaceAll("^_*(.*?)_*$", "$1");

        return out;
    }
}
