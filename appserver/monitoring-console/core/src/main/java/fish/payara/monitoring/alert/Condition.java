package fish.payara.monitoring.alert;

import java.util.Objects;

import fish.payara.monitoring.model.SeriesDataset;

public final class Condition {

    public static final Condition NONE = new Condition(Operator.EQ, 0L);

    public enum Operator {
        LT("<"), LE("<="), EQ("="), GT(">"), GE(">=");

        public String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    public final Operator comparison;
    public final long threshold;
    public final Number forLast;
    public final boolean onAverage;

    public Condition(Operator comparison, long threshold) {
        this(comparison, threshold, null, false);
    }

    public Condition(Operator comparison, long threshold, Number forLast, boolean onAverage) {
        this.comparison = comparison;
        this.threshold = threshold;
        this.forLast = forLast;
        this.onAverage = onAverage;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isForLastPresent() {
        return forLast != null;
    }

    public boolean isForLastMillis() {
        return forLast instanceof Long;
    }

    public boolean isForLastTimes() {
        return forLast instanceof Integer;
    }

    public boolean isSatisfied(SeriesDataset data) {
        if (isNone()) {
            return true;
        }
        long value = data.lastValue();
        if ((!onAverage || data.isStable()) && !compare(value)) {
            return false;
        }
        if (isForLastMillis()) {
            return isSatisfiedForLastMillis(data);
        }
        if (isForLastTimes()) {
            return isSatisfiedForLastTimes(data);
        }
        return true;
    }

    private boolean isSatisfiedForLastMillis(SeriesDataset data) {
        long forLastMillis = forLast.longValue();
        if (forLastMillis <= 0) {
            return isSatisfiedForLastTimes(data.points(), -1);
        }
        if (data.isStable()) {
            return data.getStableSince() <= data.lastTime() - forLastMillis ;
        }
        long startTime = data.lastTime() - forLastMillis;
        long[] points = data.points();
        if (points[0] > startTime && forLastMillis < 30000L) {
            return false; // not enough data
        }
        int index = points.length - 2; // last time index
        while (index >= 0 && points[index] > startTime) {
            index -= 2;
        }
        return isSatisfiedForLastTimes(points, index <= 0 ? points.length / 2 : (points.length - index) / 2);
    }

    private boolean isSatisfiedForLastTimes(SeriesDataset data) {
        int forLastTimes = forLast.intValue();
        if (data.isStable()) {
            return data.getStableCount() >= forLastTimes;
        }
        return isSatisfiedForLastTimes(data.points(), forLastTimes);
    }

    private boolean isSatisfiedForLastTimes(long[] points, int forLastTimes) {
        int maxPoints = points.length / 2;
        int n = forLastTimes <= 0 ? maxPoints : Math.min(maxPoints, forLastTimes);
        if (forLastTimes > 0 && n < forLastTimes && n < 30) {
            return false; // not enough data yet
        }
        int index = points.length - 1; // last value index
        if (onAverage) {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                sum += points[index];
                index -= 2;
            }
            return compare(sum / n);
        }
        for (int i = 0; i < n; i++) {
            if (!compare(points[index])) {
                return false;
            }
            index -= 2;
        }
        return true;
    }

    private boolean compare(long value) {
        switch (comparison) {
        default:
        case EQ: return value == threshold;
        case LE: return value <= threshold;
        case LT: return value < threshold;
        case GE: return value >= threshold;
        case GT: return value > threshold;
        }
    }

    @Override
    public int hashCode() {
        return (int) (comparison.hashCode() ^ threshold ^ (forLast == null ? 0 : forLast.intValue())); // good enough to avoid most collisions
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Condition && equalTo((Condition) obj);
    }

    public boolean equalTo(Condition other) {
        return comparison == other.comparison && threshold == other.threshold
                && Objects.equals(forLast, other.forLast) && onAverage == other.onAverage;
    }

    @Override
    public String toString() {
        if (isNone()) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        str.append("value ").append(comparison.symbol).append(' ').append(threshold);
        if (isForLastMillis()) {
            str.append(" for ").append(forLast).append(" times");
        }
        if (isForLastTimes()) {
            str.append(" for ").append(forLast).append("ms");
        }
        return str.toString();
    }

}
