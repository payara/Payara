package fish.payara.nucleus.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author steve
 */
@Service(name = "payara-cleanup")
@RunLevel(StartupRunLevel.VAL)
public class CleanupPostBoot implements EventListener {

    @Inject
    Events events;
    
    private static Logger logger = Logger.getLogger(CleanupPostBoot.class.getName());

    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event event) {

        if (event.is(EventTypes.SERVER_READY)) {
            logger.info("Cleaning JarFileFactory Cache to prevent jar FD leaks");
        try {
            // Ensure JarFile is closed
            Class clazz = Class.forName("sun.net.www.protocol.jar.JarFileFactory", true, URL.class.getClassLoader());
            Field fields[] = clazz.getDeclaredFields();
            for (Field field : fields) {
                if ("fileCache".equals(field.getName())) {
                    field.setAccessible(true);
                    HashMap<String,JarFile> files = (HashMap<String,JarFile>) field.get(null);
                    Set<JarFile> jars = new HashSet<>();
                    jars.addAll(files.values());
                    for (JarFile file : jars) {
                        file.close();
                    }
                } 
            }
        } catch (ClassNotFoundException | IllegalAccessException | SecurityException | IllegalArgumentException ex) {
            logger.log(Level.SEVERE, null, ex);
        }   catch (IOException ex) {            
                logger.log(Level.SEVERE, null, ex);
            }            
        }
    }

}
