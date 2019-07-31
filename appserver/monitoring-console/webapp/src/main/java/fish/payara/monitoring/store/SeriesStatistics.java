package fish.payara.monitoring.store;

import java.math.BigInteger;

public final class SeriesStatistics {

    public final String series;
    public final Point[] points;
    public final long observedMax;
    public final long observedMin;
    public final BigInteger observedSum;
    public final int observedValues;
    public final int observedValueChanges;
    public final int unchangedCount;
    public final long unchangedSince;

    public SeriesStatistics(String series, Point[] points, long observedMax, long observedMin, BigInteger observedSum,
            int observedValues, int observedValueChanges, int unchangedCount, long unchangedSince) {
        this.series = series;
        this.points = points;
        this.observedMax = observedMax;
        this.observedMin = observedMin;
        this.observedSum = observedSum;
        this.observedValues = observedValues;
        this.observedValueChanges = observedValueChanges;
        this.unchangedCount = unchangedCount;
        this.unchangedSince = unchangedSince;
    }
}
