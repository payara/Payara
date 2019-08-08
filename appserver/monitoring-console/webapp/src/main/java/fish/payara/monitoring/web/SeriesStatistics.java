package fish.payara.monitoring.web;

import java.math.BigInteger;
import java.util.Collection;

import fish.payara.monitoring.model.SeriesDataset;

public final class SeriesStatistics {

    public static SeriesStatistics[] from(Collection<SeriesDataset> sets) {
        SeriesStatistics[] stats = new SeriesStatistics[sets.size()];
        int i = 0;
        for (SeriesDataset set : sets) {
            stats[i++] = new SeriesStatistics(set);
        }
        return stats;
    }

    public final String series;
    public final String instance;
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
        this.instance = set.getInstance();
        this.series = set.getSeries().toString();
        this.points = set.points();
        this.observedMax = set.getObservedMax();
        this.observedMin = set.getObservedMin();
        this.observedSum = set.getObservedSum();
        this.observedValues = set.getObservedValues();
        this.observedValueChanges = set.getObservedValueChanges();
        this.observedSince = set.getObservedSince();
        this.stableCount = set.getStableCount();
        this.stableSince = set.getStableSince();
    }
}
