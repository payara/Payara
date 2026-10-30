/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2023] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Member;
import com.hazelcast.internal.partition.membergroup.DefaultMemberGroup;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.partitiongroup.MemberGroup;
import com.hazelcast.spi.partitiongroup.PartitionGroupStrategy;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.io.InstanceDirs;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.grizzly.utils.Holder;
import org.glassfish.internal.api.Globals;
import static fish.payara.nucleus.hazelcast.HazelcastCore.INSTANCE_GROUP_ATTRIBUTE;
import static fish.payara.nucleus.hazelcast.MemberAddressPicker.initAddress;
import static fish.payara.nucleus.hazelcast.PayaraHazelcastDiscoveryFactory.HOST_AWARE_PARTITIONING;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Delegate to the Domain Discovery Service for node discovery.
 * <p>
 * Separate different domain instance groups into partition groups,
 * and optionally implements host-aware partitioning
 * since it's not possible to combine Hazelcast's (very) simple implementation
 * of host-aware partitioning and custom API-based discovery strategy
 *
 * @author lprimak
 * @author steve
 */
public final class DomainDiscoveryStrategy extends AbstractDiscoveryStrategy {
    private final boolean hostAwarePartitioning;
    private final Holder.LazyHolder<InetAddress> chosenAddress =
            Holder.LazyHolder.lazyHolder(MemberAddressPicker::findMyAddressOrLocalHost);

    @SuppressWarnings("rawtypes")
    public DomainDiscoveryStrategy(ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        hostAwarePartitioning = getOrDefault("fish.payara.cluster", HOST_AWARE_PARTITIONING, Boolean.FALSE);
    }

    /**
     * Provides a Discovery SPI implementation for Hazelcast that uses knowledge
     * of the domain topology to build out the cluster and discover members
     * @since 5.0
     */
    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        getLogger().fine("Starting Domain Node Discovery");
        List<DiscoveryNode> nodes = new LinkedList<>();
        Domain domain = Globals.getDefaultHabitat().getService(Domain.class);
        ServerEnvironment env = Globals.getDefaultHabitat().getService(ServerEnvironment.class);
        // add the DAS
        HazelcastRuntimeConfiguration hzConfig = domain.getExtensionByType(HazelcastRuntimeConfiguration.class);

        if (!env.isDas()) {
            try {
                // first get hold of the DAS host
                getLogger().fine("This is a Standalone Instance");
                String dasHost = hzConfig.getDASPublicAddress();
                if (dasHost == null || dasHost.isEmpty()) {
                    dasHost = hzConfig.getDASBindAddress();
                }
                dasHost = Optional.ofNullable(initAddress(dasHost, 0)).map(InetSocketAddress::getHostString).orElse("");

                if (dasHost.isEmpty()) {
                    // ok drag it off the properties file
                    getLogger().fine("Neither DAS Public Address or Bind Address is set in the configuration");
                    InstanceDirs instance = new InstanceDirs(env.getInstanceRoot());
                    Properties dasProps = new Properties();
                    dasProps.load(new FileInputStream(instance.getDasPropertiesFile()));
                    getLogger().fine("Loaded the das.properties file from the agent directory");
                    dasHost = dasProps.getProperty("agent.das.host");
                    // then do an IP lookup
                    dasHost = InetAddress.getByName(dasHost).getHostAddress();
                    getLogger().log(Level.FINE, String.format("Loaded the das.properties file from the agent directory and found DAS IP %s", dasHost));
                }

                if (dasHost.isEmpty() || dasHost.equals("127.0.0.1") || dasHost.equals("localhost")) {
                    getLogger().fine("Looks like the DAS IP is loopback or empty let's find the actual IP of this machine as that is where the DAS is");
                    nodes.add(new SimpleDiscoveryNode(new Address(chosenAddress.get(), Integer.parseInt(hzConfig.getDasPort()))));
                } else {
                    getLogger().log(Level.FINE, String.format("DAS should be listening on %s", dasHost));
                    nodes.add(new SimpleDiscoveryNode(new Address(dasHost, Integer.valueOf(hzConfig.getDasPort()))));
                }

                // also add all nodes we are aware of in the domain to see if we can get in using start port
                getLogger().fine("Also adding all known domain nodes and start ports in case the DAS is down");
                for (Node node : domain.getNodes().getNode()) {
                    InetAddress address = InetAddress.getByName(node.getNodeHost());
                    if (!address.isLoopbackAddress()) {
                        getLogger().log(Level.FINE, String.format("Adding Node %s", address));
                        nodes.add(new SimpleDiscoveryNode(new Address(address.getHostAddress(), Integer.valueOf(hzConfig.getStartPort()))));
                    }
                }
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Exception", ex);
            }

        } else if (env.isMicro()) {
            try {
                getLogger().log(Level.FINE, String.format("We are Payara Micro therefore adding DAS %s", hzConfig.getDASPublicAddress()));
                // check if user has added locahost as unlikely to work
                String dasHost = Optional.ofNullable(initAddress(hzConfig.getDASPublicAddress(), 0))
                        .map(InetSocketAddress::getHostString).orElse("");
                if (hzConfig.getDasPort().equals("4848")) {
                    getLogger().log(Level.WARNING,"You have specified 4848 as the datagrid domain port however this is the default DAS admin port, the default domain datagrid port is 4900");
                }
                if (dasHost.isEmpty() || dasHost.equals("127.0.0.1") || dasHost.equals("localhost")) {
                    nodes.add(new SimpleDiscoveryNode(new Address(chosenAddress.get(), Integer.parseInt(hzConfig.getDasPort()))));
                } else {
                    nodes.add(new SimpleDiscoveryNode(new Address(InetAddress.getByName(dasHost), Integer.valueOf(hzConfig.getDasPort()))));
                }
            } catch (UnknownHostException | NumberFormatException ex) {
                getLogger().log(Level.SEVERE, "Exception", ex);
            }
        } else {
            // ok this is the DAS
            getLogger().fine("We are the DAS therefore we will add all known nodes with start port as IP addresses to connect to");

            // Embedded runtimese don't have nodes
            if (domain.getNodes() == null) {
                nodes.add(new SimpleDiscoveryNode(new Address(chosenAddress.get(), Integer.parseInt(hzConfig.getStartPort()))));
            } else {
                for (Node node : domain.getNodes().getNode()) {
                    try {
                        InetAddress address = InetAddress.getByName(node.getNodeHost());
                        if (!address.isLoopbackAddress()) {
                            getLogger().log(Level.FINE, String.format("Adding Node %s", address));
                            nodes.add(new SimpleDiscoveryNode(new Address(address.getHostAddress(),
                                    Integer.valueOf(hzConfig.getStartPort()))));
                        } else {
                            // we need to add our IP address so add each interface address with the start port
                            nodes.add(new SimpleDiscoveryNode(new Address(chosenAddress.get(), Integer.parseInt(hzConfig.getStartPort()))));
                        }
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Exception", ex);
                    }
                }
            }
        }

        return nodes;
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
