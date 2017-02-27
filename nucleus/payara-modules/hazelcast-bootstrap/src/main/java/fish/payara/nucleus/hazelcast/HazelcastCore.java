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
package fish.payara.nucleus.hazelcast;

import org.glassfish.internal.api.JavaEEContextUtil;
import fish.payara.nucleus.hazelcast.contextproxy.CachingProviderProxy;
import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigLoader;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.PartitionGroupConfig;
import com.hazelcast.config.ScheduledExecutorConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.serialization.Serializer;
import com.hazelcast.nio.serialization.StreamSerializer;
import fish.payara.nucleus.events.HazelcastEvents;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "hazelcast-core")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastCore implements EventListener {

    public final static String INSTANCE_ATTRIBUTE = "GLASSFISH-INSTANCE";
    public final static String INSTANCE_GROUP_ATTRIBUTE = "GLASSFISH_INSTANCE_GROUP";
    public static final String CLUSTER_EXECUTOR_SERVICE_NAME="payara-cluster-execution";
    public static final String SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME="payara-scheduled-execution";
    private static HazelcastCore theCore;

    private HazelcastInstance theInstance;

    private CachingProvider hazelcastCachingProvider;
    private boolean enabled;
    private boolean booted=false;
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

    @Inject @Optional
    private JavaEEContextUtil ctxUtil;

    public static HazelcastCore getCore() {
        return theCore;
    }

    @PostConstruct
    public void postConstruct() {
        theCore = this;
        events.register(this);
        enabled = Boolean.valueOf(configuration.getEnabled());
    }
    
    public String getMemberName() {
        if (enabled && !booted) {
            bootstrapHazelcast();
        }
        return memberName;
    }
    
    public String getMemberGroup() {
        if (enabled && !booted) {
            bootstrapHazelcast();
        }
        return memberGroup;
    }
    
    public String getUUID() {
        bootstrapHazelcast();
        if (!enabled) {
            return UUID.randomUUID().toString();
        }        
        return theInstance.getCluster().getLocalMember().getUuid();
    }
    
    public boolean isLite() {
        bootstrapHazelcast();
        if (!enabled) {
            return false;
        }
        return theInstance.getCluster().getLocalMember().isLiteMember();
    }

    public HazelcastInstance getInstance() {
        bootstrapHazelcast();
        return theInstance;
    }

    public CachingProvider getCachingProvider() {
        bootstrapHazelcast();
        return hazelcastCachingProvider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutdownHazelcast();
        } else if (event.is(EventTypes.SERVER_STARTUP)) {
            bootstrapHazelcast();
        }
    }

    public void setEnabled(Boolean enabled) {
        if (!this.enabled && !enabled) {
            // do nothing
        } else if (this.enabled && !enabled) {
            this.enabled = false;
            shutdownHazelcast();
            booted = false;
        } else if (!this.enabled && enabled) {
            this.enabled = true;
            bootstrapHazelcast();
        } else if (this.enabled && enabled) {
            // we need to reboot
            shutdownHazelcast();
            booted =false;
            bootstrapHazelcast();
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
                if(ctxUtil == null) {
                    Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Hazelcast Application Object Serialization Not Available");
                }
                SerializationConfig serConfig = config.getSerializationConfig();
                if (serConfig == null || serConfig.getGlobalSerializerConfig() == null) {
                    SerializationConfig serializationConfig = new SerializationConfig()
                            .setGlobalSerializerConfig(new GlobalSerializerConfig().setImplementation(
                                    new PayaraHazelcastSerializer(ctxUtil, null))
                                    .setOverrideJavaSerialization(true));
                    config.setSerializationConfig(serializationConfig);
                }
                Serializer ser = config.getSerializationConfig().getGlobalSerializerConfig().getImplementation();
                if(ctxUtil != null && ser instanceof StreamSerializer) {
                    config.getSerializationConfig().getGlobalSerializerConfig().setImplementation(
                            new PayaraHazelcastSerializer(ctxUtil, (StreamSerializer<?>)ser));
                }
                else {
                    Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Global serializer is not StreamSerializer: {0}", ser.getClass().getName());
                }
            } else { // there is no config override
                config.setClassLoader(clh.getCommonClassLoader());
                if(ctxUtil != null) {
                    SerializationConfig serializationConfig = new SerializationConfig()
                            .setGlobalSerializerConfig(new GlobalSerializerConfig().setImplementation(
                                    new PayaraHazelcastSerializer(ctxUtil, null))
                                    .setOverrideJavaSerialization(true));
                    config.setSerializationConfig(serializationConfig);
                }
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
                
                // build the executor config
                ExecutorConfig executorConfig = config.getExecutorConfig(CLUSTER_EXECUTOR_SERVICE_NAME);
                executorConfig.setStatisticsEnabled(true);
                executorConfig.setPoolSize(Integer.valueOf(configuration.getExecutorPoolSize()));
                executorConfig.setQueueCapacity(Integer.valueOf(configuration.getExecutorQueueCapacity()));
                
                ScheduledExecutorConfig scheduledExecutorConfig = config.getScheduledExecutorConfig(SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
                scheduledExecutorConfig.setDurability(1);
                scheduledExecutorConfig.setCapacity(Integer.valueOf(configuration.getScheduledExecutorQueueCapacity()));
                scheduledExecutorConfig.setPoolSize(Integer.valueOf(configuration.getScheduledExecutorPoolSize()));
                            
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

    private synchronized void bootstrapHazelcast() {
        if (!booted && enabled) {
            Config config = buildConfiguration();
            theInstance = Hazelcast.newHazelcastInstance(config);
            if (env.isMicro()) {
                memberName = configuration.getMemberName();
                memberGroup = configuration.getMemberGroup();
                if (Boolean.valueOf(configuration.getGenerateNames()) || memberName == null) {
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
            hazelcastCachingProvider = new CachingProviderProxy(HazelcastServerCachingProvider.createCachingProvider(theInstance), context);
            events.send(new Event(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE));
            bindToJNDI();
            booted = true;
        }
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
