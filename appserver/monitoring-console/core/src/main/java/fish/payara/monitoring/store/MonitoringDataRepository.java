package fish.payara.monitoring.store;

import org.jvnet.hk2.annotations.Contract;

import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

@Contract
public interface MonitoringDataRepository {

    /**
     * The store name used in the cluster store to share data of instances with the DAS.
     */
    String MONITORING_DATA_CLUSTER_STORE_NAME = "payara-monitoring-data";

    Iterable<SeriesDataset> selectAllSeries();

    SeriesDataset selectSeries(Series series);
}
