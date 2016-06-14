/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;

import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Daniel
 */
@Service(name="set-schedule")
@PerLookup
public class SetScheduleCommand implements AdminCommand{
    @Inject
    ScheduleConfig config;
    
    @Param(name="name",optional=false)
    String name;
    
    @Param(name="cron",optional=false)
    String cron;
    
    @Param(name="filePath", optional=false)
    String filePath;
    
    List<String> newList;
    
    @Inject
    ScheduleService service;
    
    String newJob;
    
    String job;

    @Override
    public void execute(AdminCommandContext context) {
        System.out.println("SET RUN");
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                    if (name != null){
                        Boolean found = false;

                        List<String> jobs = new ArrayList();
                        for (int i =0; i < configProxy.getJobs().size();i++){
                            job = configProxy.getJobs().get(i);
                            String[] configInfo = job.split(",");
                            String[] nameInfo = configInfo[0].split("=");

                            System.out.println("SET: the name form the domain.xml is "+nameInfo[1]);
                            if (nameInfo[1].equals(name) ){ 
                                newJob = "name="+nameInfo[1]+",cron="+service.buildCron(cron)+",filePath="+filePath;
                                if (service.validateSchedule(newJob,false)){                                      
                                    jobs.add(newJob);
                                    System.out.println("the job is" + configProxy.getJobs().get(i));
                                    System.out.println("The new job is " +newJob);

                                    System.out.println("the job is" + configProxy.getJobs().get(i));
                                    found=true;
                                }                              
                            } else{
                                jobs.add(configProxy.getJobs().get(i));
                            }
                            
                        }
                                            
                        if (found.equals(false)){
                            System.out.println("The schedule name was not found, please check that schedule exists");
                        }else {
                            HashMap david = service.getFuturesList();                            
                            System.out.println("futures = "+ david.toString());
                            System.out.println(" NUMBER FOUR");
                            System.out.println(david.get(name));
                            Future job = (Future)david.get(name);
                            job.cancel(true);
                            configProxy.setJobs(jobs);
                            service.validateSchedule(newJob, true);
                            
                        }
                    }
                    return null;
                }
            },config);
        }catch (TransactionFailure ex){
            System.out.println("The transaction has failed "+ ex);
        }//en transaction
    }
    
    
    
}
