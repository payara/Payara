package fish.payara.monitoring.collect;

import org.jvnet.hk2.annotations.Contract;

/**
 * Implemented by each source of monitoring data to provide the current data.
 * 
 * Each source first establishes its relative context using the
 * {@link MonitoringDataCollector#tag(CharSequence, CharSequence)} method. After the context is complete for a group of
 * data points these are added using {@link MonitoringDataCollector#collect(CharSequence, long)}.
 *
 * @author Jan Bernitt
 */
@Contract
@FunctionalInterface
public interface MonitoringDataSource {

    /**
     * Collects all the data points of this at the current moment.
     * 
     * @param collector the {@link MonitoringDataCollector} instance to use to collect the data points of this source
     */
    void collect(MonitoringDataCollector collector);
}
