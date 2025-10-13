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

import static fish.payara.microprofile.metrics.MetricUnitsUtils.scaleToBaseUnit;

import fish.payara.microprofile.metrics.impl.HistogramImpl;
import fish.payara.microprofile.metrics.impl.TimerImpl;
import fish.payara.microprofile.metrics.impl.WeightedSnapshot;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

/**
 * Writes {@link Metric}s according to the OpenMetrics standard as defined in <a href=
 * "https://download.eclipse.org/microprofile/microprofile-metrics-2.3/microprofile-metrics-spec-2.3.pdf">microprofile-metrics-spec-2.3.pdf</a>.
 *
 * The <code>append</code> method code is organised so that its output is reflected in the use of
 * {@link #appendHELP(String, Metadata)}, {@link #appendTYPE(String, OpenMetricsType)} and
 * {@link #appendValue(String, Tag[], Number)} which each emit a single output line.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class OpenMetricsExporter implements MetricExporter {

    protected enum OpenMetricsType {
        counter, gauge, summary, histogram
    }

    protected final String scope;

    protected final PrintWriter out;
    protected final Set<String> typeWrittenByGlobalName;
    protected final Set<String> helpWrittenByGlobalName;
    
    private static final String GC_TOTAL_ID = "gc_total";
    
    private static final String GC_TIME_SECONDS_TOTAL_ID = "gc_time_seconds_total";

    public OpenMetricsExporter(Writer out) {
        this(null, out instanceof PrintWriter ? (PrintWriter) out : new PrintWriter(out), new HashSet<>(), new HashSet<>());
    }

    protected OpenMetricsExporter(String scope, PrintWriter out, Set<String> typeWrittenByGlobalName,
                                  Set<String> helpWrittenByGlobalName) {
        this.scope = scope;
        this.out = out;
        this.typeWrittenByGlobalName = typeWrittenByGlobalName;
        this.helpWrittenByGlobalName = helpWrittenByGlobalName;
    }

    @Override
    public MetricExporter in(String scope, boolean asNode) {
        return new OpenMetricsExporter(scope, out, typeWrittenByGlobalName, helpWrittenByGlobalName);
    }

    @Override
    public void exportComplete() {
        // noop
    }

    @Override
    public void export(MetricID metricID, Counter counter, Metadata metadata) {
        String total = globalName(metricID, metadata, "_total");
        appendTYPE(total, OpenMetricsType.counter);
        appendHELP(total, metadata);
        appendValue(total, metricID.getTagsAsArray(), scaleToBaseUnit((double)counter.getCount(), metadata));
    }

    @Override
    public void export(MetricID metricID, Gauge<?> gauge, Metadata metadata) {
        Object value = null;
        try {
            value = gauge.getValue();
        } catch (IllegalStateException ex) {
            // The forwarding gauge is unloaded
            return;
        }
        if (!(value instanceof Number)) {
            LOGGER.log(Level.FINER, "Skipping OpenMetrics output for Gauge: {0} of type {1}",
                    new Object[] { metricID, value.getClass() });
            return;
        }
        String valueName = globalName(metricID, metadata);
        appendTYPE(valueName, OpenMetricsType.gauge);
        appendHELP(valueName, metadata);
        appendValue(valueName, metricID.getTagsAsArray(), scaleToBaseUnit((Number) value, metadata));
    }

    @Override
    public void export(MetricID metricID, Histogram histogram, Metadata metadata) {
        exportSampling(metricID, histogram, histogram::getCount, histogram::getSum, metadata);
    }

    private void exportSampling(MetricID metricID, Sampling sampling, LongSupplier count, Supplier<Number> sum, Metadata metadata) {
        Tag[] tags = metricID.getTagsAsArray();
        Snapshot snapshot = sampling.getSnapshot();
        String mean = globalName(metricID, metadata, "_mean");
        appendTYPE(mean, OpenMetricsType.gauge);
        appendValue(mean, tags, scaleToBaseUnit(snapshot.getMean(), metadata));
        String max = globalName(metricID, metadata, "_max");
        appendTYPE(max, OpenMetricsType.gauge);
        appendHELP(max, metadata);
        appendValue(max, tags, scaleToBaseUnit(snapshot.getMax(), metadata));

        String summary = globalName(metricID, metadata);
        appendHELP(summary, metadata);
        Snapshot.PercentileValue[] percentileValues = snapshot.percentileValues();
        if (snapshot instanceof WeightedSnapshot) {
            WeightedSnapshot w = (WeightedSnapshot) snapshot;
            if (w.getConfigAdapter() != null) {
                if (w.bucketValues() != null && w.bucketValues().length > 0) {
                    appendTYPE(summary, OpenMetricsType.histogram);
                    printCustomPercentile(percentileValues, summary, tags, metadata);
                    printBuckets(snapshot.bucketValues(), globalName(metricID, metadata, "_bucket"),
                                tags, metadata, sampling, count);
                } else {
                    appendTYPE(summary, OpenMetricsType.summary);
                    printCustomPercentile(percentileValues, summary, tags, metadata);
                }
            } else {
                appendTYPE(summary, OpenMetricsType.summary);
                printMedian(percentileValues, summary, tags, metadata);
            }
        } else {
            appendTYPE(summary, OpenMetricsType.summary);
            printMedian(percentileValues, summary, tags, metadata);
        }

        appendValue(globalName(metricID, metadata, "_count"), tags, ((double) count.getAsLong()));
        appendValue(globalName(metricID, metadata, "_sum"), tags, (sum.get()).doubleValue());
    }

    public void printCustomPercentile(Snapshot.PercentileValue[] pencentileValues, String summary, Tag[] tags, Metadata metadata) {
        for (Snapshot.PercentileValue value : pencentileValues) {
            appendValue(summary, tags("quantile", Double.toString(value.getPercentile()), tags), value.getValue());
        }
    }

    public void printBuckets(Snapshot.HistogramBucket[] buckets, String summary, Tag[] tags, Metadata metadata,
                             Sampling sampling, LongSupplier count) {
        if (sampling != null && sampling instanceof HistogramImpl) {
            for (Snapshot.HistogramBucket b : buckets) {
                appendValue(summary, tags("le", Double.toString(b.getBucket()), tags), ((double) evaluateBucketCount(b.getBucket(), sampling)));
            }
        } else {
            List<Long> bucketsList = Stream.of(buckets)
                    .map(bucket -> TimeUnit.MILLISECONDS.convert((long) bucket.getBucket(), TimeUnit.NANOSECONDS))
                    .collect(Collectors.toList());

            for (long b : bucketsList) {
                double seconds = b / 1000.0;
                appendValue(summary, tags("le", Double.toString(seconds), tags), ((double) evaluateBucketCount(seconds, sampling)));
            }
        }
        appendValue(summary, tags("le", "+Inf", tags), ((double) count.getAsLong()));
    }

    public long evaluateBucketCount(double bucket, Sampling sampling) {
        Snapshot snapshot = sampling.getSnapshot();
        if (snapshot instanceof WeightedSnapshot) {
            WeightedSnapshot weightedSnapshot = (WeightedSnapshot) snapshot;
            double[] conversionArray = null;
            long[] values = weightedSnapshot.getValues();
            if (sampling instanceof TimerImpl) {
                conversionArray = Arrays.stream(values).mapToDouble(l -> l / 1000000000D).toArray();
            } else {
                conversionArray = Arrays.stream(values).mapToDouble(l -> Long.valueOf(l).doubleValue()).toArray();
            }
            return Arrays.stream(conversionArray).filter(s -> s <= bucket).count();
        }
        return 0L;
    }
    
    public void printMedian(Snapshot.PercentileValue[] pencentileValues, String summary, Tag[] tags, Metadata metadata) {
        Optional<Snapshot.PercentileValue> median = Arrays.stream(pencentileValues)
                .filter(p -> p.getPercentile() == 0.5).findFirst();
        Optional<Snapshot.PercentileValue> percentile75th = Arrays.stream(pencentileValues)
                .filter(p -> p.getPercentile() == 0.75).findFirst();
        Optional<Snapshot.PercentileValue> percentile95th = Arrays.stream(pencentileValues)
                .filter(p -> p.getPercentile() == 0.95).findFirst();
        Optional<Snapshot.PercentileValue> percentile98th = Arrays.stream(pencentileValues)
                .filter(p -> p.getPercentile() == 0.98).findFirst();
        Optional<Snapshot.PercentileValue> percentile99th = Arrays.stream(pencentileValues)
                .filter(p -> p.getPercentile() == 0.99).findFirst();
        Optional<Snapshot.PercentileValue> percentile999th = Arrays.stream(pencentileValues)
                .filter(p -> p.getPercentile() == 0.999).findFirst();

        if(median.isPresent()) {
            appendValue(summary, tags("quantile", "0.5", tags),
                    scaleToBaseUnit(median.get().getValue(), metadata));
        }

        if(percentile75th.isPresent()) {
            appendValue(summary, tags("quantile", "0.75", tags),
                    scaleToBaseUnit(percentile75th.get().getValue(), metadata));
        }

        if(percentile95th.isPresent()) {
            appendValue(summary, tags("quantile", "0.95", tags),
                    scaleToBaseUnit(percentile95th.get().getValue(), metadata));
        }

        if(percentile98th.isPresent()) {
            appendValue(summary, tags("quantile", "0.98", tags),
                    scaleToBaseUnit(percentile98th.get().getValue(), metadata));
        }

        if(percentile99th.isPresent()) {
            appendValue(summary, tags("quantile", "0.99", tags),
                    scaleToBaseUnit(percentile99th.get().getValue(), metadata));
        }

        if(percentile999th.isPresent()) {
            appendValue(summary, tags("quantile", "0.999", tags),
                    scaleToBaseUnit(percentile999th.get().getValue(), metadata));
        }
    }

    @Override
    public void export(MetricID metricID, Timer timer, Metadata metadata) {
        exportSampling(metricID, timer, timer::getCount, () -> toSeconds(timer.getElapsedTime()), metadata);
    }

    protected void appendTYPE(String globalName, OpenMetricsType type) {
        if (!typeWrittenByGlobalName.add(globalName)) {
            // write metadata only once per metric
            return;
        }
        out.append("# TYPE ").append(globalName).append(' ').append(type.name()).append('\n');
    }

    protected void appendHELP(String globalName, Metadata metadata) {
        if (!helpWrittenByGlobalName.add(globalName)) {
            // write metadata only once per metric
           return;
        }
        Optional<String> description = metadata.description();
        out.append("# HELP ").append(globalName).append(' ').append(description.isPresent() ? description.get(): "").append('\n');
    }

    protected void appendValue(String globalName, Tag[] tags, Number value) {
        out.append(globalName);
        out.append(tagsToString(tags));
        if(globalName.equals(GC_TOTAL_ID) || globalName.equals(GC_TIME_SECONDS_TOTAL_ID)) {
            out.append(' ').append(value.toString()).append('\n');
        } else {
            out.append(' ').append(value == null ? "NaN" : roundValue(value)).append('\n');
        }
    }

    private void appendValue(String globalName, Tag[] tags, long value) {
        appendValue(globalName, tags, Long.valueOf(value));
    }

    private void appendValue(String globalName, Tag[] tags, double value) {
        appendValue(globalName, tags, Double.valueOf(value));
    }

    protected String roundValue(Number value) {
        String valString = value.toString();
        if (valString.endsWith("000000001")) {
            valString = valString.substring(0, valString.length() - 9); // cut off double representation error
        }
        if (valString.contains("000000001E")) {
            valString = valString.replace("000000001E", "E"); // cut off double representation error for exponential form
        }
        return valString;
    }

    protected static String tagsToString(Tag[] tags) {
        if (tags.length == 0) {
            return "";
        }
        String result = "";
        result += "{";
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                result += ",";
            }
            result += sanitizeMetricName(tags[i].getTagName())
                    + "=\""
                    + escapeTagValue(tags[i].getTagValue())
                    + '"';
        }
        result += "}";
        return result;
    }

    private String globalName(MetricID metricID, Metadata unit) {
        return globalName(metricID, "", unit, "");
    }

    private String globalName(MetricID metricID, String infix, Metadata unit) {
        return globalName(metricID, infix, unit, "");
    }

    private String globalName(MetricID metricID, Metadata unit, String suffix) {
            return globalName(metricID, "", unit, suffix);
    }

    private String globalName(MetricID metricID, String infix, Metadata metadata, String suffix) {
        if (!metadata.unit().isPresent()) {
            return globalName(metricID, infix + suffix);
        }
        String unit = metadata.getUnit();
        switch (unit) {
        case MetricUnits.NANOSECONDS:
        case MetricUnits.MICROSECONDS:
        case MetricUnits.MILLISECONDS:
        case MetricUnits.SECONDS:
        case MetricUnits.MINUTES:
        case MetricUnits.HOURS:
        case MetricUnits.DAYS:
            return globalName(metricID, infix + "_seconds" + suffix);
        case MetricUnits.BITS:
        case MetricUnits.BYTES:
        case MetricUnits.KILOBITS:
        case MetricUnits.MEGABITS:
        case MetricUnits.GIGABITS:
        case MetricUnits.MEBIBITS:
        case MetricUnits.GIBIBITS:
        case MetricUnits.KILOBYTES:
        case MetricUnits.MEGABYTES:
            return globalName(metricID, infix + "_bytes" + suffix);
        case MetricUnits.PERCENT:
            return globalName(metricID, infix + "_ratio" + suffix);
        case MetricUnits.PER_SECOND:
            return globalName(metricID, infix + "_per_second" + suffix);
        case MetricUnits.NONE:
            return globalName(metricID, infix + suffix);
        case MetricUnits.KIBIBITS:
        case MetricUnits.GIGABYTES:
        default:
            return globalName(metricID, infix + "_" + unit + suffix);
        }
    }

    private String globalName(MetricID metricID, String suffix) {
        String name = metricID.getName();
        return sanitizeMetricName(!suffix.isEmpty() && (name.endsWith(suffix) || name.contains(".total"))
                ? name
                : name + suffix);
    }

    private static CharSequence escapeTagValue(String name) {
        StringBuilder str = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\n') {
                str.append("\\n");
            } else {
                if (c == '\\' || c == '"') {
                    str.append('\\');
                }
                str.append(c);
            }
        }
        return str;
    }

    public static String sanitizeMetricName(String name) {
        //Translation rules :
        //All characters not in the range a-z A-Z or 0-9 are translated to underscore (_)
        //Double underscore is translated to single underscore
        String out = name.replaceAll("[^a-zA-Z0-9_]+", "_");
        //Colon-underscore (:_) is translated to single colon
        return out.replaceAll(":_", ":");
    }

    private static Tag[] tags(String name, String value, Tag[] rest) {
        Tag tag = new Tag(name, value);
        if (rest.length == 0) {
            return new Tag[] { tag };
        }
        Tag[] res = Arrays.copyOf(rest, rest.length + 1);
        res[rest.length] = tag;
        return res;
    }

    private static final BigDecimal NANOS_IN_SECOND = BigDecimal.valueOf(1000000000L);

    private static Number toSeconds(Duration d) {
        if (d == null) {
            return null;
        }
        if (d.getNano() == 0) {
            return d.getSeconds() / 1000d;
        }
        BigDecimal nanos = BigDecimal.valueOf(d.getSeconds()).multiply(NANOS_IN_SECOND).add(BigDecimal.valueOf(d.getNano()));
        return nanos.divide(NANOS_IN_SECOND).doubleValue();
    }
}
