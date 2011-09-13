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

package org.glassfish.paas.gfplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Jagadish Ramu
 */
@Service(name = "_delete-glassfish-service")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class DeleteGlassFishService implements AdminCommand {

    @Param(name = "waitforcompletion", optional = true, defaultValue = "false")
    private boolean waitforcompletion;

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Param(name="appname", optional=true)
    private String appName;

    @Inject
    private ProvisionerUtil provisionerUtil;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    // TODO :: remove dependency on VirtualCluster(s).
    @Inject(optional = true) // made it optional for non-virtual scenario to work
    VirtualClusters virtualClusters;

    @Inject
    VirtualMachineLifecycle vmLifecycle;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        System.out.println("_delete-glassfish-service called.");

        // Parse clusterName
        if (serviceName.indexOf('.') != serviceName.lastIndexOf('.')) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Multiple dots not allowed in the servicename ["+serviceName+"]");
        }

        // Check if the service is already configured.
        if (!gfServiceUtil.isServiceAlreadyConfigured(serviceName, appName, ServiceType.APPLICATION_SERVER)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Service with name ["+serviceName+"] not found");
        }

        if (templateRepository != null && virtualClusters != null) { // we are in virtualized environment.
            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceName);
            ApplicationServerProvisioner provisioner =
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);
            provisioner.stopCluster(dasIPAddress, serviceName); // this stops all the VMs also.
            if (virtualClusters != null && serviceName != null) {
                try {
                    VirtualCluster virtualCluster = virtualClusters.byName(serviceName);
                    Collection<String> instances =
                            gfServiceUtil.getAllSubComponents(serviceName, appName);
                    for(String instance : instances){
                        String vmId = gfServiceUtil.getInstanceID(
                                instance, appName, ServiceType.APPLICATION_SERVER);
                        VirtualMachine vm = virtualCluster.vmByName(vmId); // TODO :: IMS should give differnt way to get hold of VM using the vmId
                        vmLifecycle.delete(vm); // TODO :: use executor service.
                        gfServiceUtil.unregisterASInfo(instance, appName);
                    }
                    if (virtualCluster != null) {
                        virtualClusters.remove(virtualCluster);  // removes config.
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            // TODO :: delete instances manually before deleting the cluster.
            provisioner.deleteCluster(dasIPAddress, serviceName, false);
            gfServiceUtil.unregisterASInfo(serviceName, appName);
            return; //we are done unprovisioning...
        }

        String clusterName = gfServiceUtil.getClusterName(serviceName, appName);

        String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceName);
        ApplicationServerProvisioner provisioner = provisionerUtil.getAppServerProvisioner(dasIPAddress);

        //Stop the cluster.
        provisioner.stopCluster(dasIPAddress, clusterName);

        //update Registry -> cluster + its instances
        gfServiceUtil.updateState(clusterName, appName, ServiceInfo.State.NotRunning.toString(), ServiceType.APPLICATION_SERVER) ;

        Collection<String> instances = gfServiceUtil.getAllSubComponents(clusterName, appName);
        for(String instance : instances){
            gfServiceUtil.updateState(instance, appName, ServiceInfo.State.NotRunning.toString(), ServiceType.APPLICATION_SERVER) ;
            //TODO should we update the IP address also ? (ie., remove it ? )
        }

        // get the instances of the cluster

        for(String instance : instances){
            String node = null;
            //instance here is of the form "cluster.instance".
            String instanceName = gfServiceUtil.getInstanceName(instance);
            String dottedName = "servers.server."+instanceName+".node-ref";
            CommandResult result = provisioner.executeRemoteCommand("get", dottedName);
            String output = result.getOutput();
            //output of the form "servers.server.instance.node-ref=mynode\n"
            if(output.contains(dottedName+"=")){
                node = output.substring((dottedName+"=").length(), output.length()-1);
            }

            String ipAddress = gfServiceUtil.getIPAddress(instance, appName, ServiceType.APPLICATION_SERVER);
            provisioner.unProvisionNode(dasIPAddress, ipAddress, node, instanceName);


        }

        List<String> instanceIDs = new ArrayList<String>();
        for(String instance : instances){
            String instanceID = gfServiceUtil.getInstanceID(instance, appName, ServiceType.APPLICATION_SERVER);
            instanceIDs.add(instanceID);
        }

        // delete VM.
        CloudProvisioner cloudProvisioner = provisionerUtil.getCloudProvisioner();
        cloudProvisioner.deleteInstances(instanceIDs);

        // update Registry
        for(String instance : instances){
            gfServiceUtil.unregisterASInfo(instance, appName);
        }

        provisioner.deleteCluster(dasIPAddress, clusterName, false);

        // update Registry
        gfServiceUtil.unregisterASInfo(clusterName, appName);
    }
}
