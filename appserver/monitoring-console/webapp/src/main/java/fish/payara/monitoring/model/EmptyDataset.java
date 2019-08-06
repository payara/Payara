package fish.payara.monitoring.model;

import java.math.BigInteger;

/**
 * The {@link EmptyDataset} is the starting point for any of the other {@link SeriesDataset} implementations.
 *
 * When first point is added to the {@link EmptyDataset} it becomes a {@link ConstantDataset}.
 * 
 * {@link EmptyDataset} are initialised with a {@link #capacity} so it can be passed on as the set eventually evolves
 * into a {@link PartialDataset} which has a {@link #capacity()} limit.
 *
 * @author Jan Bernitt
 */
public final class EmptyDataset extends SeriesDataset {

    private final int capacity;

    public EmptyDataset(Series series, int capacity) {
        super(series, -1L, 0);
        this.capacity = capacity;
    }

    @Override
    public int getObservedValueChanges() {
        return 0;
    }

    @Override
    public long[] points() {
        return new long[0];
    }

    @Override
    public SeriesDataset add(long time, long value) {
        return new ConstantDataset(getSeries(), capacity, time, value);
    }

    @Override
    public long getObservedMin() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getObservedMax() {
        return Long.MIN_VALUE;
    }

    @Override
    public BigInteger getObservedSum() {
        return BigInteger.ZERO;
    }

    @Override
    public long getStableSince() {
        return -1;
    }

    @Override
    public int getStableCount() {
        return 0;
    }

    @Override
    public boolean isOutdated() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public long lastValue() {
        return 0;
    }

    @Override
    public long firstTime() {
        return -1;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int estimatedBytesMemory() {
        return 24;
    }
}
