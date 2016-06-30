/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;


import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import java.util.logging.Level;
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
@Service(name="set-schedule-thread-pool")
@PerLookup
public class SetScheduleServicePoolSizeCommand implements AdminCommand{

    private static final Logger log = Logger.getLogger(SetScheduleServicePoolSizeCommand.class.getCanonicalName());
    
    @Inject
    ScheduleConfig config;
        
    @Inject
    ScheduleService service;
    
    @Param
    String coreSize;
    
    @Param
    String useCoreSize;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{                       
                        configProxy.setCoreSize(coreSize);
                        service.setPoolSize(coreSize);
                        if (useCoreSize.equals("true")|useCoreSize.equals("false")){
                            configProxy.setFixedSize(useCoreSize);
                        }else{
                            actionReport.setMessage("useCoreSize must be 'true' or false'");
                            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        }
                        configProxy.setFixedSize(useCoreSize);                                                                        
                        return null;
                 }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.INFO ,"The transaction has failed "+ ex);
            actionReport.setMessage("Set core size. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//en transaction
        
    }
    
}
