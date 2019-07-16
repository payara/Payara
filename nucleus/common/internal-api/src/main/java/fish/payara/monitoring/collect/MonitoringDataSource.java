package fish.payara.monitoring.collect;

import org.jvnet.hk2.annotations.Contract;

/**
 * Implemented by each source of monitoring data to provide the current data.
 *
 * @author Jan Bernitt
 */
@Contract
public interface MonitoringDataSource {

    void collect(MonitoringDataCollector collector);
}
