package fish.payara.microprofile.faulttolerance.cdi;

import java.util.function.LongSupplier;

import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;

import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;

/**
 * A {@link FaultToleranceMetrics} service that uses {@link CDI} to resolve the {@link MetricRegistry} if needed.
 * 
 * @author Jan Bernitt
 */
public class CdiFaultToleranceMetrics implements FaultToleranceMetrics {

    private MetricRegistry metricRegistry;

    public CdiFaultToleranceMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void increment(String keyPattern, InvocationContext context) {
        getMetricRegistry().counter(metricName(keyPattern, context)).inc();
    }

    @Override
    public void add(String keyPattern, long duration, InvocationContext context) {
        getMetricRegistry().histogram(metricName(keyPattern, context)).update(duration);
    }

    @Override
    public void insert(String keyPattern, LongSupplier gauge, InvocationContext context) {
        String metricName = metricName(keyPattern, context);
        Gauge<?> existingGauge = getMetricRegistry().getGauges().get(metricName);
        if (existingGauge == null) {
            Gauge<Long> newGauge = gauge::getAsLong;
            getMetricRegistry().register(metricName, newGauge);
        }
    }

    private static String metricName(String keyPattern, InvocationContext context) {
        return String.format(keyPattern, FaultToleranceCdiUtils.getCanonicalMethodName(context));
    }

    private MetricRegistry getMetricRegistry() {
        if (metricRegistry == null) {
            metricRegistry = CDI.current().select(MetricRegistry.class).get();
        }
        return metricRegistry;
    }
}
