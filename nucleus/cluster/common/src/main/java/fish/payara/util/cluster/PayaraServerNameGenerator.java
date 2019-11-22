/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.util.cluster;

import com.sun.enterprise.admin.report.DoNothingActionReporter;
import fish.payara.api.admin.config.NameGenerator;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.internal.api.Globals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class to generate random names for Payara Server instances.
 * <p>
 * There are over 14,000 different possible names.
 * @author Andrew Pielage
 */
public final class PayaraServerNameGenerator {

    public static String validateInstanceNameUnique(String instanceName, AdminCommandContext context) {
        List<String> namesInUse = getAllNamesInUse(context);
        return validateInstanceNameUnique(instanceName, namesInUse);
    }

    public static String validateInstanceNameUnique(String instanceName, List<String> namesInUse) {
        if (!namesInUse.contains(instanceName)) {
            return instanceName;
        }

        return validateInstanceNameUnique(instanceName + "-" + generateNameNoHyphen(), namesInUse);
    }

    public static String generateNameNoHyphen() {
        int adjectivesIndex = ThreadLocalRandom.current().nextInt(0, NameGenerator.adjectives.length);
        int fishIndex = ThreadLocalRandom.current().nextInt(0, NameGenerator.fishes.length);

        return NameGenerator.adjectives[adjectivesIndex] + NameGenerator.fishes[fishIndex];
    }

    private static List<String> getAllNamesInUse(AdminCommandContext context) {
        List<String> namesInUse = new ArrayList<>();
        CommandRunner commandRunner = Globals.getDefaultBaseServiceLocator().getService(CommandRunner.class);

        namesInUse.addAll(getInstanceNames(new DoNothingActionReporter(), context, commandRunner));
        namesInUse.addAll(getNodeNames(new DoNothingActionReporter(), context, commandRunner));
        namesInUse.addAll(getClusterNames(new DoNothingActionReporter(), context, commandRunner));
        namesInUse.addAll(getDeploymentGroupNames(new DoNothingActionReporter(), context, commandRunner));
        namesInUse.addAll(getConfigNames(new DoNothingActionReporter(), context, commandRunner));

        return namesInUse;
    }

    private static List<String> getInstanceNames(ActionReport report, AdminCommandContext context,
            CommandRunner commandRunner) {
        List<String> instanceNames = new ArrayList<>();

        CommandRunner.CommandInvocation listInstancesCommand = commandRunner.getCommandInvocation("list-instances",
                report, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("nostatus", "true");
        listInstancesCommand.parameters(commandParameters);
        listInstancesCommand.execute();
        Properties extraProperties = listInstancesCommand.report().getExtraProperties();
        if (extraProperties != null) {
            List<Map<String, String>> instanceList = (List<Map<String, String>>) extraProperties.get("instanceList");
            for (Map<String, String> instanceMap : instanceList) {
                instanceNames.add(instanceMap.get("name"));
            }
        }

        return instanceNames;
    }

    private static List<String> getNodeNames(ActionReport report, AdminCommandContext context,
            CommandRunner commandRunner) {
        List<String> nodeNames = new ArrayList<>();

        CommandRunner.CommandInvocation listNodesCommand = commandRunner.getCommandInvocation("list-nodes", report,
                context.getSubject());
        listNodesCommand.execute();
        Properties extraProperties = listNodesCommand.report().getExtraProperties();
        if (extraProperties != null) {
            nodeNames.addAll((List<String>) extraProperties.get("nodeNames"));
        }

        return nodeNames;
    }

    private static List<String> getClusterNames(ActionReport report, AdminCommandContext context,
            CommandRunner commandRunner) {
        List<String> clusterNames = new ArrayList<>();

        CommandRunner.CommandInvocation listClustersCommand = commandRunner.getCommandInvocation("list-clusters",
                report, context.getSubject());
        listClustersCommand.execute();
        Properties extraProperties = listClustersCommand.report().getExtraProperties();
        if (extraProperties != null) {
            clusterNames.addAll((Set<String>) extraProperties.get("clusterNames"));
        }

        return clusterNames;
    }

    private static List<String> getDeploymentGroupNames(ActionReport report, AdminCommandContext context,
            CommandRunner commandRunner) {
        List<String> deploymentGroupNames = new ArrayList<>();

        CommandRunner.CommandInvocation listDeploymentGroupsCommand = commandRunner.getCommandInvocation(
                "list-deployment-groups", report, context.getSubject());
        listDeploymentGroupsCommand.execute();
        Properties extraProperties = listDeploymentGroupsCommand.report().getExtraProperties();
        if (extraProperties != null) {
            deploymentGroupNames.addAll((List<String>) extraProperties.get("listOfDeploymentGroups"));
        }

        return deploymentGroupNames;
    }

    private static List<String> getConfigNames(ActionReport report, AdminCommandContext context,
            CommandRunner commandRunner) {
        List<String> configNames = new ArrayList<>();

        CommandRunner.CommandInvocation listConfigsCommand = commandRunner.getCommandInvocation("list-configs",
                report, context.getSubject());
        listConfigsCommand.execute();
        Properties extraProperties = listConfigsCommand.report().getExtraProperties();
        if (extraProperties != null) {
            configNames.addAll((List<String>) extraProperties.get("configNames"));
        }

        return configNames;
    }


}
