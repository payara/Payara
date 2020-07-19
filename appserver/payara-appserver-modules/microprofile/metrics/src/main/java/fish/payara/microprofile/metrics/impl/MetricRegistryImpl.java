/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Vetoed;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.microprofile.metrics.MetricFilter.ALL;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;

import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import static org.eclipse.microprofile.metrics.MetricType.HISTOGRAM;
import static org.eclipse.microprofile.metrics.MetricType.METERED;
import static org.eclipse.microprofile.metrics.MetricType.SIMPLE_TIMER;
import static org.eclipse.microprofile.metrics.MetricType.TIMER;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

/**
 * The MetricRegistry stores the metrics and metadata information
 */
@Vetoed
public class MetricRegistryImpl extends MetricRegistry {

    private static final Logger LOGGER = Logger.getLogger(MetricRegistryImpl.class.getName());

    static final class MetricFamily<T extends Metric> {
        final Metadata metadata;
        final ConcurrentMap<MetricID, T> metrics = new ConcurrentHashMap<>();

        MetricFamily(Metadata metadata) {
            this.metadata = metadata;
        }

        boolean remove(MetricID metricID) {
            return metrics.remove(metricID) != null;
        }

        T get(MetricID metricID) {
            return metrics.get(metricID);
        }
    }

    private final ConcurrentMap<String, MetricFamily<?>> metricsFamiliesByName = new ConcurrentHashMap<>();
    private final Clock clock;
    private final List<MetricRegistrationListener> listeners = new ArrayList<>();

    public MetricRegistryImpl() {
        this(Clock.defaultClock());
    }

    public MetricRegistryImpl(Clock clock) {
        this.clock = clock;
    }

    public MetricRegistryImpl addListener(MetricRegistrationListener listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public Counter counter(String name) {
        return findMetricOrCreate(name, COUNTER, new Tag[0]);
    }

    @Override
    public Counter counter(Metadata metadata) {
        return findMetricOrCreate(metadata, COUNTER);
    }

    @Override
    public Counter counter(String name, Tag... tags) {
        return findMetricOrCreate(name, COUNTER, tags);
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, COUNTER, tags);
    }

    @Override
    public Histogram histogram(String name) {
        return findMetricOrCreate(name, HISTOGRAM, new Tag[0]);
    }

    @Override
    public Histogram histogram(Metadata metadata) {
         return findMetricOrCreate(metadata, HISTOGRAM);
    }

    @Override
    public Meter meter(String name) {
        return findMetricOrCreate(name, METERED, new Tag[0]);
    }

    @Override
    public Meter meter(Metadata metadata) {
         return findMetricOrCreate(metadata, METERED);
    }

    @Override
    public SimpleTimer simpleTimer(String name) {
        return findMetricOrCreate(name, SIMPLE_TIMER, new Tag[0]);
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata) {
        return findMetricOrCreate(metadata, MetricType.SIMPLE_TIMER, new Tag[0]);
    }

    @Override
    public Timer timer(String name) {
        return findMetricOrCreate(name, TIMER, new Tag[0]);
    }

