package fish.payara.monitoring.collect;

@FunctionalInterface
public interface MonitoringDataSink {

    void accept(CharSequence key, long value);
}
