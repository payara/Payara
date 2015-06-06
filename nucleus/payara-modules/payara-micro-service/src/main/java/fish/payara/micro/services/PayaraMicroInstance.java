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
import com.hazelcast.core.Member;
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
 *
 * @author steve
 */
@Service(name = "payara-micro-instance")
@RunLevel(StartupRunLevel.VAL)
public class PayaraMicroInstance implements EventListener {
    
    private static final Logger logger = Logger.getLogger(PayaraMicroInstance.class);
    
    @Inject
    HazelcastCore hazelcast;
    
    @Inject
    ServerContext context;
        
    @Inject
    Events events;

    
    @Inject
    CommandRunner commandRunner;
    
    IMap<String,InstanceDescriptor> payaraMicroMap;
    
    String myCurrentID;
    
    String instanceName;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
    
    
    public ClusterCommandRunner getClusterCommandRunner(Collection<InstanceDescriptor> members) {
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
    public void postConstruct() {
        events.register(this);
        
        HazelcastInstance instance = hazelcast.getInstance();
        if (hazelcast.isEnabled()) {
            try {
                payaraMicroMap = instance.getMap("PayaraMicro");
                
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
    @SuppressWarnings("unchecked")
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            
        } else if (event.is(Deployment.APPLICATION_LOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                    ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                    // get my instance ID
                    InstanceDescriptor descriptor = payaraMicroMap.get(myCurrentID);
                    descriptor.addApplication(applicationInfo);
                    payaraMicroMap.set(myCurrentID, descriptor);
            }            
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
             if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                     ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                    // get my instance ID
                    InstanceDescriptor descriptor = payaraMicroMap.get(myCurrentID);
                    descriptor.removeApplication(applicationInfo);
                    payaraMicroMap.set(myCurrentID, descriptor);
           }
        }
    }
    
    public List<InstanceDescriptor> getClusteredPayaras() {
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
    
}
