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
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
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

    private Map<String, ServiceMetadata> serviceMetadata = new HashMap<String, ServiceMetadata>();
    private Map<String, Set<ProvisionedService>> provisionedServices = new HashMap<String, Set<ProvisionedService>>();

    private static final Class [] PRE_DEPLOY_PHASE_STATES = {ServiceDependencyDiscoveryState.class, ProvisioningState.class,
            PreDeployAssociationState.class};
    private static final Class [] POST_DEPLOY_PHASE_STATES = {PostDeployAssociationState.class, DeploymentCompletionState.class};
    private static final Class [] PRE_UNDEPLOY_PHASE_STATES = {PreUndeployDissociationState.class};
    private static final Class [] POST_UNDEPLOY_PHASE_STATES = {PostUndeployDissociationState.class, UnprovisioningState.class};
    private static final Class [] ENABLE_PHASE_STATES = {ServiceDependencyDiscoveryState.class, EnableState.class};
    private static final Class [] DISABLE_PHASE_STATES = {DisableState.class};
    private static final Class [] SERVER_STARTUP_PHASE_STATES = {ServiceDependencyDiscoveryState.class, ServerStartupState.class};
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
        //TODO for now, returning only deployment states as
        //TODO we will have support of atomicity only during deployment.
        Set<Class> allStates = new HashSet<Class>();
        allStates.addAll(DEPLOYMENT_STATES);
        return Collections.unmodifiableSet(allStates);
    }

    public Set<Plugin> getPlugins() {
        if(pluginsSet == null){
            Set<Plugin> plugins = new HashSet<Plugin>();
            plugins.addAll(habitat.getAllByContract(Plugin.class));
            logger.log(Level.INFO, "Discovered plugins:" + plugins);
            checkForDuplicatePlugins(plugins);
            pluginsSet = plugins;
        }
        return pluginsSet;
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

    public Set<ProvisionedService> getProvisionedServices(String appName) {
        return provisionedServices.get(appName);
    }

    public ServiceMetadata getServiceMetadata(String appName) {
        return serviceMetadata.get(appName);
    }

    private void orchestrateTask(Class[] tasks, String appName, DeploymentContext dc, boolean deployment) {
        for(Class clz : tasks){
            PaaSDeploymentState state = habitat.getByType(clz.getName());
            PaaSDeploymentContext pc = new PaaSDeploymentContext(appName, dc, this);
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

    private void handleFailure(String appName, Class[] tasks, boolean deployment, PaaSDeploymentState state, PaaSDeploymentContext pc,
                               Exception e) {
        logger.log(Level.WARNING, "Failure while handling [ " + state.getClass().getSimpleName() + " ] : ", e);
        if(deployment){
            rollbackDeployment(pc, state, DEPLOYMENT_STATES);
            throw new DeploymentException("Failure while deploying application [ "+appName+" ], rolled back all operations. Refer root cause", e);
        }else{
            throw new DeploymentException("Failure while undeploying application [ "+appName+" ]. Refer root cause", e);
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

    public ServiceMetadata getServices(ReadableArchive archive){
        ServiceDependencyDiscoveryState state = habitat.getByType(ServiceDependencyDiscoveryState.class);
        PaaSDeploymentContext pc = new PaaSDeploymentContext(archive.getName(), null, this);
        return state.getServiceDependencyMetadata(pc, getPlugins(), archive.getName(), archive);
    }

    public Collection<ProvisionedService> getServicesProvisionedByPlugin(Plugin plugin,
                                                                          Set<ProvisionedService> allProvisionedServices){
        List<ProvisionedService> provisionedServices = new ArrayList<ProvisionedService>();
        for(ProvisionedService ps : allProvisionedServices){
            if(ps.getServiceType().equals(plugin.getServiceType())){
                provisionedServices.add(ps);
            }
        }
        return provisionedServices;
    }

    // Name of the JavaEE service will be the name of the virtual cluster.
    public String getVirtualClusterName(ServiceMetadata appServiceMetadata) {
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
    
    public Plugin<?> getPluginForServiceType(Set<Plugin> installedPlugins, String serviceType) {
        //XXX: for now assume that there is one plugin per servicetype
        //and choose the first plugin that handles this service type.
        //in the future, need to handle conflicts
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.getServiceType().toString().equalsIgnoreCase(serviceType)) return svcPlugin;
        }
        return null;
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
             * app Name and hence a temproary workaround to find the right appName 
             */
            //Hack starts here.
            String effectiveAppName = appName;
            
            String tmpAppName = null;
            for (String app: provisionedServices.keySet()){
                logger.log(Level.INFO, "Checking app for Service " + svcName);
                Set<ProvisionedService> appsServices = provisionedServices.get(app);
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
            Set<ProvisionedService> appPS = provisionedServices.get(effectiveAppName);
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
            ServiceMetadata appServiceMetadata = serviceMetadata.get(effectiveAppName); 
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

    public void addProvisionedServices(String appName, Set<ProvisionedService> provisionedServiceSet) {
        provisionedServices.put(appName, provisionedServiceSet);
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
