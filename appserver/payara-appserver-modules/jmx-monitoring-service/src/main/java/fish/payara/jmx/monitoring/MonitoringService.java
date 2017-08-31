/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jmx.monitoring;

import fish.payara.jmx.monitoring.configuration.MonitoringServiceConfiguration;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptions;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptionsFactoryStore;
import fish.payara.nucleus.notification.log.LogNotifierExecutionOptions;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import java.util.ArrayList;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;

/**
 * Service which monitors a set of MBeans and their attributes while logging the data as a series of key-value pairs.
 *
 * @since 4.1.1.163
 * @author savage
 */
@Service(name = "payara-monitoring")
@RunLevel(StartupRunLevel.VAL)
public class MonitoringService implements EventListener {

    private final String PREFIX = "payara-monitoring-service(";

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    MonitoringServiceConfiguration configuration;

    @Inject
    ServiceLocator habitat;
    
    @Inject
    private Events events;
    
    @Inject
    private NotifierExecutionOptionsFactoryStore executionOptionsFactoryStore;
    
    @Inject
    private NotificationEventFactoryStore eventStore;
    
    @Inject
    private NotificationService notificationService;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private ScheduledExecutorService executor;
    private MonitoringFormatter formatter;
    private boolean enabled;
    private int amxBootDelay = 10;
    private int monitoringDelay = amxBootDelay + 15;

    private List<NotifierExecutionOptions> notifierExecutionOptionsList;
    
    @PostConstruct
    public void postConstruct() throws NamingException {
        events.register(this);
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN) ) {
            shutdownMonitoringService();
        } else if (event.is(EventTypes.SERVER_READY)) {
            if (configuration != null && Boolean.valueOf(configuration.getEnabled())) {
                enabled = Boolean.valueOf(configuration.getEnabled());
                bootstrapMonitoringService();
            }
        }
    }

    /**
     * Bootstraps the monitoring service.
     *  Creates a thread pool for the ScheduledExecutorService.
     *  Schedules the AMXBoot class to execute if AMX is enabled.
     *  Schedules the MonitoringFormatter to execute at a specified fixed rate 
     * if enabled in the configuration.
     */
    public void bootstrapMonitoringService() {
        if (configuration != null) {
            shutdownMonitoringService();//To make sure that there aren't multiple monitoring services running
            
            executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, PREFIX.concat(Integer.toString(threadNumber.getAndIncrement())).concat(")")
                    );
                }
            });

            final MBeanServer server = getPlatformMBeanServer();

            formatter = new MonitoringFormatter(server, buildJobs(), this, eventStore, notificationService);

            Logger.getLogger(MonitoringService.class.getName()).log(Level.INFO, "Monitoring Service will startup");

            if (Boolean.valueOf(configuration.getAmx())) {
                executor.schedule(new AMXBoot(server), amxBootDelay, TimeUnit.SECONDS);
            }

            executor.scheduleAtFixedRate(formatter, monitoringDelay,
                    TimeUnit.SECONDS.convert(Long.valueOf(configuration.getLogFrequency()),
                            TimeUnit.valueOf(configuration.getLogFrequencyUnit())),
                    TimeUnit.SECONDS);
            
            bootstrapNotifierList();
        }
    }
    
    /**
     * Starts notifiers that are enabled with the monitoring service
     * @since 4.1.2.174
     */
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
    
    /**
     * Returns the configuration of all the notifiers configured with the monitoring service
     * @since 4.1.2.174
     * @return 
     */
    public List<NotifierExecutionOptions> getNotifierExecutionOptionsList() {
        return notifierExecutionOptionsList;
    }

    /**
     * Shuts the scheduler down.
     */
    private void shutdownMonitoringService() {
        if (executor != null) {
            Logger.getLogger(MonitoringService.class.getName()).log(Level.INFO, "Monitoring Service will shutdown");
            executor.shutdownNow();
        }
    }

    /**
     * Sets the service to be enabled/disabled.
     * @param enabled If true will reboot the monitoring service
     */
    public void setEnabled(Boolean enabled) {
        amxBootDelay = 0;
        monitoringDelay = amxBootDelay + 5;
        
        if (enabled) {
            this.enabled = enabled;
            bootstrapMonitoringService();
        } else if (this.enabled && !enabled) {
            this.enabled = enabled;
            shutdownMonitoringService();
        }
    }

    /**
     * Builds the monitoring jobs from the service configuration.
     *
     * @return List of built jobs.
     */
    private List<MonitoringJob> buildJobs() {
        List<MonitoringJob> jobs = new LinkedList<>();

        for (Property prop : configuration.getProperty()) {
            boolean exists = false;

            for (MonitoringJob job : jobs) {
                if (job.getMBean().getCanonicalKeyPropertyListString()
                        .equals(prop.getValue())) {
                    job.addAttribute(prop.getName());
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                ObjectName name;
                List<String> list;
                try {
                    name = new ObjectName(prop.getValue());
                    list = new LinkedList<>();
                    list.add(prop.getName());
                    jobs.add(new MonitoringJob(name, list));
                } catch (MalformedObjectNameException ex) {
                    Logger.getLogger(MonitoringService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return jobs;
    }

    /**
     * Runnable class to schedule invocation of the bootAMX operation.
     * 
     * @author savage
     */
    private static class AMXBoot implements Runnable {

        private final String BOOT_AMX_MBEAN_NAME = "amx-support:type=boot-amx";
        private final String BOOT_AMX_OPERATION_NAME = "bootAMX";

        private final MBeanServer server;

        /**
         * Constructor for the AMXBoot class.
         *
         * @param server MBeanServer to invoke the bootAMX operation on.
         */
        public AMXBoot(MBeanServer server) {
            this.server = server;
        }

        /**
         * Class runnable method.
         *  Boots AMX by invoking the bootAMX operation on the boot-amx MBean.
         */
        @Override
        public void run() {
            try {
                ObjectName bootAMXObject = new ObjectName(BOOT_AMX_MBEAN_NAME);
                server.invoke(bootAMXObject, BOOT_AMX_OPERATION_NAME, null, null);
            } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException ex) {
                Logger.getLogger(MonitoringService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
