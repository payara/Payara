
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import java.math.BigInteger;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fish.payara.nucleus.hazelcast.xsd package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Keystore_QNAME = new QName("http://www.hazelcast.com/schema/config", "keystore");
    private final static QName _FactoryOrClassName_QNAME = new QName("http://www.hazelcast.com/schema/config", "factory-or-class-name");
    private final static QName _ClassName_QNAME = new QName("http://www.hazelcast.com/schema/config", "class-name");
    private final static QName _SecureStoreSubstitutionGroup_QNAME = new QName("http://www.hazelcast.com/schema/config", "secure-store-substitution-group");
    private final static QName _Vault_QNAME = new QName("http://www.hazelcast.com/schema/config", "vault");
    private final static QName _RecentlyActiveSplitBrainProtection_QNAME = new QName("http://www.hazelcast.com/schema/config", "recently-active-split-brain-protection");
    private final static QName _FactoryClassName_QNAME = new QName("http://www.hazelcast.com/schema/config", "factory-class-name");
    private final static QName _MemberCountSplitBrainProtection_QNAME = new QName("http://www.hazelcast.com/schema/config", "member-count-split-brain-protection");
    private final static QName _ChoiceOfSplitBrainProtectionFunction_QNAME = new QName("http://www.hazelcast.com/schema/config", "choice-of-split-brain-protection-function");
    private final static QName _ProbabilisticSplitBrainProtection_QNAME = new QName("http://www.hazelcast.com/schema/config", "probabilistic-split-brain-protection");
    private final static QName _TcpIpMemberList_QNAME = new QName("http://www.hazelcast.com/schema/config", "member-list");
    private final static QName _TcpIpMember_QNAME = new QName("http://www.hazelcast.com/schema/config", "member");
    private final static QName _TcpIpInterface_QNAME = new QName("http://www.hazelcast.com/schema/config", "interface");
    private final static QName _TcpIpMembers_QNAME = new QName("http://www.hazelcast.com/schema/config", "members");
    private final static QName _TcpIpRequiredMember_QNAME = new QName("http://www.hazelcast.com/schema/config", "required-member");
    private final static QName _PermissionsConfigPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "config-permission");
    private final static QName _PermissionsAllPermissions_QNAME = new QName("http://www.hazelcast.com/schema/config", "all-permissions");
    private final static QName _PermissionsReplicatedmapPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "replicatedmap-permission");
    private final static QName _PermissionsCachePermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "cache-permission");
    private final static QName _PermissionsMultimapPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "multimap-permission");
    private final static QName _PermissionsScheduledExecutorPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "scheduled-executor-permission");
    private final static QName _PermissionsMapPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "map-permission");
    private final static QName _PermissionsCardinalityEstimatorPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "cardinality-estimator-permission");
    private final static QName _PermissionsDurableExecutorServicePermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "durable-executor-service-permission");
    private final static QName _PermissionsReliableTopicPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "reliable-topic-permission");
    private final static QName _PermissionsManagementPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "management-permission");
    private final static QName _PermissionsPnCounterPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "pn-counter-permission");
    private final static QName _PermissionsSetPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "set-permission");
    private final static QName _PermissionsLockPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "lock-permission");
    private final static QName _PermissionsTransactionPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "transaction-permission");
    private final static QName _PermissionsQueuePermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "queue-permission");
    private final static QName _PermissionsRingBufferPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "ring-buffer-permission");
    private final static QName _PermissionsCountdownLatchPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "countdown-latch-permission");
    private final static QName _PermissionsSemaphorePermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "semaphore-permission");
    private final static QName _PermissionsExecutorServicePermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "executor-service-permission");
    private final static QName _PermissionsAtomicLongPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "atomic-long-permission");
    private final static QName _PermissionsAtomicReferencePermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "atomic-reference-permission");
    private final static QName _PermissionsUserCodeDeploymentPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "user-code-deployment-permission");
    private final static QName _PermissionsListPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "list-permission");
    private final static QName _PermissionsTopicPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "topic-permission");
    private final static QName _PermissionsFlakeIdGeneratorPermission_QNAME = new QName("http://www.hazelcast.com/schema/config", "flake-id-generator-permission");
    private final static QName _KerberosAuthenticationSecurityRealm_QNAME = new QName("http://www.hazelcast.com/schema/config", "security-realm");
    private final static QName _KerberosAuthenticationRelaxFlagsCheck_QNAME = new QName("http://www.hazelcast.com/schema/config", "relax-flags-check");
    private final static QName _KerberosAuthenticationKeytabFile_QNAME = new QName("http://www.hazelcast.com/schema/config", "keytab-file");
    private final static QName _KerberosAuthenticationPrincipal_QNAME = new QName("http://www.hazelcast.com/schema/config", "principal");
    private final static QName _KerberosAuthenticationLdap_QNAME = new QName("http://www.hazelcast.com/schema/config", "ldap");
    private final static QName _KerberosAuthenticationUseNameWithoutRealm_QNAME = new QName("http://www.hazelcast.com/schema/config", "use-name-without-realm");
    private final static QName _HazelcastConfigReplacers_QNAME = new QName("http://www.hazelcast.com/schema/config", "config-replacers");
    private final static QName _HazelcastWanReplication_QNAME = new QName("http://www.hazelcast.com/schema/config", "wan-replication");
    private final static QName _HazelcastPnCounter_QNAME = new QName("http://www.hazelcast.com/schema/config", "pn-counter");
    private final static QName _HazelcastCpSubsystem_QNAME = new QName("http://www.hazelcast.com/schema/config", "cp-subsystem");
    private final static QName _HazelcastMap_QNAME = new QName("http://www.hazelcast.com/schema/config", "map");
    private final static QName _HazelcastPartitionGroup_QNAME = new QName("http://www.hazelcast.com/schema/config", "partition-group");
    private final static QName _HazelcastSecurity_QNAME = new QName("http://www.hazelcast.com/schema/config", "security");
    private final static QName _HazelcastScheduledExecutorService_QNAME = new QName("http://www.hazelcast.com/schema/config", "scheduled-executor-service");
    private final static QName _HazelcastInstanceName_QNAME = new QName("http://www.hazelcast.com/schema/config", "instance-name");
    private final static QName _HazelcastNetwork_QNAME = new QName("http://www.hazelcast.com/schema/config", "network");
    private final static QName _HazelcastAdvancedNetwork_QNAME = new QName("http://www.hazelcast.com/schema/config", "advanced-network");
    private final static QName _HazelcastSql_QNAME = new QName("http://www.hazelcast.com/schema/config", "sql");
    private final static QName _HazelcastNativeMemory_QNAME = new QName("http://www.hazelcast.com/schema/config", "native-memory");
    private final static QName _HazelcastUserCodeDeployment_QNAME = new QName("http://www.hazelcast.com/schema/config", "user-code-deployment");
    private final static QName _HazelcastReliableTopic_QNAME = new QName("http://www.hazelcast.com/schema/config", "reliable-topic");
    private final static QName _HazelcastDurableExecutorService_QNAME = new QName("http://www.hazelcast.com/schema/config", "durable-executor-service");
    private final static QName _HazelcastMultimap_QNAME = new QName("http://www.hazelcast.com/schema/config", "multimap");
    private final static QName _HazelcastCrdtReplication_QNAME = new QName("http://www.hazelcast.com/schema/config", "crdt-replication");
    private final static QName _HazelcastMetrics_QNAME = new QName("http://www.hazelcast.com/schema/config", "metrics");
    private final static QName _HazelcastFlakeIdGenerator_QNAME = new QName("http://www.hazelcast.com/schema/config", "flake-id-generator");
    private final static QName _HazelcastTopic_QNAME = new QName("http://www.hazelcast.com/schema/config", "topic");
    private final static QName _HazelcastInstanceTracking_QNAME = new QName("http://www.hazelcast.com/schema/config", "instance-tracking");
    private final static QName _HazelcastProperties_QNAME = new QName("http://www.hazelcast.com/schema/config", "properties");
    private final static QName _HazelcastQueue_QNAME = new QName("http://www.hazelcast.com/schema/config", "queue");
    private final static QName _HazelcastHotRestartPersistence_QNAME = new QName("http://www.hazelcast.com/schema/config", "hot-restart-persistence");
    private final static QName _HazelcastManagementCenter_QNAME = new QName("http://www.hazelcast.com/schema/config", "management-center");
    private final static QName _HazelcastSerialization_QNAME = new QName("http://www.hazelcast.com/schema/config", "serialization");
    private final static QName _HazelcastLiteMember_QNAME = new QName("http://www.hazelcast.com/schema/config", "lite-member");
    private final static QName _HazelcastExecutorService_QNAME = new QName("http://www.hazelcast.com/schema/config", "executor-service");
    private final static QName _HazelcastRingbuffer_QNAME = new QName("http://www.hazelcast.com/schema/config", "ringbuffer");
    private final static QName _HazelcastCardinalityEstimator_QNAME = new QName("http://www.hazelcast.com/schema/config", "cardinality-estimator");
    private final static QName _HazelcastLicenseKey_QNAME = new QName("http://www.hazelcast.com/schema/config", "license-key");
    private final static QName _HazelcastReplicatedmap_QNAME = new QName("http://www.hazelcast.com/schema/config", "replicatedmap");
    private final static QName _HazelcastSplitBrainProtection_QNAME = new QName("http://www.hazelcast.com/schema/config", "split-brain-protection");
    private final static QName _HazelcastClusterName_QNAME = new QName("http://www.hazelcast.com/schema/config", "cluster-name");
    private final static QName _HazelcastList_QNAME = new QName("http://www.hazelcast.com/schema/config", "list");
    private final static QName _HazelcastSet_QNAME = new QName("http://www.hazelcast.com/schema/config", "set");
    private final static QName _HazelcastCache_QNAME = new QName("http://www.hazelcast.com/schema/config", "cache");
    private final static QName _HazelcastListeners_QNAME = new QName("http://www.hazelcast.com/schema/config", "listeners");
    private final static QName _HazelcastAuditlog_QNAME = new QName("http://www.hazelcast.com/schema/config", "auditlog");
    private final static QName _LdapAuthenticationRoleSearchScope_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-search-scope");
    private final static QName _LdapAuthenticationRoleNameAttribute_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-name-attribute");
    private final static QName _LdapAuthenticationPasswordAttribute_QNAME = new QName("http://www.hazelcast.com/schema/config", "password-attribute");
    private final static QName _LdapAuthenticationSocketFactoryClassName_QNAME = new QName("http://www.hazelcast.com/schema/config", "socket-factory-class-name");
    private final static QName _LdapAuthenticationSystemUserPassword_QNAME = new QName("http://www.hazelcast.com/schema/config", "system-user-password");
    private final static QName _LdapAuthenticationSkipAuthentication_QNAME = new QName("http://www.hazelcast.com/schema/config", "skip-authentication");
    private final static QName _LdapAuthenticationUserFilter_QNAME = new QName("http://www.hazelcast.com/schema/config", "user-filter");
    private final static QName _LdapAuthenticationUserSearchScope_QNAME = new QName("http://www.hazelcast.com/schema/config", "user-search-scope");
    private final static QName _LdapAuthenticationRoleContext_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-context");
    private final static QName _LdapAuthenticationRoleFilter_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-filter");
    private final static QName _LdapAuthenticationParseDn_QNAME = new QName("http://www.hazelcast.com/schema/config", "parse-dn");
    private final static QName _LdapAuthenticationRoleMappingAttribute_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-mapping-attribute");
    private final static QName _LdapAuthenticationRoleRecursionMaxDepth_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-recursion-max-depth");
    private final static QName _LdapAuthenticationUrl_QNAME = new QName("http://www.hazelcast.com/schema/config", "url");
    private final static QName _LdapAuthenticationUserNameAttribute_QNAME = new QName("http://www.hazelcast.com/schema/config", "user-name-attribute");
    private final static QName _LdapAuthenticationRoleMappingMode_QNAME = new QName("http://www.hazelcast.com/schema/config", "role-mapping-mode");
    private final static QName _LdapAuthenticationUserContext_QNAME = new QName("http://www.hazelcast.com/schema/config", "user-context");
    private final static QName _LdapAuthenticationSystemUserDn_QNAME = new QName("http://www.hazelcast.com/schema/config", "system-user-dn");
    private final static QName _LdapAuthenticationSystemAuthentication_QNAME = new QName("http://www.hazelcast.com/schema/config", "system-authentication");
    private final static QName _FilterListClass_QNAME = new QName("http://www.hazelcast.com/schema/config", "class");
    private final static QName _FilterListPackage_QNAME = new QName("http://www.hazelcast.com/schema/config", "package");
    private final static QName _FilterListPrefix_QNAME = new QName("http://www.hazelcast.com/schema/config", "prefix");
    private final static QName _WanBatchPublisherAcknowledgeType_QNAME = new QName("http://www.hazelcast.com/schema/config", "acknowledge-type");
    private final static QName _WanBatchPublisherAzure_QNAME = new QName("http://www.hazelcast.com/schema/config", "azure");
    private final static QName _WanBatchPublisherEndpoint_QNAME = new QName("http://www.hazelcast.com/schema/config", "endpoint");
    private final static QName _WanBatchPublisherGcp_QNAME = new QName("http://www.hazelcast.com/schema/config", "gcp");
    private final static QName _WanBatchPublisherKubernetes_QNAME = new QName("http://www.hazelcast.com/schema/config", "kubernetes");
    private final static QName _WanBatchPublisherEureka_QNAME = new QName("http://www.hazelcast.com/schema/config", "eureka");
    private final static QName _WanBatchPublisherQueueFullBehavior_QNAME = new QName("http://www.hazelcast.com/schema/config", "queue-full-behavior");
    private final static QName _WanBatchPublisherMaxConcurrentInvocations_QNAME = new QName("http://www.hazelcast.com/schema/config", "max-concurrent-invocations");
    private final static QName _WanBatchPublisherIdleMaxParkNs_QNAME = new QName("http://www.hazelcast.com/schema/config", "idle-max-park-ns");
    private final static QName _WanBatchPublisherAws_QNAME = new QName("http://www.hazelcast.com/schema/config", "aws");
    private final static QName _WanBatchPublisherPublisherId_QNAME = new QName("http://www.hazelcast.com/schema/config", "publisher-id");
    private final static QName _WanBatchPublisherTargetEndpoints_QNAME = new QName("http://www.hazelcast.com/schema/config", "target-endpoints");
    private final static QName _WanBatchPublisherBatchSize_QNAME = new QName("http://www.hazelcast.com/schema/config", "batch-size");
    private final static QName _WanBatchPublisherResponseTimeoutMillis_QNAME = new QName("http://www.hazelcast.com/schema/config", "response-timeout-millis");
    private final static QName _WanBatchPublisherBatchMaxDelayMillis_QNAME = new QName("http://www.hazelcast.com/schema/config", "batch-max-delay-millis");
    private final static QName _WanBatchPublisherIdleMinParkNs_QNAME = new QName("http://www.hazelcast.com/schema/config", "idle-min-park-ns");
    private final static QName _WanBatchPublisherSync_QNAME = new QName("http://www.hazelcast.com/schema/config", "sync");
    private final static QName _WanBatchPublisherQueueCapacity_QNAME = new QName("http://www.hazelcast.com/schema/config", "queue-capacity");
    private final static QName _WanBatchPublisherDiscoveryStrategies_QNAME = new QName("http://www.hazelcast.com/schema/config", "discovery-strategies");
    private final static QName _WanBatchPublisherMaxTargetEndpoints_QNAME = new QName("http://www.hazelcast.com/schema/config", "max-target-endpoints");
    private final static QName _WanBatchPublisherUseEndpointPrivateAddress_QNAME = new QName("http://www.hazelcast.com/schema/config", "use-endpoint-private-address");
    private final static QName _WanBatchPublisherInitialPublisherState_QNAME = new QName("http://www.hazelcast.com/schema/config", "initial-publisher-state");
    private final static QName _WanBatchPublisherDiscoveryPeriodSeconds_QNAME = new QName("http://www.hazelcast.com/schema/config", "discovery-period-seconds");
    private final static QName _WanBatchPublisherSnapshotEnabled_QNAME = new QName("http://www.hazelcast.com/schema/config", "snapshot-enabled");
    private final static QName _AdvancedNetworkJoin_QNAME = new QName("http://www.hazelcast.com/schema/config", "join");
    private final static QName _AdvancedNetworkMemberAddressProvider_QNAME = new QName("http://www.hazelcast.com/schema/config", "member-address-provider");
    private final static QName _AdvancedNetworkRestServerSocketEndpointConfig_QNAME = new QName("http://www.hazelcast.com/schema/config", "rest-server-socket-endpoint-config");
    private final static QName _AdvancedNetworkFailureDetector_QNAME = new QName("http://www.hazelcast.com/schema/config", "failure-detector");
    private final static QName _AdvancedNetworkWanServerSocketEndpointConfig_QNAME = new QName("http://www.hazelcast.com/schema/config", "wan-server-socket-endpoint-config");
    private final static QName _AdvancedNetworkClientServerSocketEndpointConfig_QNAME = new QName("http://www.hazelcast.com/schema/config", "client-server-socket-endpoint-config");
    private final static QName _AdvancedNetworkWanEndpointConfig_QNAME = new QName("http://www.hazelcast.com/schema/config", "wan-endpoint-config");
    private final static QName _AdvancedNetworkMemcacheServerSocketEndpointConfig_QNAME = new QName("http://www.hazelcast.com/schema/config", "memcache-server-socket-endpoint-config");
    private final static QName _AdvancedNetworkMemberServerSocketEndpointConfig_QNAME = new QName("http://www.hazelcast.com/schema/config", "member-server-socket-endpoint-config");
    private final static QName _WanReplicationBatchPublisher_QNAME = new QName("http://www.hazelcast.com/schema/config", "batch-publisher");
    private final static QName _WanReplicationConsumer_QNAME = new QName("http://www.hazelcast.com/schema/config", "consumer");
    private final static QName _WanReplicationCustomPublisher_QNAME = new QName("http://www.hazelcast.com/schema/config", "custom-publisher");
    private final static QName _BasicAuthenticationSkipEndpoint_QNAME = new QName("http://www.hazelcast.com/schema/config", "skip-endpoint");
    private final static QName _BasicAuthenticationSkipRole_QNAME = new QName("http://www.hazelcast.com/schema/config", "skip-role");
    private final static QName _BasicAuthenticationSkipIdentity_QNAME = new QName("http://www.hazelcast.com/schema/config", "skip-identity");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fish.payara.nucleus.hazelcast.xsd
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PersistentMemory }
     * 
     */
    public PersistentMemory createPersistentMemory() {
        return new PersistentMemory();
    }

    /**
     * Create an instance of {@link CacheEntryListener }
     * 
     */
    public CacheEntryListener createCacheEntryListener() {
        return new CacheEntryListener();
    }

    /**
     * Create an instance of {@link BasePermission }
     * 
     */
    public BasePermission createBasePermission() {
        return new BasePermission();
    }

    /**
     * Create an instance of {@link QueryCache }
     * 
     */
    public QueryCache createQueryCache() {
        return new QueryCache();
    }

    /**
     * Create an instance of {@link TcpIp }
     * 
     */
    public TcpIp createTcpIp() {
        return new TcpIp();
    }

    /**
     * Create an instance of {@link CpSubsystem }
     * 
     */
    public CpSubsystem createCpSubsystem() {
        return new CpSubsystem();
    }

    /**
     * Create an instance of {@link SplitBrainProtection }
     * 
     */
    public SplitBrainProtection createSplitBrainProtection() {
        return new SplitBrainProtection();
    }

    /**
     * Create an instance of {@link Serialization }
     * 
     */
    public Serialization createSerialization() {
        return new Serialization();
    }

    /**
     * Create an instance of {@link ReliableTopic }
     * 
     */
    public ReliableTopic createReliableTopic() {
        return new ReliableTopic();
    }

    /**
     * Create an instance of {@link Topic }
     * 
     */
    public Topic createTopic() {
        return new Topic();
    }

    /**
     * Create an instance of {@link Set }
     * 
     */
    public Set createSet() {
        return new Set();
    }

    /**
     * Create an instance of {@link List }
     * 
     */
    public List createList() {
        return new List();
    }

    /**
     * Create an instance of {@link Cache }
     * 
     */
    public Cache createCache() {
        return new Cache();
    }

    /**
     * Create an instance of {@link Map }
     * 
     */
    public Map createMap() {
        return new Map();
    }

    /**
     * Create an instance of {@link Queue }
     * 
     */
    public Queue createQueue() {
        return new Queue();
    }

    /**
     * Create an instance of {@link PartitionGroup }
     * 
     */
    public PartitionGroup createPartitionGroup() {
        return new PartitionGroup();
    }

    /**
     * Create an instance of {@link MemberCountSplitBrainProtection }
     * 
     */
    public MemberCountSplitBrainProtection createMemberCountSplitBrainProtection() {
        return new MemberCountSplitBrainProtection();
    }

    /**
     * Create an instance of {@link Import }
     * 
     */
    public Import createImport() {
        return new Import();
    }

    /**
     * Create an instance of {@link ProbabilisticSplitBrainProtection }
     * 
     */
    public ProbabilisticSplitBrainProtection createProbabilisticSplitBrainProtection() {
        return new ProbabilisticSplitBrainProtection();
    }

    /**
     * Create an instance of {@link MemberAttributes }
     * 
     */
    public MemberAttributes createMemberAttributes() {
        return new MemberAttributes();
    }

    /**
     * Create an instance of {@link Attribute }
     * 
     */
    public Attribute createAttribute() {
        return new Attribute();
    }

    /**
     * Create an instance of {@link QueueStore }
     * 
     */
    public QueueStore createQueueStore() {
        return new QueueStore();
    }

    /**
     * Create an instance of {@link Properties }
     * 
     */
    public Properties createProperties() {
        return new Properties();
    }

    /**
     * Create an instance of {@link Hazelcast }
     * 
     */
    public Hazelcast createHazelcast() {
        return new Hazelcast();
    }

    /**
     * Create an instance of {@link ConfigReplacers }
     * 
     */
    public ConfigReplacers createConfigReplacers() {
        return new ConfigReplacers();
    }

    /**
     * Create an instance of {@link ManagementCenter }
     * 
     */
    public ManagementCenter createManagementCenter() {
        return new ManagementCenter();
    }

    /**
     * Create an instance of {@link WanReplication }
     * 
     */
    public WanReplication createWanReplication() {
        return new WanReplication();
    }

    /**
     * Create an instance of {@link Network }
     * 
     */
    public Network createNetwork() {
        return new Network();
    }

    /**
     * Create an instance of {@link ExecutorService }
     * 
     */
    public ExecutorService createExecutorService() {
        return new ExecutorService();
    }

    /**
     * Create an instance of {@link DurableExecutorService }
     * 
     */
    public DurableExecutorService createDurableExecutorService() {
        return new DurableExecutorService();
    }

    /**
     * Create an instance of {@link ScheduledExecutorService }
     * 
     */
    public ScheduledExecutorService createScheduledExecutorService() {
        return new ScheduledExecutorService();
    }

    /**
     * Create an instance of {@link Multimap }
     * 
     */
    public Multimap createMultimap() {
        return new Multimap();
    }

    /**
     * Create an instance of {@link Replicatedmap }
     * 
     */
    public Replicatedmap createReplicatedmap() {
        return new Replicatedmap();
    }

    /**
     * Create an instance of {@link Ringbuffer }
     * 
     */
    public Ringbuffer createRingbuffer() {
        return new Ringbuffer();
    }

    /**
     * Create an instance of {@link Listeners }
     * 
     */
    public Listeners createListeners() {
        return new Listeners();
    }

    /**
     * Create an instance of {@link NativeMemory }
     * 
     */
    public NativeMemory createNativeMemory() {
        return new NativeMemory();
    }

    /**
     * Create an instance of {@link Security }
     * 
     */
    public Security createSecurity() {
        return new Security();
    }

    /**
     * Create an instance of {@link LiteMember }
     * 
     */
    public LiteMember createLiteMember() {
        return new LiteMember();
    }

    /**
     * Create an instance of {@link HotRestartPersistence }
     * 
     */
    public HotRestartPersistence createHotRestartPersistence() {
        return new HotRestartPersistence();
    }

    /**
     * Create an instance of {@link UserCodeDeployment }
     * 
     */
    public UserCodeDeployment createUserCodeDeployment() {
        return new UserCodeDeployment();
    }

    /**
     * Create an instance of {@link CardinalityEstimator }
     * 
     */
    public CardinalityEstimator createCardinalityEstimator() {
        return new CardinalityEstimator();
    }

    /**
     * Create an instance of {@link FlakeIdGenerator }
     * 
     */
    public FlakeIdGenerator createFlakeIdGenerator() {
        return new FlakeIdGenerator();
    }

    /**
     * Create an instance of {@link CrdtReplication }
     * 
     */
    public CrdtReplication createCrdtReplication() {
        return new CrdtReplication();
    }

    /**
     * Create an instance of {@link PnCounter }
     * 
     */
    public PnCounter createPnCounter() {
        return new PnCounter();
    }

    /**
     * Create an instance of {@link AdvancedNetwork }
     * 
     */
    public AdvancedNetwork createAdvancedNetwork() {
        return new AdvancedNetwork();
    }

    /**
     * Create an instance of {@link FactoryClassWithProperties }
     * 
     */
    public FactoryClassWithProperties createFactoryClassWithProperties() {
        return new FactoryClassWithProperties();
    }

    /**
     * Create an instance of {@link Metrics }
     * 
     */
    public Metrics createMetrics() {
        return new Metrics();
    }

    /**
     * Create an instance of {@link InstanceTracking }
     * 
     */
    public InstanceTracking createInstanceTracking() {
        return new InstanceTracking();
    }

    /**
     * Create an instance of {@link Sql }
     * 
     */
    public Sql createSql() {
        return new Sql();
    }

    /**
     * Create an instance of {@link Keystore }
     * 
     */
    public Keystore createKeystore() {
        return new Keystore();
    }

    /**
     * Create an instance of {@link RecentlyActiveSplitBrainProtection }
     * 
     */
    public RecentlyActiveSplitBrainProtection createRecentlyActiveSplitBrainProtection() {
        return new RecentlyActiveSplitBrainProtection();
    }

    /**
     * Create an instance of {@link Vault }
     * 
     */
    public Vault createVault() {
        return new Vault();
    }

    /**
     * Create an instance of {@link PartitionLostListener }
     * 
     */
    public PartitionLostListener createPartitionLostListener() {
        return new PartitionLostListener();
    }

    /**
     * Create an instance of {@link Icmp }
     * 
     */
    public Icmp createIcmp() {
        return new Icmp();
    }

    /**
     * Create an instance of {@link WanBatchPublisher }
     * 
     */
    public WanBatchPublisher createWanBatchPublisher() {
        return new WanBatchPublisher();
    }

    /**
     * Create an instance of {@link JavaSerializationFilter }
     * 
     */
    public JavaSerializationFilter createJavaSerializationFilter() {
        return new JavaSerializationFilter();
    }

    /**
     * Create an instance of {@link TlsAuthentication }
     * 
     */
    public TlsAuthentication createTlsAuthentication() {
        return new TlsAuthentication();
    }

    /**
     * Create an instance of {@link KerberosAuthentication }
     * 
     */
    public KerberosAuthentication createKerberosAuthentication() {
        return new KerberosAuthentication();
    }

    /**
     * Create an instance of {@link Predicate }
     * 
     */
    public Predicate createPredicate() {
        return new Predicate();
    }

    /**
     * Create an instance of {@link EntryListeners }
     * 
     */
    public EntryListeners createEntryListeners() {
        return new EntryListeners();
    }

    /**
     * Create an instance of {@link WanReplicationRefFilters }
     * 
     */
    public WanReplicationRefFilters createWanReplicationRefFilters() {
        return new WanReplicationRefFilters();
    }

    /**
     * Create an instance of {@link Permissions }
     * 
     */
    public Permissions createPermissions() {
        return new Permissions();
    }

    /**
     * Create an instance of {@link Property }
     * 
     */
    public Property createProperty() {
        return new Property();
    }

    /**
     * Create an instance of {@link Join }
     * 
     */
    public Join createJoin() {
        return new Join();
    }

    /**
     * Create an instance of {@link LdapAuthentication }
     * 
     */
    public LdapAuthentication createLdapAuthentication() {
        return new LdapAuthentication();
    }

    /**
     * Create an instance of {@link AliasedDiscoveryStrategy }
     * 
     */
    public AliasedDiscoveryStrategy createAliasedDiscoveryStrategy() {
        return new AliasedDiscoveryStrategy();
    }

    /**
     * Create an instance of {@link SocketInterceptor }
     * 
     */
    public SocketInterceptor createSocketInterceptor() {
        return new SocketInterceptor();
    }

    /**
     * Create an instance of {@link DiscoveryStrategy }
     * 
     */
    public DiscoveryStrategy createDiscoveryStrategy() {
        return new DiscoveryStrategy();
    }

    /**
     * Create an instance of {@link IndexAttributes }
     * 
     */
    public IndexAttributes createIndexAttributes() {
        return new IndexAttributes();
    }

    /**
     * Create an instance of {@link Index }
     * 
     */
    public Index createIndex() {
        return new Index();
    }

    /**
     * Create an instance of {@link EncryptionAtRest }
     * 
     */
    public EncryptionAtRest createEncryptionAtRest() {
        return new EncryptionAtRest();
    }

    /**
     * Create an instance of {@link ItemListener }
     * 
     */
    public ItemListener createItemListener() {
        return new ItemListener();
    }

    /**
     * Create an instance of {@link SecureStore }
     * 
     */
    public SecureStore createSecureStore() {
        return new SecureStore();
    }

    /**
     * Create an instance of {@link Semaphore }
     * 
     */
    public Semaphore createSemaphore() {
        return new Semaphore();
    }

    /**
     * Create an instance of {@link SymmetricEncryption }
     * 
     */
    public SymmetricEncryption createSymmetricEncryption() {
        return new SymmetricEncryption();
    }

    /**
     * Create an instance of {@link Actions }
     * 
     */
    public Actions createActions() {
        return new Actions();
    }

    /**
     * Create an instance of {@link EndpointGroups }
     * 
     */
    public EndpointGroups createEndpointGroups() {
        return new EndpointGroups();
    }

    /**
     * Create an instance of {@link ServerSocketEndpointConfig }
     * 
     */
    public ServerSocketEndpointConfig createServerSocketEndpointConfig() {
        return new ServerSocketEndpointConfig();
    }

    /**
     * Create an instance of {@link WanConsumer }
     * 
     */
    public WanConsumer createWanConsumer() {
        return new WanConsumer();
    }

    /**
     * Create an instance of {@link Interceptor }
     * 
     */
    public Interceptor createInterceptor() {
        return new Interceptor();
    }

    /**
     * Create an instance of {@link NearCache }
     * 
     */
    public NearCache createNearCache() {
        return new NearCache();
    }

    /**
     * Create an instance of {@link HotRestart }
     * 
     */
    public HotRestart createHotRestart() {
        return new HotRestart();
    }

    /**
     * Create an instance of {@link EndpointConfig }
     * 
     */
    public EndpointConfig createEndpointConfig() {
        return new EndpointConfig();
    }

    /**
     * Create an instance of {@link Identity }
     * 
     */
    public Identity createIdentity() {
        return new Identity();
    }

    /**
     * Create an instance of {@link EventJournal }
     * 
     */
    public EventJournal createEventJournal() {
        return new EventJournal();
    }

    /**
     * Create an instance of {@link DiscoveryStrategies }
     * 
     */
    public DiscoveryStrategies createDiscoveryStrategies() {
        return new DiscoveryStrategies();
    }

    /**
     * Create an instance of {@link Replacer }
     * 
     */
    public Replacer createReplacer() {
        return new Replacer();
    }

    /**
     * Create an instance of {@link QueryCaches }
     * 
     */
    public QueryCaches createQueryCaches() {
        return new QueryCaches();
    }

    /**
     * Create an instance of {@link LoginModule }
     * 
     */
    public LoginModule createLoginModule() {
        return new LoginModule();
    }

    /**
     * Create an instance of {@link Token }
     * 
     */
    public Token createToken() {
        return new Token();
    }

    /**
     * Create an instance of {@link RestApi }
     * 
     */
    public RestApi createRestApi() {
        return new RestApi();
    }

    /**
     * Create an instance of {@link Attributes }
     * 
     */
    public Attributes createAttributes() {
        return new Attributes();
    }

    /**
     * Create an instance of {@link RealmReference }
     * 
     */
    public RealmReference createRealmReference() {
        return new RealmReference();
    }

    /**
     * Create an instance of {@link ManagementPermission }
     * 
     */
    public ManagementPermission createManagementPermission() {
        return new ManagementPermission();
    }

    /**
     * Create an instance of {@link Realms }
     * 
     */
    public Realms createRealms() {
        return new Realms();
    }

    /**
     * Create an instance of {@link AutoDetection }
     * 
     */
    public AutoDetection createAutoDetection() {
        return new AutoDetection();
    }

    /**
     * Create an instance of {@link InstancePermission }
     * 
     */
    public InstancePermission createInstancePermission() {
        return new InstancePermission();
    }

    /**
     * Create an instance of {@link TrustedInterfaces }
     * 
     */
    public TrustedInterfaces createTrustedInterfaces() {
        return new TrustedInterfaces();
    }

    /**
     * Create an instance of {@link MemberAddressProvider }
     * 
     */
    public MemberAddressProvider createMemberAddressProvider() {
        return new MemberAddressProvider();
    }

    /**
     * Create an instance of {@link BitmapIndexOptions }
     * 
     */
    public BitmapIndexOptions createBitmapIndexOptions() {
        return new BitmapIndexOptions();
    }

    /**
     * Create an instance of {@link WanReplicationRef }
     * 
     */
    public WanReplicationRef createWanReplicationRef() {
        return new WanReplicationRef();
    }

    /**
     * Create an instance of {@link SplitBrainProtectionListener }
     * 
     */
    public SplitBrainProtectionListener createSplitBrainProtectionListener() {
        return new SplitBrainProtectionListener();
    }

    /**
     * Create an instance of {@link SocketOptions }
     * 
     */
    public SocketOptions createSocketOptions() {
        return new SocketOptions();
    }

    /**
     * Create an instance of {@link MerkleTree }
     * 
     */
    public MerkleTree createMerkleTree() {
        return new MerkleTree();
    }

    /**
     * Create an instance of {@link MemorySize }
     * 
     */
    public MemorySize createMemorySize() {
        return new MemorySize();
    }

    /**
     * Create an instance of {@link Interceptors }
     * 
     */
    public Interceptors createInterceptors() {
        return new Interceptors();
    }

    /**
     * Create an instance of {@link SecurityObject }
     * 
     */
    public SecurityObject createSecurityObject() {
        return new SecurityObject();
    }

    /**
     * Create an instance of {@link PartitionLostListeners }
     * 
     */
    public PartitionLostListeners createPartitionLostListeners() {
        return new PartitionLostListeners();
    }

    /**
     * Create an instance of {@link DiscoveryNodeFilter }
     * 
     */
    public DiscoveryNodeFilter createDiscoveryNodeFilter() {
        return new DiscoveryNodeFilter();
    }

    /**
     * Create an instance of {@link SerializationFactory }
     * 
     */
    public SerializationFactory createSerializationFactory() {
        return new SerializationFactory();
    }

    /**
     * Create an instance of {@link ListenerBase }
     * 
     */
    public ListenerBase createListenerBase() {
        return new ListenerBase();
    }

    /**
     * Create an instance of {@link CacheEntryListeners }
     * 
     */
    public CacheEntryListeners createCacheEntryListeners() {
        return new CacheEntryListeners();
    }

    /**
     * Create an instance of {@link WanSync }
     * 
     */
    public WanSync createWanSync() {
        return new WanSync();
    }

    /**
     * Create an instance of {@link FailureDetector }
     * 
     */
    public FailureDetector createFailureDetector() {
        return new FailureDetector();
    }

    /**
     * Create an instance of {@link BasicAuthentication }
     * 
     */
    public BasicAuthentication createBasicAuthentication() {
        return new BasicAuthentication();
    }

    /**
     * Create an instance of {@link EndpointGroup }
     * 
     */
    public EndpointGroup createEndpointGroup() {
        return new EndpointGroup();
    }

    /**
     * Create an instance of {@link MergePolicy }
     * 
     */
    public MergePolicy createMergePolicy() {
        return new MergePolicy();
    }

    /**
     * Create an instance of {@link TimedExpiryPolicyFactory }
     * 
     */
    public TimedExpiryPolicyFactory createTimedExpiryPolicyFactory() {
        return new TimedExpiryPolicyFactory();
    }

    /**
     * Create an instance of {@link EntryListener }
     * 
     */
    public EntryListener createEntryListener() {
        return new EntryListener();
    }

    /**
     * Create an instance of {@link MapAttribute }
     * 
     */
    public MapAttribute createMapAttribute() {
        return new MapAttribute();
    }

    /**
     * Create an instance of {@link MapStore }
     * 
     */
    public MapStore createMapStore() {
        return new MapStore();
    }

    /**
     * Create an instance of {@link Port }
     * 
     */
    public Port createPort() {
        return new Port();
    }

    /**
     * Create an instance of {@link LoginModules }
     * 
     */
    public LoginModules createLoginModules() {
        return new LoginModules();
    }

    /**
     * Create an instance of {@link EvictionMap }
     * 
     */
    public EvictionMap createEvictionMap() {
        return new EvictionMap();
    }

    /**
     * Create an instance of {@link UsernamePassword }
     * 
     */
    public UsernamePassword createUsernamePassword() {
        return new UsernamePassword();
    }

    /**
     * Create an instance of {@link PersistentMemoryDirectory }
     * 
     */
    public PersistentMemoryDirectory createPersistentMemoryDirectory() {
        return new PersistentMemoryDirectory();
    }

    /**
     * Create an instance of {@link MemcacheProtocol }
     * 
     */
    public MemcacheProtocol createMemcacheProtocol() {
        return new MemcacheProtocol();
    }

    /**
     * Create an instance of {@link Serializer }
     * 
     */
    public Serializer createSerializer() {
        return new Serializer();
    }

    /**
     * Create an instance of {@link Eviction }
     * 
     */
    public Eviction createEviction() {
        return new Eviction();
    }

    /**
     * Create an instance of {@link RestServerSocketEndpointConfig }
     * 
     */
    public RestServerSocketEndpointConfig createRestServerSocketEndpointConfig() {
        return new RestServerSocketEndpointConfig();
    }

    /**
     * Create an instance of {@link FencedLock }
     * 
     */
    public FencedLock createFencedLock() {
        return new FencedLock();
    }

    /**
     * Create an instance of {@link Authentication }
     * 
     */
    public Authentication createAuthentication() {
        return new Authentication();
    }

    /**
     * Create an instance of {@link MetricsJmx }
     * 
     */
    public MetricsJmx createMetricsJmx() {
        return new MetricsJmx();
    }

    /**
     * Create an instance of {@link Interfaces }
     * 
     */
    public Interfaces createInterfaces() {
        return new Interfaces();
    }

    /**
     * Create an instance of {@link FilterList }
     * 
     */
    public FilterList createFilterList() {
        return new FilterList();
    }

    /**
     * Create an instance of {@link Multicast }
     * 
     */
    public Multicast createMulticast() {
        return new Multicast();
    }

    /**
     * Create an instance of {@link GlobalSerializer }
     * 
     */
    public GlobalSerializer createGlobalSerializer() {
        return new GlobalSerializer();
    }

    /**
     * Create an instance of {@link WanCustomPublisher }
     * 
     */
    public WanCustomPublisher createWanCustomPublisher() {
        return new WanCustomPublisher();
    }

    /**
     * Create an instance of {@link RaftAlgorithm }
     * 
     */
    public RaftAlgorithm createRaftAlgorithm() {
        return new RaftAlgorithm();
    }

    /**
     * Create an instance of {@link MetricsManagementCenter }
     * 
     */
    public MetricsManagementCenter createMetricsManagementCenter() {
        return new MetricsManagementCenter();
    }

    /**
     * Create an instance of {@link OutboundPorts }
     * 
     */
    public OutboundPorts createOutboundPorts() {
        return new OutboundPorts();
    }

    /**
     * Create an instance of {@link Realm }
     * 
     */
    public Realm createRealm() {
        return new Realm();
    }

    /**
     * Create an instance of {@link RingbufferStore }
     * 
     */
    public RingbufferStore createRingbufferStore() {
        return new RingbufferStore();
    }

    /**
     * Create an instance of {@link KerberosIdentity }
     * 
     */
    public KerberosIdentity createKerberosIdentity() {
        return new KerberosIdentity();
    }

    /**
     * Create an instance of {@link PersistentMemory.Directories }
     * 
     */
    public PersistentMemory.Directories createPersistentMemoryDirectories() {
        return new PersistentMemory.Directories();
    }

    /**
     * Create an instance of {@link CacheEntryListener.CacheEntryListenerFactory }
     * 
     */
    public CacheEntryListener.CacheEntryListenerFactory createCacheEntryListenerCacheEntryListenerFactory() {
        return new CacheEntryListener.CacheEntryListenerFactory();
    }

    /**
     * Create an instance of {@link CacheEntryListener.CacheEntryEventFilterFactory }
     * 
     */
    public CacheEntryListener.CacheEntryEventFilterFactory createCacheEntryListenerCacheEntryEventFilterFactory() {
        return new CacheEntryListener.CacheEntryEventFilterFactory();
    }

    /**
     * Create an instance of {@link BasePermission.Endpoints }
     * 
     */
    public BasePermission.Endpoints createBasePermissionEndpoints() {
        return new BasePermission.Endpoints();
    }

    /**
     * Create an instance of {@link QueryCache.Indexes }
     * 
     */
    public QueryCache.Indexes createQueryCacheIndexes() {
        return new QueryCache.Indexes();
    }

    /**
     * Create an instance of {@link TcpIp.MemberList }
     * 
     */
    public TcpIp.MemberList createTcpIpMemberList() {
        return new TcpIp.MemberList();
    }

    /**
     * Create an instance of {@link CpSubsystem.Semaphores }
     * 
     */
    public CpSubsystem.Semaphores createCpSubsystemSemaphores() {
        return new CpSubsystem.Semaphores();
    }

    /**
     * Create an instance of {@link CpSubsystem.Locks }
     * 
     */
    public CpSubsystem.Locks createCpSubsystemLocks() {
        return new CpSubsystem.Locks();
    }

    /**
     * Create an instance of {@link SplitBrainProtection.Listeners }
     * 
     */
    public SplitBrainProtection.Listeners createSplitBrainProtectionListeners() {
        return new SplitBrainProtection.Listeners();
    }

    /**
     * Create an instance of {@link Serialization.DataSerializableFactories }
     * 
     */
    public Serialization.DataSerializableFactories createSerializationDataSerializableFactories() {
        return new Serialization.DataSerializableFactories();
    }

    /**
     * Create an instance of {@link Serialization.PortableFactories }
     * 
     */
    public Serialization.PortableFactories createSerializationPortableFactories() {
        return new Serialization.PortableFactories();
    }

    /**
     * Create an instance of {@link Serialization.Serializers }
     * 
     */
    public Serialization.Serializers createSerializationSerializers() {
        return new Serialization.Serializers();
    }

    /**
     * Create an instance of {@link ReliableTopic.MessageListeners }
     * 
     */
    public ReliableTopic.MessageListeners createReliableTopicMessageListeners() {
        return new ReliableTopic.MessageListeners();
    }

    /**
     * Create an instance of {@link Topic.MessageListeners }
     * 
     */
    public Topic.MessageListeners createTopicMessageListeners() {
        return new Topic.MessageListeners();
    }

    /**
     * Create an instance of {@link Set.ItemListeners }
     * 
     */
    public Set.ItemListeners createSetItemListeners() {
        return new Set.ItemListeners();
    }

    /**
     * Create an instance of {@link List.ItemListeners }
     * 
     */
    public List.ItemListeners createListItemListeners() {
        return new List.ItemListeners();
    }

    /**
     * Create an instance of {@link Cache.KeyType }
     * 
     */
    public Cache.KeyType createCacheKeyType() {
        return new Cache.KeyType();
    }

    /**
     * Create an instance of {@link Cache.ValueType }
     * 
     */
    public Cache.ValueType createCacheValueType() {
        return new Cache.ValueType();
    }

    /**
     * Create an instance of {@link Cache.CacheLoaderFactory }
     * 
     */
    public Cache.CacheLoaderFactory createCacheCacheLoaderFactory() {
        return new Cache.CacheLoaderFactory();
    }

    /**
     * Create an instance of {@link Cache.CacheLoader }
     * 
     */
    public Cache.CacheLoader createCacheCacheLoader() {
        return new Cache.CacheLoader();
    }

    /**
     * Create an instance of {@link Cache.CacheWriterFactory }
     * 
     */
    public Cache.CacheWriterFactory createCacheCacheWriterFactory() {
        return new Cache.CacheWriterFactory();
    }

    /**
     * Create an instance of {@link Cache.CacheWriter }
     * 
     */
    public Cache.CacheWriter createCacheCacheWriter() {
        return new Cache.CacheWriter();
    }

    /**
     * Create an instance of {@link Cache.ExpiryPolicyFactory }
     * 
     */
    public Cache.ExpiryPolicyFactory createCacheExpiryPolicyFactory() {
        return new Cache.ExpiryPolicyFactory();
    }

    /**
     * Create an instance of {@link Map.Indexes }
     * 
     */
    public Map.Indexes createMapIndexes() {
        return new Map.Indexes();
    }

    /**
     * Create an instance of {@link Map.Attributes }
     * 
     */
    public Map.Attributes createMapAttributes() {
        return new Map.Attributes();
    }

    /**
     * Create an instance of {@link Queue.ItemListeners }
     * 
     */
    public Queue.ItemListeners createQueueItemListeners() {
        return new Queue.ItemListeners();
    }

    /**
     * Create an instance of {@link PartitionGroup.MemberGroup }
     * 
     */
    public PartitionGroup.MemberGroup createPartitionGroupMemberGroup() {
        return new PartitionGroup.MemberGroup();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Keystore }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "keystore", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "secure-store-substitution-group")
    public JAXBElement<Keystore> createKeystore(Keystore value) {
        return new JAXBElement<Keystore>(_Keystore_QNAME, Keystore.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "factory-or-class-name")
    public JAXBElement<Object> createFactoryOrClassName(Object value) {
        return new JAXBElement<Object>(_FactoryOrClassName_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "class-name", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "factory-or-class-name")
    public JAXBElement<Object> createClassName(Object value) {
        return new JAXBElement<Object>(_ClassName_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "secure-store-substitution-group")
    public JAXBElement<Object> createSecureStoreSubstitutionGroup(Object value) {
        return new JAXBElement<Object>(_SecureStoreSubstitutionGroup_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Vault }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "vault", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "secure-store-substitution-group")
    public JAXBElement<Vault> createVault(Vault value) {
        return new JAXBElement<Vault>(_Vault_QNAME, Vault.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RecentlyActiveSplitBrainProtection }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "recently-active-split-brain-protection", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "choice-of-split-brain-protection-function")
    public JAXBElement<RecentlyActiveSplitBrainProtection> createRecentlyActiveSplitBrainProtection(RecentlyActiveSplitBrainProtection value) {
        return new JAXBElement<RecentlyActiveSplitBrainProtection>(_RecentlyActiveSplitBrainProtection_QNAME, RecentlyActiveSplitBrainProtection.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "factory-class-name", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "factory-or-class-name")
    public JAXBElement<Object> createFactoryClassName(Object value) {
        return new JAXBElement<Object>(_FactoryClassName_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MemberCountSplitBrainProtection }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "member-count-split-brain-protection", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "choice-of-split-brain-protection-function")
    public JAXBElement<MemberCountSplitBrainProtection> createMemberCountSplitBrainProtection(MemberCountSplitBrainProtection value) {
        return new JAXBElement<MemberCountSplitBrainProtection>(_MemberCountSplitBrainProtection_QNAME, MemberCountSplitBrainProtection.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "choice-of-split-brain-protection-function")
    public JAXBElement<Object> createChoiceOfSplitBrainProtectionFunction(Object value) {
        return new JAXBElement<Object>(_ChoiceOfSplitBrainProtectionFunction_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ProbabilisticSplitBrainProtection }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "probabilistic-split-brain-protection", substitutionHeadNamespace = "http://www.hazelcast.com/schema/config", substitutionHeadName = "choice-of-split-brain-protection-function")
    public JAXBElement<ProbabilisticSplitBrainProtection> createProbabilisticSplitBrainProtection(ProbabilisticSplitBrainProtection value) {
        return new JAXBElement<ProbabilisticSplitBrainProtection>(_ProbabilisticSplitBrainProtection_QNAME, ProbabilisticSplitBrainProtection.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TcpIp.MemberList }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "member-list", scope = TcpIp.class)
    public JAXBElement<TcpIp.MemberList> createTcpIpMemberList(TcpIp.MemberList value) {
        return new JAXBElement<TcpIp.MemberList>(_TcpIpMemberList_QNAME, TcpIp.MemberList.class, TcpIp.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "member", scope = TcpIp.class, defaultValue = "127.0.0.1")
    public JAXBElement<String> createTcpIpMember(String value) {
        return new JAXBElement<String>(_TcpIpMember_QNAME, String.class, TcpIp.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "interface", scope = TcpIp.class, defaultValue = "127.0.0.1")
    public JAXBElement<String> createTcpIpInterface(String value) {
        return new JAXBElement<String>(_TcpIpInterface_QNAME, String.class, TcpIp.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "members", scope = TcpIp.class)
    public JAXBElement<String> createTcpIpMembers(String value) {
        return new JAXBElement<String>(_TcpIpMembers_QNAME, String.class, TcpIp.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "required-member", scope = TcpIp.class)
    public JAXBElement<String> createTcpIpRequiredMember(String value) {
        return new JAXBElement<String>(_TcpIpRequiredMember_QNAME, String.class, TcpIp.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BasePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "config-permission", scope = Permissions.class)
    public JAXBElement<BasePermission> createPermissionsConfigPermission(BasePermission value) {
        return new JAXBElement<BasePermission>(_PermissionsConfigPermission_QNAME, BasePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BasePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "all-permissions", scope = Permissions.class)
    public JAXBElement<BasePermission> createPermissionsAllPermissions(BasePermission value) {
        return new JAXBElement<BasePermission>(_PermissionsAllPermissions_QNAME, BasePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "replicatedmap-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsReplicatedmapPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsReplicatedmapPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cache-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsCachePermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsCachePermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "multimap-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsMultimapPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsMultimapPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "scheduled-executor-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsScheduledExecutorPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsScheduledExecutorPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "map-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsMapPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsMapPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cardinality-estimator-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsCardinalityEstimatorPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsCardinalityEstimatorPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "durable-executor-service-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsDurableExecutorServicePermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsDurableExecutorServicePermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "reliable-topic-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsReliableTopicPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsReliableTopicPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ManagementPermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "management-permission", scope = Permissions.class)
    public JAXBElement<ManagementPermission> createPermissionsManagementPermission(ManagementPermission value) {
        return new JAXBElement<ManagementPermission>(_PermissionsManagementPermission_QNAME, ManagementPermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "pn-counter-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsPnCounterPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsPnCounterPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "set-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsSetPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsSetPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "lock-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsLockPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsLockPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BasePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "transaction-permission", scope = Permissions.class)
    public JAXBElement<BasePermission> createPermissionsTransactionPermission(BasePermission value) {
        return new JAXBElement<BasePermission>(_PermissionsTransactionPermission_QNAME, BasePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "queue-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsQueuePermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsQueuePermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "ring-buffer-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsRingBufferPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsRingBufferPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "countdown-latch-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsCountdownLatchPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsCountdownLatchPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "semaphore-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsSemaphorePermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsSemaphorePermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "executor-service-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsExecutorServicePermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsExecutorServicePermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "atomic-long-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsAtomicLongPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsAtomicLongPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "atomic-reference-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsAtomicReferencePermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsAtomicReferencePermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "user-code-deployment-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsUserCodeDeploymentPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsUserCodeDeploymentPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "list-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsListPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsListPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "topic-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsTopicPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsTopicPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "flake-id-generator-permission", scope = Permissions.class)
    public JAXBElement<InstancePermission> createPermissionsFlakeIdGeneratorPermission(InstancePermission value) {
        return new JAXBElement<InstancePermission>(_PermissionsFlakeIdGeneratorPermission_QNAME, InstancePermission.class, Permissions.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "security-realm", scope = KerberosAuthentication.class)
    public JAXBElement<String> createKerberosAuthenticationSecurityRealm(String value) {
        return new JAXBElement<String>(_KerberosAuthenticationSecurityRealm_QNAME, String.class, KerberosAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "relax-flags-check", scope = KerberosAuthentication.class)
    public JAXBElement<Boolean> createKerberosAuthenticationRelaxFlagsCheck(Boolean value) {
        return new JAXBElement<Boolean>(_KerberosAuthenticationRelaxFlagsCheck_QNAME, Boolean.class, KerberosAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "keytab-file", scope = KerberosAuthentication.class)
    public JAXBElement<String> createKerberosAuthenticationKeytabFile(String value) {
        return new JAXBElement<String>(_KerberosAuthenticationKeytabFile_QNAME, String.class, KerberosAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "principal", scope = KerberosAuthentication.class)
    public JAXBElement<String> createKerberosAuthenticationPrincipal(String value) {
        return new JAXBElement<String>(_KerberosAuthenticationPrincipal_QNAME, String.class, KerberosAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LdapAuthentication }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "ldap", scope = KerberosAuthentication.class)
    public JAXBElement<LdapAuthentication> createKerberosAuthenticationLdap(LdapAuthentication value) {
        return new JAXBElement<LdapAuthentication>(_KerberosAuthenticationLdap_QNAME, LdapAuthentication.class, KerberosAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "use-name-without-realm", scope = KerberosAuthentication.class)
    public JAXBElement<Boolean> createKerberosAuthenticationUseNameWithoutRealm(Boolean value) {
        return new JAXBElement<Boolean>(_KerberosAuthenticationUseNameWithoutRealm_QNAME, Boolean.class, KerberosAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ConfigReplacers }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "config-replacers", scope = Hazelcast.class)
    public JAXBElement<ConfigReplacers> createHazelcastConfigReplacers(ConfigReplacers value) {
        return new JAXBElement<ConfigReplacers>(_HazelcastConfigReplacers_QNAME, ConfigReplacers.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanReplication }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "wan-replication", scope = Hazelcast.class)
    public JAXBElement<WanReplication> createHazelcastWanReplication(WanReplication value) {
        return new JAXBElement<WanReplication>(_HazelcastWanReplication_QNAME, WanReplication.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PnCounter }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "pn-counter", scope = Hazelcast.class)
    public JAXBElement<PnCounter> createHazelcastPnCounter(PnCounter value) {
        return new JAXBElement<PnCounter>(_HazelcastPnCounter_QNAME, PnCounter.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CpSubsystem }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cp-subsystem", scope = Hazelcast.class)
    public JAXBElement<CpSubsystem> createHazelcastCpSubsystem(CpSubsystem value) {
        return new JAXBElement<CpSubsystem>(_HazelcastCpSubsystem_QNAME, CpSubsystem.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Map }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "map", scope = Hazelcast.class)
    public JAXBElement<Map> createHazelcastMap(Map value) {
        return new JAXBElement<Map>(_HazelcastMap_QNAME, Map.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PartitionGroup }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "partition-group", scope = Hazelcast.class)
    public JAXBElement<PartitionGroup> createHazelcastPartitionGroup(PartitionGroup value) {
        return new JAXBElement<PartitionGroup>(_HazelcastPartitionGroup_QNAME, PartitionGroup.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Security }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "security", scope = Hazelcast.class)
    public JAXBElement<Security> createHazelcastSecurity(Security value) {
        return new JAXBElement<Security>(_HazelcastSecurity_QNAME, Security.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ScheduledExecutorService }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "scheduled-executor-service", scope = Hazelcast.class)
    public JAXBElement<ScheduledExecutorService> createHazelcastScheduledExecutorService(ScheduledExecutorService value) {
        return new JAXBElement<ScheduledExecutorService>(_HazelcastScheduledExecutorService_QNAME, ScheduledExecutorService.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "instance-name", scope = Hazelcast.class)
    public JAXBElement<String> createHazelcastInstanceName(String value) {
        return new JAXBElement<String>(_HazelcastInstanceName_QNAME, String.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Network }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "network", scope = Hazelcast.class)
    public JAXBElement<Network> createHazelcastNetwork(Network value) {
        return new JAXBElement<Network>(_HazelcastNetwork_QNAME, Network.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AdvancedNetwork }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "advanced-network", scope = Hazelcast.class)
    public JAXBElement<AdvancedNetwork> createHazelcastAdvancedNetwork(AdvancedNetwork value) {
        return new JAXBElement<AdvancedNetwork>(_HazelcastAdvancedNetwork_QNAME, AdvancedNetwork.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Sql }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "sql", scope = Hazelcast.class)
    public JAXBElement<Sql> createHazelcastSql(Sql value) {
        return new JAXBElement<Sql>(_HazelcastSql_QNAME, Sql.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NativeMemory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "native-memory", scope = Hazelcast.class)
    public JAXBElement<NativeMemory> createHazelcastNativeMemory(NativeMemory value) {
        return new JAXBElement<NativeMemory>(_HazelcastNativeMemory_QNAME, NativeMemory.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UserCodeDeployment }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "user-code-deployment", scope = Hazelcast.class)
    public JAXBElement<UserCodeDeployment> createHazelcastUserCodeDeployment(UserCodeDeployment value) {
        return new JAXBElement<UserCodeDeployment>(_HazelcastUserCodeDeployment_QNAME, UserCodeDeployment.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReliableTopic }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "reliable-topic", scope = Hazelcast.class)
    public JAXBElement<ReliableTopic> createHazelcastReliableTopic(ReliableTopic value) {
        return new JAXBElement<ReliableTopic>(_HazelcastReliableTopic_QNAME, ReliableTopic.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DurableExecutorService }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "durable-executor-service", scope = Hazelcast.class)
    public JAXBElement<DurableExecutorService> createHazelcastDurableExecutorService(DurableExecutorService value) {
        return new JAXBElement<DurableExecutorService>(_HazelcastDurableExecutorService_QNAME, DurableExecutorService.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Multimap }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "multimap", scope = Hazelcast.class)
    public JAXBElement<Multimap> createHazelcastMultimap(Multimap value) {
        return new JAXBElement<Multimap>(_HazelcastMultimap_QNAME, Multimap.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CrdtReplication }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "crdt-replication", scope = Hazelcast.class)
    public JAXBElement<CrdtReplication> createHazelcastCrdtReplication(CrdtReplication value) {
        return new JAXBElement<CrdtReplication>(_HazelcastCrdtReplication_QNAME, CrdtReplication.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Metrics }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "metrics", scope = Hazelcast.class)
    public JAXBElement<Metrics> createHazelcastMetrics(Metrics value) {
        return new JAXBElement<Metrics>(_HazelcastMetrics_QNAME, Metrics.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FlakeIdGenerator }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "flake-id-generator", scope = Hazelcast.class)
    public JAXBElement<FlakeIdGenerator> createHazelcastFlakeIdGenerator(FlakeIdGenerator value) {
        return new JAXBElement<FlakeIdGenerator>(_HazelcastFlakeIdGenerator_QNAME, FlakeIdGenerator.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Topic }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "topic", scope = Hazelcast.class)
    public JAXBElement<Topic> createHazelcastTopic(Topic value) {
        return new JAXBElement<Topic>(_HazelcastTopic_QNAME, Topic.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InstanceTracking }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "instance-tracking", scope = Hazelcast.class)
    public JAXBElement<InstanceTracking> createHazelcastInstanceTracking(InstanceTracking value) {
        return new JAXBElement<InstanceTracking>(_HazelcastInstanceTracking_QNAME, InstanceTracking.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Properties }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "properties", scope = Hazelcast.class)
    public JAXBElement<Properties> createHazelcastProperties(Properties value) {
        return new JAXBElement<Properties>(_HazelcastProperties_QNAME, Properties.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Queue }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "queue", scope = Hazelcast.class)
    public JAXBElement<Queue> createHazelcastQueue(Queue value) {
        return new JAXBElement<Queue>(_HazelcastQueue_QNAME, Queue.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HotRestartPersistence }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "hot-restart-persistence", scope = Hazelcast.class)
    public JAXBElement<HotRestartPersistence> createHazelcastHotRestartPersistence(HotRestartPersistence value) {
        return new JAXBElement<HotRestartPersistence>(_HazelcastHotRestartPersistence_QNAME, HotRestartPersistence.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ManagementCenter }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "management-center", scope = Hazelcast.class)
    public JAXBElement<ManagementCenter> createHazelcastManagementCenter(ManagementCenter value) {
        return new JAXBElement<ManagementCenter>(_HazelcastManagementCenter_QNAME, ManagementCenter.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Serialization }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "serialization", scope = Hazelcast.class)
    public JAXBElement<Serialization> createHazelcastSerialization(Serialization value) {
        return new JAXBElement<Serialization>(_HazelcastSerialization_QNAME, Serialization.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LiteMember }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "lite-member", scope = Hazelcast.class)
    public JAXBElement<LiteMember> createHazelcastLiteMember(LiteMember value) {
        return new JAXBElement<LiteMember>(_HazelcastLiteMember_QNAME, LiteMember.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExecutorService }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "executor-service", scope = Hazelcast.class)
    public JAXBElement<ExecutorService> createHazelcastExecutorService(ExecutorService value) {
        return new JAXBElement<ExecutorService>(_HazelcastExecutorService_QNAME, ExecutorService.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Ringbuffer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "ringbuffer", scope = Hazelcast.class)
    public JAXBElement<Ringbuffer> createHazelcastRingbuffer(Ringbuffer value) {
        return new JAXBElement<Ringbuffer>(_HazelcastRingbuffer_QNAME, Ringbuffer.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CardinalityEstimator }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cardinality-estimator", scope = Hazelcast.class)
    public JAXBElement<CardinalityEstimator> createHazelcastCardinalityEstimator(CardinalityEstimator value) {
        return new JAXBElement<CardinalityEstimator>(_HazelcastCardinalityEstimator_QNAME, CardinalityEstimator.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "license-key", scope = Hazelcast.class)
    public JAXBElement<String> createHazelcastLicenseKey(String value) {
        return new JAXBElement<String>(_HazelcastLicenseKey_QNAME, String.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Replicatedmap }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "replicatedmap", scope = Hazelcast.class)
    public JAXBElement<Replicatedmap> createHazelcastReplicatedmap(Replicatedmap value) {
        return new JAXBElement<Replicatedmap>(_HazelcastReplicatedmap_QNAME, Replicatedmap.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SplitBrainProtection }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "split-brain-protection", scope = Hazelcast.class)
    public JAXBElement<SplitBrainProtection> createHazelcastSplitBrainProtection(SplitBrainProtection value) {
        return new JAXBElement<SplitBrainProtection>(_HazelcastSplitBrainProtection_QNAME, SplitBrainProtection.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cluster-name", scope = Hazelcast.class)
    public JAXBElement<String> createHazelcastClusterName(String value) {
        return new JAXBElement<String>(_HazelcastClusterName_QNAME, String.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link List }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "list", scope = Hazelcast.class)
    public JAXBElement<List> createHazelcastList(List value) {
        return new JAXBElement<List>(_HazelcastList_QNAME, List.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Set }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "set", scope = Hazelcast.class)
    public JAXBElement<Set> createHazelcastSet(Set value) {
        return new JAXBElement<Set>(_HazelcastSet_QNAME, Set.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Cache }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cache", scope = Hazelcast.class)
    public JAXBElement<Cache> createHazelcastCache(Cache value) {
        return new JAXBElement<Cache>(_HazelcastCache_QNAME, Cache.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Listeners }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "listeners", scope = Hazelcast.class)
    public JAXBElement<Listeners> createHazelcastListeners(Listeners value) {
        return new JAXBElement<Listeners>(_HazelcastListeners_QNAME, Listeners.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FactoryClassWithProperties }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "auditlog", scope = Hazelcast.class)
    public JAXBElement<FactoryClassWithProperties> createHazelcastAuditlog(FactoryClassWithProperties value) {
        return new JAXBElement<FactoryClassWithProperties>(_HazelcastAuditlog_QNAME, FactoryClassWithProperties.class, Hazelcast.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LdapSearchScope }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-search-scope", scope = LdapAuthentication.class)
    public JAXBElement<LdapSearchScope> createLdapAuthenticationRoleSearchScope(LdapSearchScope value) {
        return new JAXBElement<LdapSearchScope>(_LdapAuthenticationRoleSearchScope_QNAME, LdapSearchScope.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-name-attribute", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationRoleNameAttribute(String value) {
        return new JAXBElement<String>(_LdapAuthenticationRoleNameAttribute_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "password-attribute", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationPasswordAttribute(String value) {
        return new JAXBElement<String>(_LdapAuthenticationPasswordAttribute_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "socket-factory-class-name", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationSocketFactoryClassName(String value) {
        return new JAXBElement<String>(_LdapAuthenticationSocketFactoryClassName_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "system-user-password", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationSystemUserPassword(String value) {
        return new JAXBElement<String>(_LdapAuthenticationSystemUserPassword_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "skip-authentication", scope = LdapAuthentication.class)
    public JAXBElement<Boolean> createLdapAuthenticationSkipAuthentication(Boolean value) {
        return new JAXBElement<Boolean>(_LdapAuthenticationSkipAuthentication_QNAME, Boolean.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "user-filter", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationUserFilter(String value) {
        return new JAXBElement<String>(_LdapAuthenticationUserFilter_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LdapSearchScope }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "user-search-scope", scope = LdapAuthentication.class)
    public JAXBElement<LdapSearchScope> createLdapAuthenticationUserSearchScope(LdapSearchScope value) {
        return new JAXBElement<LdapSearchScope>(_LdapAuthenticationUserSearchScope_QNAME, LdapSearchScope.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-context", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationRoleContext(String value) {
        return new JAXBElement<String>(_LdapAuthenticationRoleContext_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-filter", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationRoleFilter(String value) {
        return new JAXBElement<String>(_LdapAuthenticationRoleFilter_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "security-realm", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationSecurityRealm(String value) {
        return new JAXBElement<String>(_KerberosAuthenticationSecurityRealm_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "parse-dn", scope = LdapAuthentication.class)
    public JAXBElement<Boolean> createLdapAuthenticationParseDn(Boolean value) {
        return new JAXBElement<Boolean>(_LdapAuthenticationParseDn_QNAME, Boolean.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-mapping-attribute", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationRoleMappingAttribute(String value) {
        return new JAXBElement<String>(_LdapAuthenticationRoleMappingAttribute_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-recursion-max-depth", scope = LdapAuthentication.class)
    public JAXBElement<Integer> createLdapAuthenticationRoleRecursionMaxDepth(Integer value) {
        return new JAXBElement<Integer>(_LdapAuthenticationRoleRecursionMaxDepth_QNAME, Integer.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "url", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationUrl(String value) {
        return new JAXBElement<String>(_LdapAuthenticationUrl_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "user-name-attribute", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationUserNameAttribute(String value) {
        return new JAXBElement<String>(_LdapAuthenticationUserNameAttribute_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LdapRoleMappingMode }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "role-mapping-mode", scope = LdapAuthentication.class)
    public JAXBElement<LdapRoleMappingMode> createLdapAuthenticationRoleMappingMode(LdapRoleMappingMode value) {
        return new JAXBElement<LdapRoleMappingMode>(_LdapAuthenticationRoleMappingMode_QNAME, LdapRoleMappingMode.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "user-context", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationUserContext(String value) {
        return new JAXBElement<String>(_LdapAuthenticationUserContext_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "system-user-dn", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationSystemUserDn(String value) {
        return new JAXBElement<String>(_LdapAuthenticationSystemUserDn_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "system-authentication", scope = LdapAuthentication.class)
    public JAXBElement<String> createLdapAuthenticationSystemAuthentication(String value) {
        return new JAXBElement<String>(_LdapAuthenticationSystemAuthentication_QNAME, String.class, LdapAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "class", scope = FilterList.class)
    public JAXBElement<String> createFilterListClass(String value) {
        return new JAXBElement<String>(_FilterListClass_QNAME, String.class, FilterList.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "package", scope = FilterList.class)
    public JAXBElement<String> createFilterListPackage(String value) {
        return new JAXBElement<String>(_FilterListPackage_QNAME, String.class, FilterList.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "prefix", scope = FilterList.class)
    public JAXBElement<String> createFilterListPrefix(String value) {
        return new JAXBElement<String>(_FilterListPrefix_QNAME, String.class, FilterList.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanAcknowledgeType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "acknowledge-type", scope = WanBatchPublisher.class, defaultValue = "ACK_ON_OPERATION_COMPLETE")
    public JAXBElement<WanAcknowledgeType> createWanBatchPublisherAcknowledgeType(WanAcknowledgeType value) {
        return new JAXBElement<WanAcknowledgeType>(_WanBatchPublisherAcknowledgeType_QNAME, WanAcknowledgeType.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "azure", scope = WanBatchPublisher.class)
    public JAXBElement<AliasedDiscoveryStrategy> createWanBatchPublisherAzure(AliasedDiscoveryStrategy value) {
        return new JAXBElement<AliasedDiscoveryStrategy>(_WanBatchPublisherAzure_QNAME, AliasedDiscoveryStrategy.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "endpoint", scope = WanBatchPublisher.class)
    public JAXBElement<String> createWanBatchPublisherEndpoint(String value) {
        return new JAXBElement<String>(_WanBatchPublisherEndpoint_QNAME, String.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "gcp", scope = WanBatchPublisher.class)
    public JAXBElement<AliasedDiscoveryStrategy> createWanBatchPublisherGcp(AliasedDiscoveryStrategy value) {
        return new JAXBElement<AliasedDiscoveryStrategy>(_WanBatchPublisherGcp_QNAME, AliasedDiscoveryStrategy.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "kubernetes", scope = WanBatchPublisher.class)
    public JAXBElement<AliasedDiscoveryStrategy> createWanBatchPublisherKubernetes(AliasedDiscoveryStrategy value) {
        return new JAXBElement<AliasedDiscoveryStrategy>(_WanBatchPublisherKubernetes_QNAME, AliasedDiscoveryStrategy.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "eureka", scope = WanBatchPublisher.class)
    public JAXBElement<AliasedDiscoveryStrategy> createWanBatchPublisherEureka(AliasedDiscoveryStrategy value) {
        return new JAXBElement<AliasedDiscoveryStrategy>(_WanBatchPublisherEureka_QNAME, AliasedDiscoveryStrategy.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanQueueFullBehavior }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "queue-full-behavior", scope = WanBatchPublisher.class, defaultValue = "DISCARD_AFTER_MUTATION")
    public JAXBElement<WanQueueFullBehavior> createWanBatchPublisherQueueFullBehavior(WanQueueFullBehavior value) {
        return new JAXBElement<WanQueueFullBehavior>(_WanBatchPublisherQueueFullBehavior_QNAME, WanQueueFullBehavior.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "max-concurrent-invocations", scope = WanBatchPublisher.class, defaultValue = "-1")
    public JAXBElement<BigInteger> createWanBatchPublisherMaxConcurrentInvocations(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherMaxConcurrentInvocations_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Properties }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "properties", scope = WanBatchPublisher.class)
    public JAXBElement<Properties> createWanBatchPublisherProperties(Properties value) {
        return new JAXBElement<Properties>(_HazelcastProperties_QNAME, Properties.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Long }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "idle-max-park-ns", scope = WanBatchPublisher.class, defaultValue = "250000000")
    public JAXBElement<Long> createWanBatchPublisherIdleMaxParkNs(Long value) {
        return new JAXBElement<Long>(_WanBatchPublisherIdleMaxParkNs_QNAME, Long.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "aws", scope = WanBatchPublisher.class)
    public JAXBElement<AliasedDiscoveryStrategy> createWanBatchPublisherAws(AliasedDiscoveryStrategy value) {
        return new JAXBElement<AliasedDiscoveryStrategy>(_WanBatchPublisherAws_QNAME, AliasedDiscoveryStrategy.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "publisher-id", scope = WanBatchPublisher.class)
    public JAXBElement<String> createWanBatchPublisherPublisherId(String value) {
        return new JAXBElement<String>(_WanBatchPublisherPublisherId_QNAME, String.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "target-endpoints", scope = WanBatchPublisher.class)
    public JAXBElement<String> createWanBatchPublisherTargetEndpoints(String value) {
        return new JAXBElement<String>(_WanBatchPublisherTargetEndpoints_QNAME, String.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "batch-size", scope = WanBatchPublisher.class, defaultValue = "500")
    public JAXBElement<BigInteger> createWanBatchPublisherBatchSize(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherBatchSize_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "response-timeout-millis", scope = WanBatchPublisher.class, defaultValue = "60000")
    public JAXBElement<BigInteger> createWanBatchPublisherResponseTimeoutMillis(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherResponseTimeoutMillis_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "batch-max-delay-millis", scope = WanBatchPublisher.class, defaultValue = "1000")
    public JAXBElement<BigInteger> createWanBatchPublisherBatchMaxDelayMillis(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherBatchMaxDelayMillis_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "cluster-name", scope = WanBatchPublisher.class)
    public JAXBElement<String> createWanBatchPublisherClusterName(String value) {
        return new JAXBElement<String>(_HazelcastClusterName_QNAME, String.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Long }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "idle-min-park-ns", scope = WanBatchPublisher.class, defaultValue = "10000000")
    public JAXBElement<Long> createWanBatchPublisherIdleMinParkNs(Long value) {
        return new JAXBElement<Long>(_WanBatchPublisherIdleMinParkNs_QNAME, Long.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanSync }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "sync", scope = WanBatchPublisher.class)
    public JAXBElement<WanSync> createWanBatchPublisherSync(WanSync value) {
        return new JAXBElement<WanSync>(_WanBatchPublisherSync_QNAME, WanSync.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "queue-capacity", scope = WanBatchPublisher.class, defaultValue = "10000")
    public JAXBElement<BigInteger> createWanBatchPublisherQueueCapacity(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherQueueCapacity_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DiscoveryStrategies }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "discovery-strategies", scope = WanBatchPublisher.class)
    public JAXBElement<DiscoveryStrategies> createWanBatchPublisherDiscoveryStrategies(DiscoveryStrategies value) {
        return new JAXBElement<DiscoveryStrategies>(_WanBatchPublisherDiscoveryStrategies_QNAME, DiscoveryStrategies.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "max-target-endpoints", scope = WanBatchPublisher.class, defaultValue = "2147483647")
    public JAXBElement<BigInteger> createWanBatchPublisherMaxTargetEndpoints(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherMaxTargetEndpoints_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "use-endpoint-private-address", scope = WanBatchPublisher.class, defaultValue = "false")
    public JAXBElement<Boolean> createWanBatchPublisherUseEndpointPrivateAddress(Boolean value) {
        return new JAXBElement<Boolean>(_WanBatchPublisherUseEndpointPrivateAddress_QNAME, Boolean.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InitialPublisherState }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "initial-publisher-state", scope = WanBatchPublisher.class, defaultValue = "REPLICATING")
    public JAXBElement<InitialPublisherState> createWanBatchPublisherInitialPublisherState(InitialPublisherState value) {
        return new JAXBElement<InitialPublisherState>(_WanBatchPublisherInitialPublisherState_QNAME, InitialPublisherState.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "discovery-period-seconds", scope = WanBatchPublisher.class, defaultValue = "10")
    public JAXBElement<BigInteger> createWanBatchPublisherDiscoveryPeriodSeconds(BigInteger value) {
        return new JAXBElement<BigInteger>(_WanBatchPublisherDiscoveryPeriodSeconds_QNAME, BigInteger.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "snapshot-enabled", scope = WanBatchPublisher.class, defaultValue = "false")
    public JAXBElement<Boolean> createWanBatchPublisherSnapshotEnabled(Boolean value) {
        return new JAXBElement<Boolean>(_WanBatchPublisherSnapshotEnabled_QNAME, Boolean.class, WanBatchPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "class-name", scope = WanCustomPublisher.class)
    public JAXBElement<String> createWanCustomPublisherClassName(String value) {
        return new JAXBElement<String>(_ClassName_QNAME, String.class, WanCustomPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Properties }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "properties", scope = WanCustomPublisher.class)
    public JAXBElement<Properties> createWanCustomPublisherProperties(Properties value) {
        return new JAXBElement<Properties>(_HazelcastProperties_QNAME, Properties.class, WanCustomPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "publisher-id", scope = WanCustomPublisher.class)
    public JAXBElement<String> createWanCustomPublisherPublisherId(String value) {
        return new JAXBElement<String>(_WanBatchPublisherPublisherId_QNAME, String.class, WanCustomPublisher.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Join }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "join", scope = AdvancedNetwork.class)
    public JAXBElement<Join> createAdvancedNetworkJoin(Join value) {
        return new JAXBElement<Join>(_AdvancedNetworkJoin_QNAME, Join.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MemberAddressProvider }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "member-address-provider", scope = AdvancedNetwork.class)
    public JAXBElement<MemberAddressProvider> createAdvancedNetworkMemberAddressProvider(MemberAddressProvider value) {
        return new JAXBElement<MemberAddressProvider>(_AdvancedNetworkMemberAddressProvider_QNAME, MemberAddressProvider.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RestServerSocketEndpointConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "rest-server-socket-endpoint-config", scope = AdvancedNetwork.class)
    public JAXBElement<RestServerSocketEndpointConfig> createAdvancedNetworkRestServerSocketEndpointConfig(RestServerSocketEndpointConfig value) {
        return new JAXBElement<RestServerSocketEndpointConfig>(_AdvancedNetworkRestServerSocketEndpointConfig_QNAME, RestServerSocketEndpointConfig.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FailureDetector }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "failure-detector", scope = AdvancedNetwork.class)
    public JAXBElement<FailureDetector> createAdvancedNetworkFailureDetector(FailureDetector value) {
        return new JAXBElement<FailureDetector>(_AdvancedNetworkFailureDetector_QNAME, FailureDetector.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "wan-server-socket-endpoint-config", scope = AdvancedNetwork.class)
    public JAXBElement<ServerSocketEndpointConfig> createAdvancedNetworkWanServerSocketEndpointConfig(ServerSocketEndpointConfig value) {
        return new JAXBElement<ServerSocketEndpointConfig>(_AdvancedNetworkWanServerSocketEndpointConfig_QNAME, ServerSocketEndpointConfig.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "client-server-socket-endpoint-config", scope = AdvancedNetwork.class)
    public JAXBElement<ServerSocketEndpointConfig> createAdvancedNetworkClientServerSocketEndpointConfig(ServerSocketEndpointConfig value) {
        return new JAXBElement<ServerSocketEndpointConfig>(_AdvancedNetworkClientServerSocketEndpointConfig_QNAME, ServerSocketEndpointConfig.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "wan-endpoint-config", scope = AdvancedNetwork.class)
    public JAXBElement<EndpointConfig> createAdvancedNetworkWanEndpointConfig(EndpointConfig value) {
        return new JAXBElement<EndpointConfig>(_AdvancedNetworkWanEndpointConfig_QNAME, EndpointConfig.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "memcache-server-socket-endpoint-config", scope = AdvancedNetwork.class)
    public JAXBElement<ServerSocketEndpointConfig> createAdvancedNetworkMemcacheServerSocketEndpointConfig(ServerSocketEndpointConfig value) {
        return new JAXBElement<ServerSocketEndpointConfig>(_AdvancedNetworkMemcacheServerSocketEndpointConfig_QNAME, ServerSocketEndpointConfig.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "member-server-socket-endpoint-config", scope = AdvancedNetwork.class)
    public JAXBElement<ServerSocketEndpointConfig> createAdvancedNetworkMemberServerSocketEndpointConfig(ServerSocketEndpointConfig value) {
        return new JAXBElement<ServerSocketEndpointConfig>(_AdvancedNetworkMemberServerSocketEndpointConfig_QNAME, ServerSocketEndpointConfig.class, AdvancedNetwork.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanBatchPublisher }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "batch-publisher", scope = WanReplication.class)
    public JAXBElement<WanBatchPublisher> createWanReplicationBatchPublisher(WanBatchPublisher value) {
        return new JAXBElement<WanBatchPublisher>(_WanReplicationBatchPublisher_QNAME, WanBatchPublisher.class, WanReplication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanConsumer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "consumer", scope = WanReplication.class)
    public JAXBElement<WanConsumer> createWanReplicationConsumer(WanConsumer value) {
        return new JAXBElement<WanConsumer>(_WanReplicationConsumer_QNAME, WanConsumer.class, WanReplication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WanCustomPublisher }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "custom-publisher", scope = WanReplication.class)
    public JAXBElement<WanCustomPublisher> createWanReplicationCustomPublisher(WanCustomPublisher value) {
        return new JAXBElement<WanCustomPublisher>(_WanReplicationCustomPublisher_QNAME, WanCustomPublisher.class, WanReplication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "skip-endpoint", scope = BasicAuthentication.class)
    public JAXBElement<Boolean> createBasicAuthenticationSkipEndpoint(Boolean value) {
        return new JAXBElement<Boolean>(_BasicAuthenticationSkipEndpoint_QNAME, Boolean.class, BasicAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "skip-role", scope = BasicAuthentication.class)
    public JAXBElement<Boolean> createBasicAuthenticationSkipRole(Boolean value) {
        return new JAXBElement<Boolean>(_BasicAuthenticationSkipRole_QNAME, Boolean.class, BasicAuthentication.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.hazelcast.com/schema/config", name = "skip-identity", scope = BasicAuthentication.class)
    public JAXBElement<Boolean> createBasicAuthenticationSkipIdentity(Boolean value) {
        return new JAXBElement<Boolean>(_BasicAuthenticationSkipIdentity_QNAME, Boolean.class, BasicAuthentication.class, value);
    }

}
