/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import fish.payara.microprofile.metrics.cdi.MetricUtils;
import jakarta.enterprise.inject.Vetoed;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.microprofile.metrics.MetricFilter.ALL;

/**
 * The MetricRegistry stores the metrics and metadata information
 */
@Vetoed
public class MetricRegistryImpl implements MetricRegistry {

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

    private final String scope;
    private final ConcurrentMap<String, MetricFamily<?>> metricsFamiliesByName = new ConcurrentHashMap<>();
    private final Clock clock;
    private final List<MetricRegistrationListener> listeners = new ArrayList<>();

    public static final String METRIC_PERCENTILES_PROPERTY = "mp.metrics.distribution.percentiles";

    public static final String METRIC_HISTOGRAM_BUCKETS_PROPERTY = "mp.metrics.distribution.histogram.buckets";
    
    public static final String METRIC_TIMER_BUCKETS_PROPERTY = "mp.metrics.distribution.timer.buckets";

    private Map<String, Collection<MetricsCustomPercentiles>> percentilesConfigMap = 
            new HashMap<String, Collection<MetricsCustomPercentiles>>();
    
    private Map<String, Collection<MetricsCustomBuckets>> histogramBucketsConfigMap =
            new HashMap<>();
    
    private Map<String, Collection<MetricsCustomBuckets>> timerBucketsConfigMap =
            new HashMap<>();

    public MetricRegistryImpl() {
        this.scope = null;
        this.clock = Clock.defaultClock();
    }

    public MetricRegistryImpl(String registryScope) {
        this(registryScope, Clock.defaultClock());
    }

    public MetricRegistryImpl(String type, Clock clock) {
        this.scope = type;
        this.clock = clock;
    }

    public MetricRegistryImpl addListener(MetricRegistrationListener listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public Counter counter(String name) {
        return findMetricOrCreate(name, Counter.class.getTypeName(), new CounterImpl(),
                new Tag[0]);
    }

    @Override
    public Counter counter(Metadata metadata) {
        return findMetricOrCreate(metadata, Counter.class.getTypeName());
    }

    @Override
    public Counter counter(String name, Tag... tags) {
        return findMetricOrCreate(name, Counter.class.getTypeName(), new CounterImpl(), tags);
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, Counter.class.getTypeName(), tags);
    }

