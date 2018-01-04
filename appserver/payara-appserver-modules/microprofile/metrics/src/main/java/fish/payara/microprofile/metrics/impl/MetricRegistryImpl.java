/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.metrics.impl.CounterImpl;
import fish.payara.microprofile.metrics.impl.HistogramImpl;
import fish.payara.microprofile.metrics.impl.MeterImpl;
import fish.payara.microprofile.metrics.impl.TimerImpl;
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
import javax.enterprise.inject.Vetoed;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
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
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return register(name, metric, new Metadata(name, MetricType.from(metric.getClass())));
    }

    @Override
    public <T extends Metric> T register(String name, T metric, Metadata metadata) throws IllegalArgumentException {   
        if (metricMap.containsKey(name)) {
            throw new IllegalArgumentException("A metric with name " + name + " already exists");
        }

        metricMap.put(name, metric);
        metadataMap.put(name, metadata);

        return metric;
    }

    @Override
    public Counter counter(String name) {
        return counter(new Metadata(name, MetricType.COUNTER));
    }

    @Override
    public Counter counter(Metadata metadata) {
        return getOrAdd(metadata, MetricType.COUNTER);
    }

    @Override
    public Histogram histogram(String name) {
        return histogram(new Metadata(name, MetricType.HISTOGRAM));
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return getOrAdd(metadata, MetricType.HISTOGRAM);
    }

    @Override
    public Meter meter(String name) {
        return meter(new Metadata(name, MetricType.METERED));
    }

    @Override
    public Meter meter(Metadata metadata) {
        return getOrAdd(metadata, MetricType.METERED);
    }

    @Override
    public Timer timer(String name) {
        return timer(new Metadata(name, MetricType.TIMER));
    }

    @Override
    public Timer timer(Metadata metadata) {
        return getOrAdd(metadata, MetricType.TIMER);
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
    public SortedSet<String> getNames() {
        return new TreeSet<>(metricMap.keySet());
    }

    @Override
    public SortedMap<String, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Gauge> getGauges(MetricFilter metricFilter) {
        return getMetrics(Gauge.class, metricFilter);
    }

    @Override
    public SortedMap<String, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Counter> getCounters(MetricFilter metricFilter) {
        return getMetrics(Counter.class, metricFilter);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms(MetricFilter metricFilter) {
        return getMetrics(Histogram.class, metricFilter);
    }

    @Override
    public SortedMap<String, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Meter> getMeters(MetricFilter metricFilter) {
        return getMetrics(Meter.class, metricFilter);
    }

    @Override
    public SortedMap<String, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Timer> getTimers(MetricFilter metricFilter) {
        return getMetrics(Timer.class, metricFilter);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return new HashMap<>(metricMap);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return new HashMap<>(metadataMap);
    }

    private <T extends Metric> T getOrAdd(Metadata metadata, MetricType type) {
        String name = metadata.getName();
        if (!metadataMap.containsKey(name)) {
            Metric metric;
            switch (type) {
                case COUNTER:
                    metric = new CounterImpl();
                    break;
                case GAUGE:
                    throw new IllegalArgumentException("Gauge " + name + " was not registered, this should not happen");
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
                    throw new IllegalStateException("Invalid metrics type");
            }
            register(name, metric, metadata);
        }
        return (T) metricMap.get(name);
    }

    private <T extends Metric> SortedMap<String, T> getMetrics(Class<T> klass, MetricFilter filter) {
        SortedMap<String, T> out = new TreeMap<>();

        Iterator<Map.Entry<String, Metric>> iterator = metricMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Metric> entry = iterator.next();
            if (klass.isInstance(entry.getValue())
                    && filter.matches(entry.getKey(), entry.getValue())) {
                out.put(entry.getKey(), (T) entry.getValue());
            }
        }

        return Collections.unmodifiableSortedMap(out);
    }

}
