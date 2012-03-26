/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.state;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentException;
import org.glassfish.paas.orchestrator.PaaSDeploymentState;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
@Service
public class UndeployState extends AbstractPaaSDeploymentState {

    @Inject
    private CommandRunner commandRunner;

    public void handle(PaaSDeploymentContext context) throws PaaSDeploymentException {
        String appName = context.getAppName();
        boolean dasProvisioningEnabled = Boolean.getBoolean("org.glassfish.paas.provision-das");
        if(dasProvisioningEnabled){
            Set<org.glassfish.paas.orchestrator.service.spi.Service> allServices = appInfoRegistry.getServices(appName);
            for(org.glassfish.paas.orchestrator.service.spi.Service service : allServices){
                ServiceDescription sd = service.getServiceDescription();
                ServicePlugin plugin = sd.getPlugin();
                plugin.undeploy(context, service);
                //TODO atomic deployment support .
            }
        }else{
            ParameterMap parameterMap = new ParameterMap();
            parameterMap.add("DEFAULT", appName);
            parameterMap.add("properties", ServiceOrchestratorImpl.ORCHESTRATOR_UNDEPLOY_CALL +"=true");
            ActionReport report =  habitat.getComponent(ActionReport.class);
            CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("undeploy", report);
            invocation.parameters(parameterMap).execute();
            Object args[]=new Object[]{appName,report.getMessage()};
            logger.log(Level.FINEST, "undeploying.app",args);
        }
    }

    public Class<PaaSDeploymentState> getRollbackState() {
        return null;
    }
}
