/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
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
package fish.payara.jmx.monitoring;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import fish.payara.jmx.monitoring.configuration.MonitoringServiceConfiguration;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author savage
 */
@Service(name = "payara-monitoring")
@RunLevel(StartupRunLevel.VAL)
public class MonitoringService implements EventListener {
    
    private static final String PREFIX = "payara-monitoring-service(";

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    MonitoringServiceConfiguration configuration;    

    @Inject 
    private Events events;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private ScheduledExecutorService executor;    
    private MonitoringFormatter formatter;

    @PostConstruct
    public void postConstruct() throws NamingException { 
        events.register(this);
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            executor.shutdownNow();
        }
        else if (event.is(EventTypes.SERVER_READY)) {
            if (configuration != null && configuration.getEnabled()) {
                bootstrapMonitoringService();
            }
        }
    }

    /**
     * Bootstrap process for the monitoring service
     * Creates thread pool and the MonitoringFormatter, scheduling it
     */
    public void bootstrapMonitoringService() {
        if (configuration != null) {
            executor = Executors.newScheduledThreadPool(4, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, PREFIX.concat(Integer.toString
                        (threadNumber.getAndIncrement())).concat(")")
                    );
                }
            });

            MBeanServer server = getPlatformMBeanServer(); 
            formatter = new MonitoringFormatter(server, buildJobs());
            
            if (configuration.getEnabled()) {
                executor.scheduleAtFixedRate(formatter, 5, 
                        configuration.getLogFrequency(), 
                        TimeUnit.SECONDS);
            }

        } 
    }

    /**
     * Shuts the scheduler down
     */
    public void shutdownMonitoringService() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Builds the monitoring jobs from the service configuration
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
                    jobs.add(new MonitoringJob(name,list));
                } catch (MalformedObjectNameException ex) {
                    Logger.getLogger(MonitoringService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return jobs;
    }
}
