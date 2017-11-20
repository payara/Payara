/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.faulttolerance.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.microprofile.faulttolerance.FaultToleranceServiceConfiguration;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "set-fault-tolerance-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, 
    CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = FaultToleranceServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-fault-tolerance-configuration",
            description = "Sets the Fault Tolerance Configuration")
})
public class SetFaultToleranceConfigurationCommand implements AdminCommand {
    
    private static final Logger logger = Logger.getLogger(SetFaultToleranceConfigurationCommand.class.getName());
    
    @Inject
    private Target targetUtil;
    
    @Param(optional = true, alias = "managedexecutoeservicename")
    private String managedExecutorServiceName;
    
    @Param(optional = true, alias = "managedscheduledexecutorservicename")
    private String managedScheduledExecutorServiceName;
    
    @Param(optional = true, defaultValue = "server-config")
    private String target;
    
    @Override
    public void execute(AdminCommandContext acc) {
        Config targetConfig = targetUtil.getConfig(target);
        FaultToleranceServiceConfiguration faultToleranceServiceConfiguration = targetConfig
                .getExtensionByType(FaultToleranceServiceConfiguration.class);
        
        try {
            ConfigSupport.apply((FaultToleranceServiceConfiguration configProxy) -> {
                if (managedExecutorServiceName != null) {
                    configProxy.setManagedExecutorService(managedExecutorServiceName);
                }
                
                if (managedScheduledExecutorServiceName != null) {
                    configProxy.setManagedScheduledExecutorService(managedScheduledExecutorServiceName);
                }
                
                return null;
            }, faultToleranceServiceConfiguration);
        } catch (TransactionFailure ex) {
            acc.getActionReport().failure(logger, "Failed to update Fault Tolerance configuration", ex);
        }
    }
    
}
