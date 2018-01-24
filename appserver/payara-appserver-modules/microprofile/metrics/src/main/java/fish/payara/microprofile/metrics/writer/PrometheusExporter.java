/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 * *****************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package fish.payara.microprofile.metrics.writer;

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
import static org.eclipse.microprofile.metrics.MetricUnits.BITS;
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
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.Timer;

public class PrometheusExporter {

    private static final String LF = "\n";
    private static final String LEFT_BRACES = "{";
    private static final String RIGHT_BRACES = "}";
    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String UNDERSCORE = "_";
    
    private static final String TYPE_TITLE = "# TYPE ";
    private static final String HELP_TITLE = "# HELP ";

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
    
    private static final String APPENDED_SECONDS = "_seconds";
    private static final String APPENDED_BYTES = "_bytes";
    private static final String APPENDED_PERCENT = "_percent";

    //Conversion factors
    private static final double NANOSECOND_CONVERSION = 1 / 1_000_000_000;
    private static final double MICROSECOND_CONVERSION = 1 / 1_000_000;
    private static final double MILLISECOND_CONVERSION = 1 / 1_000;
    private static final double SECOND_CONVERSION = 1;
    private static final double MINUTE_CONVERSION = 60;
    private static final double HOUR_CONVERSION = 60 * 60;
    private static final double DAY_CONVERSION = 60 * 60 * 24;
    private static final double BIT_CONVERSION = 1/8;
    private static final double KILOBIT_CONVERSION = 1_000 / 8;
    private static final double MEGABIT_CONVERSION = 1_000_000 / 8;
    private static final double GIGABIT_CONVERSION = 1_000_000_000 / 8;
    private static final double KIBIBIT_CONVERSION = 128;
    private static final double MEBIBIT_CONVERSION = 128 * 1024;
    private static final double GIBIBIT_CONVERSION = 128 * 1024 * 1024;
    private static final double BYTE_CONVERSION = 1;
    private static final double KILOBYTE_CONVERSION = 1024;
    private static final double MEGABYTE_CONVERSION = 1024 * 1024;
    private static final double GIGABYTE_CONVERSION = 1024 * 1024 * 1024;
    
    private static final Logger LOGGER = Logger.getLogger(PrometheusExporter.class.getName());
    
    private final StringBuilder builder;
    
    public PrometheusExporter(StringBuilder builder){
        this.builder = builder;
    }

    public void exportCounter(Counter counter, String name, String description, String tags) {
        writeTypeHelpValueLine(name, COUNTER.toString(), description, counter.getCount(), tags);
    }

    public void exportGauge(Gauge<?> gauge, String name, String description, String tags, String unit) {
        Number value;
        Object gaugeValue;
        try {
            gaugeValue = gauge.getValue();
        } catch (IllegalStateException e) {
            // The forwarding gauge is unloaded
            return;
        }
        if (!Number.class.isInstance(gaugeValue)) {
            LOGGER.log(Level.FINER, "Skipping Prometheus output for Gauge: {0} of type {1}", new Object[]{name, gaugeValue.getClass()});
            return;
        }
        value = (Number) gaugeValue;
        Double conversionFactor = getConversionFactor(unit);
        if (!Double.isNaN(conversionFactor)) {
            value = value.doubleValue() * conversionFactor;
        }
        writeTypeHelpValueLine(name, GAUGE.toString(), description, value, tags, getAppendUnit(unit));
    }

    public void exportHistogram(Histogram histogram, String name, String description, String tags, String unit) {
        exportSampling(histogram, name, description, tags, unit);
    }

    public void exportMeter(Meter meter, String name, String description, String tags) {
        exportCounting(meter, name, description, tags);
        exportMetered(meter, name, description, tags);
    }

    public void exportTimer(Timer timer, String name, String description, String tags, String unit) {
        exportMetered(timer, name, description, tags);
        exportSampling(timer, name, description, tags, unit);
    }

