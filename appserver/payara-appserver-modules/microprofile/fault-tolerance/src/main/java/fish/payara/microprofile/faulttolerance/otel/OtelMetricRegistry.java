/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * only if the code is changed by the third party and used as a new code
 * in combination with Open Source Software developed by Glassfish/Payara
 * or its successors.
 */
package fish.payara.microprofile.faulttolerance.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.DoubleStream;


/**
 * OpenTelemetry-backed implementation of the MicroProfile {@link MetricRegistry}.
 * 
 * This implementation delegates metric recording to OpenTelemetry, using wrapper classes
 * for Counter, Histogram, and Gauge.
 * 
 * Observable gauge callbacks are tracked and deregistered on {@link #close()}.
 */
public final class OtelMetricRegistry implements MetricRegistry, AutoCloseable {

    private static final String MP_SCOPE_KEY = "mp.scope";
    /** FT spec metric that accumulates time — always increasing, so mapped to a monotonic OTel counter. */
    private static final String CIRCUITBREAKER_STATE_METRIC = "ft.circuitbreaker.state.total";

    private final Meter meter;
    private final String scope;
    
    private final Map<MetricID, Metric> metrics = new ConcurrentHashMap<>();
    private final Map<String, Metadata> metadataByName = new ConcurrentHashMap<>();
    private final List<AutoCloseable> observableHandles = new CopyOnWriteArrayList<>();

    public OtelMetricRegistry(Meter meter, String scope) {
        this.meter = meter;
        this.scope = scope;
    }

    @Override
    public Counter counter(String name) {
        return counter(new MetricID(name));
    }

    @Override
    public Counter counter(String name, Tag... tags) {
        return counter(new MetricID(name, tags));
    }

    @Override
    public Counter counter(MetricID metricID) {
        return (Counter) metrics.computeIfAbsent(metricID, id -> {
            LongCounter otelCounter = meter.counterBuilder(id.getName()).build();
            return new OtelCounter(otelCounter, scope, id.getTags());
        });
    }

