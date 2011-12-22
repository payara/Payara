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

package org.glassfish.paas.orchestrator.provisioning.cli;


import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.config.SharedService;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * @author: Sandhya Kripalani K
 */


@Service(name = "start-shared-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "start-shared-service", description = "Start a shared service")
})
public class StartSharedService implements AdminCommand {

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private ServiceOrchestratorImpl orchestrator;


    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        boolean sharedServiceFound = false;

        Services services = domain.getExtensionByType(Services.class);
        if (services != null) {
            for (org.glassfish.paas.orchestrator.config.Service service : services.getServices()) {
                if (service instanceof SharedService) {
                    SharedService sharedService = (SharedService) service;
                    String serviceType = sharedService.getType();
                    if (sharedService.getServiceName().equalsIgnoreCase(serviceName)) {
                        sharedServiceFound = true;
                        if (sharedService.getState().equalsIgnoreCase(ServiceStatus.RUNNING.toString())) {
                            report.setMessage("A shared service by name [" + sharedService.getServiceName() + "] is already running");
                            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                            return;
                        } else {
                            ServiceInfo serviceInfo = serviceUtil.getServiceInfo(serviceName, null, null);
                            ServiceDescription serviceDescription = orchestrator.getSharedServiceDescription(serviceName);
                            ServicePlugin plugin = serviceDescription.getPlugin();
                            /*ServiceDescription serviceDescription = serviceUtil.getSharedServiceDescription(serviceInfo);
                            Plugin plugin = orchestrator.getPlugin(serviceDescription);*/
                            ProvisionedService provisionedService = plugin.startService(serviceDescription, serviceInfo);
                            if (provisionedService != null) {
                                orchestrator.addSharedService(serviceName, provisionedService);
                                try {
                                    if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                                        public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                                            for (org.glassfish.paas.orchestrator.config.Service service : param.getServices()) {
                                                if (service instanceof SharedService) {
                                                    SharedService sharedSvc = (SharedService) service;
                                                    if (sharedSvc.getServiceName().equalsIgnoreCase(serviceName)) {
                                                        Transaction t = Transaction.getTransaction(param);
                                                        SharedService sharedService_w = t.enroll(sharedSvc);
                                                        sharedService_w.setState(ServiceStatus.RUNNING.toString());
                                                    }
                                                }
                                            }
                                            return param;
                                        }
                                    }, services) == null) {
                                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                        report.setMessage("Unable to start the shared service [" + serviceName + "]");
                                    } else {
                                        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                                        return;
                                    }

                                } catch (TransactionFailure transactionFailure) {
                                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                    report.setFailureCause(transactionFailure);
                                    return;

                                }

                            } else {
                                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                report.setMessage("Unable to start the shared service [" + serviceName + "]");
                                return;
                            }

                        }
                    }
                }
            }
            if (!sharedServiceFound) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("A shared service by name [" + serviceName + "] does not exist.");
                return;
            }
        } else { //Shared Service does not exist..
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("A shared service by name [" + serviceName + "] does not exist.");
            return;
        }


    }
}
