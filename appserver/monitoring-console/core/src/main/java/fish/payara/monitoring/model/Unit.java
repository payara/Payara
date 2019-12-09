package fish.payara.monitoring.model;

public enum Unit {

    COUNT("count"),
    PERCENT("percent"),
    BYTES("bytes"),
    SECONDS("sec"),
    MILLIS("ms"),
    NANOS("ns");

    public static Unit fromShortName(String shortName) {
        for (Unit u : values()) {
            if (u.shortName.equals(shortName)) {
                return u;
            }
        }
        return Unit.COUNT;
    }

    private final String shortName;

    Unit(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }
}