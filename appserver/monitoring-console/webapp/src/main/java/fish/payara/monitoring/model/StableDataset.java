package fish.payara.monitoring.model;

import java.math.BigInteger;

/**
 * A {@link StableDataset} is a dataset of a {@link Series} that was a {@link PartialDataset} before it became stable
 * for such a long duration that it moved to a {@link StableDataset}.
 * 
 * In contrast to a {@link ConstantDataset} a {@link StableDataset} does have a history of observed values changes and
 * differing sum, minimum and maximum values. A {@link ConstantDataset} on the other hand has only ever observed the
 * very same value which is its minimum, maximum and average value for any number of observed values.
 * 
 * @author Jan Bernitt
 */
public class StableDataset extends ConstantDataset {

    private final int observedValueChanges;
    private final long observedMax;
    private final long observedMin;
    private final BigInteger observedSum;
    private final int stableCount;

    public StableDataset(SeriesDataset predecessor, long time) {
        super(predecessor.getSeries(), predecessor.capacity(), predecessor.getObservedValues() + 1,
                predecessor.firstTime(), time, predecessor.lastValue());
        this.observedValueChanges = predecessor.getObservedValueChanges();
        this.observedMax = predecessor.getObservedMax();
        this.observedMin = predecessor.getObservedMin();
        this.observedSum = predecessor.getObservedSum().add(BigInteger.valueOf(predecessor.lastValue()));
        this.stableCount = predecessor.getStableCount() + 1;
    }

    @Override
    public int getObservedValueChanges() {
        return observedValueChanges;
    }

    @Override
    public long getObservedMax() {
        return observedMax;
    }

    @Override
    public long getObservedMin() {
        return observedMin;
    }

    @Override
    public BigInteger getObservedSum() {
        return observedSum;
    }

    @Override
    public int getStableCount() {
        return stableCount;
    }

    @Override
    public SeriesDataset add(long time, long value) {
        return value == lastValue() ? new StableDataset(this, time) : new PartialDataset(this, time, value);
    }
}
