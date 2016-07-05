/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.service;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.common.util.timer.TimerSchedule;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;



/**
 *
 * @author Daniel
 */
@Service(name="payara-timer")
@RunLevel(StartupRunLevel.VAL)
public class ScheduleService implements EventListener, PostConstruct{
    
    private final String operatingSystem = System.getProperty("os.name");
    String asadmin;
    private static final Logger log = Logger.getLogger(ScheduleService.class.getCanonicalName());
    
    @Inject
    Events events;
    
    private List<Future> futureList;
    
    HashMap map = new HashMap();
    
    @Inject
    ScheduleConfig config;
    
    
    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_STARTUP)){
            //start service
            bootstrapScheduler();
        }
        if (event.is(EventTypes.SERVER_SHUTDOWN)){
            //end service
            
        }
    }

    @Override
    public void postConstruct() {
        events.register(this);
        
    }
 
    public void bootstrapScheduler(){
        if (config.getEnabled()==true){
            startPool();
            prepareSchedules();
            String installRoot = System.getProperty("com.sun.aas.installRoot");
            if (operatingSystem.contains("Windows")){               
                    asadmin = installRoot.substring(0, installRoot.length() - 9) + "bin\\asadmin.bat";           
                } else {
                    asadmin = installRoot.substring(0, installRoot.length() - 9) + "bin/asadmin";            
                }
        }
    }
    
    public void startPool(){
        if (config.getCoreSize()<1){
            executor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1);
        } else{
            executor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(config.getCoreSize());
        }
        
    }
    
    public void prepareSchedules(){
        for (String job: config.getJobs()){
            validateSchedule(job, true);
        }
    }
    
    ScheduledThreadPoolExecutor executor;
    String hello = "hello i am the runnable";
    public void startRun(final String name,final String cron, final String filePath){
            log.log(Level.INFO,"Starting Scheduling");
            
               TimerSchedule schedule = new TimerSchedule(cron);
               Calendar date = schedule.getNextTimeout();
               Date endDate = date.getTime();
                        
            
            
        executor.schedule(new Runnable() {
            
            @Override
            public void run(){
                //do the job
                //TO DO ADD IMPLEMENTATION
                

                
                ThreadLocal nameInstance = new ThreadLocal();
                nameInstance.set(name);
                ThreadLocal cronInstance = new ThreadLocal();
                cronInstance.set(cron);                
                ThreadLocal filePathInstance = new ThreadLocal();
                filePathInstance.set(filePath);
                
                 if (readFile(filePathInstance.get().toString(), nameInstance.toString())){
                     log.log(Level.INFO,"Schedule "+nameInstance.get()+" has run the file successfully: the server log contains the full details");
                 } else {
                     log.log(Level.WARNING,"Schedule "+nameInstance.get()+" encountered some failures: the server log contains the full details");
                 }
                
                
                //schedule next task
                //ArrayList jobList = new ArrayList();
                Boolean found = false;
                for (String job: config.getJobs()){
                    String[] configInfo = job.split(",");
                    String[] nameInfo = configInfo[0].split("=");
                    String[] cronInfo = configInfo[1].split("=");
                    String[] filepathInfo = configInfo[2].split("=");
                    if (nameInfo[1].equals(nameInstance.get()) ){
                        if (validateSchedule(job,false)){
                            cronInstance.set(cronInfo[1]);
                            filePathInstance.set(filepathInfo[1]);
                            found=true;

                            break;
                        }
                    }
                }

                if (found.equals(false)){
                    log.log(Level.SEVERE,nameInstance.get()+" was not found, will not schedule job for it");
                    
                } else if(found.equals(true)){
                    TimerSchedule checkSchedule = new TimerSchedule(cronInstance.get().toString());
                    
                    ScheduledFuture<?> nextJob = executor.schedule(this, checkSchedule.getNextTimeout().getTime().getTime()-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    map.put(name, nextJob);
                    futureList.add(nextJob);
                    
                    
                }
            }
        }, (endDate.getTime()-System.currentTimeMillis()), TimeUnit.MILLISECONDS);

    } //end executor
    

    public HashMap getFuturesList(){
        return map;
    }
    
    public void setFuturesList(HashMap newMap){
        map=newMap;
    }
    
    public Boolean validateSchedule(String job, Boolean start){
        //chnage this the list is reste each time
        
            try {
                String[] configInfo = job.split(",");
                String[] nameInfo = configInfo[0].split("=");
                String[] cronInfo = configInfo[1].split("=");
                String[] filepathInfo = configInfo[2].split("=");
                
            
                String[] cronParts = cronInfo[1].split(" # ");
                if (cronParts.length != 10){
                    log.log(Level.INFO,"size is"+cronParts.length);
                    log.log(Level.INFO,"The cron sequence"+Arrays.toString(cronInfo)+" for "+nameInfo[1]+"is incomplete it must have 7 fields eg * * * * * * *");
                    return false;
                }
                

                if( new File(filepathInfo[1]).exists() == false){
                    log.log(Level.INFO,"Cannot read the file"+filepathInfo[1] +" for "+nameInfo[1]);
                    return false;
                }
                
                
                if (start.equals(true)){
                   startRun(nameInfo[1], cronInfo[1], filepathInfo[1]); 
                }
                return true;
            }catch(IndexOutOfBoundsException OofB) {
                log.log(Level.INFO,"Could not parse <jobs> in Domain.xml please ensure it is correctly formatted, for help look at the create-schedule command");
                OofB.printStackTrace();
            }
        return false;
    }
    
    public String buildCron(String cron){
        String[] cronParts=cron.split(" ");
        if (cronParts.length == 7){
            String cronSequence = cronParts[0] + " # " + cronParts[1] + " # "+ cronParts[2] + " # "+ cronParts[3]+ " # " + cronParts[4]+ " # " + cronParts[5]
            + " # " + cronParts[6]+ " # " + "null" +" # "+"null" + " # "+"null";
            return cronSequence;
        }
        log.log(Level.WARNING,"The cron contains an incorrect number of fields. This cron requires 7 fields");
        throw new StringIndexOutOfBoundsException("The cron contains an incorrect number of fields. This cron requires 7 fields");
    }
    
    public void add(){
        if (config.getFixedSize().equals(false)){
            executor.setCorePoolSize(executor.getCorePoolSize()+1);
        }

    }
    
    public void delete(){
        if (config.getFixedSize().equals(false)){
            executor.setCorePoolSize(executor.getCorePoolSize()-1);   
        }
    }
    
    public void setPoolSize(String i){
        executor.setCorePoolSize(Integer.valueOf(i));
    }
    

    
    public void shutdown(){
        executor.shutdown();
        
    }
    
    public String runCommand(ArrayList<String> arguments){
        String Output="";
        try {
            //Carry out an CLI command and read in the output
            //creates process to run, format is parameters that are seperated by commas
            ProcessBuilder builder = new ProcessBuilder(arguments);
            //merge error and output streams for conveniance
            builder.redirectErrorStream(true);
            Process process = builder.start();
            //Input stream gets the output of the command and cast to a reader for efficiency
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null){
                Output += line;
            }
            //tidy up the method to prevent leaking of resources
            process.waitFor();
            reader.close();
        
        } catch(IOException ex){
            Logger.getLogger(ScheduleService.class.getName()).log(Level.SEVERE, null, ex);
            
        } catch (Exception ex) {
            Logger.getLogger(ScheduleService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Output;
    }
    
    public Boolean readFile(String fileLocation, String name){
        Boolean totalSuccess = true;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)))){
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ArrayList<String> array = new ArrayList();
                array.add(asadmin);
                for (String operand: line.split(" ")){
                    array.add(operand);
                }
                System.out.println(array.toString());
                String output = runCommand(array);
                if (output.contains("success")){
                    log.log(Level.INFO, name + " has successfully run "+array.toString());
                } else {
                    log.log(Level.WARNING, name + " has failed to run "+array.toString());
                    log.log(Level.WARNING, output);
                    totalSuccess = false;
                }
            }
        }catch(Exception io) {
            io.printStackTrace();
        }
        return totalSuccess;
        
    }
}
