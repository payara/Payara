package fish.payara.monitoring.store;

import org.jvnet.hk2.annotations.Contract;

import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

@Contract
public interface MonitoringDataStore {

    Iterable<SeriesDataset> selectAllSeries();

    SeriesDataset selectSeries(Series series);
}
