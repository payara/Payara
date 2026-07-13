/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.faulttolerance.service;

import static java.lang.System.arraycopy;
import static org.eclipse.microprofile.metrics.MetricUnits.NANOSECONDS;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;

/**
 * The {@link MethodFaultToleranceMetrics} is a {@link FaultToleranceMetrics} for a particular {@link Method}.
 *
 * @author Jan Bernitt
 */
public final class MethodFaultToleranceMetrics implements FaultToleranceMetrics {

    private final MetricRegistry registry;
    /**
     * This is "cached" as soon as an instance is bound using the
     * {@link #FaultToleranceMetricsFactory(MetricRegistry, String)} constructor.
     */
    private final String canonicalMethodName;
    private final AtomicBoolean registered;
    private final Map<MetricID, Counter> countersByMetricID;
    private final Map<MetricID, Histogram> histogramsByMetricID;
    private FallbackUsage fallbackUsage;
    private boolean retried;

    public MethodFaultToleranceMetrics(MetricRegistry registry, String canonicalMethodName) {
        this(registry, canonicalMethodName, FallbackUsage.notDefined, new AtomicBoolean(), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
    }

    private MethodFaultToleranceMetrics(MetricRegistry registry, String canonicalMethodName, FallbackUsage fallbackUsage,
            AtomicBoolean registered, Map<MetricID, Counter> countersByMetricID, Map<MetricID, Histogram> histogramsByMetricID) {
        this.registry = registry;
        this.canonicalMethodName = canonicalMethodName;
        this.fallbackUsage = fallbackUsage;
        this.registered = registered;
        this.countersByMetricID = countersByMetricID;
        this.histogramsByMetricID = histogramsByMetricID;
    }

    /**
     * The first thread calling creates the metrics all thread calling the same method work with. Other threads have to
     * wait until these metrics actually exist. The easiest to make that happen was to make this method synchronised.
     * The {@link #registered} is still an {@link AtomicBoolean} so that a state change affects other
     * {@link MethodFaultToleranceMetrics} instances for the same method that were created in the meantime as well.
     */
    @Override
    public synchronized FaultToleranceMetrics boundTo(FaultToleranceMethodContext context, FaultTolerancePolicy policy) {
        if (registered.compareAndSet(false, true)) {
            FaultToleranceMetrics.super.boundTo(context, policy); // trigger registration if needed
        }
        return new MethodFaultToleranceMetrics(registry, canonicalMethodName,
                policy.isFallbackPresent() ? FallbackUsage.notApplied : FallbackUsage.notDefined,
                registered, countersByMetricID, histogramsByMetricID);
    }

    /*
     * General Metrics
     */


    @Override
    public void register(String metric, String unit, LongSupplier gauge, String... tag) {
        MetricID key = withMethodTag(metric, asTag(tag));
        if (unit == null || MetricUnits.NONE.equals(unit)) {
            registry.gauge(key, () -> gauge.getAsLong());
        } else {
            registry.gauge(withUnit(key, unit), () -> gauge.getAsLong(), key.getTagsAsArray());
        }
    }

    @Override
    public void register(String metricType, String metric, String[]... tagsPermutations) {
        if (metricType.equals(Counter.class.getTypeName())) {
            registerPermutations(tagsPermutations, tags ->
                    countersByMetricID.computeIfAbsent(withMethodTag(metric, tags),
                            key -> registry.counter(key)));
        } else if (metricType.equals(Histogram.class.getTypeName())) {
            registerPermutations(tagsPermutations, tags ->
                    histogramsByMetricID.computeIfAbsent(withMethodTag(metric, tags),
                            key -> registry.histogram(withUnit(key, NANOSECONDS), key.getTagsAsArray())));
        } else {
            throw new UnsupportedOperationException("Only counter and histogram are supported but got: " + metricType);
        }
    }

    private static void registerPermutations(String[][] tags, Consumer<Tag[]> register) {
        if (tags.length == 0) {
            register.accept(NO_TAGS);
            return;
        }
        if (tags.length == 1) {
            String[] tag1 = tags[0];
            for (int i = 1; i < tag1.length; i++) {
                register.accept(new Tag[] { new Tag(tag1[0], tag1[i]) });
            }
            return;
        }
        if (tags.length == 2) {
            String[] tag1 = tags[0];
            String[] tag2 = tags[1];
            for (int i = 1; i < tag1.length; i++) {
                for (int j = 1; j < tag2.length; j++) {
                    register.accept(new Tag[] { new Tag(tag1[0], tag1[i]), new Tag(tag2[0], tag2[j])});
                }
            }
            return;
        }
        throw new UnsupportedOperationException("Only 0 to 2 tags supported but got: " + tags.length);
    }

    private static Tag[] asTag(String... tag) {
        return tag.length == 0 ? NO_TAGS : new Tag[] { new Tag(tag[0], tag[1])};
    }

    @Override
    public void incrementCounter(String metric, Tag... tags) {
        countersByMetricID.get(withMethodTag(metric, tags)).inc();
    }

    @Override
    public void addToHistogram(String metric, long duration, Tag... tags) {
        histogramsByMetricID.get(withMethodTag(metric, tags)).update(duration);
    }

    private static Metadata withUnit(MetricID key, String unit) {
        return Metadata.builder().withName(key.getName()).withUnit(unit).build();
    }

    private MetricID withMethodTag(String metric, Tag[] tags) {
        Tag method = new Tag("method", canonicalMethodName);
        if (tags.length == 0) {
            return new MetricID(metric, method);
        }
        Tag[] newTags = new Tag[tags.length + 1];
        newTags[0] = method;
        arraycopy(tags, 0, newTags, 1, tags.length);
        return new MetricID(metric, newTags);
    }

    /*
     * @Retry, @Timeout, @CircuitBreaker, @Bulkhead and @Fallback
     */

    @Override
    public FallbackUsage getFallbackUsage() {
        return fallbackUsage;
    }

    /*
     * @Fallback
     */

    @Override
    public void incrementFallbackCallsTotal() {
        fallbackUsage = FaultToleranceMetrics.FallbackUsage.applied;
    }

    /*
     * @Retry
     */

    @Override
    public void incrementRetryRetriesTotal() {
        retried = true;
        FaultToleranceMetrics.super.incrementRetryRetriesTotal();
    }

    @Override
    public boolean isRetried() {
        return retried;
    }
}
