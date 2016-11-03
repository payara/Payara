/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.micro.services.asadmin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.micro.services.PayaraInstance;
import fish.payara.appserver.micro.services.command.ClusterCommandResult;
import fish.payara.appserver.micro.services.data.InstanceDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "send-asadmin-command")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("send.asadmin.command")
@ExecuteOn(value = RuntimeType.DAS)
@TargetType(value = CommandTarget.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "send-asadmin-command",
            description = "Sends an asadmin command to an instance")
})
public class SendAsadminCommand implements AdminCommand
{
    @Inject
    private PayaraInstance payaraMicro;
    
    @Param(name = "targets", optional = true)
    private String targets;
    
    @Param(name = "command", optional = false)
    private String command;
    
    @Param(name = "parameters", optional = true, primary = true, multiple = true)
    private String[] parameters;
    
    @Param(name = "explicitTarget", optional = true, multiple = true)
    private String[] explicitTargets;
    
    @Override
    public void execute(AdminCommandContext context)
    {
        final ActionReport actionReport = context.getActionReport();
    
        // Check if the DAS is in a Hazelcast cluster
        if (payaraMicro.isClustered())
        {
            // Get the subset of targets if provided, otherwise just get all clustered Micro instances
            List<String> targetInstanceGuids = getTargetGuids(targets);
            
            // Get the command parameters if provided, otherwise initialise to an empty String
            if (parameters != null) {
                parameters = parseParameters(parameters);
            } else {
                parameters = new String[]{""};
            }
            
            // Run the asadmin command against the targets (or all instances if no targets given)          
            Map<String, Future<ClusterCommandResult>> results = payaraMicro.executeClusteredASAdmin(targetInstanceGuids, 
                    command, parameters);
            
            // Check the command results for any failures
            if (results != null) {
                List<String> successMessages = new ArrayList<>();
                List<String> warningMessages = new ArrayList<>();
                List<String> failureMessages = new ArrayList<>();
                for (Future<ClusterCommandResult> result : results.values()) {
                    try
                    {
                        CommandResult commandResult = result.get();
                        switch (commandResult.getExitStatus())
                        {
                            case SUCCESS:
                                successMessages.add(commandResult.getOutput());
                                break;
                            case WARNING:
                                // If one of the commands has not already failed, set the exit code as WARNING
                                if (actionReport.getActionExitCode() != ExitCode.FAILURE) {
                                    actionReport.setActionExitCode(ExitCode.WARNING);
                                }
                                warningMessages.add(commandResult.getOutput());
                                break;
                            case FAILURE:
                                actionReport.setActionExitCode(ExitCode.FAILURE);
                                failureMessages.add(commandResult.getOutput());
                                break;
                        }
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        Logger.getLogger(SendAsadminCommand.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                for (String successMessage : successMessages) {
                    System.out.println(successMessage);
                }
                
                for (String warningMessage : warningMessages) {
                    System.out.println(warningMessage);
                }
                
                for (String failureMessage : failureMessages) {
                    System.out.println(failureMessage);
                }
                
                switch (actionReport.getActionExitCode()) {
                    case SUCCESS:
                        actionReport.setMessage("Command executed successfully");
                        break;
                    case WARNING:
                        actionReport.setMessage("Command completed with warnings: " + 
                                Arrays.toString(warningMessages.toArray()));
                        break;
                    case FAILURE:
                        actionReport.setMessage("Failures reported: " + Arrays.toString(failureMessages.toArray()));
                        break;
                }
            } else {
                actionReport.setMessage("No results returned!");
                actionReport.setActionExitCode(ExitCode.FAILURE);
            }
        } else {
            actionReport.setMessage("Hazelcast not enabled");
        }
    }  
    
    private List<String> getTargetGuids(String targets) {
        List<String> targetInstanceGuids = new ArrayList<>();
        
        // Get all of the clustered instances
        Set<InstanceDescriptor> instances = payaraMicro.getClusteredPayaras();
        
        // Get the Micro instances that match the targets if provided, otherwise just get all Micro instances
        if (targets != null) {
            // Split the targets into an array to separate them out
            String[] splitTargets = targets.split(",");

            for (InstanceDescriptor instance : instances) {
                for (String target : splitTargets) {
                    // Just match on the name for now, and only send the command to Micro instances
                    if (instance.getInstanceName().equalsIgnoreCase(target) && instance.isMicroInstance()) {
                        targetInstanceGuids.add(instance.getMemberUUID());
                        break;
                    }
                }
            }
        } else {
            for (InstanceDescriptor instance : instances) {
                // Only send the command to Micro instances
                if (instance.isMicroInstance()) {
                    targetInstanceGuids.add(instance.getMemberUUID());
                }
            }
        }
        
        return targetInstanceGuids;
    }
    
    private String[] parseParameters(String[] parameters) throws IllegalArgumentException {
        String primaryParameter = "";
        int primaryParameterIndex = 0;
        
        List<String> parsedParameters = new ArrayList<>();
        
        for (int i = 0; i < parameters.length; i++) {          
            // If the parameter does not contain an "=" sign, then this may be the command's primary parameter, otherwise 
            // just add it to the list
            if (!parameters[i].contains("=")) {
                // If it contains "--", then this is not the command's primary parameter
                if (parameters[i].contains("--")) {
                    // Append the next parameter to this one to make a complete parameter and add it to the List
                    parsedParameters.add(parameters[i] + "=" + parameters[i + 1]);
                    // Skip the next parameter as we've already added it
                    i++;
                } else if (primaryParameter.equals("")) {
                    // If this is the primary parameter, grab it for use later
                    primaryParameter = parameters[i];
                } else {
                    throw new IllegalArgumentException("Parameter " + parameters[i] + "was not prepended with \"--\", and "
                            + "a primary parameter has already been identified: " + primaryParameter);
                }
            } else {
                parsedParameters.add(parameters[i]);
            }
        }           
        
        // Convert the parsedParameters to an array, adding the primary parameter to the end if we found one
        if (!primaryParameter.equals("")) {
            parameters = new String[parsedParameters.size() + 1];
            parameters = parsedParameters.toArray(parameters);
            parameters[parameters.length - 1] = primaryParameter;
        } else {
            parameters = new String[parsedParameters.size()];
            parameters = parsedParameters.toArray(parameters);
        }
        
        return parameters;
    }
}