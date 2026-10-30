/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jmx.monitoring;

import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.admin.amx.config.AMXConfiguration;
import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotificationFactory;
import fish.payara.jmx.monitoring.configuration.MonitoredAttribute;
import fish.payara.jmx.monitoring.configuration.MonitoringServiceConfiguration;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import java.beans.PropertyVetoException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

/**
 * Service which monitors a set of MBeans and their attributes while logging the
 * data as a series of key-value pairs.
 *
 * @since 4.1.1.163
 * @author savage
 */
@Service(name = "payara-monitoring")
@RunLevel(StartupRunLevel.VAL)
public class JMXMonitoringService implements EventListener {

    private final static LocalStringManagerImpl strings = new LocalStringManagerImpl(JMXMonitoringService.class);

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
    private Topic<PayaraNotification> notificationEventBus;

    @Inject
    private PayaraNotificationFactory notificationFactory;
    
    @Inject
    PayaraExecutorService executor;

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private JMXMonitoringFormatter formatter;
    private boolean enabled;
    private int amxBootDelay = 10;
    private long monitoringDelay = amxBootDelay + 15;
    private final Set<String> enabledNotifiers = new LinkedHashSet<>();
    private ScheduledFuture<?> monitoringFuture;

    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
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
     * Schedules the AMXBoot class to execute if AMX
     * is enabled. Schedules the JMXMonitoringFormatter to execute at a
     * specified fixed rate if enabled in the configuration.
     */
    public void bootstrapMonitoringService() {
        if (configuration != null && configuration.getEnabled().equalsIgnoreCase("true")) {
            shutdownMonitoringService();//To make sure that there aren't multiple monitoring services running

            final MBeanServer server = getPlatformMBeanServer();

            formatter = new JMXMonitoringFormatter(server, buildJobs(), this, notificationEventBus, notificationFactory, enabledNotifiers);

            Logger.getLogger(JMXMonitoringService.class.getName()).log(Level.INFO, "JMX Monitoring Service will startup");

            if (Boolean.valueOf(configuration.getAmx())) {
                AMXConfiguration amxConfig = habitat.getService(AMXConfiguration.class);
                try {
                    amxConfig.setEnabled(String.valueOf(configuration.getAmx()));
                    configuration.setAmx(null);
                } catch (PropertyVetoException ex) {
                    Logger.getLogger(JMXMonitoringService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            monitoringFuture = executor.scheduleAtFixedRate(formatter, monitoringDelay * 1000,
                    TimeUnit.MILLISECONDS.convert(Long.valueOf(configuration.getLogFrequency()),
                            TimeUnit.valueOf(configuration.getLogFrequencyUnit())),
                    TimeUnit.MILLISECONDS);

            bootstrapNotifierList();
        }
    }

    /**
     * Starts notifiers that are enabled with the monitoring service
     *
     * @since 4.1.2.174
     */
    public void bootstrapNotifierList() {
        enabledNotifiers.clear();
        if (configuration.getNotifierList() != null) {
            configuration.getNotifierList().forEach(enabledNotifiers::add);
        }
    }

    /**
     * Returns all notifiers configured with the monitoring service
     *
     * @since 4.1.2.174
     * @return
     */
    public Set<String> getEnabledNotifiers() {
        return enabledNotifiers;
    }

    /**
     * Shuts the scheduler down.
     */
    private void shutdownMonitoringService() {
        if (monitoringFuture != null) {
            Logger.getLogger(JMXMonitoringService.class.getName()).log(Level.INFO, "JMX Monitoring Service will shutdown");
            monitoringFuture.cancel(false);
            monitoringFuture = null;
        }
    }

    /**
     * Sets the service to be enabled/disabled.
     *
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
    private List<JMXMonitoringJob> buildJobs() {
        List<JMXMonitoringJob> jobs = new LinkedList<>();

        for (MonitoredAttribute mbean : configuration.getMonitoredAttributes()) {
            boolean exists = false;

            for (JMXMonitoringJob job : jobs) {
                if (job.getMBean().getCanonicalKeyPropertyListString()
                        .equals(mbean.getObjectName())) {
                    job.addAttribute(mbean.getAttributeName());
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                ObjectName name;
                List<String> list;
                try {
                    name = new ObjectName(mbean.getObjectName());
                    list = new LinkedList<>();
                    list.add(mbean.getAttributeName());
                    jobs.add(new JMXMonitoringJob(name, list));
                } catch (MalformedObjectNameException ex) {
                    Logger.getLogger(JMXMonitoringService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return jobs;
    }

    public LocalStringManagerImpl getLocalStringManager() {
        return strings;
    }
}
