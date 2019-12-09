package fish.payara.monitoring.model;

import java.util.List;

@FunctionalInterface
public interface SeriesLookup {

    List<SeriesDataset> selectSeries(Series series, String... instances);
}
