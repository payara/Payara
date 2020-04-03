/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 */
package fish.payara.microprofile.metrics.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.metrics.ConcurrentGauge;

/**
 * Implementation of ConcurrentGauge from Microprofile Metrics
 *
 * @see ConcurrentGauge
 * @since 5.193
 */
@Vetoed
public class ConcurrentGaugeImpl implements ConcurrentGauge {

    /**
     * The number of threads currently executing the annotated method.
     */
    private final AtomicInteger threads = new AtomicInteger();

    /**
     * Minimum and maximum of current minute
     */
    private AtomicReference<MinMax> openStats;

    /**
     * Minimum and maximum during previously completed minute
     */
    private volatile MinMax closedStats;

    private final Clock clock;

    public ConcurrentGaugeImpl() {
        this(Clock.DEFAULT);
    }

    public ConcurrentGaugeImpl(Clock clock) {
        this.clock = clock;
        // must run with clock initialised:
        this.openStats = new AtomicReference<>(new MinMax(0, getCurrentMinute()));
        this.closedStats = new MinMax(0, getCurrentMinute());
    }

    /**
     * Increment the counter by one.
     */
    @Override
    public void inc() {
        threads.incrementAndGet();
        currentStats().updateMax(threads.longValue());
    }

    @Override
    public void dec() {
        threads.decrementAndGet();
        currentStats().updateMin(threads.longValue());
    }

    /**
     * Returns the counter's current value.
     *
     * @return the counter's current value
     */
    @Override
    public long getCount() {
        return threads.get();
    }

    @Override
    public long getMax() {
        currentStats();
        return closedStats.max.get();
    }

    @Override
    public long getMin() {
        currentStats();
        return closedStats.min.get();
    }

    private Instant getCurrentMinute() {
        return Instant.ofEpochMilli(clock.getTime()).truncatedTo(ChronoUnit.MINUTES);
    }

    private MinMax currentStats() {
        Instant now = getCurrentMinute();
        MinMax possiblyOutdated = openStats.getAndUpdate(
                value -> value.markIfOld(now) ? new MinMax(threads.longValue(), now) : value);
        if (possiblyOutdated.finished.get()) {
            // we got previous MinMax instance, that has set finished=true just before it was replaced
            closedStats = possiblyOutdated;
            // if value was not updated for longer than one minute, this is still correct answer,
            // as the gauge doesn't reset by itself.
            return openStats.get();
        }
        return possiblyOutdated;
    }

    /**
     * Stats captured by the gauge. Note that even if it is stored in
     * AtomicReference, the class itself may be accessed concurrently.
     */
    private static class MinMax {

        final AtomicLong min;
        final AtomicLong max;
        final Instant minute;
        final AtomicBoolean finished = new AtomicBoolean(false);

        MinMax(long initialValue, Instant minute) {
            this.min = new AtomicLong(initialValue);
            this.max = new AtomicLong(initialValue);
            this.minute = minute;
        }

        /**
         * Ensures each instance will only return true a single time even when called concurrently.
         *
         * @return true, if this {@link MinMax} was identified and marked as old, else false
         */
        boolean markIfOld(Instant now) {
            return !now.equals(minute) && finished.compareAndSet(false, true);
        }

        void updateMin(long value) {
            min.accumulateAndGet(value, Math::min);
        }

        void updateMax(long value) {
            max.accumulateAndGet(value, Math::max);
        }
    }
}
