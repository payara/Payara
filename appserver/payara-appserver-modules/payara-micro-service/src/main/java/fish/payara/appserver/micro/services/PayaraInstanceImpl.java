/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2017 Payara Foundation. All rights reserved.

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
package fish.payara.appserver.micro.services;

import fish.payara.micro.PayaraInstance;
import fish.payara.micro.event.PayaraClusterListener;
import fish.payara.micro.event.CDIEventListener;
import com.sun.enterprise.deployment.Application;
import fish.payara.appserver.micro.services.command.AsAdminCallable;
import fish.payara.appserver.micro.services.command.ClusterCommandResultImpl;
import fish.payara.micro.data.ApplicationDescriptor;
import fish.payara.appserver.micro.services.data.ApplicationDescriptorImpl;
import fish.payara.appserver.micro.services.data.InstanceDescriptorImpl;
import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.micro.event.PayaraClusteredCDIEvent;
import fish.payara.nucleus.cluster.PayaraCluster;
import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.MessageReceiver;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;

/**
 * Internal Payara Service for describing instances
 *
 * @author steve
 */
@Service(name = "payara-instance")
@RunLevel(StartupRunLevel.VAL)
@Contract()
public class PayaraInstanceImpl implements EventListener, MessageReceiver, PayaraInstance {

    public static final String INSTANCE_STORE_NAME = "payara.instance.store";

    public static final String INTERNAL_EVENTS_NAME = "payara.micro.cluster.event";

    public static final String CDI_EVENTS_NAME = "payara.micro.cdi.event";

    public static final String APPLICATIONS_STORE_NAME = "payara.micro.applications.store";    
    
    private static final Logger logger = Logger.getLogger(PayaraInstanceImpl.class.getName());

    @Inject
    private PayaraCluster cluster;

    @Inject
    private ServerContext context;

    @Inject
    private Events events;

    @Inject
    private CommandRunner commandRunner;

    private HashSet<PayaraClusterListener> myListeners;

    private HashSet<CDIEventListener> myCDIListeners;

    private String myCurrentID;

    private String instanceName;
    private String instanceGroup;

    private InstanceDescriptorImpl me;
    
    @Inject
    private ServerEnvironment environment;
    
