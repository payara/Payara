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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.common.util;

import org.glassfish.admingui.common.handlers.RestUtilHandlers;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author anilam
 */
public class TargetUtil {

    public static boolean isCluster(String name){
        if (GuiUtil.isEmpty(name)){
            return false;
        }
        return getClusters().contains(name);
    }

    public static boolean isInstance(String name){
        if (GuiUtil.isEmpty(name)){
            return false;
        }
        return getInstances().contains(name);
    }

    public static List getStandaloneInstances(){
        List<String> result = new ArrayList<String>();
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/list-instances" ;
        Map attrsMap = new HashMap();
        attrsMap.put("standaloneonly", "true");
        try{
            Map responseMap = RestUtil.restRequest( endpoint , attrsMap, "get" , null, false);
            Map  dataMap = (Map) responseMap.get("data");
            Map<String, Object>  extraProps = (Map<String, Object>) dataMap.get("extraProperties");
            if (extraProps == null){
                return result;
            }
            List<Map<String, String>> props = (List<Map<String, String>>) extraProps.get("instanceList");            
            if (props == null){
                return result;
            }
            result = RestUtilHandlers.getListFromMapKey(props);
        }catch (Exception ex){
            GuiUtil.getLogger().severe("Error in getStandaloneInstances ; \nendpoint = " +endpoint + ", attrsMap=" + attrsMap);
        }

        return result;
    }

    public static List getClusters(){
        List clusters = new ArrayList();
        try{
            clusters.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster").keySet());
        }catch (Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getClusters") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return clusters;
    }

    public static List getInstances(){
        List instances = new ArrayList();
        try{
            instances.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/servers/server").keySet());
        }catch (Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getInstances") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return instances;
    }

    public static List getConfigs(){
        List config = new ArrayList();
        try{
            config.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/configs/config").keySet());
        }catch (Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getClusters") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return config;
    }

    public static List getClusteredInstances(String cluster) {
        List instances = new ArrayList();
        try {
            instances.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster/" + cluster + "/server-ref").keySet());
        } catch (Exception ex) {
            GuiUtil.getLogger().severe(ex.getMessage());
        }
        return instances;
    }

    public static String getTargetEndpoint(String target){
        try{
            String encodedName = URLEncoder.encode(target, "UTF-8");
            String endpoint = (String)GuiUtil.getSessionValue("REST_URL");
            if (target.equals("server")){
                endpoint = endpoint + "/servers/server/server";
            }else{
                List clusters = TargetUtil.getClusters();
                if (clusters.contains(target)){
                    endpoint = endpoint + "/clusters/cluster/" + encodedName;
                }else{
                    endpoint = endpoint + "/servers/server/" + encodedName;
                }
            }
            return endpoint;
        }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getTargetEndpoint") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
            return "";
        }
    }

    public static String getConfigName(String target) {
        String endpoint = getTargetEndpoint(target);
        return (String)RestUtil.getAttributesMap(endpoint).get("configRef");
    }

    public static Collection<String> getHostNames(String target) {
        Set<String> hostNames = new HashSet();
        hostNames.toArray();
        List clusters = TargetUtil.getClusters();
        List<String> instances = new ArrayList();
        if (clusters.contains(target)){
             instances = getClusteredInstances(target);
        } else {
            instances.add(target);
        }

        for (String instance : instances) {
            String hostName = null;
            String ep = (String)GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + instance;
            String node =
                    (String)RestUtil.getAttributesMap(ep).get("nodeRef");
            if (node != null) {
                ep = (String)GuiUtil.getSessionValue("REST_URL") + "/nodes/node/" + node;
                hostName =  (String)RestUtil.getAttributesMap(ep).get("nodeHost");
            }
            if (hostName == null)
                hostName = "localhost";
            hostNames.add(hostName);
        }
        return hostNames;
    }
}
