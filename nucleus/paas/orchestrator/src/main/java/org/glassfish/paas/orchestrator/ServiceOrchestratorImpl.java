/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
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

    @Inject
    private PaaSAppInfoRegistry appRegistry;

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

    private static Logger logger = LogDomains.getLogger(ServiceOrchestratorImpl.class,LogDomains.PAAS_LOGGER);

    private Set<ServicePlugin> pluginsSet = null;

    public static final String ORCHESTRATOR_UNDEPLOY_CALL = "orchestrator.undeploy.call";
    public static final String PARALLEL_PROVISIONING_FLAG = "org.glassfish.paas.orchestrator.parallel-provisioning";
    public static final String ATOMIC_DEPLOYMENT_FLAG = "org.glassfish.paas.orchestrator.atomic-deployment";

    public static boolean parallelProvisioningEnabled ;
    public static boolean atomicDeploymentEnabled ;

    private static StringManager localStrings = StringManager.getManager(ServiceOrchestratorImpl.class);

    static{
        composeDeploymentStates();
        detectParallalProvisioningSetting();
        detectAtomicDeploymentSetting();
    }

    private static void detectAtomicDeploymentSetting() {
        atomicDeploymentEnabled = Boolean.valueOf(System.getProperty(ServiceOrchestratorImpl.ATOMIC_DEPLOYMENT_FLAG, "true"));
    }

    private static void detectParallalProvisioningSetting() {
        parallelProvisioningEnabled = Boolean.valueOf(System.getProperty(ServiceOrchestratorImpl.PARALLEL_PROVISIONING_FLAG, "true"));
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

    /**
     * get plugins list for association or dissociation or re-association
     * @param appServiceMetadata ServiceMetadata
     * @return Set<Plugin> Set of plugins.
     */
    public Set<ServicePlugin> getPlugins(ServiceMetadata appServiceMetadata) {
        Set<ServicePlugin> plugins = new LinkedHashSet<ServicePlugin>();
        for(ServiceDescription sd : appServiceMetadata.getServiceDescriptions()){
            plugins.add(sd.getPlugin());
        }
        for(ServiceReference sr : appServiceMetadata.getServiceReferences()){
            if(sr.getRequestingPlugin() != null){
                plugins.add(sr.getRequestingPlugin());
            }
        }
        return plugins;
    }

    public Set<ServicePlugin> getPlugins() {
        if(pluginsSet == null){
            Set<ServicePlugin> plugins = new LinkedHashSet<ServicePlugin>();
            plugins.addAll(habitat.getAllByContract(ServicePlugin.class));
            logger.log(Level.INFO,"discovered.plugins",plugins);
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
            servicesSet.addAll(appRegistry.getProvisionedServices(appName));
            servicesSet.addAll(appRegistry.getConfiguredServices(appName));
        return servicesSet;
    }

    public Set<org.glassfish.paas.orchestrator.service.spi.Service> getServicesForDissociation(String appName){
        return getServicesForAssociation(appName);
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
        Object[] args=new Object[]{state.getClass().getSimpleName(),e};
        logger.log(Level.WARNING, "failure.handling",args);
        if(deployment){
            DeploymentException de = null;
            if(isAtomicDeploymentEnabled()){
                rollbackDeployment(pc, state, DEPLOYMENT_STATES);
                de = new DeploymentException("Failure while deploying application [ "+appName+" ], " +
                        "rolled back all deploy operations.");
            }else{
                de = new DeploymentException("Failure while deploying application [ "+appName+" ]. Atomic" +
                        "deployment is disabled, manual cleanup is required");
            }
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
            logger.log(Level.WARNING, "no.such.task.to.initiate.rollback",failedState.getClass());
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
                    Object args[]=new Object[] {context.getAppName(),rollbackState.getClass().getSimpleName(),e};
                    logger.log(Level.WARNING,"failure.while.rollback",args);
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
        logger.log(Level.FINER, localStrings.getString("METHOD.provisionServicesForApplication"));
        orchestrateTask(PRE_DEPLOY_PHASE_STATES, appName, dc, true);
        logger.log(Level.FINER, localStrings.getString("METHOD.provisionServicesForApplication"));
    }

    public void postDeploy(String appName, DeploymentContext dc) {
        logger.log(Level.FINER, localStrings.getString("METHOD.postDeploy"));
        orchestrateTask(POST_DEPLOY_PHASE_STATES, appName, dc, true);
        logger.log(Level.FINER, localStrings.getString("METHOD.postDeploy"));
    }

    public void startup(String appName, DeploymentContext dc) {
        logger.log(Level.FINER, localStrings.getString("METHOD.startup"));
        orchestrateTask(SERVER_STARTUP_PHASE_STATES, appName, dc, false);
        logger.log(Level.FINER, localStrings.getString("METHOD.startup"));
    }

    public void enable(String appName, DeploymentContext dc) {
        logger.log(Level.FINER, localStrings.getString("METHOD.enable"));
        orchestrateTask(ENABLE_PHASE_STATES, appName, dc, false);
        logger.log(Level.FINER, localStrings.getString("METHOD.enable"));
    }

    public void disable(String appName, ExtendedDeploymentContext dc) {
        logger.log(Level.FINER, localStrings.getString("METHOD.disable"));
        orchestrateTask(DISABLE_PHASE_STATES, appName, dc, false);
        logger.log(Level.FINER, localStrings.getString("METHOD.disable"));
    }

    public void preUndeploy(String appName, DeploymentContext dc) {
        logger.log(Level.FINER, localStrings.getString("METHOD.preUndeploy"));
        if(!isOrchestratorInitiatedUndeploy(dc.getCommandParameters(OpsParams.class))){
            orchestrateTask(PRE_UNDEPLOY_PHASE_STATES, appName, dc, false);
        }
        logger.log(Level.FINER, localStrings.getString("METHOD.preUndeploy"));
    }

    public void postUndeploy(String appName, DeploymentContext dc) {
        logger.log(Level.FINER, localStrings.getString("METHOD.postUndeploy"));
        if(!isOrchestratorInitiatedUndeploy(dc.getCommandParameters(OpsParams.class))){
            orchestrateTask(POST_UNDEPLOY_PHASE_STATES, appName, dc, false);
        }
        logger.log(Level.FINER, localStrings.getString("METHOD.postUndeploy"));
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

    public Collection<org.glassfish.paas.orchestrator.service.spi.Service> getServicesManagedByPlugin(ServicePlugin plugin,
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

    public Collection<ProvisionedService> getServicesProvisionedByPlugin(ServicePlugin plugin,
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

    public Collection<ProvisionedService> getServicesToUnprovision(String appName){
        Set<ProvisionedService> provisionedServices = appRegistry.getProvisionedServices(appName);
        Set<ProvisionedService> servicesToUnprovision = new LinkedHashSet<ProvisionedService>();
        for(ProvisionedService ps : provisionedServices){
            if(ServiceScope.APPLICATION.equals(ps.getServiceDescription().getServiceScope())){
                servicesToUnprovision.add(ps);
            }
        }
        return servicesToUnprovision;
    }

    public ProvisionedService getProvisionedService(ServiceDescription sd, String appName){
        ProvisionedService provisionedService = null;
        Set<ProvisionedService> provisionedServices = appRegistry.getProvisionedServices(appName);
        for(ProvisionedService ps : provisionedServices){
            if(sd.getName().equals(ps.getName())){
                provisionedService = ps;
                break;
            }
        }
        return provisionedService;
    }
    public Collection<ServiceDescription> getServiceDescriptionsToProvision(String appName){
        ServiceMetadata appServiceMetadata = appRegistry.getServiceMetadata(appName);
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
            Object args[]=new Object[]{ex.getLocalizedMessage(),ex};
            logger.log(Level.WARNING,"exception.while.remove.cluster",args);
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
        Object args[]=new Object[]{virtualClusterName,commandResult.getOutput()};
        logger.log(Level.INFO,"delete.cluster.exec.output",args);
        Throwable failureCause = commandResult.getFailureCause();
        if (failureCause != null) {
            args[0]= failureCause.getLocalizedMessage();
            args[1]=failureCause;
            logger.log(Level.WARNING,"failure.cause",args);

        }
    }

    public ServicePlugin getDefaultPluginForServiceRef(String serviceRefType) {
        ServicePlugin defaultPlugin = null;

        List<ServicePlugin> matchingPlugin = new ArrayList<ServicePlugin>();
        for (ServicePlugin plugin : getPlugins()) {
            if (plugin.isReferenceTypeSupported(serviceRefType)) {
                matchingPlugin.add(plugin);
            }
        }
        //TODO we are assuming that no two different plugin types will support same service-ref type
        for (ServicePlugin plugin : matchingPlugin) {
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

    public ServicePlugin getDefaultPlugin(Collection<ServicePlugin> pluginsList, String type) {
        ServicePlugin defaultPlugin = null;
        if(pluginsList != null){
            ServiceProvisioningEngines spes = habitat.getComponent(ServiceProvisioningEngines.class);
            if(spes != null){
                for(ServiceProvisioningEngine spe : spes.getServiceProvisioningEngines()){
                    if(spe.getType().equalsIgnoreCase(type) && spe.getDefault()){
                        String className = spe.getClassName();
                        for(ServicePlugin plugin : pluginsList){
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

    //TODO revisit this for a shared-service as app-name is used here.
    @Override
    public boolean scaleService(final String appName, final String svcName,
            final int scaleCount, final AllocationStrategy allocStrategy) {

        Object args[] =new Object[]{svcName,appName,scaleCount};
        logger.log(Level.INFO, "scale.services",args);

        
        AdminCommandLock.runWithSuspendedLock(new Runnable() {
            public void run() {
            /*
             * At this point in time, the CEM passes the Service Name as the
             * app Name and hence a temporary workaround to find the right appName
             */
            //Hack starts here.
            String effectiveAppName = appName;
            
            String tmpAppName = null;
            for (String app: appRegistry.getAllProvisionedServices().keySet()){
                logger.log(Level.FINER, localStrings.getString("check.app.for.service ", svcName));
                Set<ProvisionedService> appsServices = appRegistry.getProvisionedServices(app);
                for(ProvisionedService p: appsServices){
                    if (p.getName().equals(svcName)) {
                        tmpAppName = app;
                    }
                }
            }
            if (tmpAppName != null) {
                effectiveAppName = tmpAppName; //reset
                logger.log(Level.FINER, localStrings.getString("setAppName"));
            }
            //Hack ends here.
    
            //Get Old PS
            Set<ProvisionedService> appPS = appRegistry.getProvisionedServices(effectiveAppName);
            logger.log(Level.FINER, localStrings.getString("appPS", appPS));
            ProvisionedService oldPS = null;
            for(ProvisionedService ps: appPS) {
                if (ps.getName().equals(svcName)) oldPS = ps;
            }
            logger.log(Level.FINER, localStrings.getString("oldPS",oldPS));
            
            //Find Plugin that provided this Service
            //Set<Plugin> installedPlugins = getPlugins();
/*
            Plugin<?> chosenPlugin = getPluginForServiceType(
                    installedPlugins, oldPS.getServiceDescription().getServiceType());
*/
            ServicePlugin<?> chosenPlugin = oldPS.getServiceDescription().getPlugin();
            ServiceInfo oldServiceInfo = serviceUtil.getServiceInfo(oldPS.getName(), appName, null);

            //ask it to scale the service and get new PS

            Object args[]=new Object[]{svcName,chosenPlugin};
            logger.log(Level.INFO, "scale.service.using.plugin",args);

            ProvisionedService newPS = chosenPlugin.scaleService(oldPS, scaleCount, allocStrategy);
            serviceUtil.unregisterService(oldServiceInfo);
            serviceUtil.registerService(appName, newPS);
            args[0]=svcName;
            args[1]=newPS;
            logger.log(Level.INFO, "new.provisioned.service",args);
            
            //Simple assertions to ensure that we have the scaled Service.
            assert newPS.getName().equals(oldPS.getName());
            assert newPS.getServiceType().equals(oldPS.getServiceType());
            
            //now re-associate all plugins with the new PS.
            ServiceMetadata appServiceMetadata = appRegistry.getServiceMetadata(effectiveAppName);
            Set<ServicePlugin> plugins = getPlugins(appServiceMetadata);
            for (ServicePlugin<?> svcPlugin : plugins) {
                //re-associate the new PS only with plugins that handle other service types.
                if (!newPS.getServiceType().equals(svcPlugin.getServiceType())) {
                    Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                    for (ServiceReference serviceRef : appSRs) {
                        Object args1[]=new Object[]{newPS,serviceRef,svcPlugin};
                        logger.log(Level.INFO,"reassociate.provisionedservice.serviceref",args1);

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
        ServiceMetadata appServiceMetadata = appRegistry.getServiceMetadata(appName);
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

    public void preDeploy(String appName, ExtendedDeploymentContext context) {
        provisionServicesForApplication(appName, context);
    }

    public void addSharedService(String serviceName, ProvisionedService provisionedService) {
        sharedServices.put(serviceName, provisionedService);
    }

    public void addExternalService(String serviceName, ConfiguredService configuredService){
        ServiceDescription sd = configuredService.getServiceDescription();
        if(configuredService.getServiceDescription().getPlugin() == null){
            ServicePlugin plugin = getPlugin(sd);
            sd.setPlugin(plugin);
        }
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
            //ServiceDescription sd = getExternalServiceDescription(serviceName);
            configuredService = serviceUtil.getExternalService(serviceName);
            addExternalService(serviceName, configuredService);
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
            ServicePlugin plugin = sd.getPlugin();
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
        return getConfiguredService(serviceName).getServiceDescription();
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
            ServicePlugin plugin = getPlugin(sd);
            sd.setPlugin(plugin);
            sd.setVirtualClusterName(sd.getName()); //TODO need to generate unique virtual-cluster-name
        }else{
            throw new RuntimeException("No such shared service ["+serviceName+"] is available");
        }
        return sd;
    }

    public ServicePlugin getPlugin(ServiceDescription sd){
                        Collection<ServicePlugin> plugins = new LinkedHashSet<ServicePlugin>();
            for(ServicePlugin plugin : getPlugins()){
                if( plugin.handles(sd) ){
                    plugins.add(plugin);
                }

                if(sd.getServiceType().equalsIgnoreCase(plugin.getServiceType().toString())){
                    plugins.add(plugin);
                }
            }

            ServicePlugin matchingPlugin = null;
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


    public boolean isParallelProvisioningEnabled(){
        return parallelProvisioningEnabled;
    }

    public boolean isAtomicDeploymentEnabled(){
        return atomicDeploymentEnabled;
    }
}
