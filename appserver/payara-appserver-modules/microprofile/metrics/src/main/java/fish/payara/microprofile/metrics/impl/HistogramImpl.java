/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import jakarta.enterprise.inject.Vetoed;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;

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
    private String metricName;

    private HistogramAdapter histogramAdapter;
    
    private Map<String, Collection<MetricsCustomPercentile>> percentilesConfigMap;
    
    private Map<String, Collection<MetricsCustomBucket>> bucketsConfigMap;
    
    public static final String METRIC_PERCENTILES_PROPERTY = "mp.metrics.distribution.percentiles";
    
    public static final String METRIC_HISTOGRAM_BUCKETS_PROPERTY = "mp.metrics.distribution.histogram.buckets";
    
    
    public HistogramImpl(String metricName, 
                         Map<String, Collection<MetricsCustomPercentile>> percentilesConfigMap,
                         Map<String, Collection<MetricsCustomBucket>> bucketsConfigMap) {
        this();
        this.metricName = metricName;
        this.percentilesConfigMap = percentilesConfigMap;
        this.bucketsConfigMap = bucketsConfigMap;
        validateMetricsConfiguration();
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
    
    void validateMetricsConfiguration() {
        Collection<MetricsCustomPercentile> computedPercentiles = percentilesConfigMap.computeIfAbsent(this.metricName, this::processPercentileMap);
        Collection<MetricsCustomBucket> computedBuckets = bucketsConfigMap.computeIfAbsent(this.metricName, this::processBucketMap);

        histogramAdapter = new HistogramAdapter();
        
        if(computedPercentiles != null && computedPercentiles.size() != 0) {
            MetricsCustomPercentile result = MetricsCustomPercentile.matches(computedPercentiles, this.metricName);
            if(result != null && result.getPercentiles() != null && result.getPercentiles().length > 0) {
                histogramAdapter.setPercentilesFromConfig(result.getPercentiles());
            } 
        } 
        
        if(computedBuckets != null && computedBuckets.size() != 0) {
            MetricsCustomBucket result = MetricsCustomBucket.matches(computedBuckets, this.metricName);
            if(result!= null && result.getBuckets() != null && result.getBuckets().length > 0) {
                histogramAdapter.setBucketValuesFromConfig(result.getBuckets());
            } 
        } 

        this.reservoir.setHistogramAdapter(histogramAdapter);
    }
    
    private synchronized Collection<MetricsCustomPercentile> processPercentileMap(String appName) {
        Optional<String> customPercentiles = ConfigProvider.getConfig().getOptionalValue(METRIC_PERCENTILES_PROPERTY, String.class);
        return (customPercentiles.isPresent()) ? MetricsConfigParserUtil.parsePercentile(customPercentiles.get()) : null;
    }
    
    private synchronized Collection<MetricsCustomBucket> processBucketMap(String appName) {
        Optional<String> customBuckets = ConfigProvider.getConfig().getOptionalValue(METRIC_HISTOGRAM_BUCKETS_PROPERTY, String.class);
        return (customBuckets.isPresent()) ? MetricsConfigParserUtil.parseHistogramBuckets(customBuckets.get()) : null;
    }
}
