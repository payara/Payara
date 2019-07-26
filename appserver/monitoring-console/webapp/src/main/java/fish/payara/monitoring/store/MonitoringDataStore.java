package fish.payara.monitoring.store;

public interface MonitoringDataStore {

    Iterable<SeriesSlidingWindow> selectAllSeriesWindow();

    SeriesSlidingWindow selectSlidingWindow(Series series);
}
