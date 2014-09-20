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
 * DeploymentHandler.java
 *
 */
package org.glassfish.admingui.common.handlers;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.SyntaxException;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.layout.template.TemplateParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.admingui.common.util.AppUtil;
import org.glassfish.admingui.common.util.DeployUtil;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.admingui.common.util.TargetUtil;


/**
 *
 */
public class DeploymentHandler {

    @Handler(id = "deploy",
    input = {
        @HandlerInput(name = "filePath", type = String.class),
        @HandlerInput(name = "origPath", type = String.class),
        @HandlerInput(name = "allMaps", type = Map.class),
        @HandlerInput(name = "appType", type = String.class),
        @HandlerInput(name="targets", type=String[].class, required=true ),
        @HandlerInput(name = "propertyList", type = List.class)
    })
    public static void deploy(HandlerContext handlerCtx) {

        String appType = (String) handlerCtx.getInputValue("appType");
        String origPath = (String) handlerCtx.getInputValue("origPath");
        String filePath = (String) handlerCtx.getInputValue("filePath");
        Map allMaps = (Map) handlerCtx.getInputValue("allMaps");
        Map attrMap = new HashMap((Map) allMaps.get(appType));
        Map payload = new HashMap();
        List<String> appRefs = new ArrayList();

        if (GuiUtil.isEmpty(origPath)) {
            GuiUtil.getLogger().info("deploy(): origPath is NULL");
            String mesg = GuiUtil.getMessage("msg.deploy.nullArchiveError");
            GuiUtil.handleError(handlerCtx, mesg);
            return;
        }
        try{
            String decodedName = URLDecoder.decode((String)attrMap.get("name"), "UTF-8");
            attrMap.put("name", decodedName);
        }catch(Exception ex){
            //ignore
        }
        

        /* Take care some special properties, such as VS  */
        //do not send VS if user didn't specify, refer to bug#6542276
        String[] vs = (String[]) attrMap.get("virtualservers");
        if (vs != null && vs.length > 0) {
            if (!GuiUtil.isEmpty(vs[0])) {
                String vsTargets = GuiUtil.arrayToString(vs, ",");
                payload.put("virtualservers", vsTargets);
            }
        }
        attrMap.remove("virtualservers");

        //Take care of checkBox
        List<String> convertToFalseList = (List) attrMap.get("convertToFalseList");
        if (convertToFalseList != null) {
            for (String one : convertToFalseList) {
                if (attrMap.get(one) == null) {
                    attrMap.put(one, "false");
                }
            }
            attrMap.remove("convertToFalseList");
        }

        StringBuilder sb = new StringBuilder();
        String sep = "";
        Properties props = new Properties();
        for (Object attr : attrMap.keySet()) {
            String key = (String) attr;
            String prefix = "PROPERTY-";
            String value = (String) attrMap.get(key);
            if (value == null) {
                continue;
            }
            if (key.startsWith(prefix)) {
                if (! value.equals("")){
                    props.setProperty(key.substring(prefix.length()), value);
                    sb.append(sep).append(key.substring(prefix.length())).append("=").append(value);
                    sep=":";
                }

            } else {
                payload.put(key, value);
            }
        }
        // include any  additional property that user enters
        List<Map<String, String>> propertyList = (List) handlerCtx.getInputValue("propertyList");
        if (propertyList != null) {
            Set propertyNames = new HashSet();
            for (Map<String, String> oneRow : propertyList) {
                final String name = oneRow.get("name");
                if (GuiUtil.isEmpty(name)) {
                    continue;
                }
                if (propertyNames.contains(name)) {
                    Logger logger = GuiUtil.getLogger();
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, GuiUtil.getCommonMessage("LOG_IGNORE_DUP_PROP", new Object[]{name}));
                    }
                    continue;
                } else {
                    propertyNames.add(name);
                }
                String value = oneRow.get("value");
                if (GuiUtil.isEmpty(value)) {
                    continue;
                }
                props.setProperty(name, value);

            }
        }
        if (sb.length() > 0){
            payload.put("properties", sb.toString());
        }
        payload.put("id", filePath);
        String[] targets = (String[])handlerCtx.getInputValue("targets");
        if (targets == null || targets.length==0){
            payload.put("target", "domain");
        }else{
            //deploy to the 1st target, and add ref to the remaining target.
            payload.put("target", targets[0]);
            for(int i=1; i< targets.length; i++){
                appRefs.add(targets[i]);
            }
        }
        try {
            String prefix = (String) GuiUtil.getSessionValue("REST_URL");
            RestUtil.restRequest(prefix +  "/applications/application", payload, "post", null, true);
            //Create application-ref if needed.
            if (appRefs.size() > 0){
                List clusters = TargetUtil.getClusters();
                List standalone = TargetUtil.getStandaloneInstances();
                for(int i=0; i< appRefs.size(); i++){
                    AppUtil.manageAppTarget((String)attrMap.get("name"), 
                            appRefs.get(i), true, (String)attrMap.get("enabled"), 
                            clusters, standalone, handlerCtx);
                }
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    /**
     *  <p> This handler redeploy any application
     */
    @Handler(id = "gf.redeploy",
    input = {
        @HandlerInput(name = "filePath", type = String.class, required = true),
        @HandlerInput(name = "deployMap", type = Map.class, required = true),
        @HandlerInput(name = "convertToFalse", type = List.class, required = true),
        @HandlerInput(name = "valueMap", type = Map.class, required = true)
    })
    public static void redeploy(HandlerContext handlerCtx) {
        try {
            String filePath = (String) handlerCtx.getInputValue("filePath");
            Map<String,String> deployMap = (Map) handlerCtx.getInputValue("deployMap");
            Map<String,String> valueMap = (Map) handlerCtx.getInputValue("valueMap");
            List<String> convertToFalsList = (List<String>) handlerCtx.getInputValue("convertToFalse");
            if (convertToFalsList != null)
            for (String one : convertToFalsList) {
                if (deployMap.get(one) == null) {
                    deployMap.put(one, "false");
                }
            }
            String appName = deployMap.get("appName");
            Map payload = new HashMap();

             //If we are redeploying a web app, we want to preserve context root.
             String ctxRoot = valueMap.get("contextroot");
             if (ctxRoot != null){
                 payload.put("contextroot", ctxRoot);
             }
             String keepState = deployMap.get("keepState");
             if (keepState != null){
                payload.put("keepstate",keepState);
             }
             payload.put("id", filePath);
             payload.put("force", "true");
             payload.put("name", appName);
             payload.put("deploymentOrder", deployMap.get("deploymentOrder"));
             payload.put("verify", deployMap.get("verify"));
             payload.put("precompilejsp", deployMap.get("precompilejsp"));
             payload.put("availabilityEnabled", deployMap.get("availabilityEnabled"));
             if ("osgi".equals(deployMap.get("type"))){
                 payload.put("type", "osgi");
             }

             StringBuilder sb = new StringBuilder();
             String sep = "";
             if (deployMap.containsKey("java-web-start-enabled")){
                 if ("false".equals(deployMap.get("java-web-start-enabled"))){
                    sb.append("java-web-start-enabled").append("=").append("false");
                    sep = ":";
                 }else{
                     sb.append("java-web-start-enabled").append("=").append("true");
                     sep = ":";
                 }
             }
             if (deployMap.containsKey("preserveAppScopedResources")){
                 if ("true".equals(deployMap.get("preserveAppScopedResources"))){
                    sb.append(sep).append("preserveAppScopedResources").append("=").append("true");
                 }
             }
             if (sb.length()> 0){
                payload.put("properties", sb.toString());
             }

             List<String> targetList = DeployUtil.getApplicationTarget(appName, "application-ref");
             if ( targetList.size() != 1){
                payload.put("target", "domain");
             }else{
                payload.put("target",  targetList.get(0));
	     }
             String prefix = (String) GuiUtil.getSessionValue("REST_URL");
             RestUtil.restRequest(prefix +  "/applications/application", payload, "POST", null, true);
         } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
         }
    }

    /**
     *	<p> This handler takes in selected rows, and do the undeployment
     *  <p> Input  value: "selectedRows" -- Type: <code>java.util.List</code></p>
     *  <p> Input  value: "appType" -- Type: <code>String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="gf.undeploy",
    input={
        @HandlerInput(name="selectedRows", type=List.class, required=true)})
    public static void undeploy(HandlerContext handlerCtx) {

        Object obj = handlerCtx.getInputValue("selectedRows");

        List warningList = new ArrayList();
        //List undeployedAppList = new ArrayList();
        List selectedRows = (List) obj;
        //Hard coding to server, fix me for actual targets in EE.
        for (int i = 0; i < selectedRows.size(); i++) {
            Map oneRow = (Map) selectedRows.get(i);
            String appName = (String) oneRow.get("name");
            List targets = DeployUtil.getApplicationTarget(appName, "application-ref");

            Map payload = new HashMap();
            payload.put("target", "domain");
            String prefix = (String) GuiUtil.getSessionValue("REST_URL");
            try {
                Map responseMap = RestUtil.restRequest(prefix +  "/applications/application/" + appName, payload, "DELETE", null, true, true);
                if (targets.size() > 0){
                    removeFromDefaultWebModule(appName, targets);
                }
                if (RestUtil.hasWarning(responseMap)){
                    warningList.add(appName);
                }
            }catch(Exception ex){
                //we DO want it to continue and call the rest handlers, ie navigate(). This will
                //re-generate the table data because there may be some apps thats been undeployed
                //successfully.  If we stopProcessing, the table data is stale and still shows the
                //app that has been gone.

                //TODO: we should display the list of warning that happens before.
                GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), ex.getMessage());
                return;
            }
            
        }
        if (warningList.size() > 0){
            GuiUtil.prepareAlert("warning", GuiUtil.getCommonMessage("msg.Undeployment.warning"),  GuiUtil.getCommonMessage("msg.referToApp") + warningList );
        }
    }

    //For any undeployed applications, we need to ensure that it is no longer specified as the
    //default web module of any VS.
    static private void  removeFromDefaultWebModule(String undeployedAppName, List<String> targets){

        String prefix = GuiUtil.getSessionValue("REST_URL")+"/configs/config/";
        Map attrsMap = new HashMap();
        attrsMap.put("defaultWebModule", "");
        for(String oneTarget:  targets){
            try{
                //find the config ref. by this target
                String endpoint = TargetUtil.getTargetEndpoint(oneTarget);
                String configName = (String) RestUtil.getEntityAttrs(endpoint, "entity").get("configRef");
                String encodedConfigName = URLEncoder.encode(configName, "UTF-8");

                //get all the VS of this config
                String vsEndpoint =  prefix + encodedConfigName + "/http-service/virtual-server";
                Map vsMap = RestUtil.getChildMap( vsEndpoint );

                //for each VS, look at the defaultWebModule
                if (vsMap.size()>0){
                    List<String> vsList = new ArrayList(vsMap.keySet());
                    for(String oneVs : vsList){
                        String oneEndpoint = vsEndpoint+"/" + oneVs ;
                        String defWebModule = (String) RestUtil.getEntityAttrs( oneEndpoint , "entity").get("defaultWebModule");
                        if (GuiUtil.isEmpty(defWebModule)){
                            continue;
                        }
                        int index = defWebModule.indexOf("#");
                        if (index != -1){
                            defWebModule = defWebModule.substring(0, index);
                        }
                        if (undeployedAppName.equals(defWebModule)){
                            RestUtil.restRequest(oneEndpoint, attrsMap, "POST", null, false);
                        }
                    }
                }
            }catch(Exception ex){

            }
        }
    }
     
    /**
     *	<p> This method returns the deployment descriptors for a given app. </p>
     *
     *  <p> Output value: "descriptors" -- Type: <code>java.util.List</code>/</p>
     *	@param	handlerCtx	The HandlerContext.
     */
            
    @Handler(id = "gf.getDeploymentDescriptorList",
    input = {
        @HandlerInput(name = "data", type = List.class, required = true)},
    output = {
        @HandlerOutput(name = "descriptors", type = List.class)
    })
    public static void getDeploymentDescriptorList(HandlerContext handlerCtx) {
        List<Map<String, Object>> ddList = (List) handlerCtx.getInputValue("data");
        List result = new ArrayList();
        if ((ddList != null) && ddList.size() > 0) {
            for (Map<String, Object> oneDD : ddList) {
                HashMap oneRow = new HashMap();
                Map<String, String> props = (Map) oneDD.get("properties");
                final String mName = props.get(MODULE_NAME_KEY);
                oneRow.put("moduleName", (mName==null)?  "" : mName);
                final String ddPath = props.get(DD_PATH_KEY);
                oneRow.put("ddPath", (ddPath==null)? "" : ddPath);
                result.add(oneRow);
            }
        }
        handlerCtx.setOutputValue("descriptors", result);
    }

    @Handler(id = "gf.getDeploymentDescriptor",
    input = {
        @HandlerInput(name = "moduleName", type = String.class, required = true),
        @HandlerInput(name = "descriptorName", type = String.class, required = true),
        @HandlerInput(name = "data", type = List.class, required = true)
    }, output = {
        @HandlerOutput(name = "content", type = String.class),
        @HandlerOutput(name = "encoding", type = String.class)
    })
    public static void getDeploymentDescriptor(HandlerContext handlerCtx) {
        String moduleName = (String) handlerCtx.getInputValue("moduleName");
        if (moduleName == null){
            moduleName = "";   //for application.xml and sun-application.xml  where it is at top leverl, with a module name.
        }
        String descriptorName = (String) handlerCtx.getInputValue("descriptorName");
        List<Map<String, Object>> ddList = (List) handlerCtx.getInputValue("data");
        if (ddList.size() > 0) {
            for (Map<String, Object> oneDD : ddList) {
                Map<String, String> props = (Map) oneDD.get("properties");
                if (moduleName.equals(props.get(MODULE_NAME_KEY)) && descriptorName.equals(props.get(DD_PATH_KEY))) {
                    final String ddContent = props.get(DD_CONTENT_KEY);
                    String content = (ddContent==null)? "" : ddContent;
                    handlerCtx.setOutputValue("content", content);
                    handlerCtx.setOutputValue("encoding", getEncoding(content));
                }
            }
        }
    }

    private static String getEncoding(String xmlDoc) {
	String encoding = null;
	TemplateParser parser = new TemplateParser(new ByteArrayInputStream(xmlDoc.getBytes()));
	try {
	    parser.open();
	    encoding = parser.readUntil("encoding", false);
	    if (encoding.endsWith("encoding")) {
		// Read encoding="..."
		parser.readUntil('=', false);
		encoding = (String) parser.getNVP("encoding").getValue();
	    } else {
		// Not found...
		encoding = null;
	    }
	} catch (SyntaxException ex) {
	    encoding = null;
	} catch (IOException ex) {
	    encoding = null;
	}

        if (encoding == null) {
            encoding = "UTF-8";
        }
        return encoding;
    }

    private static final String MODULE_NAME_KEY = "module-name";
    private static final String DD_PATH_KEY = "dd-path";
    private static final String DD_CONTENT_KEY = "dd-content";

}
