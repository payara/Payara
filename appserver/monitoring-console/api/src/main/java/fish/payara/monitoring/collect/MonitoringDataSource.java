package fish.payara.monitoring.collect;

/**
 * Implemented by each source of monitoring data to provide the current data.
 *
 * @author Jan Bernitt
 */
public interface MonitoringDataSource {

    void collect(MonitoringDataCollector collector);
}
