/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
 * ClusterHandler.java
 *
 * Created on July 1,2010  9:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.admingui.common.util.TargetUtil;
import org.glassfish.api.admin.InstanceState;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;

public class ClusterHandler {
    public static final String CLUSTER_RESOURCE_NAME = "org.glassfish.cluster.admingui.Strings";

    //The following is defined in v3/cluster/admin/src/main/java/..../cluster/Constants.java
    public static final String RUNNING = "RUNNING";
    public static final String NOT_RUNNING = "NOT_RUNNING";
    public static final String PARTIALLY_RUNNING = "PARTIALLY_RUNNING";

    /** Creates a new instance of InstanceHandler */
    public ClusterHandler() {
    }

    /**
     * This method takes in a list of instances with status, which is the output of list-instances
     * and count the # of instance that is running and non running.
     * @param handlerCtx
     */
    @Handler(id = "gf.getClusterStatusSummary",
        input = {
            @HandlerInput(name = "statusMap", type = Map.class, required = true)
        },
        output = {
            @HandlerOutput(name = "numRunning", type = String.class),
            @HandlerOutput(name = "numNotRunning", type = String.class),
            @HandlerOutput(name = "numRequireRestart", type = String.class),
            @HandlerOutput(name = "disableStart", type = Boolean.class),
            @HandlerOutput(name = "disableStop", type = Boolean.class),
            @HandlerOutput(name = "disableEjb", type = Boolean.class)
        })
    public static void getClusterStatusSummary(HandlerContext handlerCtx) {
        Map statusMap = (Map) handlerCtx.getInputValue("statusMap");
        int running=0;
        int notRunning=0;
        int requireRestart=0;
        int unknown = 0;
        try{
            for (Iterator it=statusMap.values().iterator(); it.hasNext(); ) {
                Object value = it.next();
                if (value.toString().equals(InstanceState.StateType.RUNNING.getDescription())){
                    running++;
                }else
                if (value.toString().equals(InstanceState.StateType.NOT_RUNNING.getDescription())){
                    notRunning++;
                }else
                if (value.toString().equals(InstanceState.StateType.RESTART_REQUIRED.getDescription())){
                    requireRestart++;
                }else {
                    unknown++;
                    GuiUtil.getLogger().severe("Unknown Status");
                }
            }

            handlerCtx.setOutputValue("disableEjb", (notRunning > 0) ? false :true);  //refer to bug#6342445
            handlerCtx.setOutputValue("disableStart", (notRunning > 0) ? false :true);
            handlerCtx.setOutputValue("disableStop", ( (running+requireRestart) > 0) ? false :true);
            handlerCtx.setOutputValue( "numRunning" , (running > 0) ?
                GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.number.instance.running", new String[]{""+running, GuiUtil.getCommonMessage("status.image.RUNNING")} ) : "");

            handlerCtx.setOutputValue( "numNotRunning" , (notRunning > 0) ?
                GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.number.instance.notRunning", new String[]{""+notRunning , GuiUtil.getCommonMessage("status.image.NOT_RUNNING")}) : "");

            handlerCtx.setOutputValue( "numRequireRestart" , (requireRestart > 0) ?
                GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.number.instance.requireRestart", new String[]{""+requireRestart, GuiUtil.getCommonMessage("status.image.REQUIRES_RESTART")}) : "");
        }catch(Exception ex){
            handlerCtx.setOutputValue("numRunning", GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.status.unknown"));
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getClusterStatusSummary") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
         }
     }


    @Handler(id = "gf.isClusterName",
        input = {
            @HandlerInput(name = "clusterName", type = String.class, required = true)
        },
        output = {
            @HandlerOutput(name = "exists", type = Boolean.class)
        })
    public static void isClusterName(HandlerContext handlerCtx) {
        if( ! TargetUtil.isCluster((String) handlerCtx.getInputValue("clusterName"))){
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.NoSuchCluster"));
            handlerCtx.setOutputValue("exists",  false);
        }else{
            handlerCtx.setOutputValue("exists",  true);
        }
    }

    @Handler(id = "gf.isInstanceName",
        input = {
                @HandlerInput(name = "instanceName", type = String.class, required = true)
        },
        output = {
                @HandlerOutput(name = "exists", type = Boolean.class)
        })
    public static void isInstanceName(HandlerContext handlerCtx) {
        if( ! TargetUtil.isInstance((String) handlerCtx.getInputValue("instanceName"))){
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.NoSuchInstance"));
            handlerCtx.setOutputValue("exists",  false);
        }else{
            handlerCtx.setOutputValue("exists",  true);
        }
    }

    @Handler(id = "gf.isConfigName",
        input = {
            @HandlerInput(name = "configName", type = String.class, required = true)
        },
        output = {
                    @HandlerOutput(name = "exists", type = Boolean.class)
        })
    public static void isConfigName(HandlerContext handlerCtx) {
        String configName = (String) handlerCtx.getInputValue("configName");
        List config = TargetUtil.getConfigs();
        if(!config.contains(configName)){
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.NoSuchConfig"));
            handlerCtx.setOutputValue("exists",  false);
        }else{
            handlerCtx.setOutputValue("exists",  true);
        }
    }

    @Handler(id = "gf.saveInstanceWeight",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true)})
    public static void saveInstanceWeight(HandlerContext handlerCtx) {
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        List errorInstances = new ArrayList();
        Map response = null;

        String prefix = GuiUtil.getSessionValue("REST_URL") + "/servers/server/";
        for (Map oneRow : rows) {
            String instanceName = (String) oneRow.get("encodedName");
            Map attrsMap = new HashMap();
            attrsMap.put("lbWeight", oneRow.get("lbWeight"));
            try{
                response = RestUtil.restRequest( prefix+instanceName , attrsMap, "post" , null, false);
            }catch (Exception ex){
                GuiUtil.getLogger().severe(
                        GuiUtil.getCommonMessage("LOG_SAVE_INSTANCE_WEIGHT_ERROR" ,  new Object[]{prefix+instanceName, attrsMap}));
                response = null;
            }
            if (response ==null){
                errorInstances.add(instanceName);
            }
        }
        if (errorInstances.size() > 0){
            String details = GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "instance.error.updateWeight" , new String[]{""+errorInstances});
            GuiUtil.handleError(handlerCtx, details);
        }
     }


    @Handler(id = "gf.clusterAction",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true),
            @HandlerInput(name = "action", type = String.class, required = true),
            @HandlerInput(name = "extraInfo", type = Object.class) })
    public static void clusterAction(HandlerContext handlerCtx) {
        String action = (String) handlerCtx.getInputValue("action");
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        String  errorMsg = null;
        String prefix = GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster/";

        for (Map oneRow : rows) {
            String clusterName = (String) oneRow.get("name");
            String endpoint = prefix + clusterName + "/" + action;
            String method = "post";
            if (action.equals("delete-cluster")){
                //need to delete the clustered instance first
                Map clusterInstanceMap = (Map)handlerCtx.getInputValue("extraInfo");
                List<String> instanceNameList = (List) clusterInstanceMap.get(clusterName);
                for(String instanceName : instanceNameList){
                    errorMsg = deleteInstance(instanceName);
                    if (errorMsg != null){
                        GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), errorMsg);
                        return;
                    }
                }
                endpoint = prefix + clusterName;
                method = "delete";
            }
            try{
                GuiUtil.getLogger().info(endpoint);
                RestUtil.restRequest( endpoint, null, method, null, false);
            }catch (Exception ex){
                GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), ex.getMessage());
                return;
            }
        }
     }


    @Handler(id = "gf.instanceAction",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true),
            @HandlerInput(name = "action", type = String.class, required = true)})
    public static void instanceAction(HandlerContext handlerCtx) {
        String action = (String) handlerCtx.getInputValue("action");
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        String prefix = GuiUtil.getSessionValue("REST_URL") + "/servers/server/";

        for (Map oneRow : rows) {
            String instanceName = (String) oneRow.get("name");
            if(action.equals("delete-instance")){
                String errorMsg = deleteInstance(instanceName);
                if (errorMsg != null){
                    GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), errorMsg);
                    return;
                }
            }else{
                try {
                    String endpoint = prefix + instanceName + "/" + action;
                    GuiUtil.getLogger().info(endpoint);
                    RestUtil.restRequest(endpoint , null, "post" ,null, false);
                } catch (Exception ex){
                    String endpoint=prefix + instanceName + "/" + action;
                    GuiUtil.getLogger().severe(
                        GuiUtil.getCommonMessage("LOG_ERROR_INSTANCE_ACTION", new Object[]{endpoint, "null"}));
                    GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), ex.getMessage());
                    return;
                }
            }
        }
        if(action.equals("stop-instance")){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                //noop.
            }
        }
     }


    @Handler(id = "gf.nodeAction",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true),
            @HandlerInput(name = "action", type = String.class, required = true),
            @HandlerInput(name = "nodeInstanceMap", type = Map.class)})
    public static void nodeAction(HandlerContext handlerCtx) {
        String action = (String) handlerCtx.getInputValue("action");
        Map nodeInstanceMap = (Map) handlerCtx.getInputValue("nodeInstanceMap");
        if (nodeInstanceMap == null){
            nodeInstanceMap=new HashMap();
        }
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        String prefix = GuiUtil.getSessionValue("REST_URL") + "/nodes/";

        for (Map oneRow : rows) {
            String nodeName = (String) oneRow.get("name");
            final String localhostNodeName = (String) GuiUtil.getSessionValue("localhostNodeName");
            if (nodeName.equals(localhostNodeName)){
                GuiUtil.prepareAlert("error",  GuiUtil.getMessage("msg.Error"),
                        GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "node.error.removeLocalhost" , new String[]{localhostNodeName}));
                return;
            }
            List instancesList = (List)nodeInstanceMap.get(nodeName);
            if ( instancesList!= null && (instancesList.size()) != 0){
                GuiUtil.prepareAlert("error",  GuiUtil.getMessage("msg.Error"),
                        GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "nodes.instanceExistError", new String[]{ nodeName, nodeInstanceMap.get(nodeName).toString()}));
                return;
            }
            if(action.equals("delete-node")){
                try{
                    String endpoint = prefix + "node/" + nodeName;
                    GuiUtil.getLogger().info(endpoint);
                    RestUtil.restRequest(endpoint, null, "DELETE",null, false);
                }catch (Exception ex){
                    GuiUtil.getLogger().severe(
                            GuiUtil.getCommonMessage("LOG_NODE_ACTION_ERROR", new Object[]{prefix + nodeName, "DELETE" , "null"}));
                    GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), ex.getMessage());
                    return;
                }
                continue;
            }
            Map payload = null;
            String type = (String) oneRow.get("type");
            String endpoint = "";
            if(action.equals("delete-node-uninstall")){
                try{
                    if ("CONFIG".equals(type)){
                        endpoint = prefix + "delete-node-config";
                        payload = new HashMap();
                        payload.put("id", nodeName);
                    }else
                    if ("SSH".equals(type)){
                        endpoint = prefix +  "delete-node-ssh";
                        payload = new HashMap();
                        payload.put("id", nodeName);
                        payload.put("uninstall", "true");
                    }else
                    if ("DCOM".equals(type)){
                        endpoint = prefix +  "delete-node-dcom";
                        payload = new HashMap();
                        payload.put("id", nodeName);
                        payload.put("uninstall", "true");
        }
                    GuiUtil.getLogger().info(endpoint);
                    RestUtil.restRequest(endpoint, payload, "DELETE",null, false);
                }catch (Exception ex){
                    GuiUtil.getLogger().severe(
                            GuiUtil.getCommonMessage("LOG_NODE_ACTION_ERROR", new Object[]{endpoint,"" , payload}));
                    GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), ex.getMessage());
                    return;
     }
            }
        }
     }



    @Handler(id = "gf.createClusterInstances",
        input = {
            @HandlerInput(name = "clusterName", type = String.class, required = true),
            @HandlerInput(name = "instanceRow", type = List.class, required = true)})
    public static void createClusterInstances(HandlerContext handlerCtx) {
        String clusterName = (String) handlerCtx.getInputValue("clusterName");
        List<Map> instanceRow =  (List<Map>) handlerCtx.getInputValue("instanceRow");
        Map attrsMap = new HashMap();
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/create-instance";
        for (Map oneInstance : instanceRow) {
            attrsMap.put("name", oneInstance.get("name"));
            attrsMap.put("cluster", clusterName);
            attrsMap.put("node", oneInstance.get("node"));
            try{
                GuiUtil.getLogger().info(endpoint);
                GuiUtil.getLogger().info(attrsMap.toString());
                RestUtil.restRequest( endpoint , attrsMap, "post" ,null, false);
                //set lb weight
                String wt = (String) oneInstance.get("weight");
                if ( !GuiUtil.isEmpty(wt)) {
                    String encodedInstanceName = URLEncoder.encode((String)oneInstance.get("name"), "UTF-8");
                    String ep = GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + encodedInstanceName ;
                    Map wMap = new HashMap();
                    wMap.put("lbWeight", wt);
                    RestUtil.restRequest( ep , wMap, "post" , null, false);
                }
            }catch (Exception ex){
                GuiUtil.getLogger().severe(
                    GuiUtil.getCommonMessage("LOG_CREATE_CLUSTER_INSTANCE" ,  new Object[]{clusterName,endpoint, attrsMap}));
                GuiUtil.prepareException(handlerCtx, ex);
            }
        }

    }


    /*
     * getDeploymentTargets takes in a list of cluster names, and an list of Properties that is returned from the
     * list-instances --standaloneonly=true.  Extract the instance name from this properties list.
     * The result list will include "server",  clusters and standalone instances,  suitable for deployment or create resources.
     *
     */
    @Handler(id = "gf.getDeploymentTargets",
        input = {
            @HandlerInput(name = "clusterList", type = List.class), // TODO: Should this be a map too?
            @HandlerInput(name = "listInstanceProps", type = List.class)
        },
        output = {
            @HandlerOutput(name = "result", type = List.class)
        })
    public static void getDeploymentTargets(HandlerContext handlerCtx) {
        List<String> result = new ArrayList();
        result.add("server");
        try{
            List<String> clusterList = (List) handlerCtx.getInputValue("clusterList");
            if (clusterList != null){
                for(String oneCluster : clusterList){
                    result.add(oneCluster);
                }
            }

            List<Map> instances = (List<Map>) handlerCtx.getInputValue("listInstanceProps");
            if (instances != null) {
                for (Map instance : instances) {
                    result.add((String)instance.get("name"));
                    //result.addAll(props.keySet());
                }
            }
         }catch(Exception ex){
             GuiUtil.getLogger().severe(ex.getLocalizedMessage());//"getDeploymentTargets failed.");
             //print stacktrace ??
         }
        handlerCtx.setOutputValue("result", result);
     }

    // If successfully deleted the instance, null will be returned, otherwise, return the error string to be displayed to user.
    private static String deleteInstance(String instanceName){
        try{
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + instanceName + "/delete-instance";
            GuiUtil.getLogger().info(endpoint);
            RestUtil.restRequest( endpoint, null,"post", null, false );
            return null;
        }catch(Exception ex){
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + instanceName + "/delete-instance\n";
            GuiUtil.getLogger().severe(
                    GuiUtil.getCommonMessage("LOG_DELETE_INSTANCE", new Object[]{endpoint, "null"}));
            return ex.getMessage();
        }
    }

    @Handler(id = "gf.listClusters",
            output = {
                    @HandlerOutput(name = "clusters", type = List.class)
            })
    public static void listClusters(HandlerContext handlerCtx) {
        List clusters = TargetUtil.getClusters();
        handlerCtx.setOutputValue("clusters", clusters);
    }

    @Handler(id = "gf.listConfigs",
            output = {
                    @HandlerOutput(name = "configs", type = List.class)
            })
    public static void listConfigs(HandlerContext handlerCtx) {
        List configs = TargetUtil.getConfigs();
        handlerCtx.setOutputValue("configs", configs);
    }


    @Handler(id = "gf.listInstances",
        input = {
            @HandlerInput(name="optionKeys", type=List.class, required=true),
            @HandlerInput(name="optionValues", type=List.class, required=true)
        },
        output = {
            @HandlerOutput(name = "instances", type = List.class),
            @HandlerOutput(name = "statusMap", type = Map.class),
            @HandlerOutput(name = "uptimeMap", type = Map.class),
            @HandlerOutput(name = "listEmpty", type = Boolean.class)
        })
    public static void listInstances(HandlerContext handlerCtx) {

        List instances = new ArrayList();
        Map statusMap = new HashMap();
        Map uptimeMap = new HashMap();
        List<String> keys = (List<String>) handlerCtx.getInputValue("optionKeys");
        List values = (List) handlerCtx.getInputValue("optionValues");
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (keys != null && values != null) {
            for (int i = 0; i < keys.size(); i++) {
                attrs.put(keys.get(i), values.get(i));
            }
        }
        String endpoint = GuiUtil.getSessionValue("REST_URL")+"/list-instances";
        try{
            Map responseMap = RestUtil.restRequest( endpoint , attrs, "GET" , handlerCtx, false);
            Map extraPropertiesMap = (Map)((Map)responseMap.get("data")).get("extraProperties");
            if (extraPropertiesMap != null){
                List<Map> instanceList = (List)extraPropertiesMap.get("instanceList");
                if (instanceList != null){
                    for(Map oneInstance : instanceList){
                        instances.add(oneInstance.get("name"));
                        statusMap.put(oneInstance.get("name"), oneInstance.get("status"));
                        uptimeMap.put(oneInstance.get("name"), oneInstance.get("uptime"));
                    }
                }
            }
        }catch (Exception ex){
            GuiUtil.getLogger().severe(
                 GuiUtil.getCommonMessage("LOG_LIST_INSTANCES", new Object[]{endpoint, attrs}));
            //we don't need to call GuiUtil.handleError() because thats taken care of in restRequest() when we pass in the handler.
        }
        handlerCtx.setOutputValue("instances", instances);
        handlerCtx.setOutputValue("statusMap", statusMap);
        handlerCtx.setOutputValue("uptimeMap", uptimeMap);
        handlerCtx.setOutputValue("listEmpty", instances.isEmpty());
    }

    @Handler(id = "gf.getClusterNameForInstance",
        input = {
            @HandlerInput(name="instanceName", type=String.class, required=true)},
        output = {
            @HandlerOutput(name = "clusterName", type = String.class)})
    public static void getClusterNameForInstance(HandlerContext handlerCtx) {

        String instanceName = (String) handlerCtx.getInputValue("instanceName");
        try{
            List<String> clusterList = new ArrayList(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL")+"/clusters/cluster").keySet());
            for(String oneCluster : clusterList){
                List<String> serverRefs = new ArrayList (RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL")+ "/clusters/cluster/" +
                        URLEncoder.encode(oneCluster, "UTF-8") + "/server-ref").keySet());
                if (serverRefs.contains(instanceName)){
                    handlerCtx.setOutputValue("clusterName", oneCluster);
                    return;
                }
            }
        }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("LOG_GET_CLUSTERNAME_FOR_INSTANCE"));
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
    }


    @Handler(id = "gf.convertNodePswd",
        input = {
            @HandlerInput(name="pswd", type=String.class, required=true)},
        output = {
            @HandlerOutput(name = "pswdText", type = String.class),
            @HandlerOutput(name = "pswdAlias", type = String.class),
            @HandlerOutput(name = "psSelected", type = String.class)})
    public static void convertNodePswd(HandlerContext handlerCtx) {

        String pswd = (String) handlerCtx.getInputValue("pswd");
        if (GuiUtil.isEmpty(pswd)){
            handlerCtx.setOutputValue("psSelected", 1);
            return;
        }
        if (pswd.startsWith("${ALIAS=") && pswd.endsWith("}")){
            String pswdAlias = pswd.substring(8, pswd.length()-1);
            handlerCtx.setOutputValue("pswdAlias", pswdAlias);
            handlerCtx.setOutputValue("psSelected", 3);
            return;
        }
        handlerCtx.setOutputValue("psSelected", 2);
        handlerCtx.setOutputValue("pswdText", pswd);
    }

