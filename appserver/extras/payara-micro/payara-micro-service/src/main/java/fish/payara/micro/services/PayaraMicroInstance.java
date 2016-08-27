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
package fish.payara.micro.services;

import com.sun.enterprise.deployment.Application;
import fish.payara.micro.services.command.AsAdminCallable;
import fish.payara.micro.services.command.ClusterCommandResult;
import fish.payara.micro.services.data.InstanceDescriptor;
import fish.payara.nucleus.cluster.PayaraCluster;
import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.MessageReceiver;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
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
import org.jboss.logging.Logger;
import org.jvnet.hk2.annotations.Service;

/**
 * Internal Payara Micro Service
 *
 * @author steve
 */
@Service(name = "payara-micro-instance")
@RunLevel(StartupRunLevel.VAL)
public class PayaraMicroInstance implements EventListener, MessageReceiver {

    public static final String INSTANCE_STORE_NAME = "payara.instance.store";

    public static final String INTERNAL_EVENTS_NAME = "payara.micro.cluster.event";

    public static final String CDI_EVENTS_NAME = "payara.micro.cdi.event";

    public static final String APPLICATIONS_STORE_NAME = "payara.micro.applications.store";    
    
    private static final Logger logger = Logger.getLogger(PayaraMicroInstance.class);

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

    private InstanceDescriptor me;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        me.setInstanceName(instanceName);
    }

    public <T extends Serializable> Map<String, Future<T>> runCallable(Collection<String> memberUUIDS, Callable<T> callable) {
        return cluster.getExecService().runCallable(memberUUIDS, callable);
    }

    public <T extends Serializable> Map<String, Future<T>> runCallable(Callable<T> callable) {
        return cluster.getExecService().runCallable(callable);
    }

    public ClusterCommandResult executeLocalAsAdmin(String command, String... parameters) {
        return new ClusterCommandResult(commandRunner.run(command, parameters));
    }

    public Map<String, Future<ClusterCommandResult>> executeClusteredASAdmin(String command, String... parameters) {
        AsAdminCallable callable = new AsAdminCallable(command, parameters);
        Map<String, Future<ClusterCommandResult>> result = cluster.getExecService().runCallable(callable);
        return result;
    }

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

        } else if (msg.getPayload() instanceof PayaraClusteredCDIEvent) {
            PayaraClusteredCDIEvent cast = PayaraClusteredCDIEvent.class
                    .cast(msg.getPayload());
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
        // determine https Port
        NetworkListener httpListener = context.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("http-listener");
        int port = Integer.parseInt(httpListener.getPort());
        boolean httpPortEnabled = Boolean.parseBoolean(httpListener.getEnabled());
        NetworkListener httpsListener = context.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("https-listener");
        int sslPort = Integer.parseInt(httpsListener.getPort());
        boolean httpsPortEnabled = Boolean.parseBoolean(httpsListener.getEnabled());

        if (cluster.isEnabled()) {
            cluster.getEventBus().addMessageReceiver(INTERNAL_EVENTS_NAME, this);
            cluster.getEventBus().addMessageReceiver(CDI_EVENTS_NAME, this);
            try {
                // generate unique key for this instance in the Map
                myCurrentID = cluster.getLocalUUID();
                me = new InstanceDescriptor(myCurrentID);
                if (httpPortEnabled) {
                    me.setHttpPort(port);
                }

                if (httpsPortEnabled) {
                    me.setHttpsPort(sslPort);
                }

                me.setInstanceName(instanceName);
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, me);

            } catch (UnknownHostException ex) {
                java.util.logging.Logger.getLogger(PayaraMicroInstance.class.getName()).log(Level.SEVERE, "Could not find local hostname", ex);
            }
        } else {
            try {
                me = new InstanceDescriptor(instanceName);
                if (httpPortEnabled) {
                    me.setHttpPort(port);
                }

                if (httpsPortEnabled) {
                    me.setHttpsPort(sslPort);
                }

                me.setInstanceName(instanceName);
            } catch (UnknownHostException ex) {
                java.util.logging.Logger.getLogger(PayaraMicroInstance.class.getName()).log(Level.SEVERE, null, ex);
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
            PayaraInternalEvent pie = new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.ADDED, me);
            ClusterMessage<PayaraInternalEvent> message = new ClusterMessage<>(pie);
            this.cluster.getEventBus().publish(INTERNAL_EVENTS_NAME, message);

        } 
        // Adds the application to the clustered register of deployed applications
        else if (event.is(Deployment.APPLICATION_LOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                me.addApplication(applicationInfo);
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, me);
            }
        }
        // ensures the same application id is used when there is a deployment 
        // if the application id is in the cluster keyed by application name
        else if (event.is(Deployment.APPLICATION_PREPARED)) {
            if (event.hook() != null && event.hook() instanceof DeploymentContext) {
                DeploymentContext deploymentContext = (DeploymentContext) event.hook();
                Application app = deploymentContext.getModuleMetaData(Application.class);
                Long appID = (Long) cluster.getClusteredStore().get(APPLICATIONS_STORE_NAME, app.getName());
                if (appID != null) {
                    app.setUniqueId(appID);
                } else {
                    cluster.getClusteredStore().set(APPLICATIONS_STORE_NAME, app.getName(), new Long(app.getUniqueId()));
                }
            }

        } 
        // removes the application from the clustered registry of applications
        else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                me.removeApplication(applicationInfo);
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, me);
            }
        } else if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
            PayaraInternalEvent pie = new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.REMOVED, me);
            ClusterMessage<PayaraInternalEvent> message = new ClusterMessage<>(pie);
            this.cluster.getClusteredStore().remove(INSTANCE_STORE_NAME, myCurrentID);
            this.cluster.getEventBus().publish(INTERNAL_EVENTS_NAME, message);
        }
    }

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

    public void pubishCDIEvent(PayaraClusteredCDIEvent event) {
        if (event.getInstanceDescriptor() == null) {
            event.setInstanceDescriptor(me);
        }
        ClusterMessage<PayaraClusteredCDIEvent> message = new ClusterMessage<>(event);
        cluster.getEventBus().publish(CDI_EVENTS_NAME, message);
    }

    public void removeBootstrapListenr(PayaraClusterListener listener) {
        myListeners.remove(listener);
    }

    public void addBootstrapListener(PayaraClusterListener listener) {
        myListeners.add(listener);
    }

    public void removeCDIListenr(CDIEventListener listener) {
        myCDIListeners.remove(listener);
    }

    public void addCDIListener(CDIEventListener listener) {
        myCDIListeners.add(listener);
    }

    public InstanceDescriptor getLocalDescriptor() {
        return me;
    }

    public InstanceDescriptor getDescriptor(String member) {
        InstanceDescriptor result = null;
        if (cluster.isEnabled()) {
            result = (InstanceDescriptor) cluster.getClusteredStore().get(INSTANCE_STORE_NAME, member);
        }
        return result;
    }

}
