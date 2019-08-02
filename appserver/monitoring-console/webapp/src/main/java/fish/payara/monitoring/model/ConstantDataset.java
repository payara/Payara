package fish.payara.monitoring.model;

import java.math.BigInteger;

/**
 * A special {@link SeriesDataset} for {@link Series} for only same value was observed so far.
 * 
 * The main advantage over a {@link PartialDataset} is the low memory footprint and overall smaller object size for
 * values that do not change anyway. Should they change the {@link #add(long, long)} method returns a
 * {@link PartialDataset}.
 * 
 * A minor second advantage is that the observed span can have any length and still be represented with just two points.
 * So in contrast to a {@link PartialDataset} which only has information for a fixed sliding time-frame the constant
 * nature allows the {@link ConstantDataset} to span any time-frame giving the user a more information while using less
 * resources.
 * 
 * Last but not least the {@link ConstantDataset} does not risk to become {@link PartialDataset#isOutdated()}.
 * 
 * @author Jan Bernitt
 */
public class ConstantDataset extends SeriesDataset {

    /**
     * The time expected at {@link #time(int)} called with zero. If this time is different the window is out-dated.
     */
    private final long stableSince;
    /**
     * The most recent time the {@link #value} was observed
     */
    private final long time;
    /**
     * The constant value (only observed value)
     */
    private final long value;

    private final int capacity;

    public ConstantDataset(ConstantDataset predecessor, long time) {
        super(predecessor.getSeries(), predecessor.getObservedValues() + 1);
        this.capacity = predecessor.capacity;
        this.stableSince = predecessor.stableSince;
        this.time = time;
        this.value = predecessor.value;
    }

    public ConstantDataset(Series series, int capacity, int observedValues, long stableSince, long time, long value) {
        super(series, observedValues);
        this.capacity = capacity;
        this.stableSince = stableSince;
        this.time = time;
        this.value = value;
    }

    @Override
    public long[] points() {
        return size() == 1
                ? new long[] { stableSince, value }
                : new long[] { stableSince, value, time, value };
    }

    @Override
    public SeriesDataset add(long time, long value) {
        return value == lastValue() ? new ConstantDataset(this, time) : new PartialDataset(this, time, value);
    }

    @Override
    public long getObservedMin() {
        return value;
    }

    @Override
    public long getObservedMax() {
        return value;
    }

    @Override
    public BigInteger getObservedSum() {
        return BigInteger.valueOf(getObservedValues()).multiply(BigInteger.valueOf(value));
    }

    @Override
    public int getObservedValueChanges() {
        return 1;
    }

    @Override
    public long getStableSince() {
        return stableSince;
    }

    @Override
    public int getStableCount() {
        return getObservedValues();
    }

    @Override
    public final boolean isOutdated() {
        return false;
    }

    @Override
    public final int size() {
        return stableSince == time ? 1 : 2;
    }

    @Override
    public final long firstTime() {
        return stableSince;
    }

    @Override
    public final long lastValue() {
        return value;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int estimatedBytesMemory() {
        return 40;
    }
}
