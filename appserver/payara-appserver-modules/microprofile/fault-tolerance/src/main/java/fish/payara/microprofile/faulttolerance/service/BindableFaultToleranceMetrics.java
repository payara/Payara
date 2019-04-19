package fish.payara.microprofile.faulttolerance.service;

import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;

import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;

/**
 * The {@link BindableFaultToleranceMetrics} works both as a factory where {@link #bindTo(InvocationContext)} is used to
 * create context aware instances of a {@link FaultToleranceMetrics}.
 * 
 * This {@link FaultToleranceMetrics} uses {@link CDI} to resolve the {@link MetricRegistry}. When resolution fails
 * {@link #bindTo(InvocationContext)} will use {@link FaultToleranceMetrics#DISABLED}.
 * 
 * @author Jan Bernitt
 */
final class BindableFaultToleranceMetrics implements FaultToleranceMetrics {

    private static final Logger logger = Logger.getLogger(BindableFaultToleranceMetrics.class.getName());

    private final MetricRegistry metricRegistry;
    /**
     * This is "cached" as soon as an instance is bound using the
     * {@link #FaultToleranceMetricsFactory(MetricRegistry, String)} constructor.
     */
    private final String canonicalMethodName;

    public BindableFaultToleranceMetrics() {
        this.metricRegistry = resolveRegistry();
        this.canonicalMethodName = "(unbound)";
    }

    private BindableFaultToleranceMetrics(MetricRegistry metricRegistry, String canonicalMethodName) {
        this.metricRegistry = metricRegistry;
        this.canonicalMethodName = canonicalMethodName;
    }

    private static MetricRegistry resolveRegistry() {
        logger.log(Level.INFO, "Resolving Fault Tolerance MetricRegistry from CDI.");
        try {
            return CDI.current().select(MetricRegistry.class).get();
        } catch (Exception ex) {
            logger.log(Level.INFO, "No MetricRegistry could be found, disabling metrics.", ex);
            return null;
        }
    }

    /*
     * Factory method
     */

    FaultToleranceMetrics bindTo(InvocationContext context) {
        return metricRegistry == null
                ? FaultToleranceMetrics.DISABLED
                : new BindableFaultToleranceMetrics(metricRegistry, FaultToleranceUtils.getCanonicalMethodName(context));
    }

    /*
     * General Metrics
     */

    @Override
    public void incrementCounter(String keyPattern) {
        metricRegistry.counter(metricName(keyPattern)).inc();
    }

    @Override
    public void addToHistogram(String keyPattern, long duration) {
        metricRegistry.histogram(metricName(keyPattern)).update(duration);
    }

    @Override
    public void linkGauge(String keyPattern, LongSupplier gauge) {
        String metricName = metricName(keyPattern);
        Gauge<?> existingGauge = metricRegistry.getGauges().get(metricName);
        if (existingGauge == null) {
            Gauge<Long> newGauge = gauge::getAsLong;
            metricRegistry.register(metricName, newGauge);
        }
    }

    private String metricName(String keyPattern) {
        return String.format(keyPattern, canonicalMethodName);
    }

}
