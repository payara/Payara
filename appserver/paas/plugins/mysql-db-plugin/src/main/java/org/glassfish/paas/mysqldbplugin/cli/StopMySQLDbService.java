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

package org.glassfish.paas.mysqldbplugin.cli;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;

import static org.glassfish.paas.orchestrator.provisioning.ServiceInfo.State.*;
import static org.glassfish.paas.orchestrator.provisioning.cli.ServiceType.*;

import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.spi.VirtualCluster;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * @author Shalini M
 */
@Service(name = "_stop-mysql-db-service")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class StopMySQLDbService implements AdminCommand {

    @Param(name = "stopvm", optional = true)
    boolean stopVM;

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Param(name = "appname", optional = true)
    private String appName;

    @Param(name = "virtualcluster", optional = true)
    private String virtualClusterName;

    @Inject
    ServiceUtil serviceUtil;

    @Inject(optional = true)
    VirtualClusters virtualClusters;

    VirtualCluster virtualCluster;
    VirtualMachine virtualMachine;

    private static Logger logger = Logger.getLogger(StopMySQLDbService.class.getName());

    @Override
    public void execute(AdminCommandContext context) {

        logger.entering(getClass().getName(), "execute");
        final ActionReport report = context.getActionReport();
        // Check if the service is already configured.
        if (serviceUtil.isValidService(serviceName, appName, DATABASE)) {
            synchronized (StopMySQLDbService.class) {
                ServiceInfo entry = serviceUtil.getServiceInfo(serviceName, appName, DATABASE);
                String ipAddress = entry.getIpAddress();
                String status = entry.getState();
                try {
                    if(status == null || status.equalsIgnoreCase(ServiceInfo.State.NotRunning.toString())) {
                        report.setMessage("MySQL-db-service [" + serviceName + "] already stopped");
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        logger.log(Level.WARNING, "MySQL DB Service already stopped");
                        return;
                    }
                    retrieveVirtualMachine();
                    if(stopVM) {
                        if(virtualMachine != null) {
                            virtualMachine.stop();
                        }
                    }
                    serviceUtil.updateState(serviceName, appName, NotRunning.toString(), DATABASE);
                    report.setMessage("mysql-db-service [" + serviceName + "] stopped");
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    logger.log(Level.INFO, "Stopped MySQL DB Service successfully");
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Exception  while stopping mysql db service : " + ex);
                    report.setMessage("MySQL DB service [" + serviceName + "] stop failed");
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                }
            }
        } else {
            report.setMessage("Invalid mysql-db-service name [" + serviceName + "]");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            logger.log(Level.WARNING, "Invalid MySQL DB Service name : " + serviceName);
        }
    }

    private void retrieveVirtualMachine() throws VirtException {
        if (virtualClusters != null && serviceName != null && virtualClusterName != null) {
            virtualCluster = virtualClusters.byName(virtualClusterName);
            String vmId = serviceUtil.getInstanceID(serviceName, appName, DATABASE);
            if (vmId != null) {
                logger.log(Level.INFO, "Found MySQL DB VM with id : " + vmId);
                virtualMachine = virtualCluster.vmByName(vmId);
                // TODO :: IMS should give differnt way to get hold of VM using the vmId
                return;
            }
            logger.log(Level.WARNING, "Unable to find VirtualMachine for mysql db with vmId : " + vmId);
        } else {
            logger.log(Level.WARNING, "Unable to find VirtualMachine for mysql db as " +
                    "virtualClusters or serviceName is null");
        }
    }

}
