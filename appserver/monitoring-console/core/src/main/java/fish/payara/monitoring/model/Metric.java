package fish.payara.monitoring.model;

public final class Metric {

    public final Series series;
    public final Unit unit;

    public Metric(Series series) {
        this(series, Unit.COUNT);
    }

    public Metric(Series series, Unit unit) {
        this.series = series;
        this.unit = unit;
    }

    public Metric withUnit(Unit unit) {
        return new Metric(series, unit);
    }

    @Override
    public int hashCode() {
        return series.hashCode() ^ unit.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Metric && equalTo((Metric) obj);
    }

    public boolean equalTo(Metric other) {
        return series.equalTo(other.series) && unit == other.unit;
    }

    @Override
    public String toString() {
        return series + " unit:" + unit.toString();
    }
}
