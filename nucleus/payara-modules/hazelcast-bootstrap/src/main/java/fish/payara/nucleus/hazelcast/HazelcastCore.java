/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.hazelcast;

import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.ClusterState;
import com.hazelcast.cluster.Member;
import com.hazelcast.collection.ISet;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.KubernetesConfig;
import com.hazelcast.config.MemberAddressProviderConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.PartitionGroupConfig;
import com.hazelcast.config.ScheduledExecutorConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.YamlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.cp.event.CPGroupAvailabilityEvent;
import com.hazelcast.cp.event.CPGroupAvailabilityListener;
import com.hazelcast.cp.event.CPMembershipEvent;
import com.hazelcast.cp.event.CPMembershipListener;
import com.hazelcast.cp.exception.CPGroupDestroyedException;
import com.hazelcast.internal.config.ConfigLoader;
import com.hazelcast.kubernetes.KubernetesProperties;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.Serializer;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.spi.properties.ClusterProperty;
import com.sun.enterprise.util.Utility;
import fish.payara.nucleus.events.HazelcastEvents;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import javax.cache.spi.CachingProvider;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.hazelcast.cp.CPGroup.METADATA_CP_GROUP_NAME;
import static fish.payara.nucleus.hazelcast.PayaraHazelcastDiscoveryFactory.HOST_AWARE_PARTITIONING;
import static java.lang.String.valueOf;

/**
 * The core class for using Hazelcast in Payara
 * @author Steve Millidge (Payara Foundation)
 * @since 4.1.151
 */
