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

import static fish.payara.microprofile.metrics.MetricUnitsUtils.scaleToBaseUnit;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.logging.Level;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import org.eclipse.microprofile.metrics.MetricRegistry.Type;

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
        counter, gauge, summary
    }

    protected final Type scope;
    protected final PrintWriter out;
    protected final Set<String> typeWrittenByGlobalName;
    protected final Set<String> helpWrittenByGlobalName;

    public OpenMetricsExporter(Writer out) {
        this(null, out instanceof PrintWriter ? (PrintWriter) out : new PrintWriter(out), new HashSet<>(), new HashSet<>());
    }

    protected OpenMetricsExporter(Type scope, PrintWriter out, Set<String> typeWrittenByGlobalName,
            Set<String> helpWrittenByGlobalName) {
        this.scope = scope;
        this.out = out;
        this.typeWrittenByGlobalName = typeWrittenByGlobalName;
        this.helpWrittenByGlobalName = helpWrittenByGlobalName;
    }

    @Override
    public MetricExporter in(Type scope, boolean asNode) {
        return new OpenMetricsExporter(scope, out, typeWrittenByGlobalName, helpWrittenByGlobalName);
    }

    @Override
    public void exportComplete() {
        // noop
    }

    @Override
    public void export(MetricID metricID, Counter counter, Metadata metadata) {
        String total = globalName(metricID, "_total");
        appendTYPE(total, OpenMetricsType.counter);
        appendHELP(total, metadata);
        appendValue(total, metricID.getTagsAsArray(), counter.getCount());
    }

    @Override
    public void export(MetricID metricID, ConcurrentGauge gauge, Metadata metadata) {
        Tag[] tags = metricID.getTagsAsArray();
        String current = globalName(metricID, "_current");
        appendTYPE(current, OpenMetricsType.gauge);
        appendHELP(current, metadata);
        appendValue(current, tags, gauge.getCount());
        String min = globalName(metricID, "_min");
        appendTYPE(min, OpenMetricsType.gauge);
        appendValue(min, tags, gauge.getMin());
        String max = globalName(metricID, "_max");
        appendTYPE(max, OpenMetricsType.gauge);
        appendValue(max, tags, gauge.getMax());
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
        exportSampling(metricID, histogram, histogram::getCount, metadata);
    }

    private void exportSampling(MetricID metricID, Sampling sampling, LongSupplier count, Metadata metadata) {
        Tag[] tags = metricID.getTagsAsArray();
        Snapshot snapshot = sampling.getSnapshot();
        String mean = globalName(metricID, "_mean", metadata);
        appendTYPE(mean, OpenMetricsType.gauge);
        appendValue(mean, tags, scaleToBaseUnit(snapshot.getMean(), metadata));
        String max = globalName(metricID, "_max", metadata);
        appendTYPE(max, OpenMetricsType.gauge);
        appendValue(max, tags, scaleToBaseUnit(snapshot.getMax(), metadata));
        String min = globalName(metricID, "_min", metadata);
        appendTYPE(min, OpenMetricsType.gauge);
        appendValue(min, tags, scaleToBaseUnit(snapshot.getMin(), metadata));
        String stddev = globalName(metricID, "_stddev", metadata);
        appendTYPE(stddev, OpenMetricsType.gauge);
        appendValue(stddev, tags, scaleToBaseUnit(snapshot.getStdDev(), metadata));
        String summary = globalName(metricID, metadata);
        appendTYPE(summary, OpenMetricsType.summary);
        appendHELP(summary, metadata);
        appendValue(globalName(metricID, metadata, "_count"), tags, count.getAsLong());
        appendValue(summary, tags("quantile", "0.5", tags), scaleToBaseUnit(snapshot.getMedian(), metadata));
        appendValue(summary, tags("quantile", "0.75", tags), scaleToBaseUnit(snapshot.get75thPercentile(), metadata));
        appendValue(summary, tags("quantile", "0.95", tags), scaleToBaseUnit(snapshot.get95thPercentile(), metadata));
        appendValue(summary, tags("quantile", "0.98", tags), scaleToBaseUnit(snapshot.get98thPercentile(), metadata));
        appendValue(summary, tags("quantile", "0.99", tags), scaleToBaseUnit(snapshot.get99thPercentile(), metadata));
        appendValue(summary, tags("quantile", "0.999", tags), scaleToBaseUnit(snapshot.get999thPercentile(), metadata));
    }

    @Override
    public void export(MetricID metricID, Meter meter, Metadata metadata) {
        Tag[] tags = metricID.getTagsAsArray();
        String total = globalName(metricID, "_total");
        appendTYPE(total, OpenMetricsType.counter);
        appendHELP(total, metadata);
        appendValue(total, tags, meter.getCount());
        exportMetered(metricID, meter);
    }

    private void exportMetered(MetricID metricID, Metered metered) {
        Tag[] tags = metricID.getTagsAsArray();
        String rate = globalName(metricID, "_rate_per_second");
        appendTYPE(rate, OpenMetricsType.gauge);
        appendValue(rate, tags, metered.getMeanRate());
        String oneMinRate = globalName(metricID, "_one_min_rate_per_second");
        appendTYPE(oneMinRate, OpenMetricsType.gauge);
        appendValue(oneMinRate, tags, metered.getOneMinuteRate());
        String fiveMinRate = globalName(metricID, "_five_min_rate_per_second");
        appendTYPE(fiveMinRate, OpenMetricsType.gauge);
        appendValue(fiveMinRate, tags, metered.getFiveMinuteRate());
        String fifteenMinRate = globalName(metricID, "_fifteen_min_rate_per_second");
        appendTYPE(fifteenMinRate, OpenMetricsType.gauge);
        appendValue(fifteenMinRate, tags, metered.getFifteenMinuteRate());
    }

    @Override
    public void export(MetricID metricID, SimpleTimer timer, Metadata metadata) {
        Tag[] tags = metricID.getTagsAsArray();
        String total = globalName(metricID, "_total");
        appendTYPE(total, OpenMetricsType.counter);
        appendHELP(total, metadata);
        appendValue(total, tags, timer.getCount());
        String elapsedTime = globalName(metricID, "_elapsedTime_seconds");
        appendTYPE(elapsedTime, OpenMetricsType.gauge);
        appendValue(elapsedTime, tags, timer.getElapsedTime().toMillis() / 1000d);
    }

    @Override
    public void export(MetricID metricID, Timer timer, Metadata metadata) {
        exportMetered(metricID, timer);
        exportSampling(metricID, timer, timer::getCount, metadata);
    }

    protected void appendTYPE(String globalName, OpenMetricsType type) {
        if (typeWrittenByGlobalName.contains(globalName)) {
            return;
        }
        typeWrittenByGlobalName.add(globalName);
        out.append("# TYPE ").append(globalName).append(' ').append(type.name()).append('\n');
    }

    protected void appendHELP(String globalName, Metadata metadata) {
        if (helpWrittenByGlobalName.contains(globalName)) {
            return;
        }
        helpWrittenByGlobalName.add(globalName);
        Optional<String> description = metadata.getDescription();
        if (!description.isPresent()) {
            return;
        }
        String text = description.get();
        if (text.isEmpty()) {
            return;
        }
        out.append("# HELP ").append(globalName).append(' ').append(text).append('\n');
    }

    protected void appendValue(String globalName, Tag[] tags, Number value) {
        out.append(globalName);
        out.append(tagsToString(tags));
        out.append(' ').append(roundValue(value)).append('\n');
    }

    private void appendValue(String globalName, Tag[] tags, long value) {
        appendValue(globalName, tags, Long.valueOf(value));
    }

    private void appendValue(String globalName, Tag[] tags, double value) {
        appendValue(globalName, tags, Double.valueOf(value));
    }

    protected String roundValue(Number value) {
        String valString = value.toString();
        if (valString.endsWith(".0")) {
            valString = valString.substring(0, valString.length() - 2); // avoid decimal NNN.0 => NNN
        }
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
        if (!metadata.getUnit().isPresent()) {
            return globalName(metricID, infix + suffix);
        }
        String unit = metadata.getUnit().get();
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
        case MetricUnits.KIBIBITS:
        case MetricUnits.MEBIBITS:
        case MetricUnits.GIBIBITS:
        case MetricUnits.KILOBYTES:
        case MetricUnits.MEGABYTES:
        case MetricUnits.GIGABYTES:
            return globalName(metricID, infix + "_bytes" + suffix);
        case MetricUnits.PERCENT:
            return globalName(metricID, infix + "_ratio" + suffix);
        case MetricUnits.PER_SECOND:
            return globalName(metricID, infix + "_per_second" + suffix);
        case MetricUnits.NONE:
            return globalName(metricID, infix + suffix);
        default:
            return globalName(metricID, infix + "_" + unit + suffix);
        }
    }

    private String globalName(MetricID metricID, String suffix) {
        String name = metricID.getName();
        return sanitizeMetricName(!suffix.isEmpty() && name.endsWith(suffix)
                ? scope.getName() + '_' + name
                : scope.getName() + '_' + name + suffix);
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
}
