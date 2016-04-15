


import javax.inject.Inject;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Daniel
 */
@Service(name = "CreateBackupConfigService")
public class CreateBackupConfigService implements EventListener, PostConstruct{
    @Inject
    Events events;
    
    @Override
    public void postConstruct(){
        events.register(this);
    }
    
    @Override
    public void event(Event event){
        if (event.is(EventTypes.SERVER_SHUTDOWN)){
            System.out.println("Service Shutting down");
        } else if (event.is(EventTypes.SERVER_STARTUP)){
            System.out.println("Service is starting");
        } else if (event.is(EventTypes.SERVER_READY)) {
            System.out.println("Service is ready");
        }
    }//end event
    
    public void Backup(){
        System.out.println("Hello World!");
    }
}//end CreateBackupConfigService
