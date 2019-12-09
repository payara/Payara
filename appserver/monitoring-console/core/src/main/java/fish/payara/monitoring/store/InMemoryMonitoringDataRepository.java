/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.monitoring.store;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.sun.enterprise.config.serverbeans.Config;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.logging.Level;

/**
 * A simple in-memory store for a fixed size sliding window for each {@link Series}.
 * 
 * 
 * <h3>Consistency Remarks</h3>
 * 
 * The store uses two maps working like a doubled buffered image. While collection writes to the {@link #secondsWrite}
 * map request are served from the {@link #secondsRead} map. This makes sure that a consistent dataset across all series
 * can be used to create a consistent visualisation that isn't half updated while the response is composed. However
 * this requires that callers are provided with a method that returns all the {@link SeriesDataset}s they need in a
 * single method invocation. Making multiple calls to this stores methods does not guarantee a consistent dataset across
 * all series since the {@link #swapLocalBuffer()} can happen inbetween method calls.
 * 
 * @author Jan Bernitt
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class InMemoryMonitoringDataRepository extends ConfigListeningService implements MonitoringDataRepository {

    /**
     * The topic name used to share data of instances with the DAS.
     */
    private static final String MONITORING_DATA_TOPIC_NAME = "payara-monitoring-data";
    private final Set<String> sourcesFailingBefore = ConcurrentHashMap.newKeySet();
    @Inject
    private ServiceLocator serviceLocator;
    @Inject
    private ServerEnvironment serverEnv;
    @Inject
    private PayaraExecutorService executor;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;
    @Inject
    private HazelcastCore hazelcastCore;

    private boolean isDas;
    private String instanceName;
    private ITopic<SeriesDatasetsSnapshot> exchange;

    private volatile Map<Series, SeriesDataset> secondsWrite = new ConcurrentHashMap<>();
    private volatile Map<Series, SeriesDataset> secondsRead = new ConcurrentHashMap<>();
    private final Map<Series, SeriesDataset[]> remoteInstanceDatasets = new ConcurrentHashMap<>();
    private final Set<String> instances = ConcurrentHashMap.newKeySet();
    private final JobHandle dataCollectionJob = new JobHandle("monitoring data collection");
    private long collectedSecond;
    private int estimatedNumberOfSeries = 50;

    @PostConstruct
    public void init() {
        isDas = serverEnv.isDas();
        if (hazelcastCore.isEnabled()) {
            HazelcastInstance hz = hazelcastCore.getInstance();
            instanceName = hz.getCluster().getLocalMember().getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
            exchange = hz.getTopic(InMemoryMonitoringDataRepository.MONITORING_DATA_TOPIC_NAME);
        } else {
            instanceName = "server";
        }
        instances.add(instanceName);
        if (isDas && exchange != null) {
            MessageListener<SeriesDatasetsSnapshot> subscriber = this::addRemoteDatasets;
            exchange.addMessageListener(subscriber);
        }
        changedConfig(parseBoolean(serverConfig.getMonitoringService().getMonitoringEnabled()));
    }

    @Override
    void changedConfig(boolean enabled) {
        if (!enabled) {
            dataCollectionJob.stop();
        } else {
            LOGGER.info("Starting monitoring data collection for " + instanceName);
            dataCollectionJob.start(executor, 1, SECONDS, isDas ? this::collectSourcesToMemory : this::collectSourcesToPublish);
        }
    }

    @Override
    public Set<String> instances() {
        return instances;
    }

    public void addRemoteDatasets(Message<SeriesDatasetsSnapshot> message) {
        String instance = message.getPublishingMember().getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
        instances.add(instance);
        SeriesDatasetsSnapshot snapshot = message.getMessageObject();
        long time = snapshot.time;
        for (int i = 0; i < snapshot.numberOfSeries; i++) {
            Series series = null;
            try {
                series = new Series(snapshot.series[i]);
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.FINEST, "Failed to add remote series: " + snapshot.series[i], ex);
            }
            if (series != null) {
                long value = snapshot.values[i];
                remoteInstanceDatasets.compute(series, (key, seriesByInstance) -> addRemotePoint(seriesByInstance, instance, key, time, value));
            }
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
        if (exchange != null) {
            tick();
            SeriesDatasetsSnapshot msg = new SeriesDatasetsSnapshot(collectedSecond, estimatedNumberOfSeries);
            collectAll(new SinkDataCollector(msg));
            estimatedNumberOfSeries = msg.numberOfSeries;
            exchange.publish(msg);
        }
    }

    private void collectAll(MonitoringDataCollector collector) {
        for (Entry<Series, SeriesDataset> e : secondsRead.entrySet()) {
            secondsWrite.put(e.getKey(), e.getValue());
        }
        List<MonitoringDataSource> sources = serviceLocator.getAllServices(MonitoringDataSource.class);
        long collectionStart = System.currentTimeMillis();
        int collectedSources = 0;
        int failedSources = 0;
        for (MonitoringDataSource source : sources) {
            String sourceId = source.getClass().getSimpleName(); // for now this is the ID, we might want to replace that later
            try {
                collectedSources++;
                source.collect(collector);
                sourcesFailingBefore.remove(sourceId);
            } catch (RuntimeException e) {
                if (!sourcesFailingBefore.contains(sourceId)) {
                    // only long once unless being successful again
                    LOGGER.log(Level.FINE, "Error collecting metrics", e);
                }
                failedSources++;
                sourcesFailingBefore.add(sourceId);
            }
        }
        long estimatedTotalBytesMemory = 0L;
        for (SeriesDataset set : secondsWrite.values()) {
            estimatedTotalBytesMemory += set.estimatedBytesMemory();
        }
        int seriesCount = secondsWrite.size();
        collector.in("mc")
            .collect("CollectionDuration", System.currentTimeMillis() - collectionStart)
            .collectNonZero("SeriesCount", seriesCount)
            .collectNonZero("TotalBytesMemory", estimatedTotalBytesMemory)
            .collectNonZero("AverageBytesMemoryPerSeries", seriesCount == 0 ? 0L : estimatedTotalBytesMemory / seriesCount)
            .collect("SourcesCount", collectedSources)
            .collect("FailedCollectionCount", failedSources);
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
        Series series = null;
        try {
            series = new Series(key.toString());
        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, "Failed to add local series: " + key, ex);
            return;
        }
        secondsWrite.compute(series, (s, dataset) -> dataset == null 
                ?  emptySet(s).add(collectedSecond, value) 
                : dataset.add(collectedSecond, value));
    }

    private SeriesDataset emptySet(Series series) {
        return new EmptyDataset(instanceName, series, 60);
    }

    @Override
    public List<SeriesDataset> selectSeries(Series series, String... instances) {
        if (!isDas) {
            return emptyList();
        }
        Set<String> instanceFilter = instances == null || instances.length == 0 
                ? this.instances
                : new HashSet<>(asList(instances));
        List<SeriesDataset> res = new ArrayList<>(instanceFilter.size());
        selectSeries(res, Collections.singleton(series), instanceFilter);
        return res;
    }

    private void selectSeries(List<SeriesDataset> res, Set<Series> seriesSet, Set<String> instanceFilter) {
        for (Series series : seriesSet) {
            if (series.isPattern()) {
                selectSeries(res, seriesMatchingPattern(series), instanceFilter);
            } else {
                SeriesDataset localSet = secondsRead.get(series);
                SeriesDataset[] remoteSets = remoteInstanceDatasets.get(series);

                if (localSet != null && isRelevantSet(localSet, instanceFilter)) {
                    res.add(localSet);
                }
                if (remoteSets != null && remoteSets.length > 0) {
                    for (SeriesDataset remoteSet : remoteSets) {
                        if (isRelevantSet(remoteSet, instanceFilter)) {
                            res.add(remoteSet);
                        }
                    }
                }
            }
        }
    }

    private static boolean isRelevantSet(SeriesDataset set, Set<String> instanceFilter) {
        return instanceFilter.contains(set.getInstance());
    }

    private Set<Series> seriesMatchingPattern(Series pattern) {
        Set<Series> matches = new HashSet<>();
        for (Series candidate : secondsRead.keySet()) {
            if (pattern.matches(candidate)) {
                matches.add(candidate);
            }
        }
        for (Series candidate : remoteInstanceDatasets.keySet()) {
            if (pattern.matches(candidate)) {
                matches.add(candidate);
            }
        }
        return matches;
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