    @Override
    public Counter counter(Metadata metadata) {
        return counter(new MetricID(metadata.getName()));
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        metadataByName.putIfAbsent(metadata.getName(), metadata);
        return counter(new MetricID(metadata.getName(), tags));
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(String name, T object, java.util.function.Function<T, R> func, Tag... tags) {
        return gauge(new MetricID(name, tags), object, func);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T object, java.util.function.Function<T, R> func) {
        return gauge(metricID, () -> func.apply(object));
    }


    @Override
    public <T, R extends Number> Gauge<R> gauge(Metadata metadata, T object, java.util.function.Function<T, R> func, Tag... tags) {
        return gauge(new MetricID(metadata.getName(), tags), object, func);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(String name, java.util.function.Supplier<T> supplier, Tag... tags) {
        return gauge(new MetricID(name, tags), supplier);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, java.util.function.Supplier<T> supplier) {
        return (Gauge<T>) metrics.computeIfAbsent(metricID, id -> buildGauge(id, supplier, null));
    }

    @Override
    public <T extends Number> Gauge<T> gauge(Metadata metadata, java.util.function.Supplier<T> supplier, Tag... tags) {
        MetricID id = new MetricID(metadata.getName(), tags);
        return (Gauge<T>) metrics.computeIfAbsent(id, key -> buildGauge(key, supplier, metadata.getUnit()));
    }

    <T extends Number> Gauge<T> buildGauge(MetricID id, java.util.function.Supplier<T> supplier) {
        return buildGauge(id, supplier, null);
    }

    private <T extends Number> Gauge<T> buildGauge(MetricID id, java.util.function.Supplier<T> supplier, String unit) {
        Attributes attributes = tagsScopeToAttributes(scope, id.getTags());
        if (isMonotonicGauge(id.getName())) {
            // ft.circuitbreaker.state.total accumulates nanoseconds — always increasing, so a monotonic counter
            var builder = meter.counterBuilder(id.getName());
            if (unit != null && !unit.isEmpty()) {
                builder.setUnit(unit);
            }
            ObservableLongCounter otelCounter = builder
                    .buildWithCallback(measurement -> {
                        T value = supplier.get();
                        if (value != null) {
                            measurement.record(value.longValue(), attributes);
                        }
                    });
            observableHandles.add(otelCounter);
        } else {
            // ft.bulkhead.executionsRunning / executionsWaiting — can go up and down
            ObservableLongUpDownCounter otelGauge = meter.upDownCounterBuilder(id.getName())
                    .buildWithCallback(measurement -> {
                        T value = supplier.get();
                        if (value != null) {
                            measurement.record(value.longValue(), attributes);
                        }
                    });
            observableHandles.add(otelGauge);
        }
        return new OtelGauge<>();
    }

    /**
     * Returns {@code true} for FT gauge metrics whose values are monotonically increasing
     * (i.e. they accumulate over time and must be reported as OTel monotonic counters).
     * <p>
     * Currently only {@link #CIRCUITBREAKER_STATE_METRIC} qualifies — it tracks nanoseconds
     * spent in each circuit-breaker state and never decreases.
     */
    private static boolean isMonotonicGauge(String name) {
        return CIRCUITBREAKER_STATE_METRIC.equals(name);
    }

    @Override
    public Histogram histogram(String name) {
        return histogram(new MetricID(name));
    }

    @Override
    public Histogram histogram(String name, Tag... tags) {
        return histogram(new MetricID(name, tags));
    }

    @Override
    public Histogram histogram(MetricID metricID) {
        return histogramWithUnit(metricID, null);
    }

    private Histogram histogramWithUnit(MetricID metricID, String unit) {
        return (Histogram) metrics.computeIfAbsent(metricID, id -> {
            var builder = meter.histogramBuilder(id.getName())
                    .setExplicitBucketBoundariesAdvice(
                            DoubleStream.of(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10d)
                                    .boxed().toList());
            if (unit != null) {
                builder.setUnit(unit);
            }
            return new OtelHistogram(builder.build(), scope, id.getTags());
        });
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        metadataByName.putIfAbsent(metadata.getName(), metadata);
        return histogramWithUnit(new MetricID(metadata.getName()), otelUnit(metadata.getUnit()));
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        metadataByName.putIfAbsent(metadata.getName(), metadata);
        return histogramWithUnit(new MetricID(metadata.getName(), tags), otelUnit(metadata.getUnit()));
    }

    /**
     * Converts MP Metrics unit to the OTel unit string.
     * FT histograms store nanoseconds but {@link OtelHistogram#update} converts them to seconds.
     * The MP Fault Tolerance spec mandates exact unit strings for its OTel metrics (e.g. "seconds",
     * "nanoseconds") — these are spec-required, not UCUM, and must not be changed without a spec update.
     */
    private static String otelUnit(String mpUnit) {
        if (MetricUnits.NANOSECONDS.equals(mpUnit)) {
            return "seconds";
        }
        return mpUnit;
    }

    @Override
    public Timer timer(String name) {
        throw new UnsupportedOperationException("Timer is not yet supported in the OpenTelemetry bridge.");
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        throw new UnsupportedOperationException("Timer is not yet supported in the OpenTelemetry bridge.");
    }

    @Override
    public Timer timer(MetricID metricID) {
        throw new UnsupportedOperationException("Timer is not yet supported in the OpenTelemetry bridge.");
    }

    @Override
    public Timer timer(Metadata metadata) {
        throw new UnsupportedOperationException("Timer is not yet supported in the OpenTelemetry bridge.");
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        throw new UnsupportedOperationException("Timer is not yet supported in the OpenTelemetry bridge.");
    }

    @Override
    public Metric getMetric(MetricID metricID) {
        return metrics.get(metricID);
    }

    @Override
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> asType) {
        Metric metric = getMetric(metricID);
        return asType.isInstance(metric) ? asType.cast(metric) : null;
    }

    @Override
    public Counter getCounter(MetricID metricID) {
        return getMetric(metricID, Counter.class);
    }

    @Override
    public Gauge<?> getGauge(MetricID metricID) {
        return getMetric(metricID, Gauge.class);
    }

    @Override
    public Histogram getHistogram(MetricID metricID) {
        return getMetric(metricID, Histogram.class);
    }

    @Override
    public Timer getTimer(MetricID metricID) {
        return getMetric(metricID, Timer.class);
    }

    @Override
    public Metadata getMetadata(String name) {
        return metadataByName.get(name);
    }

    @Override
    public boolean remove(String name) {
        return metrics.entrySet().removeIf(entry -> entry.getKey().getName().equals(name));
    }

    @Override
    public boolean remove(MetricID metricID) {
        return metrics.remove(metricID) != null;
    }

    @Override
    public void removeMatching(MetricFilter filter) {
        metrics.entrySet().removeIf(entry -> filter.matches(entry.getKey(), entry.getValue()));
    }

    @Override
    public SortedSet<String> getNames() {
        return new TreeSet<>(
            metrics.keySet().stream().map(MetricID::getName).toList()
        );
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        return new TreeSet<>(metrics.keySet());
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getMetrics(Gauge.class, MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return getMetrics(Counter.class, MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return getMetrics(Histogram.class, MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getMetrics(Timer.class, MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    @Override
    public SortedMap<MetricID, Metric> getMetrics(MetricFilter filter) {
        SortedMap<MetricID, Metric> result = new TreeMap<>();
        for (var entry : metrics.entrySet()) {
            if (filter.matches(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @Override
    public <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> type, MetricFilter filter) {
        SortedMap<MetricID, T> result = new TreeMap<>();
        for (var entry : metrics.entrySet()) {
            if (type.isInstance(entry.getValue()) && filter.matches(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), type.cast(entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return Map.copyOf(metrics);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return java.util.Collections.unmodifiableMap(metadataByName);
    }

    @Override
    public String getScope() {
        return scope;
    }

    /**
     * Deregisters all observable gauge callbacks and cleans up resources.
     * Must be called when the registry is no longer needed.
     */
    @Override
    public void close() {
        for (AutoCloseable handle : observableHandles) {
            try {
                handle.close();
            } catch (Exception ignored) {
            }
        }
        observableHandles.clear();
    }

    private static Attributes tagsScopeToAttributes(String scope, Map<String, String> tags) {
        AttributesBuilder builder = Attributes.builder();
        if (scope != null) {
            builder.put(MP_SCOPE_KEY, scope);
        }
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(builder::put);
        }
        return builder.build();
    }

}
