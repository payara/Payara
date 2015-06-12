/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.micro.services;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import fish.payara.micro.services.command.ClusterCommandRunner;
import fish.payara.micro.services.data.InstanceDescriptor;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jboss.logging.Logger;
import org.jvnet.hk2.annotations.Service;

/**
 * Internal Payara Micro Service
 *
 * @author steve
 */
@Service(name = "payara-micro-instance")
@RunLevel(StartupRunLevel.VAL)
public class PayaraMicroInstance implements EventListener, MembershipListener, MessageListener {

    private static final Logger logger = Logger.getLogger(PayaraMicroInstance.class);

    @Inject
    private HazelcastCore hazelcast;

    @Inject
    private ServerContext context;

    @Inject
    private Events events;

    @Inject
    private CommandRunner commandRunner;

    private IMap<String, InstanceDescriptor> payaraMicroMap;
    
    private HashSet<PayaraClusterListener> myListeners;

    private ITopic payaraCDIBus;
    private String cdiBusRegistration;

    private ITopic payaraInternalBus;
    private String internalBusRegistration;

    private String myCurrentID;

    private String instanceName;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public ClusterCommandRunner getClusterCommandRunner(Collection<InstanceDescriptor> members) {

        if (!hazelcast.isEnabled()) {
            return new ClusterCommandRunner();
        }

        HazelcastInstance instance;
        instance = hazelcast.getInstance();
        Set<Member> allMembers = instance.getCluster().getMembers();
        Set<Member> hzMembers = new HashSet<>(members.size());
        for (InstanceDescriptor allMember : members) {
            for (Member hzMember : instance.getCluster().getMembers()) {
                if (allMember.getMemberUUID().equals(hzMember.getUuid())) {
                    hzMembers.add(hzMember);
                    continue;
                }
            }
        }
        ClusterCommandRunner result = new ClusterCommandRunner(instance, hzMembers);
        return result;
    }

    /**
     *
     * @return
     */
    public CommandRunner getCommandRunner() {
        return commandRunner;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void postConstruct() {
        events.register(this);
        myListeners = new HashSet<>(10);

        if (hazelcast.isEnabled()) {
            try {
                HazelcastInstance instance = hazelcast.getInstance();
                payaraMicroMap = instance.getMap("PayaraMicro");
                payaraCDIBus = instance.getTopic("PayaraMicroCDIBus");
                payaraCDIBus.addMessageListener(this);
                payaraInternalBus = instance.getTopic("PayaraMicroInternalBus");
                payaraInternalBus.addMessageListener(this);

                // generate unique key for this instance in the Map
                myCurrentID = instance.getLocalEndpoint().getUuid();

                // determine https Port
                NetworkListener httpListener = context.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("http-listener");
                int port = Integer.parseInt(httpListener.getPort());
                boolean httpPortEnabled = Boolean.parseBoolean(httpListener.getEnabled());
                NetworkListener httpsListener = context.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("https-listener");
                int sslPort = Integer.parseInt(httpsListener.getPort());
                boolean httpsPortEnabled = Boolean.parseBoolean(httpsListener.getEnabled());

                InstanceDescriptor id = new InstanceDescriptor(myCurrentID);
                if (httpPortEnabled) {
                    id.setHttpPort(port);
                }

                if (httpsPortEnabled) {
                    id.setHttpsPort(sslPort);
                }

                id.setInstanceName(instanceName);

                payaraMicroMap.put(myCurrentID, id);

            } catch (UnknownHostException ex) {
                java.util.logging.Logger.getLogger(PayaraMicroInstance.class.getName()).log(Level.SEVERE, "Could not find local hostname", ex);
            }
        }

    }

    /**
     *
     * @param event
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            if (payaraInternalBus != null) {
                payaraInternalBus.publish(new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.ADDED, payaraMicroMap.get(myCurrentID)));
            }
        } else if (event.is(Deployment.APPLICATION_LOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                // get my instance ID
                if (payaraMicroMap != null) {
                    InstanceDescriptor descriptor = payaraMicroMap.get(myCurrentID);
                    descriptor.addApplication(applicationInfo);
                    payaraMicroMap.set(myCurrentID, descriptor);
                }

            }
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                // get my instance ID
                if (payaraMicroMap != null) {
                    InstanceDescriptor descriptor = payaraMicroMap.get(myCurrentID);
                    descriptor.removeApplication(applicationInfo);
                    payaraMicroMap.set(myCurrentID, descriptor);
                }
            }
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            if (payaraMicroMap != null) {
                payaraInternalBus.publish(new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.REMOVED, payaraMicroMap.get(myCurrentID)));
                payaraMicroMap.remove(myCurrentID);
                payaraCDIBus.removeMessageListener(cdiBusRegistration);
                payaraInternalBus.removeMessageListener(internalBusRegistration);
            }
        }
    }

    public List<InstanceDescriptor> getClusteredPayaras() {

        if (!hazelcast.isEnabled()) {
            return new ArrayList<>(0);
        }
        HazelcastInstance instance = hazelcast.getInstance();
        Set<Member> members = instance.getCluster().getMembers();
        List<InstanceDescriptor> result = new ArrayList<>(members.size());
        for (Member member : members) {
            String key = member.getUuid();
            InstanceDescriptor descriptor = payaraMicroMap.get(key);
            if (descriptor != null) {
                result.add(descriptor);
            }
        }
        return result;
    }

    @Override
    public void memberAdded(MembershipEvent me) {
        // currently do nothing
    }

    @Override
    public void memberRemoved(MembershipEvent me) {
        // remove the member from the map if it is present
        payaraMicroMap.removeAsync(me.getMember().getUuid());
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent mae) {

    }

    @Override
    public void onMessage(Message msg) {
        if (msg.getMessageObject() instanceof PayaraInternalEvent) {
            PayaraInternalEvent pie = PayaraInternalEvent.class.cast(msg.getMessageObject());
            switch (pie.getMessageType()) {
                case ADDED:
                for (PayaraClusterListener myListener : myListeners) {
                    myListener.memberAdded(pie.getId());
                }
                break;
                case REMOVED:
                for (PayaraClusterListener myListener : myListeners) {
                    myListener.memberAdded(pie.getId());
                }
                break;                    
            }

        } else if (msg.getMessageObject() instanceof PayaraClusteredCDIEvent) {

        }
    }
    
    public void removeBootstrapListenr(PayaraClusterListener listener) {
        myListeners.remove(listener);
    }
    
    
    public void addBootstrapListener(PayaraClusterListener listener) {
        myListeners.add(listener);
    }

}
