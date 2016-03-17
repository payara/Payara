/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.asadmin.recorder.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.asadmin.recorder.AsadminRecorderConfiguration;
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
@Service(name = "asadmin-recorder-enabled")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("asadmin.recorder.enabled")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class, 
            opType = RestEndpoint.OpType.GET,
            path = "asadmin-recorder-enabled",
            description = "Checks if the Asadmin Recorder Service is enabled")
})
public class AsadminRecorderEnabled implements AdminCommand
{
    @Inject
    private Target targetUtil;
    
    @Inject
    AsadminRecorderConfiguration asadminRecorderConfiguration;
    
    private final String target = "server";
    
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

        final ActionReport actionReport = context.getActionReport();
        
        Properties extraProps = new Properties();
        
        if (asadminRecorderConfiguration.isEnabled()) {
            extraProps.put("asadminRecorderEnabled", true);
            actionReport.setMessage("Asadmin Recorder Service is enabled");
        } else {
            extraProps.put("asadminRecorderEnabled", false);
            actionReport.setMessage("Asadmin Recorder Service is disabled");
        }
          
        actionReport.setExtraProperties(extraProps);
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }  
}
