/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

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
package fish.payara.nucleus.cluster;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import fish.payara.nucleus.eventbus.EventBus;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.exec.ClusterExecutionService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.store.ClusteredStore;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
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

    private static final Logger logger = Logger.getLogger(ClusteredStore.class.getCanonicalName());
    
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

    private String localUUID;
    
    public boolean isEnabled() {
        return hzCore.isEnabled();
    }

    public ClusterExecutionService getExecService() {
        return execService;
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
            myListener.memberAdded(me.getMember().getUuid());
        }
    }

    @Override
    public void memberRemoved(MembershipEvent me) {
        for (ClusterListener myListener : myListeners) {
            myListener.memberRemoved(me.getMember().getUuid());
        }
    }

    public String getLocalUUID() {
        return localUUID;
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent mae) {
    }

    public void removeClusterListener(ClusterListener listener) {
        myListeners.remove(listener);
    }

    public void addClusterListener(ClusterListener listener) {
        myListeners.add(listener);
    }

    public Set<String> getClusterMembers() {
        HashSet<String> result = new HashSet<String>(5);

        if (hzCore.isEnabled()) {
            Set<Member> members = hzCore.getInstance().getCluster().getMembers();
            for (Member member : members) {
                result.add(member.getUuid());
            }
        }
        return result;
    }

    @PostConstruct
    void postConstruct() {
        events.register(this);
        myListeners = new HashSet<ClusterListener>(2);
    }

    @Override
    public void event(Event event) {
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)){
            if (hzCore.isEnabled()) {
                logger.info("Payara Cluster Service Enabled");
                Cluster cluster = hzCore.getInstance().getCluster();
                localUUID = cluster.getLocalMember().getUuid();
                cluster.addMembershipListener(this);
            }          
        }
    }

}
