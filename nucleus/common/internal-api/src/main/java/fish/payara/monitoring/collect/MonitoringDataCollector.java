package fish.payara.monitoring.collect;

public interface MonitoringDataCollector {

    MonitoringDataCollector collect(CharSequence key, long value);


    MonitoringDataCollector tag(CharSequence name, CharSequence value);

    default MonitoringDataCollector collect(CharSequence group, MonitoringDataSource source) {
        source.collect(tag("group", group));
        return this;
    }

    default MonitoringDataCollector in(CharSequence namespace) {
        return tag("ns", namespace);
    }
}
