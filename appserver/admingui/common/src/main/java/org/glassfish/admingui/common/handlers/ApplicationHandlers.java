/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * ApplicationHandlers.java
 *
 * Created on August 1, 2010, 2:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLEncoder;
import java.net.InetAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.net.URLDecoder;


import java.util.TreeSet;
import java.util.Set;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.DeployUtil;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.admingui.common.util.TargetUtil;
import org.glassfish.admingui.common.util.AppUtil;


public class ApplicationHandlers {

    /**
     *	<p> This handler returns the list of applications for populating the table.
     *  <p> Input  value: "serverName" -- Type: <code> java.lang.String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id = "gf.getDeployedAppsInfo",
        input = {
            @HandlerInput(name = "appPropsMap", type = Map.class, required=true),
            @HandlerInput(name = "filterValue", type = String.class)},
        output = {
            @HandlerOutput(name = "filters", type = java.util.List.class),
            @HandlerOutput(name = "result", type = java.util.List.class)})

    public static void getDeployedAppsInfo(HandlerContext handlerCtx) {
        Map<String, String> appPropsMap = (Map) handlerCtx.getInputValue("appPropsMap");
        String filterValue = (String) handlerCtx.getInputValue("filterValue");
        Set filters = new TreeSet();
        filters.add("");
        if (GuiUtil.isEmpty(filterValue)) {
            filterValue = null;
        }
        List result = new ArrayList();
        if (appPropsMap == null){
            handlerCtx.setOutputValue("result", result);
            return;
        }
        List<String> keys = new ArrayList(appPropsMap.keySet());
        Collections.sort(keys);

        String prefix = GuiUtil.getSessionValue("REST_URL") + "/applications/application/";
        for(String oneAppName : keys){
          try{
            String engines = appPropsMap.get(oneAppName);
            HashMap oneRow = new HashMap();
            oneRow.put("name", oneAppName);
            final String encodedName = URLEncoder.encode(oneAppName, "UTF-8");
            oneRow.put("encodedName", encodedName);
            oneRow.put("selected", false);
            oneRow.put("enableURL", DeployUtil.getTargetEnableInfo(oneAppName, true, true));
            oneRow.put("sniffers", engines);
            oneRow.put("deploymentOrder", RestUtil.getAttributesMap(prefix+encodedName).get("deploymentOrder"));

            List sniffersList = GuiUtil.parseStringList(engines, ",");
            oneRow.put("sniffersList", sniffersList);
            for(int ix=0; ix< sniffersList.size(); ix++)
                filters.add(sniffersList.get(ix));
            if (filterValue != null){
                if (! sniffersList.contains(filterValue))
                    continue;
            }

            getLaunchInfo(oneAppName, null, oneRow);

            result.add(oneRow);
          }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getDeployedAppsInfo") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
          }
        }
        handlerCtx.setOutputValue("result", result);
        handlerCtx.setOutputValue("filters", new ArrayList(filters));
    }


    @Handler(id = "gf.getSubComponents",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true),
            @HandlerInput(name = "moduleList", type = List.class, required = true)},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)})
    public static void getSubComponents(HandlerContext handlerCtx) {
        List result = new ArrayList();
        try{
            String appName = (String) handlerCtx.getInputValue("appName");
            String encodedAppName = URLEncoder.encode(appName, "UTF-8");
            List<String> modules = (List) handlerCtx.getInputValue("moduleList");
            for(String oneModule: modules){
                String encodedModuleName = URLEncoder.encode(oneModule, "UTF-8");
                Map oneRow = new HashMap();
                List<String> snifferList = AppUtil.getSnifferListOfModule(encodedAppName, encodedModuleName);
                String moduleName = oneModule;
                oneRow.put("moduleName", moduleName);
                oneRow.put("name", " ----------- ");
                oneRow.put("type", " ----------- ");
                oneRow.put("hasEndpoint", false);
                oneRow.put("hasLaunch", false);
                oneRow.put("hasAppClientLaunch", false);
                oneRow.put("hasAppClientStub", false);
                oneRow.put("sniffers", snifferList.toString());

                if (snifferList.contains("web")) {
                    String endpoint =  GuiUtil.getSessionValue("REST_URL") + "/applications/application/"+ encodedAppName +"/get-context-root.xml?" +
                            "modulename=" + encodedModuleName;
                    Map map = (Map)RestUtil.restRequest(endpoint, null, "GET", null, false).get("data");
                    Map props = (Map)map.get("properties");
                    String contextRoot = (String) props.get("contextRoot");
                    getLaunchInfo(appName, contextRoot, oneRow);
                }

                //JWS is disabled only if the property is present and is set to false.   Otherwise, its enabled.
                if (snifferList.contains("appclient")){
                    String jwEnabled = RestUtil.getPropValue(GuiUtil.getSessionValue("REST_URL") + "/applications/application/"+encodedAppName, "java-web-start-enabled",  handlerCtx);
                    if (GuiUtil.isEmpty(jwEnabled) || jwEnabled.equals("true") ){
                        List<String> targetList = DeployUtil.getApplicationTarget(appName, "application-ref");
                        oneRow.put("hasAppClientLaunch", (targetList.isEmpty())? false: true);
                    }
                    oneRow.put("hasAppClientStub", true);
                }
                result.add(oneRow);
                getSubComponentDetail(appName, moduleName, snifferList, result);
            }
          }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getSubComponents") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
          }
          handlerCtx.setOutputValue("result", result);
    }



    private static List<Map> getSubComponentDetail(String appName, String moduleName, List<String> snifferList, List<Map> result){
        try{
            String encodedAppName = URLEncoder.encode(appName, "UTF-8");
            String encodedModuleName = URLEncoder.encode(moduleName, "UTF-8");
            Map wsAppMap = null;
            if (snifferList.contains("webservices")){
                wsAppMap = AppUtil.getWsEndpointMap(appName, moduleName, snifferList);
            }
            Map attrMap = new HashMap();
            attrMap.put("appname", encodedAppName);
            attrMap.put("id", encodedModuleName);
            String prefix = GuiUtil.getSessionValue("REST_URL") + "/applications/application/" + encodedAppName;
            Map subMap = RestUtil.restRequest(prefix + "/list-sub-components", attrMap, "GET", null, false);
            Map data = (Map)subMap.get("data");
            if(data != null){
                Map<String, Object> props = (Map) data.get("properties");
                if (props == null){
                    return result;
                }
                for(Map.Entry<String,Object> e : props.entrySet()){
                    Map oneRow = new HashMap();
                    oneRow.put("moduleName", moduleName);
                    oneRow.put("name", e.getKey());
                    oneRow.put("type", e.getValue());
                    oneRow.put("hasLaunch", false);
                    oneRow.put("sniffers", "");
                    oneRow.put("hasEndpoint", false);
                    oneRow.put("hasAppClientLaunch", false);
                    oneRow.put("hasAppClientStub", false);
                    if (wsAppMap != null){
                        if (! (AppUtil.getEndpointDetails( wsAppMap, moduleName, e.getKey()) == null)){
                            oneRow.put("hasEndpoint", true );
                        }
                    }
                    result.add(oneRow);
                }
            }
        }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getSubComponentDetail") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return result;
    }

    @Handler(id = "gf.addToAppScopedResourcesTable",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true),
            @HandlerInput(name = "resources", type = Map.class),
            @HandlerInput(name = "moduleName", type = String.class),
            @HandlerInput(name = "listOfRows", type = java.util.List.class)},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)})
    public static void addToAppScopedResourcesTable(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");
        String moduleName = (String) handlerCtx.getInputValue("moduleName");
        Map<String, String> resources = (Map<String, String>) handlerCtx.getInputValue("resources");
        List<Map<String, String>> result = (List<Map<String, String>>) handlerCtx.getInputValue("listOfRows");
        if (result == null) {
            result = new ArrayList<Map<String, String>>();
        }
        if (GuiUtil.isEmpty(moduleName)) {
            moduleName = "-----------";
        }
        if( resources != null )
        for (Map.Entry<String, String> e : resources.entrySet())  {
            String resource = e.getKey();
            String value = e.getValue();
            Map oneRow = new HashMap();
            oneRow.put("appName", appName);
            oneRow.put("moduleName", moduleName);
            oneRow.put("resName", resource);
            oneRow.put("resType", AppUtil.getAppScopedResType(value, "display"));
            String link = AppUtil.getAppScopedResType(value, "edit") + resource + "&appName=" + appName;
            if (!moduleName.equals("-----------")) {
                link = link + "&moduleName=" + moduleName;
            }
            oneRow.put("link", link);
            result.add(oneRow);
        }
        handlerCtx.setOutputValue("result", result);
    }

    @Handler(id = "gf.appScopedResourcesExist",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true)
            },
        output = {
            @HandlerOutput(name = "appScopedResExists", type = java.lang.Boolean.class)})
    public static void appScopedResourcesExist(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");        
        handlerCtx.setOutputValue("appScopedResExists", AppUtil.doesAppContainsResources(appName));
    }

    @Handler(id = "gf.getLifecyclesInfo",
        input = {
            @HandlerInput(name = "children", type = List.class, required=true)},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)})

    public static void getLifecyclesInfo(HandlerContext handlerCtx) {
        List<Map> children = (List) handlerCtx.getInputValue("children");
        List result = new ArrayList();
        String prefix =  GuiUtil.getSessionValue("REST_URL") + "/applications/application/";
        if (children == null){
            handlerCtx.setOutputValue("result", result);
            return;
        }
        for(Map oneChild : children){
            Map oneRow = new HashMap();
            try{
                String name = (String) oneChild.get("message");
                String encodedName = URLEncoder.encode(name, "UTF-8");
                oneRow.put("name", name);
                oneRow.put("encodedName", encodedName);
                oneRow.put("selected", false);
                oneRow.put("loadOrder", RestUtil.getPropValue(prefix+encodedName, "load-order", handlerCtx));
                oneRow.put("enableURL", DeployUtil.getTargetEnableInfo(name, true, true));
                result.add(oneRow);
            }catch(Exception ex){
                GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getLifecyclesInfo") + ex.getLocalizedMessage());
                if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                    ex.printStackTrace();
                }
            }
        }
        handlerCtx.setOutputValue("result", result);
    }



    @Handler(id = "gf.deleteLifecycle",
        input = {
            @HandlerInput(name = "selectedList", type = List.class, required=true)})

    public static void deleteLifecycle(HandlerContext handlerCtx) {
        List<Map> selectedList = (List) handlerCtx.getInputValue("selectedList");
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/applications/application/delete-lifecycle-module" ;
        Map attrs = new HashMap();
        try{
            for(Map oneRow: selectedList){
                String name = (String) oneRow.get("name");
                String encodedName = URLEncoder.encode(name, "UTF-8");
                attrs.put("id", encodedName);
                //delete all application-ref first
                List<Map> appRefs = DeployUtil.getRefEndpoints(name, "application-ref");
                for(Map  oneRef:  appRefs){
                    attrs.put("target", oneRef.get("targetName"));
                    RestUtil.restRequest((String)oneRef.get("endpoint"), attrs, "DELETE", null, false);
                }
                attrs.put("target", "domain");
                RestUtil.restRequest( endpoint, attrs, "POST", handlerCtx, false);
            }
        }catch(Exception ex){
            GuiUtil.prepareException(handlerCtx, ex);
        }
    }

    private static void getLaunchInfo(String appName, String contextRoot, Map oneRow) {
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/applications/application/" + appName + ".json";
        Map map = RestUtil.restRequest(endpoint, null, "GET", null, false);
        Map data = (Map)map.get("data");
        boolean enabled = false;
        if (data != null) {
            Map extraProperties = (Map)data.get("extraProperties");
            if (extraProperties != null) {
                Map entity = (Map)extraProperties.get("entity");
                if (entity != null) {
                    if (contextRoot == null)
                        contextRoot = (String) entity.get("contextRoot");
                    enabled = Boolean.parseBoolean((String) entity.get("enabled"));
                }
            }
        }
        oneRow.put("contextRoot", (contextRoot==null)? "" : contextRoot);
        oneRow.put("hasLaunch", false);

        if ( !enabled || GuiUtil.isEmpty(contextRoot)){
            return ;
        }

        List<String> targetList = DeployUtil.getApplicationTarget(appName, "application-ref");
        for(String target : targetList) {
            String virtualServers = getVirtualServers(target, appName);
            String ep = TargetUtil.getTargetEndpoint(target) + "/application-ref/" + appName;
            enabled = Boolean.parseBoolean((String)RestUtil.getAttributesMap(ep).get("enabled"));
            if (!enabled)
                continue;
            if (virtualServers != null && virtualServers.length() > 0) {
                oneRow.put("hasLaunch", true);
            }
        }
    }

    private static String getVirtualServers(String target, String appName) {
        List clusters = TargetUtil.getClusters();
        List standalone = TargetUtil.getStandaloneInstances();
        standalone.add("server");
        String ep = (String)GuiUtil.getSessionValue("REST_URL");
        if (clusters.contains(target)){
            ep = ep + "/clusters/cluster/" + target + "/application-ref/" + appName;
        }else{
            ep = ep + "/servers/server/" + target + "/application-ref/" + appName;
        }
        String virtualServers =
                (String)RestUtil.getAttributesMap(ep).get("virtualServers");
        return virtualServers;
    }


    @Handler(id = "gf.getTargetEndpoint",
        input = {
            @HandlerInput(name = "target", type = String.class, required = true)},
        output = {
            @HandlerOutput(name = "endpoint", type = String.class)})
    public static void getTargetEndpoint(HandlerContext handlerCtx) {
        handlerCtx.setOutputValue("endpoint", TargetUtil.getTargetEndpoint( (String) handlerCtx.getInputValue("target")));
    }


    //TODO:  whoever that calls gf.getConfigName() should call getTargetEndpoint and then grep the config in jsf.
    @Handler(id = "gf.getConfigName",
        input = {
            @HandlerInput(name = "target", type = String.class, required = true)},
        output = {
            @HandlerOutput(name = "configName", type = String.class)})
    public static void getConfigName(HandlerContext handlerCtx) {
        handlerCtx.setOutputValue("configName", 
                TargetUtil.getConfigName((String) handlerCtx.getInputValue("target")));
    }


    @Handler(id = "gf.getApplicationTarget",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true)},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)})
    public static void getApplicationTarget(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");
        handlerCtx.setOutputValue( "result", DeployUtil.getApplicationTarget(appName, "application-ref"));
    }


     @Handler(id = "gf.changeTargetStatus",
        input = {
            @HandlerInput(name = "selectedRows", type = List.class, required = true),
            @HandlerInput(name = "Enabled", type = String.class, required = true),
            @HandlerInput(name = "forLB", type = Boolean.class, required = true)})
    public static void changeTargetStatus(HandlerContext handlerCtx) {
        String Enabled = (String) handlerCtx.getInputValue("Enabled");
        List<Map>  selectedRows = (List) handlerCtx.getInputValue("selectedRows");
        boolean forLB = (Boolean) handlerCtx.getInputValue("forLB");
        for(Map oneRow : selectedRows){
            Map attrs = new HashMap();
            String endpoint = (String) oneRow.get("endpoint");
            if(forLB){
                attrs.put("lbEnabled", Enabled);
                RestUtil.restRequest(endpoint, attrs, "post", handlerCtx, false);
            }else{
                attrs.put("enabled", Enabled);
                RestUtil.restRequest(endpoint, attrs, "post", handlerCtx, false);
            }
        }
     }


     @Handler(id="gf.changeAppTargets",
        input={
        @HandlerInput(name="appName", type=String.class, required=true),
        @HandlerInput(name="targets", type=String[].class, required=true),
        @HandlerInput(name="status", type=String.class)})
    public static void changeAppTargets(HandlerContext handlerCtx) {
        String appName = (String)handlerCtx.getInputValue("appName");
        String status = (String)handlerCtx.getInputValue("status");
        String[] selTargets = (String[])handlerCtx.getInputValue("targets");
        List<String> selectedTargets = Arrays.asList(selTargets);

        List clusters = TargetUtil.getClusters();
        List standalone = TargetUtil.getStandaloneInstances();
        standalone.add("server");

        Map attrs = new HashMap();
        attrs.put("id", appName);
        List<String> associatedTargets = DeployUtil.getApplicationTarget(appName, "application-ref");
        for(String newTarget :  selectedTargets){
            if (associatedTargets.contains(newTarget)){
                //no need to add or remove.
                associatedTargets.remove(newTarget);
                continue;
            }else{
                AppUtil.manageAppTarget(appName, newTarget, true, status, clusters, standalone, handlerCtx);
            }
         }

         for(String oTarget :  associatedTargets){
            AppUtil.manageAppTarget(appName, oTarget, false, null, clusters, standalone, handlerCtx);
        }
    }

   @Handler(id = "gf.reloadApplication",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true)
        })
    public static void reloadApplication(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");
        List<String> targets = DeployUtil.getApplicationTarget(appName, "application-ref");
        if (DeployUtil.reloadApplication(appName, targets, handlerCtx)){
            GuiUtil.prepareAlert("success", GuiUtil.getMessage("org.glassfish.web.admingui.Strings", "restart.successPE"), null);
       }
    }

   @Handler(id = "gf.getTargetEnableInfo",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true),
            @HandlerInput(name = "isApp", type = Boolean.class)
        },
        output = {
            @HandlerOutput(name = "status", type = String.class)})
    public static void getTargetEnableInfo(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");
        Boolean isApp = (Boolean) handlerCtx.getInputValue("isApp");
        if(isApp == null) {
            isApp = true;
        }
        handlerCtx.setOutputValue("status", DeployUtil.getTargetEnableInfo(appName, false, isApp));
    }

   @Handler(id = "getVsForDeployment",
        input = {
            @HandlerInput(name = "targetConfig", type = String.class, defaultValue="server-config")
        },
        output = {
        @HandlerOutput(name = "result", type = List.class)})
    public static void getVsForDeployment(HandlerContext handlerCtx) {
       String targetConfig = (String) handlerCtx.getInputValue("targetConfig");
        String endpoint = GuiUtil.getSessionValue("REST_URL")+"/configs/config/"+targetConfig+"/http-service/virtual-server";
        List vsList = new ArrayList();
        try{
            vsList = new ArrayList(RestUtil.getChildMap(endpoint).keySet());
            vsList.remove("__asadmin");
       }catch(Exception ex){
           //TODO: error handling.
       }
        handlerCtx.setOutputValue("result", vsList);
   }


    @Handler(id = "gf.getTargetListInfo",
        input = {
            @HandlerInput(name = "appName", type = String.class, required = true)},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)})
    public static void getTargetListInfo(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");
        String prefix = (String) GuiUtil.getSessionValue("REST_URL");
        List clusters = TargetUtil.getClusters();
        List standalone = TargetUtil.getStandaloneInstances();
        standalone.add("server");
        List<String> targetList = DeployUtil.getApplicationTarget(appName, "application-ref");
        List result = new ArrayList();
        Map attrs = null;
        String endpoint="";
        for(String oneTarget : targetList){
            HashMap oneRow = new HashMap();
            if (clusters.contains(oneTarget)){
                endpoint = prefix + "/clusters/cluster/" + oneTarget + "/application-ref/" + appName;
                attrs = RestUtil.getAttributesMap(endpoint);
            }else{
                endpoint = prefix+"/servers/server/" + oneTarget + "/application-ref/" + appName;
                attrs = RestUtil.getAttributesMap(endpoint);
            }
            oneRow.put("name", appName);
            oneRow.put("selected", false);
            oneRow.put("endpoint", endpoint);
            oneRow.put("targetName", oneTarget);
            oneRow.put("enabled", attrs.get("enabled"));
            oneRow.put("lbEnabled", attrs.get("lbEnabled"));
            result.add(oneRow);
        }
        handlerCtx.setOutputValue("result", result);
    }

    /*
     * This handler is called for populating the application table in the cluster or instance Application tab.
     */
    @Handler(id = "gf.getSingleTargetAppsInfo",
        input = {
            @HandlerInput(name = "appPropsMap", type = Map.class, required=true),
            @HandlerInput(name = "appRefEndpoint", type = String.class, required=true),
            @HandlerInput(name = "target", type = String.class, required=true),
            @HandlerInput(name = "filterValue", type = String.class)},
        output = {
            @HandlerOutput(name = "filters", type = java.util.List.class),
            @HandlerOutput(name = "result", type = java.util.List.class)})

