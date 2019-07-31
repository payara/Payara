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
 * The store uses two maps working like a doubled buffered image. While collection writes to the {@link #secondsWrite} map
 * request are served from the {@link #secondsRead} map. This makes sure that a consistent overall snapshot over all series can
 * be used to create a consistent visualisation that isn't halve updated while the response is composed. However this
 * requires that callers are provided with a method that returns all the {@link SeriesSlidingWindow}s they need in a
 * single method invocation.
 * 
 * @author Jan Bernitt
 */
@ApplicationScoped
public class SlidingWindowMonitoringDataStore implements MonitoringDataStore {

    private ServiceLocator serviceLocator;
    private volatile Map<Series, SeriesSlidingWindow> secondsWrite = new ConcurrentHashMap<>();
    private volatile Map<Series, SeriesSlidingWindow> secondsRead = new ConcurrentHashMap<>();
    private long collectedSecond;

    @PostConstruct
    public void init() {
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        serviceLocator.getService(PayaraExecutorService.class).scheduleAtFixedRate(this::collectAllSources, 0L, 1L, TimeUnit.SECONDS);
    }

    private void collectAllSources() {
        List<MonitoringDataSource> sources = serviceLocator.getAllServices(MonitoringDataSource.class);
        collectedSecond = (System.currentTimeMillis() / 1000L) * 1000L;
        MonitoringDataCollector collector = new SinkDataCollector(this::addPoint);
        long collectionStart = System.currentTimeMillis();
        int collectedSources = 0;
        int failedSources = 0;
        for (MonitoringDataSource source : sources) {
            try {
                collectedSources++;
                source.collect(collector);
            } catch (RuntimeException e) {
                failedSources++;
                // ignore and continue with next
            }
        }
        collector.in("collect")
            .collect("duration", System.currentTimeMillis() - collectionStart)
            .collectNonZero("sources", collectedSources)
            .collectNonZero("failed", failedSources);
        swap();
    }

    private void swap() {
        Map<Series, SeriesSlidingWindow> tmp = secondsRead;
        secondsRead = secondsWrite;
        secondsWrite = tmp;
    }

    private void addPoint(CharSequence key, long value) {
        Series s = new Series(key.toString());
        SeriesSlidingWindow window = secondsRead.get(s);
        if (window == null) {
            window = new SeriesSlidingWindow(s, 60);
        }
        secondsWrite.put(s, window.add(collectedSecond, value));
    }

    @Override
    public SeriesSlidingWindow selectSlidingWindow(Series series) {
        SeriesSlidingWindow res = secondsRead.get(series);
        return res != null ? res : new SeriesSlidingWindow(series, 0);
    }

    @Override
    public Iterable<SeriesSlidingWindow> selectAllSeriesWindow() {
        return secondsRead.values();
    }
}
