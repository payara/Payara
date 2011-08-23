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
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.AllocationPhase;
import org.glassfish.virtualization.spi.IAAS;
import org.glassfish.virtualization.spi.ListenableFuture;
import org.glassfish.virtualization.spi.TemplateCondition;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VMOrder;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.VirtualizationType;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author bhavanishankar@java.net
 */
@Service(name = "_create-glassfish-service")
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

    @Param(name="templateid", optional=true)
    private String templateId;

    @Param(name="servicecharacteristics", optional=true, separator=':')
    public Properties serviceCharacteristics;

    @Param(name="serviceconfigurations", optional=true, separator=':')
    public Properties serviceConfigurations;

    @Inject
    private Domain domain;

    //private String domainName;
    private String clusterName;
    private String instanceName;

    @Inject
    private ProvisionerUtil provisionerUtil;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    IAAS iaas;

    // TODO :: remove dependency on VirtualCluster(s).
    @Inject(optional = true) // // made it optional for non-virtual scenario to work
    VirtualClusters virtualClusters;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        System.out.println("_create-glassfish-service called.");

        // Parse clusterName
        if (serviceName.indexOf('.') != serviceName.lastIndexOf('.')) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Multiple dots not allowed in the servicename ["+serviceName+"]");
        }

        // Check if the service is already configured.
        if (gfServiceUtil.isServiceAlreadyConfigured(serviceName, appName, ServiceType.APPLICATION_SERVER)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Service with name ["+serviceName+"] is already configured");
        }

/*
        domainName = serviceName.indexOf(".") > -1 ?
                serviceName.substring(0, serviceName.indexOf(".")) : serviceName;
*/
        instanceCount = instanceCount <= 0 ? Integer.parseInt(
                serviceConfigurations.getProperty("max.clustersize")) : instanceCount;
        
        if (instanceCount >= 0) {
            clusterName = serviceName.indexOf(".") > -1 ?
                serviceName.substring(0, serviceName.indexOf(".")) : serviceName;
        } else {
            instanceName = serviceName.indexOf(".") > -1 ?
                    serviceName.substring(serviceName.indexOf('.') + 1) : null;
        }

        String dasIPAddress = "Obtaining";

        // Save domain's ServiceInfo in the DB
        ServiceInfo entry = new ServiceInfo();

/*
        String domainName = domain.getProperty(Domain.DOMAIN_NAME_PROPERTY).getValue();
        if (!gfServiceUtil.isServiceAlreadyConfigured(domainName, appName, ServiceType.APPLICATION_SERVER)) { // domain might exist already.
            entry.setServiceName(domainName);
            entry.setIpAddress(dasIPAddress);
            entry.setServerType(ServiceInfo.Type.Domain.toString());
            entry.setInstanceId("Obtaining");
            entry.setState(ServiceInfo.State.Initializing.toString());
            entry.setAppName(appName);
            gfServiceUtil.registerASInfo(entry);
        }
*/


        if (clusterName != null) {
            // Save cluster's ServiceInfo in the DB
            entry.setServiceName(serviceName);
            entry.setInstanceId("NA");
            entry.setIpAddress("NA");
            entry.setState(ServiceInfo.State.Initializing.toString());
            entry.setServerType(ServiceInfo.Type.Cluster.toString());
            entry.setAppName(appName);
            gfServiceUtil.registerASInfo(entry);
        }

/*
        if (instanceName != null) {
            // Save cluster's ServiceInfo in the DB
            entry.setServiceName(serviceName);
            //entry.setInstanceId("NA");
            //entry.setIpAddress("NA");
            entry.setServerType(ServiceInfo.Type.StandAloneInstance.toString());
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
    private void update(ServiceInfo entry) {
        gfServiceUtil.updateIPAddress(entry.getServiceName(), entry.getAppName(), entry.getIpAddress(), ServiceType.APPLICATION_SERVER);
        gfServiceUtil.updateState(entry.getServiceName(), entry.getAppName(), entry.getState(), ServiceType.APPLICATION_SERVER);
    }



    public void run() {
        TemplateInstance matchingTemplate = null;
        if (templateRepository != null) {
            if (templateId == null) {
                // search for matching template based on service characteristics
                if (serviceCharacteristics != null) {
                    /**
                     * TODO :: use templateRepository.get(ServiceCriteria) when
                     * an implementation of ServiceCriteria becomes available.
                     * for now, iterate over all template instances and find the right one.
                     */
                    Set<TemplateCondition> andConditions = new HashSet<TemplateCondition>();
                    andConditions.add(new org.glassfish.virtualization.util.ServiceType(
                            serviceCharacteristics.getProperty("service-type")));
                    andConditions.add(new VirtualizationType(
                            serviceCharacteristics.getProperty("virtualization-type")));
                    for (TemplateInstance ti : templateRepository.all()) {
                        boolean allConditionsSatisfied = true;
                        for (TemplateCondition condition : andConditions) {
                            if (!ti.satisfies(condition)) {
                                allConditionsSatisfied = false;
                                break;
                            }
                        }
                        if (allConditionsSatisfied) {
                            matchingTemplate = ti;
                            break;
                        }
                    }
                    if (matchingTemplate != null) {
                        templateId = matchingTemplate.getConfig().getName();
                    }
                }
            } else {
                for (TemplateInstance ti : templateRepository.all()) {
                    if (ti.getConfig().getName().equals(templateId)) {
                        matchingTemplate = ti;
                        break;
                    }
                }
            }
        }

        if (matchingTemplate != null) {
            try {
                // Create the cluster.
                // TODO :: we may need to create cluster in remote DAS.
                // TODO :: So, first we may first need to provision the remote DAS
                // TODO :: for now, everything is managed by the local DAS.
                String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceName);
                ApplicationServerProvisioner provisioner =
                        provisionerUtil.getAppServerProvisioner(dasIPAddress);
                provisioner.createCluster(dasIPAddress, serviceName);

                // provision VMs.
                VirtualCluster vCluster = virtualClusters.byName(serviceName);
                String maxClusterSize = serviceConfigurations.getProperty("max.clustersize");
                int max = maxClusterSize != null ? Integer.parseInt(maxClusterSize) : 0;
                for (int i = 0; i < max; i++) {
                    ListenableFuture<AllocationPhase, VirtualMachine> future =
                            iaas.allocate(new VMOrder(matchingTemplate, vCluster), null);
                    VirtualMachine vm = future.get();
                }

                // start the cluster.
                provisioner.startCluster(dasIPAddress, serviceName);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
            return; // we are done provisioning, thanks. Bye...
        }

