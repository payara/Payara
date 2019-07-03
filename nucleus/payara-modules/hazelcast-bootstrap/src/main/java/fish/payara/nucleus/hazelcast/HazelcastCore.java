/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigLoader;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MemberAddressProviderConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.PartitionGroupConfig;
import com.hazelcast.config.ScheduledExecutorConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.kubernetes.KubernetesProperties;
import com.hazelcast.nio.serialization.Serializer;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.sun.enterprise.util.Utility;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.contextproxy.CachingProviderProxy;
import java.beans.PropertyChangeEvent;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.ServerEnvironment.Status;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * The core class for using Hazelcast in Payara
 * @author Steve Millidge (Payara Foundation)
 * @since 4.1.151
 */
@Service(name = "hazelcast-core")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastCore implements EventListener, ConfigListener {

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
    HazelcastRuntimeConfiguration configuration;
    
    @Inject
    HazelcastConfigSpecificConfiguration nodeConfig;

    @Inject
    private ClassLoaderHierarchy clh;

    @Inject @Optional
    private JavaEEContextUtil ctxUtil;
    
    // Provides ability to register a configuration listener
    @Inject
    Transactions transactions;

    private static final ThreadLocal<Boolean> thrLocalDisabled = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    /**
     * Returns the version of the object that has been instantiated.
     * @return null if an instance of {@link HazelcastCore} has not been created
     */
    public static HazelcastCore getCore() {
        return theCore;
    }

    public static void setThreadLocalDisabled(boolean tf) {
        thrLocalDisabled.set(tf);
    }

    @PostConstruct
    public void postConstruct() {
        theCore = this;
        events.register(this);
        enabled = Boolean.valueOf(nodeConfig.getEnabled());
        transactions.addListenerForType(HazelcastConfigSpecificConfiguration.class, this);
        transactions.addListenerForType(HazelcastRuntimeConfiguration.class, this);
        
        if (env.isMicro()) {
            memberName = nodeConfig.getMemberName();
            memberGroup = nodeConfig.getMemberGroup();
        } else {
            memberName = context.getInstanceName();
            memberGroup = nodeConfig.getMemberGroup();
        }
    }
    
    /**
     * Returns the Hazelcast name of the instance
     * <p>
     * Note this is not the same as the name of the instance config or node
     * @return {@code Payara} by default
     * @since 4.1.1.171
     */
    public String getMemberName() {
        return memberName;
    }
    
    /**
     * Gets the name of the member group that this instance belongs to
     * @return {@code MicroShoal} by default
     * @since 4.1.1.171
     */
    public String getMemberGroup() {
        return memberGroup;
    }
    
    /**
     * Returns the UUID of the instance.
     * If Hazelcast is not enabled then a new random one will be returned.
     * @return a 128-bit immutable universally unique identifier
     * @since 4.1.1.171
     */
    public String getUUID() {
        bootstrapHazelcast();
        if (!enabled) {
            return UUID.randomUUID().toString();
        }        
        return theInstance.getCluster().getLocalMember().getUuid();
    }
    
    /**
     * Returns true if this instance is a Hazelcast Lite instance
     * @return
     * @since 4.1.1.171
     */
    public boolean isLite() {
        bootstrapHazelcast();
        if (!enabled) {
            return false;
        }
        return theInstance.getCluster().getLocalMember().isLiteMember();
    }

    /**
     * Gets the actual Hazelcast instance.
     * Hazelcast will be booted by this method if
     * it hasn't already started.
     * @return
     */
    public HazelcastInstance getInstance() {
        bootstrapHazelcast();
        return theInstance;
    }

    /**
     * Gets the JCache provider used by Hazelcast
     * @return
     * @see <a href=http://docs.hazelcast.org/docs/3.8.6/javadoc/com/hazelcast/cache/HazelcastCachingProvider.html">HazelcastCachingProvider</a>
     */
    public CachingProvider getCachingProvider() {
        bootstrapHazelcast();
        return hazelcastCachingProvider;
    }

    /**
     * 
     * @return Whether Hazelcast is currently enabled
     */
    public boolean isEnabled() {
        return enabled && !thrLocalDisabled.get();
    }

    @Override
    public void event(Event event) {
        if (event.is(Deployment.ALL_APPLICATIONS_STOPPED)) {
            shutdownHazelcast();
        } else if (event.is(Deployment.ALL_APPLICATIONS_LOADED)) {
            ClassLoader oldCL = Utility.getClassLoader();
            try {
                Utility.setContextClassLoader(clh.getCommonClassLoader());
                bootstrapHazelcast();
            } finally {
                Utility.setContextClassLoader(oldCL);
            }
        }
        else if(event.is(EventTypes.SERVER_STARTUP) && isEnabled() && booted) {
            // send this event only after all Startup services have been initialized
            events.send(new Event(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE));
        }
    }

    /**
     * Sets whether Hazelcast should be enabled.
     * @param enabled If true will start Hazelcast or restart if currently running;
     * if false will shut down Hazelcast.
     */
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
            this.enabled = true;
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
                } else {
                    SerializationConfig serConfig = config.getSerializationConfig();
                    if (serConfig == null) {
                        serConfig = new SerializationConfig();
                        setPayaraSerializerConfig(serConfig);
                        config.setSerializationConfig(serConfig);
                    } else {
                        if(serConfig.getGlobalSerializerConfig() == null) {
                            setPayaraSerializerConfig(serConfig);
                        } else {
                            Serializer ser = serConfig.getGlobalSerializerConfig().getImplementation();
                            if (ser instanceof StreamSerializer) {
                                config.getSerializationConfig().getGlobalSerializerConfig().setImplementation(
                                        new PayaraHazelcastSerializer(ctxUtil, (StreamSerializer<?>) ser));
                            } else {
                                Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Global serializer is not StreamSerializer: {0}", ser.getClass().getName());
                            }
                        }
                    }
                }
            } else { // there is no config override
                config.setClassLoader(clh.getCommonClassLoader());
                if(ctxUtil != null) {
                    SerializationConfig serializationConfig = new SerializationConfig();
                    setPayaraSerializerConfig(serializationConfig);
                    config.setSerializationConfig(serializationConfig);
                }
                
                buildNetworkConfiguration(config);
               
                config.setLicenseKey(configuration.getLicenseKey());
                config.setLiteMember(Boolean.parseBoolean(nodeConfig.getLite()));
                
                
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
                executorConfig.setPoolSize(Integer.valueOf(nodeConfig.getExecutorPoolSize()));
                executorConfig.setQueueCapacity(Integer.valueOf(nodeConfig.getExecutorQueueCapacity()));
                
                ScheduledExecutorConfig scheduledExecutorConfig = config.getScheduledExecutorConfig(SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
                scheduledExecutorConfig.setDurability(1);
                scheduledExecutorConfig.setCapacity(Integer.valueOf(nodeConfig.getScheduledExecutorQueueCapacity()));
                scheduledExecutorConfig.setPoolSize(Integer.valueOf(nodeConfig.getScheduledExecutorPoolSize()));
                            
                config.setProperty("hazelcast.jmx", "true");
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Unable to parse server config URL", ex);
        } catch (IOException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Hazelcast Core could not load configuration file " + hazelcastFilePath + " using default configuration", ex);
        }
        return config;
    }

    private void setPayaraSerializerConfig(SerializationConfig serConfig) {
        if(serConfig == null || ctxUtil == null) {
            throw new IllegalStateException("either serialization config or ctxUtil is null");
        }
        serConfig.setGlobalSerializerConfig(new GlobalSerializerConfig().setImplementation(
                new PayaraHazelcastSerializer(ctxUtil, null))
                .setOverrideJavaSerialization(true));
    }

    private void buildNetworkConfiguration(Config config) throws NumberFormatException {
        NetworkConfig nConfig = config.getNetworkConfig();
        if (nodeConfig.getPublicAddress() != null && !nodeConfig.getPublicAddress().isEmpty()) {
            nConfig.setPublicAddress(nodeConfig.getPublicAddress());
        }

        if (!configuration.getInterface().isEmpty()) {
            // add an interfaces configuration
           String[] interfaceNames = configuration.getInterface().split(",");
            for (String interfaceName : interfaceNames) {
                nConfig.getInterfaces().addInterface(interfaceName);
            }
            nConfig.getInterfaces().setEnabled(true);
        } else {
            MemberAddressProviderConfig memberAddressProviderConfig = nConfig.getMemberAddressProviderConfig();
            memberAddressProviderConfig.setEnabled(enabled);
            memberAddressProviderConfig.setImplementation(new MemberAddressPicker(env, configuration, nodeConfig));
        }
        
        String discoveryMode = configuration.getDiscoveryMode();
        if (discoveryMode.startsWith("tcpip")) {
            TcpIpConfig tConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
            tConfig.setEnabled(true);
            tConfig.addMember(configuration.getTcpipMembers());
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);   
        } else if (discoveryMode.startsWith("dns")) {
            config.setProperty("hazelcast.discovery.enabled", "true");
            config.getNetworkConfig().getJoin().getDiscoveryConfig().setDiscoveryServiceProvider(new DnsDiscoveryServiceProvider(configuration.getDnsMembers()));
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false); 
        } else if (discoveryMode.startsWith("multicast")) {
            // build networking
            MulticastConfig mcConfig = config.getNetworkConfig().getJoin().getMulticastConfig();
            mcConfig.setEnabled(true);                       
            mcConfig.setMulticastGroup(configuration.getMulticastGroup());
            mcConfig.setMulticastPort(Integer.valueOf(configuration.getMulticastPort()));      
        } else if (discoveryMode.startsWith("kubernetes")) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getKubernetesConfig().setEnabled(true)
                    .setProperty(KubernetesProperties.NAMESPACE.key(), configuration.getKubernetesNamespace())
                    .setProperty(KubernetesProperties.SERVICE_NAME.key(), configuration.getKubernetesServiceName())
                    .setProperty(KubernetesProperties.SERVICE_PORT.key(), configuration.getStartPort());
        } else {
            //build the domain discovery config
            config.setProperty("hazelcast.discovery.enabled", "true");
            config.getNetworkConfig().getJoin().getDiscoveryConfig().setDiscoveryServiceProvider(new DomainDiscoveryServiceProvider());            
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);            
        }
        int port = Integer.valueOf(configuration.getStartPort());
        if (env.isDas() && !env.isMicro()) {
            port = Integer.valueOf(configuration.getDasPort());
        }
        config.getNetworkConfig().setPort(port);
        config.getNetworkConfig().setPortAutoIncrement("true".equalsIgnoreCase(configuration.getAutoIncrementPort()));
    }

    private void shutdownHazelcast() {
        if (theInstance != null) {
            enabled = false;
            events.send(new Event(HazelcastEvents.HAZELCAST_SHUTDOWN_STARTED, true));
            unbindFromJNDI();
            hazelcastCachingProvider.getCacheManager().close();
            hazelcastCachingProvider.close();
            theInstance.shutdown();
            theInstance = null;
            events.send(new Event(HazelcastEvents.HAZELCAST_SHUTDOWN_COMPLETE));
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Shutdown Hazelcast");
        }
    }

    /**
     * Starts Hazelcast if not already enabled
     */
    private synchronized void bootstrapHazelcast() {
        if (!booted && enabled && !thrLocalDisabled.get()) {
            Config config = buildConfiguration();
            theInstance = Hazelcast.newHazelcastInstance(config);
            if (env.isMicro()) {
                if (Boolean.valueOf(configuration.getGenerateNames()) || memberName == null) {
                    memberName = PayaraMicroNameGenerator.generateName();
                    Set<com.hazelcast.core.Member> clusterMembers = theInstance.getCluster().getMembers();

                    // If the instance name was generated, we need to compile a list of all the instance names in use within 
                    // the instance group, excluding this local instance
                    List<String> takenNames = new ArrayList<>();
                    for (com.hazelcast.core.Member member : clusterMembers) {
                        if (member != theInstance.getCluster().getLocalMember()
                                && member.getStringAttribute(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE) != null 
                                && member.getStringAttribute(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE).equalsIgnoreCase(memberGroup)) {
                            takenNames.add(member.getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE));
                        }
                    }

                    // If our generated name is already in use within the instance group, either generate a new one or set the 
                    // name to this instance's UUID if there are no more unique generated options left
                    if (takenNames.contains(memberName)) {
                        memberName = PayaraMicroNameGenerator.generateUniqueName(takenNames,
                                theInstance.getCluster().getLocalMember().getUuid());
                        theInstance.getCluster().getLocalMember().setStringAttribute(
                                HazelcastCore.INSTANCE_ATTRIBUTE, memberName);
                    }
                }
            } 
            theInstance.getCluster().getLocalMember().setStringAttribute(INSTANCE_ATTRIBUTE, memberName);
            theInstance.getCluster().getLocalMember().setStringAttribute(INSTANCE_GROUP_ATTRIBUTE, memberGroup);
            hazelcastCachingProvider = new CachingProviderProxy(HazelcastServerCachingProvider.createCachingProvider(theInstance), context);
            bindToJNDI();
            if(env.getStatus() == Status.started) {
                // only issue this event if the server is already running,
                // otherwise the SERVER_STARTUP event will issue this event as well
                events.send(new Event(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE));
            }
            booted = true;
        }
    }

    private void bindToJNDI() {
        try {
            InitialContext ctx;
            ctx = new InitialContext();
            ctx.bind(nodeConfig.getJNDIName(), theInstance);
            ctx.bind(nodeConfig.getCachingProviderJNDIName(), hazelcastCachingProvider);
            ctx.bind(nodeConfig.getCacheManagerJNDIName(), hazelcastCachingProvider.getCacheManager());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Hazelcast Instance Bound to JNDI at {0}", nodeConfig.getJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Caching Provider Bound to JNDI at {0}", nodeConfig.getCachingProviderJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Default Cache Manager Bound to JNDI at {0}", nodeConfig.getCacheManagerJNDIName());
        } catch (NamingException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void unbindFromJNDI() {
        try {
            InitialContext ctx;
            ctx = new InitialContext();
            ctx.unbind(nodeConfig.getJNDIName());
            ctx.unbind(nodeConfig.getCacheManagerJNDIName());
            ctx.unbind(nodeConfig.getCachingProviderJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Hazelcast Instance Unbound from JNDI at {0}", nodeConfig.getJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Caching Provider Unbound from JNDI at {0}", nodeConfig.getCachingProviderJNDIName());
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "JSR107 Cache Manager Unbound from JNDI at {0}", nodeConfig.getCacheManagerJNDIName());
        } catch (NamingException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets the port that Hazelcast in running on
     * @return The default is {@link 54327}
     */
    public int getPort() {
        return theInstance.getCluster().getLocalMember().getSocketAddress().getPort();
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] pces) {
        return null;
    }
}