    private void exportCounting(Counting counting, String name, String description, String tags) {
        writeTypeHelpValueLine(name + TOTAL_SUFFIX, COUNTER.toString(), description, counting.getCount(), tags);
    }

    private void exportMetered(Metered metered, String name, String description, String tags) {
        writeTypeValueLine(name + RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getMeanRate(), tags);
        writeTypeValueLine(name + ONE_MIN_RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getOneMinuteRate(), tags);
        writeTypeValueLine(name + FIVE_MIN_RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getFiveMinuteRate(), tags);
        writeTypeValueLine(name + FIFTEEN_MIN_RATE + MetricUnits.PER_SECOND, GAUGE.toString(), metered.getFifteenMinuteRate(), tags);
    }

    private void exportSampling(Sampling sampling, String name, String description, String tags, String unit) {

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

        Double conversionFactor = getConversionFactor(unit);
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

        String appendUnit = getAppendUnit(unit);
        writeTypeValueLine(name + MEAN_SUFFIX, GAUGE.toString(), mean, tags, appendUnit);
        writeTypeValueLine(name + MAX_SUFFIX, GAUGE.toString(), max, tags, appendUnit);
        writeTypeValueLine(name + MIN_SUFFIX, GAUGE.toString(), min, tags, appendUnit);
        writeTypeValueLine(name + STDDEV_SUFFIX, GAUGE.toString(), stdDev, tags, appendUnit);

        writeTypeLine(name, SUMMARY, appendUnit);
        writeHelpLine(name, description, appendUnit);
        if (Counting.class.isInstance(sampling)) {
            writeValueLine(name, ((Counting) sampling).getCount(), tags, appendUnit == null ? COUNT_SUFFIX : appendUnit + COUNT_SUFFIX);
        }

        writeValueLine(name, median, tags, new Tag(QUANTILE, "0.5"), appendUnit);
        writeValueLine(name, percentile75th, tags, new Tag(QUANTILE, "0.75"), appendUnit);
        writeValueLine(name, percentile95th, tags, new Tag(QUANTILE, "0.95"), appendUnit);
        writeValueLine(name, percentile98th, tags, new Tag(QUANTILE, "0.98"), appendUnit);
        writeValueLine(name, percentile99th, tags, new Tag(QUANTILE, "0.99"), appendUnit);
        writeValueLine(name, percentile999th, tags, new Tag(QUANTILE, "0.999"), appendUnit);
    }

    private void writeTypeHelpValueLine(String name, String type, String description, Number value, String tags) {
        writeTypeHelpValueLine(name, type, description, value, tags, null);
    }

    private void writeTypeHelpValueLine(String name, String type, String description, Number value, String tags, String appendUnit) {
        writeTypeLine(name, type, appendUnit);
        writeHelpLine(name, description, appendUnit);
        writeValueLine(name, value, tags, appendUnit);
    }

    private void writeTypeValueLine(String name, String type, Number value, String tags) {
        writeTypeValueLine(name, type, value, tags, null);
    }

    private void writeTypeValueLine(String name, String type, Number value, String tags, String appendUnit) {
        writeTypeLine(name, type, appendUnit);
        writeValueLine(name, value, tags, appendUnit);
    }

    private void writeValueLine(String name, Number value, String tags, Tag quantile, String appendUnit) {
        String qunatileKeyValue = quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        tags = tags == null || tags.isEmpty() ? qunatileKeyValue : tags + COMMA + qunatileKeyValue;
        writeValueLine(name, value, tags, appendUnit);
    }

    private void writeValueLine(String name, Number value, String tags, String appendUnit) {
        builder.append(sanitizeMetricName(name));
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        if (tags != null && tags.length() > 0) {
            builder.append(LEFT_BRACES).append(tags).append(RIGHT_BRACES);
        }
        builder.append(SPACE);
        builder.append(value);
        builder.append(LF);
    }

    private void writeHelpLine(String name, String description, String appendUnit) {
        if (description != null && !description.isEmpty()) {
            builder.append(HELP_TITLE);
            builder.append(sanitizeMetricName(name));
            if (appendUnit != null) {
                builder.append(appendUnit);
            }
            builder.append(SPACE);
            builder.append(description);
            builder.append(LF);
        }
    }

