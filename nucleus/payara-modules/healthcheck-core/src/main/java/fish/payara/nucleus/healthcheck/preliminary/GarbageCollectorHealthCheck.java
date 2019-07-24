/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.*;
import fish.payara.nucleus.healthcheck.configuration.GarbageCollectorChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import static fish.payara.nucleus.notification.TimeHelper.prettyPrintDuration;

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
        implements MonitoringDataSource {

    private volatile long youngLastCollectionCount;
    private volatile long youngLastCollectionTime;
    private volatile long oldLastCollectionCount;
    private volatile long oldLastCollectionTime;

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (isReady()) {
            collector.in("health-check").type("checker").entity("GBGC")
                .collect("checksDone", getChecksDone())
                .collectNonZero("checksFailed", getChecksFailed())
                .collectNonZero("youngLastCollectionCount", youngLastCollectionCount)
                .collectNonZero("youngLastCollectionTime", youngLastCollectionTime)
                .collectNonZero("oldLastCollectionCount", oldLastCollectionCount)
                .collectNonZero("oldLastCollectionTime", oldLastCollectionTime);
        }
    }

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

        List<GarbageCollectorMXBean> gcBeanList = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeanList) {

            double percentage = 0;

            if (YOUNG_PS_SCAVENGE.equals(gcBean.getName()) ||
                    YOUNG_G1GC.equals(gcBean.getName()) ||
                    YOUNG_COPY.equals(gcBean.getName()) ||
                    YOUNG_PARNEW.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - youngLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - youngLastCollectionTime;

                if (diffTime > 0 && youngLastCollectionCount > 0) {
                    percentage = ((diffCount) / (youngLastCollectionCount)) * 100;

                    result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(percentage),
                            diffCount + " times Young GC (" + gcBean.getName()  + ") after " + prettyPrintDuration(diffTime)));
                }

                youngLastCollectionCount = gcBean.getCollectionCount();
                youngLastCollectionTime = gcBean.getCollectionTime();
            }
            else if (OLD_PS_MARKSWEEP.equals(gcBean.getName()) ||
                    OLD_G1GC.equals(gcBean.getName()) ||
                    OLD_MARK_SWEEP_COMPACT.equals(gcBean.getName()) ||
                    OLD_CONCURRENTMARKSWEEP.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - oldLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - oldLastCollectionTime;

                if (diffTime > 0 && oldLastCollectionCount > 0) {
                    percentage = ((diffCount) / (oldLastCollectionCount)) * 100;

                    result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(percentage),
                            diffCount + " times Old GC (" + gcBean.getName()  + ") after " + prettyPrintDuration(diffTime)));
                }

                oldLastCollectionCount = gcBean.getCollectionCount();
                oldLastCollectionTime = gcBean.getCollectionTime();
            }
            else {
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "Could not identify " +
                        "GarbageCollectorMXBean with name: " + gcBean.getName()));
            }
        }

        return result;
    }
}