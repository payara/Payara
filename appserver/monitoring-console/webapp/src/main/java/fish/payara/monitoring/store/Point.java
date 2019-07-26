package fish.payara.monitoring.store;

public final class Point {

    public final long time;
    public final long value;

    public Point(long time, long value) {
        this.time = time;
        this.value = value;
    }

}
