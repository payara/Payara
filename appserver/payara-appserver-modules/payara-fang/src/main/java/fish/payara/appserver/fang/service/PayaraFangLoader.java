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
import org.glassfish.api.admin.config.ApplicationName;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.config.ConfigBean;
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
    private final PayaraFangAdapter payaraFangAdapter;
    private final ServiceLocator habitat;
    private final Logger logger = Logger.getLogger(PayaraFangLoader.class.getName());
    private final List<String> vss;

    PayaraFangLoader(PayaraFangAdapter payaraFangAdapter, ServiceLocator habitat, Domain domain, 
            ServerEnvironmentImpl serverEnv, String contextRoot, List<String> vss) {
        this.payaraFangAdapter = payaraFangAdapter;
        this.habitat = habitat;
        this.domain = domain;
        this.serverEnv = serverEnv;
        this.contextRoot = contextRoot;
        this.vss = vss;
    }

    @Override
    public void run() {
        try {
            if (domain.getSystemApplicationReferencedFrom(serverEnv.getInstanceName(), PayaraFangService.FANG_APP_NAME) 
                    == null) {
//                if (serverEnv.isDas()) {
                    registerApplication();
//                } else {
//                    logger.log(Level.WARNING, "Payara Fang not yet registered in the DAS");
//                    return;
//                }
            }
            
            load();

            // From within this Thread mark the installation process complete
            payaraFangAdapter.setInstalling(false);
        } catch (Exception ex) {
            payaraFangAdapter.setInstalling(false);
            payaraFangAdapter.setStateMsg(PayaraFangAdapterState.NOT_REGISTERED);
            logger.log(Level.INFO, "Problem while attempting to register Payara Fang!", ex);
        }
    }

   
    /**
     * Register the application
     * @throws Exception 
     */
    private void registerApplication() throws Exception {
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.REGISTERING);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Registering the Payara Fang Application...");
        }

        //create the application entry in domain.xml
        ConfigCode code = new ConfigCode() {
            @Override
            public Object run(ConfigBeanProxy... proxies) throws PropertyVetoException, TransactionFailure {
                SystemApplications systemApplications = (SystemApplications) proxies[0];
                Application application = systemApplications.createChild(Application.class);
                systemApplications.getModules().add(application);
                application.setName(PayaraFangService.FANG_APP_NAME);
                application.setEnabled(Boolean.TRUE.toString());
                application.setObjectType("system-admin"); //TODO
                application.setDirectoryDeployed("true");
                application.setContextRoot(contextRoot);
                
                try {
                    application.setLocation("${com.sun.aas.installRootURI}/lib/install/applications/" 
                            + PayaraFangService.FANG_APP_NAME);
                } catch (Exception me) {
                    // can't do anything
                    throw new RuntimeException(me);
                }
                
                Module singleModule = application.createChild(Module.class);
                application.getModule().add(singleModule);
                singleModule.setName(application.getName());
                Engine webEngine = singleModule.createChild(Engine.class);
                webEngine.setSniffer("web");
                Engine weldEngine = singleModule.createChild(Engine.class);
                weldEngine.setSniffer("weld");
                singleModule.getEngines().add(webEngine);
                singleModule.getEngines().add(weldEngine);
                Server s = (Server) proxies[1];
                List<ApplicationRef> arefs = s.getApplicationRef();
                ApplicationRef aref = s.createChild(ApplicationRef.class);
                aref.setRef(application.getName());
                aref.setEnabled(Boolean.TRUE.toString());
                aref.setVirtualServers(getVirtualServerList());
                arefs.add(aref);
                return true;
            }
        };
        
        Server server = domain.getServerNamed(serverEnv.getInstanceName());
        ConfigSupport.apply(code, domain.getSystemApplications(), server);

        // Set the adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.NOT_LOADED);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Payara Fang Registered.");
        }
    }

    private String getVirtualServerList() {
        if (vss == null)
            return "";
        String s = Arrays.toString(vss.toArray(new String[vss.size()]));
        //standard JDK implemetation always returns this enclosed in [], remove them
        s = s.substring(1, s.length() - 1);
        return (s);
    }

    private void load() {
        ApplicationRegistry appRegistry = habitat.<ApplicationRegistry>getService(ApplicationRegistry.class);
        ApplicationInfo appInfo = appRegistry.get(PayaraFangService.FANG_APP_NAME);
        if (appInfo != null && appInfo.isLoaded()) {
            payaraFangAdapter.setStateMsg(PayaraFangAdapterState.LOADED);
            return;
        }

        Application config = payaraFangAdapter.getConfig();
        
        if (config == null) {
            throw new IllegalStateException("Payara Fang has no system app entry!");
        }
        
        // Set adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.LOADING);

        // Load the Payara Fang Application
        String sn = serverEnv.getInstanceName();
        ApplicationRef ref = domain.getApplicationRefInServer(sn, PayaraFangService.FANG_APP_NAME);
        habitat.<ApplicationLoaderService>getService(ApplicationLoaderService.class).processApplication(config, ref);

        // Set adapter state
        payaraFangAdapter.setStateMsg(PayaraFangAdapterState.LOADED);
    }
}