@Service(name = "hazelcast-core")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastCore implements EventListener, ConfigListener {

    public final static String INSTANCE_ATTRIBUTE_MAP = "payara-instance-map";
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

    private boolean datagridEncryptionValue;

    @Inject
    Events events;

    @Inject
    ServerContext context;

    @Inject
    ServerEnvironment env;

    @Inject
    HazelcastRuntimeConfiguration configuration;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HazelcastConfigSpecificConfiguration nodeConfig;

    @Inject
    private ClassLoaderHierarchy clh;

    @Inject @Optional
    private JavaEEContextUtil ctxUtil;

    // Provides ability to register a configuration listener
    @Inject
    Transactions transactions;

    final Lock cpResetLock = new ReentrantLock();
    final AtomicReference<Instant> lastResetTime = new AtomicReference<>(Instant.EPOCH);

    /**
     * Returns the version of the object that has been instantiated.
     * @return null if an instance of {@link HazelcastCore} has not been created
     */
    public static HazelcastCore getCore() {
        return theCore;
    }

    @PostConstruct
    public void postConstruct() {
        theCore = this;
        events.register(this);
        enabled = Boolean.parseBoolean(nodeConfig.getEnabled());
        transactions.addListenerForType(HazelcastConfigSpecificConfiguration.class, this);
        transactions.addListenerForType(HazelcastRuntimeConfiguration.class, this);

        if (env.isMicro()) {
            memberName = nodeConfig.getMemberName();
            memberGroup = nodeConfig.getMemberGroup();
        } else {
            memberName = context.getInstanceName();
            memberGroup = nodeConfig.getMemberGroup();
        }

        datagridEncryptionValue = Boolean.parseBoolean(configuration.getDatagridEncryptionEnabled());
        if (datagridEncryptionValue) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Data grid encryption is enabled");
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
    public UUID getUUID() {
        bootstrapHazelcast();
        if (!enabled) {
            return UUID.randomUUID();
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
     * @see <a href="http://docs.hazelcast.org/docs/3.8.6/javadoc/com/hazelcast/cache/HazelcastCachingProvider.html">HazelcastCachingProvider</a>
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
        return enabled;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
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
        try {
            Boolean isChangeToDefault = Boolean.valueOf(configuration.getChangeToDefault());
            hazelcastFilePath = System.getProperty("hazelcast.config");
            if (hazelcastFilePath == null || hazelcastFilePath.isEmpty()) {
                hazelcastFilePath = configuration.getHazelcastConfigurationFile();
            }
            File file = new File(hazelcastFilePath);
            if (file.exists()) {
                Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO,
                        "Loading Hazelcast configuration from file: {0}", hazelcastFilePath);
                config = isYamlFile(hazelcastFilePath) ?
                    new YamlConfigBuilder(hazelcastFilePath).build() : ConfigLoader.load(hazelcastFilePath);
                if (config == null) {
                    Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING,
                            "Hazelcast Core could not find configuration file {0} using default configuration",
                            hazelcastFilePath);
                    config = new Config();
                }
                config.setClassLoader(clh.getCommonClassLoader());
                if(ctxUtil == null) {
                    Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING,
                            "Hazelcast Application Object Serialization Not Available");
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
                                Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING,
                                        "Global serializer is not StreamSerializer: {0}", ser.getClass().getName());
                            }
                        }
                    }
                }
                final Config config1 = config;
                ConfigSupport.apply(new SingleConfigCode<>() {
                    @Override
                    public Object run(final HazelcastRuntimeConfiguration hazelcastRuntimeConfigurationProxy) {
                        fillHazelcastConfigurationFromConfig(config1, hazelcastRuntimeConfigurationProxy);
                        Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Hazelcast general configuration filled from file");
                        return null;
                    }
                }, configuration);
                ConfigSupport.apply(new SingleConfigCode<>() {
                    @Override
                    public Object run(final HazelcastConfigSpecificConfiguration hazelcastRuntimeConfigurationProxy) {
                        fillSpecificHazelcastConfigFromConfig(config1, hazelcastRuntimeConfigurationProxy);
                        Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Hazelcast specific configuration created");
                        return null;
                    }
                }, nodeConfig);
            } else { // there is no config override
                if (isChangeToDefault) {
                    try {
                        fillConfigurationWithDefaults();
                    } catch (TransactionFailure e) {
                        Logger.getLogger(HazelcastCore.class.getName()).log(Level.SEVERE,
                                "Hazelcast setting to default config exception: " + e.toString(), e);
                    }
                }
                config.setClassLoader(clh.getCommonClassLoader());
                // can set to zero as of Hazelcast 5.4 or greater
                config.setProperty(ClusterProperty.WAIT_SECONDS_BEFORE_JOIN.getName(), "1");

                if(ctxUtil != null) {
                    SerializationConfig serializationConfig = new SerializationConfig();
                    setPayaraSerializerConfig(serializationConfig);
                    config.setSerializationConfig(serializationConfig);
                }

                config.setLicenseKey(configuration.getLicenseKey());
                config.setLiteMember(Boolean.parseBoolean(nodeConfig.getLite()));


                config.setClusterName(configuration.getClusterGroupName());

                // build the configuration
                boolean hostAwarePartitioning = false;
                if ("true".equals(configuration.getHostAwarePartitioning())) {
                    hostAwarePartitioning = true;
                    PartitionGroupConfig partitionGroupConfig = config.getPartitionGroupConfig();
                    partitionGroupConfig.setEnabled(enabled);
                    partitionGroupConfig.setGroupType(PartitionGroupConfig.MemberGroupType.HOST_AWARE);
                }

                buildNetworkConfiguration(config, hostAwarePartitioning);

                // build the executor config
                ExecutorConfig executorConfig = config.getExecutorConfig(CLUSTER_EXECUTOR_SERVICE_NAME);
                executorConfig.setStatisticsEnabled(true);
                executorConfig.setPoolSize(Integer.parseInt(nodeConfig.getExecutorPoolSize()));
                executorConfig.setQueueCapacity(Integer.parseInt(nodeConfig.getExecutorQueueCapacity()));

                ScheduledExecutorConfig scheduledExecutorConfig = config.getScheduledExecutorConfig(SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
                scheduledExecutorConfig.setDurability(1);
                scheduledExecutorConfig.setCapacity(Integer.parseInt(nodeConfig.getScheduledExecutorQueueCapacity()));
                scheduledExecutorConfig.setPoolSize(Integer.parseInt(nodeConfig.getScheduledExecutorPoolSize()));

                config.setProperty("hazelcast.jmx", "true");
            }
            if (config.getCPSubsystemConfig().getCPMemberCount() == 0) {
                config.getCPSubsystemConfig().setCPMemberCount(Integer.getInteger("hazelcast.cp-subsystem.cp-member-count", 0));
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Unable to parse server config URL", ex);
        } catch (IOException ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Hazelcast Core could not load configuration file " + hazelcastFilePath + " using default configuration", ex);
        } catch (TransactionFailure ex) {
            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Hazelcast Configuration data could no be saved", ex);
        }
        return config;
    }

    private static boolean isYamlFile(String hazelcastFilePath) {
        return hazelcastFilePath.endsWith(".yaml") || hazelcastFilePath.endsWith(".yml");
    }

    private void setPayaraSerializerConfig(SerializationConfig serConfig) {
        if(serConfig == null || ctxUtil == null) {
            throw new IllegalStateException("either serialization config or ctxUtil is null");
        }
        serConfig.setGlobalSerializerConfig(new GlobalSerializerConfig().setImplementation(
                new PayaraHazelcastSerializer(ctxUtil, null))
                .setOverrideJavaSerialization(true));
    }

    private void buildNetworkConfiguration(Config config, boolean hostAwarePartitioning) throws NumberFormatException {
        NetworkConfig nConfig = config.getNetworkConfig();
        String noClusterProp = nodeConfig.getClusteringEnabled();
        if (noClusterProp != null && Boolean.parseBoolean(noClusterProp)) {
            config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        }
        if (nodeConfig.getPublicAddress() != null && !nodeConfig.getPublicAddress().isEmpty()) {
            nConfig.setPublicAddress(nodeConfig.getPublicAddress());
        }

        if (!configuration.getInterface().isEmpty()) {
            // add an interfaces configuration
           String[] interfaceNames = configuration.getInterface().split(",");
            for (String interfaceName : interfaceNames) {
                nConfig.getInterfaces().addInterface(interfaceName.trim());
            }
            nConfig.getInterfaces().setEnabled(true);
        } else {
            MemberAddressProviderConfig memberAddressProviderConfig = nConfig.getMemberAddressProviderConfig();
            memberAddressProviderConfig.setEnabled(enabled);
            memberAddressProviderConfig.setImplementation(new MemberAddressPicker(env, configuration, nodeConfig));
        }

        int port = Integer.parseInt(configuration.getStartPort());

        String configSpecificPort = nodeConfig.getConfigSpecificDataGridStartPort();
        if (configSpecificPort != null && !configSpecificPort.isEmpty()) {
            // Setting it equal to zero will be the same as null or empty (to maintain backwards compatibility)
            int configSpecificPortInt = Integer.parseInt(configSpecificPort);
            if (configSpecificPortInt != 0) {
                port = configSpecificPortInt;
            }
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
            mcConfig.setMulticastPort(Integer.parseInt(configuration.getMulticastPort()));
        } else if (discoveryMode.startsWith("kubernetes")) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getKubernetesConfig().setEnabled(true)
                    .setProperty(KubernetesProperties.NAMESPACE.key(), configuration.getKubernetesNamespace())
                    .setProperty(KubernetesProperties.SERVICE_NAME.key(), configuration.getKubernetesServiceName())
                    .setProperty(KubernetesProperties.SERVICE_PORT.key(), valueOf(port));
        } else {
            //build the domain discovery config
            config.setProperty("hazelcast.discovery.enabled", "true");
            config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(
                    new DiscoveryStrategyConfig(DomainDiscoveryStrategy.class.getName())
                            .addProperty(HOST_AWARE_PARTITIONING.key(), hostAwarePartitioning));
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            if (Boolean.parseBoolean(System.getProperty("hazelcast.auto-partition-group", "true"))) {
                PartitionGroupConfig partitionGroupConfig = config.getPartitionGroupConfig();
                partitionGroupConfig.setEnabled(true);
                partitionGroupConfig.setGroupType(PartitionGroupConfig.MemberGroupType.SPI);
            }
        }

        if (env.isDas() && !env.isMicro()) {
            port = Integer.parseInt(configuration.getDasPort());
        }

        config.getNetworkConfig().setPort(port);
        config.getNetworkConfig().setPortAutoIncrement("true".equalsIgnoreCase(configuration.getAutoIncrementPort()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    private synchronized void bootstrapHazelcast() {
        if (!booted && isEnabled()) {
            Config config = buildConfiguration();
            Logger cpSubsystemLogger = Logger.getLogger("com.hazelcast.cp.CPSubsystem");
            Level cpSubsystemLevel = cpSubsystemLogger.getLevel();
            try {
                // remove "CP Subsystem Unsafe" warning on startup
                cpSubsystemLogger.setLevel(Level.SEVERE);
                config.getMemberAttributeConfig().setAttribute(INSTANCE_GROUP_ATTRIBUTE, memberGroup);
                theInstance = Hazelcast.newHazelcastInstance(config);
                autoPromoteCPMembers(config);
            } finally {
                cpSubsystemLogger.setLevel(cpSubsystemLevel);
            }
            if (env.isMicro()) {
                if (Boolean.valueOf(configuration.getGenerateNames()) || memberName == null) {
                    memberName = PayaraMicroNameGenerator.generateName();
                    Set<com.hazelcast.cluster.Member> clusterMembers = theInstance.getCluster().getMembers();

                    // If the instance name was generated, we need to compile a list of all the instance names in use within
                    // the instance group, excluding this local instance
                    List<String> takenNames = new ArrayList<>();
                    for (com.hazelcast.cluster.Member member : clusterMembers) {
                        Map<String, String> attributes = getAttributes(member.getUuid());
                        if (member != theInstance.getCluster().getLocalMember()
                                && attributes.get(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE) != null
                                && attributes.get(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE).equalsIgnoreCase(memberGroup)) {
                            takenNames.add(attributes.get(HazelcastCore.INSTANCE_ATTRIBUTE));
                        }
                    }

                    // If our generated name is already in use within the instance group, either generate a new one or set the
                    // name to this instance's UUID if there are no more unique generated options left
                    if (takenNames.contains(memberName)) {
                        memberName = PayaraMicroNameGenerator.generateUniqueName(takenNames,
                                theInstance.getCluster().getLocalMember().getUuid());
                        setAttribute(theInstance.getCluster().getLocalMember().getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE, memberName);
                    }
                }
            }
            setAttribute(theInstance.getCluster().getLocalMember().getUuid(), INSTANCE_ATTRIBUTE, memberName);
            setAttribute(theInstance.getCluster().getLocalMember().getUuid(), INSTANCE_GROUP_ATTRIBUTE, memberGroup);
            hazelcastCachingProvider = new HazelcastServerCachingProvider(theInstance);
            bindToJNDI();
            if(env.getStatus() == Status.started) {
                // only issue this event if the server is already running,
                // otherwise the SERVER_STARTUP event will issue this event as well
                events.send(new Event(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE));
            }
            booted = true;
        }
    }

    public String getAttribute(UUID memberUUID, String key) {
        return getAttributes(memberUUID).get(key);
    }

    public void setAttribute(UUID memberUUID, String key, String value) {
        IMap<UUID, Map<String, String>> instanceAttributeMap = theInstance.getMap(INSTANCE_ATTRIBUTE_MAP);
        instanceAttributeMap.compute(memberUUID,
                (uuid, map) -> {
                    if (map == null) {
                        map = new HashMap<>();
                    }
                    map.put(HazelcastCore.INSTANCE_ATTRIBUTE, memberName);
                    return map;
                });
    }

    public Map<String, String> getAttributes(UUID memberUUID) {
        IMap<UUID, Map<String, String>> instanceAttributeMap = theInstance.getMap(INSTANCE_ATTRIBUTE_MAP);
        return instanceAttributeMap.getOrDefault(memberUUID, Collections.unmodifiableMap(new TreeMap<>()));
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
        List<UnprocessedChangeEvent> unprocessedChanges = new ArrayList<>();
        for (PropertyChangeEvent pce : pces) {
            if (pce.getPropertyName().equalsIgnoreCase("datagrid-encryption-enabled")
                    && Boolean.parseBoolean(pce.getOldValue().toString()) != Boolean.parseBoolean(pce.getNewValue().toString())) {
                unprocessedChanges.add(new UnprocessedChangeEvent(pce, "Hazelcast encryption settings changed"));
            }
        }

        if (unprocessedChanges.isEmpty()) {
            return null;
        }
        return new UnprocessedChangeEvents(unprocessedChanges);
    }

    public boolean isDatagridEncryptionEnabled() {
        // We want to return the value as it was at boot time here to prevent the server changing encryption behaviour
        // without a restart
        return datagridEncryptionValue;
    }

    private void fillHazelcastConfigurationFromConfig(Config config,
                                                      HazelcastRuntimeConfiguration configuration) {
        configuration.setClusterGroupName(config.getClusterName());
        configuration.setLicenseKey(config.getLicenseKey());

        if (env.isDas() && !env.isMicro()) {
            configuration.setDasPort(String.valueOf(config.getNetworkConfig().getPort()));
        } else {
            configuration.setStartPort(String.valueOf(config.getNetworkConfig().getPort()));
        }
        configuration.setAutoIncrementPort(String.valueOf(config.getNetworkConfig().isPortAutoIncrement()));

        NetworkConfig nConfig = config.getNetworkConfig();
        InterfacesConfig interfacesConfig = nConfig.getInterfaces();
        if (interfacesConfig != null) {
            configuration.setInterface(
                    interfacesConfig.getInterfaces().stream().collect(Collectors.joining(",")));
        }
        JoinConfig joinConfig = nConfig.getJoin();
        if (joinConfig != null) {
            TcpIpConfig tConfig = joinConfig.getTcpIpConfig();
            if (tConfig != null && tConfig.isEnabled()) {
                configuration.setTcpipMembers(tConfig.getMembers().stream().collect(Collectors.joining(",")));
            }
            MulticastConfig multicastConfig = joinConfig.getMulticastConfig();
            if (multicastConfig != null && multicastConfig.isEnabled()) {
                configuration.setMulticastGroup(multicastConfig.getMulticastGroup());
                configuration.setMulticastPort(String.valueOf(multicastConfig.getMulticastPort()));
            }
            KubernetesConfig kubernetesConfig = joinConfig.getKubernetesConfig();
            if (kubernetesConfig != null && kubernetesConfig.isEnabled()) {
                configuration.setKubernetesNamespace(kubernetesConfig.getProperty(KubernetesProperties.NAMESPACE.key()));
                configuration.setKubernetesServiceName(kubernetesConfig.getProperty(KubernetesProperties.SERVICE_NAME.key()));
            }
        }
    }

    private void fillSpecificHazelcastConfigFromConfig(Config config,
                                                       HazelcastConfigSpecificConfiguration nodeConfig) {
        NetworkConfig nConfig = config.getNetworkConfig();
        if(nConfig.getPublicAddress() != null && !nConfig.getPublicAddress().isEmpty()) {
            nodeConfig.setPublicAddress(nConfig.getPublicAddress());
        }
        nodeConfig.setLite(String.valueOf(config.isLiteMember()));
        ExecutorConfig executorConfig = config.getExecutorConfig(CLUSTER_EXECUTOR_SERVICE_NAME);
        nodeConfig.setExecutorPoolSize(String.valueOf(executorConfig.getPoolSize()));
        nodeConfig.setExecutorQueueCapacity(String.valueOf(executorConfig.getQueueCapacity()));
        ScheduledExecutorConfig scheduledExecutorConfig = config.getScheduledExecutorConfig(
                SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
        nodeConfig.setScheduledExecutorPoolSize(String.valueOf(scheduledExecutorConfig.getPoolSize()));
        nodeConfig.setScheduledExecutorQueueCapacity(String.valueOf(scheduledExecutorConfig.getCapacity()));
    }

    private void fillConfigurationWithDefaults() throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<>() {
            @Override
            public Object run(final HazelcastRuntimeConfiguration hazelcastRuntimeConfiguration) {
                hazelcastRuntimeConfiguration.setChangeToDefault("false");
                hazelcastRuntimeConfiguration.setClusterGroupName("development");
                hazelcastRuntimeConfiguration.setLicenseKey("");
                hazelcastRuntimeConfiguration.setDasPort("4900");
                hazelcastRuntimeConfiguration.setStartPort("5900");
                hazelcastRuntimeConfiguration.setAutoIncrementPort("true");
                hazelcastRuntimeConfiguration.setInterface("");
                hazelcastRuntimeConfiguration.setTcpipMembers("127.0.0.1:5900");
                hazelcastRuntimeConfiguration.setMulticastGroup("224.2.2.3");
                hazelcastRuntimeConfiguration.setMulticastPort("54327");
                hazelcastRuntimeConfiguration.setKubernetesNamespace("default");
                hazelcastRuntimeConfiguration.setKubernetesServiceName("");
                Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO,
                        "Hazelcast general configuration filled with defaults");
                return null;
            }
        }, configuration);
        ConfigSupport.apply(new SingleConfigCode<>() {
            @Override
            public Object run(final HazelcastConfigSpecificConfiguration hazelcastConfigSpecificConfiguration) {
                hazelcastConfigSpecificConfiguration.setPublicAddress("");
                hazelcastConfigSpecificConfiguration.setLite("false");
                hazelcastConfigSpecificConfiguration.setExecutorPoolSize("4");
                hazelcastConfigSpecificConfiguration.setExecutorQueueCapacity("20");
                hazelcastConfigSpecificConfiguration.setScheduledExecutorPoolSize("4");
                hazelcastConfigSpecificConfiguration.setScheduledExecutorQueueCapacity("20");
                Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO,
                        "Hazelcast specific configuration filled with defaults");
                return null;
            }
        }, nodeConfig);
    }

    private void autoPromoteCPMembers(Config config) {
        final String availabilityStructureName = "Payara/cluster/cp/availability";
        String waitBeforeJoinStr = config.getProperty(ClusterProperty.WAIT_SECONDS_BEFORE_JOIN.getName());
        if (waitBeforeJoinStr == null) {
            waitBeforeJoinStr = ClusterProperty.WAIT_SECONDS_BEFORE_JOIN.getDefaultValue();
        }
        final int waitBeforeJoin = Math.max(5, Integer.parseInt(waitBeforeJoinStr));

        String maxWaitBeforeJoinStr = config.getProperty(ClusterProperty.MAX_WAIT_SECONDS_BEFORE_JOIN.getName());
        if (maxWaitBeforeJoinStr == null) {
            maxWaitBeforeJoinStr = ClusterProperty.MAX_WAIT_SECONDS_BEFORE_JOIN.getDefaultValue();
        }
        final int maxWaitBeforeJoin = Integer.parseInt(maxWaitBeforeJoinStr) * 10 * 2;

        if (!config.isLiteMember() && config.getCPSubsystemConfig().getCPMemberCount() > 0 && Boolean.parseBoolean(
                System.getProperty("hazelcast.cp-subsystem.auto-promote", "true"))) {
            theInstance.getCPSubsystem().addMembershipListener(new CPMembershipListener() {
                @Override
                public void memberAdded(CPMembershipEvent cpMembershipEvent) {
                    theInstance.getMap(availabilityStructureName).remove(cpMembershipEvent.getMember().getAddress());
                }

                @Override
                public void memberRemoved(CPMembershipEvent cpMembershipEvent) {
                    try {
                        if (!cpMembershipEvent.getMember().equals(theInstance.getCPSubsystem()
                                .getCPSubsystemManagementService().getLocalCPMember())) {
                            theInstance.getCPSubsystem().getCPSubsystemManagementService()
                                    .getCPGroup(METADATA_CP_GROUP_NAME).toCompletableFuture()
                                    .get(waitBeforeJoin, TimeUnit.SECONDS);
                        }
                    } catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {
                        if (e.getCause() instanceof IllegalStateException) {
                            theInstance.getSet(availabilityStructureName).add(theInstance.getCluster().getLocalMember());
                        }
                    }
                }
            });
            theInstance.getCPSubsystem().addGroupAvailabilityListener(new CPGroupAvailabilityListener() {
                @Override
                public void availabilityDecreased(CPGroupAvailabilityEvent cpGroupAvailabilityEvent) {
                    if (cpGroupAvailabilityEvent.isMetadataGroup()) {
                        var map = theInstance.getMap(availabilityStructureName);
                        cpGroupAvailabilityEvent.getUnavailableMembers().forEach(member -> {
                            map.put(member.getAddress(), member.getUuid());
                        });
                    }
                }

                @Override
                public void majorityLost(CPGroupAvailabilityEvent cpGroupAvailabilityEvent) {
                    if (cpGroupAvailabilityEvent.isMetadataGroup()) {
                        theInstance.getSet(availabilityStructureName).add(theInstance.getCluster().getLocalMember());
                    }
                }
            });

            var cpManagementService = theInstance.getCPSubsystem().getCPSubsystemManagementService();
            if (cpManagementService.isDiscoveryCompleted()) {
                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        for (int ii = 0; ii < maxWaitBeforeJoin; ++ii) {
                            if (theInstance.getCluster().getClusterState() == ClusterState.ACTIVE) {
                                break;
                            }
                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                        sendCPResetToMaster(availabilityStructureName, waitBeforeJoin);

                        var localMember = theInstance.getCluster().getLocalMember();
                        IMap<Address, UUID> map = theInstance.getMap(availabilityStructureName);
                        UUID uuid = map.get(localMember.getAddress());
                        if (uuid != null || cpManagementService.getCPMembers().toCompletableFuture().join()
                                .size() < config.getCPSubsystemConfig().getCPMemberCount()) {
                            if (uuid != null) {
                                try {
                                    cpManagementService.removeCPMember(uuid).toCompletableFuture().join();
                                } catch (CompletionException e) {
                                }
                                map.remove(localMember.getAddress());
                            }
                            cpManagementService.promoteToCPMember();
                            Logger.getLogger(HazelcastCore.class.getName()).log(Level.INFO, "Instance Promoted into CP Subsystem");
                        }
                    } catch (HazelcastInstanceNotActiveException e) {
                    } catch (Exception exc) {
                        if (exc.getCause() instanceof CPGroupDestroyedException) { }
                        else {
                            Logger.getLogger(HazelcastCore.class.getName()).log(Level.WARNING, "Auto CP Promotion Failure", exc);
                        }
                    }
                });
            }
        }
    }

    private void sendCPResetToMaster(String availabilityStructureName, int waitBeforeJoin) {
        ISet<Member> cpMembersToReset = theInstance.getSet(availabilityStructureName);
        if (!cpMembersToReset.isEmpty()) {
            var fn = (Serializable & Runnable) () -> {
                theCore.cpResetLock.lock();
                try {
                    if (theCore.lastResetTime.get().plusSeconds(waitBeforeJoin).isAfter(Instant.now())) {
                        return;
                    }
                    try {
                        theCore.theInstance.getCPSubsystem().getCPSubsystemManagementService()
                                .getCPGroup(METADATA_CP_GROUP_NAME).toCompletableFuture().get(waitBeforeJoin, TimeUnit.SECONDS);
                    } catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {
                        theCore.theInstance.getCPSubsystem().getCPSubsystemManagementService().reset().toCompletableFuture().join();
                        theCore.lastResetTime.set(Instant.now());
                    }
                    theCore.theInstance.getSet(availabilityStructureName).clear();
                } catch (Exception exc) {
                    Logger.getLogger(HazelcastCore.class.getName()).log(Level.FINE, "Auto CP Reset Failure", exc);
                }
                finally {
                    theCore.cpResetLock.unlock();
                }
            };
            theInstance.getExecutorService(availabilityStructureName).executeOnMembers(fn, cpMembersToReset);
        }
    }
}
