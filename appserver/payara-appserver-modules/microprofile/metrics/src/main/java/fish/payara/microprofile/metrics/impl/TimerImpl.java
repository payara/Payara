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

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import jakarta.enterprise.inject.Vetoed;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

import static fish.payara.microprofile.metrics.impl.MetricRegistryImpl.METRIC_HISTOGRAM_BUCKETS_PROPERTY;
import static fish.payara.microprofile.metrics.impl.MetricRegistryImpl.METRIC_TIMER_BUCKETS_PROPERTY;

/**
 * A timer metric which aggregates timing durations and provides duration
 * statistics.
 *
 * The timer measures duration in nanoseconds.
 *
 */
@Vetoed
public class TimerImpl implements Timer {

    private final Histogram histogram;
    private final Clock clock;

    private ExponentiallyDecayingReservoir reservoir;
    
    private TimerAdapter timerAdapter;
    
    public TimerImpl(String metricName, Map<String, Collection<MetricsCustomPercentile>> percentilesConfigMap, 
                     Map<String, Collection<TimerMetricsBucket>> timerBucketsConfigMap, Clock clock) {
        this(clock);
        validateMetricsConfiguration(metricName, percentilesConfigMap, timerBucketsConfigMap);
    }

    /**
     * Creates a new {@link TimerImpl} using an
     * {@link ExponentiallyDecayingReservoir} and the default {@link Clock}.
     */
    public TimerImpl() {
        this(new ExponentiallyDecayingReservoir());
    }

    /**
     * Creates a new {@link TimerImpl} using an
     * {@link ExponentiallyDecayingReservoir} and the provided {@link Clock}.
     *
     * @param clock the {@link Clock} implementation the created timer should use
     */
    public TimerImpl(Clock clock) {
        this(new ExponentiallyDecayingReservoir(), clock);
    }

    /**
     * Creates a new {@link TimerImpl} that uses the given {@link Reservoir}.
     *
     * @param reservoir the {@link Reservoir} implementation the timer should
     * use
     */
    public TimerImpl(ExponentiallyDecayingReservoir reservoir) {
        this(reservoir, Clock.defaultClock());
    }

    /**
     * Creates a new {@link TimerImpl} that uses the given {@link Reservoir} and
     * {@link Clock}.
     *
     * @param reservoir the {@link Reservoir} implementation the timer should
     * use
     * @param clock the {@link Clock} implementation the timer should use
     */
    public TimerImpl(ExponentiallyDecayingReservoir reservoir, Clock clock) {
        this.clock = clock;
        this.reservoir = reservoir;
        this.histogram = new HistogramImpl(reservoir);
    }

    /**
     * Adds a recorded duration.
     *
     * @param duration the length of the duration
     * @param unit the scale unit of {@code duration}
     */
    public void update(long duration, TimeUnit unit) {
        update(unit.toNanos(duration));
    }

    @Override
    public void update(Duration duration) {
        update(duration.toNanos());
    }

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Callable} whose {@link Callable#call()} method
     * implements a process whose duration should be timed
     * @param <T> the type of the value returned by {@code event}
     * @return the value returned by {@code event}
     * @throws Exception if {@code event} throws an {@link Exception}
     */
    @Override
    public <T> T time(Callable<T> event) throws Exception {
        final long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(clock.getTick() - startTime);
        }
    }

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Runnable} whose {@link Runnable#run()} method
     * implements a process whose duration should be timed
     */
    @Override
    public void time(Runnable event) {
        final long startTime = clock.getTick();
        try {
            event.run();
        } finally {
            update(clock.getTick() - startTime);
        }
    }

    /**
     * Returns a new {@link Context}.
     *
     * @return a new {@link Context}
     * @see Context
     */
    @Override
    public Timer.Context time() {
        return new Context(this, clock);
    }

    @Override
    public Duration getElapsedTime() {
        return Duration.ofNanos(histogram.getSum());
    }

    @Override
    public long getCount() {
        return histogram.getCount();
    }


    @Override
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }

    private void update(long duration) {
        if (duration >= 0) {
            histogram.update(duration);
        }
    }

    @Override
    public String toString() {
        return "Timer[" + getCount() + "]";
    }

    void validateMetricsConfiguration(String metricName, Map<String, Collection<MetricsCustomPercentile>> percentilesConfigMap,
                                      Map<String, Collection<TimerMetricsBucket>> timerBucketsConfigMap) {
        Collection<MetricsCustomPercentile> computedPercentiles = percentilesConfigMap
                .computeIfAbsent(metricName, MetricsConfigParserUtil::processPercentileMap);
        Collection<TimerMetricsBucket> computedTimersBuckets = timerBucketsConfigMap
                .computeIfAbsent(metricName, this::processTimerBucketMap);
        timerAdapter = new TimerAdapter();
        MetricsCustomPercentile resultPercentile = MetricsCustomPercentile.matches(computedPercentiles, metricName);
        if (resultPercentile != null && resultPercentile.getPercentiles() != null 
                && resultPercentile.getPercentiles().length > 0) {
            timerAdapter.setPercentilesFromConfig(resultPercentile.getPercentiles());
        } else if (resultPercentile != null && resultPercentile.getPercentiles() == null 
                && resultPercentile.isDisabled()) {

        } else {
            Double[] percentiles = {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};
            timerAdapter.setPercentilesFromConfig(percentiles);
        }
        
        TimerMetricsBucket resultBucket = TimerMetricsBucket.matches(computedTimersBuckets, metricName);
        if (resultBucket != null && resultBucket.getBuckets() != null && resultBucket.getBuckets().length > 0) {
            timerAdapter.setBucketValuesFromConfig(resultBucket.getBuckets());
        }
        
        this.reservoir.setConfigAdapter(timerAdapter);
    }

    private synchronized Collection<TimerMetricsBucket> processTimerBucketMap(String appName) {
        Optional<String> customBuckets = ConfigProvider.getConfig().getOptionalValue(METRIC_TIMER_BUCKETS_PROPERTY, String.class);
        return (customBuckets.isPresent()) ? MetricsConfigParserUtil.parseTimerBuckets(customBuckets.get()) : null;
    }
    

    /**
     * A timing context.
     *
     * @see TimerImpl#time()
     */
    private static class Context implements Timer.Context {

        private final Timer timer;
        private final Clock clock;
        private final long startTime;

        Context(Timer timer, Clock clock) {
            this.timer = timer;
            this.clock = clock;
            this.startTime = clock.getTick();
        }

        /**
         * Updates the timer with the difference between current and start time.
         * Call to this method will not reset the start time. Multiple calls
         * result in multiple updates.
         *
         * @return the elapsed time in nanoseconds
         */
        @Override
        public long stop() {
            final long elapsed = clock.getTick() - startTime;
            timer.update(Duration.ofNanos(elapsed));
            return elapsed;
        }

        /**
         * Equivalent to calling {@link #stop()}.
         */
        @Override
        public void close() {
            stop();
        }
    }
}
