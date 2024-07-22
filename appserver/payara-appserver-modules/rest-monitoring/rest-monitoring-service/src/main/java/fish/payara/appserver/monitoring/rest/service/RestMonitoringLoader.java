/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.appserver.monitoring.rest.service;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemApplications;
import com.sun.enterprise.v3.server.ApplicationLoaderService;
import fish.payara.appserver.monitoring.rest.service.adapter.RestMonitoringAdapter;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Loader class for the Rest Monitoring application. Handles registering and actual loading of the application.
 * @author Andrew Pielage
 */
public class RestMonitoringLoader extends Thread {
    private final Domain domain;
    private final ServerEnvironmentImpl serverEnv;
    private final String contextRoot;
    private String applicationName;
    private final RestMonitoringAdapter restMonitoringAdapter;
    private final ServiceLocator habitat;
    private static final Logger LOGGER = Logger.getLogger(RestMonitoringLoader.class.getName());
    private final List<String> vss;
    private final boolean dynamicStart;
    
    RestMonitoringLoader(RestMonitoringAdapter restMonitoringAdapter, ServiceLocator habitat, Domain domain, 
            ServerEnvironmentImpl serverEnv, String contextRoot, String applicationName, List<String> vss) {
        this.restMonitoringAdapter = restMonitoringAdapter;
        this.habitat = habitat;
        this.domain = domain;
        this.serverEnv = serverEnv;
        this.contextRoot = contextRoot;
        this.applicationName = applicationName;
        this.vss = vss;
        this.dynamicStart = false;
    }
    
    RestMonitoringLoader(RestMonitoringAdapter restMonitoringAdapter, ServiceLocator habitat, Domain domain, 
            ServerEnvironmentImpl serverEnv, String contextRoot, String applicationName, List<String> vss, 
            boolean dynamicStart) {
        this.restMonitoringAdapter = restMonitoringAdapter;
        this.habitat = habitat;
        this.domain = domain;
        this.serverEnv = serverEnv;
        this.contextRoot = contextRoot;
        this.applicationName = applicationName;
        this.vss = vss;
        this.dynamicStart = dynamicStart;
    }

    @Override
    public void run() {
        try {
            // Check if we've started the service dynamically, and so whether or not to override the adapter's config
            if (dynamicStart) {
                if (restMonitoringAdapter.appExistsInConfig(contextRoot)) {
                    // Check if we need to reconfigure system app to match the overriding config
                    if (!restMonitoringAdapter.isAppRegistered(contextRoot)) {
                        registerApplication();
                    } else {
                        // Since we're starting dynamically, assume that we need to reconfigure
                        reconfigureSystemApplication();
                    }
                } else {
                    // If the app simply doesn't exist, create one and register it for this instance
                    createAndRegisterApplication();
                }
            } else {
                // Check if the application already exists
                if (restMonitoringAdapter.appExistsInConfig()) {
                    // Check if the app is actually registered to this instance
                    if (!restMonitoringAdapter.isAppRegistered()) {
                        // We hit here if the app exists, but hasn't been registered to this instance yet
                        registerApplication();
                    } else if (!contextRoot.equals(restMonitoringAdapter.getSystemApplicationConfig().getContextRoot())) {
                        // We hit here if there is a system application already created and registered to this instance, 
                        // but we've changed the context root and so need to reconfigure the system app
                        reconfigureSystemApplication();
                    }
                } else {
                    // If the app simply doesn't exist, create one and register it for this instance
                    createAndRegisterApplication();
                }
            }
            
            loadApplication();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Problem while attempting to register or load the Rest Monitoring application!", ex);
        }
    }
   
    /**
     * Create the system application entry and register the application
     * @throws Exception 
     */
    private void createAndRegisterApplication() throws Exception {
        LOGGER.log(Level.FINE, "Registering the Rest Monitoring Application...");

        // Create the system application entry and application-ref in the config
        ConfigCode code = new ConfigCode() {
            @Override
            public Object run(ConfigBeanProxy... proxies) throws PropertyVetoException, TransactionFailure {
                // Create the system application
                SystemApplications systemApplications = (SystemApplications) proxies[0];
                Application application = systemApplications.createChild(Application.class);
                
                // Check if the application name is valid, generating a new one if it isn't
                checkAndResolveApplicationName(systemApplications); 
                
                systemApplications.getModules().add(application);
                application.setName(applicationName);
                application.setEnabled(Boolean.TRUE.toString());
                application.setObjectType("system-admin");
                application.setDirectoryDeployed("true");
                application.setContextRoot(contextRoot);
                
                try {
                    application.setLocation("${com.sun.aas.installRootURI}/lib/install/applications/" 
                            + RestMonitoringService.DEFAULT_REST_MONITORING_APP_NAME);
                } catch (Exception me) {
                    throw new RuntimeException(me);
                }
                
                // Set the engine types
                Module singleModule = application.createChild(Module.class);
                application.getModule().add(singleModule);
                singleModule.setName(applicationName);
                Engine webEngine = singleModule.createChild(Engine.class);
                webEngine.setSniffer("web");
                Engine weldEngine = singleModule.createChild(Engine.class);
                weldEngine.setSniffer("cdi");
                Engine securityEngine = singleModule.createChild(Engine.class);
                securityEngine.setSniffer("security");
                singleModule.getEngines().add(webEngine);
                singleModule.getEngines().add(weldEngine);
                singleModule.getEngines().add(securityEngine);
                
                // Create the application-ref
                Server s = (Server) proxies[1];
                List<ApplicationRef> arefs = s.getApplicationRef();
                ApplicationRef aref = s.createChild(ApplicationRef.class);
                aref.setRef(application.getName());
                aref.setEnabled(Boolean.TRUE.toString());
                aref.setVirtualServers(getVirtualServerListAsString());
                arefs.add(aref);
                
                return true;
            }
        };
        
        Server server = domain.getServerNamed(serverEnv.getInstanceName());
        ConfigSupport.apply(code, domain.getSystemApplications(), server);

        LOGGER.log(Level.FINE, "Rest Monitoring Application Registered.");
    }
    
