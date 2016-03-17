/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.asadmin.recorder.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.asadmin.recorder.AsadminRecorderConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
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
@Service(name = "get-asadmin-recorder-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.asadmin.recorder.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class, 
            opType = RestEndpoint.OpType.GET,
            path = "get-asadmin-recorder-configuration",
            description = "Gets the current configuration settings of the "
                    + "Asadmin Recorder Service")
})
public class GetAsadminRecorderConfiguration implements AdminCommand
{
    @Inject
    private Target targetUtil;
    
    private final String target = "server";
    private final String[] headers = {"Enabled", "Filter Commands", 
            "Output Location", "Filtered Commands"};
            
    @Override
    public void execute(AdminCommandContext context)
    {
        Config config = targetUtil.getConfig(target);
        if (config == null) 
        {
            context.getActionReport().setMessage("No such config named: "
                    + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode
                    .FAILURE);
            return;
        }
        
        AsadminRecorderConfiguration asadminRecorderConfiguration = 
                config.getExtensionByType(AsadminRecorderConfiguration.class);
        final ActionReport actionReport = context.getActionReport();
        
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = {asadminRecorderConfiguration.isEnabled(), 
                asadminRecorderConfiguration.filterCommands(), 
                asadminRecorderConfiguration.getOutputLocation(),
                asadminRecorderConfiguration.getFilteredCommands()};
        
        columnFormatter.addRow(values);
        
        Map<String, Object> map = new HashMap<String, Object>();
        Properties extraProps = new Properties();
        map.put("enabled", values[0]);
        map.put("filterCommands", values[1]);
        map.put("outputLocation", values[2]);
        map.put("filteredCommands", values[3]);
        extraProps.put("getAsadminRecorderConfiguration",map);
        
        actionReport.setExtraProperties(extraProps);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }  
}
