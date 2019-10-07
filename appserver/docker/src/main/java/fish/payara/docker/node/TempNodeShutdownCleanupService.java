package fish.payara.docker.node;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Service(name = "temp-node-cleanup-service")
@RunLevel(StartupRunLevel.VAL)
public class TempNodeShutdownCleanupService implements EventListener {

    @Inject
    private Events events;

    @PostConstruct
    void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
            ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
            if (serviceLocator != null) {
                CommandRunner commandRunner = serviceLocator.getService(CommandRunner.class);
                if (commandRunner != null) {
                    commandRunner.run("_delete-temp-nodes");
                }
            }
        }
    }
}
