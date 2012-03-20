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

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import com.sun.enterprise.config.serverbeans.Domain;

/**
 * Temproary hidden command to scale services
 * 
 * @author Sivakumar Thyagarajan
 */
@org.jvnet.hk2.annotations.Service(name = "_scale-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = { CommandTarget.DAS })
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({ @RestEndpoint(configBean = Domain.class, opType = OpType.GET, path = "_scale-service", description = "Scale Services") })
public class ScaleService implements AdminCommand {

    @Param(name = "appname", optional = true)
    private String appName;

    @Param(name = "servicename", optional = false)
    private String serviceName;

    @Param(name = "scalecount", optional = false)
    private int scaleCount;

    @Inject
    private ServiceOrchestrator orchestrator;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if(scaleCount == 0){
            report.setMessage("Invalid scale count.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        if (appName != null) {
            //TODO will "target" of application play a role here ? AFAIK, no.
            if (domain.getApplications().getApplication(appName) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed");
                return;
            }
        }
        
        Services services = serviceUtil.getServices();

        if (appName != null) {
            //check if appName is valid
            if (domain.getApplications().getApplication(appName) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed");
                return;
            }
            
            boolean match = false;

            //check if there is an application-scoped Service 
            for (Service service : services.getServices()) {
                if (service instanceof ApplicationScopedService) {
                    if ((appName.equals(((ApplicationScopedService) service).getApplicationName())) && 
                    (serviceName.equals(service.getServiceName()))){
                        match = true;
                    }
                 }
            }
            
            if (!match) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such service [" + serviceName + "" +
                		"for application [" + appName + "] is deployed");
                return;
            }
        } else {
            //check if there is an external or global service with that service name
            boolean match = false;
            for (Service service : services.getServices()) {
                if (!(service instanceof ApplicationScopedService)) {
                    if (serviceName.equals(service.getServiceName())){
                        match = true;
                    }
                 }
            }
            if (!match) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such service [" + serviceName + "]" +
                        " is deployed");
                return;
            }
            
        }
            
        orchestrator.scaleService(appName, serviceName, scaleCount, null);
        report.setMessage("Scale Service called.");
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        report.setActionExitCode(ec);
        
    }
}
