/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static java.util.stream.Collectors.toMap;
import javax.enterprise.inject.Vetoed;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
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

    private final ConcurrentMap<MetricID, Metric> metricMap;
    private final ConcurrentMap<String, Metadata> metadataMap;

    public MetricRegistryImpl() {
        this.metricMap = new ConcurrentHashMap<>();
        this.metadataMap = new ConcurrentHashMap<>();
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
    public Timer timer(String name) {
        return findMetricOrCreate(name, TIMER, new Tag[0]);
    }

    @Override
    public Timer timer(Metadata metadata) {
        return findMetricOrCreate(metadata, TIMER);
    }

    @Override
    public SortedSet<String> getNames() {
        SortedSet<String> names = new TreeSet<>();
        for (MetricID metricID: metricMap.keySet()) {
            names.add(metricID.getName());
        }
        return names;
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
        return Collections.unmodifiableMap(metricMap);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Collections.unmodifiableMap(metadataMap);
    }

    @Override
    public synchronized boolean remove(String name) {
        boolean removedAny = false;
        for (MetricID metricID : metricMap.keySet()) {
            if (metricID.getName().equals(name)) {
                metricMap.remove(metricID);
                removedAny = true;
            }
        }
        metadataMap.remove(name);
        return removedAny;
    }

    @Override
    public synchronized boolean remove(MetricID mid) {
        metadataMap.remove(mid.getName());
        return metricMap.remove(mid) != null;
    }

    @Override
    public void removeMatching(MetricFilter metricFilter) {
        Iterator<Entry<MetricID, Metric>> iterator = metricMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<MetricID, Metric> entry = iterator.next();
            if (metricFilter.matches(entry.getKey(), entry.getValue())) {
                remove(entry.getKey());
            }
        }
    }
    
    private <T extends Metric> T findMetricOrCreate(String name, MetricType metricType, Tag[] tags) {
        Metadata existingMetadata = metadataMap.get(name);
        if (existingMetadata != null) {
            if (existingMetadata.getTypeRaw().equals(metricType)) {
                Metric existingMetric = metricMap.get(new MetricID(name, tags));
                if (existingMetric != null) {
                    return (T) existingMetric;
                } else {
                    return register(existingMetadata, null, tags);
                }
            } else {
                throw new IllegalArgumentException(String.format("Tried to retrieve metric with conflicting MetricType, looking for %s, got %s",
                        metricType, metadataMap.get(name).getTypeRaw().equals(metricType)));
            }
        } else {
            Metadata metadata = Metadata.builder().withName(name).withType(metricType).build();
            return register(metadata, null, tags);
        }
    }
    
    private <T extends Metric> T findMetricOrCreate(Metadata metadata, MetricType metricType) {
        Metric existing = metricMap.get(new MetricID(metadata.getName()));
        Metadata existingMetadata = metadataMap.get(metadata.getName());
        Metadata newMetadata = Metadata.builder(metadata).withType(metricType).build();
        if (existing != null) {
            if (newMetadata.equals(existingMetadata)) {
                return (T) existing;
            } else {
                throw new IllegalArgumentException(String.format("Tried to retrieve metric with conflicting metadata, looking for %s, got %s",
                        newMetadata.toString(), existingMetadata.toString()));
            }
        } else {
            return addMetric(newMetadata);
        }
    }
    
    private <T extends Metric> T findMetricOrCreate(Metadata metadata, MetricType metricType, Tag[] tags) {
        Metric existing = metricMap.get(new MetricID(metadata.getName(), tags));
        Metadata existingMetadata = metadataMap.get(metadata.getName());
        Metadata newMetadata = Metadata.builder(metadata).withType(metricType).build();
        if (existing != null) {
            if (newMetadata.equals(existingMetadata)) {
                return (T) existing;
            } else {
                throw new IllegalArgumentException(String.format("Tried to retrieve metric with conflicting metadata, looking for %s, got %s",
                        newMetadata.toString(), existingMetadata.toString()));
            }
        } else {
            return register(newMetadata, null, tags);
        }
    }

    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return register(Metadata.builder().withName(name).withType(MetricType.from(metric.getClass())).build(), metric);
    }

    @Override
    public <T extends Metric> T register(Metadata newMetadata, T metric, Tag... tags) throws IllegalArgumentException {

        String name = newMetadata.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name must not be null or empty");
        }

        Metadata existingMetadata = metadataMap.get(name);
        MetricID metricID = new MetricID(name, tags);

        if (existingMetadata == null) {
            if (metric == null) {
                metric = createMetricInstance(newMetadata);
            }
            synchronized (this) {
                metricMap.put(metricID, metric);
                metadataMap.put(name, Metadata.builder(newMetadata).build());
            }
        } else if (existingMetadata.equals(newMetadata)) {
            T existingMetric = (T) metricMap.get(metricID);
            if (existingMetric == null) {
                //same metadata, different tags
                if (metric == null) {
                    metric = createMetricInstance(newMetadata);
                }
                metricMap.put(metricID, metric);
            } else if (existingMetadata.isReusable() || newMetadata.isReusable()) {

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

                //Only metrics of the same type can be reused under the same name
                if (!existingMetadata.getTypeRaw().equals(newMetadata.getTypeRaw())) {
                    throw new IllegalArgumentException(String.format(
                            "Metric ['%s'] type['%s'] does not match with existing type['%s']",
                            name, newMetadata.getType(), existingMetadata.getType()
                    ));
                }

                //reusable does not apply to gauges
                if (GAUGE.equals(newMetadata.getTypeRaw())) {
                    throw new IllegalArgumentException(String.format(
                            "Gauge type metric['%s'] is not reusable", name
                    ));
                }

                metric = (T) metricMap.get(new MetricID(name, tags));
            } else {
                  throw new IllegalArgumentException(String.format(
                            "Metric ['%s'] already exists and declared not reusable", name
                    ));
            }
        } else {
                throw new IllegalArgumentException(String.format(
                        "Metadata ['%s'] already registered, does not match provided ['%s']",
                        existingMetadata.toString(), newMetadata.toString()
                ));
        }
        return metric;
    }
    
    @Override
    public <T extends Metric> T register(Metadata metadata, T t) throws IllegalArgumentException {
        return register(metadata, t, new Tag[0]);
    }

    private <T extends Metric> T addMetric(Metadata metadata) {
        return register(metadata, null);
    }

    private <T extends Metric> T createMetricInstance(Metadata metadata) {
        String name = metadata.getName();
        Metric metric;
        switch (metadata.getTypeRaw()) {
            case COUNTER:
                metric = new CounterImpl();
                break;
            case CONCURRENT_GAUGE:
                metric = new ConcurrentGaugeImpl();
                break;
            case GAUGE:
                throw new IllegalArgumentException(String.format(
                        "Unsupported operation for Gauge ['%s']", name
                ));
            case METERED:
                metric = new MeterImpl();
                break;
            case HISTOGRAM:
                metric = new HistogramImpl();
                break;
            case TIMER:
                metric = new TimerImpl();
                break;
            case INVALID:
            default:
                throw new IllegalStateException("Invalid metric type : " + metadata.getTypeRaw());
        }
        return (T)metric;
    }

    private <T extends Metric> SortedMap<MetricID, T> findMetrics(Class<T> metricClass, MetricFilter metricFilter) {
        SortedMap<MetricID, T> out = metricMap.entrySet().stream()
                .filter(e -> metricClass.isInstance(e.getValue()))
                .filter(e -> metricFilter.matches(e.getKey(), e.getValue()))
                .collect(toMap(Entry::getKey, e -> (T) e.getValue(), (e1, e2) -> e1, TreeMap::new));
        return Collections.unmodifiableSortedMap(out);
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
        TreeSet<MetricID> metricIDs = new TreeSet<>();
        for (MetricID metricID: metricMap.keySet()) {
            metricIDs.add(metricID);
        }
        return metricIDs;
    }

}
