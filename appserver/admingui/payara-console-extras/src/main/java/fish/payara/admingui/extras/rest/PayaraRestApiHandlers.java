/* 
 *     Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.admingui.extras.rest;

import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            if((List)payaraEndpointsExtraProps.get("endpointList") != null) {
                result = (List<Map<String, String>>)payaraEndpointsExtraProps.get("endpointList");
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
     * Gets a map of components and if they are a jersey application
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
                if((List)payaraEndpointsExtraProps.get("endpointList") != null) {
                    result.put(componentName, true);
                     // Change the component type to JAX-RS. Couldn't be obtained at an earlier point since JAX-RS resources are compiled to JSP.
                    rowMap.put("type", "JAX-RS");
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
     * Gets a map of components with CDI dev mode status
     * @param handlerCtx 
     */
    @Handler(id = "py.isCDIDevMode",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true),
            @HandlerInput(name = "rowList", type = java.util.List.class, required = true)},
        output = {
            @HandlerOutput(name = "result", type = java.util.Map.class)})
    public static void isCDIDevMode(HandlerContext handlerCtx) {
        Map result = new HashMap();
        try{
            String appName = (String) handlerCtx.getInputValue("appName");
            String encodedAppName = URLEncoder.encode(appName, "UTF-8");
            List rowList = (List) handlerCtx.getInputValue("rowList");
            for(Object row : rowList) {
                Map rowMap = (Map) row;
                String componentName = (String) rowMap.get("name");
                String encodedComponentName = URLEncoder.encode(componentName, "UTF-8");
                result.put(componentName, false);
                if(!((String)rowMap.get("sniffers")).contains("weld")){
                    continue;
                }
                
                String endpoint = GuiUtil.getSessionValue("REST_URL") + "/applications/application/" + encodedAppName + "/property";
                Map attrMap = Collections.singletonMap("componentname", encodedComponentName);
                Map payaraEndpointDataMap = RestUtil.restRequest(endpoint, attrMap, "GET", null, true, false);
                Map payaraEndpointsExtraProps = (Map) ((Map) ((Map) payaraEndpointDataMap.get("data")).get("extraProperties"));
                List<Map> properties = (List<Map>)payaraEndpointsExtraProps.get("properties");
                for (Map property : properties) {
                    if (ServerTags.CDI_DEV_MODE_ENABLED_PROP.equals(property.get("name"))
                            && Boolean.parseBoolean((String) property.get("value"))) {
                        result.put(componentName, true);
                    }
                }
            }
          }catch(Exception ex){
            GuiUtil.getLogger().log(Level.INFO, "{0}{1}", new Object[]{GuiUtil.getCommonMessage("log.error.isCDIDevMode"), ex.getLocalizedMessage()});
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
    
    @Handler(id="py.sortRequestTracingEnabledNotifierStatus",
    	input={
            @HandlerInput(name="specifiedNotifiers", type=String.class, required=true),
            @HandlerInput(name="avaliableNotifiers", type=List.class, required=true )},
        output={
            @HandlerOutput(name="enabled", type=List.class),
            @HandlerOutput(name="disabled", type=List.class)})
    public static void sortRequestTracingEnabledNotifierStatus(HandlerContext handlerctx){
        List<String> enabled = new ArrayList<String>();
        List<String> disabled = new ArrayList<String>();
        
        List<String> avaliable = (List) handlerctx.getInputValue("avaliableNotifiers");
        
        String notifiersString = (String) handlerctx.getInputValue("specifiedNotifiers");
        notifiersString = notifiersString.substring(1, notifiersString.length() - 2);
        String[] notifiers = notifiersString.split("\\}\\,");
        for (String notifier : notifiers){
            String name = notifier.split("notifierName=", 2)[1];
            if (notifier.contains("notifierEnabled=true")){
                enabled.add(name);
            } else {
                disabled.add(name);
            }
            avaliable.remove(name);
        }
        for (String unused : avaliable){
            disabled.add(unused);
        }        
        handlerctx.setOutputValue("disabled", disabled);
        handlerctx.setOutputValue("enabled", enabled);
                
    }
    
        @Handler(id="py.sortHealthcheckEnabledNotifierStatus",
    	input={
            @HandlerInput(name="specifiedNotifiers", type=String.class, required=true),
            @HandlerInput(name="avaliableNotifiers", type=List.class, required=true )},
        output={
            @HandlerOutput(name="enabled", type=List.class),
            @HandlerOutput(name="disabled", type=List.class)})
    public static void sortHealthcheckEnabledNotifierStatus(HandlerContext handlerctx){
        List<String> enabled = new ArrayList<String>();
        List<String> disabled = new ArrayList<String>();
        
        List<String> avaliable = (List) handlerctx.getInputValue("avaliableNotifiers");
        
        String notifiersString = (String) handlerctx.getInputValue("specifiedNotifiers");
        notifiersString = notifiersString.substring(1, notifiersString.length() - 2);
        
        String[] notifiers = notifiersString.split("[\\}\\]]\\,");
        for (String notifier : notifiers){
            //Check to see if this is actually a notifier
            notifier = notifier.trim();
            if (!notifier.startsWith("notifierList")){
                continue;
            }
            
            String name = "service-" + notifier.split("notifierName=", 2)[1].toLowerCase();
            if (notifier.contains("notifierEnabled=true")){
                enabled.add(name);
            } else {
                disabled.add(name);
            }
            avaliable.remove(name);
        }
        for (String unused : avaliable){
            disabled.add(unused);
        }        
        handlerctx.setOutputValue("disabled", disabled);
        handlerctx.setOutputValue("enabled", enabled);
                
    }
    
    /**
     * Updates the request tracing notifiers to be enabled or disabled
     * @param handlerCtx 
     */
    @Handler(id="py.updateNotifiers",
            input={
                @HandlerInput(name="endpoint", type=String.class, required=true),
                @HandlerInput(name="selected", type=String[].class, required=true),
                @HandlerInput(name="notifiers", type=String[].class, required=true),
                @HandlerInput(name="dynamic", type=Boolean.class, required=true),
                @HandlerInput(name="quiet", type=boolean.class, defaultValue="false"),
                @HandlerInput(name="throwException", type=boolean.class, defaultValue="true")
            })
    public static void updateNotifiers(HandlerContext handlerCtx) {
        String[] notifiers = (String[]) handlerCtx.getInputValue("notifiers");
        String[] enabled = (String[]) handlerCtx.getInputValue("selected");
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        Boolean dynamic = (Boolean) handlerCtx.getInputValue("dynamic");
        Boolean quiet = (Boolean) handlerCtx.getInputValue("quiet");
        Boolean throwException = (Boolean) handlerCtx.getInputValue("throwException");
        List<String> enabledNotifiers = Arrays.asList(enabled);

        boolean forRequestTracing = false;
        boolean forHealthCheck = false;
        boolean forMonitoring = false;

        for (String notifier : notifiers){
            String name = notifier.split("-")[1];
            String restEndpoint;
            if (endpoint.contains("request-tracing-service-configuration")){
                restEndpoint = endpoint + "/requesttracing-" + name + "-notifier-configure";
                forRequestTracing = true;
            } else if (endpoint.contains("health-check-service-configuration")){
                restEndpoint = endpoint + "/healthcheck-" + name + "-notifier-configure";
                forHealthCheck = true;
            } else if (endpoint.contains("monitoring-service-configuration")){
                restEndpoint = endpoint + "/monitoring-" + name + "-notifier-configure";
                forMonitoring = true;
            } else {
                //Unknown service being configured
                throw new UnknownConfigurationException();
            }
            
            HashMap<String, Object> attrs = new HashMap<>();
            if (enabledNotifiers.contains(notifier)){
                attrs.put("enabled", "true");                
            } else {
                attrs.put("enabled", "false");
            }
            //PAYARA-1616 go silent, bootstrap will take place after iteration.
            attrs.put("dynamic", "false");
            RestUtil.restRequest(restEndpoint, attrs, "post", handlerCtx, quiet, throwException);
        }
        // PAYARA-1616
        // manually bootstrap healthCheck and requestTracing services for once so that it doesn't get bootstrapped each time for enabled notifier.
        if (dynamic){
            if (forRequestTracing) {
                String restEndpoint = endpoint + "/bootstrap-requesttracing";
                RestUtil.restRequest(restEndpoint, null, "post", handlerCtx, quiet, throwException);
            }
            if (forHealthCheck) {
                String restEndpoint = endpoint +  "/bootstrap-healthcheck";
                RestUtil.restRequest(restEndpoint, null, "post", handlerCtx, quiet, throwException);
            }
            if (forMonitoring){
                String restEndpoint = endpoint + "/bootstrap-monitoring";
                RestUtil.restRequest(restEndpoint, null, "post", handlerCtx, quiet, throwException);
            }
        }
    }
  
    @Handler(id = "py.getHistoricHealthcheckMessages",
            input = @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
            output = @HandlerOutput(name = "result", type = java.util.List.class))
    public static void getHistoricHealthcheckMessages(HandlerContext handlerCtx){
        String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
        String endpoint;

        // Check for trailing slashes
        endpoint = parentEndpoint.endsWith("/") ? parentEndpoint + "list-historic-healthchecks" : parentEndpoint 
                + "/" + "list-historic-healthchecks";
        
        Map responseMap = RestUtil.restRequest(endpoint, null, "GET", handlerCtx, false, false);
        Map data = (Map) responseMap.get("data");
         
        // Extract the information from the Map and place it in a List for representation in the dataTable
        List<Map> messages = new ArrayList<>();
        if (data != null) {
            Map extraProperties = (Map) data.get("extraProperties");
            if (extraProperties != null) {
                messages = (List<Map>) extraProperties.get("historicmessages");
                if (messages == null) {
                    // Re-initialise to empty if members is not found
                    messages = new ArrayList<>();
                }
            }
        }
        handlerCtx.setOutputValue("result", messages);
    }

    @Handler(id = "py.getHistoricRequestTracingMessages",
            input = @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
            output = @HandlerOutput(name = "result", type = java.util.List.class))
    public static void getHistoricRequestTracingMessages(HandlerContext handlerCtx){

        String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
        String endpoint;

        // Check for trailing slashes
        endpoint = parentEndpoint.endsWith("/") ? parentEndpoint + "list-historic-requesttraces" : parentEndpoint
                + "/" + "list-historic-requesttraces";

        Map responseMap = RestUtil.restRequest(endpoint, null, "GET", handlerCtx, false, false);
        Map data = (Map) responseMap.get("data");

        // Extract the information from the Map and place it in a List for representation in the dataTable
        List<Map> messages = new ArrayList<>();
        if (data != null) {
            Map extraProperties = (Map) data.get("extraProperties");
            if (extraProperties != null) {
                messages = (List<Map>) extraProperties.get("historicmessages");
                if (messages != null)
                if (messages == null) {
                    // Re-initialise to empty if members is not found
                    messages = new ArrayList<>();
                }
            }
        }
        handlerCtx.setOutputValue("result", messages);
    }

    /**
     * Gets the context roots of all deployed applications on the domain.
     * This is outputted as List<Map<String, String>> for usage within the Virtual-Servers page.
     * @param handlerCtx 
     */
    @Handler(id = "py.getVirtualServersAttributes",
            input = {
                @HandlerInput(name = "configName", type = String.class, required = true),
                @HandlerInput(name = "sessionScopeRestURL", type = String.class, required = true),
                @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                @HandlerInput(name = "childType", type = String.class, required = true),
                @HandlerInput(name = "skipList", type = List.class, required = false),
                @HandlerInput(name = "includeList", type = List.class, required = false),
                @HandlerInput(name = "id", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "result", type = java.util.List.class)
    })
    public static void getVirtualServersAttributes(HandlerContext handlerCtx) {
        String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
        String childType = (String) handlerCtx.getInputValue("childType");
        String configName = (String) handlerCtx.getInputValue("configName");
        String sessionScopeRestURL = (String) handlerCtx.getInputValue("sessionScopeRestURL");
        sessionScopeRestURL = sessionScopeRestURL.endsWith("/") ? sessionScopeRestURL : sessionScopeRestURL + "/";
        String serverName = "";
       
        try {
            List<Map> table = RestUtil.buildChildEntityList(
                    (String)handlerCtx.getInputValue("parentEndpoint"),
                    (String)handlerCtx.getInputValue("childType"),
                    (List)handlerCtx.getInputValue("skipList"),
                    (List)handlerCtx.getInputValue("includeList"),
                    (String)handlerCtx.getInputValue("id"));
            
            if (configName.equals("default-config")) {
                for (Map row : table) {
                    row.put("contextRoot", "");
                }
            } else {
                List<String> instances = RestUtil.getChildList(sessionScopeRestURL + "servers/server");
                for (String instance : instances) {
                    String configRef = (String) RestUtil.getAttributesMap(instance).get("configRef");
                    if (configRef.equals(configName)) {
                        serverName = instance.substring(instance.lastIndexOf("/") + 1);
                    }
                }
                String deployedApplicationsEndpoint = sessionScopeRestURL + "servers/server/" + serverName 
                        + "/application-ref";

                List<String> deployedApplications = RestUtil.getChildList(deployedApplicationsEndpoint);
                List<String> virtualServers = RestUtil.getChildList(parentEndpoint + "/" + childType);
                List<String> applications = RestUtil.getChildList(sessionScopeRestURL + "applications/application");

                for (String virtualServer : virtualServers) {         
                    String virtualServerName = virtualServer.substring(virtualServer.lastIndexOf("/") + 1);

                    for (int i = 0; i < deployedApplications.size(); i++) {
                        deployedApplications.set(i, deployedApplications.get(i)
                                .substring(deployedApplications.get(i).lastIndexOf("/") + 1));
                    }

                    String contextRoots = "";

                    for (String application : applications) {
                        String applicationName = application.substring(application.lastIndexOf("/") + 1);
                        String[] deployedVirtualServers; 
                        if (RestUtil.get(deployedApplicationsEndpoint + "/" + applicationName).isSuccess()) {
                            String deployedVirtualServersString = ((String) RestUtil.getAttributesMap(
                                    deployedApplicationsEndpoint + "/" + applicationName).get("virtualServers"));
			    if (deployedVirtualServersString != null) {
			    	deployedVirtualServers = deployedVirtualServersString.split(",");
                                for (String deployedVirtualServer : deployedVirtualServers) {
    	                            if (!deployedVirtualServer.equals("") && deployedApplications.contains(applicationName) 
        	                            && virtualServerName.equals(deployedVirtualServer)) {
                	                if (!contextRoots.equals("")) {
                        	            contextRoots += "<br>" + RestUtil.getAttributesMap(application).get("contextRoot");
                                	} else {
                                            contextRoots += RestUtil.getAttributesMap(application).get("contextRoot");
	                                }
                                    }
                            	}
			    }
                        }
                    }

                    for (Map row : table) {
                        if (row.get("name").equals(virtualServerName)) {
                            row.put("contextRoot", contextRoots);
                        }
                    }
                }
            }
            handlerCtx.setOutputValue("result", table);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
}
