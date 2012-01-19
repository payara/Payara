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

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.lbplugin.Constants;
import org.glassfish.paas.lbplugin.LBProvisionerFactory;
import org.glassfish.paas.lbplugin.LBProvisioner;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.lbplugin.util.LBServiceConfiguration;
import org.glassfish.paas.orchestrator.provisioning.*;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.virtualization.config.TemplateIndex;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.types.Property;

/**
 * @author Jagadish Ramu
 */
@Service(name = "_create-lb-service")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class CreateLBService extends BaseLBService implements AdminCommand, Runnable {

    @Param(name = "waitforcompletion", optional = true, defaultValue = "false")
    private boolean waitforcompletion;

    @Param(name="templateid", optional=true)
    private String templateId;

    @Param(name="domainname", optional=true)
    private String domainName;

    @Param(name="servicecharacteristics", optional=true, separator=':')
    public Properties serviceCharacteristics;

    @Param(name="serviceconfigurations", optional=true, separator=':')
    public Properties serviceConfigurations;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    IAAS iaas;

    @Inject
    Habitat habitat;

    private ActionReport report;

    private static final String VENDOR_NAME = "vendor-name";
    private static final String SCRIPTS_DIR_PROP_NAME = "scripts-dir";
    private static final String INSTALL_DIR_PROP_NAME = "install-dir";

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        //using extra-properties to return the provisioned VM and IP Address related information.
        report.setExtraProperties(new Properties());

        LBPluginLogger.getLogger().log(Level.INFO,"_create-lb-service called.");

        if (waitforcompletion) {
            run();
        } else {
            ServiceUtil.getThreadPool().execute(this);
        }
    }


    @Override
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
                            //org.glassfish.virtualization.util.ServiceType.Type.LB.name()));
                            serviceCharacteristics.getProperty("service-type")));
