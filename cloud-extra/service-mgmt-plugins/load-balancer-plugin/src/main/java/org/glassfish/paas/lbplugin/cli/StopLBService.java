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

package org.glassfish.paas.lbplugin.cli;

import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.lbplugin.LBServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;

import static org.glassfish.paas.orchestrator.provisioning.cli.ServiceType.*;

import org.glassfish.paas.orchestrator.service.ServiceStatus;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.paas.lbplugin.LBProvisionerFactory;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;

/**
 * @author Jagadish Ramu
 */
@Service(name = "_stop-lb-service")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
public class StopLBService extends BaseLBService implements AdminCommand {

    @Param(name = "stopvm", optional = true)
    boolean stopVM;
    
    @Inject
    VirtualMachineLifecycle vmlifecycle;
    
    @Override
    public void execute(AdminCommandContext context) {

        LBPluginLogger.getLogger().log(Level.INFO,"_stop-lb-service called.");

        final ActionReport report = context.getActionReport();

        if (lbServiceUtil.isValidService(serviceName, appName)) {
            ServiceInfo entry = lbServiceUtil.retrieveCloudEntry(serviceName, appName);
            String ipAddress = entry.getIpAddress();
            String status = entry.getState();
            if (status == null || status.equalsIgnoreCase(ServiceStatus.STOP_IN_PROGRESS.toString())
                    || status.equalsIgnoreCase(ServiceStatus.NOT_RUNNING.toString())) {
                report.setMessage("Invalid lb-service [" + serviceName + "] state [" + status + "]");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            lbServiceUtil.updateState(serviceName, appName, ServiceStatus.STOP_IN_PROGRESS.toString());

            try {
                retrieveVirtualMachine();
                LBProvisionerFactory.getInstance().getLBProvisioner().stopLB(virtualMachine);
                lbServiceUtil.updateState(serviceName, appName, ServiceStatus.NOT_RUNNING.toString());
                if(stopVM){
                    vmlifecycle.stop(virtualMachine);
                }
                report.setMessage("lb-service [" + serviceName + "] stopped");
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            } catch (Exception ex) {
                LBPluginLogger.getLogger().log(Level.INFO,"exception",ex);
                lbServiceUtil.updateState(serviceName, appName, ServiceStatus.NOT_RUNNING.toString());
                report.setMessage("lb-service [" + serviceName + "] stop failed");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }

        } else {
            report.setMessage("Invalid lb-service name [" + serviceName + "]");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
