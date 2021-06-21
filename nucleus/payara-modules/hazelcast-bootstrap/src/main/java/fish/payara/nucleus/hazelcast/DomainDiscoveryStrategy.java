/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.hazelcast.cluster.Member;
import com.hazelcast.internal.partition.membergroup.DefaultMemberGroup;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.partitiongroup.MemberGroup;
import com.hazelcast.spi.partitiongroup.PartitionGroupStrategy;
import static fish.payara.nucleus.hazelcast.HazelcastCore.INSTANCE_GROUP_ATTRIBUTE;
import static fish.payara.nucleus.hazelcast.PayaraHazelcastDiscoveryFactory.HOST_AWARE_PARTITIONING;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate to the Domain Discovery Service for node discovery.
 * <p>
 * Separate different domain instance groups into partition groups,
 * and optionally implements host-aware partitioning
 * since it's not possible to combine Hazelcast's (very) simple implementation
 * of host-aware partitioning and custom API-based discovery strategy
 *
 * @author lprimak
 */
public final class DomainDiscoveryStrategy extends AbstractDiscoveryStrategy {
    private final DomainDiscoveryService discovery = new DomainDiscoveryService();
    private final boolean hostAwarePartitioning;

    @SuppressWarnings("rawtypes")
    public DomainDiscoveryStrategy(ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        hostAwarePartitioning = getOrDefault("fish.payara.cluster", HOST_AWARE_PARTITIONING, Boolean.FALSE);
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        getLogger().finest("Partition - Discover Nodes");
        return discovery.discoverNodes();
    }

    @Override
    public PartitionGroupStrategy getPartitionGroupStrategy(Collection<? extends Member> allMembers) {
        getLogger().finest("Getting Partition Strategy");
        Map<String, MemberGroup> memberGroups = new HashMap<>();
        collectMembers(allMembers, memberGroups);
        return memberGroups::values;
    }

    private void collectMembers(Collection<? extends Member> members, Map<String, MemberGroup> memberGroups) {
        for (Member member : members) {
            StringBuilder sb = new StringBuilder();
            sb.append(member.getAttribute(INSTANCE_GROUP_ATTRIBUTE));
            if (hostAwarePartitioning) {
                sb.append("|");
                sb.append(member.getAddress().getHost());
            }
            memberGroups.compute(sb.toString(), (key, val) -> {
                if (val == null) {
                    val = new DefaultMemberGroup();
                }
                val.addMember(member);
                getLogger().fine(String.format("added %s - %s to %s", member.getAddress(),
                        member.getUuid(), key));
                return val;
            });
        }
    }
}
