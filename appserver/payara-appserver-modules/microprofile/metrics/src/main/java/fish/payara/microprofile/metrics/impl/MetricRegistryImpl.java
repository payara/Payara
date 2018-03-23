/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import static java.util.stream.Collectors.toMap;
import javax.enterprise.inject.Vetoed;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import static org.eclipse.microprofile.metrics.MetricFilter.ALL;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import static org.eclipse.microprofile.metrics.MetricType.HISTOGRAM;
import static org.eclipse.microprofile.metrics.MetricType.METERED;
import static org.eclipse.microprofile.metrics.MetricType.TIMER;
import org.eclipse.microprofile.metrics.Timer;

/**
 * The MetricRegistry stores the metrics and metadata information
 */
@Vetoed
public class MetricRegistryImpl extends MetricRegistry {

    private final Map<String, Metric> metricMap;
    private final Map<String, Metadata> metadataMap;

    public MetricRegistryImpl() {
        this.metricMap = new ConcurrentHashMap<>();
        this.metadataMap = new ConcurrentHashMap<>();
    }

    @Override
    public Counter counter(String name) {
        return counter(new Metadata(name, COUNTER));
    }

    @Override
    public Counter counter(Metadata metadata) {
        return getOrAdd(metadata, COUNTER);
    }

    @Override
    public Histogram histogram(String name) {
        return histogram(new Metadata(name, HISTOGRAM));
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return getOrAdd(metadata, HISTOGRAM);
    }

    @Override
    public Meter meter(String name) {
        return meter(new Metadata(name, METERED));
    }

    @Override
    public Meter meter(Metadata metadata) {
        return getOrAdd(metadata, METERED);
    }

    @Override
    public Timer timer(String name) {
        return timer(new Metadata(name, TIMER));
    }

    @Override
    public Timer timer(Metadata metadata) {
        return getOrAdd(metadata, TIMER);
    }

    @Override
    public SortedSet<String> getNames() {
        return new TreeSet<>(metricMap.keySet());
    }

    @Override
    public SortedMap<String, Gauge> getGauges() {
        return getGauges(ALL);
    }

    @Override
    public SortedMap<String, Gauge> getGauges(MetricFilter metricFilter) {
        return findMetrics(Gauge.class, metricFilter);
    }

    @Override
    public SortedMap<String, Counter> getCounters() {
        return getCounters(ALL);
    }

    @Override
    public SortedMap<String, Counter> getCounters(MetricFilter metricFilter) {
        return findMetrics(Counter.class, metricFilter);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms() {
        return getHistograms(ALL);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms(MetricFilter metricFilter) {
        return findMetrics(Histogram.class, metricFilter);
    }

    @Override
    public SortedMap<String, Meter> getMeters() {
        return getMeters(ALL);
    }

    @Override
    public SortedMap<String, Meter> getMeters(MetricFilter metricFilter) {
        return findMetrics(Meter.class, metricFilter);
    }

    @Override
    public SortedMap<String, Timer> getTimers() {
        return getTimers(ALL);
    }

    @Override
    public SortedMap<String, Timer> getTimers(MetricFilter metricFilter) {
        return findMetrics(Timer.class, metricFilter);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return new HashMap<>(metricMap);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return new HashMap<>(metadataMap);
    }
    
    @Override
    public boolean remove(String name) {
        final Metric metric = metricMap.remove(name);
        metadataMap.remove(name);
        return metric != null;
    }

    @Override
    public void removeMatching(MetricFilter metricFilter) {
        Iterator<Entry<String, Metric>> iterator = metricMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Metric> entry = iterator.next();
            if (metricFilter.matches(entry.getKey(), entry.getValue())) {
                remove(entry.getKey());
            }
        }
    }
    
    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return register(name, metric, new Metadata(name, MetricType.from(metric.getClass())));
    }

    @Deprecated
    @Override
    public <T extends Metric> T register(String name, T metric, Metadata metadata) throws IllegalArgumentException {
        metadata.setName(name);
        return register(metadata, metric);
        
    }
    
    @Override
    public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
           
        String name = metadata.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name must not be null or empty");
        }
        
        Metadata existingMetadata = metadataMap.get(name);
        
        if (existingMetadata != null) {
            //if existing metric declared not reusable
            if (!existingMetadata.isReusable()) {
                throw new IllegalArgumentException(String.format(
                        "Metric ['%s'] already exists and declared not reusable", name
                ));
            }

            //Only metrics of the same type can be reused under the same name
            if (!existingMetadata.getTypeRaw().equals(metadata.getTypeRaw())) {
                throw new IllegalArgumentException(String.format(
                        "Metric ['%s'] type['%s'] does not match with existing type['%s']",
                        name, metadata.getType(), existingMetadata.getType()
                ));
            }
            
            //reusable does not apply to gauges
            if(GAUGE.equals(metadata.getTypeRaw())) {
                throw new IllegalArgumentException(String.format(
                        "Gauge type metric['%s'] is not reusable", name
                ));
            }
        }

        metricMap.put(name, metric);
        metadataMap.put(name, metadata);

        return metric;
    }

    private <T extends Metric> T getOrAdd(Metadata metadata, MetricType metricType) {
        String name = metadata.getName();
        Metadata existingMetadata = metadataMap.get(name);
        if (existingMetadata == null) {
            Metric metric;
            switch (metricType) {
                case COUNTER:
                    metric = new CounterImpl();
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
                    throw new IllegalStateException("Invalid metric type : " + metricType);
            }
            register(metadata, metric);
        }  else if (!existingMetadata.getTypeRaw().equals(metadata.getTypeRaw())) {
            throw new IllegalArgumentException(String.format(
                        "Metric ['%s'] type['%s'] does not match with existing type['%s']",
                        name, metadata.getType(), existingMetadata.getType()
                ));
        }
        return (T) metricMap.get(name);
    }

    private <T extends Metric> SortedMap<String, T> findMetrics(Class<T> metricClass, MetricFilter metricFilter) {
        SortedMap<String, T> out = metricMap.entrySet().stream()
                .filter(e -> metricClass.isInstance(e.getValue()))
                .filter(e -> metricFilter.matches(e.getKey(), e.getValue()))
                .collect(toMap(Entry::getKey, e -> (T) e.getValue(), (e1, e2) -> e1, TreeMap::new));
        return Collections.unmodifiableSortedMap(out);
    }

}
