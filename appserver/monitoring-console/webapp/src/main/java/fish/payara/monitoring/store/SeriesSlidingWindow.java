package fish.payara.monitoring.store;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Keeps point data for a fixed window size.
 * 
 * The implementation is "effectively immutable". While all primitive fields are immutable the sliding window itself is
 * shared. This does not affect the immutability of the observable window as long as the number of added points since
 * the instance was created is less than the window size. An out-dated window can be detected though using
 * {@link #isOutdated()}. In practice the instances can be treated as if they are fully immutable as long as they are
 * monotonically updated and reading is done from the more recent instances.
 * 
 * When {@link #add(long, long)}ing points the window is first filled to it capacity. Since the buffers are twice the
 * window size they start to slide once the {@link #length()} reaches the {@link #capacity()}. When the sliding window
 * reaches the end of the buffer the most recent halve is copied to the first half and sliding starts from there again.
 * This keeps copying memory only occur every {@link #capacity()} points.
 * 
 * @author Jan Bernitt
 */
public final class SeriesSlidingWindow implements Serializable {

    private final Series series;

    // window
    /**
     * The time expected at {@link #time(int)} called with zero. If this time is different the window is out-dated.
     */
    private final long time0;
    private final long[] times;
    private final long[] values;
    private final int offset;
    private final int length;

    // further statistics 
    private final long observedMax;
    private final long observedMin;
    private final BigInteger observedSum;
    private final int observedValues;
    private final int observedValueChanges;
    private final int unchangedCount;
    private final long unchangedSince;

    public SeriesSlidingWindow(Series series, int size) {
        this(series, new long[size * 2], 0L, new long[size * 2], 0, 0, Long.MIN_VALUE, Long.MAX_VALUE, BigInteger.ZERO, 0, 0, 0, 0L);
    }

    public SeriesSlidingWindow(Series series, long[] times, long time0, long[] values, int offset, int length, long observedMax,
            long observedMin, BigInteger observedSum, int observedValues, int observedValueChanges, int unchangedCount,
            long unchangedSince) {
        super();
        this.series = series;
        this.times = times;
        this.time0 = time0;
        this.values = values;
        this.offset = offset;
        this.length = length;
        this.observedMax = observedMax;
        this.observedMin = observedMin;
        this.observedSum = observedSum;
        this.observedValues = observedValues;
        this.observedValueChanges = observedValueChanges;
        this.unchangedCount = unchangedCount;
        this.unchangedSince = unchangedSince;
    }

    public boolean isOutdated() {
        return time(0) != time0;
    }

    public Series getSeries() {
        return series;
    }

    public long getUnchangedSince() {
        return unchangedSince;
    }

    public int getUnchangedCount() {
        return unchangedCount;
    }

    public int getObservedValues() {
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
    public int getObservedValueChanges() {
        return observedValueChanges;
    }

    public long getObservedMin() {
        return observedMin;
    }

    public long getObservedMax() {
        return observedMax;
    }

    public BigInteger getObservedSum() {
        return observedSum;
    }

    public BigInteger getObservedAvg() {
        return observedSum.divide(BigInteger.valueOf(observedValues));
    }

    public int length() {
        return length;
    }

    public int capacity() {
        return times.length / 2;
    }

    public long value(int index) {
        return values[offset + index];
    }

    public long time(int index) {
        return times[offset + index];
    }

    public long last() {
        return values[offset + length - 1];
    }

    public long start() {
        return length == 0 ? -1 : times[offset];
    }

    public Point[] points() {
        Point[] points = new Point[length];
        for (int i = 0; i < length; i++) {
            points[i] = new Point(time(i), value(i));
        }
        return points;
    }

    public SeriesStatistics statistics() {
        return new SeriesStatistics(series.toString(), points(), observedMax, observedMin, observedSum,
                observedValues, observedValueChanges, unchangedCount, unchangedSince);
    }

    public SeriesSlidingWindow add(long time, long value) {
        boolean changed = length == 0 || value != values[offset + length - 1];
        int newOffset = offset;
        int newLength = length;
        if (length < capacity()) {
            times[length] = time;
            values[length] = value;
            newLength++;
        } else {
            if (offset == length) { // time to roll over
                System.arraycopy(times, offset, times, 0, length);
                System.arraycopy(values, offset, values, 0, length);
                newOffset = 0;
            }
            times[newOffset + length] = time;
            values[newOffset + length] = value;
            newOffset++;
        }
        return new SeriesSlidingWindow(series, times, length == 0 ? time : time0, values, newOffset, newLength, 
                Math.max(value, observedMax), Math.min(value, observedMin), observedSum.add(BigInteger.valueOf(value)), 
                observedValues + 1, 
                observedValueChanges + (changed ? 1 : 0), 
                changed ? 1 : unchangedCount + 1, 
                changed ? time : unchangedSince);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < length; i++) {
            str.append(time(i)).append(':').append(value(i)).append('\n');
        }
        return str.toString();
    }
}
