/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.backup.service;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.AccessControlContext;
import java.util.*;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Schedule;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
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
    Thread thread;
    private static final Logger logger = Logger.getLogger(BackupConfigService.class.getCanonicalName());
    
    @Inject
    private Events events;
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    BackupConfigConfiguration config;



    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_STARTUP)){
            System.out.println("PREPARE YOURSELF");
            System.out.println(config.getMinutes());
            testSchedule();
        }
        
    }

    @Override
    public void postConstruct() {
        events.register(this);
        System.out.println("HELLOES");
    }
    
    
    public void printSchedule() {
        Timer time = new Timer(); // Instantiate Timer Object
        //BackupTimer st = new BackupTimer(); // Instantiate SheduledTask class
        //time.schedule(st, 0, 10000); // Create Repetitively task for every 1 secs
    }
    
    public void testSchedule(){
        BackupTimer timer = new BackupTimer();
        timer.run();
       /* System.out.println("starting thread");
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleAtFixedRate(thread, 1, 1, TimeUnit.SECONDS);
                
                try {
                    PrintWriter writer = new PrintWriter("test.txt", "UTF-8");
                    writer.println("HELLO");
                    PrintWriter writer2 = new PrintWriter("/home/user/test.txt", "UTF-8");
                    writer2.println("HELLO");
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(BackupConfigService.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(BackupConfigService.class.getName()).log(Level.SEVERE, null, ex);
                }
                BackupTimer timer = new BackupTimer("1","2");
                timer.timer();
             
            }
            
        });

            */     
    } //end of testSchedule
    
    
    
        
}//end of class

