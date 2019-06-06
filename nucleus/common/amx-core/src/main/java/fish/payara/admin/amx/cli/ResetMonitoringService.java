/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.admin.amx.cli;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.monitoring.MonitoringItem;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author Alan Roth
 */
@Service(name = "reset-monitoring-service")
@PerLookup
public class ResetMonitoringService implements AdminCommand {

    @Param(name="target", optional = true, defaultValue = "server-config")
    private String target;

    @Inject
    private Target targetUtil;
    
    @Inject
    static CommandRunner commandRunner;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Config config = targetUtil.getConfig(target);

        if (config == null) {
            context.getActionReport().setMessage("No such config name: " + targetUtil);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

       ParameterMap data = new ParameterMap();

        //MonitoringService monitoringService = config.getMonitoringService();
        List<Property> properties = config.getMonitoringService().getProperty();

        Map<String, String> enabledMonitoringLevels = new HashMap<>();

        for (Property property : properties) {
            if (!property.getValue().equals("OFF")) {
                enabledMonitoringLevels.put(property.getName(), property.getValue());
            }
        
            try {
                property.setValue("OFF");
            } catch (PropertyVetoException ex) {
                context.getActionReport().setMessage("Couldn't set property to OFF: " + ex.getMessage());
            }
        }
        
        for (Property property : properties) {
            if (enabledMonitoringLevels.containsKey(property.getName())) {
                try {
                    property.setValue(enabledMonitoringLevels.get(property.getName()));
                } catch (PropertyVetoException ex) {
                    context.getActionReport().setMessage("Couldn't re-enabled properties: " + ex.getMessage());
                }
            }
        }
    
        //String payload = "";
        
        for (Property property : properties) {
            if(!property.getValue().equals("OFF")){
                enabledMonitoringLevels.put(property.getName(), property.getValue());
            }
            data.add(property.getName(), "OFF");
        }

        //data.add("id", payload);
        data.add("DEFAULT", "configs.config."+ config.getName() + ".monitoring-service.module-monitoring-levels");
        data.add("target", config.getName());
        data.add("profiler", "false");
       
        runCommand("set", data, context.getSubject(), false);
    }
    
    /**
     * @param commandName
     * @param parameters
     * @param subject
     * @param managedJob
     * @return
     */
    
    public static RestActionReporter runCommand(String commandName,
                                                ParameterMap parameters,
                                                Subject subject,
                                                boolean managedJob) {            

        RestActionReporter ar = new RestActionReporter();
        CommandInvocation commandInvocation = commandRunner.getCommandInvocation(commandName, ar, subject);
        if (managedJob) {
            commandInvocation.managedJob();
        }
        
        commandInvocation.parameters(parameters).execute();

        return ar;
    }
}
