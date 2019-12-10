package fish.payara.monitoring.store;

import static fish.payara.monitoring.alert.Condition.Operator.GE;
import static fish.payara.monitoring.alert.Condition.Operator.LT;
import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Config;

import fish.payara.monitoring.alert.Alert;
import fish.payara.monitoring.alert.AlertService;
import fish.payara.monitoring.alert.Circumstance;
import fish.payara.monitoring.alert.Condition;
import fish.payara.monitoring.alert.Watch;
import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.Unit;
import fish.payara.nucleus.executorservice.PayaraExecutorService;

@Service
@RunLevel(StartupRunLevel.VAL)
public class InMemoryAlarmService extends ConfigListeningService implements AlertService {

    private static final int MAX_ALERTS_PER_SERIES = 10;
    @Inject
    private PayaraExecutorService executor;
    @Inject
    private ServerEnvironment serverEnv;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;
    @Inject
    private MonitoringDataRepository monitoringDataRepository;

    private boolean isDas;
    private final JobHandle checker = new JobHandle("watch checker");
    private final Map<Series, Map<Integer, Watch>> simpleWatches = new ConcurrentHashMap<>();
    private final Map<Series, Map<Integer, Watch>> patternWatches = new ConcurrentHashMap<>();
    private final Map<Series, Deque<Alert>> alerts = new ConcurrentHashMap<>();
    private final AtomicReference<AlertStatistics> statistics = new AtomicReference<>(new AlertStatistics());

    @PostConstruct
    public void init() {
        isDas = serverEnv.isDas();
        changedConfig(parseBoolean(serverConfig.getMonitoringService().getMonitoringEnabled()));
        addWatch(new Watch("Heap Usage", new Metric(new Series("ns:jvm HeapUsage"), Unit.PERCENT), 
                new Circumstance(Level.RED, new Condition(GE, 30, 5, 0, 0), new Condition(LT, 30, 5, 0, 0)), 
                new Circumstance(Level.AMBER, new Condition(GE, 20, 5, 0, 0), new Condition(LT, 20, 5, 0, 0)), 
                new Circumstance(Level.GREEN, new Condition(LT, 20, 1, 0, 0), Condition.NONE), 
                new Metric(new Series("ns:jvm CpuUsage"), Unit.PERCENT)));
    }

    @Override
    void changedConfig(boolean enabled) {
        if (!isDas) {
            return; // only check alerts on DAS
        }
        if (!enabled) {
            checker.stop();
        } else {
            checker.start(executor, 2, SECONDS, this::checkWatches);
        }
    }

    @Override
    public AlertStatistics getAlertStatistics() {
        return statistics.get();
    }

    @Override
    public Collection<Alert> alertsMatching(Predicate<Alert> filter) {
        List<Alert> matches = new ArrayList<>();
        for (Queue<Alert> queue : alerts.values()) {
            for (Alert a : queue) {
                if (filter.test(a)) {
                    matches.add(a);
                }
            }
        }
        return matches;
    }

    @Override
    public Collection<Alert> alerts() {
        List<Alert> all = new ArrayList<>();
        for (Queue<Alert> queue : alerts.values()) {
            all.addAll(queue);
        }
        return all;
    }

    @Override
    public Collection<Alert> alertsFor(Series series) {
        if (!series.isPattern()) {
            // this is the usual path called while polling for data so this should not be too expensive
            return unmodifiableCollection(alerts.get(series));
        }
        Collection<Alert> matches = null;
        for (Entry<Series, Deque<Alert>> e : alerts.entrySet()) {
            if (series.matches(e.getKey())) {
                if (matches == null) {
                    matches = e.getValue();
                } else {
                    if (!(matches instanceof ArrayList)) {
                        matches = new ArrayList<>(matches);
                    }
                    matches.addAll(e.getValue());
                }
            }
        }
        return matches == null ? emptyList() : unmodifiableCollection(matches);
    }

    @Override
    public void addWatch(Watch watch) {
        Series series = watch.watched.series;
        Map<Series, Map<Integer, Watch>> target = series.isPattern() ? patternWatches : simpleWatches;
        target.computeIfAbsent(series, key -> new ConcurrentHashMap<>()).put(watch.serial, watch);
    }

