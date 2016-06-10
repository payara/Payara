/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;


import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleJobConfig;
import fish.payara.schedule.service.ScheduleService;

import java.beans.PropertyVetoException;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Event;

import javax.inject.Inject;
import javax.inject.Qualifier;
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
@Service(name="create-schedule")
@PerLookup
public class CreateScheduleCommand implements AdminCommand{

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

    
    @Override
    public void execute(AdminCommandContext context) {
        
        System.out.println("I AM FOO");
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                    if (name != null){
                        //addStuff.fire("hello");
                        List<String> jobs = configProxy.getJobs();
                        for (String job:jobs){
                            service.validateSchedule(job, false);
                        }
                        String configInfo = "name="+name+",cron="+cron+",filePath="+filePath;
                        service.validateSchedule(configInfo, true);
                        jobs.add(configInfo);


                        configProxy.setEnabled(name);
                        //configProxy.setJobConfig(job);
                    }
                    return null;
                }
            },config);
        }catch (TransactionFailure ex){
            System.out.println("The transaction has failed "+ ex);
        }//en transaction
        
        service.add();
        
    }//end execute
    
    public List<String> createList(){
        List<String> jobs = config.getJobs();
        String configInfo = "name="+name+",cron="+cron+",filePath="+filePath;
        jobs.add(configInfo);
        return jobs;
    }

}
