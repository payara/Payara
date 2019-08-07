package fish.payara.monitoring.store;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
import fish.payara.monitoring.model.ConstantDataset;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.store.ClusteredStore;

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
 * all series since the {@link #swap()} can happen inbetween method calls.
 * 
 * @author Jan Bernitt
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class InMemoryMonitoringDataRepository implements MonitoringDataRepository, MessageListener<SeriesDataset[]> {

    private static final Logger LOGGER = Logger.getLogger(InMemoryMonitoringDataRepository.class.getName());

    private ServiceLocator serviceLocator;
    private ServerEnvironment serverEnv;
    private ITopic<SeriesDataset[]> exchange;

    private volatile ConcurrentHashMap<Series, SeriesDataset> secondsWrite = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<Series, SeriesDataset> secondsRead = new ConcurrentHashMap<>();
    private long collectedSecond;

    @PostConstruct
    public void init() {
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        serverEnv = serviceLocator.getService(ServerEnvironment.class);
        HazelcastCore hz = serviceLocator.getService(HazelcastCore.class);
        exchange = hz.getInstance().getTopic(MONITORING_DATA_CLUSTER_STORE_NAME);
        if (serverEnv.isDas()) {
            exchange.addMessageListener(this);
        }
        serviceLocator.getService(PayaraExecutorService.class).scheduleAtFixedRate(this::collectAllSources, 0L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(Message<SeriesDataset[]> message) {
        LOGGER.info("received "+message.getMessageObject().length);
    }

    private void collectAllSources() {
        List<MonitoringDataSource> sources = serviceLocator.getAllServices(MonitoringDataSource.class);
        tick();
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
        swap();
        aggregateCluster();
    }

    /**
     * The DAS fetches the data shared by other instances while other instances share their data with the DAS using the
     * clustered store.
     */
    private void aggregateCluster() {
        if (!serverEnv.isDas()) {
            SeriesDataset[] msg = new SeriesDataset[secondsRead.size()];
            int i = 0;
            for (SeriesDataset set : secondsRead.values()) {
                msg[i++] = set;
            }
            exchange.publish(msg);
        } else {
        }
    }

    /**
     * Forwards the collection time to the current second (milliseconds are stripped)
     */
    private void tick() {
        collectedSecond = (System.currentTimeMillis() / 1000L) * 1000L;
    }

    private void swap() {
        ConcurrentHashMap<Series, SeriesDataset> tmp = secondsRead;
        secondsRead = secondsWrite;
        secondsWrite = tmp;
    }

    private void addPoint(CharSequence key, long value) {
        Series series = new Series(key.toString());
        SeriesDataset dataset = secondsRead.get(series);
        if (dataset == null) {
            dataset = new ConstantDataset(series, 60, collectedSecond, value);
        } else {
            dataset = dataset.add(collectedSecond, value);
        }
        secondsWrite.put(series, dataset);
    }

    @Override
    public SeriesDataset selectSeries(Series series) {
        SeriesDataset res = secondsRead.get(series);
        return res != null ? res : new EmptyDataset(series, 0);
    }

    @Override
    public Iterable<SeriesDataset> selectAllSeries() {
        return secondsRead.values();
    }
}
