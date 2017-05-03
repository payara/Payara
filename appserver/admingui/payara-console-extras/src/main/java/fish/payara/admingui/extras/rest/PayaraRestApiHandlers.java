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
package fish.payara.admingui.extras.rest;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;

/**
 * A class containing Payara specific handler methods for the REST API
 * @author Andrew Pielage
 */
public class PayaraRestApiHandlers
{
    /**
     * Gets information about the instances current registered to the Hazelcast cluster.
     * @param handlerCtx 
     */
    @Handler(id = "py.getHazelcastClusterMembers",
            input = {
                @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                @HandlerInput(name = "type", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = java.util.List.class)
    })
    public static void getHazelcastClusterMembers(HandlerContext handlerCtx) {
        String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
        String type = (String) handlerCtx.getInputValue("type");
        String endpoint;
        
        // Check for trailing slashes
        endpoint = parentEndpoint.endsWith("/") ? parentEndpoint + "list-hazelcast-cluster-members" : parentEndpoint 
                + "/" + "list-hazelcast-cluster-members";
        
        try {       
            // Set a target type if one has been provided
            if (type != null) {
                if (type.equals("micro")) {
                    endpoint += "?type=micro";
                } else if (type.equals("server")) {
                    endpoint += "?type=server";
                }
            }
                
            // Run the list-hazelcast-cluster-members command using the Rest endpoint
            Map responseMap = RestUtil.restRequest(endpoint, null, "GET", handlerCtx, false, true);
            Map data = (Map) responseMap.get("data");
           
            // Extract the information from the Map and place it in a List for representation in the dataTable
            List<Map> instances = new ArrayList<>();
            if (data != null) {
                Map extraProperties = (Map) data.get("extraProperties");
                if (extraProperties != null) {
                    try {
                        instances = (List<Map>) extraProperties.get("members");
                        if (instances == null) {
                            // Re-initialise to empty if members is not found
                            instances = new ArrayList<>();
                        } else {
                            for (Map instance : instances) {
                                instance.put("selected", false);
                            }
                        }
                    } catch (ClassCastException ex) {
                        // This exception should only be caught if Hazelcast is not enabled, as the command returns a 
                        // String instead of a List. In such a case, re-initialise to an empty List
                        instances = new ArrayList<>();
                    } 
                }
            }
            
            handlerCtx.setOutputValue("result", instances);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
    
    /**
     * Sends the asadmin command with the parameters provided to the instances selected in the table
     * @param handlerCtx 
     */
    @Handler(id = "py.sendAsadminCommandToSelectedInstances", input = {
                @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                @HandlerInput(name = "rows", type = List.class, required = true),
                @HandlerInput(name = "command", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "response", type = Map.class)
            })
    public static void sendAsadminCommandToSelectedInstances(HandlerContext handlerCtx) {
        String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
        String endpoint = parentEndpoint.endsWith("/") ? parentEndpoint + "send-asadmin-command" : parentEndpoint 
                + "/" + "send-asadmin-command";
        List<HashMap> rows = (List<HashMap>) handlerCtx.getInputValue("rows");
        String command = (String) handlerCtx.getInputValue("command");
        
        // Check that the text box isn't empty
        if (command != null) {
            // Get the selected rows
            List<HashMap> selectedRows = new ArrayList<>();
            for (HashMap row : rows) {
                try {
                    boolean selected = (boolean) row.get("selected");
                    if (selected) {
                        selectedRows.add(row);
                    }
                } catch (ClassCastException ex) {
                    // Ignore and move on
                }
            }

            // Split the command and parameters
            String[] splitCommand = command.split(" ");
            command = splitCommand[0];

            // Convert the parameters into a space-separated string
            String parameters = "";
            for (int i = 1; i < splitCommand.length; i++) {
                parameters += splitCommand[i] + " ";
            }

            // Remove any trailing spaces
            parameters = parameters.trim();

            // Get the parameters from each row, and send the asadmin command
            for (Map row : selectedRows) {
                String instanceName = (String) row.get("instanceName");
                String hazelcastPort = Long.toString((Long) row.get("hazelcastPort"));
                String hostName = (String) row.get("hostName");
                String ipAddress = hostName.split("/")[1];

                Map attrsMap = new HashMap();
                attrsMap.put("explicitTarget", ipAddress + ":" + hazelcastPort + ":" + instanceName);
                attrsMap.put("command", command);
                attrsMap.put("id", parameters);
                attrsMap.put("logOutput", "true");

                try{
                    Map response = RestUtil.restRequest(endpoint, attrsMap, "POST", handlerCtx, false, false);
                    handlerCtx.setOutputValue("response", response);
                } catch (Exception ex) {
                    GuiUtil.handleException(handlerCtx, ex);
                }
            }
        }
    }
    
    /**
     * Gets the REST endpoints from a given app name and optional component name
     * @param handlerCtx 
     */
    @Handler(id = "py.getRestEndpoints",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true),
            @HandlerInput(name = "componentName", type = String.class, required = false)},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)})
    public static void getRestEndpoints(HandlerContext handlerCtx) {
        List result = new ArrayList();
        try{
            String appName = (String) handlerCtx.getInputValue("appName");
            String encodedAppName = URLEncoder.encode(appName, "UTF-8");
            String componentName = (String) handlerCtx.getInputValue("componentName");
            String encodedComponentName = URLEncoder.encode(componentName, "UTF-8");
            String prefix = GuiUtil.getSessionValue("REST_URL") + "/applications/application/" + encodedAppName;
            
            // get the extra properties from the list-rest-endpoints command, passing in the component name
            Map attrMap = new HashMap();
            attrMap.put("componentname", encodedComponentName);
            Map payaraEndpointDataMap = RestUtil.restRequest(prefix + "/list-rest-endpoints", attrMap, "GET", null, false, false);
            Map payaraEndpointsExtraProps = (Map) ((Map) ((Map) payaraEndpointDataMap.get("data")).get("extraProperties"));

            // Check if the command returned any endpoints
            if((Map)payaraEndpointsExtraProps.get("endpointMap") != null) {
                Map<String, String> endpointMap = (Map)payaraEndpointsExtraProps.get("endpointMap");
                for(String key : endpointMap.keySet()) {
                    // Count through returned endpoints
                    Map<String, String> rowMap = new HashMap<>();
                    rowMap.put("endpointName", key);
                    rowMap.put("requestMethod", endpointMap.get(key));
                    result.add(rowMap);
                }
            }
          }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getRestEndpoints") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
          }
          handlerCtx.setOutputValue("result", result);
    }
    
    /**
     * Gets a Map of components and their REST endpoints from a given sub component list
     * @param handlerCtx 
     */
    @Handler(id = "py.hasRestEndpoints",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true),
            @HandlerInput(name = "rowList", type = java.util.List.class, required = true)},
        output = {
            @HandlerOutput(name = "result", type = java.util.Map.class)})
    public static void hasRestEndpoints(HandlerContext handlerCtx) {
        Map result = new HashMap();
        try{
            String appName = (String) handlerCtx.getInputValue("appName");
            String encodedAppName = URLEncoder.encode(appName, "UTF-8");
            List rowList = (List) handlerCtx.getInputValue("rowList");
            for(Object row : rowList) {
                Map rowMap = (Map) row;
                
                String componentName = (String) rowMap.get("name");
                String encodedComponentName = URLEncoder.encode(componentName, "UTF-8");
                String prefix = GuiUtil.getSessionValue("REST_URL") + "/applications/application/" + encodedAppName;

                // Get the result of the list-rest-endpoints command and get it's extra properties
                Map attrMap = new HashMap();
                attrMap.put("componentname", encodedComponentName);
                Map payaraEndpointDataMap = RestUtil.restRequest(prefix + "/list-rest-endpoints", attrMap, "GET", null, true, false);
                Map payaraEndpointsExtraProps = (Map) ((Map) ((Map) payaraEndpointDataMap.get("data")).get("extraProperties"));

                // Enter into the map the key of the component and whether it has endpoints or not
                result.put(componentName, false);
                if((Map)payaraEndpointsExtraProps.get("endpointMap") != null) {
                    result.put(componentName, true);
                }
            }
          }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.hasRestEndpoints") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
          }
          handlerCtx.setOutputValue("result", result);
    }
    
    /**
     * Sets the successful command message to be displayed
     * @param handlerCtx 
     */
    @Handler(id="py.prepareSuccessfulCommandMsg")
    public static void prepareSuccessfulCommandMsg(HandlerContext handlerCtx){
        GuiUtil.prepareAlert("success", "Command sent successfully", null);
    }
}