    public static void getSingleTargetAppsInfo(HandlerContext handlerCtx) {
        String appRefEndpoint = (String) handlerCtx.getInputValue("appRefEndpoint");
        String target = (String) handlerCtx.getInputValue("target");
        Map<String, String> appPropsMap = (Map) handlerCtx.getInputValue("appPropsMap");
        String filterValue = (String) handlerCtx.getInputValue("filterValue");
        Set filters = new TreeSet();
        filters.add("");
        if (GuiUtil.isEmpty(filterValue)) {
            filterValue = null;
        }
        List result = new ArrayList();
        String prefix = (String) GuiUtil.getSessionValue("REST_URL");
	if (appPropsMap != null) {
            for(Map.Entry<String,String> e : appPropsMap.entrySet()){
                try{
                    String engines = e.getValue();
                    HashMap oneRow = new HashMap();
                    oneRow.put("name", e.getKey());
                    String encodedName = URLEncoder.encode(e.getKey(), "UTF-8");
                    oneRow.put("targetName", target);
                    oneRow.put("selected", false);
                    String endpoint = prefix  + appRefEndpoint + encodedName;
                    oneRow.put("endpoint", endpoint);
                    Map appRefAttrsMap = RestUtil.getAttributesMap(endpoint);
                    String image = (appRefAttrsMap.get("enabled").equals("true")) ?  "/resource/images/enabled.png" : "/resource/images/disabled.png";
                    oneRow.put("enabled", image);
                    image = (appRefAttrsMap.get("lbEnabled").equals("true")) ?  "/resource/images/enabled.png" : "/resource/images/disabled.png";
                    oneRow.put("lbEnabled",  image);
                    oneRow.put("sniffers", engines);
                    List sniffersList = GuiUtil.parseStringList(engines, ",");
                    oneRow.put("sniffersList", sniffersList);
                    for(int ix=0; ix< sniffersList.size(); ix++)
                        filters.add(sniffersList.get(ix));
                    if (filterValue != null){
                        if (! sniffersList.contains(filterValue))
                            continue;
                    }
                    result.add(oneRow);
                }catch(Exception ex){
                    //skip this app.
                }
	    }
	}
        handlerCtx.setOutputValue("result", result);
        handlerCtx.setOutputValue("filters", new ArrayList(filters));
    }


