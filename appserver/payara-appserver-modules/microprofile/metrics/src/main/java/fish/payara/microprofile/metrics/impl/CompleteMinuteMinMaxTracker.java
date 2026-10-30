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
 */
package fish.payara.microprofile.metrics.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.metrics.Metric;

/**
 * A metric that tracks minimum and maximum values as updated for the most recent complete minute.
 *
 * @author Jan Bernitt
 */
abstract class CompleteMinuteMinMaxTracker implements Metric {

    protected final Clock clock;

    /**
     * Minimum and maximum of current minute
     */
    private final AtomicReference<MinMax> currentMinute;

    /**
     * Minimum and maximum during previously completed minute
     */
    private volatile MinMax completeMinute;

    public CompleteMinuteMinMaxTracker(Clock clock) {
        this.clock = clock;
        // must run with clock initialised:
        this.currentMinute = new AtomicReference<>();
        this.completeMinute = null;
    }

    Long getMaxValue() {
        currentMinMax(null);
        return completeMinute == null ? null : completeMinute.max.get();
    }

    Long getMinValue() {
        currentMinMax(null);
        return completeMinute == null ? null : completeMinute.min.get();
    }

    void updateMaxValue(long currentValue) {
        currentMinMax(currentValue).updateMax(currentValue);
    }

    void updateMinValue(long currentValue) {
        currentMinMax(currentValue).updateMin(currentValue);
    }

    void updateValue(long currentValue) {
        MinMax stats = currentMinMax(currentValue);
        stats.updateMin(currentValue);
        stats.updateMax(currentValue);
    }

    private long currentEpochMinute() {
        return Instant.ofEpochMilli(clock.getTime()).truncatedTo(ChronoUnit.MINUTES).getEpochSecond() / 60;
    }

    private MinMax currentMinMax(Long currentValue) {
        long currentEpochMinute = currentEpochMinute();
        MinMax possiblyOutdated = currentMinute.getAndUpdate(
                value -> value == null || value.markIfOld(currentEpochMinute)
                    ? currentValue == null ? null : new MinMax(currentValue, currentEpochMinute)
                    : value);
        if (possiblyOutdated != null && possiblyOutdated.finished.get()) {
            // we got previous MinMax instance, that has set finished=true just before it was replaced
            completeMinute = possiblyOutdated;
            // if value was not updated for longer than one minute, this is still correct answer,
            // as the gauge doesn't reset by itself.
            return currentMinute.get();
        } else if (completeMinute != null && currentEpochMinute-1L > completeMinute.epochMinute) {
            completeMinute = null;
        }
        return possiblyOutdated != null ? possiblyOutdated : currentMinute.get();
    }

    /**
     * Stats captured by the gauge. Note that even if it is stored in
     * AtomicReference, the class itself may be accessed concurrently.
     */
    private static final class MinMax {

        final AtomicLong min;
        final AtomicLong max;
        final long epochMinute;
        final AtomicBoolean finished = new AtomicBoolean(false);

        MinMax(long initialValue, long currentEpochMinute) {
            this.min = new AtomicLong(initialValue);
            this.max = new AtomicLong(initialValue);
            this.epochMinute = currentEpochMinute;
        }

        /**
         * Ensures each instance will only return true a single time even when called concurrently.
         *
         * @return true, if this {@link MinMax} was identified and marked as old, else false
         */
        boolean markIfOld(long currentEpochMinute) {
            return currentEpochMinute > epochMinute && finished.compareAndSet(false, true);
        }

        void updateMin(long value) {
            min.accumulateAndGet(value, Math::min);
        }

        void updateMax(long value) {
            max.accumulateAndGet(value, Math::max);
        }
    }
}
