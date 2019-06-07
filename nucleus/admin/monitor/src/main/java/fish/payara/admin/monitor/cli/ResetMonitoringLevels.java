/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.admin.monitor.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.ColumnFormatter;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
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
 * @author Alan Roth
 */
@Service(name = "reset-monitoring-levels")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class ResetMonitoringLevels  implements AdminCommand {
    
    @Param(name = "target", optional = true, defaultValue = "server-config")
    String target;
    
    @Param(optional = true, shortName = "v")
    private boolean verbose;

    @Inject
    private Target targetUtil;
    
    @Inject
    private Logger logger;
    
    private MonitoringService monitoringService;
    
    @Override
    public void execute(AdminCommandContext context){
        ActionReport actionReport = context.getActionReport();
        
        List<String> validModuleList = Arrays.asList(Constants.validModuleNames);
        Config config = targetUtil.getConfig(target);
        if(config != null){
            monitoringService = config.getMonitoringService();
        }else{
            actionReport.setMessage("Cound not find target: " + target);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        Map<String, String> enabledModules = getPreviouslyEnabledModules(validModuleList);

        //Setting all modules to "OFF"
        for(String module : validModuleList){
            try {
                ConfigSupport.apply((final MonitoringService monitoringServiceProxy) -> {
                    monitoringServiceProxy.setMonitoringLevel(module, "OFF");
                    return monitoringServiceProxy;
                }, monitoringService);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Failed to execute the command set-monitoring-level, while setting modules to 'OFF': {0}", ex.getCause().getMessage());
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        }
        
        //Setting modules back to what they were previously
        for (String module : enabledModules.keySet()) {
            String moduleLevel = enabledModules.get(module);
            try {
                ConfigSupport.apply((final MonitoringService monitoringServiceProxy) -> {
                    monitoringServiceProxy.setMonitoringLevel(module, moduleLevel);
                    return monitoringServiceProxy;
                }, monitoringService);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Failed to execute the command set-monitoring-level, while setting modules to previous values: {0}", ex.getCause().getMessage());
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        }
        
        if (verbose) {
            final String[] headers = {"Module", "Monitoring Level"};
            ColumnFormatter columnFormatter = new ColumnFormatter(headers);
            Map<String, Object> extraPropertiesMap = new HashMap<>();

            for (String module : enabledModules.keySet()) {
                columnFormatter.addRow(new Object[]{module,
                    monitoringService.getMonitoringLevel(module)});
                extraPropertiesMap.put(module,
                        monitoringService.getMonitoringLevel(module));
                actionReport.setMessage(columnFormatter.toString());
            }
        }else{
            actionReport.setMessage("Reset " + enabledModules.size() + " modules");
        }
    }
    
    private Map<String, String> getPreviouslyEnabledModules(List<String> validModules){
        Map<String, String> enabledModules = new HashMap<>();
        if(!validModules.isEmpty()){
            for (String module : validModules) {
                String level = monitoringService.getMonitoringLevel(module);
                if(!level.equals("OFF")){
                    enabledModules.put(module, level);
                }
            }
        }
        return enabledModules;
    }
}
