/*
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptions;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptionsFactoryStore;
import fish.payara.nucleus.notification.log.LogNotifier;
import fish.payara.nucleus.notification.log.LogNotifierExecutionOptions;
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
import org.jvnet.hk2.config.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
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

/**
 * @author steve
 */
@Service(name = "healthcheck-core")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener, ConfigListener {

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
    ServerEnvironment env;

    @Inject
    Transactions transactions;

    @Inject
    private NotifierExecutionOptionsFactoryStore executionOptionsFactoryStore;

    @Inject
    private HistoricHealthCheckEventStore healthCheckEventStore;

    private List<NotifierExecutionOptions> notifierExecutionOptionsList;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private ScheduledExecutorService executor;
    private final Map<String, HealthCheckTask> registeredTasks = new HashMap<String, HealthCheckTask>();
    private boolean enabled;
    private boolean historicalTraceEnabled;
    private Integer historicalTraceStoreSize;

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN) && executor != null) {
            executor.shutdownNow();
        }
        else if (event.is(EventTypes.SERVER_READY)) {
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
            if (configuration.getNotifierList() != null && configuration.getNotifierList().isEmpty()) {
                try {
                    ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                        @Override
                        public Object run(final HealthCheckServiceConfiguration configurationProxy)
                                throws PropertyVetoException, TransactionFailure {
                            LogNotifier notifier = configurationProxy.createChild(LogNotifier.class);
                            configurationProxy.getNotifierList().add(notifier);
                            return configurationProxy;
                        }
                    }, configuration);
                } catch (TransactionFailure e) {
                    logger.log(Level.SEVERE, "Error occurred while setting initial log notifier", e);
                }
            }

            if (Boolean.parseBoolean(configuration.getEnabled())) {
                enabled = true;
            }
            historicalTraceEnabled = Boolean.valueOf(configuration.getHistoricalTraceEnabled());
            String historicalTraceStoreSizeConfig = configuration.getHistoricalTraceStoreSize();
            if (historicalTraceStoreSizeConfig != null) {
                this.historicalTraceStoreSize = Integer.parseInt(historicalTraceStoreSizeConfig);
            }
        }
    }

    public void bootstrapHealthCheck() {
        if (configuration != null) {
            executor = Executors.newScheduledThreadPool(configuration.getCheckerList().size(),  new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return new Thread(r, PREFIX + threadNumber.getAndIncrement());
                }
            });
            if (enabled) {
                executeTasks();
                if (historicalTraceEnabled) {
                    healthCheckEventStore.initialize(historicalTraceStoreSize);
                }
                logger.info("Payara Health Check Service Started.");
            }

            bootstrapNotifierList();
        }
    }

    public void bootstrapNotifierList() {
        notifierExecutionOptionsList = new ArrayList<>();
        if (configuration.getNotifierList() != null) {
            for (Notifier notifier : configuration.getNotifierList()) {
                ConfigView view = ConfigSupport.getImpl(notifier);
                NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);
                notifierExecutionOptionsList.add(executionOptionsFactoryStore.get(annotation.type()).build(notifier));
            }
        }
        if (notifierExecutionOptionsList.isEmpty()) {
            // Add logging execution options by default
            LogNotifierExecutionOptions logNotifierExecutionOptions = new LogNotifierExecutionOptions();
            logNotifierExecutionOptions.setEnabled(true);
            notifierExecutionOptionsList.add(logNotifierExecutionOptions);
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

    /**
     * 
     * @return Whether the healthcheck service is enabled
     */
    public boolean isEnabled(){
        return enabled;
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

    public boolean isHistoricalTraceEnabled() {
        return historicalTraceEnabled;
    }

    public void setHistoricalTraceEnabled(boolean historicalTraceEnabled) {
        this.historicalTraceEnabled = historicalTraceEnabled;
    }

    public Integer getHistoricalTraceStoreSize() {
        return historicalTraceStoreSize;
    }

    public void setHistoricalTraceStoreSize(Integer historicalTraceStoreSize) {
        this.historicalTraceStoreSize = historicalTraceStoreSize;
    }

    public List<NotifierExecutionOptions> getNotifierExecutionOptionsList() {
        return notifierExecutionOptionsList;
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