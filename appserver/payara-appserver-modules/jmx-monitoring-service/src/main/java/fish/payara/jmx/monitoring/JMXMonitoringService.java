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

import fish.payara.jmx.monitoring.configuration.JMXMonitoringServiceConfiguration;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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

/**
 *
 * @author savage
 */
@Service(name = "payara-monitoring")
@RunLevel(StartupRunLevel.VAL)
public class JMXMonitoringService implements EventListener {
    
    private static final String PREFIX = "payara-monitoring-service(";

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    JMXMonitoringServiceConfiguration configuration;    

    @Inject
    private ServiceLocator habitat;

    @Inject 
    private Events events;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private ScheduledExecutorService executor;    
    private MonitoringFormatter formatter;
    private boolean enabled;

    @PostConstruct
    public void postConstruct() throws NamingException { 
        if (configuration != null && configuration.getEnabled()) {
            bootstrapMonitoringService();
        } else {
            for (int i=0;i<10;i++) {
                System.out.println("NULL CONFIG");
            }
        }

    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            executor.shutdownNow();
        }
        else if (event.is(EventTypes.SERVER_READY)) {
            bootstrapMonitoringService();
        }
    }

    public void bootstrapMonitoringService() {
        JMXMonitoringServiceConfiguration configuration = habitat.getService(JMXMonitoringServiceConfiguration.class);
        if (configuration != null) {
        // <<<< TEST BLOCK >>>>
        System.out.println(configuration.getEnabled());
        System.out.println(configuration.getLogType());
        System.out.println(configuration.getHost());
        System.out.println(configuration.getPort());
        System.out.println(configuration.getLogFrequency());
        // <<<< TEST BLOCK >>>>
            executor = Executors.newScheduledThreadPool(4, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, PREFIX.concat(Integer.toString(threadNumber.getAndIncrement())).concat(")"));
                }
            });

            String urlString = "service:jmx:rmi:///jndi/rmi://" 
                    + configuration.getHost() + ":" 
                    + configuration.getPort() + "/jmxrmi";

            formatter = new MonitoringFormatter(urlString,
                                        configuration.getMonitoringJobList());

            try {
                formatter.createNewConnection();
            } catch (IOException ex) {
                enabled = false;
                Logger.getLogger(JMXMonitoringService.class.getName()).log(Level.SEVERE, null, ex);
            }

            events.register(this);
            if (enabled) {
                executor.scheduleAtFixedRate(formatter, 0, 
                        configuration.getLogFrequency(), 
                        TimeUnit.SECONDS);
            }

        } 
    }

    public void setEnabled(Boolean enabled) {
        if (this.enabled && !enabled) {
            
        } else if (!this.enabled && enabled) {
            this.enabled = true;
            bootstrapMonitoringService();
        } else if (this.enabled && enabled) {
            shutdownMonitoringService();
            bootstrapMonitoringService();
        }
    }

    public void shutdownMonitoringService() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
