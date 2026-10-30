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
 *
 * *****************************************************************************
 * Copyright 2010-2013 Coda Hale and Yammer, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fish.payara.microprofile.metrics.impl;

import fish.payara.microprofile.metrics.cdi.MetricUtils;
import jakarta.enterprise.inject.Vetoed;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;

import static fish.payara.microprofile.metrics.impl.MetricRegistryImpl.METRIC_HISTOGRAM_BUCKETS_PROPERTY;

/**
 * A metric which calculates the distribution of a value.
 *
 * @see <a href="http://www.johndcook.com/standard_deviation.html">Accurately
 * computing running variance</a>
 */
@Vetoed
public class HistogramImpl implements Histogram {

    private final ExponentiallyDecayingReservoir reservoir;
    private final LongAdder count;
    private final AtomicLong sum;
    private ConfigurationProperties configurationProperties;
    
    public HistogramImpl(String metricName, 
                         Map<String, Collection<MetricsCustomPercentiles>> percentilesConfigMap,
                         Map<String, Collection<MetricsCustomBuckets>> bucketsConfigMap) {
        this();
        validateMetricsConfiguration(metricName, percentilesConfigMap, bucketsConfigMap);
    }

    /**
     * Creates a new {@link HistogramImpl} using an
     * {@link ExponentiallyDecayingReservoir}.
     */
    public HistogramImpl() {
        this(new ExponentiallyDecayingReservoir());
    }

    /**
     * Creates a new {@link HistogramImpl} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     */
    public HistogramImpl(ExponentiallyDecayingReservoir reservoir) {
        this.reservoir = reservoir;
        this.count = new LongAdder();
        this.sum = new AtomicLong();
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    @Override
    public void update(int value) {
        update((long) value);
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    @Override
    public void update(long value) {
        count.increment();
        sum.getAndAdd(value);
        reservoir.update(value);
    }

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public long getSum() {
        return sum.get();
    }

    @Override
    public Snapshot getSnapshot() {
        return reservoir.getSnapshot();
    }

    @Override
    public String toString() {
        return "Histogram[" + getCount() + "]";
    }

    void validateMetricsConfiguration(String metricName, Map<String, Collection<MetricsCustomPercentiles>> percentilesConfigMap,
                                      Map<String, Collection<MetricsCustomBuckets>> bucketsConfigMap) {
        Collection<MetricsCustomPercentiles> computedPercentiles = percentilesConfigMap
                .computeIfAbsent(metricName, MetricsConfigParserUtil::processPercentileMap);
        Collection<MetricsCustomBuckets> computedBuckets = bucketsConfigMap
                .computeIfAbsent(metricName, this::processHistogramBucketMap);
        MetricsCustomPercentiles resultPercentile = null;
        MetricsCustomBuckets resultBuckets = null;
        configurationProperties = new ConfigurationProperties();
        if (computedPercentiles != null && !computedPercentiles.isEmpty()) {
            resultPercentile = MetricsCustomPercentiles.matches(computedPercentiles, metricName);
        }

        if (resultPercentile != null && resultPercentile.getPercentiles() != null
                && resultPercentile.getPercentiles().length > 0) {
            configurationProperties.setPercentilesFromConfig(resultPercentile.getPercentiles());
        } else if (resultPercentile != null && resultPercentile.getPercentiles() == null
                && resultPercentile.isDisabled()) {
            //skip this case
        } else {
            Double[] percentiles = {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};
            configurationProperties.setPercentilesFromConfig(percentiles);
        }
        if (computedBuckets != null && computedBuckets.size() != 0) {
            resultBuckets = MetricsCustomBuckets.matches(computedBuckets, metricName);
        }
        if (resultBuckets != null && resultBuckets.getBuckets() != null && resultBuckets.getBuckets().length > 0) {
            configurationProperties.setBucketValuesFromConfig(resultBuckets.getBuckets());
        }

        this.reservoir.setConfigAdapter(configurationProperties);
    }

    private Collection<MetricsCustomBuckets> processHistogramBucketMap(String appName) {
        Config config = MetricUtils.getConfigProvider();
        if (config != null) {
            Optional<String> customBuckets = config.getOptionalValue(METRIC_HISTOGRAM_BUCKETS_PROPERTY, String.class);
            return (customBuckets.isPresent()) ? MetricsConfigParserUtil.parseHistogramBuckets(customBuckets.get()) : null;
        } else {
            return null;
        }
    }
}
