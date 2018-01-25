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

import fish.payara.microprofile.metrics.impl.WeightedSnapshot.WeightedSample;
import static java.lang.Math.exp;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.microprofile.metrics.Snapshot;

/**
 * An exponentially-decaying random reservoir of {@code long}s. Uses Cormode et
 * al's forward-decaying priority reservoir sampling method to produce a
 * statistically representative sampling reservoir, exponentially biased towards
 * newer entries.
 *
 * @see <a href="http://dimacs.rutgers.edu/~graham/pubs/papers/fwddecay.pdf">
 * Cormode et al. Forward Decay: A Practical Time Decay Model for Streaming
 * Systems. ICDE '09: Proceedings of the 2009 IEEE International Conference on
 * Data Engineering (2009)</a>
 */
public class ExponentiallyDecayingReservoir implements Reservoir {

    private static final int DEFAULT_SIZE = 1028;
    private static final double DEFAULT_ALPHA = 0.015;
    private static final long RESCALE_THRESHOLD = TimeUnit.HOURS.toNanos(1);

    private final ConcurrentSkipListMap<Double, WeightedSample> values;
    private final ReentrantReadWriteLock lock;
    private final double alpha;
    private final int size;
    private final AtomicLong count;
    private volatile long startTime;
    private final AtomicLong nextScaleTime;
    private final Clock clock;

    /**
     * Creates a new {@link ExponentiallyDecayingReservoir} of 1028 elements,
     * which offers a 99.9% confidence level with a 5% margin of error assuming
     * a normal distribution, and an alpha factor of 0.015, which heavily biases
     * the reservoir to the past 5 minutes of measurements.
     */
    public ExponentiallyDecayingReservoir() {
        this(DEFAULT_SIZE, DEFAULT_ALPHA);
    }

    /**
     * Creates a new {@link ExponentiallyDecayingReservoir}.
     *
     * @param size the number of samples to keep in the sampling reservoir
     * @param alpha the exponential decay factor; the higher this is, the more
     * biased the reservoir will be towards newer values
     */
    public ExponentiallyDecayingReservoir(int size, double alpha) {
        this(size, alpha, Clock.defaultClock());
    }

    /**
     * Creates a new {@link ExponentiallyDecayingReservoir}.
     *
     * @param size the number of samples to keep in the sampling reservoir
     * @param alpha the exponential decay factor; the higher this is, the more
     * biased the reservoir will be towards newer values
     * @param clock the clock used to timestamp samples and track rescaling
     */
    public ExponentiallyDecayingReservoir(int size, double alpha, Clock clock) {
        this.values = new ConcurrentSkipListMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.alpha = alpha;
        this.size = size;
        this.clock = clock;
        this.count = new AtomicLong(0);
        this.startTime = currentTimeInSeconds();
        this.nextScaleTime = new AtomicLong(clock.getTick() + RESCALE_THRESHOLD);
    }

    @Override
    public int size() {
        return (int) min(size, count.get());
    }

    @Override
    public void update(long value) {
        update(value, currentTimeInSeconds());
    }

    /**
     * Adds an old value with a fixed timestamp to the reservoir.
     *
     * @param value the value to be added
     * @param timestamp the epoch timestamp of {@code value} in seconds
     */
    public void update(long value, long timestamp) {
        rescaleIfNeeded();
        lockForRegularUsage();
        try {
            final double itemWeight = weight(timestamp - startTime);
            final WeightedSample sample = new WeightedSample(value, itemWeight);
            final double priority = itemWeight / ThreadLocalRandom.current().nextDouble();

            final long newCount = count.incrementAndGet();
            if (newCount <= size) {
                values.put(priority, sample);
            } else {
                Double first = values.firstKey();
                if (first < priority && values.putIfAbsent(priority, sample) == null) {
                    // ensure we always remove an item
                    while (values.remove(first) == null) {
                        first = values.firstKey();
                    }
                }
            }
        } finally {
            unlockForRegularUsage();
        }
    }

    private void rescaleIfNeeded() {
        final long now = clock.getTick();
        final long next = nextScaleTime.get();
        if (now >= next) {
            rescale(now, next);
        }
    }

    @Override
    public Snapshot getSnapshot() {
        rescaleIfNeeded();
        lockForRegularUsage();
        try {
            return new WeightedSnapshot(values.values());
        } finally {
            unlockForRegularUsage();
        }
    }

    private long currentTimeInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(clock.getTime());
    }

    private double weight(long t) {
        return exp(alpha * t);
    }

    /* "A common feature of the above techniquesâ€”indeed, the key technique that
     * allows us to track the decayed weights efficientlyâ€”is that they maintain
     * counts and other quantities based on g(ti âˆ’ L), and only scale by g(t âˆ’ L)
     * at query time. But while g(ti âˆ’L)/g(tâˆ’L) is guaranteed to lie between zero
     * and one, the intermediate values of g(ti âˆ’ L) could become very large. For
     * polynomial functions, these values should not grow too large, and should be
     * effectively represented in practice by floating point values without loss of
     * precision. For exponential functions, these values could grow quite large as
     * new values of (ti âˆ’ L) become large, and potentially exceed the capacity of
     * common floating point types. However, since the values stored by the
     * algorithms are linear combinations of g values (scaled sums), they can be
     * rescaled relative to a new landmark. That is, by the analysis of exponential
     * decay in Section III-A, the choice of L does not affect the final result. We
     * can therefore multiply each value based on L by a factor of exp(âˆ’Î±(Lâ€² âˆ’ L)),
     * and obtain the correct value as if we had instead computed relative to a new
     * landmark Lâ€² (and then use this new Lâ€² at query time). This can be done with
     * a linear pass over whatever data structure is being used."
     */
    private void rescale(long now, long next) {
        lockForRescale();
        try {
            if (nextScaleTime.compareAndSet(next, now + RESCALE_THRESHOLD)) {
                final long oldStartTime = startTime;
                this.startTime = currentTimeInSeconds();
                final double scalingFactor = exp(-alpha * (startTime - oldStartTime));
                if (Double.compare(scalingFactor, 0) == 0) {
                    values.clear();
                } else {
                    final ArrayList<Double> keys = new ArrayList<>(values.keySet());
                    for (Double key : keys) {
                        final WeightedSample sample = values.remove(key);
                        final WeightedSample newSample = new WeightedSample(sample.value, sample.weight * scalingFactor);
                        values.put(key * scalingFactor, newSample);
                    }
                }

                // make sure the counter is in sync with the number of stored samples.
                count.set(values.size());
            }
        } finally {
            unlockForRescale();
        }
    }

    private void unlockForRescale() {
        lock.writeLock().unlock();
    }

    private void lockForRescale() {
        lock.writeLock().lock();
    }

    private void lockForRegularUsage() {
        lock.readLock().lock();
    }

    private void unlockForRegularUsage() {
        lock.readLock().unlock();
    }
}
