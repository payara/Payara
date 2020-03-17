package fish.payara.microprofile.metrics.writer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

public interface MetricExporter {

    Logger LOGGER = Logger.getLogger(MetricExporter.class.getName());

    MetricExporter in(String scope);

    void export(MetricID metricID, Counter counter, Metadata metadata);

    void export(MetricID metricID, ConcurrentGauge gauge, Metadata metadata);

    void export(MetricID metricID, Gauge<?> gauge, Metadata metadata);

    void export(MetricID metricID, Histogram histogram, Metadata metadata);

    void export(MetricID metricID, Meter meter, Metadata metadata);

    void export(MetricID metricID, SimpleTimer timer, Metadata metadata);

    void export(MetricID metricID, Timer timer, Metadata metadata);

    default void export(MetricID metricID, Metric metric, Metadata metadata) {
        switch (MetricType.from(metric.getClass())) {
        case COUNTER: export(metricID, (Counter) metric, metadata); break;
        case CONCURRENT_GAUGE: export(metricID, (ConcurrentGauge) metric, metadata); break;
        case GAUGE: export(metricID, (Gauge<?>) metric, metadata); break;
        case HISTOGRAM: export(metricID, (Histogram) metric, metadata); break;
        case METERED: export(metricID, (Meter) metric, metadata); break;
        case SIMPLE_TIMER: export(metricID, (SimpleTimer) metric, metadata); break;
        case TIMER: export(metricID, (Timer) metric, metadata); break;
        case INVALID:
        default:
            LOGGER.log(Level.WARNING, "Metric type {0} for {1} is not supported",
                    new Object[] { metric.getClass(), metricID });
        }
    }

}
