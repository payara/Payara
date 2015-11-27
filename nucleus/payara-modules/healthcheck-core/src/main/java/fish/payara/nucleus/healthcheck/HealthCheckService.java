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
package fish.payara.nucleus.healthcheck;

import fish.payara.nucleus.healthcheck.configuration.CpuUsageChecker;
import fish.payara.nucleus.healthcheck.configuration.GarbageCollectorChecker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HeapMemoryUsageChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import fish.payara.nucleus.healthcheck.preliminary.CpuUsageHealthCheck;
import fish.payara.nucleus.healthcheck.preliminary.GarbageCollectorHealthCheck;
import fish.payara.nucleus.healthcheck.preliminary.HeapMemoryUsageHealthCheck;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
@Service(name = "healthcheck-core")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener {

    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getCanonicalName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HealthCheckServiceConfiguration configuration;

    @Inject
    private Events events;

    private ScheduledExecutorService executor;

    private final Map<String, HealthCheckTask> registeredTasks = new HashMap<String, HealthCheckTask>(5);

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            executor.shutdownNow();
        }
        else if (event.is(EventTypes.SERVER_READY)) {
            for (HealthCheckTask registeredTask : registeredTasks.values()) {
                logger.info("Scheduling Health Check " + registeredTask.getName());
                executor.scheduleAtFixedRate(registeredTask, 0,
                        registeredTask.getCheck().getOptions().getTime(),
                        registeredTask.getCheck().getOptions().getUnit());
            }
        }
    }

    public void setCheckEnabled(String name, boolean state) {
        HealthCheckTask task = registeredTasks.get(name);
        if (task != null) {
            task.setEnabled(state);
        }
    }

    public void registerCheck(String name, BaseHealthCheck check) {
        registeredTasks.put(name, new HealthCheckTask(name, check));
    }

    @PostConstruct
    void postConstruct() {
        executor = Executors.newScheduledThreadPool(3);
        events.register(this);
        logger.info("Payara Health Check Service Started");

        if (configuration.getCheckEnabled()) {
            GarbageCollectorChecker garbageCollectorChecker = configuration.getCheckerByType(GarbageCollectorChecker.class);
            CpuUsageChecker cpuUsageChecker = configuration.getCheckerByType(CpuUsageChecker.class);
            HeapMemoryUsageChecker heapMemoryUsageChecker = configuration.getCheckerByType(HeapMemoryUsageChecker.class);

            if (garbageCollectorChecker != null &&
                    Boolean.valueOf(garbageCollectorChecker.getEnabled())) {
                registerCheck(garbageCollectorChecker.getName(),
                        new GarbageCollectorHealthCheck(
                                new HealthCheckExecutionOptions(garbageCollectorChecker.getTime(),
                                        asTimeUnit(garbageCollectorChecker.getUnit()))));
            }
            if (cpuUsageChecker != null &&
                    Boolean.valueOf(cpuUsageChecker.getEnabled())) {
                registerCheck(cpuUsageChecker.getName(),
                        new CpuUsageHealthCheck(
                                new HealthCheckExecutionOptions(cpuUsageChecker.getTime(),
                                        asTimeUnit(cpuUsageChecker.getUnit()))));

            }
            if (heapMemoryUsageChecker != null &&
                    Boolean.valueOf(heapMemoryUsageChecker.getEnabled())) {
                registerCheck(heapMemoryUsageChecker.getName(),
                        new HeapMemoryUsageHealthCheck(
                                new HealthCheckExecutionOptions(heapMemoryUsageChecker.getTime(),
                                        asTimeUnit(heapMemoryUsageChecker.getUnit()))));
            }
        }
    }

    private TimeUnit asTimeUnit(String unit) {
        return TimeUnit.valueOf(unit);
    }
}