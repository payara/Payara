/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.util;

import java.util.ArrayList;
import java.util.List;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.Map;

/**
 *
 * @author anilam
 */

public class DeployUtil {

    /* reload application for each target.  If the app is disabled for the target, it is an no-op.
     * otherwise, the app is disabled and then enabled to force the reload.
     */
    static public boolean reloadApplication(String appName, List<String> targets, HandlerContext handlerCtx){
        try{
            String decodedName = URLDecoder.decode(appName, "UTF-8");
            List clusters =  TargetUtil.getClusters();
            String clusterEndpoint = GuiUtil.getSessionValue("REST_URL")+"/clusters/cluster/";
            String serverEndpoint = GuiUtil.getSessionValue("REST_URL")+"/servers/server/";
            for(String targetName : targets){
                String endpoint ;
                if (clusters.contains(targetName)){
                    endpoint = clusterEndpoint + targetName + "/application-ref/" + decodedName ;
                }else{
                    endpoint = serverEndpoint + targetName + "/application-ref/"  + decodedName ;
                }
                String status = (String) RestUtil.getAttributesMap(endpoint).get("enabled");
                if ( Boolean.parseBoolean(status)){
                    Map attrs = new HashMap();
                    attrs.put("enabled", "false");
                    RestUtil.restRequest(endpoint, attrs, "POST", null, false, true);
                    attrs.put("enabled", "true");
                    RestUtil.restRequest(endpoint, attrs, "POST", null, false, true);
                }
            }
       }catch(Exception ex){
            GuiUtil.handleError(handlerCtx, ex.getMessage());
            return false;
       }
        return true;
    }


    //This method returns the list of targets (clusters and standalone instances) of any deployed application
    static public List getApplicationTarget(String appName, String ref){
        List targets = new ArrayList();
        try{
            //check if any cluster has this application-ref
            List<String> clusters = TargetUtil.getClusters();
            for(String oneCluster:  clusters){
                List appRefs = new ArrayList(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL")+"/clusters/cluster/"+oneCluster+"/"+ref).keySet());

                if (appRefs.contains(appName)){
                    targets.add(oneCluster);
                }
            }
            List<String> servers = TargetUtil.getStandaloneInstances();
            servers.add("server");
            for(String oneServer:  servers){
                List appRefs = new ArrayList(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + oneServer + "/" + ref).keySet());
                if (appRefs.contains(appName)){
                    targets.add(oneServer);
                }
            }

        }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.appTarget") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return targets;
    }
    
    static public List<Map> getRefEndpoints(String name, String ref){
        List endpoints = new ArrayList();
        try{
            String encodedName = URLEncoder.encode(name, "UTF-8");
            //check if any cluster has this application-ref
            List<String> clusters = TargetUtil.getClusters();
            for(String oneCluster:  clusters){
                List appRefs = new ArrayList(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL")+"/clusters/cluster/"+oneCluster+"/"+ref).keySet());
                if (appRefs.contains(name)){
                    Map aMap = new HashMap();
                    aMap.put("endpoint", GuiUtil.getSessionValue("REST_URL")+"/clusters/cluster/"+oneCluster+"/" + ref + "/" + encodedName);
                    aMap.put("targetName", oneCluster);
                    endpoints.add(aMap);
                }
            }
            List<String> servers = TargetUtil.getStandaloneInstances();
            servers.add("server");
            for(String oneServer:  servers){
                List appRefs = new ArrayList(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + oneServer + "/" + ref).keySet());
                if (appRefs.contains(name)){
                    Map aMap = new HashMap();
                    aMap.put("endpoint", GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + oneServer + "/" + ref + "/" + encodedName);
                    aMap.put("targetName", oneServer);
                    endpoints.add(aMap);
                }
            }
        }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getRefEndpoints")+ ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return endpoints;
    }


    public static String getTargetEnableInfo(String appName, boolean useImage, boolean isApp){
        String prefix = (String) GuiUtil.getSessionValue("REST_URL");
        List clusters = TargetUtil.getClusters();
        List standalone = TargetUtil.getStandaloneInstances();
        String enabled = "true";
        int numEnabled = 0;
        int numDisabled = 0;
        String ref = "application-ref";
        if (!isApp) {
            ref = "resource-ref";
        }
        if (clusters.isEmpty() && standalone.isEmpty()){
            //just return Enabled or not.
            enabled = (String)RestUtil.getAttributesMap(prefix  +"/servers/server/server/"+ref+"/"+appName).get("enabled");
            //for DAS only system, there should always be application-ref created for DAS. However, for the case where the application is
            //deploye ONLY to a cluster/instance, and then that instance is deleted.  The system becomes 'DAS' only, but then the application-ref
            //for DAS will not exist. For this case, we just look at the application itself
            if (enabled == null){
                enabled = (String)RestUtil.getAttributesMap(prefix +"/applications/application/" + appName).get("enabled");
            }
            if (useImage){
                return (Boolean.parseBoolean(enabled))? "/resource/images/enabled.png" : "/resource/images/disabled.png";
            }else{
                return enabled;
            }
        }
        standalone.add("server");        
        List<String> targetList = new ArrayList<String> ();
        try {
            targetList = getApplicationTarget(URLDecoder.decode(appName, "UTF-8"), ref);
        } catch (Exception ex) {
            //ignore
        }
        for (String oneTarget : targetList) {
            if (clusters.contains(oneTarget)) {
                enabled = (String) RestUtil.getAttributesMap(prefix + "/clusters/cluster/" + oneTarget + "/" + ref + "/" + appName).get("enabled");
            } else {
                enabled = (String) RestUtil.getAttributesMap(prefix + "/servers/server/" + oneTarget + "/" + ref + "/" + appName).get("enabled");
            }
            if (Boolean.parseBoolean(enabled)) {
                numEnabled++;
            } else {
                numDisabled++;
            }
        }
                
        int numTargets = targetList.size();
        /*
        if (numEnabled == numTargets){
            return GuiUtil.getMessage("deploy.allEnabled");
        }
        if (numDisabled == numTargets){
            return GuiUtil.getMessage("deploy.allDisabled");
        }
         */
        return (numTargets==0) ?  GuiUtil.getMessage("deploy.noTarget") :
                GuiUtil.getMessage("deploy.someEnabled", new String[]{""+numEnabled, ""+numTargets });
        
    }

}
