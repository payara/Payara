/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.configuration.GarbageCollectorChecker;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * @author mertcaliskan
 *
 * -XX:+UseSerialGC                           --> <YOUNG_COPY, OLD_MARK_SWEEP_COMPACT>
 * -XX:+UseG1GC                               --> <YOUNG_G1GC, OLD_G1GC>
 * -XX:+UseParallelGC                         --> <YOUNG_PS_SCAVENGE, OLD_PS_MARKSWEEP>
 * -XX:+UseParNewGC                           --> <YOUNG_PARNEW, OLD_MARK_SWEEP_COMPACT>
 * -XX:+UseConcMarkSweepGC -XX:+UseParNewGC   --> <YOUNG_PARNEW, OLD_CONCURRENTMARKSWEEP>
 * -XX:+UseConcMarkSweepGC -XX:-UseParNewGC   --> <YOUNG_COPY, OLD_CONCURRENTMARKSWEEP>
 */
@Service(name = "healthcheck-gc")
@RunLevel(StartupRunLevel.VAL)
public class GarbageCollectorHealthCheck extends BaseHealthCheck {

    private long youngLastCollectionCount;
    private long youngLastCollectionTime;
    private long oldLastCollectionCount;
    private long oldLastCollectionTime;

    @PostConstruct
    void postConstruct() {
        postConstruct(this, GarbageCollectorChecker.class);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();

        List<GarbageCollectorMXBean> gcBeanList = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeanList) {

            if (YOUNG_PS_SCAVENGE.equals(gcBean.getName()) ||
                    YOUNG_G1GC.equals(gcBean.getName()) ||
                    YOUNG_COPY.equals(gcBean.getName()) ||
                    YOUNG_PARNEW.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - youngLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - youngLastCollectionTime;
                
                if (diffTime > 0) {
                    result.add(new HealthCheckResultEntry(decideOnStatusWithDuration(diffTime),
                            diffCount + " times Young GC (" + gcBean.getName()  + ") after " + prettyPrintDuration(diffTime)));

                    youngLastCollectionCount = gcBean.getCollectionCount();
                    youngLastCollectionTime = gcBean.getCollectionTime();
                }
            }
            else if (OLD_PS_MARKSWEEP.equals(gcBean.getName()) ||
                    OLD_G1GC.equals(gcBean.getName()) ||
                    OLD_MARK_SWEEP_COMPACT.equals(gcBean.getName()) ||
                    OLD_CONCURRENTMARKSWEEP.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - oldLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - oldLastCollectionTime;

                if (diffTime > 0) {
                    result.add(new HealthCheckResultEntry(decideOnStatusWithDuration(diffTime),
                            diffCount + " times Old GC (\" + gcBean.getName()  + \") after " + prettyPrintDuration(diffTime)));

                    oldLastCollectionCount = gcBean.getCollectionCount();
                    oldLastCollectionTime = gcBean.getCollectionTime();
                }
            }
            else {
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "Could not identify " +
                        "GarbageCollectorMXBean with name: " + gcBean.getName()));
            }
        }

        return result;
    }
}
