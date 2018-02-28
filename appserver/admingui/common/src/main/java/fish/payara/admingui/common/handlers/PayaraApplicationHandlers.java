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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

/**
 *
 * @author Susan Rai
 */
public class PayaraApplicationHandlers {

    @Handler(id = "py.getApplicationTargetList",
            input = {
                @HandlerInput(name = "appName", type = String.class, required = true)},
            output = {
                @HandlerOutput(name = "result", type = java.util.List.class)})
    public static void getTargetListInfo(HandlerContext handlerCtx) {
        String applicationName = (String) handlerCtx.getInputValue("appName");
        String prefix = (String) GuiUtil.getSessionValue("REST_URL");
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
                endpoint = prefix + "/clusters/cluster/" + oneTarget + "/application-ref/" + applicationName;
                attributes = RestUtil.getAttributesMap(endpoint);
                addToResult = true;
            } else if (standalone.contains(oneTarget) && !instancesInDeploymentGroup.contains(oneTarget)) {
                endpoint = prefix + "/servers/server/" + oneTarget + "/application-ref/" + applicationName;
                attributes = RestUtil.getAttributesMap(endpoint);
                addToResult = true;
            } else if (deploymentGroup.contains(oneTarget)) {
                endpoint = prefix + "/deployment-groups/deployment-group/" + oneTarget + "/application-ref/" + applicationName;
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
}
