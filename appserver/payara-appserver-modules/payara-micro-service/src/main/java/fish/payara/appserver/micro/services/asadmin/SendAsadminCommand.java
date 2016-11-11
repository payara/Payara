/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.micro.services.asadmin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.micro.services.PayaraInstance;
import fish.payara.appserver.micro.services.command.ClusterCommandResult;
import fish.payara.appserver.micro.services.data.InstanceDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
 * Sends an asadmin command to members of the Domain Hazelcast Cluster
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
            
            // Add any explicit targets to our list of target GUIDS
            targetInstanceGuids.addAll(getExplicitTargetGUIDS(explicitTargets));
            
            // If no targets have been found, throw an exception and fail out
            if (targetInstanceGuids.isEmpty()) {
                throw new IllegalArgumentException("No targets match!");
            }
            
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
                List<String> warningMessages = new ArrayList<>();
                List<String> failureMessages = new ArrayList<>();
                for (Future<ClusterCommandResult> result : results.values()) {
                    try
                    {
                        CommandResult commandResult = result.get();
                        switch (commandResult.getExitStatus())
                        {
                            case WARNING:
                                // If one of the commands has not already failed, set the exit code as WARNING
                                if (actionReport.getActionExitCode() != ExitCode.FAILURE) {
                                    actionReport.setActionExitCode(ExitCode.WARNING);
                                }
                                // We only want to get the message, not the formatter name or exit code
                                String warningMessage = commandResult.getOutput().split("WARNING")[1];
                                failureMessages.add(warningMessage);
                                break;
                            case FAILURE:
                                actionReport.setActionExitCode(ExitCode.FAILURE);
                                // We only want to get the message, not the formatter name or exit code
                                String failureMessage = commandResult.getOutput().split("FAILURE")[1];
                                failureMessages.add(failureMessage);
                                break;
                        }
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        actionReport.setActionExitCode(ExitCode.FAILURE);
                        actionReport.failure(Logger.getLogger(SendAsadminCommand.class.getName()), 
                                "Ran into an exception during execution: \n", ex);
                    }
                }
                
                switch (actionReport.getActionExitCode()) {
                    case SUCCESS:
                        actionReport.setMessage("Command executed successfully");
                        break;
                    case WARNING:
                        actionReport.setMessage("Command completed with warnings: ");
                        for (String warningMessage : warningMessages) {
                            actionReport.appendMessage("\n" + warningMessage);
                        }
                        break;
                    case FAILURE:
                        actionReport.setMessage("Failures reported: ");
                        for (String failureMessage : failureMessages) {
                            actionReport.appendMessage("\n" + failureMessage);
                        }
                        break;
                }
            } else {
                actionReport.setMessage("No results returned!");
                actionReport.setActionExitCode(ExitCode.FAILURE);
            }
        } else {
            actionReport.setMessage("Hazelcast not enabled");
            actionReport.setActionExitCode(ExitCode.FAILURE);
        }
    }  
    
    /**
     * Gets the GUIDs of the instances in the cluster that match the targets specified by the --targets option
     * @param targets The targets to match
     * @return A list containing the GUIDS of all matching instances
     */
    private List<String> getTargetGuids(String targets) {
        List<String> targetInstanceGuids = new ArrayList<>();
        
        // Get all of the clustered instances
        Set<InstanceDescriptor> instances = payaraMicro.getClusteredPayaras();
        
        // Get the Micro instances that match the targets if provided, otherwise just get all Micro instances if no 
        // explicit targets have been defined
        if (targets != null) {
            // Split the targets into an array to separate them out
            String[] splitTargets = targets.split(",");

            for (InstanceDescriptor instance : instances) {
                for (String target : splitTargets) {
                    // Split the group from the instance name if a group has been provided, otherwise just match instances
                    if (target.contains(":")) {
                        String splitTarget[] = target.split(":");
                        // Make sure it's in the correct format
                        if (splitTarget.length == 2) {
                            String targetGroup = splitTarget[0];
                            String targetInstance = splitTarget[1];

                            // Get the target GUIDS, taking into account wildcards
                            if (targetGroup.equals("*")) {
                                if (targetInstance.equals(("*"))) {
                                    // Match everything
                                    if (instance.isMicroInstance()) {
                                        targetInstanceGuids.add(instance.getMemberUUID());
                                    }
                                } else {
                                    // Match on instance name only
                                    if (instance.getInstanceName().equalsIgnoreCase(targetInstance) && 
                                            instance.isMicroInstance()) {
                                        targetInstanceGuids.add(instance.getMemberUUID());
                                        break;
                                    }
                                }   
                            } else if (targetInstance.equals(("*"))) {
                                // Match on group name only
                                if (instance.getInstanceGroup().equalsIgnoreCase(targetGroup) && 
                                        instance.isMicroInstance()) {
                                    targetInstanceGuids.add(instance.getMemberUUID());
                                    break;
                                }   
                            } else {
                                // Match on group and instance name
                                if ((instance.getInstanceGroup().equalsIgnoreCase(targetGroup) && 
                                        instance.getInstanceName().equalsIgnoreCase(targetInstance)) && 
                                        instance.isMicroInstance()) {
                                    targetInstanceGuids.add(instance.getMemberUUID());
                                    break;
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Target contains more than one colon \":\", "
                                    + "this is not allowed");
                        }
                    } else {
                        // Match everything
                        if (target.equals(("*"))) {
                            if (instance.isMicroInstance()) {
                                targetInstanceGuids.add(instance.getMemberUUID());
                            }
                        } else {
                            // Match on instance name
                            if (instance.getInstanceName().equalsIgnoreCase(target) && instance.isMicroInstance()) {
                                targetInstanceGuids.add(instance.getMemberUUID());
                                break;
                            }
                        }
                    }
                }
            }
        } else if (explicitTargets.length == 0) {
            for (InstanceDescriptor instance : instances) {
                // Only send the command to Micro instances
                if (instance.isMicroInstance()) {
                    targetInstanceGuids.add(instance.getMemberUUID());
                }
            }
        }
        
        return targetInstanceGuids;
    }
    
    /**
     * Gets the GUIDs of the instances in the cluster that match the targets specified by the --explicitTarget option
     * @param explicitTargets The explicit targets
     * @return A list containing the GUIDs of the matched targets
     */
    private List<String> getExplicitTargetGUIDS(String[] explicitTargets) {
        List<String> targetGuids = new ArrayList<>();
        
        // Get all of the clustered instances
        Set<InstanceDescriptor> instances = payaraMicro.getClusteredPayaras();
        
        // Loop through all instances to find any matching targets and add them to the list
        for (String explicitTarget : explicitTargets) {
            // Split the target into its individual components
            String[] explicitTargetComponents = explicitTarget.split(":");
            if (explicitTargetComponents.length == 3) {
                String host = explicitTargetComponents[0];
                String portNumber = explicitTargetComponents[1];
                String instanceName = explicitTargetComponents[2];
                
                for (InstanceDescriptor instance : instances) {
                    // Check if this instance's host name (or IP address), hazelcast port number, and name match
                    if ((instance.getHostName().getHostName().equals(host) || 
                            instance.getHostName().getHostAddress().equals(host)) && 
                            Integer.toString(instance.getHazelcastPort()).equals(portNumber) && 
                            instance.getInstanceName().equalsIgnoreCase(instanceName)) {
                        targetGuids.add(instance.getMemberUUID());
                        // We've found an instance that matches a target, so remove it from the list and move on to the 
                        // next target
                        instances.remove(instance);
                        break;
                    }
                }
            } else {
                throw new IllegalArgumentException("Explicit target needs to take the form of: "
                        + "hostOrIpAddress:hazelcastPort:instanceName"
                        + "\nMake sure there are exactly 3 colons (\":\")");
            }
        }
        
        return targetGuids;
    }
    
    /**
     * Helper method to parse the parameters into a usable format
     * @param parameters The parameters to parse
     * @return A String array containing the parsed parameters in a usable format
     */
    private String[] parseParameters(String[] parameters) {
        String primaryParameter = "";
        List<String> parsedParameters = new ArrayList<>();
        
        // Loop through all provided parameters and parse them
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
                    // If we've reached here, that means a primary parameter has already been found, so fail out
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