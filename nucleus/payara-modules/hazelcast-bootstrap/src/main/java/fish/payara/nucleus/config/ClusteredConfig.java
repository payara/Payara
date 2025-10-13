/*
 * Copyright (c) 2020-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.config;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipAdapter;
import com.hazelcast.cluster.MembershipEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.replicatedmap.ReplicatedMap;

import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.UUID;

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

    private UUID membershipListenerRegistrationId;

    /**
     * The names of the {@link ReplicatedMap}s holding the instances values of shared configurations.
     *
     * This uses {@link ReplicatedMap}s as responsiveness is important and eventual consistency good enough.
     */
    private final Set<String> sharedConfigurations = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void postConstruct() {
        HazelcastInstance hzInstance = hzCore.getInstance();
        if (hzInstance != null) {
            membershipListenerRegistrationId = hzInstance.getCluster().addMembershipListener(this);
        }
    }

    @PreDestroy
    public void preDestroy() {
        HazelcastInstance hzInstance = hzCore.getInstance();
        if (hzInstance != null && membershipListenerRegistrationId != null) { // can already be null when HZ was shutdown before
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
            Member localMember = hzInstance.getCluster().getLocalMember();
            String instance = instanceName(localMember);
            String mapName = CONFIGURATION_PREFIX + name;
            if (instance != null && !localMember.isLiteMember()) {
                hzInstance.getReplicatedMap(mapName).remove(instance);
            }
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

    private String instanceName(Member member) {
        return hzCore.getAttribute(member.getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE);
    }
}
