package fish.payara.monitoring.web;

import java.math.BigInteger;

import fish.payara.monitoring.model.SeriesDataset;

public final class SeriesStatistics {

    public final String series;
    public final long[] points;
    public final long observedMax;
    public final long observedMin;
    public final BigInteger observedSum;
    public final int observedValues;
    public final int observedValueChanges;
    public final long observedSince;
    public final int stableCount;
    public final long stableSince;

    public SeriesStatistics(SeriesDataset set) {
        this.series = set.getSeries().toString();
        this.points = set.points();
        this.observedMax = set.getObservedMax();
        this.observedMin = set.getObservedMin();
        this.observedSum = set.getObservedSum();
        this.observedValues = set.getObservedValues();
        this.observedValueChanges = set.getObservedValueChanges();
        this.observedSince = set.firstTime(); // FIXME not quite the same
        this.stableCount = set.getStableCount();
        this.stableSince = set.getStableSince();
    }
}
