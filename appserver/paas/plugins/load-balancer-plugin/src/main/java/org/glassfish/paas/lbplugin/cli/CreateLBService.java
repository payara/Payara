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
package org.glassfish.paas.lbplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.paas.gfplugin.cli.GlassFishServiceUtil;
import org.glassfish.paas.lbplugin.LBServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.*;

import static org.glassfish.paas.orchestrator.provisioning.ServiceInfo.State.*;

import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;

import static org.glassfish.paas.orchestrator.provisioning.cli.ServiceType.*;

import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.paas.lbplugin.GlassFishLBProvisioner;
import org.glassfish.paas.orchestrator.provisioning.LBProvisioner;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * @author Jagadish Ramu
 */
@Service(name = "_create-lb-service")
@Scoped(PerLookup.class)
public class CreateLBService implements AdminCommand {

    @Param(name = "servicename", primary = true, optional = false)
    private String serviceName;

    @Param(name = "appserver_servicename", optional = true)
    private String appServerServiceName;

    @Param(name = "_ignore_appserver_association", optional = true, defaultValue = "false")
    private boolean _ignoreAppServerAssociation;

    @Param(name="appname", optional = true)
    private String appName;

    @Inject
    private ProvisionerUtil provisionerUtil;

    @Inject
    private LBServiceUtil lbServiceUtil;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        // Check if the service is already configured.
        if (lbServiceUtil.isServiceAlreadyConfigured(serviceName, appName,  LOAD_BALANCER)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Service with name [" + serviceName + "] is already configured.");
            return;
        }

        String domainName = null;
        String targetName = null;

        if (!_ignoreAppServerAssociation) {
            if (!lbServiceUtil.isValidService(appServerServiceName, appName, APPLICATION_SERVER)) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Invalid AppServer Service name [" + appServerServiceName + "].");
                return;
            }

            //domainName = gfServiceUtil.getDomainName(appServerServiceName);
            //targetName = null;
            targetName = appServerServiceName;

            /*if (gfServiceUtil.isDomain(appServerServiceName)) {
                targetName = appServerServiceName;
            } else*/ if (gfServiceUtil.isCluster(appServerServiceName, appName)) {
                targetName = gfServiceUtil.getClusterName(appServerServiceName, appName);
            } /*else if (gfServiceUtil.isStandaloneInstance(appServerServiceName)) {
                targetName = gfServiceUtil.getStandaloneInstanceName(appServerServiceName);
            } */else if (gfServiceUtil.isClusteredInstance(appServerServiceName)) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Invalid AppServer Service name [" + appServerServiceName + "], " +
                        "clustered instance is not supported");
                return;
            } else {
                //Not necessary as we have already completed validation for invalid service. TBS
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Invalid AppServer Service name [" + appServerServiceName + "].");
                return;
            }
        }

        String dasIPAddress = lbServiceUtil.getIPAddress(domainName, appName, APPLICATION_SERVER);

        CloudProvisioner cloudProvisioner = provisionerUtil.getCloudProvisioner();
        String instanceID = cloudProvisioner.createInstance(
                provisionerUtil.getProperties().getProperty(GlassFishLBProvisioner.LB_IMAGE_ID));
        String ipAddress = cloudProvisioner.getIPAddress(instanceID);

        LBProvisioner lbProvisioner = provisionerUtil.getLBProvisioner();

        lbProvisioner.configureLB(ipAddress);

        if (!_ignoreAppServerAssociation) {
            lbProvisioner.associateApplicationServerWithLB(ipAddress, dasIPAddress, domainName);
        }

        lbProvisioner.startLB(ipAddress);

        if (!_ignoreAppServerAssociation) {
            createHttpLBConfig(targetName, dasIPAddress, ipAddress, serviceName);
        }

        registerLBInfo(instanceID, ipAddress);

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage("Service with name [" + serviceName + "] is configured successfully.");
    }

    private void registerLBInfo(String instanceID, String ipAddress) {
        ServiceInfo entry = new ServiceInfo();
        entry.setInstanceId(instanceID);
        entry.setIpAddress(ipAddress);
        entry.setState(Running.toString());
        entry.setServiceName(serviceName);
        entry.setAppName(appName);
        entry.setServerType("load-balancer");

        lbServiceUtil.registerLBInfo(entry);
    }


    private void createHttpLBConfig(String targetName, String dasIPAddress, String ipAddress, String lbConfigName) {
        ApplicationServerProvisioner asProvisioner = provisionerUtil.getAppServerProvisioner(dasIPAddress);
        String deviceHost = ipAddress;
        String command = "create-http-lb";
        String[] options = new String[]{
                "--target", targetName,
                "--devicehost", deviceHost,
                "--deviceport", "50443",
                lbConfigName};

        asProvisioner.executeRemoteCommand(command, options);


        //apply http-lb-changes to that initial setup is done.
        command = "apply-http-lb-changes";
        options = new String[]{
                lbConfigName
        };
        asProvisioner.executeRemoteCommand(command, options);
        //TODO HACK : executing it twice makes the command succeed.
        asProvisioner.executeRemoteCommand(command, options);
    }
}
