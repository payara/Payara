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
@Service(name="disable-scheduling")
@PerLookup
public class DisableScheduleServiceCommand implements AdminCommand{
    
    private static final Logger log = Logger.getLogger(DisableScheduleServiceCommand.class.getCanonicalName());
    
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
                        configProxy.setEnabled("false");
                        configProxy.setCoreSize("0");
                        configProxy.setFixedSize("false");
                        service.shutdown();
                        log.log(Level.INFO, "Shutting down the scheduling service");
                        return null;
                 }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.WARNING, "The transaction has failed "+ ex);
            ex.printStackTrace();
            actionReport.setMessage("Could not disable scheduling. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//en transaction
        
    }
}
