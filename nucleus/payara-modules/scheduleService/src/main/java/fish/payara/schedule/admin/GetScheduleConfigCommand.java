/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;

import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;
import java.beans.PropertyVetoException;
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
@Service(name="get-schedule-config")
@PerLookup
public class GetScheduleConfigCommand implements AdminCommand{
    private static final Logger log = Logger.getLogger(EnableScheduleServiceCommand.class.getCanonicalName());
    
    @Inject
    ScheduleConfig config;
        
    @Inject
    ScheduleService service;
    
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                        StringBuilder outputString = new StringBuilder();
                        outputString.append("Configuration information for the scheduling service"+ System.lineSeparator());
                        outputString.append( System.lineSeparator());
                        outputString.append("Scheduling Enabled:        "+configProxy.getEnabled() + System.lineSeparator());
                        outputString.append("Thread Pool Core Size:     "+configProxy.getCoreSize()+ System.lineSeparator());
                        outputString.append("Use Fixed Core Size Value: "+ configProxy.getFixedSize()+ System.lineSeparator());
                        outputString.append("List of Schedules:         "+ System.lineSeparator());
                        for (String job: configProxy.getJobs()){
                            outputString.append(readJob(job)+ System.lineSeparator());
                        }
                        
                        actionReport.setMessage(outputString.toString());
                        
                        return null;
                 }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.WARNING, "The transaction has failed "+ ex);
            ex.printStackTrace();
            actionReport.setMessage("Could not get config for scheduling. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//en transaction
        
    }

    public String readJob(String schedule){
        String[] configInfo = schedule.split(",");
        String[] nameInfo = configInfo[0].split("=");
        String[] cronInfo = configInfo[1].split("=");
        String[] filepathInfo = configInfo[2].split("=");


        String[] cronParts = cronInfo[1].split(" # ");
        String humanReadableCron = cronParts[0] +" "+ cronParts[1] +" "+ cronParts[2] +" "+ cronParts[3] +" "+ cronParts[4] +" "+ cronParts[5]
             +" "+ cronParts[6];
        
        return "Schedule name="+nameInfo[1]+", cron= " + humanReadableCron + ", file Path= "+ filepathInfo[1];
    }
}
