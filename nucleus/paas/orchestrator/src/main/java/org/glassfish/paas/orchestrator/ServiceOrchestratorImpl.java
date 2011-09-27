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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.admin.AdminCommandLock;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.internal.deployment.ApplicationLifecycleInterceptor;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Habitat;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;

@org.jvnet.hk2.annotations.Service
@Scoped(Singleton.class)
public class ServiceOrchestratorImpl implements ServiceOrchestrator, ApplicationLifecycleInterceptor {

    @Inject
    protected Habitat habitat;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private CommandRunner commandRunner;

    @Inject(optional = true) // injection optional for non-virtual scenario to work
    private VirtualClusters virtualClusters;

    private Map<String, ServiceMetadata> serviceMetadata = new HashMap<String, ServiceMetadata>();
    private Map<String, Set<ProvisionedService>> provisionedServices = new HashMap<String, Set<ProvisionedService>>();

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    public Set<Plugin> getPlugins() {
        Set<Plugin> plugins = new HashSet<Plugin>();
        plugins.addAll(habitat.getAllByContract(Plugin.class));
        logger.log(Level.INFO, "Discovered plugins:" + plugins);
        checkForDuplicatePlugins(plugins);
        return plugins;
    }

    private void checkForDuplicatePlugins(Set<Plugin> plugins) {
        Map<String, Plugin> serviceTypes = new HashMap<String, Plugin>();
        for(Plugin plugin : plugins){
            String serviceType = plugin.getServiceType().toString();
            if(serviceTypes.get(serviceType) != null){
                throw new RuntimeException("Support for choosing a plugin from multiple plugins ["+plugin.getClass().getName()+ "," +
                        serviceTypes.get(serviceType).getClass().getName() +"] that handle the service" +
                        "type ["+serviceType+"] is not yet available");
            }else{
                serviceTypes.put(serviceType, plugin);
            }
        }
    }


    /**
     * Discover the dependencies of the application and provision the various 
     * Services that are needed by the application.
     * 
     * @param appName Application Name
     * @param archive Application Archive
     * @param dc DeploymentContext associated with the current deployment operation
     */
    private void provisionServicesForApplication(String appName, ReadableArchive archive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "provisionServicesForApplication");
        //Get all plugins installed in this runtime
        Set<Plugin> installedPlugins = getPlugins();

        //1. Perform service dependency discovery
        ServiceMetadata appServiceMetadata = serviceDependencyDiscovery(appName, archive, installedPlugins);

        //2. Provision dependent services
        Set<ProvisionedService> appProvisionedSvcs = provisionServices(installedPlugins, appServiceMetadata, dc);
        logger.log(Level.FINE, "Provisioned Services for Application " + appName + " : " + appProvisionedSvcs);
        serviceMetadata.put(appName, appServiceMetadata);
        provisionedServices.put(appName, appProvisionedSvcs);