    @Handler(id="getTargetURLList",
        input={
            @HandlerInput(name="AppID", type=String.class, required=true),
            @HandlerInput(name="contextRoot", type=String.class)},
        output={
            @HandlerOutput(name="URLList", type=List.class)})

    public void getTargetURLList(HandlerContext handlerCtx) {
	String appID = (String)handlerCtx.getInputValue("AppID");
        String contextRoot = (String)handlerCtx.getInputValue("contextRoot");
        String ctxRoot = calContextRoot(contextRoot);
        Set<String> URLs = new TreeSet();
        List<String> targetList = DeployUtil.getApplicationTarget(appID, "application-ref");
        for(String target : targetList) {
            String ep = TargetUtil.getTargetEndpoint(target) + "/application-ref/" + appID;
            boolean enabled = Boolean.parseBoolean((String)RestUtil.getAttributesMap(ep).get("enabled"));
            if (!enabled)
                continue;

            String virtualServers = getVirtualServers(target, appID);
            String configName = TargetUtil.getConfigName(target);

            List clusters = TargetUtil.getClusters();
            List<String> instances = new ArrayList();
            if (clusters.contains(target)){
                instances = TargetUtil.getClusteredInstances(target);
            } else {
                instances.add(target);
            }

            for (String instance : instances) {
                Collection<String> hostNames = TargetUtil.getHostNames(instance);
                URLs.addAll(getURLs(GuiUtil.parseStringList(virtualServers, ","), configName, hostNames, instance));
            }
        }

	Iterator it = URLs.iterator();
	String url = null;
        ArrayList list = new ArrayList();

	while (it.hasNext()) {
	    url = (String)it.next();
            String target = "";
            int i = url.indexOf("@@@");
            if (i >= 0) {
                target = url.substring(0, i);
                url = url.substring(i + 3);                
            }
                
            HashMap<String, String> m = new HashMap();
            m.put("url", url + ctxRoot);
            m.put("target", target);
            list.add(m);
	}
                
        handlerCtx.setOutputValue("URLList", list);

    }


