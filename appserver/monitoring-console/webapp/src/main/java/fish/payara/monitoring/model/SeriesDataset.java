package fish.payara.monitoring.model;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * A {@link SeriesDataset} contains data observed so far for a particular {@link Series}.
 * 
 * @author Jan Bernitt
 * 
 * @see ConstantDataset
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
        return getObservedSum().divide(BigInteger.valueOf(observedValues));
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

    public abstract long[] points();

    public abstract SeriesDataset add(long time, long value);

    public abstract long getObservedMin();

    public abstract long getObservedMax();

    public abstract BigInteger getObservedSum();

    public abstract long getStableSince();

    public abstract int getStableCount();

    public abstract boolean isOutdated();

    /**
     * @return the number of actual points in the dataset. This is not the number of observed points but the number of
     *         points still known.
     */
    public abstract int size();

    public abstract long lastValue();

    public abstract long firstTime();

    public abstract int capacity();

    public abstract int estimatedBytesMemory();
}