/*
        String domainState = gfServiceUtil.getServiceState(domainName, ServiceType.APPLICATION_SERVER);
        String dasIPAddress;
*/
        CloudProvisioner cloudProvisioner = provisionerUtil.getCloudProvisioner();
        ApplicationServerProvisioner provisioner;

/*
        if (ServiceInfo.State.Initializing.toString().equals(domainState)) {

            // Invoke CloudProvisioner to install DAS image on an VM
            String instanceID = cloudProvisioner.createMasterInstance();
            gfServiceUtil.updateInstanceID(domainName, instanceID, ServiceType.APPLICATION_SERVER);
            String ipAddress = cloudProvisioner.getIPAddress(instanceID);

            cloudProvisioner.uploadCredentials(ipAddress);

            dasIPAddress = ipAddress;
            provisioner = provisionerUtil.getAppServerProvisioner(dasIPAddress);
            provisioner.createDomain(domainName, dasIPAddress, "--user=admin", "--nopassword");
            provisioner.startDomain(dasIPAddress, domainName);

            // enable secure admin in the domain.
            provisioner.enableSecureAdmin(dasIPAddress);
            provisioner.stopDomain(dasIPAddress, domainName);
            provisioner.startDomain(dasIPAddress, domainName);
            updateIPAndState(dasIPAddress, ServiceInfo.State.Running.toString());
        } else if (ServiceInfo.State.NotRunning.toString().equals(domainState)) {
            dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);
            String instanceID = gfServiceUtil.getInstanceID(serviceName, ServiceType.APPLICATION_SERVER);

            Map<String, String> instances = new LinkedHashMap<String, String>();
            instances.put(instanceID, dasIPAddress);
            cloudProvisioner.startInstances(instances);

            provisioner = provisionerUtil.getAppServerProvisioner(dasIPAddress);
            provisioner.startDomain(dasIPAddress, domainName);
            updateIPAndState(dasIPAddress, ServiceInfo.State.Running.toString());
        } else {
            //if the DAS is already running and someone is trying to create a cluster/instance.
            dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);
        }
*/

        String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceName);
        provisioner = provisionerUtil.getAppServerProvisioner(dasIPAddress);
        if (instanceCount >= 1) {
            //TODO run this parallely ?
            if (clusterName != null) {

                provisioner.createCluster(dasIPAddress, clusterName);
                provisioner.startCluster(dasIPAddress, clusterName);

                // update DB :: update cluster state.
                ServiceInfo entry = new ServiceInfo();
                entry.setIpAddress("NA");
                entry.setServiceName(clusterName);
                entry.setAppName(appName);
                entry.setState(ServiceInfo.State.Running.toString());
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

        ServiceInfo entry = new ServiceInfo();
        // Update DB :: Create entry for the instances.
        if (clusterName != null) {
            entry.setServiceName(serviceName + "." + instanceName);
        } else {
            entry.setServiceName(serviceName);
        }
        entry.setServerType(clusterName == null ?
                ServiceInfo.Type.StandAloneInstance.toString() :
                ServiceInfo.Type.ClusterInstance.toString());
        entry.setIpAddress(instanceIP);
        entry.setInstanceId(instanceId);
        entry.setState(ServiceInfo.State.Running.toString());
        entry.setAppName(appName);
        gfServiceUtil.registerASInfo(entry);
    }

/*
    private void updateIPAndState(String dasIPAddress, String state) {
        // update DB.
        ServiceInfo entry = new ServiceInfo();
        entry.setServiceName(domainName);
        entry.setIpAddress(dasIPAddress);
        entry.setState(state);
        update(entry);
        //return entry;
    }
*/
}
