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
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.virtualization.spi.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.PhasedFuture;
import org.glassfish.virtualization.spi.AllocationConstraints;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author Jagadish Ramu
 * @author Shalini M
 */
@Service(name = "_create-derby-service")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class CreateDerbyService implements AdminCommand, Runnable {

    @Param(name = "waitforcompletion", optional = true, defaultValue = "false")
    private boolean waitforcompletion;

    @Param(name="templateid", optional=true)
    private String templateId;

    @Param(name="servicecharacteristics", optional=true, separator=':')
    public Properties serviceCharacteristics;

    @Param(name="serviceconfigurations", optional=true, separator=':')
    public Properties serviceConfigurations;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    IAAS iaas;

    // TODO :: remove dependency on VirtualCluster(s).
    @Inject(optional = true) // // made it optional for non-virtual scenario to work
    VirtualClusters virtualClusters;


    @Param(name = "servicename", primary = true, optional = false)
    private String serviceName;

    @Inject
    private ProvisionerUtil provisionerUtil;

    @Inject
    private DatabaseServiceUtil dbServiceUtil;

    @Param(name="appname", optional=true)
    private String appName;

    @Param(name = "virtualcluster", optional = true)
    private String virtualClusterName;

    public void execute(AdminCommandContext context) {
        System.out.println("_create-derby-service called.");
        final ActionReport report = context.getActionReport();
        // Check if the service is already configured.
        if (dbServiceUtil.isServiceAlreadyConfigured(serviceName, appName, ServiceType.DATABASE)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Service with name [" +
                    serviceName + "] is already configured.");
            return;
        }
        if (waitforcompletion) {
            run();
        } else {
            ServiceUtil.getThreadPool().execute(this);
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage("Service with name [" +
                serviceName + "] is configured successfully.");
    }

    public void run() {
        TemplateInstance matchingTemplate = null;
        if (templateRepository != null) {
            if (templateId == null) {
                // search for matching template based on service characteristics
                if (serviceCharacteristics != null) {
                    /**
                     * TODO :: use templateRepository.get(SearchCriteria) when
                     * an implementation of SearchCriteria becomes available.
                     * for now, iterate over all template instances and find the right one.
                     */
                    Set<TemplateCondition> andConditions = new HashSet<TemplateCondition>();
                    andConditions.add(new org.glassfish.virtualization.util.ServiceType(
                            serviceCharacteristics.getProperty("service-type")));
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
                VirtualCluster vCluster = virtualClusters.byName(virtualClusterName);
                PhasedFuture<AllocationPhase, VirtualMachine> future =
                        iaas.allocate(new AllocationConstraints(matchingTemplate, vCluster), null);

                VirtualMachine vm = future.get();
                // add app-scoped-service config for each vm instance as well.
                //DatabaseProvisioner dbProvisioner = new DerbyProvisioner();//provisionerUtil.getDatabaseProvisioner();
                //dbProvisioner.startDatabase(vm.getAddress());
                ServiceInfo entry = new ServiceInfo();
                entry.setInstanceId(vm.getName());
                entry.setIpAddress(vm.getAddress());
                entry.setState(ServiceInfo.State.Running.toString());
                entry.setServiceName(serviceName);
                entry.setAppName(appName);
                entry.setServerType(ServiceType.DATABASE.toString());

                dbServiceUtil.registerDBInfo(entry);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
            return;
        }else{
            //local mode related functionality.
            CloudProvisioner cloudProvisioner = provisionerUtil.getCloudProvisioner();
            String instanceID = cloudProvisioner.createMasterInstance();
            String ipAddress = cloudProvisioner.getIPAddress(instanceID);

            DatabaseProvisioner dbProvisioner = provisionerUtil.getDatabaseProvisioner();
            dbProvisioner.startDatabase(ipAddress);

            ServiceInfo entry = new ServiceInfo();
            entry.setInstanceId(instanceID);
            entry.setIpAddress(ipAddress);
            entry.setState(ServiceInfo.State.Running.toString());
            entry.setServiceName(serviceName);
            entry.setAppName(appName);
            entry.setServerType(ServiceType.DATABASE.toString());
            dbServiceUtil.registerDBInfo(entry);
        }
    }
}
