/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.appserver.micro.services;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.v3.services.impl.GrizzlyService;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;

import fish.payara.appserver.micro.services.command.AsAdminCallable;
import fish.payara.appserver.micro.services.command.ClusterCommandResultImpl;
import fish.payara.appserver.micro.services.data.ApplicationDescriptorImpl;
import fish.payara.appserver.micro.services.data.InstanceDescriptorImpl;
import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.PayaraInstance;
import fish.payara.micro.data.ApplicationDescriptor;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.micro.event.CDIEventListener;
import fish.payara.micro.event.PayaraClusterListener;
import fish.payara.micro.event.PayaraClusteredCDIEvent;
import fish.payara.nucleus.cluster.PayaraCluster;
import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.MessageReceiver;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.utils.Holder.LazyHolder;
import static org.glassfish.grizzly.utils.Holder.lazyHolder;

/**
 * Internal Payara Service for describing instances
 *
 * @author steve
 */
@Service(name = "payara-instance")
@RunLevel(StartupRunLevel.VAL)
@Contract
public class PayaraInstanceImpl implements EventListener, MessageReceiver, PayaraInstance {

    public static final String INSTANCE_STORE_NAME = "payara.instance.store";

    public static final String INTERNAL_EVENTS_NAME = "payara.micro.cluster.event";

    public static final String CDI_EVENTS_NAME = "payara.micro.cdi.event";

    public static final String APPLICATIONS_STORE_NAME = "payara.micro.applications.store";

    private static final String APP_UNIQUE_ID_PROP = "org.glassfish.ejb.container.application_unique_id";

    private static final Logger logger = Logger.getLogger(PayaraInstanceImpl.class.getName());

    @Inject
    private ServiceLocator habitat;

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

    private UUID myCurrentID;

    private LazyHolder<InstanceDescriptorImpl> descriptor = lazyHolder(this::initialiseInstanceDescriptor);

    @Inject
    private ServerEnvironment environment;

    @Inject
    private HazelcastCore hazelcast;

    @Inject
    private ApplicationRegistry appRegistry;

    @Inject
    private PayaraExecutorService executor;


    @Override
    public String getInstanceName() {
        return descriptor.get().getInstanceName();
    }

    @Override
    public void setInstanceName(String instanceName) {
        descriptor.get().setInstanceName(instanceName);
    }

    @Override
    public <T extends Serializable> Map<UUID, Future<T>> runCallable(Collection<UUID> memberUUIDS, Callable<T> callable) {
        return cluster.getExecService().runCallable(memberUUIDS, callable);
    }

    @Override
    public <T extends Serializable> Map<UUID, Future<T>> runCallable(Callable<T> callable) {
        return cluster.getExecService().runCallableAllMembers(callable);
    }

    @Override
    public ClusterCommandResult executeLocalAsAdmin(String command, String... parameters) {
        return new ClusterCommandResultImpl(commandRunner.run(command, parameters));
    }

    @Override
    public Map<UUID, Future<ClusterCommandResult>> executeClusteredASAdmin(String command, String... parameters) {
        AsAdminCallable callable = new AsAdminCallable(command, parameters);
        Map<UUID, Future<ClusterCommandResult>> result = cluster.getExecService().runCallableAllMembers(callable);
        return result;
    }

    @Override
    public Map<UUID, Future<ClusterCommandResult>> executeClusteredASAdmin(Collection<UUID> memberGUIDs, String command, String... parameters) {
        AsAdminCallable callable = new AsAdminCallable(command, parameters);
        Map<UUID, Future<ClusterCommandResult>> result = cluster.getExecService().runCallable(memberGUIDs, callable);
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
    }

