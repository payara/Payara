/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.config.cdi;

import fish.payara.microprofile.config.spi.InjectedPayaraConfig;
import fish.payara.microprofile.config.spi.PayaraConfig;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Steve Millidge <Payara Services Limited>
 */
@ApplicationScoped
public class ConfigProducer {
    
    private InvocationManager im;
    
    @PostConstruct
    public void postConstruct() {
        im = Globals.getDefaultHabitat().getService(InvocationManager.class);
    }
    
    @Produces
    @Dependent
    public Config getConfig() {
        return new InjectedPayaraConfig(ConfigProvider.getConfig(),im.getCurrentInvocation().getAppName());
    }
    
}
