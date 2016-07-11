/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014,2015,2016 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.nucleus.hazelcast;

import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigLoader;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author steve
 */
@Service(name = "hazelcast-core")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastCore implements EventListener {

    public final static String INSTANCE_ATTRIBUTE = "GLASSFISH-INSTANCE";
    private static HazelcastCore theCore;
    
    private static MulticastConfiguration overrideConfiguration;

    
    private HazelcastInstance theInstance;

    private CachingProvider hazelcastCachingProvider;
    private boolean enabled;
    private String memberName;

    @Inject
    Events events;

    @Inject
    ServerContext context;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HazelcastRuntimeConfiguration configuration;


    public static HazelcastCore getCore() {
        return theCore;
    }
    
    public static void setMulticastOverride(MulticastConfiguration config){
        overrideConfiguration = config;
    }

    @PostConstruct
    public void postConstruct() {
        theCore = this;
        enabled = Boolean.valueOf(configuration.getEnabled());
        events.register(this);

        if ((Boolean.valueOf(configuration.getEnabled()))) {
            bootstrapHazelcast();
        }
    }

    public HazelcastInstance getInstance() {
        return theInstance;
    }

    public CachingProvider getCachingProvider() {
        return hazelcastCachingProvider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutdownHazelcast();
        } else if (event.is(EventTypes.SERVER_READY)) {
            if (enabled) {
                bindToJNDI();
            }
            
        }
    }

    public void setEnabled(Boolean enabled) {
        if (!this.enabled && !enabled) {
            // do nothing
        } else if (this.enabled && !enabled) {
            this.enabled = false;
            shutdownHazelcast();
        } else if (!this.enabled && enabled) {
            this.enabled = true;
            bootstrapHazelcast();
            bindToJNDI();
        } else if (this.enabled && enabled) {
            // we need to reboot
            shutdownHazelcast();
            bootstrapHazelcast();
            bindToJNDI();
        }
    }

    private Config buildConfiguration() {
        Config config = new Config();

        String hazelcastFilePath = "";
        URL serverConfigURL;
        try {
            if (overrideConfiguration != null && overrideConfiguration.getAlternateConfigFile() != null) {
                XmlConfigBuilder builder = new XmlConfigBuilder(overrideConfiguration.getAlternateConfigFile().toURL());
                config = builder.build();
                return config;
            }
            serverConfigURL = new URL(context.getServerConfigURL());
            File serverConfigFile = new File(serverConfigURL.getPath());
            hazelcastFilePath = serverConfigFile.getParentFile().getAbsolutePath() + File.separator + configuration.getHazelcastConfigurationFile();
            File file = new File(hazelcastFilePath);
            if (file.exists()) {
                config = ConfigLoader.load(hazelcastFilePath);
                if (config == null) {
                    Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Hazelcast Core could not find configuration file {0} using default configuration", hazelcastFilePath);
                    config = new Config();
                }
            } else { // there is no config override
                
                memberName = context.getInstanceName();
                MulticastConfig mcConfig = config.getNetworkConfig().getJoin().getMulticastConfig();
                config.getNetworkConfig().setPortAutoIncrement(true);
                mcConfig.setEnabled(true);                // check Payara micro overrides
                if (overrideConfiguration != null) {
                    mcConfig.setMulticastGroup(overrideConfiguration.getMulticastGroup());
                    mcConfig.setMulticastPort(overrideConfiguration.getMulticastPort());
                    config.getNetworkConfig().setPort(overrideConfiguration.getStartPort());
                    if (overrideConfiguration.getMemberName() != null) {
                        memberName = overrideConfiguration.getMemberName();
                    }
                    config.setLiteMember(overrideConfiguration.isLite());
                    config.setLicenseKey(overrideConfiguration.getLicenseKey());
                    // set group config
                    GroupConfig gc = config.getGroupConfig();
                    gc.setName(overrideConfiguration.getClusterGroupName());
                    gc.setPassword(overrideConfiguration.getClusterGroupPassword());
                    
                } else {
                   mcConfig.setMulticastGroup(configuration.getMulticastGroup());
                   mcConfig.setMulticastPort(Integer.valueOf(configuration.getMulticastPort()));
                   config.getNetworkConfig().setPort(Integer.valueOf(configuration.getStartPort()));
                   config.setLicenseKey(configuration.getLicenseKey());
                   config.setLiteMember(Boolean.parseBoolean(configuration.getLite()));
                   // set group config
                   GroupConfig gc = config.getGroupConfig();
                   gc.setName(configuration.getClusterGroupName());
                   gc.setPassword(configuration.getClusterGroupPassword());
                }

                // build the configuration
                config.setProperty("hazelcast.jmx", "true");

            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Unable to parse server config URL", ex);
        } catch (IOException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Hazelcast Core could not load configuration file " + hazelcastFilePath + " using default configuration", ex);
        }
        return config;
    }

    private void shutdownHazelcast() {
        if (theInstance != null) {
            unbindFromJNDI();
            hazelcastCachingProvider.getCacheManager().close();
            hazelcastCachingProvider.close();
            theInstance.shutdown();
            theInstance = null;
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Shutdown Hazelcast");
        }
    }

    private void bootstrapHazelcast() { 
        Config config = buildConfiguration();
        theInstance = Hazelcast.newHazelcastInstance(config);
        if (memberName == null) {
            memberName = context.getInstanceName();
        }
        theInstance.getCluster().getLocalMember().setStringAttribute(INSTANCE_ATTRIBUTE, memberName);
        hazelcastCachingProvider = HazelcastServerCachingProvider.createCachingProvider(theInstance);
    }

    private void bindToJNDI() {
        try {
            InitialContext ctx;
            ctx = new InitialContext();
            ctx.bind(configuration.getJNDIName(), theInstance);
            ctx.bind(configuration.getCachingProviderJNDIName(), hazelcastCachingProvider);
            ctx.bind(configuration.getCacheManagerJNDIName(), hazelcastCachingProvider.getCacheManager());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Hazelcast Instance Bound to JNDI at {0}", configuration.getJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Caching Provider Bound to JNDI at {0}", configuration.getCachingProviderJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Default Cache Manager Bound to JNDI at {0}", configuration.getCacheManagerJNDIName());
        } catch (NamingException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void unbindFromJNDI() {
        try {
            InitialContext ctx;
            ctx = new InitialContext();
            ctx.unbind(configuration.getJNDIName());
            ctx.unbind(configuration.getCacheManagerJNDIName());
            ctx.unbind(configuration.getCachingProviderJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Hazelcast Instance Unbound from JNDI at {0}", configuration.getJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Caching Provider Unbound from JNDI at {0}", configuration.getCachingProviderJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Cache Manager Unbound from JNDI at {0}", configuration.getCacheManagerJNDIName());
        } catch (NamingException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}
