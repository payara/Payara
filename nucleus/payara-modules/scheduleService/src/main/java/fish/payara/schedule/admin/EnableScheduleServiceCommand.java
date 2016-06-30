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
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Daniel
 */
@Service(name="enable-scheduling")
@PerLookup
public class EnableScheduleServiceCommand implements AdminCommand{
    
    private static final Logger log = Logger.getLogger(EnableScheduleServiceCommand.class.getCanonicalName());
    
    @Inject
    ScheduleConfig config;
        
    @Inject
    ScheduleService service;
    
    @Param(optional = true)
    String coreSize;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                        
                        if (coreSize != null){
                            configProxy.setCoreSize(coreSize);
                            configProxy.setFixedSize("true");
                        }
                        
                        configProxy.setEnabled("true");
                        service.bootstrapScheduler();
                        log.log(Level.INFO, "Starting Scheduling Service");
                        return null;
                 }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.WARNING, "The transaction has failed "+ ex);
            ex.printStackTrace();
            actionReport.setMessage("Could not enable scheduling. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//en transaction
        
    } 
}
