/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.monitoring.model;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * A {@link SeriesDataset} contains data observed so far for a particular {@link Series}.
 * 
 * @author Jan Bernitt
 * 
 * @see EmptyDataset
 * @see ConstantDataset
 * @see StableDataset
 * @see PartialDataset
 */
public abstract class SeriesDataset implements Serializable {

    private final Series series;
    private final String instance;
    private final long observedSince;
    private final int observedValues;

    public SeriesDataset(SeriesDataset predecessor) {
        this.series = predecessor.series;
        this.instance = predecessor.instance;
        this.observedSince = predecessor.observedSince;
        this.observedValues = predecessor.observedValues + 1;
    }

    public SeriesDataset(Series series, String instance, long observedSince, int observedValues) {
        this.series = series;
        this.instance = instance;
        this.observedSince = observedSince;
        this.observedValues = observedValues;
    }

    public final Series getSeries() {
        return series;
    }

    public String getInstance() {
        return instance;
    }

    public final BigInteger getObservedAvg() {
        return observedValues == 0 ? BigInteger.ZERO : getObservedSum().divide(BigInteger.valueOf(observedValues));
    }

    /**
     * @return the time of the first ever value observed by this dataset
     */
    public long getObservedSince() {
        return observedSince;
    }

    /**
     * The number of times a value was observed since start of collection
     */
    public final int getObservedValues() {
        return observedValues;
    }

    /**
     * Note that minimum is 1 (changing from unknown to know value). Zero means no values have been observed yet.
     * 
     * Note also that change count of 1 does not imply
     * that the value never did change just that such a change was never observed.
     * 
     * @return Number of times the value as altered since it has been monitored.
     */
    public abstract int getObservedValueChanges();

    /**
     * Example: 
     * <pre>
     * [t0, v0, t1, v1, t2, v2]
     * </pre>
     * 
     * @return this dataset as flat array with alternating time and value data.
     */
    public abstract long[] points();

    public abstract SeriesDataset add(long time, long value);

    /**
     * @return The smallest value observed so far. If no value was observed {@link Long#MAX_VALUE}.
     */
    public abstract long getObservedMin();

    /**
     * @return The largest value observed so far. If no value was observed {@link Long#MIN_VALUE}.
     */
    public abstract long getObservedMax();

    /**
     * @return sum of all observed values.
     */
    public abstract BigInteger getObservedSum();

    /**
     * @return the time of the first value that is still same as {@link #lastValue()}.
     */
    public abstract long getStableSince();

    /**
     * @return the number of recent observed points that all had the same value which is same as {@link #lastValue()}
     */
    public abstract int getStableCount();

    /**
     * @return true in case data sharing with updated sets has caused this set to be outdated. This practically does not
     *         happen if the sets are used as intended and usually suggests misuse of a programming mistake.
     */
    public abstract boolean isOutdated();

    /**
     * @return the number of actual points in the dataset. This is not the number of observed points but the number of
     *         points still known.
     */
    public abstract int size();

    /**
     * @return the value of the last of the {@link #points()}
     */
    public abstract long lastValue();

    /**
     * @return the time value of the first of the {@link #points()}
     */
    public abstract long firstTime();

    /**
     * @return the time value of the last of the {@link #points()}
     */
    public abstract long lastTime();
    
    /**
     * @return the maximum number of points in a dataset before adding a new point does remove the oldest point
     */
    public abstract int capacity();

    /**
     * @return the estimated memory in bytes used by this dataset. Since the object layout in memory is a JVM internal
     *         this is only a rough estimation based on the fields. References are assumed to use 8 bytes. Padding is
     *         not included.
     */
    public abstract int estimatedBytesMemory();

    public boolean isStable() {
        return true;
    }

    public final boolean isStableZero() {
        return isStable() && lastValue() == 0L;
    }

    @Override
    public final String toString() {
        StringBuilder str = new StringBuilder();
        long[] points = points();
        str.append(getSeries()).append('@').append(getInstance());
        str.append("[\n");
        for (int i = 0; i < points.length; i+=2) {
            str.append('\t').append(points[i]).append('@').append(points[i+1]).append('\n');
        }
        str.append(']');
        return str.toString();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SeriesDataset)) {
            return false;
        }
        SeriesDataset other = (SeriesDataset) obj;
        return instance.equals(other.instance) && series.equals(other.series);
    }

    @Override
    public final int hashCode() {
        return instance.hashCode() ^ series.hashCode();
    }

    /**
     * Converts an array of {@link SeriesDataset#points()} to one reflecting the change per second. For each pair of
     * points this is the delta between the earlier and later point of the pair. Since this is a delta the result array
     * contains one less point.
     * 
     * @param points point data as returned by {@link SeriesDataset#points()}
     * @return Points representing the delta or per-second change of the provided input data. The delta is associated
     *         with the end point time of each pair.
     */
    public static long[] perSecond(long[] points) {
        long[] perSec = new long[points.length - 2];
        for (int i = 0; i < perSec.length; i+=2) {
            perSec[i] = points[i + 2]; // time for diff is second points time
            long deltaTime = points[i + 2] - points[i];
            long deltaValue = points[i + 3] - points[i + 1];
            if (deltaTime == 1000L) { // is already 1 sec between points
                perSec[i + 1] = deltaValue;
            } else if (deltaTime % 1000L == 0L) { // exact number of secs in between points
                perSec[i + 1] = deltaValue / (deltaTime / 1000L);
            } else {
                perSec[i + 1] = Math.round(((double)deltaValue / deltaTime) * 1000L);
            }
        }
        return perSec;
    }
}
