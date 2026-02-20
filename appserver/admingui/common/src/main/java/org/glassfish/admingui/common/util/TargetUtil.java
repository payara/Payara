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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package org.glassfish.admingui.common.util;

import java.io.IOException;
import org.glassfish.admingui.common.handlers.RestUtilHandlers;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author anilam
 */
public class TargetUtil {

    public static boolean isInstance(String name){
        if (GuiUtil.isEmpty(name)){
            return false;
        }
        return getInstances().contains(name);
    }

    public static boolean isDeploymentGroup(String name) {
        if (GuiUtil.isEmpty(name)){
            return false;
        }
        return getDeploymentGroups().contains(name);
    }

    public static List<String> getStandaloneInstances(){
        List<String> result = new ArrayList<>();
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/list-instances" ;
        Map<String, Object> attrsMap = new HashMap<>();
        attrsMap.put("standaloneonly", "true");
        attrsMap.put("nostatus", "true");
        try{
            Map<String, Object> responseMap = RestUtil.restRequest( endpoint , attrsMap, "get" , null, false);
            Map<?, ?>  dataMap = (Map<?, ?>) responseMap.get("data");
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

    public static List<String> getDeploymentGroups() {
        List<String> dgs = new ArrayList<>();
        try{
            dgs.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/deployment-groups/deployment-group").keySet());
        }catch (Exception ex){
            GuiUtil.getLogger().log(Level.INFO, "{0}{1}", new Object[]{GuiUtil.getCommonMessage("log.error.getDeploymentGroups"), ex.getLocalizedMessage()});
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return dgs;
    }

    public static List<String> getInstances() {
        List<String> instances = new ArrayList<>();
        try{
            instances.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/servers/server").keySet());
        }catch (Exception ex){
            GuiUtil.getLogger().log(Level.INFO, "{0}{1}", new Object[]{GuiUtil.getCommonMessage("log.error.getInstances"), ex.getLocalizedMessage()});
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
        return instances;
    }

    public static List<String> getConfigs() {
        List<String> config = new ArrayList<>();
        try {
            config.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/configs/config").keySet());
        } catch (Exception ex) {
            GuiUtil.getLogger().log(Level.INFO, "{0}{1}", new Object[]{GuiUtil.getCommonMessage("log.error.getConfigs"), ex.getLocalizedMessage()});
            if (GuiUtil.getLogger().isLoggable(Level.FINE)) {
                ex.printStackTrace();
            }
        }
        if ((config.isEmpty())) {
            // Maybe Fix PAYARA-323
            GuiUtil.getLogger().warning("Detected Broken Session, forcing session reinitialisation");
            GuiUtil.setSessionValue("_SESSION_INITIALIZED", null);
        }
        return config;
    }

    public static List<String> getDGInstances(String dg) {
        List<String> instances = new ArrayList<>();
        try {
            instances.addAll(RestUtil.getChildMap(GuiUtil.getSessionValue("REST_URL") + "/deployment-groups/deployment-group/" + dg + "/dg-server-ref").keySet());
        } catch (Exception ex) {
            GuiUtil.getLogger().severe(ex.getMessage());
        }
        return instances;
    }

    public static String getTargetEndpoint(String target) {
        try {
            String encodedName = URLEncoder.encode(target, "UTF-8");
            String endpoint = (String) GuiUtil.getSessionValue("REST_URL");
            if (target.equals("server")) {
                endpoint = endpoint + "/servers/server/server";
            } else {
                List<String> dgs = TargetUtil.getDeploymentGroups();
                if (dgs.contains(target)) {
                    endpoint = endpoint + "/deployment-groups/deployment-group/" + encodedName;
                } else {
                    endpoint = endpoint + "/servers/server/" + encodedName;
                }
            }
            return endpoint;
        } catch (Exception ex) {
            GuiUtil.getLogger().log(Level.INFO, "{0}{1}", new Object[]{GuiUtil.getCommonMessage("log.error.getTargetEndpoint"),
                    ex.getLocalizedMessage()});
            if (GuiUtil.getLogger().isLoggable(Level.FINE)) {
                ex.printStackTrace();
            }
            return "";
        }
    }

    public static String getConfigName(String target) {
        List<String> dgs = TargetUtil.getDeploymentGroups();
        if (dgs.contains(target)) {
            // find the config of a server
            String endpoint = (String)GuiUtil.getSessionValue("REST_URL");
            try {
                List<String> targets = RestUtil.getChildResourceList(endpoint + "/deployment-groups/deployment-group/"+target+"/dg-server-ref");
                if (targets.size() >= 1) {
                    target = targets.get(0);
                } else {
                    return "server-config";
                }
            } catch (SAXException | IOException | ParserConfigurationException ex) {
                Logger.getLogger(TargetUtil.class.getName()).log(Level.SEVERE, null, ex);
                return "server-config";
            }
        }
        String endpoint = getTargetEndpoint(target);
        return (String)RestUtil.getAttributesMap(endpoint).get("configRef");
    }

    public static Collection<String> getHostNames(String target) {
        Set<String> hostNames = new HashSet<>();
        hostNames.toArray();
        List<String> instances = new ArrayList<>();
        instances.add(target);

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
