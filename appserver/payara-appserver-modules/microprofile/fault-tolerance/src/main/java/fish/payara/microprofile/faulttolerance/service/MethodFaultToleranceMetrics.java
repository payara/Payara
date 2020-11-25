/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;

/**
 * The {@link MethodFaultToleranceMetrics} is a {@link FaultToleranceMetrics} for a particular {@link Method}.
 *
 * @author Jan Bernitt
 */
final class MethodFaultToleranceMetrics implements FaultToleranceMetrics {

    private final MetricRegistry registry;
    /**
     * This is "cached" as soon as an instance is bound using the
     * {@link #FaultToleranceMetricsFactory(MetricRegistry, String)} constructor.
     */
    private final String canonicalMethodName;
    private final Map<MetricID, Counter> countersByMetricID;
    private final Map<MetricID, Histogram> histogramsByMetricID;
    private final Map<MetricID, Gauge<Long>> gaugesByMetricID;
    private FallbackUsage fallbackUsage;
    private boolean retried;

    MethodFaultToleranceMetrics(MetricRegistry registry, String canonicalMethodName) {
        this(registry, canonicalMethodName, FallbackUsage.notDefined, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    private MethodFaultToleranceMetrics(MetricRegistry registry, String canonicalMethodName, FallbackUsage fallbackUsage,
            Map<MetricID, Counter> countersByMetricID, Map<MetricID, Histogram> histogramsByMetricID,
            Map<MetricID, Gauge<Long>> gaugesByMetricID) {
        this.registry = registry;
        this.canonicalMethodName = canonicalMethodName;
        this.fallbackUsage = fallbackUsage;
        this.countersByMetricID = countersByMetricID;
        this.histogramsByMetricID = histogramsByMetricID;
        this.gaugesByMetricID = gaugesByMetricID;
    }

    @Override
    public FaultToleranceMetrics bind(boolean fallbackDefined) {
        return new MethodFaultToleranceMetrics(registry, canonicalMethodName,
                fallbackDefined ? FallbackUsage.notApplied : FallbackUsage.notDefined,
                countersByMetricID, histogramsByMetricID, gaugesByMetricID);
    }

    /*
     * General Metrics
     */

    @Override
    public void incrementCounter(String metric, Tag... tags) {
        countersByMetricID.computeIfAbsent(withMethodTag(metric, tags),
                key -> registry.counter(key)).inc();
    }

    @Override
    public void addToHistogram(String metric, long duration, Tag... tags) {
        histogramsByMetricID.computeIfAbsent(withMethodTag(metric, tags),
                key -> registry.histogram(key)).update(duration);
    }

    @Override
    public void linkGauge(String metric, LongSupplier gauge, Tag... tags) {
        gaugesByMetricID.computeIfAbsent(withMethodTag(metric, tags),
                key -> registry.gauge(key, () -> gauge.getAsLong()));
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
