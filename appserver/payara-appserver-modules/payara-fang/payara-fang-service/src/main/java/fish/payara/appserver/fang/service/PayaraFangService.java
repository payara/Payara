/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

package fish.payara.appserver.fang.service;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.fang.service.adapter.PayaraFangAdapter;
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
 * The core service for the Payara Fang application. Handles starting and reconfiguration of the application.
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
    
    private void loadApplication(boolean dynamicStart) {
        startAttempted = true;
        try {
            new PayaraFangLoader(payaraFangAdapter, habitat, domain, serverEnv, 
                    contextRoot, payaraFangConfiguration.getApplicationName(), 
                    payaraFangAdapter.getVirtualServers(), dynamicStart).start();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load Payara Fang!", ex);
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        List<UnprocessedChangeEvent> unprocessedChanges = new ArrayList<>();
        boolean dynamicStart = false;
        
        for (PropertyChangeEvent propertyChangeEvent : propertyChangeEvents) {
            // Check that the property change event is for us.
            if (propertyChangeEvent.getSource().toString().equals("GlassFishConfigBean." 
                    + PayaraFangConfiguration.class.getName()) && isCurrentInstanceMatchTarget(propertyChangeEvent)) {
                // Check if the property has actually changed
                if (!propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue())) {
                    // If the application hasn't attempted to start yet
                    if (!startAttempted) {
                        // We can only get here if enabled was false at server start, so in the case of the enabled 
                        // property, we don't need to compare it to the current value - it can only be true
                        if (propertyChangeEvent.getPropertyName().equals("enabled")) {
                            // Flag that we want to dynamically start Payara Fang
                            dynamicStart = true;
                        } else if (propertyChangeEvent.getPropertyName().equals("context-root")) {
                            // If we haven't attempted to start the app yet, grab the new context root
                            Config serverConfig = domain.getServerNamed(serverEnv.getInstanceName()).getConfig();
                            PayaraFangEndpointDecider endpointDecider = new PayaraFangEndpointDecider(serverConfig, 
                                    payaraFangConfiguration);
                            contextRoot = endpointDecider.getContextRoot();
                        }
                    } else if (!propertyChangeEvent.getPropertyName().equals("security-enabled")) {
                        // If a startup has been attempted and the changed property isn't securityEnabled, throw an 
                        // unprocessed change event as we need to restart
                        unprocessedChanges.add(new UnprocessedChangeEvent(propertyChangeEvent, 
                                "Payara Fang redeploy required"));
                    }
                }
            }
        }
        
        // This should only be true if Payara Fang was not enabled at startup, and we've just enabled the service
        if (dynamicStart) {
            loadApplication(true);
        }
        
        // If we need to restart, throw an unprocessed change event
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