    private void registerApplication() throws Exception {
        LOGGER.log(Level.FINE, "Registering the Rest Monitoring Application...");

        // Create the application-ref entry in the domain.xml
        ConfigCode code = new ConfigCode() {
            @Override
            public Object run(ConfigBeanProxy... proxies) throws PropertyVetoException, TransactionFailure {
                // Get the system application config
                SystemApplications systemApplications = (SystemApplications) proxies[0];
                Application application = null;
                for (Application systemApplication : systemApplications.getApplications()) {
                    if (systemApplication.getName().equals(applicationName)) {
                        application = systemApplication;
                        break;
                    }
                }
                
                if (application == null) {
                    throw new IllegalStateException("The Rest Monitoring application has no system app entry!");
                }
                
                // Create the application-ref
                Server s = (Server) proxies[1];
                List<ApplicationRef> arefs = s.getApplicationRef();
                ApplicationRef aref = s.createChild(ApplicationRef.class);
                aref.setRef(application.getName());
                aref.setEnabled(Boolean.TRUE.toString());
                aref.setVirtualServers(getVirtualServerListAsString());
                arefs.add(aref);
                return true;
            }
        };
        
        Server server = domain.getServerNamed(serverEnv.getInstanceName());
        ConfigSupport.apply(code, domain.getSystemApplications(), server);

        // Update the adapter state
        LOGGER.log(Level.FINE, "Rest Monitoring Application Registered.");
    }

    private String getVirtualServerListAsString() {
        if (vss == null) {
            return "";
        }
            
        String virtualServers = Arrays.toString(vss.toArray(new String[vss.size()]));
        
        // Standard JDK implemetation always returns this enclosed in [], which we don't want
        virtualServers = virtualServers.substring(1, virtualServers.length() - 1);
        
        return virtualServers;
    }

    /**
     * Loads the application
     */
    private void loadApplication() {
        ApplicationRegistry appRegistry = habitat.getService(ApplicationRegistry.class);
        ApplicationInfo appInfo = appRegistry.get(applicationName);
        if (appInfo != null && appInfo.isLoaded()) {
            LOGGER.log(Level.FINE, "Rest Monitoring Application already loaded.");
            return;
        }
        
        Application config = null;
        if (dynamicStart) {
            config = restMonitoringAdapter.getSystemApplicationConfig(contextRoot);
        } else {
            config = restMonitoringAdapter.getSystemApplicationConfig();
        }
        
        
        if (config == null) {
            throw new IllegalStateException("The Rest Monitoring application has no system app entry!");
        }

        // Load the Rest Monitoring Application
        String instanceName = serverEnv.getInstanceName();
        ApplicationRef ref = domain.getApplicationRefInServer(instanceName, applicationName);
        Deployment lifecycle = habitat.getService(Deployment.class);
        for(Deployment.ApplicationDeployment depl : habitat.getService(ApplicationLoaderService.class).processApplication(config, ref)) {
            lifecycle.initialize(depl.appInfo, depl.appInfo.getSniffers(), depl.context);
        }

        // Mark as registered
        restMonitoringAdapter.setAppRegistered(true);
        
        LOGGER.log(Level.FINE, "Rest Monitoring Application Loaded.");
    }
    
    private void checkAndResolveApplicationName(SystemApplications systemApplications) {
        // Check if the application name is not empty
        if (applicationName == null || applicationName.equals("")) {
            LOGGER.log(Level.INFO, "No or incorrect application name detected for Rest Monitoring: reverting to default");
            applicationName = RestMonitoringService.DEFAULT_REST_MONITORING_APP_NAME;
        }
        
        // Loop through the system applications
        boolean validApplicationNameFound = false;
        int applicationNameSuffix = 1;
        while (!validApplicationNameFound) {
            // Check if the current application name is in use
            validApplicationNameFound = isApplicationNameValid(systemApplications);
            
            if (!validApplicationNameFound) {
                // If the name isn't valid, append a number to it and try again
                applicationName = applicationName + "-" + applicationNameSuffix;
                applicationNameSuffix++;
            }
        }
        
    }
    
    private boolean isApplicationNameValid(SystemApplications systemApplications) {
        boolean validApplicationNameFound = true;
        
        // Search through the system application names to check if there are any apps with the same name
        for (Application systemApplication : systemApplications.getApplications()) {
            if (systemApplication.getName().equals(applicationName)) {
                // We've found an application with the same name, that means we can't use this one
                validApplicationNameFound = false;
                break;
            }
        }
        
        return validApplicationNameFound;
    }
    
    private void reconfigureSystemApplication() throws Exception {
        Application systemApplication = restMonitoringAdapter.getSystemApplicationConfig();
        
        LOGGER.log(Level.FINE, "Reconfiguring the Rest Monitoring Application...");        

        // Reconfigure the system-application entry in the domain.xml
        ConfigCode code = new ConfigCode() {
            @Override
            public Object run(ConfigBeanProxy... proxies) throws PropertyVetoException, TransactionFailure {
                Application systemApplication = (Application) proxies[0];
                systemApplication.setContextRoot(contextRoot);
                
                return true;
            }
        };
        
        ConfigSupport.apply(code, systemApplication);

        LOGGER.log(Level.FINE, "Rest Monitoring Application Reconfigured.");
    }
}
