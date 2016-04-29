/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.backup.service;

import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Daniel
 */
@Service(name="backup-config-service")
@RunLevel(StartupRunLevel.VAL)
public class BackupConfigService implements EventListener,PostConstruct {
    
    @Inject
    private Events events;

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_STARTUP)){
            System.out.println("PREPARE YOURSELF");
        }
        
    }

    @Override
    public void postConstruct() {
        events.register(this);
        System.out.println("HELLOES");
    }
}
