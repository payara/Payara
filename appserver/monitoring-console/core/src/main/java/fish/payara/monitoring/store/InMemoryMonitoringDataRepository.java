package fish.payara.monitoring.store;

import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.hazelcast.HazelcastCore;

/**
 * A simple in-memory store for a fixed size sliding window for each {@link Series}.
 * 
 * 
 * <h3>Consistency Remarks</h3>
 * 
 * The store uses two maps working like a doubled buffered image. While collection writes to the {@link #secondsWrite}
 * map request are served from the {@link #secondsRead} map. This makes sure that a consistent dataset across all series
 * can be used to create a consistent visualisation that isn't halve updated while the response is composed. However
 * this requires that callers are provided with a method that returns all the {@link SeriesDataset}s they need in a
 * single method invocation. Making multiple calls to this stores methods does not guarantee a consistent dataset across
 * all series since the {@link #swapLocalBuffer()} can happen inbetween method calls.
 * 
 * @author Jan Bernitt
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class InMemoryMonitoringDataRepository implements MonitoringDataRepository {

    private ServiceLocator serviceLocator;
    private ServerEnvironment serverEnv;
    private boolean isDas;
    private String instanceName;
    private ITopic<SeriesDatasetsSnapshot> exchange;

    private volatile Map<Series, SeriesDataset> secondsWrite = new ConcurrentHashMap<>();
    private volatile Map<Series, SeriesDataset> secondsRead = new ConcurrentHashMap<>();
    private final Map<Series, SeriesDataset[]> remoteInstanceDatasets = new ConcurrentHashMap<>();
    private long collectedSecond;
    private int estimatedNumberOfSeries = 50;

    @PostConstruct
    public void init() {
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        serverEnv = serviceLocator.getService(ServerEnvironment.class);
        HazelcastCore hz = serviceLocator.getService(HazelcastCore.class);
        instanceName = hz.getInstance().getCluster().getLocalMember().getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
        exchange = hz.getInstance().getTopic(MONITORING_DATA_CLUSTER_STORE_NAME);
        isDas = serverEnv.isDas();
        PayaraExecutorService executor = serviceLocator.getService(PayaraExecutorService.class);
        if (isDas) {
            MessageListener<SeriesDatasetsSnapshot> subscriber = this::addRemoteDatasets;
            exchange.addMessageListener(subscriber);
            executor.scheduleAtFixedRate(this::collectSourcesToMemory, 0L, 1L, TimeUnit.SECONDS);
        } else {
            executor.scheduleAtFixedRate(this::collectSourcesToPublish, 0L, 1L, TimeUnit.SECONDS);
        }
    }

    public void addRemoteDatasets(Message<SeriesDatasetsSnapshot> message) {
        String instance = message.getPublishingMember().getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
        SeriesDatasetsSnapshot snapshot = message.getMessageObject();
        long time = snapshot.time;
        for (int i = 0; i < snapshot.numberOfSeries; i++) {
            Series series = new Series(snapshot.series[i]);
            long value = snapshot.values[i];
            remoteInstanceDatasets.compute(series, (key, seriesByInstance) -> addRemotePoint(seriesByInstance, instance, key, time, value));
        }
    }

    private static SeriesDataset[] addRemotePoint(SeriesDataset[] seriesByInstance, String instance, Series series, long time, long value) {
        if (seriesByInstance == null) {
            return new SeriesDataset[] { new EmptyDataset(instance, series, 60).add(time, value) };
        }
        for (int i = 0; i < seriesByInstance.length; i++) {
            SeriesDataset instanceSet = seriesByInstance[i];
            if (instanceSet.getInstance().equals(instance)) {
                seriesByInstance[i] = seriesByInstance[i].add(time, value);
                return seriesByInstance;
            }
        }
        seriesByInstance = Arrays.copyOf(seriesByInstance, seriesByInstance.length + 1);
        seriesByInstance[seriesByInstance.length - 1] = new EmptyDataset(instance, series, 60).add(time, value);
        return seriesByInstance;
    }

    private void collectSourcesToMemory() {
        tick();
        collectAll(new SinkDataCollector(this::addLocalPoint));
        swapLocalBuffer();
    }

    private void collectSourcesToPublish() {
        tick();
        SeriesDatasetsSnapshot msg = new SeriesDatasetsSnapshot(collectedSecond, estimatedNumberOfSeries);
        collectAll(new SinkDataCollector(msg));
        estimatedNumberOfSeries = msg.numberOfSeries;
        exchange.publish(msg);
    }

    private void collectAll(MonitoringDataCollector collector) {
        List<MonitoringDataSource> sources = serviceLocator.getAllServices(MonitoringDataSource.class);
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
        long estimatedTotalBytesMemory = 0L;
        for (SeriesDataset set : secondsWrite.values()) {
            estimatedTotalBytesMemory += set.estimatedBytesMemory();
        }
        int seriesCount = secondsWrite.size();
        collector.in("collect")
            .collect("duration", System.currentTimeMillis() - collectionStart)
            .collectNonZero("series", seriesCount)
            .collectNonZero("estimatedTotalBytesMemory", estimatedTotalBytesMemory)
            .collectNonZero("estimatedAverageBytesMemory", seriesCount == 0 ? 0L : Math.round(estimatedTotalBytesMemory / seriesCount))
            .collectNonZero("sources", collectedSources)
            .collectNonZero("failed", failedSources);
    }

    /**
     * Forwards the collection time to the current second (milliseconds are stripped)
     */
    private void tick() {
        collectedSecond = (System.currentTimeMillis() / 1000L) * 1000L;
    }

    private void swapLocalBuffer() {
        Map<Series, SeriesDataset> tmp = secondsRead;
        secondsRead = secondsWrite;
        secondsWrite = tmp;
    }

    private void addLocalPoint(CharSequence key, long value) {
        Series series = new Series(key.toString());
        secondsWrite.put(series, secondsRead.computeIfAbsent(series, this::emptySet).add(collectedSecond, value));
    }

    private SeriesDataset emptySet(Series series) {
        return new EmptyDataset(instanceName, series, 60);
    }

    @Override
    public List<SeriesDataset> selectSeries(Series series) {
        if (!isDas) {
            return emptyList();
        }
        SeriesDataset localSet = secondsRead.get(series);
        SeriesDataset[] remoteSets = remoteInstanceDatasets.get(series);
        if (remoteSets == null) {
            return singletonList(localSet);
        }
        long cutOffTime = System.currentTimeMillis() - 30_000;
        List<SeriesDataset> res = new ArrayList<>(remoteSets.length + 1);
        if (!localSet.isStableZero()) {
            res.add(localSet);
        }
        for (SeriesDataset remoteSet : remoteSets) {
            if (remoteSet.lastTime() >= cutOffTime && !remoteSet.isStableZero()) {
                res.add(remoteSet);
            }
        }
        if (res.isEmpty()) {
            res.add(localSet);
        }
        return res;
    }

    @Override
    public Iterable<SeriesDataset> selectAllSeries() {
        return secondsRead.values();
    }

    static final class SeriesDatasetsSnapshot implements Serializable, MonitoringDataSink {
        final long time;
        int numberOfSeries;
        String[] series;
        long[] values;

        SeriesDatasetsSnapshot(long time, int estimatedNumberOfSeries) {
            this.time = time;
            this.series = new String[estimatedNumberOfSeries];
            this.values = new long[estimatedNumberOfSeries];
        }

        @Override
        public void accept(CharSequence key, long value) {
            if (numberOfSeries >= series.length) {
                series = copyOf(series, Math.round(series.length * 1.3f));
                values = copyOf(values, series.length);
            }
            series[numberOfSeries] = key.toString();
            values[numberOfSeries++] = value;
        }
    }
}

