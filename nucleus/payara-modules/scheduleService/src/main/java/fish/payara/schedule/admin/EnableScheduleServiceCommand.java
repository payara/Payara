/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;

import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;
import java.beans.PropertyVetoException;
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
@Service(name="enable-schedule")
@PerLookup
public class EnableScheduleServiceCommand implements AdminCommand{
       @Inject
    ScheduleConfig config;
        
    @Inject
    ScheduleService service;
    
    @Param(optional = true)
    String coreSize;
    
    @Override
    public void execute(AdminCommandContext context) {
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                        if (coreSize != null){
                            config.setCoreSize(coreSize);
                        }
                        
                        config.setEnabled("true");
                        service.bootstrapScheduler();
                        return null;
                 }
            },config);
        }catch (TransactionFailure ex){
            System.out.println("The transaction has failed "+ ex);
        }//en transaction
        
    } 
}