    @Override
    public Timer timer(Metadata metadata) {
        return findMetricOrCreate(metadata, TIMER);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name) {
        return findMetricOrCreate(name, MetricType.CONCURRENT_GAUGE, new Tag[0]);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name, Tag... tags) {
        return findMetricOrCreate(name, MetricType.CONCURRENT_GAUGE, tags);
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return findMetricOrCreate(metadata, MetricType.CONCURRENT_GAUGE);
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, MetricType.CONCURRENT_GAUGE, tags);
    }

    @Override
    public Histogram histogram(String name, Tag... tags) {
        return findMetricOrCreate(name, MetricType.HISTOGRAM, tags);
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, MetricType.HISTOGRAM, tags);
    }

    @Override
    public Meter meter(String name, Tag... tags) {
        return findMetricOrCreate(name, METERED, tags);
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, METERED, tags);
    }

    @Override
    public SimpleTimer simpleTimer(String name, Tag... tags) {
        return findMetricOrCreate(name, SIMPLE_TIMER, tags);
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, MetricType.SIMPLE_TIMER, tags);
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        return findMetricOrCreate(name, TIMER, tags);
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, TIMER, tags);
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        TreeSet<MetricID> ids = new TreeSet<>();
        for (MetricFamily<?> e : metricsFamiliesByName.values()) {
            ids.addAll(e.metrics.keySet());
        }
        return ids;
    }

    @Override
    public SortedSet<String> getNames() {
        return new TreeSet<>(metricsFamiliesByName.keySet());
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getGauges(ALL);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter filter) {
        return findMetrics(Gauge.class, filter);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return getConcurrentGauges(ALL);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter filter) {
        return findMetrics(ConcurrentGauge.class, filter);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return getCounters(ALL);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter filter) {
        return findMetrics(Counter.class, filter);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return getHistograms(ALL);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter filter) {
        return findMetrics(Histogram.class, filter);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return getMeters(ALL);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter filter) {
        return findMetrics(Meter.class, filter);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return getSimpleTimers(ALL);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter filter) {
        return findMetrics(SimpleTimer.class, filter);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getTimers(ALL);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter filter) {
        return findMetrics(Timer.class, filter);
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return findMetrics((id, metric) -> true);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return metricsFamiliesByName.entrySet().stream().collect(toMap(Entry::getKey,
                e -> e.getValue().metadata));
    }

    @Override
    public boolean remove(String name) {
        return metricsFamiliesByName.remove(name) != null;
    }

    @Override
    public boolean remove(MetricID metricID) {
        AtomicBoolean removed = new AtomicBoolean();
        metricsFamiliesByName.computeIfPresent(metricID.getName(), (name, family) -> {
            if (family.remove(metricID)) {
                removed.set(true);
            }
            return family.metrics.isEmpty() ? null : family;
        });
        return removed.get();
    }

    @Override
    public void removeMatching(MetricFilter filter) {
        if (filter == MetricFilter.ALL) {
            metricsFamiliesByName.clear();
        }
        Iterator<MetricFamily<?>> familyIter = metricsFamiliesByName.values().iterator();
        while (familyIter.hasNext()) {
            MetricFamily<?> family = familyIter.next();
            Iterator<? extends Entry<MetricID, ? extends Metric>> metricIter = family.metrics.entrySet().iterator();
            while (metricIter.hasNext()) {
                Entry<MetricID, ? extends Metric> entry = metricIter.next();
                if (filter.matches(entry.getKey(), entry.getValue())) {
                    remove(entry.getKey()); // OBS! it is important to not use the iterator.remove() so that the family is removed "atomically" when empty
                }
            }
        }
    }

    private <T extends Metric> SortedMap<MetricID, T> findMetrics(Class<T> metricClass, MetricFilter filter) {
        return findMetrics((id, metric) -> metricClass.isInstance(metric) && filter.matches(id, metric));
    }
    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<MetricID, T> findMetrics(MetricFilter filter) {
        SortedMap<MetricID, T> matches = new TreeMap<>();
        for (MetricFamily<?> family : metricsFamiliesByName.values()) {
            for (Entry<MetricID, ? extends Metric> entry : family.metrics.entrySet()) {
                if (filter.matches(entry.getKey(), entry.getValue())) {
                    matches.put(entry.getKey(), (T) entry.getValue());
                }
            }
        }
        return matches;
    }

    private <T extends Metric> T findMetricOrCreate(String name, MetricType metricType, Tag... tags) {
        checkNameIsNotNullOrEmpty(name);
        Metadata metadata = Metadata.builder()
                .withName(name)
                .withType(metricType)
                .withOptionalDisplayName(null)
                .build();
        return findMetricOrCreate(metadata, true, tags);
    }

    private <T extends Metric> T findMetricOrCreate(Metadata metadata, MetricType metricType, Tag... tags) {
        return findMetricOrCreate(withType(metadata, metricType), false, tags);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T findMetricOrCreate(Metadata metadata, boolean useExistingMetadata, Tag... tags) {
        MetricID metricID = new MetricID(metadata.getName(), tags);
        MetricFamily<?> family = metricsFamiliesByName.get(metricID.getName());
        if (family == null) {
            return register(metadata, useExistingMetadata, null, tags);
        }
        Metric existing = family.get(metricID);
        if (existing == null) {
            checkSameType(metricID.getName(), metadata, family.metadata);
            return register(metadata, useExistingMetadata, null, tags);
        }
        if (useExistingMetadata && metadata.getType() != family.metadata.getType()
                || !useExistingMetadata && !metadata.equals(family.metadata)) {
            throw new IllegalArgumentException(
                    String.format("Tried to lookup a metric with conflicting metadata, looup is %s, existing is %s",
                            metadata.toString(), family.metadata.toString()));
        }
        return (T) existing;
    }

    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        checkNameIsNotNullOrEmpty(name);
        return register(Metadata.builder().withName(name).build(), true, metric);
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
        return register(metadata, false, metric);
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T metric, Tag... tags) throws IllegalArgumentException {
        return register(metadata, false, metric, tags);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T register(Metadata metadata, boolean useExistingMetadata, T metric, Tag... tags) {
        if (metadata.getTypeRaw() == MetricType.INVALID) {
            metadata = withType(metadata, MetricType.from(metric.getClass()));
        }
        String name = metadata.getName();
        checkNameIsNotNullOrEmpty(name);
        if (useExistingMetadata) {
            Metadata existingMetadata = getMetadata(name);
            if (existingMetadata != null) {
                checkSameType(name, metadata, existingMetadata);
                metadata = existingMetadata;
            }
        }
        final Metadata newMetadata = metadata;
        final T newMetric = metric != null ? metric : (T) createMetricInstance(newMetadata);
        MetricFamily<T> family = (MetricFamily<T>) metricsFamiliesByName.computeIfAbsent(name,
                key -> new MetricFamily<>(newMetadata));
        MetricID metricID = new MetricID(name, tags);
        if (family.metadata != newMetadata) {
            checkReusableMetadata(name, newMetadata, family.metadata, family.metrics.containsKey(metricID));
        }
        T current = family.metrics.computeIfAbsent(metricID, key -> newMetric);
        if (current != newMetric) {
            checkNotAGauge(name, newMetadata);
        }
        notifyRegistrationListeners(metricID);
        return current;
    }

    private void notifyRegistrationListeners(MetricID metricID) {
        for (MetricRegistrationListener l : listeners) {
            try {
                l.onRegistration(metricID, this);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "Registration listener threw exception:", ex);
            }
        }
    }

    private static void checkNameIsNotNullOrEmpty(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name must not be null or empty");
        }
    }

    private static void checkReusableMetadata(String name, Metadata newMetadata, Metadata existingMetadata, boolean checkReusable) {
        if (checkReusable) {
            checkIsReusable(name, newMetadata, existingMetadata);
            checkNotAGauge(name, newMetadata);
        }

        checkSameType(name, newMetadata, existingMetadata);

        if (!existingMetadata.equals(newMetadata)) {
            throw new IllegalArgumentException(String.format(
                  "Metadata ['%s'] already registered, does not match provided ['%s']",
                  existingMetadata.toString(), newMetadata.toString()
          ));
        }
    }

    private static void checkIsReusable(String name, Metadata newMetadata, Metadata existingMetadata) {
        //if existing metric declared not reusable
        if (!existingMetadata.isReusable()) {
            throw new IllegalArgumentException(String.format(
                    "Metric ['%s'] already exists and declared not reusable", name
                    ));
        }

        //registration call itself declares the metric to not be reusable
        if (!newMetadata.isReusable()) {
            throw new IllegalArgumentException(String.format(
                    "Metric ['%s'] already exists and declared reusable but registration call declares the metric to not be reusable", name
                    ));
        }
    }

    private static void checkNotAGauge(String name, Metadata newMetadata) {
        //reusable does not apply to gauges
        if (GAUGE.equals(newMetadata.getTypeRaw())) {
            throw new IllegalArgumentException(String.format(
                    "Gauge type metric['%s'] is not reusable", name
            ));
        }
    }

    private static void checkSameType(String name, Metadata newMetadata, Metadata existingMetadata) {
        //Only metrics of the same type can be reused under the same name
        if (existingMetadata.getTypeRaw() != newMetadata.getTypeRaw()) {
            throw new IllegalArgumentException(String.format(
                    "Metric ['%s'] type['%s'] does not match with existing type['%s']",
                    name, newMetadata.getType(), existingMetadata.getType()
            ));
        }
    }

    private Metric createMetricInstance(Metadata metadata) {
        String name = metadata.getName();
        switch (metadata.getTypeRaw()) {
        case COUNTER:
            return new CounterImpl();
        case CONCURRENT_GAUGE:
            return new ConcurrentGaugeImpl(clock);
        case GAUGE:
            throw new IllegalArgumentException(String.format("Unsupported operation for Gauge ['%s']", name));
        case METERED:
            return new MeterImpl();
        case HISTOGRAM:
            return new HistogramImpl();
        case TIMER:
            return new TimerImpl(clock);
        case SIMPLE_TIMER:
            return new SimpleTimerImpl(clock);
        case INVALID:
        default:
            throw new IllegalStateException("Invalid metric type : " + metadata.getTypeRaw());
        }
    }

    /*
     * Non-API Methods (Extra Methods)
     */

    @SuppressWarnings("unchecked")
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> metricType) {
        MetricFamily<?> family = metricsFamiliesByName.get(metricID.getName());
        if (family == null) {
            return null;
        }
        Metric metric = family.get(metricID);
        if (!metricType.isAssignableFrom(metric.getClass())) {
            throw new IllegalArgumentException("Invalid metric type : " + metricType);
        }
        return (T) metric;
    }

    public Set<MetricID> getMetricsIDs(String name) {
        MetricFamily<?> family = metricsFamiliesByName.get(name);
        return family == null ? emptySet() : unmodifiableSet(family.metrics.keySet());
    }

    public Map<MetricID, Metric> getMetrics(String name) {
        MetricFamily<?> family = metricsFamiliesByName.get(name);
        return family == null
                ? emptyMap()
                : unmodifiableMap(family.metrics);
    }

    public Metadata getMetadata(String name) {
        MetricFamily<?> family = metricsFamiliesByName.get(name);
        return family == null ? null : family.metadata;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Entry<String, MetricFamily<?>> family : metricsFamiliesByName.entrySet()) {
            str.append(family.getKey()).append(": ").append(family.getValue().metadata).append('\n');
            for (Entry<MetricID, ? extends Metric> entry : family.getValue().metrics.entrySet()) {
                str.append('\t').append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
        }
        return str.toString();
    }

    /**
     * This is a workaround to prevent setting display name field as side effect of using
     * {@link Metadata#builder(Metadata)}. It does not prevent the issue in case the type needs setting but it can if it
     * does not need to be set. There is no API way to make the resulting {@link Metadata} identical in case the type is
     * set.
     */
    private static Metadata withType(Metadata metadata, MetricType type) {
        if (type == metadata.getTypeRaw()) {
            return metadata; // already set
        }
        return Metadata.builder(metadata).withType(type).build();
    }
}
