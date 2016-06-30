/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;


import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import org.glassfish.api.ActionReport;
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

    private static final Logger log = Logger.getLogger(CreateScheduleCommand.class.getCanonicalName());
    
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
        final ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                        if (name != null){
                            //addStuff.fire("hello");
                            List<String> jobs = configProxy.getJobs();
                            System.out.println("ONE");
                            for (String job:jobs){
                                String[] configInfo = job.split(",");
                                String[] nameInfo = configInfo[0].split("=");
                                System.out.println("the name form the domain.xml is "+nameInfo[1]);
                                if (nameInfo[1].equals(name) ){
                                    actionReport.setMessage("This schedule name already exists please a different one ");
                                    actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                    return null;
                                }
                                
                            }
                            String configInfo = "name="+name+",cron="+service.buildCron(cron)+",filePath="+filePath;
                            if (service.validateSchedule(configInfo, true)){
                                jobs.add(configInfo);
                                log.log(Level.INFO,"The schedule "+ name +" has been added");
                                //configProxy.setJobConfig(job);
                                service.add(); 
                            } else {
                                actionReport.setMessage("Could not add schedule. See server.log for more details");
                                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            }
                            
                        }
                        return null;
                }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.WARNING,"The transaction has failed "+ ex);
            ex.printStackTrace();
            actionReport.setMessage("Could not add schedule. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//end transaction
        
    }//end execute
    

}
