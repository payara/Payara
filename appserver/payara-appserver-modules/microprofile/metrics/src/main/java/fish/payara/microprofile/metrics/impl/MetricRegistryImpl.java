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

import java.util.Iterator;
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
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.microprofile.metrics.MetricFilter.ALL;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import static org.eclipse.microprofile.metrics.MetricType.HISTOGRAM;
import static org.eclipse.microprofile.metrics.MetricType.METERED;
import static org.eclipse.microprofile.metrics.MetricType.TIMER;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

/**
 * The MetricRegistry stores the metrics and metadata information
 */
@Vetoed
public class MetricRegistryImpl extends MetricRegistry {

    static final class MetricEntry {
        final Metric metric;
        final Metadata metadata;

        MetricEntry(Metric metric, Metadata metadata) {
            this.metric = metric;
            this.metadata = metadata;
        }
    }

    private final ConcurrentMap<String, ConcurrentMap<MetricID, MetricEntry>> metricsByNameAndId = new ConcurrentHashMap<>();

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
        for (ConcurrentMap<MetricID, MetricEntry> e : metricsByNameAndId.values()) {
            ids.addAll(e.keySet());
        }
        return ids;
    }

    @Override
    public SortedSet<String> getNames() {
        return new TreeSet<>(metricsByNameAndId.keySet());
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getGauges(ALL);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        return findMetrics(Gauge.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return getConcurrentGauges(ALL);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter metricFilter) {
        return findMetrics(ConcurrentGauge.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return getCounters(ALL);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        return findMetrics(Counter.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return getHistograms(ALL);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter metricFilter) {
        return findMetrics(Histogram.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return getMeters(ALL);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter metricFilter) {
        return findMetrics(Meter.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getTimers(ALL);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        return findMetrics(Timer.class, metricFilter);
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return findMetrics((id, metric) -> true);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return metricsByNameAndId.entrySet().stream().collect(toMap(Entry::getKey, 
                e -> e.getValue().values().iterator().next().metadata));
    }

    @Override
    public boolean remove(String name) {
        return metricsByNameAndId.remove(name) != null;
    }

    @Override
    public boolean remove(MetricID metricID) {
        AtomicBoolean removed = new AtomicBoolean();
        metricsByNameAndId.computeIfPresent(metricID.getName(), (name, group) -> {
            if (group.remove(metricID) != null) {
                removed.set(true);
            }
            return group.size() == 0 ? null : group;
        });
        return removed.get();
    }

    @Override
    public void removeMatching(MetricFilter filter) {
        if (filter == MetricFilter.ALL) {
            metricsByNameAndId.clear();
        }
        Iterator<ConcurrentMap<MetricID, MetricEntry>> groupIter = metricsByNameAndId.values().iterator();
        while (groupIter.hasNext()) {
            ConcurrentMap<MetricID, MetricEntry> group = groupIter.next();
            Iterator<Entry<MetricID, MetricEntry>> metricIter = group.entrySet().iterator();
            while (metricIter.hasNext()) { 
                Entry<MetricID, MetricEntry> entry = metricIter.next();
                if (filter.matches(entry.getKey(), entry.getValue().metric)) {
                    groupIter.remove();
                }
            }
            if (group.size() == 0) {
                groupIter.remove();
            }
        }
    }

    private <T extends Metric> SortedMap<MetricID, T> findMetrics(Class<T> metricClass, MetricFilter filter) {
        return findMetrics((id, metric) -> metricClass.isInstance(metric) && filter.matches(id, metric));
    }
    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<MetricID, T> findMetrics(MetricFilter filter) {
        SortedMap<MetricID, T> matches = new TreeMap<>();
        for (ConcurrentMap<MetricID, MetricEntry> group : metricsByNameAndId.values()) {
            for (Entry<MetricID, MetricEntry> entry : group.entrySet()) {
                if (filter.matches(entry.getKey(), entry.getValue().metric)) {
                    matches.put(entry.getKey(), (T) entry.getValue().metric);
                }
            }
        }
        return matches;
    }

    private <T extends Metric> T findMetricOrCreate(String name, MetricType metricType, Tag... tags) {
        checkNameIsNotNullOrEmpty(name);
        return findMetricOrCreate(Metadata.builder().withName(name).withType(metricType).build(), true, tags);
    }

    private <T extends Metric> T findMetricOrCreate(Metadata metadata, MetricType metricType, Tag... tags) {
        return findMetricOrCreate(Metadata.builder(metadata).withType(metricType).build(), false, tags);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T findMetricOrCreate(Metadata metadata, boolean useExistingMetadata, Tag... tags) {
        MetricID metricID = new MetricID(metadata.getName(), tags);
        ConcurrentMap<MetricID, MetricEntry> group = metricsByNameAndId.get(metricID.getName());
        if (group == null) {
            return register(metadata, null, tags);
        }
        MetricEntry entry = group.get(metricID);
        if (entry == null) {
            Iterator<MetricEntry> iter = group.values().iterator();
            if (iter.hasNext()) {
                checkSameType(metricID.getName(), metadata, iter.next().metadata);
            }
            return register(metadata, useExistingMetadata, null, tags);
        }
        Metric existing = entry.metric;
        Metadata existingMetadata = entry.metadata;
        if (useExistingMetadata && metadata.getType() != existingMetadata.getType() 
                || !useExistingMetadata && !metadata.equals(existingMetadata)) {
            throw new IllegalArgumentException(
                    String.format("Tried to retrieve metric with conflicting metadata, looking for %s, got %s",
                            metadata.toString(), existingMetadata.toString()));
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
            metadata = Metadata.builder(metadata).withType(MetricType.from(metric.getClass())).build();
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
        final Metric newMetric = metric != null ? metric : createMetricInstance(newMetadata);
        ConcurrentMap<MetricID, MetricEntry> group = metricsByNameAndId.computeIfAbsent(name, 
                key -> new ConcurrentHashMap<>());
        MetricID metricID = new MetricID(name, tags);
        MetricEntry entry = group.computeIfAbsent(metricID, key -> new MetricEntry(newMetric, newMetadata));
        if (entry.metric != metric) {
            try {
                checkReusableMetadata(name, newMetadata, entry.metadata);
            } catch (IllegalArgumentException ex) {
                remove(metricID); // this metric was illegal, remove again (best way to make sure valid path is correct)
                throw ex;
            }
        }
        return (T) entry.metric;
    }

    private static void checkNameIsNotNullOrEmpty(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name must not be null or empty");
        }
    }

    private static void checkReusableMetadata(String name, Metadata newMetadata, Metadata existingMetadata) {
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

        checkSameType(name, newMetadata, existingMetadata);

        //reusable does not apply to gauges
        if (GAUGE.equals(newMetadata.getTypeRaw())) {
            throw new IllegalArgumentException(String.format(
                    "Gauge type metric['%s'] is not reusable", name
            ));
        }

        if (!existingMetadata.equals(newMetadata)) {
            throw new IllegalArgumentException(String.format(
                  "Metadata ['%s'] already registered, does not match provided ['%s']",
                  existingMetadata.toString(), newMetadata.toString()
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

    private static Metric createMetricInstance(Metadata metadata) {
        String name = metadata.getName();
        switch (metadata.getTypeRaw()) {
        case COUNTER:
            return new CounterImpl();
        case CONCURRENT_GAUGE:
            return new ConcurrentGaugeImpl();
        case GAUGE:
            throw new IllegalArgumentException(String.format("Unsupported operation for Gauge ['%s']", name));
        case METERED:
            return new MeterImpl();
        case HISTOGRAM:
            return new HistogramImpl();
        case TIMER:
            return new TimerImpl();
        case INVALID:
        default:
            throw new IllegalStateException("Invalid metric type : " + metadata.getTypeRaw());
        }
    }

    /*
     * Non-API Methods (Extra Methods)
     */

    public Set<MetricID> getMetricsIDs(String name) {
        ConcurrentMap<MetricID, MetricEntry> group = metricsByNameAndId.get(name);
        return group == null ? emptySet() : unmodifiableSet(group.keySet()); 
    }

    public Map<MetricID, Metric> getMetrics(String name) {
        ConcurrentMap<MetricID, MetricEntry> group = metricsByNameAndId.get(name);
        return group == null 
                ? emptyMap()
                : group.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().metric));
    }

    public Metadata getMetadata(String name) {
        ConcurrentMap<MetricID, MetricEntry> group = metricsByNameAndId.get(name);
        return group == null ? null : group.values().iterator().next().metadata;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Entry<String, ConcurrentMap<MetricID, MetricEntry>> group : metricsByNameAndId.entrySet()) {
            str.append(group.getKey());
            Iterator<MetricEntry> iter = group.getValue().values().iterator();
            if (iter.hasNext()) {
                str.append(": ").append(iter.next().metadata);
            }
            str.append('\n');
            for (Entry<MetricID, MetricEntry> entry : group.getValue().entrySet()) {
                str.append('\t').append(entry.getKey()).append(": ").append(entry.getValue().metric).append('\n');
            }
        }
        return str.toString();
    }
}