        //3. Associate provisioned services with each other
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, true /*before deployment*/, dc);
        logger.exiting(getClass().getName(), "provisionServicesForApplication");
    }

    public void postDeploy(String appName, ReadableArchive archive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "postDeploy");
        //4b. post-deployment association

        Set<Plugin> installedPlugins = getPlugins();
        ServiceMetadata appServiceMetadata = getServiceMetadata(appName);
        Set<ProvisionedService> appProvisionedSvcs = getProvisionedServices(appName);
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, false /*after deployment*/, dc);

        //TODO should we remove them, or book keep it till its stopped/undeployed ?
        //serviceMetadata.remove(appName);
        //provisionedServices.remove(appName);
        logger.exiting(getClass().getName(), "postDeploy");
    }

    private Set<ProvisionedService> getProvisionedServices(String appName) {
        return provisionedServices.get(appName);
    }


    public void deployApplication(String appName, ReadableArchive cloudArchive) {
        /*
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
                        appProvisionedSvcs, true */
        /*before deployment*//*
        , null);

                //4a. Application Deployment
                deployArchive(cloudArchive, installedPlugins);

                //4b. post-deployment association
                associateProvisionedServices(installedPlugins, appServiceMetadata,
                        appProvisionedSvcs, false */
        /*after deployment*//*
        , null);
        */
    }

    public void prepareForUndeploy(String appName, ReadableArchive archive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "prepareForUndeploy");
        //Get all plugins installed in this runtime
        Set<Plugin> installedPlugins = getPlugins();

        ServiceMetadata appServiceMetadata = getServiceMetadata(appName);
        if(appServiceMetadata == null){
            appServiceMetadata =
                    serviceDependencyDiscovery(appName, archive, installedPlugins);
            serviceMetadata.put(appName, appServiceMetadata);
        }

        Set<ProvisionedService> appProvisionedServices = getProvisionedServices(appName);
        if(appProvisionedServices == null){
            appProvisionedServices =
                    retrieveProvisionedServices(installedPlugins, appServiceMetadata, dc);
            provisionedServices.put(appName, appProvisionedServices);
        }

        dissociateProvisionedServices(installedPlugins, appServiceMetadata, appProvisionedServices, true, dc);
    }

    private ServiceMetadata getServiceMetadata(String appName) {
        return serviceMetadata.get(appName);
    }

    public void postUndeploy(String appName, ReadableArchive archive, DeploymentContext dc) {
        logger.entering(getClass().getName(), "postUndeploy");
        //4b. post-undeploy disassociation

        Set<Plugin> installedPlugins = getPlugins();
        ServiceMetadata appServiceMetadata = getServiceMetadata(appName);
        Set<ProvisionedService> appProvisionedSvcs = getProvisionedServices(appName);
        dissociateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, false /*after undeployment*/, dc);

        unprovisionServices(installedPlugins, appServiceMetadata, dc);

        logger.exiting(getClass().getName(), "postUndeploy");
    }

    public ServiceMetadata getServices(ReadableArchive archive){
        return serviceDependencyDiscovery(archive.getName(), archive, getPlugins());
    }


    private ServiceMetadata serviceDependencyDiscovery(String appName, ReadableArchive archive, Set<Plugin> installedPlugins) {
        logger.entering(getClass().getName(), "serviceDependencyDiscovery");
        //1. SERVICE DISCOVERY
        //parse glassfish-services.xml to get all declared SRs and SDs
        //Get the first ServicesXMLParser implementation

        ServicesXMLParser parser = habitat.getAllByContract(
                ServicesXMLParser.class).iterator().next();

        //1.1 discover all Service References and Descriptions already declared for this application
        ServiceMetadata appServiceMetadata = parser.discoverDeclaredServices(appName, archive);

        //if no meta-data is found, create empty ServiceMetadata
        if (appServiceMetadata == null) {
            appServiceMetadata = new ServiceMetadata();
            appServiceMetadata.setAppName(appName);
        }

        logger.log(Level.INFO, "Discovered declared service metadata via glassfish-services.xml = " + appServiceMetadata);

        //1.2 Get implicit service-descriptions (for instance a war is deployed, and it has not
        //specified a javaee service-description in its orchestration.xml, the PaaS runtime
        //through the GlassFish plugin that a default javaee service-description
        //is implied
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.handles(archive)) {
                //If a ServiceDescription has not been declared explicitly in
                //the application for the plugin's type, ask the plugin (since it 
                //supports this type of archive) if it has any implicit 
                //service-description for this application
                if (!serviceDescriptionExistsForType(appServiceMetadata, svcPlugin.getServiceType())) {
                    Set<ServiceDescription> implicitServiceDescs = svcPlugin.getImplicitServiceDescriptions(archive, appName);
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
            if (svcPlugin.handles(archive)) {
                Set<ServiceReference> implicitServiceRefs = svcPlugin.getServiceReferences(appName, archive);
                for (ServiceReference sr : implicitServiceRefs) {
                    System.out.println("ServiceReference:" + sr);
                    appServiceMetadata.addServiceReference(sr);
                }
            }
        }
        logger.log(Level.INFO, "After adding ServiceReferences = " + appServiceMetadata);
        Map<String, Plugin> existingSDs = new HashMap<String, Plugin>();
        //1.3 Ensure all service references have a related service description
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
        for (ServiceReference sr : appSRs) {
            String targetSD = sr.getTarget();
            String svcRefType = sr.getServiceRefType();
            boolean serviceDescriptionExists = false;
            for (ServiceDescription sd : appSDs) {
                //XXX: For now we assume all SRs are satisfied by app-scoped SDs
                //In the future this has to be modified to search in global SDs
                //as well
                if (sd.getName().equals(targetSD)) {
                    serviceDescriptionExists = true;
                }
            }
            if (!serviceDescriptionExists) {
                //create a default SD for this service ref and add to application's
                //service metadata
                for (Plugin svcPlugin : installedPlugins) {
                    if (svcPlugin.isReferenceTypeSupported(svcRefType)) {
                        ServiceDescription defSD = svcPlugin.getDefaultServiceDescription(appName, sr);
                        if(existingSDs.containsKey(defSD.getName())){
                            Plugin plugin = existingSDs.get(defSD.getName());
                            if(svcPlugin.getClass().equals(plugin.getClass()) && svcPlugin.getServiceType().equals(plugin.getServiceType())){
                                //service description provided by same plugin, avoid adding the service-description.
                                continue;
                            }else{
                                existingSDs.put(defSD.getName(), svcPlugin);
                            }
                        }else{
                            existingSDs.put(defSD.getName(), svcPlugin);
                        }
                        addServiceDescriptionWithoutDuplicate(appServiceMetadata, defSD);
                        continue; //ignore the rest of the plugins
                    }
                }
            }
        }

        assertMetadataComplete(appSDs, appSRs);
        logger.log(Level.INFO, "Final Service Metadata = " + appServiceMetadata);
        return appServiceMetadata;
    }

    private void addServiceDescriptionWithoutDuplicate(ServiceMetadata appServiceMetadata, ServiceDescription defSD) {
        Set<ServiceDescription> serviceDescriptions = appServiceMetadata.getServiceDescriptions();
        for(ServiceDescription sd : serviceDescriptions){
            if(sd.getName().equals(defSD.getName())){
                if(sd.getServiceType().equals(defSD.getServiceType())){
                    return; //duplicate. We may also have to check whether its provided by same plugin
                    //or implement equals in service-description so as to make it easier for comparisons.
                }
            }
        }
        appServiceMetadata.addServiceDescription(defSD);
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

    private void unprovisionServices(final Set<Plugin> installedPlugins, ServiceMetadata appServiceMetadata,
                                     final DeploymentContext dc) {
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        List<Future> unprovisioningFutures = new ArrayList<Future>();
        String virtualClusterName = getVirtualClusterName(appServiceMetadata);

        for (final ServiceDescription sd : appSDs) {
            sd.setVirtualClusterName(virtualClusterName);
            Future future = ServiceUtil.getThreadPool().submit(new Runnable() {
                public void run() {
                    Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
                    logger.log(Level.INFO, "Unprovisioning Service for " + sd + " through " + chosenPlugin);
                    chosenPlugin.unprovisionService(sd, dc);
                }
            });
            unprovisioningFutures.add(future);
        }

        boolean failed = false;
        for(Future future : unprovisioningFutures){
            try {
                future.get();
            } catch (InterruptedException e) {
                failed = true;
                e.printStackTrace();
            } catch (ExecutionException e) {
                failed = true;
                e.printStackTrace();
            }
        }
        if(failed){
            //TODO need a better mechanism ?
            throw new RuntimeException("Failure while unprovisioning services, refer server.log for more details");
        }

        // Clean up the glassfish cluster, virtual cluster config, etc..
        // TODO :: assuming app-scoped virtual cluster. fix it when supporting shared/external service.
        removeVirtualCluster(virtualClusterName);
    }

    // Name of the JavaEE service will be the name of the virtual cluster.
    private String getVirtualClusterName(ServiceMetadata appServiceMetadata) {
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        String virtualClusterName = null;
        for(ServiceDescription sd : appSDs) {
            if("JavaEE".equalsIgnoreCase(sd.getServiceType())) {
                virtualClusterName = sd.getName();
            }
        }
        if(virtualClusterName == null) {
            throw new RuntimeException("Application does not seem to contain any JavaEE " +
                    "service requirement. Hence unable compute the name of virtual cluster.");
        }
        return virtualClusterName;
    }

    private Set<ProvisionedService> provisionServices(final Set<Plugin> installedPlugins,
                                                      ServiceMetadata appServiceMetadata, final DeploymentContext dc) {
        logger.entering(getClass().getName(), "provisionServices");
        final Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();

        // create one virtual cluster per deployment unit.
        String virtualClusterName = getVirtualClusterName(appServiceMetadata);
        CommandResult result = commandRunner.run("create-cluster", virtualClusterName);
        logger.info("Command create-cluster [" + virtualClusterName + "] executed. " +
                "Command Output [" + result.getOutput() + "]");
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new RuntimeException("Failure while provisioning services, " +
                    "Unable to create cluster [" + virtualClusterName + "]");
        }
        
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        boolean failed = false;
        Exception rootCause = null;
        if (Boolean.getBoolean("org.glassfish.paas.orchestrator.parallel-provisioning")) {
            List<Future<ProvisionedService>> provisioningFutures = new ArrayList<Future<ProvisionedService>>();
            for (final ServiceDescription sd : appSDs) {
                sd.setVirtualClusterName(virtualClusterName);
                Future<ProvisionedService> future = ServiceUtil.getThreadPool().submit(new Callable<ProvisionedService>() {
                    public ProvisionedService call() {
                        Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
                        logger.log(Level.INFO, "Started Provisioning Service in parallel for " + sd + " through " + chosenPlugin);
                        return chosenPlugin.provisionService(sd, dc);
                    }
                });
                provisioningFutures.add(future);
            }


            for (Future<ProvisionedService> future : provisioningFutures) {
                try {
                    ProvisionedService ps = future.get();
                    appPSs.add(ps);
                    logger.log(Level.INFO, "Completed Provisioning Service in parallel " + ps);
                } catch (Exception e) {
                    failed = true;
                    logger.log(Level.WARNING, "Failure while provisioning service", e);
                    if (rootCause == null) {
                        rootCause = e; //we are caching only the first failure and logging all failures
                    }
                }
            }
        } else {

            for (final ServiceDescription sd : appSDs) {
                try {
                    sd.setVirtualClusterName(virtualClusterName);
                    Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
                    logger.log(Level.INFO, "Started Provisioning Service serially for " + sd + " through " + chosenPlugin);
                    ProvisionedService ps = chosenPlugin.provisionService(sd, dc);
                    appPSs.add(ps);
                    logger.log(Level.INFO, "Completed Provisioning Service serially " + ps);
                } catch (Exception e) {
                    failed = true;
                    logger.log(Level.WARNING, "Failure while provisioning service", e);
                    rootCause = e;
                    break; //since we are provisioning serially, we can abort
                }
            }
        }
        if(failed){
            for(ProvisionedService ps : appPSs){
                try{
                    ServiceDescription sd = ps.getServiceDescription();
                    Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
                    logger.log(Level.INFO, "Rolling back provisioned-service for " + sd + " through " + chosenPlugin );
                    chosenPlugin.unprovisionService(sd, dc); //TODO we could do unprovisioning in parallel.
                    logger.log(Level.INFO, "Rolled back provisioned-service for " + sd + " through " + chosenPlugin );
                }catch(Exception e){
                    logger.log(Level.FINEST, "Failure while rolling back provisioned service " + ps, e);
                }
            }

            // Clean up the glassfish cluster, virtual cluster config, etc..
            // TODO :: assuming app-scoped virtual cluster. fix it when supporting shared/external service.
            removeVirtualCluster(virtualClusterName);

            //XXX (Siva): Failure handling. Exception design.
            DeploymentException re = new DeploymentException("Failure while provisioning services");
            if(rootCause != null){
                re.initCause(rootCause);
            }
            throw re;
        }
        return appPSs;
    }

    private void removeVirtualCluster(String virtualClusterName) {
        try {
            VirtualCluster virtualCluster = virtualClusters.byName(virtualClusterName);
            if (virtualCluster != null) {
                virtualClusters.remove(virtualCluster);  // removes config.
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, ex.getLocalizedMessage(), ex);
        }

        /*
        stop-cluster is deliberately commented. invoking stop-cluster causes the
        re-deploy to fail next time (due to IMS layer fails to create virtual-machine config).
        But since all the instances in the cluster are already stopped we don't really need to call stop-cluster.

       CommandResult commandResult = commandRunner.run("stop-cluster", virtualClusterName);
       Throwable failureCause = commandResult.getFailureCause();
       if (failureCause != null) {
           logger.log(Level.WARNING, failureCause.getLocalizedMessage(), failureCause);
       }
        */
        CommandResult commandResult = commandRunner.run("delete-cluster", virtualClusterName);
        logger.info("Command delete-cluster [" + virtualClusterName + "] executed. " +
                "Command Output [" + commandResult.getOutput() + "]");
        Throwable failureCause = commandResult.getFailureCause();
        if (failureCause != null) {
            logger.log(Level.WARNING, failureCause.getLocalizedMessage(), failureCause);
        }
    }
    
    private Set<ProvisionedService> retrieveProvisionedServices(final Set<Plugin> installedPlugins,
                                                      ServiceMetadata appServiceMetadata, final DeploymentContext dc) {
        logger.entering(getClass().getName(), "retrieveProvisionedServices");
        final Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();
        String appName = getAppName(dc);
        String virtualClusterName = getVirtualClusterName(appServiceMetadata);
        System.out.println("Retrieve PS for app=" + appName + " virtualCluster=" + virtualClusterName);
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        for (final ServiceDescription sd : appSDs) {
                //Temporary workaround to set virtual-cluster in all ProvisionedServices
                sd.setVirtualClusterName(virtualClusterName);
                
                Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
                logger.log(Level.INFO, "Retrieving provisioned Service for " + sd + " through " + chosenPlugin);
                ServiceInfo serviceInfo = serviceUtil.retrieveCloudEntry(sd.getName(), appName, null );
                if(serviceInfo != null){
                    ProvisionedService ps = chosenPlugin.getProvisionedService(sd, serviceInfo);
                    appPSs.add(ps);
                }else{
                    logger.warning("unable to retrieve service-info for service : " + sd.getName() + " of application : " + appName);
                }
        }
        return appPSs;
    }

    private String getAppName(DeploymentContext dc) {
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        return params.name();
    }


    private Plugin<?> getPluginForServiceType(Set<Plugin> installedPlugins, String serviceType) {
        //XXX: for now assume that there is one plugin per servicetype
        //and choose the first plugin that handles this service type.
        //in the future, need to handle conflicts
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.getServiceType().toString().equalsIgnoreCase(serviceType)) return svcPlugin;
        }
        return null;
    }

    private boolean serviceDescriptionExistsForType(
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
            boolean serviceDescriptionExists = false;
            for (ServiceDescription sd : appSDs) {
                if (sd.getName().equals(targetSD)) {
                    serviceDescriptionExists = true;
                }
            }
            assert serviceDescriptionExists;
        }
    }

    //NOTE : refer & update isValidDeploymentTarget if needed as we are dependent on the list of "Origins" used in this method.
    public void before(final ExtendedDeploymentContext.Phase phase, final ExtendedDeploymentContext context) {

        logEvent(true, phase, context);

        if (isOrchestrationEnabled(context) && serverEnvironment.isDas()) {
            AdminCommandLock.runWithSuspendedLock(new Runnable() {
                public void run() {
                    if (phase.equals(ExtendedDeploymentContext.Phase.PREPARE)) {
                        ReadableArchive archive = context.getSource();
                        OpsParams params = context.getCommandParameters(OpsParams.class);
                        String appName = params.name();
                        if (params.origin == OpsParams.Origin.deploy) {
                            provisionServicesForApplication(appName, archive, context);
                        } else if (params.origin == OpsParams.Origin.load) {
                            if (params.command == OpsParams.Command.startup_server) {
                                if (isValidApplication(appName)) {
                                    Set<Plugin> installedPlugins = getPlugins();
                                    ServiceMetadata appServiceMetadata =
                                            serviceDependencyDiscovery(appName, archive, installedPlugins);
                                    serviceMetadata.put(appName, appServiceMetadata);
                                    Set<ProvisionedService> provisionedServiceSet =
                                            retrieveProvisionedServices(installedPlugins, appServiceMetadata, context);
                                    provisionedServices.put(appName, provisionedServiceSet);
                                }
                            } else {
                                if (params.command == OpsParams.Command.enable) {
                                    if (isValidApplication(appName)) {
                                        Set<Plugin> installedPlugins = getPlugins();
                                        ServiceMetadata appServiceMetadata =
                                                serviceDependencyDiscovery(appName, archive, installedPlugins);
                                        serviceMetadata.put(appName, appServiceMetadata);

                                        Set<ProvisionedService> provisionedServiceSet =
                                                startServices(installedPlugins, appServiceMetadata, context);
                                        provisionedServices.put(appName, provisionedServiceSet);
                                    }
                                }
                            }
                        }
                    } else if (phase.equals(ExtendedDeploymentContext.Phase.STOP)) {
                        ReadableArchive archive = context.getSource();
                        OpsParams params = context.getCommandParameters(OpsParams.class);
                        String appName = params.name();
                        if (params.origin == OpsParams.Origin.undeploy) {
                            if (params.command == OpsParams.Command.disable) {
                                prepareForUndeploy(appName, archive, context);
                            }
                        }
                    }
                }
            });
        }
    }


    private boolean isValidApplication(String appName) {
        boolean isValid = true;
        //TODO check whether the application uses any <services> and then invoke orchestrator.
        //TODO this is needed as it is possible to deploy the application before enabling
        //TODO virtualization (add-virtualization).

        return isValid;
    }

    //NOTE : refer & update isValidDeploymentTarget if needed as we are dependent on the list of "Origins" used in this method.
    public void after(final ExtendedDeploymentContext.Phase phase, final ExtendedDeploymentContext context) {

        logEvent(false, phase, context);

        if (isOrchestrationEnabled(context)) {
            AdminCommandLock.runWithSuspendedLock(new Runnable() {
                public void run() {
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
                            if (params.origin == OpsParams.Origin.undeploy) {
                                if (params.command == OpsParams.Command.undeploy) {
                                    String appName = params.name();
                                    postUndeploy(appName, context.getSource(), context);
                                    serviceMetadata.remove(appName);
                                    provisionedServices.remove(appName);
                                }
                            }
                            //TODO as of today, we get only after-CLEAN-unload-disable event.
                            //TODO we expect after-STOP-unload-disable, but since the target is not "DAS",
                            //TODO DAS will not receive such event.
                            String appName = params.name();
                            if (params.origin == OpsParams.Origin.unload) {
                                if (params.command == OpsParams.Command.disable) {
                                    if (isValidApplication(appName)) {
                                        Set<Plugin> installedPlugins = getPlugins();
                                        ServiceMetadata appServiceMetadata = getServiceMetadata(appName);
                                        stopServices(installedPlugins, appServiceMetadata, context);
                                        serviceMetadata.remove(appName);
                                        provisionedServices.remove(appName);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void logEvent(boolean before, ExtendedDeploymentContext.Phase phase, ExtendedDeploymentContext context) {
        try{
            StringBuilder sb = new StringBuilder();
            if(before){
                sb.append("ServiceOrchestrator receiving event \n { [Before] ");
            }else{
                sb.append("ServiceOrchestrator receiving event \n { [After] ");
            }

            sb.append(" [Phase : "+phase.toString()+"]");
            if(context != null){
                OpsParams params = context.getCommandParameters(OpsParams.class);
                sb.append(" [Command : "+params.command+"]");
                sb.append(" [Origin : "+params.origin+"]");
            }else{
                sb.append(" [DeploymentContext is null, command and origin not available]");
            }
            sb.append(" }");
            logger.log(Level.INFO, sb.toString());
        }catch(Exception e){
            //ignore, this is debugging info.
        }
    }


    private Set<ProvisionedService> startServices(Set<Plugin> installedPlugins, ServiceMetadata appServiceMetadata,
                                                  DeploymentContext context) {
        Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();
        String appName = getAppName(context);
        for(ServiceDescription sd : appServiceMetadata.getServiceDescriptions()){
            Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
            logger.log(Level.INFO, "Retrieving provisioned Service for " + sd + " through " + chosenPlugin);
            ServiceInfo serviceInfo = serviceUtil.retrieveCloudEntry(sd.getName(), appName, null );
            if(serviceInfo != null){
                ProvisionedService ps = chosenPlugin.startService(sd, serviceInfo);
                appPSs.add(ps);
            }else{
                logger.warning("unable to retrieve service-info for service : " + sd.getName() + " of application : " + appName);
            }
        }
        return appPSs;
    }

    private void stopServices(Set<Plugin> installedPlugins, ServiceMetadata appServiceMetadata,
                                                  DeploymentContext context) {
        String appName = getAppName(context);
        for(ServiceDescription sd : appServiceMetadata.getServiceDescriptions()){
            Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
            ServiceInfo serviceInfo = serviceUtil.retrieveCloudEntry(sd.getName(), appName, null );
            if(serviceInfo != null){
                chosenPlugin.stopService(sd, serviceInfo);
            }else{
                logger.warning("unable to retrieve service-info for service : " + sd.getName() + " of application : " + appName);
            }
        }
    }

    private boolean isVirtualizationEnabled(){
        boolean isVirtualEnvironment = false;
        Virtualizations v = domain.getExtensionByType(Virtualizations.class);
        if (v!=null && v.getVirtualizations().size()>0) {
                isVirtualEnvironment = true;
        }
        return isVirtualEnvironment;
    }

    private boolean isOrchestrationEnabled(DeploymentContext dc){
        return (isVirtualizationEnabled() && isValidDeploymentTarget(dc)) ||
                Boolean.getBoolean("org.glassfish.paas.orchestrator.enabled");
    }

    private boolean isValidDeploymentTarget(DeploymentContext dc) {
        if(dc == null){
            return false;
        }

        String target = null;
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if(params.origin == OpsParams.Origin.deploy || params.origin == OpsParams.Origin.load){
            DeployCommandParameters dcp = dc.getCommandParameters(DeployCommandParameters.class);
            target = dcp.target;
        }else  if(params.origin == OpsParams.Origin.undeploy || params.origin == OpsParams.Origin.unload){
            UndeployCommandParameters dcp = dc.getCommandParameters(UndeployCommandParameters.class);
            target = dcp.target;
        }else{
            return false;//we do not handle other "Origins" for now.
        }

        if(target == null){
            return true; // if target is null, we assume that its PaaS styled deployment.
        }

        //hack. temporary fix.
        if (target.equals("server")) {
            if (params.origin == OpsParams.Origin.load && params.command == OpsParams.Command.startup_server) {
                String appName = params.name();

                List<String> targets =
                        domain.getAllReferencedTargetsForApplication(appName);
                if (targets.size() == 1) {
                    target = targets.get(0);
                    /*
                    DeployCommandParameters dcp = dc.getCommandParameters(DeployCommandParameters.class);
                    dcp.target = target;
                    */
                }
            }
        }

        Cluster cluster = domain.getClusterNamed(target);
        if(cluster != null){
            List<VirtualMachineConfig> vmcList = cluster.getExtensionsByType(VirtualMachineConfig.class);
            if(vmcList != null && vmcList.size()  > 0){
                return true;
            }else{
                return false; //not a virtual cluster.
            }
        }else{
            //target is not cluster or no such target exists.
            return false;
        }
    }


    @Override
    public boolean scaleService(String appName, String svcName,
            int scaleCount, AllocationStrategy allocStrategy) {
        System.out.println("Scaling Service " + svcName + " for Application " 
                                + appName + " by " + scaleCount + " instances");

        //Get Old PS
        Set<ProvisionedService> appPS = provisionedServices.get(appName);
        logger.log(Level.FINE, "appPS: " + appPS);
        ProvisionedService oldPS = null;
        for(ProvisionedService ps: appPS) {
            if (ps.getName().equals(svcName)) oldPS = ps;
        }
        logger.log(Level.FINE, "oldPS: " + oldPS);
        
        //Find Plugin that provided this Service
        Set<Plugin> installedPlugins = getPlugins();
        Plugin<?> chosenPlugin = getPluginForServiceType(
                installedPlugins, oldPS.getServiceDescription().getServiceType());
        
        //ask it to scale the service and get new PS
        logger.log(Level.INFO, "Scaling Service " + svcName 
                + " using Plugin:" + chosenPlugin);
        ProvisionedService newPS = chosenPlugin.scaleService(
                oldPS.getServiceDescription(), scaleCount, allocStrategy);
        logger.log(Level.INFO, "New Provisioned Service after scaling " + svcName 
                + " is:" + newPS);
        
        //Simple assertions to ensure that we have the scaled Service.
        assert newPS.getName().equals(oldPS.getName());
        assert newPS.getServiceType().equals(oldPS.getServiceType());
        
        //now re-associate all plugins with the new PS.
        ServiceMetadata appServiceMetadata = serviceMetadata.get(appName); 
        for (Plugin<?> svcPlugin : installedPlugins) {
            //re-associate the new PS only with plugins that handle other service types.
            if (!newPS.getServiceType().equals(svcPlugin.getServiceType())) {
                Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                for (ServiceReference serviceRef : appSRs) {
                    logger.log(Level.INFO, "Re-associating New ProvisionedService " 
                            + newPS + " for ServiceReference " + serviceRef 
                            + " through " + svcPlugin);
                    Collection<ProvisionedService> serviceConsumers = 
                            getServicesProvisionedByPlugin(svcPlugin, appPS);
                    for(ProvisionedService serviceConsumer : serviceConsumers){
                        svcPlugin.reassociateServices(serviceConsumer, oldPS, 
                                newPS, ReconfigAction.AUTO_SCALING);
                    }
                }
            }
        }
        
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public ServiceDescription getServiceDescription (String appName, String service) {
        
        ServiceMetadata appServiceMetadata = this.getServiceMetadata(appName);
        
        for(ServiceDescription desc : appServiceMetadata.getServiceDescriptions()){
            if (desc.getName().equals(service)){
                return desc;
            }
        }
        return null;
    }
}
