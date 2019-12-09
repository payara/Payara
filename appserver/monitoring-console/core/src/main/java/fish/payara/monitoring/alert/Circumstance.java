package fish.payara.monitoring.alert;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.SeriesLookup;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.SeriesDataset;

public final class Circumstance {

    public static final Circumstance NONE = new Circumstance(Level.WHITE, Condition.NONE, Condition.NONE);

    public final Level level;
    public final Condition start;
    public final Condition stop;
    public final Condition suppress;
    public final Metric suppressing;

    public Circumstance(Level level, Condition start, Condition stop) {
        this(level, start, stop, null, Condition.NONE);
    }

    public Circumstance(Level level, Condition start, Condition stop, Metric suppressing, Condition suppress) {
        this.level = level;
        this.start = start;
        this.stop = stop;
        this.suppressing = suppressing;
        this.suppress = suppress;
    }

    public boolean isNone() {
        return level == Level.WHITE;
    }

    public boolean starts(SeriesDataset data, SeriesLookup lookup) {
        if (isNone()) {
            return false;
        }
        if (!suppress.isNone()) {
            for (SeriesDataset set : lookup.selectSeries(suppressing.series, data.getInstance())) {
                if (suppress.isSatisfied(set)) {
                    return false;
                }
            }
        }
        return start.isSatisfied(data);
    }

    public boolean stops(SeriesDataset sets) {
        return stop.isNone() ? false : stop.isSatisfied(sets);
    }

    @Override
    public int hashCode() {
        return level.hashCode() ^ start.hashCode() ^ stop.hashCode() ^ suppress.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Circumstance && equalTo((Circumstance) obj);
    }

    public boolean equalTo(Circumstance other) {
        return level == other.level && start.equalTo(other.start) && stop.equalTo(other.stop)
                && suppress.equalTo(other.suppress) 
                && (suppressing == null && other.suppressing == null || suppressing != null && suppressing.equalTo(other.suppressing));
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(level.name()).append(": ");
        if (!start.isNone()) {
            str.append(start.toString());
        }
        if (!stop.isNone()) {
            str.append(" until ").append(stop.toString());
        }
        if (!suppress.isNone()) {
            str.append(" unless ").append(suppressing).append(' ').append(suppress.toString());
        }
        return str.toString();
    }
}
