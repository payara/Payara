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

import fish.payara.nucleus.healthcheck.*;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author mertcaliskan
 */
public class GarbageCollectorHealthCheck extends BaseHealthCheck {

    static final String GC_BEANNAME1 = "PS Scavenge";
    static final String GC_BEANNAME2 = "PS MarkSweep";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    private long youngLastCollectionCount;
    private long youngLastCollectionTime;
    private long oldLastCollectionCount;
    private long oldLastCollectionTime;

    public GarbageCollectorHealthCheck(HealthCheckExecutionOptions options) {
        this.options = options;
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();

        List<GarbageCollectorMXBean> gcBeanList = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeanList) {

            if (GC_BEANNAME1.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - youngLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - youngLastCollectionTime;
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.GOOD, getTimeStamp() + "Young GC x " +
                        diffCount + ", " + diffTime + " milliseconds"));

                youngLastCollectionCount = gcBean.getCollectionCount();
                youngLastCollectionTime = gcBean.getCollectionTime();
            }
            else if (GC_BEANNAME2.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - oldLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - oldLastCollectionTime;
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.GOOD, getTimeStamp() + "Old GC x " +
                        diffCount + ", " + diffTime + " milliseconds"));

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

    private String getTimeStamp() {
        Date now = new Date();

        long upTime = ManagementFactory.getRuntimeMXBean().getUptime();
        long upTimeSecs = upTime / 1000;
        long upTimeMillis = upTime % 1000;

        return dateFormat.format(now) + " [" + upTimeSecs + "." + upTimeMillis + "] ";
    }
}
