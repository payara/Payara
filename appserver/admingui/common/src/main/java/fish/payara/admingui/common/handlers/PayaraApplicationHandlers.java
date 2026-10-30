/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glassfish.admingui.common.util.DeployUtil;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.admingui.common.util.TargetUtil;
import org.glassfish.admingui.common.util.RestResponse;
import static org.glassfish.admingui.common.util.RestUtil.get;

/**
 *
 * @author Susan Rai
 */
public class PayaraApplicationHandlers {
    
    private static final String CLUSTER = "/clusters/cluster/";
    private static final String SERVER = "/servers/server/";
    private static final String DEPLOYMENT_GROUP = "/deployment-groups/deployment-group/";
    private static final String APPLICATION_REF = "/application-ref/";
    private static final String RESOURCE_REF = "/resource-ref/";
    private static final String REST_URL = "REST_URL";
    private static final String RESOURCE_NAME = "resourceName";

    @Handler(id = "py.getApplicationTargetList",
            input = {
                @HandlerInput(name = "appName", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "result", type = List.class)})
    public static void getTargetListInfo(HandlerContext handlerCtx) {
        String applicationName = (String) handlerCtx.getInputValue("appName");
        String prefix = (String) GuiUtil.getSessionValue(REST_URL);
        
        List<String> clusters = TargetUtil.getClusters();
        List<String> standalone = TargetUtil.getStandaloneInstances();
        List<String> deploymentGroup = TargetUtil.getDeploymentGroups();
        
        standalone.add("server");
        List<String> targetList = DeployUtil.getApplicationTarget(applicationName, "application-ref");
        List<HashMap> result = new ArrayList<>();
        
        Map<String, Object> attributes = null;
        String endpoint = "";
        
        List<String> instancesInDeploymentGroup = getInstancesInDeploymentGroup(targetList);

        for (String oneTarget : targetList) {
            Boolean addToResult = false;
            HashMap<String, Object> oneRow = new HashMap<>();
            if (clusters.contains(oneTarget)) {
                endpoint = prefix + CLUSTER + oneTarget + APPLICATION_REF + applicationName;
                attributes = RestUtil.getAttributesMap(endpoint);
                addToResult = true;
            } else if (standalone.contains(oneTarget) && !instancesInDeploymentGroup.contains(oneTarget)) {
                endpoint = prefix + SERVER + oneTarget + APPLICATION_REF + applicationName;
                attributes = RestUtil.getAttributesMap(endpoint);
                addToResult = true;
            } else if (deploymentGroup.contains(oneTarget)) {
                endpoint = prefix + DEPLOYMENT_GROUP + oneTarget + APPLICATION_REF + applicationName;
                attributes = RestUtil.getAttributesMap(endpoint);
                addToResult = true;
            }
            if (addToResult) {
                oneRow.put("name", applicationName);
                oneRow.put("selected", false);
                oneRow.put("endpoint", endpoint.replaceAll("/application-ref/.*", "/update-application-ref"));
                oneRow.put("targetName", oneTarget);
                oneRow.put("enabled", attributes.get("enabled"));
                oneRow.put("lbEnabled", attributes.get("lbEnabled"));
                result.add(oneRow);
            }
        }
        handlerCtx.setOutputValue("result", result);
    }

    public static List<String> getInstancesInDeploymentGroup(List<String> targetList) {
        List<String> listOfInstancesInDeploymentGroup = new ArrayList<>();
        for (String oneTarget : targetList) {
            if (TargetUtil.isDeploymentGroup(oneTarget)) {
                List<String> instancesInDeploymentGroup = TargetUtil.getDGInstances(oneTarget);
                for (String instance : instancesInDeploymentGroup) {
                    listOfInstancesInDeploymentGroup.add(instance);
                }
            }
        }
        return listOfInstancesInDeploymentGroup;
    }

