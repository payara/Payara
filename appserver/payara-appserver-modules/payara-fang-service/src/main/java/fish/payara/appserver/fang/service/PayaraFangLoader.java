package fish.payara.appserver.fang.service;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemApplications;
import com.sun.enterprise.v3.server.ApplicationLoaderService;
import fish.payara.appserver.fang.service.adapter.PayaraFangAdapter;
import fish.payara.appserver.fang.service.adapter.PayaraFangAdapterState;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
public class PayaraFangLoader extends Thread {
    private final Domain domain;
    private final ServerEnvironmentImpl serverEnv;
    private final String contextRoot;
    private String applicationName;
    private final PayaraFangAdapter payaraFangAdapter;
    private final ServiceLocator habitat;
    private static final Logger LOGGER = Logger.getLogger(PayaraFangLoader.class.getName());
    private final List<String> vss;
    
    PayaraFangLoader(PayaraFangAdapter payaraFangAdapter, ServiceLocator habitat, Domain domain, 
            ServerEnvironmentImpl serverEnv, String contextRoot, String applicationName, List<String> vss) {
        this.payaraFangAdapter = payaraFangAdapter;
        this.habitat = habitat;
        this.domain = domain;
        this.serverEnv = serverEnv;
        this.contextRoot = contextRoot;
        this.applicationName = applicationName;
        this.vss = vss;
    }

    @Override
    public void run() {
        try {
            // Check if the application already exists
            if (payaraFangAdapter.appExistsInConfig()) {
                // Check if the app is actually registered to this instance
                if (!payaraFangAdapter.isAppRegistered()) {
                    // We hit here if the app exists, but hasn't been registered to this instance yet
                    registerApplication();
                }
            } else {
                // If the app simply doesn't exist, create one and register it for this instance
                createAndRegisterApplication();
            }
            
            loadApplication();
        } catch (Exception ex) {
            payaraFangAdapter.setStateMsg(PayaraFangAdapterState.NOT_REGISTERED);
            LOGGER.log(Level.WARNING, "Problem while attempting to register Payara Fang!", ex);
        }
    }
   
    /**
     * Create the system application entry and register the application
     * @throws Exception 
     */
    private void createAndRegisterApplication() throws Exception {
        // Update the adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.REGISTERING);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Registering the Payara Fang Application...");
        }

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
                            + PayaraFangService.DEFAULT_FANG_APP_NAME);
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
                weldEngine.setSniffer("weld");
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

        // Update the adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.NOT_LOADED);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Payara Fang Registered.");
        }
    }
    
    private void registerApplication() throws Exception {
        // Update the adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.REGISTERING);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Registering the Payara Fang Application...");
        }

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
                    throw new IllegalStateException("Payara Fang has no system app entry!");
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
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.NOT_LOADED);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Payara Fang Registered.");
        }
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
            payaraFangAdapter.setStateMsg(PayaraFangAdapterState.LOADED);
            return;
        }

        Application config = payaraFangAdapter.getSystemApplicationConfig();
        
        if (config == null) {
            throw new IllegalStateException("Payara Fang has no system app entry!");
        }
        
        // Update adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.LOADING);

        // Load the Payara Fang Application
        String instanceName = serverEnv.getInstanceName();
        ApplicationRef ref = domain.getApplicationRefInServer(instanceName, applicationName);
        habitat.getService(ApplicationLoaderService.class).processApplication(config, ref);

        // Update adapter state and mark as registered
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.LOADED);
        payaraFangAdapter.setAppRegistered(true);
    }
    
    private void checkAndResolveApplicationName(SystemApplications systemApplications) {
        // Check if the application name is not empty
        if (applicationName == null || applicationName.equals("")) {
            LOGGER.log(Level.INFO, "No or incorrect application name detected for Payara Fang: reverting to default");
            applicationName = PayaraFangService.DEFAULT_FANG_APP_NAME;
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
}