//                    andConditions.add(new VirtualizationType(
//                            serviceCharacteristics.getProperty("virtualization-type")));
                    for (TemplateInstance ti : templateRepository.all()) {
                        boolean allConditionsSatisfied = true;
                        for (TemplateCondition condition : andConditions) {
                            if (!ti.satisfies(condition)) {
                                LBPluginLogger.getLogger().log(Level.INFO,"Matching failed for template : " + ti + " due to condition - " + condition);
                                allConditionsSatisfied = false;
                                break;
                            }
                        }
                        if (allConditionsSatisfied) {
                            LBPluginLogger.getLogger().log(Level.INFO,"Matching template found : " + ti);
                            matchingTemplate = ti;
                            break;
                        }
                    }
                    if (matchingTemplate != null) {
                        templateId = matchingTemplate.getConfig().getName();
                        LBPluginLogger.getLogger().log(Level.INFO,"Matching template name : " + templateId);
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
            String installDir = null;
            String scriptsDir = null;
            String vendorName = null;
            for (Property property : matchingTemplate.getConfig().getProperties()) {
                if (property.getName().equalsIgnoreCase(VENDOR_NAME)) {
                    vendorName = property.getValue();
                } else if (property.getName().equalsIgnoreCase(SCRIPTS_DIR_PROP_NAME)) {
                    scriptsDir = property.getValue();
                } else if (property.getName().equalsIgnoreCase(INSTALL_DIR_PROP_NAME)) {
                    installDir = property.getValue();
                }
            }
            LBProvisionerFactory.getInstance().setLBProvisioner(getLBProvisioner(vendorName));
            TemplateIndex index = matchingTemplate.getConfig().byName("VirtualizationType");
            LBProvisionerFactory.getInstance().getLBProvisioner()
                    .setVirtualizationType(index.getValue());
            LBProvisionerFactory.getInstance().getLBProvisioner().initialize();
            if (installDir != null) {
                LBProvisionerFactory.getInstance().getLBProvisioner().setInstallDir(installDir);
            }
            if (scriptsDir != null) {
                LBProvisionerFactory.getInstance().getLBProvisioner().setScriptsDir(scriptsDir);
            }

            try {
                ServiceInfo entry = new ServiceInfo();
                // provision VMs.
                VirtualCluster vCluster = virtualClusters.byName(virtualClusterName);

                LBPluginLogger.getLogger().log(Level.INFO,"Calling allocate for template ...." + matchingTemplate);
                PhasedFuture<AllocationPhase, VirtualMachine> future = null;
                boolean allocateStatus = false;
                /* Commenting multiple attempt code
                for (int i = 0; i < 3 && !allocateStatus; i++) {
                    i++;
                    try {*/
                        future = iaas.allocate(new AllocationConstraints(matchingTemplate, vCluster), null);
                        allocateStatus = true;
                    /*} catch (Exception ex) {
                        if(future != null){
                            try{
                                future.cancel(true);
                            } catch (Exception ex1){
                            }
                        }
                        LBPluginLogger.getLogger().log(Level.INFO,"Allocate failed for load-balancer ... attempt count" + i);
                        LBPluginLogger.getLogger().log(Level.INFO,"exception",ex);
                    }
                }*/
                if(!allocateStatus){
                    throw new RuntimeException("Unable to allocate load-balancer");
                }
                LBPluginLogger.getLogger().log(Level.INFO,"Done  allocate for template ...." + matchingTemplate);
                LBPluginLogger.getLogger().log(Level.INFO,"Calling future.get() for template ...." + matchingTemplate);
                VirtualMachine vm = future.get();

                LBServiceConfiguration configuration =
                        LBServiceConfiguration.parseServiceConfigurations(
                        serviceConfigurations);

                LBProvisionerFactory.getInstance().getLBProvisioner()
                        .configureLB(vm, domainName, configuration);



                // add app-scoped-service config for each vm instance as well.
                entry = new ServiceInfo();
                entry.setServiceName(serviceName);
                entry.setServerType(ServiceType.LB.toString());
                entry.setIpAddress(vm.getAddress().getHostAddress());
                report.getExtraProperties().put("ip-address", vm.getAddress().getHostAddress());
                entry.setInstanceId(vm.getName());
                report.getExtraProperties().put("vm-id", vm.getName());
                entry.setState(ServiceStatus.NOT_RUNNING.toString());
                entry.setAppName(appName);
                if(domainName != null){
                    entry.setProperty(Constants.DOMAIN_NAME, domainName);
                    report.getExtraProperties().put(Constants.DOMAIN_NAME, domainName);
                }
                configuration.updateServiceInfo(entry);

                //lbServiceUtil.registerLBInfo(entry);

            } catch (Throwable ex) {
                LBPluginLogger.getLogger().log(Level.INFO,"Exception : " + ex);
                LBPluginLogger.getLogger().log(Level.INFO,"exception",ex);
                throw new RuntimeException(ex);
            }
            return; // we are done provisioning, thanks. Bye...
        } else {
            LBPluginLogger.getLogger().log(Level.INFO,"No matching template found .... exiting");
        }

        //TBD return error
    }

    public LBProvisioner getLBProvisioner(String vendorName) {
        if(vendorName == null){
            LBPluginLogger.getLogger().log(Level.INFO, "Vendor name not specified. Using default");
            vendorName = LBProvisionerFactory.getInstance().getDefaultProvisionerName();
        }
        Collection<LBProvisioner> allProvisioners =
                habitat.getAllByContract(LBProvisioner.class);
        if(allProvisioners == null || allProvisioners.isEmpty()){
            String msg = "No lbprovisioners found.";
            LBPluginLogger.getLogger().log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        for(LBProvisioner provisioner : allProvisioners){
            if(provisioner.handles(vendorName)){
                LBPluginLogger.getLogger().log(Level.INFO, "Found provisioner "
                        + provisioner + " for vendor name " + vendorName);
                return provisioner;
            }
        }
        String msg = "No matching lbprovisioners found for " + vendorName;
        LBPluginLogger.getLogger().log(Level.SEVERE, msg);
        throw new RuntimeException(msg);
    }

}
