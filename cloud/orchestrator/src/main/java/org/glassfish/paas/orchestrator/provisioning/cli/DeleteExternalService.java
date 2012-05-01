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
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.ExternalService;
import org.glassfish.paas.orchestrator.config.Services;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.List;

/**
 * @author Jagadish Ramu
 */
@Service(name = "delete-external-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "delete-external-service", description = "Delete an external service")
})
public class DeleteExternalService implements AdminCommand {

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private ServiceOrchestratorImpl serviceOrchestrator;


    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Services services = serviceUtil.getServices();
        boolean found = false;
        for (final org.glassfish.paas.orchestrator.config.Service service : services.getServices()) {
            if (service.getServiceName().equals(serviceName)) {
                if (service instanceof ExternalService) {
                    found = true;

                    //check whether the service is in use by any application
                    List<String> applicationsUsingService =
                            serviceUtil.getApplicationsUsingService(service.getServiceName());
                    if(applicationsUsingService.size() > 0){
                        report.setMessage("The external service [" + serviceName + "] is " +
                                "used by an application [" + applicationsUsingService.get(0) + "].");
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }

                    try {
                        if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                            public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                                param.getServices().remove(service);
                                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                                serviceOrchestrator.removeExternalService(serviceName);
                                return service;
                            }
                        }, services) == null) {
                        }
                    } catch (TransactionFailure e) {
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("Deleting external-service [" + serviceName + "] failed : " + e.getMessage());
                        report.setFailureCause(e);
                        return;
                    }
                }
            }
        }
        if (!found) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No external-service by name [" + serviceName + "] is available");
        }
    }
}
