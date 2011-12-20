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

import org.glassfish.api.admin.AdminCommandLock;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.paas.orchestrator.config.*;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.ServiceScope;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.*;
import org.glassfish.paas.orchestrator.state.*;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Habitat;


@org.jvnet.hk2.annotations.Service
@Scoped(Singleton.class)
public class ServiceOrchestratorImpl implements ServiceOrchestrator {

    @Inject
    protected Habitat habitat;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private VirtualClusters virtualClusters;

    private Map<String, ServiceMetadata> serviceMetadata = new LinkedHashMap<String, ServiceMetadata>();
    private Map<String, Set<ProvisionedService>> provisionedServices = new LinkedHashMap<String, Set<ProvisionedService>>();
    private Map<String, Set<ConfiguredService>> configuredServices = new LinkedHashMap<String, Set<ConfiguredService>>();
    private Map<String, ProvisionedService> sharedServices = new LinkedHashMap<String, ProvisionedService>();
    private Map<String, ConfiguredService> externalServices = new LinkedHashMap<String, ConfiguredService>();

    private static final Class [] PRE_DEPLOY_PHASE_STATES = {ServiceDependencyDiscoveryState.class, ProvisioningState.class,
            SharedServiceRegistrationState.class, ConfiguredServiceRegistrationState.class, ServiceReferenceRegistrationState.class,
            PreDeployAssociationState.class};
    private static final Class [] POST_DEPLOY_PHASE_STATES = {PostDeployAssociationState.class, DeploymentCompletionState.class};
    private static final Class [] PRE_UNDEPLOY_PHASE_STATES = {PreUndeployDissociationState.class};
    private static final Class [] POST_UNDEPLOY_PHASE_STATES = {PostUndeployDissociationState.class,
            ServiceReferenceUnregisterState.class, ConfiguredServiceUnregisterState.class, SharedServiceUnregisterState.class,
            UnprovisioningState.class};
    private static final Class [] ENABLE_PHASE_STATES = {ServiceDependencyDiscoveryState.class, SharedServiceRegistrationState.class,
            ConfiguredServiceRegistrationState.class , EnableState.class};
    private static final Class [] DISABLE_PHASE_STATES = {DisableState.class, SharedServiceUnregisterState.class,
            ConfiguredServiceUnregisterState.class, DisableCompletionState.class };
    private static final Class [] SERVER_STARTUP_PHASE_STATES = {ServiceDependencyDiscoveryState.class,
            SharedServiceRegistrationState.class, ConfiguredServiceRegistrationState.class,  ServerStartupState.class,
            };
    private static final List<Class> DEPLOYMENT_STATES = new ArrayList<Class>();

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());
    private Set<Plugin> pluginsSet = null;

    public static final String ORCHESTRATOR_UNDEPLOY_CALL = "orchestrator.undeploy.call";

    static{
        composeDeploymentStates();
    }

    private static void composeDeploymentStates() {
        Collections.addAll(DEPLOYMENT_STATES, PRE_DEPLOY_PHASE_STATES);
        DEPLOYMENT_STATES.add(DeployState.class);
        Collections.addAll(DEPLOYMENT_STATES, POST_DEPLOY_PHASE_STATES);
    }

    public static Collection<Class> getAllStates(){
        //TODO for now we will have support of atomicity only during deployment, enable, disable.
        Set<Class> allStates = new LinkedHashSet<Class>();
        allStates.addAll(DEPLOYMENT_STATES);
        Collections.addAll(allStates, ENABLE_PHASE_STATES);
        Collections.addAll(allStates, DISABLE_PHASE_STATES);
        return Collections.unmodifiableSet(allStates);
    }

    public Set<Plugin> getPlugins(ServiceMetadata appServiceMetadata) {
        Set<Plugin> plugins = new LinkedHashSet<Plugin>();
        for(ServiceDescription sd : appServiceMetadata.getServiceDescriptions()){
            plugins.add(sd.getPlugin());
        }
        return plugins;
    }

    public Set<Plugin> getPlugins() {
        if(pluginsSet == null){
            Set<Plugin> plugins = new LinkedHashSet<Plugin>();
            plugins.addAll(habitat.getAllByContract(Plugin.class));
            logger.log(Level.INFO, "Discovered plugins:" + plugins);
            pluginsSet = plugins;
        }
        return pluginsSet;
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

    public Set<org.glassfish.paas.orchestrator.service.spi.Service> getServicesForAssociation(String appName){
        Set<org.glassfish.paas.orchestrator.service.spi.Service> servicesSet =
                new LinkedHashSet<org.glassfish.paas.orchestrator.service.spi.Service>();
            servicesSet.addAll(getProvisionedServices(appName));
            servicesSet.addAll(getConfiguredServices(appName));
        return servicesSet;
    }

    public Set<org.glassfish.paas.orchestrator.service.spi.Service> getServicesForDissociation(String appName){
        return getServicesForAssociation(appName);
    }

    public ServiceMetadata getServiceMetadata(String appName) {
        return serviceMetadata.get(appName);
    }

    private void orchestrateTask(Class[] tasks, String appName, DeploymentContext dc, boolean deployment) {
        for(Class clz : tasks){
            PaaSDeploymentState state = habitat.getByType(clz.getName());
            PaaSDeploymentContext pc = new PaaSDeploymentContext(appName, dc);
            try{
                state.beforeExecution(pc);
                state.handle(pc);
                state.afterExecution(pc);
            }catch(PaaSDeploymentException e){
                handleFailure(appName, tasks, deployment, state, pc, e);
            }catch(Exception e){
                handleFailure(appName, tasks, deployment, state, pc, e);
            }
        }
    }

    private void handleFailure(String appName, Class[] tasks, boolean deployment, PaaSDeploymentState state,
                               PaaSDeploymentContext pc, Exception e) {
        logger.log(Level.WARNING, "Failure while handling [ " + state.getClass().getSimpleName() + " ] : ", e);
        if(deployment){
            rollbackDeployment(pc, state, DEPLOYMENT_STATES);
            DeploymentException de = new DeploymentException("Failure while deploying application [ "+appName+" ], " +
                    "rolled back all deploy operations.");
            de.initCause(e);
            throw de;

        }else{
            DeploymentException de = new DeploymentException("Failure while undeploying application [ "+appName+" ]." );
            de.initCause(e);
            throw de;
        }
    }

    private void rollbackDeployment(PaaSDeploymentContext context, PaaSDeploymentState failedState, List<Class> tasksList) {
        int index = tasksList.indexOf(failedState.getClass());
        if(index == -1){
            logger.log(Level.WARNING, "No such task [ "+failedState.getClass()+" ] found to initiate RollBack");
            return;
        }
        List<Class> tmpTasksList = new ArrayList<Class>();
        tmpTasksList.addAll(tasksList);
        List<Class> rollbackTasksList = tmpTasksList.subList(0, index);
        Collections.reverse(rollbackTasksList);
        for(Class clz : rollbackTasksList){
            PaaSDeploymentState state = habitat.getByType(clz.getName());
            Class rollbackClz = state.getRollbackState();
            if(rollbackClz != null){
                PaaSDeploymentState rollbackState = habitat.getByType(rollbackClz.getName());
                try{
                    rollbackState.handle(context);
                }catch(Exception e){
                    // we cannot handle failures while rolling back.
                    // continue rolling back.
                    logger.log(Level.WARNING,
                            "Failure while rolling back [Application : "+context.getAppName()+"], " +
                                    "[State : "+rollbackState.getClass().getSimpleName()+"]", e);
                }
            }
        }
    }

    /**
     * Discover the dependencies of the application and provision the various
     * Services that are needed by the application.
     *
     * @param appName Application Name
     * @param dc DeploymentContext associated with the current deployment operation
     */
    private void provisionServicesForApplication(String appName, DeploymentContext dc) {
        logger.entering(getClass().getName(), "provisionServicesForApplication");
        orchestrateTask(PRE_DEPLOY_PHASE_STATES, appName, dc, true);
        logger.exiting(getClass().getName(), "provisionServicesForApplication");
    }

    public void postDeploy(String appName, DeploymentContext dc) {
        logger.entering(getClass().getName(), "postDeploy");
        orchestrateTask(POST_DEPLOY_PHASE_STATES, appName, dc, true);
        logger.exiting(getClass().getName(), "postDeploy");
    }

    public void startup(String appName, DeploymentContext dc) {
        logger.entering(getClass().getName(), "server-startup");
        orchestrateTask(SERVER_STARTUP_PHASE_STATES, appName, dc, false);
        logger.exiting(getClass().getName(), "server-startup");
    }

    public void enable(String appName, DeploymentContext dc) {
        logger.entering(getClass().getName(), "enable");
        orchestrateTask(ENABLE_PHASE_STATES, appName, dc, false);
        logger.exiting(getClass().getName(), "enable");
    }

    public void disable(String appName, ExtendedDeploymentContext dc) {
        logger.entering(getClass().getName(), "disable");
        orchestrateTask(DISABLE_PHASE_STATES, appName, dc, false);
        logger.exiting(getClass().getName(), "disable");
    }

    public void preUndeploy(String appName, DeploymentContext dc) {
        logger.entering(getClass().getName(), "preUndeploy");
        if(!isOrchestratorInitiatedUndeploy(dc.getCommandParameters(OpsParams.class))){
            orchestrateTask(PRE_UNDEPLOY_PHASE_STATES, appName, dc, false);
        }
        logger.exiting(getClass().getName(), "preUndeploy");
    }

    public void postUndeploy(String appName, DeploymentContext dc) {
        logger.entering(getClass().getName(), "postUndeploy");
        if(!isOrchestratorInitiatedUndeploy(dc.getCommandParameters(OpsParams.class))){
            orchestrateTask(POST_UNDEPLOY_PHASE_STATES, appName, dc, false);
        }
        logger.exiting(getClass().getName(), "postUndeploy");
    }

    private boolean isOrchestratorInitiatedUndeploy(OpsParams params) {
        if(params instanceof UndeployCommandParameters){
            UndeployCommandParameters ucp = (UndeployCommandParameters)params;
            if(ucp.properties != null){
                if(Boolean.valueOf(ucp.properties.getProperty(ServiceOrchestratorImpl.ORCHESTRATOR_UNDEPLOY_CALL, "false"))){
                    return true;
                }
            }
        }
        return false;
    }

    public ServiceMetadata getServices(ReadableArchive archive) throws Exception {
        ServiceDependencyDiscoveryState state = habitat.getByType(ServiceDependencyDiscoveryState.class);
        PaaSDeploymentContext pc = new PaaSDeploymentContext(archive.getName(), null);
        return state.getServiceDependencyMetadata(pc, archive.getName(), archive);
    }

    public Collection<org.glassfish.paas.orchestrator.service.spi.Service> getServicesManagedByPlugin(Plugin plugin,
                                              Set<org.glassfish.paas.orchestrator.service.spi.Service> allServices){
        List<org.glassfish.paas.orchestrator.service.spi.Service> services =
                new ArrayList<org.glassfish.paas.orchestrator.service.spi.Service>();
        for(org.glassfish.paas.orchestrator.service.spi.Service service : allServices){
            if(service.getServiceType().equals(plugin.getServiceType())){
                services.add(service);
            }
        }
        return services;
    }

    public Collection<ProvisionedService> getServicesProvisionedByPlugin(Plugin plugin,
                                              Set<ProvisionedService> provisionedServices){
        List<ProvisionedService> services =
                new ArrayList<ProvisionedService>();
        for(ProvisionedService service : provisionedServices){
            if(service.getServiceType().equals(plugin.getServiceType())){
                services.add(service);
            }
        }
        return services;
    }

    // Name of the JavaEE service will be the name of the virtual cluster.
    public String getVirtualClusterName(ServiceMetadata appServiceMetadata) {
        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        String virtualClusterName = null;
        for(ServiceDescription sd : appSDs) {
            //TODO check whether the service-scope is app-scoped and set it.
            if("JavaEE".equalsIgnoreCase(sd.getServiceType()) && !ServiceScope.SHARED.equals(sd.getServiceScope())) {
                virtualClusterName = sd.getName();
            }
        }
        return virtualClusterName;
    }

    public Collection<ServiceDescription> getServiceDescriptionsToProvision(String appName){
        ServiceMetadata appServiceMetadata = getServiceMetadata(appName);
        Collection<ServiceDescription> serviceDescriptions = appServiceMetadata.getServiceDescriptions();
        List<ServiceDescription> sdsToProvision = new ArrayList<ServiceDescription>();
        for(ServiceDescription sd : serviceDescriptions){
            if(!ServiceScope.SHARED.equals(sd.getServiceScope()) && !ServiceScope.EXTERNAL.equals(sd.getServiceScope())){
                sdsToProvision.add(sd);
            }
        }
        return sdsToProvision;
    }

    public Collection<ServiceDescription> getServiceDescriptionsToUnprovision(String appName){
        return getServiceDescriptionsToProvision(appName);
    }


    public void removeVirtualCluster(String virtualClusterName) {
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

    public Plugin getDefaultPluginForServiceRef(String serviceRefType) {
        Plugin defaultPlugin = null;

        List<Plugin> matchingPlugin = new ArrayList<Plugin>();
        for (Plugin plugin : getPlugins()) {
            if (plugin.isReferenceTypeSupported(serviceRefType)) {
                matchingPlugin.add(plugin);
            }
        }
        //TODO we are assuming that no two different plugin types will support same service-ref type
        for (Plugin plugin : matchingPlugin) {
            ServiceProvisioningEngines spes = habitat.getComponent(ServiceProvisioningEngines.class);
            if (spes != null) {
                for (ServiceProvisioningEngine spe : spes.getServiceProvisioningEngines()) {
                    if (spe.getType().equalsIgnoreCase(plugin.getServiceType().toString()) && spe.getDefault()) {
                        String className = spe.getClassName();
                        if (plugin.getClass().getName().equals(className)) {
                            defaultPlugin = plugin;
                            break;
                        }
                    }
                }
            }
        }
        return defaultPlugin;
    }

    public Plugin getDefaultPlugin(Collection<Plugin> pluginsList, String type) {
        Plugin defaultPlugin = null;
        if(pluginsList != null){
            ServiceProvisioningEngines spes = habitat.getComponent(ServiceProvisioningEngines.class);
            if(spes != null){
                for(ServiceProvisioningEngine spe : spes.getServiceProvisioningEngines()){
                    if(spe.getType().equalsIgnoreCase(type) && spe.getDefault()){
                        String className = spe.getClassName();
                        for(Plugin plugin : pluginsList){
                            if(plugin.getClass().getName().equals(className)){
                                defaultPlugin = plugin;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return defaultPlugin;
    }

    @Override
    public boolean scaleService(final String appName, final String svcName,
            final int scaleCount, final AllocationStrategy allocStrategy) {
        logger.log(Level.INFO, "Scaling Service " + svcName + " for Apprelication "
                + appName + " by " + scaleCount + " instances");
        
        
        AdminCommandLock.runWithSuspendedLock(new Runnable() {
            public void run() {
            /*
             * At this point in time, the CEM passes the Service Name as the
             * app Name and hence a temporary workaround to find the right appName
             */
            //Hack starts here.
            String effectiveAppName = appName;
            
            String tmpAppName = null;
            for (String app: provisionedServices.keySet()){
                logger.log(Level.INFO, "Checking app for Service " + svcName);
                Set<ProvisionedService> appsServices = getProvisionedServices(app);
                for(ProvisionedService p: appsServices){
                    if (p.getName().equals(svcName)) {
                        tmpAppName = app;
                    }
                }
            }
            if (tmpAppName != null) {
                effectiveAppName = tmpAppName; //reset
                logger.log(Level.INFO, "Setting application name as appName");
            }
            //Hack ends here.
    
            //Get Old PS
            Set<ProvisionedService> appPS = getProvisionedServices(effectiveAppName);
            logger.log(Level.FINE, "appPS: " + appPS);
            ProvisionedService oldPS = null;
            for(ProvisionedService ps: appPS) {
                if (ps.getName().equals(svcName)) oldPS = ps;
            }
            logger.log(Level.FINE, "oldPS: " + oldPS);
            
            //Find Plugin that provided this Service
            //Set<Plugin> installedPlugins = getPlugins();
/*
            Plugin<?> chosenPlugin = getPluginForServiceType(
                    installedPlugins, oldPS.getServiceDescription().getServiceType());
*/
            Plugin<?> chosenPlugin = oldPS.getServiceDescription().getPlugin();

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
            ServiceMetadata appServiceMetadata = serviceMetadata.get(effectiveAppName);
            Set<Plugin> plugins = getPlugins(appServiceMetadata);
            for (Plugin<?> svcPlugin : plugins) {
                //re-associate the new PS only with plugins that handle other service types.
                if (!newPS.getServiceType().equals(svcPlugin.getServiceType())) {
                    Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                    for (ServiceReference serviceRef : appSRs) {
                        logger.log(Level.INFO, "Re-associating New ProvisionedService " 
                                + newPS + " for ServiceReference " + serviceRef 
                                + " through " + svcPlugin);
                        Collection<org.glassfish.paas.orchestrator.service.spi.Service> serviceConsumers =
                                getServicesManagedByPlugin(svcPlugin, getServicesForAssociation(appName));
                        for(org.glassfish.paas.orchestrator.service.spi.Service serviceConsumer : serviceConsumers){
                            svcPlugin.reassociateServices(serviceConsumer, oldPS, 
                                    newPS, ReconfigAction.AUTO_SCALING);
                        }
                    }
                }
            }
            }
        });
        
        return true;
    }


    /**
     * @inheritDoc
     */
    @Override
    public ServiceDescription getServiceDescription (String appName, String service) {
        ServiceMetadata appServiceMetadata = getServiceMetadata(appName);
        for(ServiceDescription desc : appServiceMetadata.getServiceDescriptions()){
            if (desc.getName().equals(service)){
                return desc;
            }
        }
        return null;
    }

    public void undeploy(OpsParams params, ExtendedDeploymentContext context) {
        String appName = params.name();
        postUndeploy(appName, context);
    }

    public void addServiceMetadata(String appName, ServiceMetadata appServiceMetadata) {
        serviceMetadata.put(appName, appServiceMetadata);
    }

    public boolean unregisterProvisionedServices(String appName, Collection<ProvisionedService> provisionedServices) {
        return getProvisionedServices(appName).removeAll(provisionedServices);
    }

    public boolean unregisterConfiguredServices(String appName, Collection<ConfiguredService> configuredServices) {
        return getConfiguredServices(appName).removeAll(configuredServices);
    }

    public void registerProvisionedServices(String appName, Collection<ProvisionedService> provisionedServices) {
        getProvisionedServices(appName).addAll(provisionedServices);
    }

    public void registerConfiguredServices(String appName, Collection<ConfiguredService> configuredServices) {
        getConfiguredServices(appName).addAll(configuredServices);
    }

    private Set<ProvisionedService> getProvisionedServices(String appName) {
        Set<ProvisionedService> ps = provisionedServices.get(appName);
        if(ps == null){
            ps = new LinkedHashSet<ProvisionedService>();
            provisionedServices.put(appName, ps);
        }
        return provisionedServices.get(appName);
    }

    private Set<ConfiguredService> getConfiguredServices(String appName) {
        Set<ConfiguredService> cs = configuredServices.get(appName);
        if(cs == null){
            cs = new LinkedHashSet<ConfiguredService>();
            configuredServices.put(appName, cs);
        }
        return configuredServices.get(appName);
    }

    public void addSharedService(String serviceName, ProvisionedService provisionedService) {
        sharedServices.put(serviceName, provisionedService);
    }

    public void addExternalService(String serviceName, ConfiguredService configuredService){
        externalServices.put(serviceName, configuredService);
    }

    public ConfiguredService removeExternalService(String serviceName){
        return externalServices.remove(serviceName);
    }

    public ProvisionedService removeSharedService(String serviceName) {
        return sharedServices.remove(serviceName);
    }

    public ConfiguredService getConfiguredService(String serviceName){
        ConfiguredService configuredService = externalServices.get(serviceName);
        if(configuredService == null){
            ServiceDescription sd = getExternalServiceDescription(serviceName);
            configuredService = serviceUtil.getExternalService(serviceName);
            externalServices.put(serviceName, configuredService);
        }
        if(configuredService == null){
            throw new RuntimeException("No such external service ["+serviceName+"] is available");
        }
        return configuredService;
    }

    public ProvisionedService getSharedService(String serviceName){
        ProvisionedService provisionedService = sharedServices.get(serviceName);
        if(provisionedService == null){
            ServiceDescription sd = getSharedServiceDescription(serviceName);
            Plugin plugin = sd.getPlugin();
            ServiceInfo serviceInfo = serviceUtil.getServiceInfo(serviceName, null, null);
            provisionedService = plugin.getProvisionedService(sd, serviceInfo);
            sharedServices.put(serviceName, provisionedService);
        }
        if(provisionedService == null){
            throw new RuntimeException("No such shared service ["+serviceName+"] is available");
        }
        return provisionedService;
    }

    public ServiceDescription getExternalServiceDescription(String serviceName){
        ServiceDescription sd = null;
        ServiceInfo serviceInfo = serviceUtil.getServiceInfo(serviceName, null, null);

        if(serviceInfo != null){
            sd = serviceUtil.getExternalServiceDescription(serviceInfo);
            if(sd != null){
                sd.setServiceScope(ServiceScope.EXTERNAL);
            }else{
                throw new RuntimeException("Could not retrieve external-service-description ["+serviceName+"] ");
            }
            sd.setServiceType(serviceInfo.getServerType());
            //TODO should we associate a plugin for external service's service-description as
            //TODO external-service is not handled by a plugin ?
            Plugin plugin = getPlugin(sd);
            sd.setPlugin(plugin);
        }else{
            throw new RuntimeException("No such external service ["+serviceName+"] is available");
        }
        return sd;
    }

    public ServiceDescription getServiceDescriptionForSharedOrExternalService(String serviceName)
            throws PaaSDeploymentException {
        org.glassfish.paas.orchestrator.config.Service service = serviceUtil.getService(serviceName, null);
        ServiceDescription sd = null;
        if(service instanceof SharedService){
            sd = getSharedServiceDescription(serviceName);
        }else if(service instanceof ExternalService){
            sd = getExternalServiceDescription(serviceName);
        }
        /* it is possible that the request is for application-scoped-service's service-description
           return null instead of throwing exception.
        if(sd == null){
            throw new PaaSDeploymentException("No external/shared service ["+serviceName+"] found");
        }
        */
        return sd;
    }

    public ServiceDescription getSharedServiceDescription(String serviceName){

        ServiceDescription sd = null;
        ServiceInfo serviceInfo = serviceUtil.getServiceInfo(serviceName, null, null);
        if(serviceInfo != null){
            sd = serviceUtil.getSharedServiceDescription(serviceInfo);
            if(sd != null){
                sd.setServiceScope(ServiceScope.SHARED);
            }else{
                throw new RuntimeException("Could not retrieve shared-service-description ["+serviceName+"] ");
            }
            Plugin plugin = getPlugin(sd);
            sd.setPlugin(plugin);
            sd.setVirtualClusterName(sd.getName()); //TODO need to generate unique virtual-cluster-name
        }else{
            throw new RuntimeException("No such shared service ["+serviceName+"] is available");
        }
        return sd;
    }

    public Plugin getPlugin(ServiceDescription sd){
                        Collection<Plugin> plugins = new LinkedHashSet<Plugin>();
            for(Plugin plugin : getPlugins()){
                if( plugin.handles(sd) ){
                    plugins.add(plugin);
                }

                if(sd.getServiceType().equalsIgnoreCase(plugin.getServiceType().toString())){
                    plugins.add(plugin);
                }
            }

            Plugin matchingPlugin = null;
            if(plugins.size() > 1){
                matchingPlugin = getDefaultPlugin(plugins, sd.getServiceType());
                if(matchingPlugin == null){
                    throw new RuntimeException("Unable to resolve conflict among multiple service-provisioning engines " +
                            "that can handle service-type ["+sd.getServiceType()+"]" +
                            " specified in the service-description ["+sd+"]");
                }
            }else if(plugins.size()  == 1){
                matchingPlugin = plugins.iterator().next();
            }else{
                throw new RuntimeException("No service-provisioning-engine can handle service-type ["+sd.getServiceType()+"]" +
                        " specified in service-description ["+sd+"]");
            }
        return matchingPlugin;
    }

    public ServiceMetadata removeServiceMetadata(String appName) {
        return serviceMetadata.remove(appName);
    }

    public Set<ProvisionedService> removeProvisionedServices(String appName) {
        return provisionedServices.remove(appName);
    }

    public void preDeploy(String appName, ExtendedDeploymentContext context) {
        provisionServicesForApplication(appName, context);
    }
}
