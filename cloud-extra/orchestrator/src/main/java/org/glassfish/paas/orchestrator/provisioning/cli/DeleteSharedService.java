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

package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.config.SharedService;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeEvent;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Jagadish Ramu
 */
@Service(name = "delete-shared-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "delete-shared-service", description = "Delete a shared service")
})
public class DeleteSharedService implements AdminCommand {

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private ServiceOrchestratorImpl serviceOrchestrator;

    private static Logger logger = LogDomains.getLogger(ServiceOrchestratorImpl.class,LogDomains.PAAS_LOGGER);

    private static StringManager localStrings = StringManager.getManager(ServiceOrchestratorImpl.class);

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        Services services = serviceUtil.getServices();
        boolean found = false;
        for (final org.glassfish.paas.orchestrator.config.Service service : services.getServices()) {
            if (service.getServiceName().equals(serviceName)) {
                if (service instanceof SharedService) {
                    found = true;
                    //check whether the service is in use by any application
                    List<String> applicationsUsingService =
                            serviceUtil.getApplicationsUsingService(service.getServiceName());
                    if(applicationsUsingService.size() > 0){
                        report.setMessage("The shared service [" + serviceName + "] is " +
                                "used by an application [" + applicationsUsingService.get(0) + "].");
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }
                    SharedService sharedService = (SharedService) service;
                    ProvisionedService provisionedService = serviceOrchestrator.getSharedService(sharedService.getServiceName());
                    ServicePlugin plugin = provisionedService.getServiceDescription().getPlugin();
                    DeploymentContext dc = null;
                    PaaSDeploymentContext pdc = new PaaSDeploymentContext(null, dc);
                    //we are caching service-info before unprovision just to make sure any Plugin
                    //does not remove the child services during unprovision.
                    ServiceInfo serviceInfo = serviceUtil.getServiceInfo(provisionedService.getName(), null);
                    plugin.unprovisionService(provisionedService.getServiceDescription(), pdc);
                    serviceOrchestrator.removeSharedService(sharedService.getServiceName());
                    serviceUtil.unregisterService(serviceInfo);
                    //TODO since the provisionedService is destroyed, it will not have all the original
                    //TODO information ?
                    serviceUtil.fireServiceChangeEvent(ServiceChangeEvent.Type.DELETED, provisionedService);

                    // delete virtual cluster
                    String virtualClusterName = service.getServiceName();
                    CommandResult result = commandRunner.run("delete-cluster", virtualClusterName);
                    Object[] args = new Object[] {virtualClusterName,result.getOutput()};
                    logger.log(Level.FINEST,localStrings.getString("delete.cluster.exec.output",args));

                    if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                        throw new RuntimeException("Failure while deleting virtual-cluster, " +
                                "Unable to delete virtual-cluster [" + virtualClusterName + "]");
                    }
                }
            }
        }
        if (!found) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No shared-service by name [" + serviceName + "] is available");
        }
    }
}