    /*
     * Get the application type for the specified appName.
     * If there isComposite property is true, the appType will be returned as 'ear'
     * Otherwise, depends on the sniffer engine
     */
    @Handler(id = "gf.getApplicationType",
        input = {
            @HandlerInput(name = "snifferMap", type = Map.class, required = true)},
        output = {
            @HandlerOutput(name = "appType", type = String.class)})
    public static void getApplicationType(HandlerContext handlerCtx) {
        Map<String,String> snifferMap = (Map) handlerCtx.getInputValue("snifferMap");
        String appType = "other";
        if (! GuiUtil.isEmpty(snifferMap.get("web"))){
            appType="war";
        }if (! GuiUtil.isEmpty(snifferMap.get("ejb"))){
            appType="ejb";
        }else
        if (! GuiUtil.isEmpty(snifferMap.get("connector"))){
            appType="rar";
        }else
        if (! GuiUtil.isEmpty(snifferMap.get("appclient"))){
            appType="appclient";
        }
        handlerCtx.setOutputValue("appType", appType);
    }


    private static String calContextRoot(String contextRoot) {
        //If context root is not specified or if the context root is "/", ensure that we don't show two // at the end.
        //refer to issue#2853
        String ctxRoot = "";
        if ((contextRoot == null) || contextRoot.equals("") || contextRoot.equals("/")) {
            ctxRoot = "/";
        } else if (contextRoot.startsWith("/")) {
            ctxRoot = contextRoot;
        } else {
            ctxRoot = "/" + contextRoot;
        }
        return ctxRoot;
    }
    


/********************/


