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
package fish.payara.nucleus.cluster;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import fish.payara.nucleus.eventbus.EventBus;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.exec.ClusterExecutionService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.store.ClusteredStore;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * A Hazelcast based Cluster for Payara
 *
 * @author steve
 */
@Service(name = "payara-cluster")
@RunLevel(StartupRunLevel.VAL)
public class PayaraCluster implements MembershipListener, EventListener {

    private static final Logger logger = Logger.getLogger(PayaraCluster.class.getCanonicalName());
    
    @Inject
    Events events;

    @Inject
    private HazelcastCore hzCore;

    @Inject
    private ClusterExecutionService execService;

    @Inject
    private ClusteredStore clusteredStore;

    @Inject
    private EventBus eventBus;

    private Set<ClusterListener> myListeners;

    private UUID localUUID;
    
    public boolean isEnabled() {
        return hzCore.isEnabled();
    }

    public ClusterExecutionService getExecService() {
        return execService;
    }
    
    public HazelcastCore getUnderlyingHazelcastService() {
        return hzCore;
    }

    public ClusteredStore getClusteredStore() {
        return clusteredStore;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void memberAdded(MembershipEvent me) {
            for (ClusterListener myListener : myListeners) {
                myListener.memberAdded(new MemberEvent(hzCore, me.getMember()));
            }
            logger.log(Level.INFO, "Data Grid Instance Added {0} at Address {1}", new String[]{me.getMember().getUuid().toString(), me.getMember().getSocketAddress().toString()});
            logClusterStatus();
    }

    @Override
    public void memberRemoved(MembershipEvent me) {
        for (ClusterListener myListener : myListeners) {
            myListener.memberRemoved(new MemberEvent(hzCore, me.getMember()));
        }
        logger.log(Level.INFO, "Data Grid Instance Removed {0} from Address {1}", new String []{me.getMember().getUuid().toString(), me.getMember().getSocketAddress().toString()});
        logClusterStatus();
    }

    public UUID getLocalUUID() {
        return localUUID;
    }

    public void removeClusterListener(ClusterListener listener) {
        myListeners.remove(listener);
    }

    public void addClusterListener(ClusterListener listener) {
        myListeners.add(listener);
    }

    public Set<UUID> getClusterMembers() {
        HashSet<UUID> result = new HashSet<>(5);

        if (hzCore.isEnabled()) {
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            for (Member member : members) {
                result.add(member.getUuid());
            }
        }
        return result;
    }
    
    public String getMemberName(UUID memberUUID) {
        String result = null;
        if (hzCore.isEnabled()) {
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            for (Member member : members) {
                if (member.getUuid().equals(memberUUID)) {
                    result = hzCore.getAttribute(member.getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE);
                }
            }                        
        }
        return result;
    }
    
    public String getMemberGroup(UUID memberUUID) {
        String result = null;
        if (hzCore.isEnabled()) {
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            for (Member member : members) {
                if (member.getUuid().equals(memberUUID)) {
                    result = member.getAttribute(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE);
                }
            }                        
        }
        return result;
    }

    
    public Set<String> getMemberNames() {
        HashSet<String> result = new HashSet<>(5);
        if (hzCore.isEnabled()) {
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            for (Member member : members) {
                String memberName = hzCore.getAttribute(member.getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE);
                if (memberName != null) {
                    result.add(memberName);
                }
            }            
        }
        return result;
    }
    
    public Set<String> getMemberNamesInGroup(String groupName) {
        HashSet<String> result = new HashSet<>(5);
        if (hzCore.isEnabled()) {
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            for (Member member : members) {
                String memberName = hzCore.getAttribute(member.getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE);
                String memberGroupName = member.getAttribute(HazelcastCore.INSTANCE_GROUP_ATTRIBUTE);
                if (memberName != null && memberGroupName != null && groupName.equals(memberGroupName)) {
                    result.add(memberName);
                }
            }            
        }
        return result;        
    }

    @PostConstruct
    void postConstruct() {
        events.register(this);
        myListeners = ConcurrentHashMap.newKeySet(2);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void event(Event event) {
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)){
            if (hzCore.isEnabled()) {
                logger.config("Payara Data Grid Service Enabled");
                logClusterStatus();
                Cluster cluster = hzCore.getInstance().getCluster();
                localUUID = cluster.getLocalMember().getUuid();
                cluster.addMembershipListener(this);
            }          
        }
    }
    
    private void logClusterStatus() {
        StringBuilder message = new StringBuilder();
        String NL = System.lineSeparator();
        message.append(NL);
        if (hzCore.isEnabled()) {
            String dataGridName = hzCore.getInstance().getConfig().getClusterName();
            Cluster cluster = hzCore.getInstance().getCluster();
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            message.append("Payara Data Grid State: DG Version: ").append(cluster.getClusterVersion().getMajor());
            message.append(" DG Name: ").append(dataGridName);
            message.append(" DG Size: ").append(members.size());
            message.append(NL);
            message.append("Instances: {").append(NL);
            for (Member member : members) {
                String name = hzCore.getAttribute(member.getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE);
                String group = hzCore.getAttribute(member.getUuid(), HazelcastCore.INSTANCE_GROUP_ATTRIBUTE);
                message.append(" DataGrid: ").append(dataGridName);
                if (group != null) {
                    message.append(" Instance Group: ").append(group);
                }

                if (name != null) {
                    message.append(" Name: ").append(name);
                }
                message.append(" Lite: ").append(Boolean.toString(member.isLiteMember()));
                message.append(" This: ").append(Boolean.toString(member.localMember()));
                message.append(" UUID: ").append(member.getUuid());
                message.append(" Address: ").append(member.getSocketAddress());

                message.append(NL);
            }
            message.append("}");
            logger.info("Data Grid Status " + message);
        }
    }

}
