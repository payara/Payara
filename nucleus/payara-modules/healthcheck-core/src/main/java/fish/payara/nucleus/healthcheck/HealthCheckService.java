/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
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

import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.requesttracing.domain.execoptions.LogNotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptionsFactoryStore;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;

/**
 * @author steve
 */
@Service(name = "healthcheck-core")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener {

    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getCanonicalName());
    private static final String PREFIX = "healthcheck-service-";

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    HealthCheckServiceConfiguration configuration;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Events events;
    
    @Inject
    ServerContext server;

    @Inject
    private NotifierExecutionOptionsFactoryStore executionOptionsFactoryStore;

    private List<NotifierExecutionOptions> notifierExecutionOptionsList = new ArrayList<>();

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private ScheduledExecutorService executor;
    private final Map<String, HealthCheckTask> registeredTasks = new HashMap<String, HealthCheckTask>();
    private boolean enabled;

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            executor.shutdownNow();
        }
        else if (event.is(EventTypes.SERVER_READY)) {
            bootstrapHealthCheck();
        }
    }

    public void registerCheck(String name, BaseHealthCheck check) {
        registeredTasks.put(name, new HealthCheckTask(name, check));
    }

    @PostConstruct
    void postConstruct() {
        if (configuration != null && Boolean.parseBoolean(configuration.getEnabled())) {
            enabled = true;
            bootstrapHealthCheck();
        }
    }

    public void bootstrapHealthCheck() {
        HealthCheckServiceConfiguration configuration = habitat.getService(HealthCheckServiceConfiguration.class);
        if (configuration != null) {
            executor = Executors.newScheduledThreadPool(configuration.getCheckerList().size(),  new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return new Thread(r, PREFIX + threadNumber.getAndIncrement());
                }
            });

            events.register(this);
            if (enabled) {
                logger.info("Payara Health Check Service Started.");
                executeTasks();
            }

            for (Notifier notifier : configuration.getNotifierList()) {
                ConfigView view = ConfigSupport.getImpl(notifier);
                NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);
                notifierExecutionOptionsList.add(executionOptionsFactoryStore.get(annotation.type()).build(notifier));
            }
            if (notifierExecutionOptionsList.isEmpty()) {
                // Add logging execution options by default
                LogNotifierExecutionOptions logNotifierExecutionOptions = new LogNotifierExecutionOptions();
                logNotifierExecutionOptions.setEnabled(true);
                notifierExecutionOptionsList.add(logNotifierExecutionOptions);
            }
        }
    }

    private void executeTasks() {
        for (HealthCheckTask registeredTask : registeredTasks.values()) {
            logger.info("Scheduling Health Check for task: " + registeredTask.getName());
            if (registeredTask.getCheck().getOptions().isEnabled()) {
                executor.scheduleAtFixedRate(registeredTask, 0,
                        registeredTask.getCheck().getOptions().getTime(),
                        registeredTask.getCheck().getOptions().getUnit());
            }
        }
    }

    public void setEnabled(Boolean enabled) {
        if (this.enabled && !enabled) {
            this.enabled = false;
            shutdownHealthCheck();
        } else if (!this.enabled && enabled) {
            this.enabled = true;
            bootstrapHealthCheck();
        } else if (this.enabled && enabled) {
            shutdownHealthCheck();
            bootstrapHealthCheck();
        }
    }
    
    public void reboot() {
        shutdownHealthCheck();
        if (configuration == null) {
            configuration = server.getConfigBean().getConfig().getExtensionByType(HealthCheckServiceConfiguration.class);
        }
        if (Boolean.valueOf(configuration.getEnabled())) {
            bootstrapHealthCheck();
        }
    }

    public void shutdownHealthCheck() {
        if (executor != null) {
            executor.shutdown();
            Logger.getLogger(HealthCheckService.class.getName()).log(Level.INFO, "Payara Health Check Service is shutdown.");
        }
    }

    public BaseHealthCheck getCheck(String serviceName) {
        return registeredTasks.get(serviceName).getCheck();
    }
    
    public HealthCheckServiceConfiguration getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(HealthCheckServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<NotifierExecutionOptions> getNotifierExecutionOptionsList() {
        return notifierExecutionOptionsList;
    }
}