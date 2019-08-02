package fish.payara.monitoring.model;

import java.math.BigInteger;

public final class EmptyDataset extends SeriesDataset {

    private final int capacity;

    public EmptyDataset(Series series, int capacity) {
        super(series, 0);
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
        return new ConstantDataset(getSeries(), capacity, 1, time, time, value);
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
        return 16;
    }
}