    @Inject
    private HazelcastCore hazelcast;
    
    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        me.setInstanceName(instanceName);
    }
    
    

    @Override
    public <T extends Serializable> Map<String, Future<T>> runCallable(Collection<String> memberUUIDS, Callable<T> callable) {
        return cluster.getExecService().runCallable(memberUUIDS, callable);
    }

    @Override
    public <T extends Serializable> Map<String, Future<T>> runCallable(Callable<T> callable) {
        return cluster.getExecService().runCallableAllMembers(callable);
    }

    @Override
    public ClusterCommandResult executeLocalAsAdmin(String command, String... parameters) {
        return new ClusterCommandResultImpl(commandRunner.run(command, parameters));
    }

    @Override
    public Map<String, Future<ClusterCommandResult>> executeClusteredASAdmin(String command, String... parameters) {
        AsAdminCallable callable = new AsAdminCallable(command, parameters);
        Map<String, Future<ClusterCommandResult>> result = cluster.getExecService().runCallableAllMembers(callable);
        return result;
    }

    @Override
    public Map<String, Future<ClusterCommandResult>> executeClusteredASAdmin(Collection<String> memberGUIDs, String command, String... parameters) {
        AsAdminCallable callable = new AsAdminCallable(command, parameters);
        Map<String, Future<ClusterCommandResult>> result = cluster.getExecService().runCallable(memberGUIDs, callable);
        return result;
    }

    @Override
    public void receiveMessage(ClusterMessage msg) {
        if (msg.getPayload() instanceof PayaraInternalEvent) {
            PayaraInternalEvent pie = PayaraInternalEvent.class.cast(msg.getPayload());
            switch (pie.getMessageType()) {
                case ADDED:
                    for (PayaraClusterListener myListener : myListeners) {
                        myListener.memberAdded(pie.getId());
                    }
                    break;
                case REMOVED:
                    for (PayaraClusterListener myListener : myListeners) {
                        myListener.memberRemoved(pie.getId());
                    }
                    break;
            }

        } else if (msg.getPayload() instanceof PayaraClusteredCDIEventImpl) {
            PayaraClusteredCDIEventImpl cast = PayaraClusteredCDIEventImpl.class.cast(msg.getPayload());
            for (CDIEventListener myListener : myCDIListeners) {
                if (!cast.isLoopBack() && cast.getInstanceDescriptor().getMemberUUID().equals(myCurrentID)) {
                    // ignore this message as it is a loopback
                } else {
                    myListener.eventReceived(cast);
                }
            }
        }
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    void postConstruct() {
        events.register(this);
        myListeners = new HashSet<>(1);
        myCDIListeners = new HashSet<>(1);       
        initialiseInstanceDescriptor();
    }

    /**
     *
     * @param event
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            initialiseInstanceDescriptor();
            PayaraInternalEvent pie = new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.ADDED, me);
            ClusterMessage<PayaraInternalEvent> message = new ClusterMessage<>(pie);
            this.cluster.getEventBus().publish(INTERNAL_EVENTS_NAME, message);

        } 
        // Adds the application to the clustered register of deployed applications
        else if (event.is(Deployment.APPLICATION_LOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                me.addApplication(new ApplicationDescriptorImpl(applicationInfo));
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, me);
            }
        }
        // ensures the same application id is used when there is a deployment 
        // if the application id is in the cluster keyed by application name
        else if (event.is(Deployment.APPLICATION_PREPARED)) {
            if (event.hook() != null && event.hook() instanceof DeploymentContext) {
                DeploymentContext deploymentContext = (DeploymentContext) event.hook();
                Application app = deploymentContext.getModuleMetaData(Application.class);
                if(app != null) {
                    Long appID = (Long) cluster.getClusteredStore().get(APPLICATIONS_STORE_NAME, app.getName());
                    if (appID != null) {
                        app.setUniqueId(appID);
                    } else {
                        cluster.getClusteredStore().set(APPLICATIONS_STORE_NAME, app.getName(), app.getUniqueId());
                    }
                }
            }
        } 
        
        // removes the application from the clustered registry of applications
        else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                me.removeApplication(new ApplicationDescriptorImpl(applicationInfo));
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, me);
            }
        } else if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
            PayaraInternalEvent pie = new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.REMOVED, me);
            ClusterMessage<PayaraInternalEvent> message = new ClusterMessage<>(pie);
            this.cluster.getClusteredStore().remove(INSTANCE_STORE_NAME, myCurrentID);
            this.cluster.getEventBus().publish(INTERNAL_EVENTS_NAME, message);
        }
        
        // When Hazelcast is bootstrapped, update the instance descriptor with any new information
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
            initialiseInstanceDescriptor();
            // remove listener first
            cluster.getEventBus().removeMessageReceiver(INTERNAL_EVENTS_NAME, this);
            cluster.getEventBus().removeMessageReceiver(CDI_EVENTS_NAME, this);
            cluster.getEventBus().addMessageReceiver(INTERNAL_EVENTS_NAME, this);
            cluster.getEventBus().addMessageReceiver(CDI_EVENTS_NAME, this);
        }
        
        // If the generated name had to be changed, update the instance descriptor with the new information
        if (event.is(HazelcastEvents.HAZELCAST_GENERATED_NAME_CHANGE)) {
            initialiseInstanceDescriptor();
        }
    }

    @Override
    public Set<InstanceDescriptor> getClusteredPayaras() {
        Set<String> members = cluster.getClusterMembers();
        HashSet<InstanceDescriptor> result = new HashSet<>(members.size());
        for (String member : members) {
            InstanceDescriptor id = (InstanceDescriptor) cluster.getClusteredStore().get(INSTANCE_STORE_NAME, member);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    @Override
    public void publishCDIEvent(PayaraClusteredCDIEvent event) {
        if (event.getInstanceDescriptor() == null) {
            event.setId(me);
        }
        ClusterMessage<PayaraClusteredCDIEvent> message = new ClusterMessage<>(event);
        cluster.getEventBus().publish(CDI_EVENTS_NAME, message);
    }

    @Override
    public void removeBootstrapListener(PayaraClusterListener listener) {
        myListeners.remove(listener);
    }

    @Override
    public void addBootstrapListener(PayaraClusterListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeCDIListener(CDIEventListener listener) {
        myCDIListeners.remove(listener);
    }

    @Override
    public void addCDIListener(CDIEventListener listener) {
        myCDIListeners.add(listener);
    }

    @Override
    public InstanceDescriptor getLocalDescriptor() {
        return me;
    }

    @Override
    public InstanceDescriptorImpl getDescriptor(String member) {
        InstanceDescriptorImpl result = null;
        if (cluster.isEnabled()) {
            result = (InstanceDescriptorImpl) cluster.getClusteredStore().get(INSTANCE_STORE_NAME, member);
        }
        return result;
    }

    private void initialiseInstanceDescriptor() {
        boolean liteMember = false;
        int hazelcastPort = 5900;
        
        // Get the Hazelcast specific information
        if (hazelcast.isEnabled()) {
            instanceName = hazelcast.getMemberName();
            instanceGroup = hazelcast.getMemberGroup();
            myCurrentID = hazelcast.getUUID();
            liteMember = hazelcast.isLite();
            hazelcastPort = hazelcast.getPort();
        } else {
            instanceName = "payara-micro";
            instanceGroup = "no-cluster";
        }
        
        // Get this instance's runtime type
        String instanceType = environment.getRuntimeType().toString();
        
        // Get the ports in use by this instance from its network listener configs
        List<Integer> ports = new ArrayList<>();
        List<Integer> sslPorts = new ArrayList<>();
        int adminPort = 0;
        for (NetworkListener networkListener : 
                context.getConfigBean().getConfig().getNetworkConfig().getNetworkListeners().getNetworkListener()) {  
            // Skip the network listener if it isn't enabled
            if (Boolean.parseBoolean(networkListener.getEnabled())) {
                // Check if this listener is using HTTP or HTTPS
                if (networkListener.findProtocol().getSecurityEnabled().equals("false")) {
                    // Check if this listener is the admin listener
                    if (networkListener.getName().equals(
                            context.getConfigBean().getConfig().getAdminListener().getName())) {
                        // Micro instances can use the admin listener as both an admin and HTTP port
                        if (instanceType.equals("MICRO")) {
                            ports.add(Integer.parseInt(networkListener.getPort()));
                        }
                        adminPort = Integer.parseInt(networkListener.getPort());
                    } else {
                        // If this listener isn't the admin listener, it must be an HTTP listener
                        ports.add(Integer.parseInt(networkListener.getPort()));
                    }
                } else if (networkListener.findProtocol().getSecurityEnabled().equals("true")) {
                    if (networkListener.getName().equals(
                            context.getConfigBean().getConfig().getAdminListener().getName())) {
                        // Micro instances can use the admin listener as both an admin and HTTPS port
                        if (instanceType.equals("MICRO")) {
                            ports.add(Integer.parseInt(networkListener.getPort()));
                        }
                        adminPort = Integer.parseInt(networkListener.getPort());
                    } else {
                        // If this listener isn't the admin listener, it must be an HTTPS listener
                        sslPorts.add(Integer.parseInt(networkListener.getPort()));
                    }
                }
            }
        } 
        
        // Initialise the instance descriptor and set all of its attributes
        try {
            // If Hazelcast is being rebooted dynamically, we don't want to lose the already registered applications
            Collection<ApplicationDescriptor> deployedApplications = new ArrayList<>();
            if (me != null) {
                deployedApplications = me.getDeployedApplications();
            }
            me = new InstanceDescriptorImpl(myCurrentID);
            me.setInstanceName(instanceName);
            me.setInstanceGroup(instanceGroup);
            for (int port : ports) {
                me.addHttpPort(port);
            }
            
            for (int sslPort : sslPorts) {
                me.addHttpsPort(sslPort);
            }
            
            me.setAdminPort(adminPort);
            me.setHazelcastPort(hazelcastPort);
            me.setLiteMember(liteMember);
            me.setInstanceType(instanceType);
            
            // If there were some deployed applications from the previous instance descriptor, register them with the new 
            // one
            if (!deployedApplications.isEmpty()) {
                for (ApplicationDescriptor application : deployedApplications) {
                    me.addApplication(application);
                }
            }
            
            // Register the instance descriptor to the cluster if it's enabled
            if (cluster.isEnabled()) {
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, me);
            }  
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, "Could not find local hostname", ex);
        }
    }
    
    /**
     * Checks whether or not this instance is in a Hazelcast cluster
     * @return true if this instance is in a Hazelcast cluster
     */
    @Override
    public boolean isClustered() {
        return cluster.isEnabled();
    }
}