    @Override
    public void removeWatchBySerial(int serial) {
        removeWatchBySerial(serial, patternWatches);
        removeWatchBySerial(serial, simpleWatches);
    }

    private static void removeWatchBySerial(int serial, Map<Series, Map<Integer, Watch>> map) {
        Iterator<Entry<Series, Map<Integer, Watch>>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Series, Map<Integer, Watch>> entry = iter.next();
            Map<Integer, Watch> watches = entry.getValue();
            Watch removed = watches.remove(serial);
            if (watches.isEmpty()) {
                iter.remove();
            }
            if (removed != null) {
                return;
            }
        }
    }

    @Override
    public Collection<Watch> watches() {
        List<Watch> all = new ArrayList<>();
        for (Map<Integer, Watch> watches : simpleWatches.values()) {
            all.addAll(watches.values());
        }
        for (Map<Integer, Watch> watches : patternWatches.values()) {
            all.addAll(watches.values());
        }
        return all;
    }

    @Override
    public Collection<Watch> wachtesFor(Series series) {
        if (!series.isPattern()) {
            // this is the usual path called while polling for data so this should not be too expensive
            Map<Integer, Watch> watches = simpleWatches.get(series);
            return watches == null ? emptyList() : unmodifiableCollection(watches.values());
        }
        Collection<Watch> watches = patternWatches.get(series).values();
        for (Map<Integer, Watch> simple : simpleWatches.values() ) {
            for (Watch w : simple.values()) {
                if (series.matches(w.watched.series)) {
                    if (!(watches instanceof ArrayList)) {
                        watches = new ArrayList<>(watches);
                    }
                    watches.add(w);
                }
            }
        }
        return unmodifiableCollection(watches);
    }

    private void checkWatches() {
        try {
            checkWatches(simpleWatches.values());
            checkWatches(patternWatches.values());
            statistics.set(computeStatistics());
        } catch (Exception ex) {
            LOGGER.log(java.util.logging.Level.FINE, "Failed to check watches", ex);
        }
    }

    private AlertStatistics computeStatistics() {
        AlertStatistics stats = new AlertStatistics();
        stats.changeCount = Alert.getChangeCount();
        if (alerts.isEmpty()) {
            return stats;
        }
        for (Deque<Alert> seriesAlerts : alerts.values()) {
            for (Alert a : seriesAlerts) {
                if (a.getLevel() == Level.RED) {
                    if (a.isAcknowledged()) {
                        stats.acknowledgedRedAlerts++;
                    } else {
                        stats.unacknowledgedRedAlerts++;
                    }
                } else if (a.getLevel() == Level.AMBER) {
                    if (a.isAcknowledged()) {
                        stats.acknowledgedAmberAlerts++;
                    } else {
                        stats.unacknowledgedAmberAlerts++;
                    }
                }
            }
        }
        return stats;
    }

    private void checkWatches(Collection<Map<Integer, Watch>> watches) {
        for (Map<Integer, Watch> group : watches) {
            for (Watch watch : group.values()) {
                try {
                    checkWatch(watch);
                } catch (Exception ex) {
                    LOGGER.log(java.util.logging.Level.FINE, "Failed to check watch : " + watch, ex);
                }
            }
        }
    }

    private void checkWatch(Watch watch) {
        for (Alert newlyRaised : watch.check(monitoringDataRepository)) {
            Deque<Alert> seriesAlerts = alerts.computeIfAbsent(newlyRaised.getSeries(),
                    key -> new ConcurrentLinkedDeque<>());
            seriesAlerts.add(newlyRaised);
            if (seriesAlerts.size() > MAX_ALERTS_PER_SERIES) {
                if (!removeFirst(seriesAlerts, Alert::isAcknowledged)) {
                    if (!removeFirst(seriesAlerts, alert -> alert.getLevel() == Level.AMBER)) {
                        seriesAlerts.removeFirst();
                    }
                }
            }
        }
    }

    private static boolean removeFirst(Deque<Alert> alerts, Predicate<Alert> test) {
        Iterator<Alert> iter = alerts.iterator();
        while (iter.hasNext()) {
            Alert a = iter.next();
            if (test.test(a)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }
}
