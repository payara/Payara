package fish.payara.microprofile.metrics.writer;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Optional;
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

    private enum OpenMetricsType {
        counter, gauge, summary
    }

    private final String scope;
    private final PrintWriter out;

    public OpenMetricsExporter(Writer out) {
        this(null, new PrintWriter(out));
    }

    private OpenMetricsExporter(String scope, PrintWriter out) {
        this.scope = scope;
        this.out = out;
    }

    @Override
    public MetricExporter in(String scope) {
        return new OpenMetricsExporter(scope, out);
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

    private void appendTYPE(String globalName, OpenMetricsType type) {
        out.append("# TYPE ").append(globalName).append(' ').append(type.name()).append('\n');
    }

    private void appendHELP(String globalName, Metadata metadata) {
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

    private void appendValue(String globalName, Tag[] tags, Number value) {
        out.append(globalName);
        appendTags(tags);
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
        out.append(' ').append(valString).append('\n');
    }

    private void appendValue(String globalName, Tag[] tags, long value) {
        appendValue(globalName, tags, Long.valueOf(value));
    }

    private void appendValue(String globalName, Tag[] tags, double value) {
        appendValue(globalName, tags, Double.valueOf(value));
    }

    private void appendTags(Tag[] tags) {
        if (tags.length == 0) {
            return;
        }
        out.append('{');
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(sanitizeMetricName(tags[i].getTagName())).append("=\"")
                .append(escapeTagValue(tags[i].getTagValue())).append('"');
        }
        out.append('}');
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
        return !suffix.isEmpty() && name.endsWith(suffix) ? scope + '_' + name : scope + '_' + name + suffix;
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

    private static String sanitizeMetricName(String name) {
        //Translation rules :
        //All characters not in the range a-z A-Z or 0-9 are translated to underscore (_)
        //Double underscore is translated to single underscore
        String out = name.replaceAll("[^a-zA-Z0-9_]+", "_");
        //Colon-underscore (:_) is translated to single colon
        return out.replaceAll(":_", ":");
    }

    private static Number scaleToBaseUnit(Number value, Metadata metadata) {
        if (!metadata.getUnit().isPresent()) {
            return value;
        }
        String unit = metadata.getUnit().get();
        if (unit == null || unit.isEmpty() || unit.equals(MetricUnits.NONE)) {
            return value;
        }
        switch (unit) {
        // bytes from bits
        case MetricUnits.BITS: return value.longValue() / 8d;
        case MetricUnits.KILOBITS: return value.doubleValue() * 1000d / 8d;
        case MetricUnits.MEGABITS: return value.doubleValue() * 1000d * 1000d / 8d;
        case MetricUnits.GIGABITS: return value.doubleValue() * 1000d * 1000d * 1000d / 8d;
        case MetricUnits.KIBIBITS: return value.doubleValue() * 1024d / 8d;
        case MetricUnits.MEBIBITS: return value.doubleValue() * 1024d * 1024d / 8d;
        case MetricUnits.GIBIBITS: return value.doubleValue() * 1024d * 1024d * 1024d / 8d;

        // bytes from bytes
        case MetricUnits.BYTES: return value;
        case MetricUnits.KILOBYTES: return value.doubleValue() * 1000d;
        case MetricUnits.MEGABYTES: return value.doubleValue() * 1000d * 1000d;
        case MetricUnits.GIGABYTES: return value.doubleValue() * 1000d * 1000d * 1000d;

        // seconds from time unit
        case MetricUnits.NANOSECONDS: return value.longValue()  / 1000d / 1000d / 1000d;
        case MetricUnits.MICROSECONDS: return value.longValue() / 1000d / 1000d;
        case MetricUnits.MILLISECONDS: return value.longValue() / 1000d;
        case MetricUnits.SECONDS: return value;
        case MetricUnits.MINUTES: return value.doubleValue() * 60d;
        case MetricUnits.HOURS: return value.doubleValue() * 60d * 60d;
        case MetricUnits.DAYS: return value.doubleValue() * 60d * 60d * 24d;

        // others
        case MetricUnits.PERCENT:
        case MetricUnits.PER_SECOND:
        default:
            return value;
        }
    }

    private static Tag[] tags(String name, String value, Tag[] rest) {
        Tag tag = new Tag(name, value);
        if (rest.length == 0) {
            return new Tag[] { tag };
        }
        Tag[] res = new Tag[rest.length + 1];
        res[0] = tag;
        System.arraycopy(rest, 0, res, 1, rest.length);
        return res;
    }
}
