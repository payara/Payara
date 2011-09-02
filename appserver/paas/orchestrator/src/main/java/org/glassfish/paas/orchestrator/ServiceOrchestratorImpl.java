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

package org.glassfish.paas.orchestrator;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.ApplicationLifecycleInterceptor;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

@Service
public class ServiceOrchestratorImpl implements ServiceOrchestrator, ApplicationLifecycleInterceptor {

    @Inject
    protected Habitat habitat;

    @Inject
    private ArchiveFactory archiveFactory;

    @Inject
    private ServerEnvironment serverEnvironment;

    private Map<String, ServiceMetadata> serviceMetadata = new HashMap<String, ServiceMetadata>();
    private Map<String, Set<ProvisionedService>> provisionedServices = new HashMap<String, Set<ProvisionedService>>();

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());
    private boolean usingDeployService = false;

    public Set<Plugin> getPlugins() {
        Set<Plugin> plugins = new HashSet<Plugin>();
        plugins.addAll(habitat.getAllByContract(Plugin.class));
        logger.log(Level.INFO, "Discovered plugins:" + plugins);
        return plugins;
    }


    public void provisionServicesForApplication(String appName, ReadableArchive archive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "provisionServicesForApplication");
        //Get all plugins installed in this runtime
        Set<Plugin> installedPlugins = getPlugins();

        //1. Perform service dependency discovery
        ServiceMetadata appServiceMetadata = serviceDependencyDiscovery(appName, archive, installedPlugins);

        //2. Provision dependent services
        Set<ProvisionedService> appProvisionedSvcs = provisionServices(installedPlugins, appServiceMetadata, dc);

        //3. Associate provisioned services with each other
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, true /*before deployment*/, dc);
        serviceMetadata.put(appName, appServiceMetadata);
        provisionedServices.put(appName, appProvisionedSvcs);
        logger.exiting(getClass().getName(), "provisionServicesForApplication");
    }

    public void postDeploy(String appName, ReadableArchive archive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "postDeploy");
        //4b. post-deployment association

        Set<Plugin> installedPlugins = getPlugins();
        ServiceMetadata appServiceMetadata = serviceMetadata.get(appName);
        Set<ProvisionedService> appProvisionedSvcs = provisionedServices.get(appName);
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, false /*after deployment*/, dc);

        //TODO should we remove them, or book keep it till its stopped/undeployed ?
        //serviceMetadata.remove(appName);
        //provisionedServices.remove(appName);
        logger.exiting(getClass().getName(), "postDeploy");
    }


    public void deployApplication(String appName, ReadableArchive cloudArchive) {
        logger.entering(getClass().getName(), "deployApplication");
        //Get all plugins installed in this runtime
        Set<Plugin> installedPlugins = getPlugins();

        //1. Perform service dependency discovery
        ServiceMetadata appServiceMetadata = serviceDependencyDiscovery(appName, cloudArchive, installedPlugins);

        //2. Provision dependent services
        //TODO passing null for deploymentContext which will break "cloud-deploy" command. FIX IT
        Set<ProvisionedService> appProvisionedSvcs = provisionServices(installedPlugins, appServiceMetadata, null);

        //3. Associate provisioned services with each other
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, true /*before deployment*/, null);

        //4a. Application Deployment
        deployArchive(cloudArchive, installedPlugins);

        //4b. post-deployment association
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, false /*after deployment*/, null);
    }

    public void prepareForUndeploy(String appName, ReadableArchive cloudArchive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "prepareForUndeploy");
        //Get all plugins installed in this runtime
        Set<Plugin> installedPlugins = getPlugins();

        ServiceMetadata appServiceMetadata = serviceMetadata.get(appName);
        Set<ProvisionedService> appProvisionedServices = provisionedServices.get(appName);

        dissociateProvisionedServices(installedPlugins, appServiceMetadata, appProvisionedServices, true, dc);
    }

    public void postUndeploy(String appName, ReadableArchive cloudArchive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "postUndeploy");
        //4b. post-undeploy disassociation

        Set<Plugin> installedPlugins = getPlugins();
        ServiceMetadata appServiceMetadata = serviceMetadata.get(appName);
        Set<ProvisionedService> appProvisionedSvcs = provisionedServices.get(appName);
        dissociateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, false /*after undeployment*/, dc);

        unprovisionServices(installedPlugins, appServiceMetadata, dc);

        logger.exiting(getClass().getName(), "postUndeploy");
    }


    private ServiceMetadata serviceDependencyDiscovery(String appName, ReadableArchive cloudArchive, Set<Plugin> installedPlugins) {
        logger.entering(getClass().getName(), "serviceDependencyDiscovery");
        //1. SERVICE DISCOVERY
        //parse services.xml to get all declared SRs and SDs
        //Get the first ServicesXMLParser implementation

        ServicesXMLParser parser = habitat.getAllByContract(
                ServicesXMLParser.class).iterator().next();

        //1.1 discover all Service References and Definitions already declared for this application
        ServiceMetadata appServiceMetadata = parser.discoverDeclaredServices(appName, cloudArchive);

        //if no meta-data is found, create empty ServiceMetadata
        if (appServiceMetadata == null) {
            appServiceMetadata = new ServiceMetadata();
            appServiceMetadata.setAppName(appName);
        }

        logger.log(Level.INFO, "Discovered declared service metadata via services.xml = " + appServiceMetadata);

        //1.2 Get implicit service-definitions (for instance a war is deployed, and it has not
        //specified a javaee service-definition in its orchestration.xml, the PaaS runtime
        //through the GlassFish plugin that a default javaee service-definition
        //is implied
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.handles(cloudArchive)) {
                //If a ServiceDescription has not been declared explicitly in
                //the application for the plugin's type, ask the plugin
                //if it has any implicit service-definition for this
                //application
                if (!serviceDefinitionExistsForType(appServiceMetadata, svcPlugin.getServiceType())) {
                    Set<ServiceDescription> implicitServiceDescs = svcPlugin.getImplicitServiceDescriptions(cloudArchive, appName);
                    for (ServiceDescription sd : implicitServiceDescs) {
                        System.out.println("Implicit ServiceDescription:" + sd);
                        appServiceMetadata.addServiceDescription(sd);
                    }
                }
            }
        }
        logger.log(Level.INFO, "After adding implicit ServiceDescriptions = " + appServiceMetadata);


        //1.2 Get implicit ServiceReferences
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.handles(cloudArchive)) {
                Set<ServiceReference> implicitServiceRefs = svcPlugin.getServiceReferences(cloudArchive);
                for (ServiceReference sr : implicitServiceRefs) {
                    System.out.println("ServiceReference:" + sr);
                    appServiceMetadata.addServiceReference(sr);
                }
            }
        }
        logger.log(Level.INFO, "After adding ServiceReferences = " + appServiceMetadata);

        //1.3 Ensure all service references have a related service definition
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
        for (ServiceReference sr : appSRs) {
            String targetSD = sr.getTarget();
            String svcRefType = sr.getServiceRefType();
            boolean serviceDefinitionExists = false;
            for (ServiceDescription sd : appSDs) {
                //XXX: For now we assume all SRs are satisfied by app-scoped SDs
                //In the future this has to be modified to search in global SDs
                //as well
                if (sd.getName().equals(targetSD)) {
                    serviceDefinitionExists = true;
                }
            }
            if (!serviceDefinitionExists) {
                //create a default SD for this service ref and add to application's
                //service metadata
                for (Plugin svcPlugin : installedPlugins) {
                    if (svcPlugin.isReferenceTypeSupported(svcRefType)) {
                        ServiceDescription defSD = svcPlugin.getDefaultServiceDescription(appName, sr);
                        appServiceMetadata.addServiceDescription(defSD);
                        continue; //ignore the rest of the plugins
                    }
                }
            }
        }

        assertMetadataComplete(appSDs, appSRs);
        logger.log(Level.INFO, "Final Service Metadata = " + appServiceMetadata);
        return appServiceMetadata;
    }

    private void deployArchive(ReadableArchive cloudArchive,
                               Set<Plugin> installedPlugins) {
        for (Plugin<?> svcPlugin : installedPlugins) {
            logger.log(Level.INFO, "Deploying Application Archive " + " through " + svcPlugin);
            svcPlugin.deploy(cloudArchive);
        }
    }

    private void dissociateProvisionedServices(Set<Plugin> installedPlugins,
                                               ServiceMetadata appServiceMetadata,
                                               Set<ProvisionedService> appProvisionedSvcs, boolean beforeUndeploy,
                                               DeploymentContext context) {
        logger.entering(getClass().getName(), "dissociateProvisionedServices=" + beforeUndeploy);
        for (ProvisionedService serviceProvider : appProvisionedSvcs) {
            for (Plugin<?> svcPlugin : installedPlugins) {
                //Dissociate the provisioned service only with plugins that handle other service types.
                //TODO why is this check done ?
                if (!serviceProvider.getServiceType().equals(svcPlugin.getServiceType())) {
                    Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                    for (ServiceReference serviceRef : appSRs) {
                        logger.log(Level.INFO, "Dissociating ProvisionedService " + serviceProvider + " for ServiceReference " + serviceRef + " through " + svcPlugin);
                        Collection<ProvisionedService> serviceConsumers = getServicesProvisionedByPlugin(svcPlugin, appProvisionedSvcs);
                        for (ProvisionedService serviceConsumer : serviceConsumers) {
                            svcPlugin.dissociateServices(serviceConsumer, serviceRef, serviceProvider, beforeUndeploy, context);
                        }

                    }
                }
            }
        }
    }

    private void associateProvisionedServices(Set<Plugin> installedPlugins,
                                              ServiceMetadata appServiceMetadata,
                                              Set<ProvisionedService> appProvisionedSvcs,
                                              boolean preDeployment, DeploymentContext context) {
        logger.entering(getClass().getName(), "associateProvisionedServices-beforeDeployment=" + preDeployment);
        for (ProvisionedService serviceProducer : appProvisionedSvcs) {
            for (Plugin<?> svcPlugin : installedPlugins) {
                //associate the provisioned service only with plugins that handle other service types.
                if (!serviceProducer.getServiceType().equals(svcPlugin.getServiceType())) {
                    Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                    for (ServiceReference serviceRef : appSRs) {
                        logger.log(Level.INFO, "Associating ProvisionedService " + serviceProducer + " for ServiceReference " + serviceRef + " through " + svcPlugin);
                        Collection<ProvisionedService> serviceConsumers = getServicesProvisionedByPlugin(svcPlugin, appProvisionedSvcs);
                        for(ProvisionedService serviceConsumer : serviceConsumers){
                            svcPlugin.associateServices(serviceConsumer, serviceRef, serviceProducer, preDeployment, context);
                        }
                    }
                }
            }
        }
    }

    private Collection<ProvisionedService> getServicesProvisionedByPlugin(Plugin plugin,
                                                                          Set<ProvisionedService> allProvisionedServices){
        List<ProvisionedService> provisionedServices = new ArrayList<ProvisionedService>();
        for(ProvisionedService ps : allProvisionedServices){
            if(ps.getServiceType().equals(plugin.getServiceType())){
                provisionedServices.add(ps);
            }
        }
        return provisionedServices;
    }

    private void unprovisionServices(Set<Plugin> installedPlugins, ServiceMetadata appServiceMetadata,
                                     DeploymentContext dc) {
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        for (ServiceDescription sd : appSDs) {
            Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
            logger.log(Level.INFO, "Unprovisioning Service for " + sd + " through " + chosenPlugin);
            chosenPlugin.unprovisionService(sd, dc);
        }
    }

    private Set<ProvisionedService> provisionServices(Set<Plugin> installedPlugins,
                                                      ServiceMetadata appServiceMetadata, DeploymentContext dc) {
        logger.entering(getClass().getName(), "provisionServices");
        Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();

        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        for (ServiceDescription sd : appSDs) {
            Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
            logger.log(Level.INFO, "Provisioning Service for " + sd + " through " + chosenPlugin);
            ProvisionedService ps = chosenPlugin.provisionService(sd, dc);
            appPSs.add(ps);
        }

        return appPSs;
    }

    private Plugin getPluginForServiceType(Set<Plugin> installedPlugins, String serviceType) {
        //XXX: for now assume that there is one plugin per servicetype
        //and choose the first plugin that handles this service type.
        //in the future, need to handle conflicts
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.getServiceType().toString().equalsIgnoreCase(serviceType)) return svcPlugin;
        }
        return null;
    }

    private boolean serviceDefinitionExistsForType(
            ServiceMetadata appServiceMetadata, ServiceType svcType) {
        for (ServiceDescription sd : appServiceMetadata.getServiceDescriptions()) {
            if (sd.getServiceType().equalsIgnoreCase(svcType.toString())) return true;
        }
        return false;
    }

    private void assertMetadataComplete(Set<ServiceDescription> appSDs,
                                        Set<ServiceReference> appSRs) {
        //Assert that all SRs have their corresponding SDs
        for (ServiceReference sr : appSRs) {
            String targetSD = sr.getTarget();
            boolean serviceDefinitionExists = false;
            for (ServiceDescription sd : appSDs) {
                if (sd.getName().equals(targetSD)) {
                    serviceDefinitionExists = true;
                }
            }
            assert serviceDefinitionExists;
        }
    }

    public void before(ExtendedDeploymentContext.Phase phase, ExtendedDeploymentContext context) {
        if (!usingDeployService) {
            //OpsParams tmp = context.getCommandParameters(OpsParams.class);
            //System.out.println("before" + phase + " " + tmp.command);
            //System.out.println("ApplicationLifecycleListener before : " + phase);
            OpsParams params = context.getCommandParameters(OpsParams.class);
            if (phase.equals(ExtendedDeploymentContext.Phase.PREPARE)) {
                if (serverEnvironment.isDas()) {
                    ReadableArchive archive = context.getSource();
                    if (params.origin == OpsParams.Origin.deploy) {
                        String appName = params.name();
                        provisionServicesForApplication(appName, archive, context);
                    }
                }
            } else if (phase.equals(ExtendedDeploymentContext.Phase.STOP)) {
                if (serverEnvironment.isDas()) {
                    ReadableArchive archive = context.getSource();
                    if(params.origin == OpsParams.Origin.undeploy){
                        if(params.command == OpsParams.Command.disable){
                            String appName = params.name();
                            prepareForUndeploy(appName, archive, context);
                        }
                    }
                }
            }
        }
    }

    public void after(ExtendedDeploymentContext.Phase phase, ExtendedDeploymentContext context) {
        if (!usingDeployService) {
            //OpsParams tmp = context.getCommandParameters(OpsParams.class);
            //System.out.println("after" + phase + " " + tmp.command);
            //System.out.println("ApplicationLifecycleListener after : " + phase);
            if (phase.equals(ExtendedDeploymentContext.Phase.REPLICATION)) {
                if (serverEnvironment.isDas()) {
                    OpsParams params = context.getCommandParameters(OpsParams.class);
                    ReadableArchive archive = context.getSource();
                    if (params.origin == OpsParams.Origin.deploy) {
                        String appName = params.name();
                        postDeploy(appName, archive, context);

                    }
                    //make sure that it is indeed undeploy and not disable.
                    //params.origin is "undeploy" for both "undeploy" as well "disable" phase
                    //hence using the actual command being used.
                    if(params.origin == OpsParams.Origin.undeploy){
                        if(params.command == OpsParams.Command.undeploy){
                            String appName = params.name();
                                postUndeploy(appName, context.getSource(), context);
                                serviceMetadata.remove(appName);
                                provisionedServices.remove(appName);
                        }
                    }
                }
            }
        }
    }

    public void setUsingDeployService(boolean usingDeployService) {
        this.usingDeployService = usingDeployService;
    }
}
