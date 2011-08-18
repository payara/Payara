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

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.List;

/**
 * @author bhavanishankar@java.net
 */
@Service(name = "create-glassfish-service")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class CreateGlassFishService implements AdminCommand, Runnable {

    @Param(name = "instancecount", optional = true, defaultValue = "-1")
    private int instanceCount;

    @Param(name = "profile", optional = true, defaultValue = "all")
    private String profile;

    @Param(name = "waitforcompletion", optional = true, defaultValue = "false")
    private boolean waitforcompletion;

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Param(name="appname", optional=true)
    private String appName;

    @Inject
    private Domain domain;

    //private String domainName;
    private String clusterName;
    private String instanceName;

    @Inject
    private CloudRegistryService cloudRegistryService;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;


    public void execute(AdminCommandContext adminCommandContext) {
        System.out.println("create-glassfish-service called.");

        // Parse domainName
        if (serviceName.indexOf('.') != serviceName.lastIndexOf('.')) {
            throw new RuntimeException("Multiple dots not allowed in the servicename");
        }

        // Check if the service is already configured.
        if (gfServiceUtil.isServiceAlreadyConfigured(serviceName, appName, ServiceType.APPLICATION_SERVER)) {
            throw new RuntimeException("Service with name [" +
                    serviceName + "] is already configured.");
        }

/*
        domainName = serviceName.indexOf(".") > -1 ?
                serviceName.substring(0, serviceName.indexOf(".")) : serviceName;
*/
        if (instanceCount >= 0) {
            clusterName = serviceName.indexOf(".") > -1 ?
                serviceName.substring(0, serviceName.indexOf(".")) : serviceName;
        } else {
            instanceName = serviceName.indexOf(".") > -1 ?
                    serviceName.substring(serviceName.indexOf('.') + 1) : null;
        }

        String dasIPAddress = "Obtaining";

        // Save domain's CloudRegistryEntry in the DB
        CloudRegistryEntry entry = new CloudRegistryEntry();

/*
        String domainName = domain.getProperty(Domain.DOMAIN_NAME_PROPERTY).getValue();
        if (!gfServiceUtil.isServiceAlreadyConfigured(domainName, appName, ServiceType.APPLICATION_SERVER)) { // domain might exist already.
            entry.setCloudName(domainName);
            entry.setIpAddress(dasIPAddress);
            entry.setServerType(CloudRegistryEntry.Type.Domain.toString());
            entry.setInstanceId("Obtaining");
            entry.setState(CloudRegistryEntry.State.Initializing.toString());
            entry.setAppName(appName);
            gfServiceUtil.registerASInfo(entry);
        }
*/


        if (clusterName != null) {
            // Save cluster's CloudRegistryEntry in the DB
            entry.setCloudName(serviceName);
            entry.setInstanceId("NA");
            entry.setIpAddress("NA");
            entry.setState(CloudRegistryEntry.State.Initializing.toString());
            entry.setServerType(CloudRegistryEntry.Type.Cluster.toString());
            entry.setAppName(appName);
            gfServiceUtil.registerASInfo(entry);
        }

/*
        if (instanceName != null) {
            // Save cluster's CloudRegistryEntry in the DB
            entry.setCloudName(serviceName);
            //entry.setInstanceId("NA");
            //entry.setIpAddress("NA");
            entry.setServerType(CloudRegistryEntry.Type.StandAloneInstance.toString());
            registerASInfo(entry);
        }
*/
        if (waitforcompletion) {
            run();
        } else {
            ServiceUtil.getThreadPool().execute(this);
        }
    }

    // Register the setup in database.
    private void update(CloudRegistryEntry entry) {
        gfServiceUtil.updateIPAddress(entry.getCloudName(), entry.getAppName(), entry.getIpAddress(), ServiceType.APPLICATION_SERVER);
        gfServiceUtil.updateState(entry.getCloudName(), entry.getAppName(), entry.getState(), ServiceType.APPLICATION_SERVER);
    }



    public void run() {

/*
        String domainState = gfServiceUtil.getServiceState(domainName, ServiceType.APPLICATION_SERVER);
        String dasIPAddress;
*/
        CloudProvisioner cloudProvisioner = cloudRegistryService.getCloudProvisioner();
        ApplicationServerProvisioner provisioner;

/*
        if (CloudRegistryEntry.State.Initializing.toString().equals(domainState)) {

            // Invoke CloudProvisioner to install DAS image on an VM
            String instanceID = cloudProvisioner.createMasterInstance();
            gfServiceUtil.updateInstanceID(domainName, instanceID, ServiceType.APPLICATION_SERVER);
            String ipAddress = cloudProvisioner.getIPAddress(instanceID);

            cloudProvisioner.uploadCredentials(ipAddress);

            dasIPAddress = ipAddress;
            provisioner = cloudRegistryService.getAppServerProvisioner(dasIPAddress);
            provisioner.createDomain(domainName, dasIPAddress, "--user=admin", "--nopassword");
            provisioner.startDomain(dasIPAddress, domainName);

            // enable secure admin in the domain.
            provisioner.enableSecureAdmin(dasIPAddress);
            provisioner.stopDomain(dasIPAddress, domainName);
            provisioner.startDomain(dasIPAddress, domainName);
            updateIPAndState(dasIPAddress, CloudRegistryEntry.State.Running.toString());
        } else if (CloudRegistryEntry.State.NotRunning.toString().equals(domainState)) {
            dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);
            String instanceID = gfServiceUtil.getInstanceID(serviceName, ServiceType.APPLICATION_SERVER);

            Map<String, String> instances = new LinkedHashMap<String, String>();
            instances.put(instanceID, dasIPAddress);
            cloudProvisioner.startInstances(instances);

            provisioner = cloudRegistryService.getAppServerProvisioner(dasIPAddress);
            provisioner.startDomain(dasIPAddress, domainName);
            updateIPAndState(dasIPAddress, CloudRegistryEntry.State.Running.toString());
        } else {
            //if the DAS is already running and someone is trying to create a cluster/instance.
            dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);
        }
*/

        String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceName);
        provisioner = cloudRegistryService.getAppServerProvisioner(dasIPAddress);
        if (instanceCount >= 1) {
            //TODO run this parallely ?
            if (clusterName != null) {

                provisioner.createCluster(dasIPAddress, clusterName);
                provisioner.startCluster(dasIPAddress, clusterName);

                // update DB :: update cluster state.
                CloudRegistryEntry entry = new CloudRegistryEntry();
                entry.setIpAddress("NA");
                entry.setCloudName(clusterName);
                entry.setAppName(appName);
                entry.setState(CloudRegistryEntry.State.Running.toString());
                update(entry);
            }

            for (int i = 1; i <= instanceCount; i++) {
                createAndRegisterInstance(dasIPAddress, cloudProvisioner, provisioner, null);
            }
        } else if (instanceName != null) {
            createAndRegisterInstance(dasIPAddress, cloudProvisioner, provisioner, instanceName);
        }
    }

    private void createAndRegisterInstance(String dasIPAddress, CloudProvisioner cloudProvisioner,
                                           ApplicationServerProvisioner provisioner, String providedInstanceName) {
        int instanceNameSuffix = 0;
        // Invoke EC2Provisioner to install instance image on EC2 instances.
        List<String> instanceIds = cloudProvisioner.createSlaveInstances(1);
        String instanceId = instanceIds.get(0); //there will be only one.

        String instanceIP = cloudProvisioner.getIPAddress(instanceId);
        //String domainName = gfServiceUtil.getServiceName(dasIPAddress, ServiceType.APPLICATION_SERVER);

        String insName;
        String nodeName;
        if (providedInstanceName == null) {
            //String ID = gfServiceUtil.getNextID(domainName);
            String ID = gfServiceUtil.getNextID(clusterName, appName);
            insName = gfServiceUtil.generateInstanceName(ID);
            nodeName = gfServiceUtil.generateNodeName(ID);
        } else {
            insName = providedInstanceName;
            //TODO while deleting the node, we need to generate the name again ?
            nodeName = gfServiceUtil.generateNodeName(providedInstanceName);
        }
        String instanceName = provisioner.provisionNode(dasIPAddress, instanceIP, clusterName, nodeName, insName);
        provisioner.startInstance(dasIPAddress, instanceName);

        CloudRegistryEntry entry = new CloudRegistryEntry();
        // Update DB :: Create entry for the instances.
        if (clusterName != null) {
            entry.setCloudName(serviceName + "." + instanceName);
        } else {
            entry.setCloudName(serviceName);
        }
        entry.setServerType(clusterName == null ?
                CloudRegistryEntry.Type.StandAloneInstance.toString() :
                CloudRegistryEntry.Type.ClusterInstance.toString());
        entry.setIpAddress(instanceIP);
        entry.setInstanceId(instanceId);
        entry.setState(CloudRegistryEntry.State.Running.toString());
        entry.setAppName(appName);
        gfServiceUtil.registerASInfo(entry);
    }

/*
    private void updateIPAndState(String dasIPAddress, String state) {
        // update DB.
        CloudRegistryEntry entry = new CloudRegistryEntry();
        entry.setCloudName(domainName);
        entry.setIpAddress(dasIPAddress);
        entry.setState(state);
        update(entry);
        //return entry;
    }
*/
}
