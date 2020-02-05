package fish.payara.nucleus.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipAdapter;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.ReplicatedMap;

import fish.payara.nucleus.hazelcast.HazelcastCore;

/**
 * The {@link ClusteredConfig} can hold configurations that should have a common values that is based on local
 * configuration values but at the same time accessible to all instances in the cluster.
 * 
 * Such cases are not supported by the configuration originating from {@code domain.xml}. First of all non DAS instances
 * only have their own data available. Secondly with micro instances involved multiple instances can see themselves as
 * the DAS instance each with its own local value.
 * 
 * This configurations allows to take a local configuration property and share it with the other instances. The
 * effective value for each instance will be computed from a merge {@link BiFunction} on the basis of the local values
 * shared by the other instances.
 * 
 * @author Jan Bernitt
 * @since 5.201
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class ClusteredConfig extends MembershipAdapter {

    /**
     * Map name prefix used for cluster configuration as accessed using
     * {@link #getSharedConfiguration(String, Number, BiFunction)}.
     */
    private static final String CONFIGURATION_PREFIX = "fish.payara.config:";

    @Inject
    private HazelcastCore hzCore;

    private String membershipListenerRegistrationId;

    /**
     * The names of the {@link ReplicatedMap}s holding the instances values of shared configurations.
     * 
     * This uses {@link ReplicatedMap}s as responsiveness is important and eventual consistency good enough.
     */
    private final Set<String> sharedConfigurations = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void postConstruct() {
        membershipListenerRegistrationId = hzCore.getInstance().getCluster().addMembershipListener(this);
    }

    @PreDestroy
    public void preDestroy() {
        HazelcastInstance hzInstance = hzCore.getInstance();
        if (hzInstance != null) { // can already be null when HZ was shutdown before
            hzInstance.getCluster().removeMembershipListener(membershipListenerRegistrationId);
        }
    }

    /**
     * Accesses and merges a shared configuration property.
     * 
     * Shared configurations are values that use a common value for each instance. The value used is computed by a merge
     * function from all local values when those become relevant. For example a local value of a disabled feature for
     * that instance is not relevant and therefore not considered for the shared value.
     * 
     * In practice this means when an instance is accessing a value that is a common or shared configuration it calls
     * this method with its local configuration value which makes it effective for this and other instances.
     * 
     * @param name       the globally unique name for the configuration property to read
     * @param localValue the value as configured locally or a fallback or default value
     * @param merge      the function to use to resolve both local and shared value being present. The resolved value is
     *                   always going to be the new shared value. If no value was shared so far the local value is
     *                   always the new shared and result value.
     * @return the merged value, local value is the default in case no other instances shared the configuration
     */
    public <T extends Number> T getSharedConfiguration(String name, T localValue, BiFunction<T, T, T> merge) {
        HazelcastInstance hzInstance = hzCore.getInstance();
        if (hzInstance == null) { // can be null during shutdown
            return localValue;
        }
        String instance = instanceName(hzInstance.getCluster().getLocalMember());
        String mapName = CONFIGURATION_PREFIX + name;
        sharedConfigurations.add(mapName);
        ReplicatedMap<String, T> map = hzInstance.getReplicatedMap(mapName);
        map.put(instance, localValue);
        T res = localValue;
        for (T e : map.values()) {
            res = merge.apply(res, e);
        }
        return res;
    }

    /**
     * Can be used to clear the shared value of this instance before the instance is shut down.
     * 
     * @param name the globally unique name for the configuration property to clear
     */
    public void clearSharedConfiguration(String name) {
        HazelcastInstance hzInstance = hzCore.getInstance();
        if (hzInstance != null) { // can be null during shutdown
            String instance = instanceName(hzInstance.getCluster().getLocalMember());
            String mapName = CONFIGURATION_PREFIX + name;
            hzInstance.getReplicatedMap(mapName).remove(instance);
        }
    }

    @Override
    public void memberRemoved(MembershipEvent event) {
        // remove configuration values from that instance so they no longer have an effect
        String instance = instanceName(event.getMember());
        for (String mapName : sharedConfigurations) {
            hzCore.getInstance().getReplicatedMap(mapName).remove(instance);
        }
    }

    private static String instanceName(Member member) {
        return member.getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
    }
}
