/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014,2015,2016,2017 Payara Foundation. All rights reserved.

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
import com.hazelcast.config.PartitionGroupConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import fish.payara.nucleus.events.HazelcastEvents;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.glassfish.internal.api.ClassLoaderHierarchy;
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
    public final static String INSTANCE_GROUP_ATTRIBUTE = "GLASSFISH_INSTANCE_GROUP";
    private static HazelcastCore theCore;

    private HazelcastInstance theInstance;

    private CachingProvider hazelcastCachingProvider;
    private boolean enabled;
    private String memberName;
    private String memberGroup;

    @Inject
    Events events;

    @Inject
    ServerContext context;
    
    @Inject
    ServerEnvironment env;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HazelcastRuntimeConfiguration configuration;

    @Inject
    private ClassLoaderHierarchy clh;
    
    public static HazelcastCore getCore() {
        return theCore;
    }

    @PostConstruct
    public void postConstruct() {
        theCore = this;
        events.register(this);
    }
    
    public String getMemberName() {
        return memberName;
    }
    
    public String getMemberGroup() {
        return memberGroup;
    }
    
    public String getUUID() {
        return theInstance.getCluster().getLocalMember().getUuid();
    }
    
    public boolean isLite() {
        return theInstance.getCluster().getLocalMember().isLiteMember();
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
        } else if (event.is(EventTypes.SERVER_STARTUP)) {
            if ((Boolean.valueOf(configuration.getEnabled()))) {
                enabled = true;
                bootstrapHazelcast();
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
                config.setClassLoader(clh.getCommonClassLoader());
            } else { // there is no config override
                config.setClassLoader(clh.getCommonClassLoader());
                MulticastConfig mcConfig = config.getNetworkConfig().getJoin().getMulticastConfig();
                config.getNetworkConfig().setPortAutoIncrement(true);
                mcConfig.setEnabled(true);                // check Payara micro overrides

                mcConfig.setMulticastGroup(configuration.getMulticastGroup());
                mcConfig.setMulticastPort(Integer.valueOf(configuration.getMulticastPort()));
                config.getNetworkConfig().setPort(Integer.valueOf(configuration.getStartPort()));
                config.setLicenseKey(configuration.getLicenseKey());
                config.setLiteMember(Boolean.parseBoolean(configuration.getLite()));
                // set group config
                GroupConfig gc = config.getGroupConfig();
                gc.setName(configuration.getClusterGroupName());
                gc.setPassword(configuration.getClusterGroupPassword());

                // build the configuration
                if ("true".equals(configuration.getHostAwarePartitioning())) {
                    PartitionGroupConfig partitionGroupConfig = config.getPartitionGroupConfig();
                    partitionGroupConfig.setEnabled(enabled);
                    partitionGroupConfig.setGroupType(PartitionGroupConfig.MemberGroupType.HOST_AWARE);
                }
                            
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
            events.send(new Event(HazelcastEvents.HAZELCAST_SHUTDOWN_COMPLETE));
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Shutdown Hazelcast");
        }
    }

    private void bootstrapHazelcast() {
        Config config = buildConfiguration();
        theInstance = Hazelcast.newHazelcastInstance(config);
        if (env.isMicro()){
            memberName = configuration.getMemberName();
            memberGroup = configuration.getMemberGroup();
            if (Boolean.valueOf(configuration.getGenerateNames())  || memberName == null) {
                NameGenerator gen = new NameGenerator();
                memberName = gen.generateName();
                Set<com.hazelcast.core.Member> clusterMembers = theInstance.getCluster().getMembers();

                // If the instance name was generated, we need to compile a list of all the instance names in use within 
                // the instance group, excluding this local instance
                List<String> takenNames = new ArrayList<>();
                for (com.hazelcast.core.Member member : clusterMembers) {
                    if (member != theInstance.getCluster().getLocalMember()
                            && member.getStringAttribute(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE).equalsIgnoreCase(
                                    memberGroup)) {
                        takenNames.add(member.getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE));
                    }
                }

                // If our generated name is already in use within the instance group, either generate a new one or set the 
                // name to this instance's UUID if there are no more unique generated options left
                if (takenNames.contains(memberName)) {
                    memberName = gen.generateUniqueName(takenNames,
                            theInstance.getCluster().getLocalMember().getUuid());
                    theInstance.getCluster().getLocalMember().setStringAttribute(
                            HazelcastCore.INSTANCE_ATTRIBUTE, memberName);
                }
            }                
        } else { 
            if (memberName == null) {
                memberName = context.getInstanceName();
            }
            if (memberGroup == null) {
                memberGroup = context.getConfigBean().getConfigRef();
            }
        }
        
        
        theInstance.getCluster().getLocalMember().setStringAttribute(INSTANCE_ATTRIBUTE, memberName);
        theInstance.getCluster().getLocalMember().setStringAttribute(INSTANCE_GROUP_ATTRIBUTE, memberGroup);
        hazelcastCachingProvider = HazelcastServerCachingProvider.createCachingProvider(theInstance);
        events.send(new Event(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE));
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

    public int getPort() {
        return theInstance.getCluster().getLocalMember().getSocketAddress().getPort();
    }

}
