/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.AdminCommandLock;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.internal.deployment.ApplicationLifecycleInterceptor;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ApplicationLifecycleInterceptorImpl implements ApplicationLifecycleInterceptor {

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private ServiceOrchestratorImpl serviceOrchestratorImpl;

    @Inject
    private ServerEnvironment serverEnvironment;


    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    //NOTE : refer & update isValidDeploymentTarget if needed as we are dependent on the list of "Origins" used in this method.
    public void before(final ExtendedDeploymentContext.Phase phase, final ExtendedDeploymentContext context) {

        if (isOrchestrationEnabled(context)) {
            logEvent(true, phase, context);
            AdminCommandLock.runWithSuspendedLock(new Runnable() {
                public void run() {
                    if (phase.equals(ExtendedDeploymentContext.Phase.PREPARE)) {
                        ReadableArchive archive = context.getSource();
                        OpsParams params = context.getCommandParameters(OpsParams.class);
                        String appName = params.name();
                        if (params.origin == OpsParams.Origin.deploy) {
                            serviceOrchestratorImpl.preDeploy(appName, context);
                        } else if (params.origin == OpsParams.Origin.load) {
                            if (params.command == OpsParams.Command.startup_server) {
                                if (isValidApplication(appName)) {
                                    serviceOrchestratorImpl.startup(appName, context);
                                }
                            } else {
                                if (params.command == OpsParams.Command.enable) {
                                    if (isValidApplication(appName)) {
                                        serviceOrchestratorImpl.enable(appName, context);
                                    }
                                }
                            }
                        }
                    } else if (phase.equals(ExtendedDeploymentContext.Phase.STOP)) {
                        OpsParams params = context.getCommandParameters(OpsParams.class);
                        String appName = params.name();
                        if (params.origin == OpsParams.Origin.undeploy) {
                            if (params.command == OpsParams.Command.disable) {
                                serviceOrchestratorImpl.preUndeploy(appName, context);
                            }
                        }
                    }
                }
            });
        }
    }

    //NOTE : refer & update isValidDeploymentTarget if needed as we are dependent on the list of "Origins" used in this method.
    public void after(final ExtendedDeploymentContext.Phase phase, final ExtendedDeploymentContext context) {

        if (isOrchestrationEnabled(context)) {
            logEvent(false, phase, context);
            AdminCommandLock.runWithSuspendedLock(new Runnable() {
                public void run() {
                    if (phase.equals(ExtendedDeploymentContext.Phase.REPLICATION)) {
                        OpsParams params = context.getCommandParameters(OpsParams.class);
                        ReadableArchive archive = context.getSource();
                        if (params.origin == OpsParams.Origin.deploy) {
                            String appName = params.name();
                            serviceOrchestratorImpl.postDeploy(appName, context);
                        }
                        //make sure that it is indeed undeploy and not disable.
                        //params.origin is "undeploy" for both "undeploy" as well "disable" phase
                        //hence using the actual command issued to confirm.
                        if (params.origin == OpsParams.Origin.undeploy) {
                            if (params.command == OpsParams.Command.undeploy) {
                                serviceOrchestratorImpl.undeploy(params, context);
                            }
                        }
                        //TODO as of today, we get only after-CLEAN-unload-disable event.
                        //TODO we expect after-STOP-unload-disable, but since the target is not "DAS",
                        //TODO DAS will not receive such event.
                        String appName = params.name();
                        if (params.origin == OpsParams.Origin.unload) {
                            if (params.command == OpsParams.Command.disable) {
                                if (isValidApplication(appName)) {
                                    serviceOrchestratorImpl.disable(appName, context);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean isValidApplication(String appName) {
        boolean isValid = true;
        //TODO check whether the application uses any <services> and then invoke orchestrator.
        //TODO this is needed as it is possible to deploy the application before enabling
        //TODO virtualization (add-virtualization).

        return isValid;
    }

    private void logEvent(boolean before, ExtendedDeploymentContext.Phase phase, ExtendedDeploymentContext context) {
        try{
            StringBuilder sb = new StringBuilder();
            if(before){
                sb.append("ServiceOrchestrator receiving event \n { [Before] ");
            }else{
                sb.append("ServiceOrchestrator receiving event \n { [After] ");
            }

            sb.append(" [Phase : "+phase.toString()+"]");
            if(context != null){
                OpsParams params = context.getCommandParameters(OpsParams.class);
                sb.append(" [Command : "+params.command+"]");
                sb.append(" [Origin : "+params.origin+"]");
            }else{
                sb.append(" [DeploymentContext is null, command and origin not available]");
            }
            sb.append(" }");
            logger.log(Level.FINEST, sb.toString());
        }catch(Exception e){
            //ignore, this is debugging info.
        }
    }

    private boolean isOrchestrationEnabled(DeploymentContext dc){
        return (serverEnvironment.isDas() && isVirtualizationEnabled() && isValidDeploymentTarget(dc)) ||
                Boolean.getBoolean("org.glassfish.paas.orchestrator.enabled");
    }

    private boolean isVirtualizationEnabled(){
        boolean isVirtualEnvironment = false;
        Virtualizations v = domain.getExtensionByType(Virtualizations.class);
        if (v!=null && v.getVirtualizations().size()>0) {
                isVirtualEnvironment = true;
        }
        return isVirtualEnvironment;
    }


    private boolean isValidDeploymentTarget(DeploymentContext dc) {
        if(dc == null){
            return false;
        }

        String target = null;
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if(params.origin == OpsParams.Origin.deploy || params.origin == OpsParams.Origin.load){
            DeployCommandParameters dcp = dc.getCommandParameters(DeployCommandParameters.class);
            target = dcp.target;
        }else  if(params.origin == OpsParams.Origin.undeploy || params.origin == OpsParams.Origin.unload){
            UndeployCommandParameters dcp = dc.getCommandParameters(UndeployCommandParameters.class);
            target = dcp.target;
        }else{
            return false;//we do not handle other "Origins" for now.
        }

        if(target == null){
            return true; // if target is null, we assume that its PaaS styled deployment.
        }

        Cluster cluster = domain.getClusterNamed(target);
        if(cluster != null){
            List<VirtualMachineConfig> vmcList = cluster.getExtensionsByType(VirtualMachineConfig.class);
            if(vmcList != null && vmcList.size()  > 0){
                return true;
            }else{
                return false; //not a virtual cluster.
            }
        }else{
            //target is not cluster or no such target exists.
            return false;
        }
    }
}