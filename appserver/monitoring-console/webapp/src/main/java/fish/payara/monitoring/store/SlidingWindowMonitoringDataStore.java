package fish.payara.monitoring.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.SinkDataCollector;
import fish.payara.nucleus.executorservice.PayaraExecutorService;

/**
 * A simple in-memory store for a fixed size sliding window for each {@link Series}.
 * 
 * @author Jan Bernitt
 */
@ApplicationScoped
public class SlidingWindowMonitoringDataStore implements MonitoringDataStore {

    private ServiceLocator serviceLocator;
    private Map<Series, SeriesSlidingWindow> slidingWindowsPerSeries = new ConcurrentHashMap<>();
    private long time;

    @PostConstruct
    public void init() {
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        serviceLocator.getService(PayaraExecutorService.class).scheduleAtFixedRate(this::collectAllSources, 0L, 5L, TimeUnit.SECONDS);
    }

    private void collectAllSources() {
        List<MonitoringDataSource> sources = serviceLocator.getAllServices(MonitoringDataSource.class);
        time = (System.currentTimeMillis() / 1000L) * 1000L;
        MonitoringDataCollector collector = new SinkDataCollector(this::addPoint);
        for (MonitoringDataSource source : sources) {
            try {
                source.collect(collector);
            } catch (RuntimeException e) {
                // ignore and continue with next
            }
        }
    }

    private void addPoint(CharSequence key, long value) {
        slidingWindowsPerSeries.computeIfAbsent(new Series(key.toString()), 
                series -> new SeriesSlidingWindow(series, 50)).add(time, value);
    }

    @Override
    public SeriesSlidingWindow selectSlidingWindow(Series series) {
        SeriesSlidingWindow res = slidingWindowsPerSeries.get(series);
        return res != null ? res : new SeriesSlidingWindow(series, 0);
    }

    @Override
    public Iterable<SeriesSlidingWindow> selectAllSeriesWindow() {
        return slidingWindowsPerSeries.values();
    }
}
