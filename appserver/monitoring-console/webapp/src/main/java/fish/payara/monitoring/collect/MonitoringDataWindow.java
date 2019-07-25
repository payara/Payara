package fish.payara.monitoring.collect;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import fish.payara.nucleus.executorservice.PayaraExecutorService;

@ApplicationScoped
public class MonitoringDataWindow implements Iterable<Entry<Series, PointsWindow>> {

    private ServiceLocator serviceLocator;
    private Map<Series, PointsWindow> slidingWindowsPerSeries = new ConcurrentHashMap<>();
    private long time;

    @PostConstruct
    public void init() {
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        serviceLocator.getService(PayaraExecutorService.class).scheduleAtFixedRate(this::collectAll, 0L, 5L, TimeUnit.SECONDS);
    }

    private void collectAll() {
        List<MonitoringDataSource> sources = serviceLocator.getAllServices(MonitoringDataSource.class);
        time = (System.currentTimeMillis() / 1000L) * 1000L;
        MonitoringDataCollector collector = new SinkDataCollector(this::addToWindow);
        for (MonitoringDataSource source : sources) {
            try {
                source.collect(collector);
            } catch (RuntimeException e) {
                // ignore and continue with next
            }
        }
    }

    private void addToWindow(CharSequence key, long value) {
        slidingWindowsPerSeries.computeIfAbsent(new Series(key.toString()), k -> new PointsWindow(20)).add(time, value);
    }

    @Override
    public Iterator<Entry<Series, PointsWindow>> iterator() {
        return slidingWindowsPerSeries.entrySet().iterator();
    }
}
