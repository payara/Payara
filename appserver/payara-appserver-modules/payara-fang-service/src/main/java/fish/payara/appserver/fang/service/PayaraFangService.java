/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.fang.service;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.fang.service.adapter.PayaraFangAdapter;
import fish.payara.appserver.fang.service.adapter.PayaraFangAdapterState;
import fish.payara.appserver.fang.service.adapter.PayaraFangEndpointDecider;
import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "payara-fang")
@RunLevel(PostStartupRunLevel.VAL)
public class PayaraFangService implements ConfigListener {
    
    public static final String DEFAULT_FANG_APP_NAME = "__fang";
    private boolean startAttempted = false;
    private String contextRoot;
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private PayaraFangConfiguration payaraFangConfiguration;
    
    @Inject
    private PayaraFangAdapter payaraFangAdapter;
    
    @Inject
    Domain domain;
    
    @Inject
    ServerEnvironmentImpl serverEnv;
    
    @Inject
    ServiceLocator habitat;
    
    @PostConstruct
    private void postConstruct() {
        contextRoot = payaraFangAdapter.getContextRoot();
        payaraFangConfiguration = habitat.getService(PayaraFangConfiguration.class);
        
        if (payaraFangConfiguration.getEnabled().equals("true")) {
            loadApplication();
        }
    }
    
    private void loadApplication() {
        startAttempted = true;
        try {
            new PayaraFangLoader(payaraFangAdapter, habitat, domain, serverEnv, 
                    contextRoot, payaraFangConfiguration.getApplicationName(), 
                    payaraFangAdapter.getVirtualServers()).start();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load Payara Fang!", ex);
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        List<UnprocessedChangeEvent> unprocessedChanges = new ArrayList<>();
        boolean attemptStart = false;
        
        for (PropertyChangeEvent propertyChangeEvent : propertyChangeEvents) {
            // Check if the property change event is for us.
            if (propertyChangeEvent.getSource().toString().equals("GlassFishConfigBean." 
                    + PayaraFangConfiguration.class.getName()) && isCurrentInstanceMatchTarget(propertyChangeEvent)) {
                // Check if the property has actually changed
                if (!propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                    // If the application hasn't attempted to start yet
                    if (propertyChangeEvent.getPropertyName().equals("enabled") && !startAttempted) {
                        // Set a flag to attempt a load of the application
                        attemptStart = true;
                    } else if (propertyChangeEvent.getPropertyName().equals("context-root") && !startAttempted) {
                        // If we haven't attempted to start yet, grab the new context root
                        Config serverConfig = domain.getServerNamed(serverEnv.getInstanceName()).getConfig();
                        PayaraFangEndpointDecider endpointDecider = new PayaraFangEndpointDecider(serverConfig, 
                                payaraFangConfiguration);
                        contextRoot = endpointDecider.getContextRoot();
                    } else {
                        unprocessedChanges.add(new UnprocessedChangeEvent(propertyChangeEvent, 
                                "Payara Fang redeploy required"));
                    }
                }
            }
        }
        
        if (attemptStart) {
            loadApplication();
        }
        
        if (unprocessedChanges.isEmpty()) {
            return null;
        } else {
            return new UnprocessedChangeEvents(unprocessedChanges);
        }
    }
    
    private boolean isCurrentInstanceMatchTarget(PropertyChangeEvent propertyChangeEvent) {
        
        // If we are an instance then the change will apply to us as it has been
        // replicated directly to us by the DAS
        if (serverEnv.isInstance()) {
            return true;
        }
        
        ConfigBeanProxy proxy = (ConfigBeanProxy) propertyChangeEvent.getSource();
        
        // Find the config parent
        while (proxy != null && !(proxy instanceof Config)) {
            proxy = proxy.getParent();
        }
        
        if (proxy != null) {
            // We have found a config node at the root
            // If the root config is the das config return true
            return ((Config)proxy).isDas();
        }
        
        return false;
    }
}
