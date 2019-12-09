package fish.payara.monitoring.alert;

import fish.payara.monitoring.model.SeriesDataset;

public final class Condition {

    public static final Condition NONE = new Condition(Operator.EQ, 0L, 0, 0, 0);

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
    public final int forTimes;
    public final int forPercent;
    public final int forMillis;

    public Condition(Operator comparison, long threshold, int forTimes, int forPercent,
            int forMillis) {
        this.comparison = comparison;
        this.threshold = threshold;
        this.forTimes = forTimes;
        this.forPercent = forPercent;
        this.forMillis = forMillis;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isSatisfied(SeriesDataset data) {
        if (isNone()) {
            return true;
        }
        long value = data.lastValue();
        if (!compare(value)) {
            return false;
        }
        if (forMillis > 0) {
            return isSatisfiedForMillis(data);
        }
        if (forPercent > 0) {
            return isSatisfiedForPercent(data);
        }
        if (forTimes > 0) {
            return isSatisfiedForTimes(data);
        }
        return true;
    }

    private boolean isSatisfiedForMillis(SeriesDataset data) {
        if (data.isStable()) {
            return data.getStableSince() <= data.lastTime() - forMillis ;
        }
        long startTime = data.lastTime() - forMillis;
        if (data.firstTime() > startTime) {
            return false;
        }
        long[] points = data.points();
        int index = points.length - 2; // last time index
        while (index >= 0 && points[index] >= startTime) {
            if (!compare(points[index])) {
                return false;
            }
            index -= 2;
        }
        return index >= 0;
    }

    private boolean isSatisfiedForPercent(SeriesDataset data) {
        if (data.isStable()) {
            return true;
        }
        long[] points = data.points();
        int satisfiedCount = 0;
        int comparedCount = 0;
        for (int i = 0; i < points.length; i+= 2) {
            comparedCount++;
            satisfiedCount += compare(points[i + 1]) ? 1 : 0;
        }
        return 100 * satisfiedCount / comparedCount > forPercent;
    }

    private boolean isSatisfiedForTimes(SeriesDataset data) {
        if (data.isStable()) {
            return data.getStableCount() >= forTimes;
        }
        long[] points = data.points();
        int index = points.length - 2; // last time index
        for (int i = 0; i < forTimes; i++) {
            if (!compare(points[index + 1])) {
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
        return (int) (comparison.hashCode() ^ threshold ^ forMillis ^ forPercent ^ forTimes); // good enough to avoid most collisions
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Condition && equalTo((Condition) obj);
    }

    public boolean equalTo(Condition other) {
        return comparison == other.comparison && threshold == other.threshold
                && forMillis == other.forMillis && forPercent == other.forPercent && forTimes == other.forTimes;
    }

    @Override
    public String toString() {
        if (isNone()) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        str.append("value ").append(comparison.symbol).append(' ').append(threshold);
        if (forTimes > 0) {
            str.append(" for ").append(forTimes).append(" times");
        }
        if (forPercent > 0) {
            str.append(" for ").append(forPercent).append('%');
        }
        if (forMillis > 0) {
            str.append(" for ").append(forMillis).append("ms");
        }
        return str.toString();
    }
}