    @Override
    public Counter counter(MetricID metricID) {
        return findMetricOrCreate(metricID.getName(), Counter.class.getTypeName(), new CounterImpl(), metricID.getTagsAsArray());
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(Metadata metadata, T object, Function<T, R> func, Tag... tags) {
        return gauge(metadata, () -> func.apply(object), tags);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> supplier) {
        return gauge(metricID.getName(), supplier, metricID.getTagsAsArray());
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T object, Function<T, R> func) {
        return gauge(metricID, () -> func.apply(object));
    }

    @Override
    public <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> supplier, Tag... tags) {
        return findMetricOrCreate(metadata, Gauge.class.getTypeName(), createGauge(supplier), tags);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(String name, Supplier<T> supplier, Tag... tags) {
        return findMetricOrCreate(name, Gauge.class.getTypeName(), createGauge(supplier), tags);
    }

    /**
     * This is a non-standard feature that the {@link Supplier} passed to a {@code gauge}-method can implement
     * {@link Gauge} as well in which case the passed instance is not wrapped in another lambda to get to {@link Gauge}
     * interface.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Number> Gauge<T> createGauge(Supplier<T> supplier) {
        return supplier instanceof Gauge ? (Gauge<T>) supplier : () -> supplier.get();
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(String name, T object, Function<T, R> func, Tag... tags) {
        return gauge(name, () -> func.apply(object), tags);
    }

    @Override
    public Histogram histogram(String name, Tag... tags) {
        return findMetricOrCreate(name, Histogram.class.getTypeName(), new HistogramImpl(name, percentilesConfigMap, histogramBucketsConfigMap), tags);
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, Histogram.class.getTypeName(),new HistogramImpl(metadata.getName(), 
                percentilesConfigMap, histogramBucketsConfigMap), tags);
    }

    @Override
    public Histogram histogram(String name) {
        return findMetricOrCreate(name, Histogram.class.getTypeName(), 
                new HistogramImpl(name, percentilesConfigMap, histogramBucketsConfigMap),
                new Tag[0]);
    }

    @Override
    public Histogram histogram(Metadata metadata) {
         return findMetricOrCreate(metadata, Histogram.class.getTypeName());
    }

    @Override
    public Histogram histogram(MetricID metricID) {
        return findMetricOrCreate(metricID.getName(), Histogram.class.getTypeName(), new HistogramImpl(metricID.getName(), 
                        percentilesConfigMap, histogramBucketsConfigMap), 
                metricID.getTagsAsArray());
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        return findMetricOrCreate(name, Timer.class.getTypeName(),  null, tags);
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return findMetricOrCreate(metadata, Timer.class.getTypeName(), null, tags);
    }

    @Override
    public Timer timer(String name) {
        return findMetricOrCreate(name, Timer.class.getTypeName(), null, new Tag[0]);
    }

    @Override
    public Timer timer(Metadata metadata) {
        return findMetricOrCreate(metadata, Timer.class.getTypeName());
    }

    @Override
    public Timer timer(MetricID metricID) {
        return findMetricOrCreate(metricID.getName(), Timer.class.getTypeName(), null, metricID.getTagsAsArray());
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
    public Metric getMetric(MetricID metricID) {
        MetricFamily<?> family = metricsFamiliesByName.get(metricID.getName());
        return family == null ? null : family.get(metricID);
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
    public String getScope() {
        return scope;
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

    private <T extends Metric> T findMetricOrCreate(String name, String typeName, T metric, Tag... tags) {
        checkNameIsNotNullOrEmpty(name);
        Metadata metadata = null;
        if(Timer.class.getTypeName().equals(typeName)) {
            metadata = Metadata.builder()
                    .withName(name)
                    .withUnit(MetricUnits.SECONDS)
                    .build();
        } else {
            metadata = Metadata.builder()
                    .withName(name)
                    .build();
        }
        return findMetricOrCreate(metadata, true, typeName, metric, tags);
    }

    private <T extends Metric> T findMetricOrCreate(Metadata metadata, String typeName, Tag... tags) {
        return findMetricOrCreate(metadata, typeName, null, tags);
    }

    private <T extends Metric> T findMetricOrCreate(Metadata metadata, String typeName, T metric, Tag... tags) {
        return findMetricOrCreate(withType(metadata), false, typeName, metric, tags);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T findMetricOrCreate(Metadata metadata, boolean useExistingMetadata, String metricType, T metric, Tag... tags) {
        validateTags(tags);
        MetricID metricID = new MetricID(metadata.getName(), tags);
        MetricFamily<?> family = metricsFamiliesByName.get(metricID.getName());
        if (family == null) {
            return register(metadata, useExistingMetadata, metricType, metric, tags);
        }
        Metric existing = family.get(metricID);
        if(existing == null) {
            checkSameType(metricID, metricType, family.metrics);
            if((useExistingMetadata && hasSameTagNames(metricID)) || hasSameTagNames(metricID)) {
                return register(metadata, useExistingMetadata, metricType, metric, tags);
            } else {
                throw new IllegalArgumentException(
                        String.format("Tried to lookup a metric id with conflicting tags,  %s", metricID.toString()));
            }
        }
        return (T) existing;
    }
    
    /**
     * This method will check if there is an available metric with the same tag names
     * @param metricID used to search an available metric
     * @return boolean indicating if there is an available Metric with same or different tag names
     */
    private boolean hasSameTagNames(MetricID metricID) {
        Optional<MetricID> optSameNameDifferentTags = getMetrics(metricID.getName()).keySet().stream()
                .filter(m -> m.getTags().size() != metricID.getTags().size()).findAny();
        if(optSameNameDifferentTags.isPresent()) {
            return false;
        }
        Optional<MetricID> optSameNameSameTagsDifferentContent = getMetrics(metricID.getName()).keySet().stream()
                .filter(m -> m.getTags().size() == metricID.getTags().size())
                .filter(m -> !m.getTagsAsString().equals(metricID.getTagsAsString())).findAny();
        if(optSameNameSameTagsDifferentContent.isPresent()) {
            List<MetricID> metrics = getMetrics(metricID.getName()).keySet().stream()
                    .filter(m -> m.getTags().size() == metricID.getTags().size()).collect(Collectors.toList());
            for(Tag t : metricID.getTagsAsArray()) {
                Optional<MetricID> result = metrics.stream().filter(m -> m.getTags().containsKey(t.getTagName())).findAny();
                if(result.isEmpty()){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method validates if the mp_scope and the mp_app tag names are used for the metric.
     * If that is the case this will throw an IllegalArgumentException indicating that those are reserved tag names
     * @param tags the array of available tags for the metric before to be created
     */
    public static void validateTags(Tag[] tags) {
        if(tags != null) {
            Optional<Tag> result = Arrays.stream(tags)
                    .filter(t -> t.getTagName().equals(MetricUtils.TAG_METRIC_MP_SCOPE_NAME)
                            || t.getTagName().equals(MetricUtils.TAG_METRIC_MP_APP_NAME)).findFirst();
            if (result.isPresent()) {
                throw new IllegalArgumentException("invalid tags: " + tags +
                        ", tags must not contain following reserved tag names: mp_scope and mp_app");
            }
        }
    }

    public <T extends Metric> T register(Metadata metadata, String metricType, T metric, Tag... tags) throws IllegalArgumentException {
        return register(metadata, false, metricType, metric, tags);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T register(Metadata metadata, boolean useExistingMetadata, String metricType, T metric, Tag... tags) {
        String name = metadata.getName();
        checkNameIsNotNullOrEmpty(name);
        if (useExistingMetadata) {
            Metadata existingMetadata = getMetadata(name);
            if (existingMetadata != null) {
                metadata = existingMetadata;
            }
        }
        final Metadata newMetadata = metadata;
        //verify here the new properties
        final T newMetric = metric != null ? metric:(T) createMetricInstance(newMetadata, metricType);
        MetricFamily<T> family = (MetricFamily<T>) metricsFamiliesByName.computeIfAbsent(name,
                key -> new MetricFamily<>(newMetadata));
        MetricID metricID = new MetricID(name, tags);
        if (family.metadata != newMetadata) {
            checkSameType(metricID, metricType, family.metrics);
            checkReusableMetadata(name, newMetadata, family.metadata);
        }
        T current = family.metrics.computeIfAbsent(metricID, key -> newMetric);
        notifyRegistrationListeners(metricID);
        return current;
    }

    private void notifyRegistrationListeners(MetricID metricID) {
        for (MetricRegistrationListener l : listeners) {
            try {
                l.onRegistration(metricID, scope);
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

    private static void checkReusableMetadata(String name, Metadata newMetadata, Metadata existingMetadata) {
        if (!existingMetadata.equals(newMetadata)) {
            throw new IllegalArgumentException(String.format(
                  "Metadata ['%s'] already registered, does not match provided ['%s']",
                  existingMetadata.toString(), newMetadata.toString()
          ));
        }
    }

    public <T extends Metric> void checkSameType(MetricID metricID, String metricType, ConcurrentMap<MetricID, ?> metrics) {
        Optional<Class<?>> optResult = Optional.empty();
        Optional<Class<?>[]> interfacesArrayOpt = metrics.values().stream().map(c -> c.getClass().getInterfaces()).findAny();
        if(interfacesArrayOpt.isPresent()) {
            optResult = Arrays.stream(interfacesArrayOpt.get()).filter(i->!i.getTypeName().contains(Supplier.class.getName()))
                    .filter(i->!i.getTypeName().equals(metricType)).findAny();
        }
        if(optResult.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "Metric ['%s'] type['%s'] does not match with existing type['%s']",
                    metricID.getName(), getMetricClassName(metricType), getMetricClassName(optResult.get().getName())
            ));
        }
    }
    
    public String getMetricClassName(String name) {
        String[] parts = name.split("\\.");
        if(parts != null && parts.length > 0) {
            return parts[parts.length -1];
        }
        return name;
    }

    private Metric createMetricInstance(Metadata metadata, String metricType) {
        String name = MetricRegistry.name(metadata.getName());

        if(Counter.class.getName().equals(metricType)) {
            return new CounterImpl();
        }
        if(Gauge.class.getName().equals(metricType)) {
            throw new IllegalArgumentException(String.format("Unsupported operation for Gauge ['%s']", name));
        }

        if(Histogram.class.getName().equals(metricType)) {
            return new HistogramImpl();
        }

        if(Timer.class.getName().equals(metricType)) {
            return new TimerImpl(name, percentilesConfigMap, timerBucketsConfigMap, clock);
        }

        throw new IllegalArgumentException("Invalid metric type : "+metricType);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Metric> T getMetricCustomScope(MetricID metricID, Class<T> ofType, String scope) {
        return this.getMetric(metricID, ofType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> ofType) {
        MetricFamily<?> family = metricsFamiliesByName.get(metricID.getName());
        if (family == null) {
            return null;
        }
        Metric metric = family.get(metricID);
        if (metric != null && !ofType.isAssignableFrom(metric.getClass())) {
            throw new IllegalArgumentException("Invalid metric type : " + ofType);
        }
        if(metric == null) {
            metric = verifyMetrics(metricID, family);
        }
        return (T) metric;
    }
    
    public <T extends Metric> T verifyMetrics(MetricID metricID, MetricFamily<?> family) {
        Metric m = null;
        List<MetricID> metricList =
                this.getMetrics().entrySet().stream()
                        .filter(e -> e.getKey().getName().equals(metricID.getName()))
                        .filter(e -> e.getKey().getTags().size() == metricID.getTags().size())
                        .map(e -> e.getKey()).collect(Collectors.toList());
        if (metricList.size() > 0) {
            Optional<MetricID> metricOptional = metricList.stream()
                    .filter(l -> l.getTags().equals(metricID.getTags())).findAny();
            if (metricOptional.isPresent()) {
                m = family.get(metricOptional.get());
            }
        }
        return (T) m;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> ofType, MetricFilter filter) {
        return (SortedMap<MetricID, T>) findMetrics(
                (metricID, metric) -> filter.matches(metricID, metric) && ofType.isAssignableFrom(metric.getClass()));
    }

    @Override
    public SortedMap<MetricID, Metric> getMetrics(MetricFilter filter) {
        return findMetrics(filter);
    }

    @Override
    public Metadata getMetadata(String name) {
        MetricFamily<?> family = metricsFamiliesByName.get(name);
        return family == null ? null : family.metadata;
    }

    /*
     * Non-API Methods (Extra Methods)
     */

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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Entry<String, MetricFamily<?>> family : metricsFamiliesByName.entrySet()) {
            str.append(family.getKey()).append(": ").append(family.getValue().metadata).append('\n');
            for (Entry<MetricID, ? extends Metric> entry : family.getValue().metrics.entrySet()) {
                str.append('\t').append(entry.getKey()).append(": ").append(toString(entry.getValue())).append('\n');
            }
        }
        return str.toString();
    }

    private static String toString(Metric metric) {
        if (isLambda(metric) && metric instanceof Gauge) {
            return "Gauge["+((Gauge<?>)metric).getValue()+"]";
        }
        return metric.toString();
    }

    public static boolean isLambda(Object obj) {
        return obj.getClass().toString().contains("$$Lambda$");
    }

    /**
     * This is a workaround to prevent setting display name field as side effect of using
     * {@link Metadata#builder(Metadata)}. It does not prevent the issue in case the type needs setting but it can if it
     * does not need to be set. There is no API way to make the resulting {@link Metadata} identical in case the type is
     * set.
     */
    private static Metadata withType(Metadata metadata) {
        return Metadata.builder(metadata).build();
    }
}