    @Handler(id = "py.getAllSelectedTarget",
            input = {
                @HandlerInput(name = "targetList", type = List.class, required = true),
                @HandlerInput(name = "resourceName", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "selectedTarget", type = List.class)
            })
    public static void getAllSelectedTarget(HandlerContext handlerCtx) {
        String prefix = (String) GuiUtil.getSessionValue(REST_URL);

        List<String> targetList = (List) handlerCtx.getInputValue("targetList");
        String resourceName = (String) handlerCtx.getInputValue(RESOURCE_NAME);
        List<String> selectedTargetList = new ArrayList<>();
        String endpoint;

        for (String targetName : targetList) {
            endpoint = prefix + CLUSTER + targetName + RESOURCE_REF + resourceName;
            boolean existsInCluster = isEndpointValid(endpoint);

            if (!existsInCluster) {
                endpoint = prefix + DEPLOYMENT_GROUP + targetName + RESOURCE_REF + resourceName;
                boolean existsInDeploymentGroup = isEndpointValid(endpoint);
                if (!existsInDeploymentGroup) {
                    endpoint = prefix + SERVER + targetName + RESOURCE_REF + resourceName;
                    boolean existsInServer = isEndpointValid(endpoint);
                    if (existsInServer) {
                        selectedTargetList.add(targetName);
                    }
                }
                if (existsInDeploymentGroup) {
                    selectedTargetList.add(targetName);
                }
            }

            if (existsInCluster) {
                selectedTargetList.add(targetName);
            }
        }

        handlerCtx.setOutputValue("selectedTarget", selectedTargetList);
    }

    private static boolean isEndpointValid(String endpoint) {
        boolean result = false;
        RestResponse response = null;

        try {
            response = get(endpoint);
            result = response.isSuccess();

        } catch (Exception exception) {
            GuiUtil.getLogger().info("Invalid endpoint" + exception);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return result;

    }
 
    @Handler(id = "py.checkIfResourceIsInInstance",
            input = {
                @HandlerInput(name = "instanceName", type = String.class, required = true),
                @HandlerInput(name = "resourceName", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "isPresent", type = Boolean.class)})
    public static void isResourceInInstance(HandlerContext handlerCtx) {
        String instanceName = (String) handlerCtx.getInputValue("instanceName");
        String resourceName = (String) handlerCtx.getInputValue(RESOURCE_NAME);
        String prefix = (String) GuiUtil.getSessionValue(REST_URL);
        String endpoint = prefix + SERVER + instanceName + RESOURCE_REF;

        boolean isPresent = false;
        Map responseMap = RestUtil.restRequest(endpoint, null, "GET", handlerCtx, false, true);
        Map data = (Map) responseMap.get("data");

        if (data != null) {
            Map extraProperties = (Map) data.get("extraProperties");
            if (extraProperties != null) {
                Map childResources = (Map) extraProperties.get("childResources");
                List<String> listOfResources = new ArrayList<String>(childResources.keySet());
                
                if (listOfResources.contains(resourceName)) {
                    isPresent = true;
                }
            }
        }
        
        handlerCtx.setOutputValue("isPresent", isPresent);
    }
    
    
    @Handler(id = "py.isInstanceInDeploymentGroupWithResource",
            input = {
                @HandlerInput(name = "selectedTarget", type = List.class, required = true),
                @HandlerInput(name = "target", type = String.class, required = true),
                @HandlerInput(name = "resourceName", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "isPresent", type = Boolean.class)})
    public static void isInstanceInDeploymentGroupWithResource(HandlerContext handlerCtx) {
        List<String> selectedTargetList = (List) handlerCtx.getInputValue("selectedTarget");
        String target = (String) handlerCtx.getInputValue("target");
        String prefix = (String) GuiUtil.getSessionValue(REST_URL);
        String resourceName = (String) handlerCtx.getInputValue(RESOURCE_NAME);
        String endpoint;
        boolean isPresent = false;

        for (String selectedTarget : selectedTargetList) {
            if (TargetUtil.isDeploymentGroup(selectedTarget)) {
                List<String> instancesInDeploymentGroup = TargetUtil.getDGInstances(selectedTarget);

                for (String instance : instancesInDeploymentGroup) {
                    if (instance.equals(target)) {
                        endpoint = prefix + DEPLOYMENT_GROUP + selectedTarget + RESOURCE_REF + resourceName;
                        if (isEndpointValid(endpoint)) {
                            isPresent = true;
                            break;
                        }
                    }
                }
            }
        }

        handlerCtx.setOutputValue("isPresent", isPresent);
    }
}