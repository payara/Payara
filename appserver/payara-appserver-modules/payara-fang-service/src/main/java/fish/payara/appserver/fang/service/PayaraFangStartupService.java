/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.fang.service;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.fang.service.adapter.PayaraFangAdapter;
import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
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

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "payara-fang")
@RunLevel(PostStartupRunLevel.VAL)
public class PayaraFangStartupService {
    
    public static final String DEFAULT_FANG_APP_NAME = "__fang";
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private PayaraFangConfiguration configuration;
    
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
        configuration = habitat.getService(PayaraFangConfiguration.class);
        
        if (configuration.getEnabled().equals("true")) {
            loadApplication();
        }
    }
    
    private void loadApplication() {
        try {
            new PayaraFangLoader(payaraFangAdapter, habitat, domain, serverEnv, 
                    payaraFangAdapter.getContextRoot(), configuration.getApplicationName(), 
                    payaraFangAdapter.getVirtualServers()).start();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load Payara Fang!", ex);
        }
    }
}
