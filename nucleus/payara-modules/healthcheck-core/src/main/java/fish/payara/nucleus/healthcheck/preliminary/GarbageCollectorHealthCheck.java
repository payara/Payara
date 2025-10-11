/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.MonitoringWatchCollector;
import fish.payara.monitoring.collect.MonitoringWatchSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.*;
import fish.payara.nucleus.healthcheck.configuration.GarbageCollectorChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.PostConstruct;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fish.payara.internal.notification.TimeUtil.prettyPrintDuration;

/**
 * -XX:+UseSerialGC                           --> <YOUNG_COPY, OLD_MARK_SWEEP_COMPACT>
 * -XX:+UseG1GC                               --> <YOUNG_G1GC, OLD_G1GC>
 * -XX:+UseParallelGC                         --> <YOUNG_PS_SCAVENGE, OLD_PS_MARKSWEEP>
 * -XX:+UseParNewGC                           --> <YOUNG_PARNEW, OLD_MARK_SWEEP_COMPACT>
 * -XX:+UseConcMarkSweepGC -XX:+UseParNewGC   --> <YOUNG_PARNEW, OLD_CONCURRENTMARKSWEEP>
 * -XX:+UseConcMarkSweepGC -XX:-UseParNewGC   --> <YOUNG_COPY, OLD_CONCURRENTMARKSWEEP>
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-gc")
@RunLevel(StartupRunLevel.VAL)
public class GarbageCollectorHealthCheck
        extends BaseThresholdHealthCheck<HealthCheckWithThresholdExecutionOptions, GarbageCollectorChecker>
        implements MonitoringDataSource, MonitoringWatchSource {

    private final GcUsage youngHealthCheck = new GcUsage();
    private final GcUsage oldHealthCheck = new GcUsage();
    private final Map<String, GcUsage> collect = new ConcurrentHashMap<>();

    @PostConstruct
    void postConstruct() {
        postConstruct(this, GarbageCollectorChecker.class);
    }

    @Override
    public HealthCheckWithThresholdExecutionOptions constructOptions(GarbageCollectorChecker checker) {
        return super.constructThresholdOptions(checker);
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.garbageCollector";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        HealthCheckResult result = new HealthCheckResult();
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (isYoungGenerationGC(gcBean)) {
                result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(youngHealthCheck.percentage(gcBean)),
                        youngHealthCheck.getNumberOfGcs() + " times Young GC (" + gcBean.getName() + ") collecting for "
                                + prettyPrintDuration(youngHealthCheck.getTimeSpendDoingGc())));
            } else if (isOldGenerationGC(gcBean)) {
                result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(oldHealthCheck.percentage(gcBean)),
                        oldHealthCheck.getNumberOfGcs() + " times Old GC (" + gcBean.getName() + ") after "
                                + prettyPrintDuration(oldHealthCheck.getTimeSpendDoingGc())));
            } else {
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "Could not identify " +
                        "GarbageCollectorMXBean with name: " + gcBean.getName()));
            }
        }

        return result;
    }

    @Override
    public void collect(MonitoringWatchCollector collector) {
        collectUsage(collector, "ns:health TotalGcPercentage", "GC Percentage", 10, true);
    }

    @Override
    @MonitoringData(ns = "health", intervalSeconds = 4)
    public void collect(MonitoringDataCollector collector) {
        if (options == null || !options.isEnabled()) {
            return;
        }
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            GcUsage usage = collect.computeIfAbsent(gcBean.getName(), key -> new GcUsage());
            double percentage = usage.percentage(gcBean); // update
            if (isYoungGenerationGC(gcBean) ) {
                collectGcUage(collector, "Young", percentage, usage.getNumberOfGcs(), usage.getTimeSpendDoingGc());
            } else if (isOldGenerationGC(gcBean)) {
                collectGcUage(collector, "Old", percentage, usage.getNumberOfGcs(), usage.getTimeSpendDoingGc());
            }
        }
        long timeSpendDoingGc = 0;
        long timePassed = 0;
        long numberOfGcs = 0;
        for (GcUsage u : collect.values()) {
            timePassed = Math.max(timePassed, u.getTimePassed());
            timeSpendDoingGc += u.getTimeSpendDoingGc();
            numberOfGcs += u.getNumberOfGcs();
        }
        double usage = timePassed == 0 ? 0d : 100d * timeSpendDoingGc / timePassed;
        collectGcUage(collector, "Total", usage, numberOfGcs, timeSpendDoingGc);
    }

    private static void collectGcUage(MonitoringDataCollector collector, String label, double usage, long numberOfGcs, long timeSpendDoingGc) {
        collector
            .collect(label + "GcPercentage", (long) usage)
            .collect(label + "GcCount", numberOfGcs)
            .collect(label + "GcDuration", timeSpendDoingGc);
    }

    private static final class GcUsage {

        private volatile long timeLastChecked;
        private volatile long lastCollectionCount;
        private volatile long lastCollectionTime;
        private volatile long timeSpendDoingGc;
        private volatile long numberOfGcs;
        private volatile long timePassed;

        GcUsage() {
            // make visible
        }

        public double percentage(GarbageCollectorMXBean gcBean) {
            long collectionCount = gcBean.getCollectionCount();
            numberOfGcs = collectionCount - lastCollectionCount;
            lastCollectionCount = collectionCount;
            long collectionTime = gcBean.getCollectionTime();
            timeSpendDoingGc = collectionTime - lastCollectionTime;
            lastCollectionTime = collectionTime;
            long now = System.currentTimeMillis();
            timePassed = now - timeLastChecked;
            timeLastChecked = now;
            return numberOfGcs == 0 || lastCollectionCount == 0 ? 0d : 100d * timeSpendDoingGc / timePassed;
        }

        long getNumberOfGcs() {
            return numberOfGcs;
        }

        long getTimeSpendDoingGc() {
            return timeSpendDoingGc;
        }

        long getTimePassed() {
            return timePassed;
        }
    }

    private static boolean isOldGenerationGC(GarbageCollectorMXBean gcBean) {
        String name = gcBean.getName();
        return OLD_PS_MARKSWEEP.equals(name) ||
                OLD_G1GC.equals(name) ||
                OLD_MARK_SWEEP_COMPACT.equals(name) ||
                OLD_CONCURRENTMARKSWEEP.equals(name);
    }

    private static boolean isYoungGenerationGC(GarbageCollectorMXBean gcBean) {
        String name = gcBean.getName();
        return YOUNG_PS_SCAVENGE.equals(name) ||
                YOUNG_G1GC.equals(name) ||
                YOUNG_COPY.equals(name) ||
                YOUNG_PARNEW.equals(name);
    }
}