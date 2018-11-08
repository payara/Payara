/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.admin.util.InstanceStateService;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.*;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * A ClusterExecutor is responsible for remotely executing commands.
 * The list of target servers (either clusters or remote instances) is obtained
 * from the parameter list.
 *
 * @author Vijay Ramachandran
 */
@Service(name="GlassFishClusterExecutor")
public class GlassFishClusterExecutor implements ClusterExecutor {

    @Inject
    private Domain domain;

    @Inject
    private ExecutorService threadExecutor;

    @Inject
    private InstanceStateService instanceState;

    @Inject
    private Target targetService;

    @Inject
    private ServiceLocator habitat;

    private static final LocalStringManagerImpl STRINGS = new LocalStringManagerImpl(GlassFishClusterExecutor.class);

    /**
     * <p>Execute the passed command on targeted remote instances. The list of remote
     * instances is usually retrieved from the passed parameters (with a "target"
     * parameter for instance) or from the configuration.
     *
     * <p>Each remote execution must return a different ActionReport so the user
     * or framework can get feedback on the success or failure or such executions.
     *
     * @param commandName the command to execute
     * @param context the original command context
     * @param parameters the parameters passed to the original local command
     * @return an array of @{link org.glassfish.api.ActionReport} for each remote
     * execution status. 
     */
    @Override
    public ActionReport.ExitCode execute(String commandName, AdminCommand command, AdminCommandContext context, ParameterMap parameters) {

        CommandModel model =
            command instanceof CommandModelProvider ?
                ((CommandModelProvider)command).getModel() :
                new CommandModelImpl(command.getClass());
        
        org.glassfish.api.admin.ExecuteOn clAnnotation = model.getClusteringAttributes();
        List<RuntimeType> runtimeTypes = new ArrayList<RuntimeType>();
        @ExecuteOn final class DefaultExecuteOn {}
        if(clAnnotation == null) {
            clAnnotation = DefaultExecuteOn.class.getAnnotation(ExecuteOn.class);
        }
        if (clAnnotation.value().length == 0) {
            runtimeTypes.add(RuntimeType.DAS);
            runtimeTypes.add(RuntimeType.INSTANCE);
        } else {
            runtimeTypes.addAll(Arrays.asList(clAnnotation.value()));
        }
        
        String targetName = parameters.getOne("target");
        if(targetName == null)
                targetName = "server";
        //Do replication only if the RuntimeType specified is ALL or
        //only if the target is not "server" or "domain"
        if( (runtimeTypes.contains(RuntimeType.ALL)) ||
            ((!CommandTarget.DAS.isValid(habitat, targetName)) && (!CommandTarget.DOMAIN.isValid(habitat, targetName))) ) {
            //If the target is a cluster and dynamic reconfig enabled is false and RuntimeType is not ALL, no replication
            if (targetService.isCluster(targetName) && !runtimeTypes.contains(RuntimeType.ALL)) {
                String dynRecfg = targetService.getClusterConfig(targetName).getDynamicReconfigurationEnabled();
                if(Boolean.FALSE.equals(Boolean.valueOf(dynRecfg))) {
                    ActionReport aReport = context.getActionReport().addSubActionsReport();
                    aReport.setActionExitCode(ActionReport.ExitCode.WARNING);
                    aReport.setMessage(STRINGS.getLocalString("glassfish.clusterexecutor.dynrecfgdisabled",
                            "WARNING: The command was not replicated to all cluster instances because the" +
                                    " dynamic-reconfig-enabled flag is set to false for cluster {0}", targetName));
                    for(Server s : targetService.getInstances(targetName)) {
                        instanceState.setState(s.getName(), InstanceState.StateType.RESTART_REQUIRED, false);
                        instanceState.addFailedCommandToInstance(s.getName(), commandName, parameters);
                    }
                    return ActionReport.ExitCode.WARNING;
                }
            }

            List<Server> instancesForReplication = new ArrayList<Server>();

            if (runtimeTypes.contains(RuntimeType.ALL)) {
                List<Server> allInstances = targetService.getAllInstances();
                Set<String> clusterNoReplication = new HashSet<String>();
                for (Server s : allInstances) {
                    String dynRecfg = s.getConfig().getDynamicReconfigurationEnabled();
                    if (Boolean.TRUE.equals(Boolean.valueOf(dynRecfg))) {
                        instancesForReplication.add(s);
                    } else {
                        clusterNoReplication.add(s.getCluster().getName());
                        instanceState.setState(s.getName(), InstanceState.StateType.RESTART_REQUIRED, false);
                        instanceState.addFailedCommandToInstance(s.getName(), commandName, parameters);
                    }
                }

                if (!clusterNoReplication.isEmpty()) {
                    ActionReport aReport = context.getActionReport().addSubActionsReport();
                    aReport.setActionExitCode(ActionReport.ExitCode.WARNING);
                    aReport.setMessage(STRINGS.getLocalString("glassfish.clusterexecutor.dynrecfgdisabled",
                            "WARNING: The command was not replicated to all cluster instances because the"
                            + " dynamic-reconfiguration-enabled flag is set to false for cluster(s) {0}", clusterNoReplication));
                }

            } else {
                instancesForReplication = targetService.getInstances(targetName);
            }

            if(instancesForReplication.isEmpty()) {
                ActionReport aReport = context.getActionReport().addSubActionsReport();
                aReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                aReport.setMessage(STRINGS.getLocalString("glassfish.clusterexecutor.notargets",
                        "Did not find any suitable instances for target {0}; command executed on DAS only", targetName));
                return ActionReport.ExitCode.SUCCESS;
            }

            return(ClusterOperationUtil.replicateCommand(commandName, clAnnotation.ifFailure(),
                    clAnnotation.ifOffline(), clAnnotation.ifNeverStarted(),
                    instancesForReplication, context, parameters, habitat));
        }
        return ActionReport.ExitCode.SUCCESS;
    }
}