    private void writeTypeLine(String name, String type, String appendUnit) {
        builder.append(TYPE_TITLE);
        builder.append(sanitizeMetricName(name));
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        builder.append(SPACE);
        builder.append(type);
        builder.append(LF);
    }

    private String sanitizeMetricName(String name) {
        //Translation rules :
        //camelCase is translated to camel_case
        String out = name.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
        //Dot (.), Space ( ), Dash (-) are translated to underscore (_)
        //Double underscore is translated to single underscore
        out = out.replaceAll("[-_.\\s]+", "_");
        //Colon-underscore (:_) is translated to single colon
        out = out.replaceAll(":_", ":");
        return out;
    }

    private double getConversionFactor(String unit) {
        double conversionFactor;
        if (unit == null || unit.trim().isEmpty() || unit.equals(NONE)) {
            conversionFactor = Double.NaN;
        } else {
            switch (unit) {
                case NANOSECONDS:
                    conversionFactor = NANOSECOND_CONVERSION;
                    break;
                case MICROSECONDS:
                    conversionFactor = MICROSECOND_CONVERSION;
                    break;
                case MILLISECONDS:
                    conversionFactor = MILLISECOND_CONVERSION;
                    break;
                case SECONDS:
                    conversionFactor = SECOND_CONVERSION;
                    break;
                case MINUTES:
                    conversionFactor = MINUTE_CONVERSION;
                    break;
                case HOURS:
                    conversionFactor = HOUR_CONVERSION;
                    break;
                case DAYS:
                    conversionFactor = DAY_CONVERSION;
                    break;
                case PERCENT:
                    conversionFactor = Double.NaN;
                    break;
                case BITS:
                    conversionFactor = BIT_CONVERSION;
                    break;
                case KILOBITS:
                    conversionFactor = KILOBIT_CONVERSION;
                    break;
                case MEGABITS:
                    conversionFactor = MEGABIT_CONVERSION;
                    break;
                case GIGABITS:
                    conversionFactor = GIGABIT_CONVERSION;
                    break;
                case KIBIBITS:
                    conversionFactor = KIBIBIT_CONVERSION;
                    break;
                case MEBIBITS:
                    conversionFactor = MEBIBIT_CONVERSION;
                    break;
                case GIBIBITS:
                    conversionFactor = GIBIBIT_CONVERSION;
                    break;
                case BYTES:
                    conversionFactor = BYTE_CONVERSION;
                    break;
                case KILOBYTES:
                    conversionFactor = KILOBYTE_CONVERSION;
                    break;
                case MEGABYTES:
                    conversionFactor = MEGABYTE_CONVERSION;
                    break;
                case GIGABYTES:
                    conversionFactor = GIGABYTE_CONVERSION;
                    break;
                default:
                    conversionFactor = Double.NaN;
                    break;
            }
        }
        return conversionFactor;
    }

    private String getAppendUnit(String unit) {
        String appendUnit;
        if (unit == null || unit.trim().isEmpty() || unit.equals(NONE)) {
            appendUnit = null;
        } else {
            switch (unit) {
                case NANOSECONDS:
                case MICROSECONDS:
                case MILLISECONDS:
                case SECONDS:
                case MINUTES:
                case HOURS:
                case DAYS:
                    appendUnit = APPENDED_SECONDS;
                    break;
                case PERCENT:
                    appendUnit = APPENDED_PERCENT;
                    break;
                case BITS:
                case KILOBITS:
                case MEGABITS:
                case GIGABITS:
                case KIBIBITS:
                case MEBIBITS:
                case GIBIBITS:
                case BYTES:
                case KILOBYTES:
                case MEGABYTES:
                case GIGABYTES:         
                    appendUnit = APPENDED_BYTES;
                    break;
                default:
                    appendUnit = UNDERSCORE + unit;
                    break;
            }
        }
        return appendUnit;
    }

}