//gf.convertToAlias(in="#{pageSession.pswdAlias}" out="#{requestScope.tmpv}");
    @Handler(id = "gf.convertToAlias",
        input = {
            @HandlerInput(name="in", type=String.class, required=true)},
        output = {
            @HandlerOutput(name = "out", type = String.class)})
    public static void convertToAlias(HandlerContext handlerCtx) {
        String in = (String) handlerCtx.getInputValue("in");
        String out = null;
        if (! GuiUtil.isEmpty(in)){
            out = "${ALIAS="+in+"}";
        }
        handlerCtx.setOutputValue("out",  out);
    }

    @Handler(id = "gf.changeClusterStatus",
        input = {
            @HandlerInput(name = "selectedRows", type = List.class, required = true),
            @HandlerInput(name = "clusterName", type = String.class, required = true),
            @HandlerInput(name = "Enabled", type = String.class, required = true),
            @HandlerInput(name = "forLB", type = Boolean.class, required = true)})
    public static void changeClusterStatus(HandlerContext handlerCtx) {
       String Enabled = (String) handlerCtx.getInputValue("Enabled");
        String clusterName = (String) handlerCtx.getInputValue("clusterName");
        List<Map>  selectedRows = (List) handlerCtx.getInputValue("selectedRows");
        boolean forLB = (Boolean) handlerCtx.getInputValue("forLB");
        for(Map oneRow : selectedRows){
            Map attrs = new HashMap();
            String name = (String) oneRow.get("name");
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster/" + clusterName + "/server-ref/" + name;
            if(forLB){
                attrs.put("lbEnabled", Enabled);
                RestUtil.restRequest(endpoint, attrs, "post", handlerCtx, false);
            }else{
                attrs.put("enabled", Enabled);
                RestUtil.restRequest(endpoint, attrs, "post", handlerCtx, false);
            }
        }
     }
    
    @Handler(id="gf.getClusterForConfig",
            input={
                @HandlerInput(name="configName", type=String.class, required=true)
            },
            output={
                @HandlerOutput(name="cluster", type=String.class)
            })
    public static void getClusterForConfig(HandlerContext handlerCtx) {
        String configName = (String)handlerCtx.getInputValue("configName");
        String clusterName = null;
        Domain domain = GuiUtil.getHabitat().getService(Domain.class);
        
        for (Cluster cluster: domain.getClusters().getCluster()) {
            if (cluster.getConfigRef().equals(configName)) {
                clusterName = cluster.getName();
                break;
            }
        }
        
        if (clusterName != null) {
            handlerCtx.setOutputValue("cluster", clusterName);
        } else {
            //
        }
    }
}
