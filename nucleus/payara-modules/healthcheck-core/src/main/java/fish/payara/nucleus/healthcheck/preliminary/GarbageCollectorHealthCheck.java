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
import fish.payara.nucleus.healthcheck.configuration.GarbageCollectorChecker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-gc")
@RunLevel(StartupRunLevel.VAL)
public class GarbageCollectorHealthCheck extends BaseHealthCheck {

    static final String GC_BEANNAME1 = "PS Scavenge";
    static final String GC_BEANNAME2 = "PS MarkSweep";

    private long youngLastCollectionCount;
    private long youngLastCollectionTime;
    private long oldLastCollectionCount;
    private long oldLastCollectionTime;

    @Inject
    protected HealthCheckService healthCheckService;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HealthCheckServiceConfiguration configuration;

    @PostConstruct
    void postConstruct() {
        super.postConstruct(configuration.getCheckerByType(GarbageCollectorChecker.class), this);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();

        List<GarbageCollectorMXBean> gcBeanList = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeanList) {

            if (GC_BEANNAME1.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - youngLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - youngLastCollectionTime;
                
                if (diffTime > 0) {
                    result.add(new HealthCheckResultEntry(decideOnStatusWithDuration(diffTime),
                            diffCount + " times Young GC after " + prettyPrintDuration(diffTime)));

                    youngLastCollectionCount = gcBean.getCollectionCount();
                    youngLastCollectionTime = gcBean.getCollectionTime();
                }
            }
            else if (GC_BEANNAME2.equals(gcBean.getName())) {
                long diffCount = gcBean.getCollectionCount() - oldLastCollectionCount;
                long diffTime = gcBean.getCollectionTime() - oldLastCollectionTime;

                if (diffTime > 0) {
                    result.add(new HealthCheckResultEntry(decideOnStatusWithDuration(diffTime),
                            diffCount + " times Old GC after " + prettyPrintDuration(diffTime)));

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

    @Override
    protected HealthCheckService getService() {
        return healthCheckService;
    }
}
