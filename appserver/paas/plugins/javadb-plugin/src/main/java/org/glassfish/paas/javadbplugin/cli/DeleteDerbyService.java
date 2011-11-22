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

package org.glassfish.paas.javadbplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.javadbplugin.DerbyProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtualMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jagadish Ramu
 * @author Shalini M
 */
@Service(name = "_delete-derby-service")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class DeleteDerbyService implements AdminCommand {

    @Param(name = "servicename", primary = true, optional = false)
    private String serviceName;

    @Inject
    private ProvisionerUtil provisionerUtil;

    @Inject
    private DatabaseServiceUtil dbServiceUtil;

    @Param(name = "appname", optional = true)
    private String appName;

    @Param(name = "waitforcompletion", optional = true, defaultValue = "false")
    private boolean waitforcompletion;

    @Param(name="virtualcluster", optional=true)
    private String virtualClusterName;
    
    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    // TODO :: remove dependency on VirtualCluster(s).
    @Inject(optional = true) // made it optional for non-virtual scenario to work
    VirtualClusters virtualClusters;

    @Inject(optional=true)
    VirtualMachineLifecycle vmLifecycle;

    @Inject
    private DerbyProvisioner derbyProvisioner;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        // Check if the service is already configured.
        if (!dbServiceUtil.isServiceAlreadyConfigured(serviceName, appName, ServiceType.DATABASE)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Service with name [" + serviceName + "] is not available.");
            return;
        }

        if (templateRepository != null && virtualClusters != null) { // we are in virtualized environment.
            if (virtualClusters != null && serviceName != null) {
                try {
                    VirtualCluster virtualCluster = virtualClusters.byName(virtualClusterName);
                    String vmId = dbServiceUtil.getInstanceID(serviceName, appName, ServiceType.DATABASE);
                    VirtualMachine vm = virtualCluster.vmByName(vmId);
                    derbyProvisioner.stopDatabase(vm);
                    vmLifecycle.delete(vm);
                    dbServiceUtil.unregisterCloudEntry(serviceName, appName);
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    report.setMessage("Service with name [" +
                            serviceName + "] is decommissioned successfully.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("deleting service [" + serviceName + "] failed");
                    report.setFailureCause(ex);
                }
            }
            return;
        }else{
            try{
                //local mode related functionality.
                String ipAddress = dbServiceUtil.getIPAddress(serviceName, appName, ServiceType.DATABASE);

                dbServiceUtil.updateState(serviceName, appName,
                ServiceInfo.State.Stop_in_progress.toString(), ServiceType.DATABASE);

                DatabaseProvisioner dbProvisioner = provisionerUtil.getDatabaseProvisioner();
                dbProvisioner.stopDatabase(ipAddress);
                dbServiceUtil.updateState(serviceName, appName,
                ServiceInfo.State.NotRunning.toString(), ServiceType.DATABASE);

                CloudProvisioner cloudProvisioner = provisionerUtil.getCloudProvisioner();
                String instanceID = dbServiceUtil.getInstanceID(serviceName, appName, ServiceType.DATABASE);
                List<String> instanceIDs = new ArrayList<String>();
                instanceIDs.add(instanceID);
                cloudProvisioner.deleteInstances(instanceIDs);
                dbServiceUtil.unregisterCloudEntry(serviceName, appName);
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                report.setMessage("Service with name [" +
                        serviceName + "] is decommissioned successfully.");

            } catch (Exception e) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("deleting service [" + serviceName + "] failed");
                report.setFailureCause(e);
            }
        }
    }
}
