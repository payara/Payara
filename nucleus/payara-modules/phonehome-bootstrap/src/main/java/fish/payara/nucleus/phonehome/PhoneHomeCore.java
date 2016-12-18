/*
 * 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) 2016 Payara Foundation and/or its affiliates.
 *  All rights reserved.
 * 
 *  The contents of this file are subject to the terms of the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 * 
 */
package fish.payara.nucleus.phonehome;

import com.sun.enterprise.config.serverbeans.Domain;
import java.beans.PropertyVetoException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author David Weaver
 */
@Service(name = "phonehome-core")
@RunLevel(StartupRunLevel.VAL)
public class PhoneHomeCore implements EventListener {
    
    private static final String THREAD_NAME = "PhoneHomeThread";
    
    private static PhoneHomeCore theCore;
    private static Boolean overrideEnabled;
    private boolean enabled;
    
    private UUID phoneHomeId;
    
    private ScheduledExecutorService executor;
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    PhoneHomeRuntimeConfiguration configuration;
    
    @Inject
    private ServerEnvironment env;
    
    @Inject
    private Events events;
    
    @Inject
    private Domain domain;
    
    @PostConstruct
    public void postConstruct() {
        theCore = this;
        events.register(this);
        
        if (env.isDas()) {
             
            if (configuration == null) {
                enabled = true;    
                phoneHomeId = UUID.randomUUID();
            } else {
                enabled = Boolean.valueOf(configuration.getEnabled());
                
                // Get the UUID from the config if one is present, otherwise use a randomly generated one
                try {
                    phoneHomeId = UUID.fromString(configuration.getPhoneHomeId());
                } catch (NullPointerException ex) {
                    phoneHomeId = UUID.randomUUID();
                    try {
                        ConfigSupport.apply(new SingleConfigCode<PhoneHomeRuntimeConfiguration>() {
                            @Override
                            public Object run(PhoneHomeRuntimeConfiguration configurationProxy)
                                    throws PropertyVetoException, TransactionFailure {
                                configurationProxy.setPhoneHomeId(phoneHomeId.toString());
                                return configurationProxy;
                            }
                        }, configuration);
                    } catch(TransactionFailure e) {
                        // Ignore and just don't write the ID to the config file
                    }
                }  
            }
            
            if (overrideEnabled != null) {
                enabled = overrideEnabled;
            }
        } else {
            enabled = false;
        }        
    }
    
    /**
     *
     * @param event
     */
    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_STARTUP)) {
            bootstrapPhoneHome();
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutdownPhoneHome();
        }
    }
    
    private void bootstrapPhoneHome() {
        if (enabled) {
            executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, THREAD_NAME);
                }
            });
            executor.scheduleAtFixedRate(new PhoneHomeTask(phoneHomeId.toString(), domain, env), 0, 1, TimeUnit.DAYS);
        }
    }
    
    private void shutdownPhoneHome() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
    
    public void enable(){
        setEnabled(true);
    }
    public void disable(){
        setEnabled(false);
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public void start() {
        if (this.enabled) {
            shutdownPhoneHome();
            bootstrapPhoneHome();         
        } else {
            this.enabled = true;
            bootstrapPhoneHome();
        }
    }
    public void stop() {
        if (this.enabled) {
            this.enabled = false;
            shutdownPhoneHome();
        }
    }

    public static void setOverrideEnabled(boolean enabled) {
        overrideEnabled = enabled;
    }
}
