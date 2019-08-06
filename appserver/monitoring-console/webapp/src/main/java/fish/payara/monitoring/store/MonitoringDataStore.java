package fish.payara.monitoring.store;

import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

public interface MonitoringDataStore {

    Iterable<SeriesDataset> selectAllSeries();

    SeriesDataset selectSeries(Series series);
}
