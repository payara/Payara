/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.faulttolerance.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.microprofile.faulttolerance.FaultToleranceServiceConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
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

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "get-fault-tolerance-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, 
    CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = FaultToleranceServiceConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-fault-tolerance-configuration",
            description = "Gets the Fault Tolerance Configuration")
})
public class GetFaultToleranceConfigurationCommand implements AdminCommand {

    private final String OUTPUT_HEADERS[] = {"Managed Executor Service Name", 
            "Managed Scheduled Executor Service Name"};
    
    @Inject
    private Target targetUtil;
    
    @Param(optional = true, defaultValue = "server-config")
    private String target;
    
    @Override
    public void execute(AdminCommandContext acc) {
        Config targetConfig = targetUtil.getConfig(target);
        
        if (targetConfig == null) {
            acc.getActionReport().setMessage("No such config name: " + targetUtil);
            acc.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        FaultToleranceServiceConfiguration faultToleranceServiceConfiguration = targetConfig
                .getExtensionByType(FaultToleranceServiceConfiguration.class);
        
        ColumnFormatter columnFormatter = new ColumnFormatter(OUTPUT_HEADERS);
        Object[] outputValues = {
            faultToleranceServiceConfiguration.getManagedExecutorService(),
            faultToleranceServiceConfiguration.getManagedScheduledExecutorService()
        };        
        columnFormatter.addRow(outputValues);
        
        acc.getActionReport().appendMessage(columnFormatter.toString());
        
        Map<String, Object> extraPropertiesMap = new HashMap<>();
        extraPropertiesMap.put("managedExecutorServiceName", faultToleranceServiceConfiguration
                .getManagedExecutorService());
        extraPropertiesMap.put("managedScheduledExecutorServiceName", faultToleranceServiceConfiguration
                .getManagedScheduledExecutorService());
        
        Properties extraProperties = new Properties();
        extraProperties.put("faultToleranceConfiguration", extraPropertiesMap);
        acc.getActionReport().setExtraProperties(extraProperties);
    }
}