    private static Set getURLs(List<String> vsList, String configName, Collection<String> hostNames, String target) {
        Set URLs = new TreeSet();
        if (vsList == null || vsList.size() == 0) {
            return URLs;
        }
        //Just to ensure we look at "server" first.
        if (vsList.contains("server")){
            vsList.remove("server");
            vsList.add(0, "server");
        }
        String ep = (String)GuiUtil.getSessionValue("REST_URL");
        ep = ep + "/configs/config/" + configName + "/http-service/virtual-server";
        Map vsInConfig = new HashMap();
        try{
            vsInConfig = RestUtil.getChildMap(ep);

        }catch (Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getURLs") + ex.getLocalizedMessage());
                if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                    ex.printStackTrace();
                }
        }
        String localHostName = null;
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            // ignore exception
        }

        for (String vsName : vsList) {
            if (vsName.equals("__asadmin")) {
                continue;
            }
            Object vs = vsInConfig.get(vsName);
            if (vs != null) {
                ep = (String)GuiUtil.getSessionValue("REST_URL") + "/configs/config/" +
                        configName + "/http-service/virtual-server/" + vsName;
                String listener = (String)RestUtil.getAttributesMap(ep).get("networkListeners");

                if (GuiUtil.isEmpty(listener)) {
                    continue;
                } else {
                    List<String> hpList = GuiUtil.parseStringList(listener, ",");
                    for (String one : hpList) {
                        ep = (String)GuiUtil.getSessionValue("REST_URL") +
"/configs/config/" + configName + "/network-config/network-listeners/network-listener/" + one;

                        Map nlAttributes = RestUtil.getAttributesMap(ep);
                        if ("false".equals((String)nlAttributes.get("enabled"))) {
                            continue;
                        }
//                        String security = (String)oneListener.findProtocol().attributesMap().get("SecurityEnabled");
                        ep = (String)GuiUtil.getSessionValue("REST_URL") + "/configs/config/" +
                                configName + "/network-config/protocols/protocol/" + (String)nlAttributes.get("protocol");
                        String security = (String)RestUtil.getAttributesMap(ep).get("securityEnabled");

                        String protocol = "http";
                        if ("true".equals(security))
                            protocol = "https";

                        String port = (String)nlAttributes.get("port");
                        if (port == null)
                            port = "";
                        String resolvedPort = RestUtil.resolveToken((String)GuiUtil.getSessionValue("REST_URL") +
                                "/servers/server/" + target, port);

                        for (String hostName : hostNames) {
                            if (localHostName != null && hostName.equalsIgnoreCase("localhost"))
                                hostName = localHostName;
//                            URLs.add("[" + target + "]  - " + protocol + "://" + hostName + ":" + resolvedPort + "[ " + one + " " + configName 
//                                    + " " + listener + " " + target + " ]");
                            URLs.add(target + "@@@" + protocol + "://" + hostName + ":" + resolvedPort);
                        }
                    }
                }
            }
        }
        return URLs;
    }
}