    /**
     *
     * @param event
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            PayaraInternalEvent pie = new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.ADDED, descriptor.get());
            ClusterMessage<PayaraInternalEvent> message = new ClusterMessage<>(pie);
            this.cluster.getEventBus().publish(INTERNAL_EVENTS_NAME, message);

            for (String appName : appRegistry.getAllApplicationNames()) {
                descriptor.get().addApplication(new ApplicationDescriptorImpl(appRegistry.get(appName)));
            }

            cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, descriptor.get());
            executor.scheduleAtFixedRate(() -> {
                descriptor.get().setLastHeartBeat(System.currentTimeMillis());
                if (myCurrentID != null) {
                    cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, descriptor.get());
                }
            }, 0, 5, TimeUnit.SECONDS);
        } // Adds the application to the clustered register of deployed applications
        else if (event.is(Deployment.APPLICATION_STARTED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                descriptor.get().addApplication(new ApplicationDescriptorImpl(applicationInfo));
                logger.log(Level.FINE, "App Loaded: {2}, Enabled: {0}, my ID: {1}", new Object[] { hazelcast.isEnabled(),
                    myCurrentID, applicationInfo.getName() });
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, descriptor.get());
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
                        deploymentContext.getAppProps().setProperty(APP_UNIQUE_ID_PROP, String.valueOf(appID));
                    } else {
                        cluster.getClusteredStore().set(APPLICATIONS_STORE_NAME, app.getName(), app.getUniqueId());
                        deploymentContext.getAppProps().setProperty(APP_UNIQUE_ID_PROP, String.valueOf(app.getUniqueId()));
                    }
                }
            }
        }
        // removes the application from the clustered registry of applications
        else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                descriptor.get().removeApplication(new ApplicationDescriptorImpl(applicationInfo));
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, descriptor.get());
            }
        } else if (event.is(HazelcastEvents.HAZELCAST_SHUTDOWN_STARTED)) {
            // Set to null to help prevent the scheduled heartbeat task re-adding the descriptor we're removing here.
            myCurrentID = null;
            PayaraInternalEvent pie = new PayaraInternalEvent(PayaraInternalEvent.MESSAGE.REMOVED, descriptor.get());
            ClusterMessage<PayaraInternalEvent> message = new ClusterMessage<>(pie);
            this.cluster.getClusteredStore().remove(INSTANCE_STORE_NAME, myCurrentID);
            this.cluster.getEventBus().publish(INTERNAL_EVENTS_NAME, message);
            // Create a new lazy initialiser
            descriptor = lazyHolder(this::initialiseInstanceDescriptor);
        }

        // When Hazelcast is bootstrapped, update the instance descriptor with any new information
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
            descriptor.get();
            logger.log(Level.FINE, "Hz Bootstrap Complete, Enabled: {0}, my ID: {1}", new Object[] { hazelcast.isEnabled(), myCurrentID });
            // remove listener first
            cluster.getEventBus().removeMessageReceiver(INTERNAL_EVENTS_NAME, this);
            cluster.getEventBus().removeMessageReceiver(CDI_EVENTS_NAME, this);
            cluster.getEventBus().addMessageReceiver(INTERNAL_EVENTS_NAME, this);
            cluster.getEventBus().addMessageReceiver(CDI_EVENTS_NAME, this);
        }

        // If the generated name had to be changed, update the instance descriptor with the new information
        if (event.is(HazelcastEvents.HAZELCAST_GENERATED_NAME_CHANGE)) {
            descriptor.get();
        }
    }

    @Override
    public Set<InstanceDescriptor> getClusteredPayaras() {
        Set<UUID> members = cluster.getClusterMembers();
        HashSet<InstanceDescriptor> result = new HashSet<>(members.size());
        for (UUID member : members) {
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
            event.setId(descriptor.get());
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
        return descriptor.get();
    }

    @Override
    public InstanceDescriptorImpl getDescriptor(UUID member) {
        InstanceDescriptorImpl result = null;
        if (cluster.isEnabled()) {
            result = (InstanceDescriptorImpl) cluster.getClusteredStore().get(INSTANCE_STORE_NAME, member);
        }
        return result;
    }

    private InstanceDescriptorImpl initialiseInstanceDescriptor() {
        String instanceName = "payara-micro";
        String instanceGroup = "no-cluster";
        boolean liteMember = false;
        int hazelcastPort = 5900;
        InetAddress hostname = null;

        // Get the Hazelcast specific information
        if (hazelcast.isEnabled()) {
            instanceName = hazelcast.getMemberName();
            instanceGroup = hazelcast.getMemberGroup();
            myCurrentID = hazelcast.getUUID();
            liteMember = hazelcast.isLite();
            hazelcastPort = hazelcast.getPort();
            hostname = hazelcast.getInstance().getCluster().getLocalMember().getSocketAddress().getAddress();
        }

        // Get this instance's runtime type
        String instanceType = environment.getRuntimeType().toString();

        // Get the ports in use by this instance from its network listener configs
        List<Integer> ports = new ArrayList<>();
        List<Integer> sslPorts = new ArrayList<>();
        int adminPort = 0;
        for (NetworkListener networkListener :
                context.getConfigBean().getConfig().getNetworkConfig().getNetworkListeners().getNetworkListener()) {

            // Try and get the port. First get the port in the domain xml, then attempt to get the dynamic config port.
            int port = Integer.parseInt(networkListener.getPort());
            try {
                port = habitat.getService(GrizzlyService.class).getRealPort(networkListener);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to get running Grizzly listener.", ex);
            }

            // Skip the network listener if it isn't enabled
            if (Boolean.parseBoolean(networkListener.getEnabled())) {
                // Check if this listener is using HTTP or HTTPS
                if (networkListener.findProtocol().getSecurityEnabled().equals("false")) {
                    // Check if this listener is the admin listener
                    if (networkListener.getName().equals(
                            context.getConfigBean().getConfig().getAdminListener().getName())) {
                        // Micro instances can use the admin listener as both an admin and HTTP port
                        if (instanceType.equals("MICRO")) {
                            ports.add(port);
                        }
                        adminPort = port;
                    } else {
                        // If this listener isn't the admin listener, it must be an HTTP listener
                        ports.add(port);
                    }
                } else if (networkListener.findProtocol().getSecurityEnabled().equals("true")) {
                    if (networkListener.getName().equals(
                            context.getConfigBean().getConfig().getAdminListener().getName())) {
                        // Micro instances can use the admin listener as both an admin and HTTPS port
                        if (instanceType.equals("MICRO")) {
                            ports.add(port);
                        }
                        adminPort = port;
                    } else {
                        // If this listener isn't the admin listener, it must be an HTTPS listener
                        sslPorts.add(port);
                    }
                }
            }
        }

        // Initialise the instance descriptor and set all of its attributes
        try {
            InstanceDescriptorImpl instanceDescriptor = new InstanceDescriptorImpl(myCurrentID);
            // If Hazelcast is being rebooted dynamically, we don't want to lose the already registered applications
            Collection<ApplicationDescriptor> deployedApplications = instanceDescriptor.getDeployedApplications();
            instanceDescriptor.setInstanceName(instanceName);
            instanceDescriptor.setInstanceGroup(instanceGroup);
            for (int port : ports) {
                instanceDescriptor.addHttpPort(port);
            }

            for (int sslPort : sslPorts) {
                instanceDescriptor.addHttpsPort(sslPort);
            }

            instanceDescriptor.setAdminPort(adminPort);
            instanceDescriptor.setHazelcastPort(hazelcastPort);
            instanceDescriptor.setLiteMember(liteMember);
            instanceDescriptor.setInstanceType(instanceType);

            if (hostname != null) {
                instanceDescriptor.setHostName(hostname);
            }

            // If there were some deployed applications from the previous instance descriptor, register them with the new
            // one
            if (!deployedApplications.isEmpty()) {
                for (ApplicationDescriptor application : deployedApplications) {
                    instanceDescriptor.addApplication(application);
                }
            }

            // Register the instance descriptor to the cluster if it's enabled
            if (cluster.isEnabled()) {
                cluster.getClusteredStore().set(INSTANCE_STORE_NAME, myCurrentID, instanceDescriptor);
            }
            return instanceDescriptor;
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
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
