/*
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
package fish.payara.nucleus.healthcheck;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.sun.enterprise.config.serverbeans.Config;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import fish.payara.internal.notification.TimeUtil;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;

/**
 * @author steve
 * @since 4.1.1.161
 */
@Service(name = "healthcheck-core")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener, ConfigListener, MonitoringDataSource {

    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getCanonicalName());

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
    ServerEnvironment env;

    @Inject
    Transactions transactions;

    @Inject
    private HistoricHealthCheckEventStore healthCheckEventStore;

    @Inject 
    private PayaraExecutorService executor;

    private Set<String> enabledNotifiers = new LinkedHashSet<>();
    private Map<String, HealthCheckTask> registeredTasks = new HashMap<>();
    private boolean enabled;
    private boolean historicalTraceEnabled;
    private Integer historicalTraceStoreSize;
    private Long historicalTraceStoreTimeout;
    private ScheduledFuture<?> historicalTraceTask;
    private Set<ScheduledFuture<?>> scheduledCheckers;

    @Override
    public void collect(MonitoringDataCollector rootCollector) {
        MonitoringDataCollector health = rootCollector.in("health");
        for (Entry<String, HealthCheckTask> task : registeredTasks.entrySet()) {
            BaseHealthCheck<?,?> check = task.getValue().getCheck();
            if (check.isReady() && check.getChecksDone() > 0) {
                HealthCheckResultStatus status = check.getMostRecentCumulativeStatus();
                if (status != null) {
                    health.collect(task.getKey(), status.getLevel());
                }
            }
        }
    }


    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapHealthCheck();
        }

        transactions.addListenerForType(HealthCheckServiceConfiguration.class, this);
    }

    public void registerCheck(String name, BaseHealthCheck check) {
        registeredTasks.put(name, new HealthCheckTask(name, check));
    }

    @PostConstruct
    void postConstruct() {
        events.register(this);
        configuration = habitat.getService(HealthCheckServiceConfiguration.class);
        if (configuration != null) {
            if (Boolean.parseBoolean(configuration.getEnabled())) {
                enabled = true;
            }
            historicalTraceEnabled = Boolean.valueOf(configuration.getHistoricalTraceEnabled());
            String historicalTraceStoreSizeConfig = configuration.getHistoricalTraceStoreSize();
            if (historicalTraceStoreSizeConfig != null) {
                this.historicalTraceStoreSize = Integer.parseInt(historicalTraceStoreSizeConfig);

                String historicalTraceStoreTimeLimit = configuration.getHistoricalTraceStoreTimeout();
                if (historicalTraceStoreTimeLimit != null) {
                    this.historicalTraceStoreTimeout = TimeUtil.setStoreTimeLimit(historicalTraceStoreTimeLimit);
                }
            }
        }
    }

    /**
     * Starts the healthcheck service. This will also bootstrap any relevant notifiers.
     */
    public void bootstrapHealthCheck() {
        if (configuration != null) {
            if (enabled) {
                scheduledCheckers = new HashSet<>();
                executeTasks();
                if (historicalTraceEnabled) {
                    healthCheckEventStore.initialize(historicalTraceStoreSize);

                    if (historicalTraceStoreTimeout != null && historicalTraceStoreTimeout > 0) {
                        // if timeout is bigger than 5 minutes execute the cleaner task in 5 minutes periods,
                        // if not use timeout value as period
                        long period = historicalTraceStoreTimeout > TimeUtil.CLEANUP_TASK_FIVE_MIN_PERIOD
                                ? TimeUtil.CLEANUP_TASK_FIVE_MIN_PERIOD : historicalTraceStoreTimeout;
                        historicalTraceTask = executor.scheduleAtFixedRate(
                                new HistoricHealthCheckCleanupTask(historicalTraceStoreTimeout), 0, period, TimeUnit.SECONDS);
                    }
                }
                logger.info("Payara Health Check Service Started.");
            }

            bootstrapNotifierList();
        }
    }

    /**
     * Starts all notifiers that have been enable with the healthcheck service.
     */
    public synchronized void bootstrapNotifierList() {
        enabledNotifiers.clear();
        if (configuration.getNotifierList() != null) {
            configuration.getNotifierList().forEach(enabledNotifiers::add);
        }
    }

    private void executeTasks() {
        for (String registeredTaskKey : registeredTasks.keySet()) {
            HealthCheckTask registeredTask = registeredTasks.get(registeredTaskKey);
            logger.info("Scheduling Health Check for task: " + registeredTask.getName());

            if (registeredTask.getCheck().getOptions().isEnabled()) {
                ScheduledFuture<?> checker = executor.scheduleAtFixedRate(registeredTask, 0,
                        registeredTask.getCheck().getOptions().getTime(),
                        registeredTask.getCheck().getOptions().getUnit());
                if (scheduledCheckers != null) {
                    scheduledCheckers.add(checker);
                }
            }
        }
    }

    /**
     * 
     * @return Whether the healthcheck service is enabled
     */
    public boolean isEnabled(){
        return enabled;
    }
    
    /**
     * Sets whether the healthcheck service is enabled.
     * @param enabled If this is true, then the healcheck service will be started.
     * If it is already enabled then healthcheck will be restarted.
     */
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
    
    /**
     * Restartes the healthcheck service and gets the configuration for it
     */ 
    public void reboot() {
        shutdownHealthCheck();
        if (configuration == null) {
            configuration = server.getConfigBean().getConfig().getExtensionByType(HealthCheckServiceConfiguration.class);
        }
        if (Boolean.valueOf(configuration.getEnabled())) {
            bootstrapHealthCheck();
        }
    }

    /**
     * Gracefully shuts down the healthcheck service
     */
    public void shutdownHealthCheck() {
        Logger.getLogger(HealthCheckService.class.getName()).log(Level.INFO, "Payara Health Check Service is shutdown.");
        
        if (historicalTraceTask != null) {
            historicalTraceTask.cancel(false);
            historicalTraceTask = null;
        }
        
        if (scheduledCheckers != null) {
            for (ScheduledFuture<?> scheduledChecker : scheduledCheckers) {
                scheduledChecker.cancel(false);
            }
            scheduledCheckers.clear();
        }
    }

    public BaseHealthCheck getCheck(String serviceName) {
        HealthCheckTask task = registeredTasks.get(serviceName);
        return task == null ? null : task.getCheck();
    }
    
    /**
     * Gets the current configuration of the healthcheck service
     * @return 
     */
    public HealthCheckServiceConfiguration getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(HealthCheckServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns true if historic healthchecks are stored
     * @return 
     */
    public boolean isHistoricalTraceEnabled() {
        return historicalTraceEnabled;
    }

    /**
     * Sets whether historic healthchecks are stored
     * @param historicalTraceEnabled 
     */
    public void setHistoricalTraceEnabled(boolean historicalTraceEnabled) {
        this.historicalTraceEnabled = historicalTraceEnabled;
    }

    /**
     * Gets the number of healthchecks to be stored.
     * This may be greater than 0 even if {@link isHistoricalTraceEnabled()} returns false
     * as this can be set independently
     * @return 
     */
    public Integer getHistoricalTraceStoreSize() {
        return historicalTraceStoreSize;
    }

    /**
     * Sets the amount of historic healthchecks to store
     * @param historicalTraceStoreSize 
     */
    public void setHistoricalTraceStoreSize(Integer historicalTraceStoreSize) {
        this.historicalTraceStoreSize = historicalTraceStoreSize;
    }

    /**
     * Sets the length in seconds to keep historic healthchecks, ones older than this will be discarded.
     * @param historicalTraceStoreTimeout if this is > 500 then 500 will be used as the limit.
     * @see fish.payara.nucleus.notification.NotificationService
     */
    public void setHistoricalTraceStoreTimeout(long historicalTraceStoreTimeout) {
        this.historicalTraceStoreTimeout = historicalTraceStoreTimeout;
    }

    /**
     * Gets a list of all notifiers enabled the healthcheck service.
     * @return 
     */
    public Set<String> getEnabledNotifiers() {
        return enabledNotifiers;
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (env.isInstance()) {
            isCurrentInstanceMatchTarget = true;
        }
        else {
            for (PropertyChangeEvent pe : events) {
                ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
                while (proxy != null && !(proxy instanceof Config)) {
                    proxy = proxy.getParent();
                }

                if (proxy != null && ((Config) proxy).isDas()) {
                    isCurrentInstanceMatchTarget = true;
                    break;
                }
            }
        }

        if (isCurrentInstanceMatchTarget) {
            return ConfigSupport.sortAndDispatch(events, new Changed() {

                @Override
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {

                    if(changedType.equals(HealthCheckServiceConfiguration.class)) {
                        configuration = (HealthCheckServiceConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }
}
