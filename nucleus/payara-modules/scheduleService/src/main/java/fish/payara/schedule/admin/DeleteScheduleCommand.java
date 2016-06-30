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
@Service(name="delete-schedule")
@PerLookup
public class DeleteScheduleCommand implements AdminCommand{
    @Inject
    ScheduleConfig config;
    
    private static final Logger log = Logger.getLogger(DeleteScheduleCommand.class.getCanonicalName());
    
    @Param(name="name",optional=false)
    String name;   
    
    @Inject
    ScheduleService service;
    
    String job;

    @Override
    public void execute(AdminCommandContext context) {
                                   
        final ActionReport actionReport = context.getActionReport();
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

                            if (nameInfo[1].equals(name) ){                                
                                    found=true;                                                             
                            } else{
                                jobs.add(configProxy.getJobs().get(i));
                            }

                            HashMap scheduledTasks = service.getFuturesList();
                            
                            Future job = (Future)scheduledTasks.get(name);
                            job.cancel(true);
                        }
                        
                        if (found.equals(false)){
                            log.log(Level.WARNING,"The schedule "+name+" was not found, please check that schedule exists");
                            actionReport.setMessage("Could not delete schedule. See server.log for more details");
                            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        }else {
                            configProxy.setJobs(jobs);
                            log.log(Level.INFO,"Schedule "+ name + " was removed");
                            service.delete();
                        }
                    }
                    return null;
                }
            },config);
        }catch (TransactionFailure ex){
            ex.printStackTrace();
            log.log(Level.WARNING,"The transaction has failed ");
            actionReport.setMessage("Could not delete schedule. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//en transaction
    }
}
