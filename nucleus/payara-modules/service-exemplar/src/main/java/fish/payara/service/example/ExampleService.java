/*
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
  */
package fish.payara.service.example;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.cluster.PayaraCluster;
import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.EventBus;
import fish.payara.nucleus.eventbus.MessageReceiver;
import fish.payara.nucleus.store.ClusteredStore;
import fish.payara.service.example.config.ExampleServiceConfiguration;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * An example of a service which can be used as an aid to developing future services
 * The service implements Event Listener to hook into the Sever wide events system
 * The service implements MessageReceiver to hook into the cluster wide messaging system
 * The service implements ConfigListener to receive notifications if it's configuration changes
 * 
 * Note you do not need to inject all the services below they are just examples of what is available
 * 
 * Note if you need to respond to configuration changes and be dynamic then implement the ConfigListener interface
 * if you are unable to respond to config changes don't implement the interface.
 * @author steve
 */
@Service(name = "example-service") // this specifies that the classis an HK2 service
@RunLevel(StartupRunLevel.VAL)  // this specifies the servce is created at server boot time
public class ExampleService implements EventListener, MessageReceiver, ConfigListener {
    
    private static final Logger LOGGER= Logger.getLogger(ExampleService.class.getCanonicalName());
    
    // Below is a list of useful services you can inject
    
    // Provides ability to register a configuration listener
    @Inject
    Transactions transactions;
    
    // Provides access to information on the server including;
    // command line, initial context, service locator, installation
    // Classloaders, config root for the server
    @Inject
    ServerContext context;
    
    // Provide access to information on the server instance
    @Inject
    ServerEnvironment serverEnv;
    
    //Provides methods to find other HK2 services
    @Inject
    ServiceLocator habitat;
    
    //Provides access to the event manager to hook into server lifecycle events
    // or to raise various events
    @Inject
    Events events;
    
    // Gives access to deployed applications
    @Inject
    ApplicationRegistry applicationRegistry;
    
    // The following are Payara Specific useful services built on Hazelcast and
    // only work if Hazelcast is enabled
    // Gives access to Hazelcast Clustered Services like the Event Bus and Clustered Store
    @Inject
    PayaraCluster cluster;
            
    // This service is access to the Hazelcast Base clustered Event Bus which can send
    // messages to other servers in the same Hazelcast cluster
    @Inject
    EventBus eventBus;
    
    // This gives access to the Hazelcast Based cluster wide in-memory data store
    // this is a bunch of named key-value stores accessible across the Hazelcast Cluster
    @Inject
    ClusteredStore clusterStore;
    
    // This injects the configuration from the domain.xml magically
    // and for the correct server configuation
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    ExampleServiceConfiguration config;
    
    /**
     * This method is called after the service instance has been created
     */
    @PostConstruct
    public void bootService() {
        LOGGER.info("Example Service has booted " + config.getMessage());
        
        // this code demonstrates how you can hook into the server event system to
        // be notified of events like when the server is running
        events.register(this);
        
        // this code hooks the service into the cluster wide messaging system
        // based on Hazelcast
        eventBus.addMessageReceiver("ExampleService", this);
        eventBus.publish("ExampleService", new ClusterMessage("Hello World"));
        
        // notify that I am interested in changes to the configuration class
        transactions.addListenerForType(ExampleServiceConfiguration.class, this);
    }

    /**
     * This is the required method for the EventListener interface for server wide messages
     * handling these events means your service can take action depending on things
     * happening within the server
     * @param event
     */
    @Override
    public void event(Event event) {
        // useful message types to register for are shown below
        if (event.is(EventTypes.SERVER_READY)) {
            LOGGER.info("Server has now finished booting and is ready to process");
        } else if (event.is(EventTypes.SERVER_STARTUP)) {
            LOGGER.info("Server is preparing to start");
        } else if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
            LOGGER.info("Server is preparing to shutdown");
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            LOGGER.info("Server has shutdown");
        } else if (event.is(Deployment.APPLICATION_STARTED)) // there are many deployment event types
        {
            LOGGER.info("Application has been loaded");
        }
    }

    /**
     * This is the required method for the Payara Specific cluster wide event bus
     * @param message
     */
    @Override
    public void receiveMessage(ClusterMessage message) {
        LOGGER.info("Received Message " + message.getPayload());
    }
    
    /**
     * This is the required method for the ConfigListener it is called
     * if our configuration is changed.
     * 
     * Note: Look at the implementation you must check whether you are the DAS
     * and if so check that it is the DAS config that has been changed
     * @see isCurrentInstanceMatchTarget
     * @param pces
     * @return 
     */
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] pces) {
        String newMessage = null;
        for (PropertyChangeEvent pce : pces) {
            if (pce.getPropertyName().equals("message")) {
                // ok it is a property we are interested in
                // we now need to check whether it is for our config
                // The DAS will receive all events even for other configs
                if (isCurrentInstanceMatchTarget(pce)) {
                    LOGGER.info("message has been change");
                    newMessage = (String) pce.getNewValue();               
                    reconfigure();
                }
            }
        }
        return null;
    }

    public void reconfigure() {
        LOGGER.info("Got a notification that I need to update myself from my config " );
        LOGGER.info("Config now says " + config.getMessage());
    }
    
    /**
     * Example method to be used by the instance targeted asadmin command
     * to interact with the service directly
     * @return 
     */
    public void doSomethingDirectly(String message) {
        LOGGER.info("Did something directly from asadmin " + message);
    }
    
    private boolean isCurrentInstanceMatchTarget(PropertyChangeEvent pe) {
        
        // if we are an instance then the change will apply to us as it has been
        // replicated directly to us by the DAS
        if (serverEnv.isInstance()) {
            return true;
        }
        // ok now we are on the DAS
        ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
        
        // find the config parent
        while (proxy != null && !(proxy instanceof Config)) {
            proxy = proxy.getParent();
        }
        
        if (proxy != null) {
            // we have found a config node at the root
            // if the root config is the das config return true
            return ((Config)proxy).isDas();
        }
        return false;
    }
    
    
}
