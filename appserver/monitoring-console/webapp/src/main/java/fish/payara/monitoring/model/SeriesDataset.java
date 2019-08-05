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
    private final int observedValues;

    public SeriesDataset(Series series, int observedValues) {
        this.series = series;
        this.observedValues = observedValues;
    }

    public final Series getSeries() {
        return series;
    }

    public final BigInteger getObservedAvg() {
        return observedValues == 0 ? BigInteger.ZERO : getObservedSum().divide(BigInteger.valueOf(observedValues));
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
     * @return the value of the last {@link #points()}
     */
    public abstract long lastValue();

    /**
     * @return the time value of the first {@link #points()}
     */
    public abstract long firstTime();

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

    @Override
    public final String toString() {
        StringBuilder str = new StringBuilder();
        long[] points = points();
        str.append("[\n");
        for (int i = 0; i < points.length; i+=2) {
            str.append('\t').append(points[i]).append('@').append(points[i+1]).append('\n');
        }
        str.append(']');
        return str.toString();
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
