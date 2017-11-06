/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.appserver.micro.services.command.ClusterCommandResultImpl;
import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.data.InstanceDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**{
        this.instanceGroup = instanceGroup;
    }
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
            opType = RestEndpoint.OpType.POST,
            path = "send-asadmin-command",
            description = "Sends an asadmin command to an instance")
})
public class SendAsadminCommand implements AdminCommand
{
    @Inject
    private PayaraInstanceImpl payaraMicro;
    
    @Param(name = "targets", optional = true)
    private String targets;
    
    @Param(name = "command", optional = false)
    private String command;
    
    @Param(name = "parameters", optional = true, primary = true, multiple = true)
    private String[] parameters;
    
    @Param(name = "explicitTarget", optional = true, multiple = true)
    private String[] explicitTargets;
    
    @Param(name = "verbose", optional = true)
    private boolean verbose;
    
    @Param(name = "logOutput", optional = true)
    private boolean logOutput;
    
    @Override
    public void execute(AdminCommandContext context)
    {
        final ActionReport actionReport = context.getActionReport();
    
        // Check if the DAS is in a Hazelcast cluster
        if (payaraMicro.isClustered())
        {
            // Get the subset of targets if provided, otherwise just get all clustered Micro instances
            Map<String, InstanceDescriptor> targetInstanceDescriptors = getTargetInstanceDescriptors(targets);
            
            // Add any explicit targets to our list of target GUIDS
            targetInstanceDescriptors.putAll(getExplicitTargetInstanceDescriptors(explicitTargets));
            
            // If no targets have been found, throw an exception and fail out
            if (targetInstanceDescriptors.isEmpty()) {
                throw new IllegalArgumentException("No targets match!");
            }
            
            // Get the command parameters if provided, otherwise initialise to an empty String
            if (parameters != null) {
                parameters = parseParameters(parameters);
            } else {
                parameters = new String[]{""};
            }
            
            // Run the asadmin command against the targets (or all instances if no targets given)          
            Map<String, Future<ClusterCommandResult>> results = payaraMicro.executeClusteredASAdmin(
                    targetInstanceDescriptors.keySet(), command, parameters);
            
            // Check the command results for any failures
            if (results != null) {
                List<String> successMessages = new ArrayList<>();
                List<String> warningMessages = new ArrayList<>();
                List<String> failureMessages = new ArrayList<>();
                
                for (Map.Entry<String, Future<ClusterCommandResult>> result : results.entrySet()) {
                    try
                    {
                        ClusterCommandResult commandResult = result.getValue().get();
                        switch (commandResult.getExitStatus())
                        {
                            case SUCCESS:
                                // Only get the success messages if we've asked for them
                                if (verbose || logOutput) {
                                    // We only want to get the message, not the formatter name or exit code
                                    String rawOutput = commandResult.getOutput();
                                    String[] outputComponents = rawOutput.split(commandResult.getExitStatus().name());
                                    String output = outputComponents.length > 1 ? outputComponents[1] : rawOutput;
                                    
                                    // Box the name and add it to the output to help split up the individual responses, 
                                    // since the success messages don't inherently provide information about what instance 
                                    // the command was run on
                                    String boxedInstanceName = boxInstanceName(output);
                                    successMessages.add("\n" 
                                            + targetInstanceDescriptors.get(result.getKey()).getInstanceName() + "\n" 
                                            + boxedInstanceName);
                                }
                                
                                break;
                            case WARNING:
                                // If one of the commands has not already failed, set the exit code as WARNING
                                if (actionReport.getActionExitCode() != ExitCode.FAILURE) {
                                    actionReport.setActionExitCode(ExitCode.WARNING);
                                }
                                // We only want to get the message, not the formatter name or exit code
                                failureMessages.add("\n" + targetInstanceDescriptors.get(result.getKey()).getInstanceName()
                                        + ":" + processException(commandResult));
                                break;
                            case FAILURE:
                                actionReport.setActionExitCode(ExitCode.FAILURE);
                                // We only want to get the message, not the formatter name or exit code
                                failureMessages.add("\n" + targetInstanceDescriptors.get(result.getKey()).getInstanceName()
                                        + ":\n" + processException(commandResult));
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
                        
                        // Skip if neither verbose or logOutput were selected
                        if (verbose || logOutput) {
                            String output = "";
                            
                            // Combine the success messages into one String
                            for (String successMessage : successMessages) {
                                output += "\n" + successMessage;
                            }
                            
                            // Only print out the messages if verbose was chosen
                            if (verbose) {
                                actionReport.setMessage(output);
                            }

                            // Only log the messages if logOutput was chosen
                            if (logOutput) {
                                Logger.getLogger(SendAsadminCommand.class.getName()).log(Level.INFO, output);
                            }
                        }
                        
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
     * Surrounds the provided instance name with a box to help dientify log outputs.
     * @param instanceName The instance name to be boxed
     * @return A String containing the boxed instance name
     */
    private String boxInstanceName(String instanceName) {
        String paddedInstanceName = "";
        final int boxSize = instanceName.length() + 6;
        
        // Draw the top of the box
        for (int i = 0; i < boxSize; i++) {
            if (i == 0 || i == boxSize - 1) {
                paddedInstanceName += "+";
            } else {
                paddedInstanceName += "=";
            }
        }
        
        // Move to the next line
        paddedInstanceName += "\n";
        
        // Draw the side of the box and add the padding
        paddedInstanceName += "|  ";
        
        // Add the instanceName
        paddedInstanceName += instanceName;
        
        // Add the appending padding and draw the other side of the box
        paddedInstanceName += "  |";
                
        //Move to the next line
        paddedInstanceName += "\n";
        
        // Draw the bottom of the box
        for (int i = 0; i < boxSize; i++) {
            if (i == 0 || i == boxSize - 1) {
                paddedInstanceName += "+";
            } else {
                paddedInstanceName += "=";
            }
        }
        
        return paddedInstanceName;
    }
    
    /**
     * @param commandResult input
     * @return readable error message from command result
     */
    private String processException(ClusterCommandResult commandResult) {
        String msg = commandResult.getOutput();
        String[] msgs = msg.split(commandResult.getExitStatus().name());
        return msgs.length > 1? msgs[1] : commandResult.getFailureCause().getMessage();
    }
    
    /**
     * Gets the GUIDs of the instances in the cluster that match the targets specified by the --targets option
     * @param targets The targets to match
     * @return A map of the target instance GUIDs and their respective InstanceDescriptors
     */
    private Map<String, InstanceDescriptor> getTargetInstanceDescriptors(String targets) {
        Map<String, InstanceDescriptor> targetInstanceDescriptors = new HashMap<>();
        
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
                                        targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
                                    }
                                } else {
                                    // Match on instance name only
                                    if (instance.getInstanceName().equalsIgnoreCase(targetInstance) && 
                                            instance.isMicroInstance()) {
                                        targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
                                        break;
                                    }
                                }   
                            } else if (targetInstance.equals(("*"))) {
                                // Match on group name only
                                if (instance.getInstanceGroup().equalsIgnoreCase(targetGroup) && 
                                        instance.isMicroInstance()) {
                                    targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
                                    break;
                                }   
                            } else {
                                // Match on group and instance name
                                if ((instance.getInstanceGroup().equalsIgnoreCase(targetGroup) && 
                                        instance.getInstanceName().equalsIgnoreCase(targetInstance)) && 
                                        instance.isMicroInstance()) {
                                    targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
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
                                targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
                            }
                        } else {
                            // Match on instance name
                            if (instance.getInstanceName().equalsIgnoreCase(target) && instance.isMicroInstance()) {
                                targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
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
                    targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
                }
            }
        }
        
        return targetInstanceDescriptors;
    }
    
    /**
     * Gets the GUIDs of the instances in the cluster that match the targets specified by the --explicitTarget option
     * @param explicitTargets The explicit targets
     * @return A map of the target instance GUIDs and their respective InstanceDescriptors
     */
    private Map<String, InstanceDescriptor> getExplicitTargetInstanceDescriptors(String[] explicitTargets) {
        Map<String, InstanceDescriptor> targetInstanceDescriptors = new HashMap<>();
        
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
                        targetInstanceDescriptors.put(instance.getMemberUUID(), instance);
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
        
        return targetInstanceDescriptors;
    }
    
    /**
     * Helper method to parse the parameters into a usable format
     * @param parameters The parameters to parse
     * @return A String array containing the parsed parameters in a usable format
     */
    private String[] parseParameters(String[] parameters) {
        String primaryParameter = "";
        List<String> parsedParameters = new ArrayList<>();
        
        // The admin console sends the parameters as one space separated string, so split it if necessary
        if (parameters.length == 1 && parameters[0].contains(" ")) {
            parameters = parameters[0].split(" ");
        }
        
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
