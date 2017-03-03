/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.zendesk;

import fish.payara.appserver.zendesk.config.ZendeskSupportConfiguration;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 *
 * @author Andrew Pielage
 */
@Service (name = "zendesk-support")
@RunLevel(StartupRunLevel.VAL)
public class ZendeskSupportService implements ConfigListener {

    private static final Logger LOGGER= Logger.getLogger(ZendeskSupportService.class.getCanonicalName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    ZendeskSupportConfiguration zendeskSupportConfiguration;
    
    @Inject
    Events events;
    
    @Inject
    ServiceLocator habitat;
    
    @PostConstruct
    void postConstruct() {
        zendeskSupportConfiguration = habitat.getService(ZendeskSupportConfiguration.class);
    }
    
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] pces) {
        for (PropertyChangeEvent pce : pces) {
            if (pce.getPropertyName().equals("emailAddress")) {
                zendeskSupportConfiguration.setEmailAddress(pce.getNewValue().toString());
            }
        }
        return null;
    }
    
}
