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

package org.glassfish.paas.orchestrator.state;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentException;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.ServicesXMLParser;
import org.glassfish.paas.orchestrator.provisioning.ServiceScope;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.jvnet.hk2.annotations.Service;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
@Service
public class ServiceDependencyDiscoveryState extends AbstractPaaSDeploymentState {

    public void handle(PaaSDeploymentContext context) throws PaaSDeploymentException{
        try{
            ServiceMetadata appServiceMetadata = serviceDependencyDiscovery(context);
            String appName = context.getAppName();
            //registering metadata with Orchestrator must be the last operation (only if service dependency discovery
            //completes without any errors).
            appInfoRegistry.addServiceMetadata(appName, appServiceMetadata);
        }catch(Exception e){
            throw new PaaSDeploymentException(e);
        }
    }

    public Class getRollbackState() {
        return null;
    }

    private ServiceMetadata serviceDependencyDiscovery(PaaSDeploymentContext context) throws PaaSDeploymentException {
        logger.log(Level.FINER, localStrings.getString("METHOD.serviceDependencyDiscovery"));

        final DeploymentContext dc = context.getDeploymentContext();
        String appName = context.getAppName();
        final ReadableArchive archive = dc.getSource();

        return getServiceDependencyMetadata(context, appName, archive);
    }

    public ServiceMetadata getServiceDependencyMetadata(PaaSDeploymentContext context,
                                                        String appName, ReadableArchive archive)
    throws PaaSDeploymentException {

        Set<ServicePlugin> installedPlugins = orchestrator.getPlugins();
        try {
            //1. SERVICE DISCOVERY
            //parse glassfish-services.xml to get all declared SRs and SDs
            //Get the first ServicesXMLParser implementation

            ServicesXMLParser parser = habitat.getByContract(ServicesXMLParser.class);

            //1.1 discover all Service References and Descriptions already declared for this application
            ServiceMetadata appServiceMetadata = parser.discoverDeclaredServices(appName, archive);

            //if no meta-data is found, create empty ServiceMetadata
            if (appServiceMetadata == null) {
                appServiceMetadata = new ServiceMetadata();
                appServiceMetadata.setAppName(appName);
            }

            logger.log(Level.INFO, "discovered.declared.metadata", appServiceMetadata);

            Map<ServiceDescription, ServicePlugin> pluginsToHandleSDs = new LinkedHashMap<ServiceDescription, ServicePlugin>();

            Set<ServiceReference> wiredServiceReferences = new LinkedHashSet<ServiceReference>();
            //Get service-references defined in services.xml and retrieve
            //the corresponding service-description (shared/external).
            for(ServiceReference serviceReference : appServiceMetadata.getServiceReferences()){
                String serviceName = serviceReference.getServiceName();
                ServiceDescription sd = orchestrator.getServiceDescriptionForSharedOrExternalService(serviceName);
                if(sd != null){
                    appServiceMetadata.addServiceDescription(sd);
                    wiredServiceReferences.add(serviceReference);
                    pluginsToHandleSDs.put(sd, sd.getPlugin());
                }
                //if sd == null, its possible that the reference is on an application-scoped-service
                //Such reference need not be satisfied as the service-reference via the descriptor is only
                //meant for including shared/external-service in the meta-data (and hence the runtime).
                //we will ignore processing it and hence it will not be part of srToSD mapping
                //or wiredServiceReferences and will not be used for association/dissociation phases.
            }

            //make sure that each service-description is wired to appropriate plugin.
            for (ServiceDescription sd : appServiceMetadata.getServiceDescriptions()) {
                if (sd.getPlugin() == null) {
                    //Get the list of plugins that handle a particular service-description.
                    List<ServicePlugin> pluginsList = new ArrayList<ServicePlugin>();
                    for (ServicePlugin svcPlugin : installedPlugins) {
                        if (svcPlugin.handles(sd)) {
                            pluginsList.add(svcPlugin);
                        }
                    }
                    //resolve the list of plugins to one plugin.
                    if (pluginsList.size() == 1) {
                        pluginsToHandleSDs.put(sd, pluginsList.get(0));
                        sd.setPlugin(pluginsList.get(0));
                    } else if (pluginsList.size() > 1) {
                        //resolve the conflict via default plugin defined in configuration.
                        ServicePlugin defaultPlugin = null;
                        ServiceType type = pluginsList.get(0).getServiceType();
                        defaultPlugin = orchestrator.getDefaultPlugin(pluginsList, type.toString());
                        if (defaultPlugin != null) {
                            pluginsToHandleSDs.put(sd, defaultPlugin);
                            sd.setPlugin(defaultPlugin);
                        } else {
                            throw new PaaSDeploymentException("Unable to resolve conflict between multiple " +
                                    "service-provisioning-engines that handle service-description [" + sd.getName() + "]");
                        }
                    }
                }
            }


            //determine the list of plugins that handle the archive.
            Map<ServiceType, List<ServicePlugin>> matchingPlugins = new HashMap<ServiceType, List<ServicePlugin>>();
            for (ServicePlugin svcPlugin : installedPlugins) {
                if (svcPlugin.handles(archive)) {
                    List<ServicePlugin> plugins = matchingPlugins.get(svcPlugin.getServiceType());
                    if (plugins == null) {
                        plugins = new ArrayList<ServicePlugin>();
                        matchingPlugins.put(svcPlugin.getServiceType(), plugins);
                    }
                    plugins.add(svcPlugin);
                }
            }

            //resolve the list to one plugin per service-type
            List<ServicePlugin> resolvedPluginsList = new ArrayList<ServicePlugin>();
            //check for duplicate plugins and resolve them.
            for (ServiceType type : matchingPlugins.keySet()) {
                List<ServicePlugin> plugins = matchingPlugins.get(type);
                if (plugins.size() > 1) {
                    ServicePlugin plugin = orchestrator.getDefaultPlugin(plugins, type.toString());
                    if (plugin != null) {
                        resolvedPluginsList.add(plugin);
                    } else {
                        throw new PaaSDeploymentException("Unable to resolve conflict between multiple " +
                                "service-provisioning-engines of type [" + type + "] that handle the archive");
                    }
                }else if(plugins.size() == 1){
                    resolvedPluginsList.add(plugins.get(0));
                }
            }

            //1.2 Get implicit service-descriptions (for instance a war is deployed, and it has not
            //specified a javaee service-description in its orchestration.xml, the PaaS runtime
            //through the GlassFish plugin that a default javaee service-description
            //is implied
            for (ServicePlugin svcPlugin : resolvedPluginsList) {
                //if (svcPlugin.handles(archive)) {
                //If a ServiceDescription has not been declared explicitly in
                //the application for the plugin's type, ask the plugin (since it
                //supports this type of archive) if it has any implicit
                //service-description for this application
                if (!serviceDescriptionExistsForType(appServiceMetadata, svcPlugin.getServiceType())) {
                    Set<ServiceDescription> implicitServiceDescs = svcPlugin.getImplicitServiceDescriptions(archive, appName);

                    for (ServiceDescription sd : implicitServiceDescs) {
                        logger.log(Level.FINEST, localStrings.getString("implicit.SD",sd));
                        pluginsToHandleSDs.put(sd, svcPlugin);
                        sd.setPlugin(svcPlugin);
                        sd.setServiceScope(ServiceScope.APPLICATION);
                        appServiceMetadata.addServiceDescription(sd);
                    }
                }
                //}
            }

            setPluginForSD(orchestrator, pluginsToHandleSDs, installedPlugins, appServiceMetadata);

            logger.log(Level.FINEST, "after.implicit.SD", appServiceMetadata);

            Map<ServiceReference, ServiceDescription> serviceRefToSD = new HashMap<ServiceReference, ServiceDescription>();

            //1.2 Get implicit ServiceReferences
            for (ServicePlugin svcPlugin : resolvedPluginsList) {
                Set<ServiceReference> implicitServiceRefs = svcPlugin.getServiceReferences(appName, archive, context);
                for (ServiceReference sr : implicitServiceRefs) {
                    sr.setRequestingPlugin(svcPlugin);
                    logger.log(Level.FINEST, localStrings.getString("serviceReference",sr));
                    appServiceMetadata.addServiceReference(sr);
                    //if the service-ref refers a service-name, retrieve the service-description of the service
                    //and add the shared/external service-description to meta-data.
                    String serviceName = sr.getServiceName();
                    if(serviceName != null){
                        ServiceDescription sd = orchestrator.getServiceDescriptionForSharedOrExternalService(serviceName);
                        if(sd != null){
                            appServiceMetadata.addServiceDescription(sd);
                            pluginsToHandleSDs.put(sd, sd.getPlugin());
                            wiredServiceReferences.add(sr);
                            serviceRefToSD.put(sr, sd);
                        }

                        //it's possible that the reference here is an application-scoped-service.
                        if(sd == null){
                            for(ServiceDescription serviceDescription : appServiceMetadata.getServiceDescriptions()){
                                if(serviceDescription.getName().equals(serviceName)){
                                    sd = serviceDescription;
                                    break;
                                }
                            }
                            if(sd != null){
                                wiredServiceReferences.add(sr);
                                serviceRefToSD.put(sr, sd);
                            }else{
                                throw new PaaSDeploymentException("unable to find the service ["+sr.getServiceName()+"] " +
                                        "for service-reference [ "+sr+" ]");
                            }
                        }
                    }
                }
            }
            logger.log(Level.FINEST, localStrings.getString("after.serviceref ",appServiceMetadata));


            //1.3 Ensure all service references have a related service description
            Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
            Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
            for (ServiceReference sr : appSRs) {
                if (!wiredServiceReferences.contains(sr)) {
                    String targetSD = sr.getServiceName();
                    boolean serviceDescriptionExists = false;
                    for (ServiceDescription sd : appSDs) {
                        //scan through available service-descriptions to see whether the referred
                        //service-description matches the service-name
                        //specified by service-reference.
                        if (sd.getName().equals(targetSD)) {
                            serviceDescriptionExists = true;
                            break;
                        }
                    }

                    //no matching service-description found. Lets try a plugin (default plugin if
                    //multiple plugins support same service-type) and get service-description.
                    if (!serviceDescriptionExists) {
                        //a plugin has asked for specific service which is not found.
                        //fail deployment.
                        if(sr.getServiceName() != null){
                            throw new PaaSDeploymentException("unable to find the service ["+sr.getServiceName()+"] " +
                                    "for service-reference [ "+sr+" ]");
                        }

                        List<ServicePlugin> pluginsList = new ArrayList<ServicePlugin>();
                        for (ServicePlugin plugin : installedPlugins) {
                            if (plugin.isReferenceTypeSupported(sr.getType())) {
                                pluginsList.add(plugin);
                            }
                        }
                        ServicePlugin matchingPlugin = null;
                        if (pluginsList.size() == 1) {
                            matchingPlugin = pluginsList.get(0);
                        } else if (pluginsList.size() == 0) {
                            throw new PaaSDeploymentException("No service-provisioning-engine available to handle service-ref [ " + sr + " ]");
                        } else {
                            matchingPlugin = orchestrator.getDefaultPluginForServiceRef(sr.getType());
                        }

                        if (matchingPlugin == null) {
                            //we could not find a matching plugin as there is no default plugin.
                            //get a plugin that handles this service-ref
                            Collection<ServicePlugin> plugins = pluginsToHandleSDs.values();
                            for (ServicePlugin plugin : plugins) {
                                if (plugin.isReferenceTypeSupported(sr.getType())) {
                                    matchingPlugin = plugin;
                                    break;
                                }
                            }
                        }


                        ServiceDescription matchingSDForServiceRef = null;

                        if (pluginsToHandleSDs.values().contains(matchingPlugin)) {
                            //get an existing SD for the plugin in question.
                            for (Map.Entry<ServiceDescription, ServicePlugin> entry : pluginsToHandleSDs.entrySet()) {
                                if (entry.getValue().equals(matchingPlugin)) {
                                    matchingSDForServiceRef = entry.getKey();
                                    break;
                                }
                            }
                        } else {
                            //get the default SD for the plugin.
                            matchingSDForServiceRef = matchingPlugin.getDefaultServiceDescription(appName, sr);
                            matchingSDForServiceRef.setServiceScope(ServiceScope.APPLICATION);
                            appServiceMetadata.addServiceDescription(matchingSDForServiceRef);
                            pluginsToHandleSDs.put(matchingSDForServiceRef, matchingPlugin);
                        }
                        serviceRefToSD.put(sr, matchingSDForServiceRef);
                        sr.setServiceName(matchingSDForServiceRef.getName());
                    }
                }
            }
            setPluginForSD(orchestrator, pluginsToHandleSDs, installedPlugins, appServiceMetadata);

            assertMetadataComplete(appSDs, appSRs);

            //set virtual-cluster name
            String virtualClusterName = orchestrator.getVirtualClusterForApplication(appName, appServiceMetadata);
            for (ServiceDescription sd : appServiceMetadata.getServiceDescriptions()) {
                if(ServiceScope.APPLICATION.equals(sd.getServiceScope())){
                    sd.setVirtualClusterName(virtualClusterName);
                }
            }
            appInfoRegistry.getPluginsToHandleSDs(appName).putAll(pluginsToHandleSDs);
            appInfoRegistry.getSRToSDMap(appName).putAll(serviceRefToSD);

            logger.log(Level.INFO, "final.servicemetadata",appServiceMetadata);
            return appServiceMetadata;
        } catch (Exception e) {
            throw new PaaSDeploymentException(e);
        }
    }

    private void setPluginForSD(ServiceOrchestratorImpl orchestrator, Map<ServiceDescription, ServicePlugin> pluginsToHandleSDs,
                                Set<ServicePlugin> installedPlugins, ServiceMetadata appServiceMetadata) throws PaaSDeploymentException {
        //make sure that each service-description has a plugin.
        for (ServiceDescription sd : appServiceMetadata.getServiceDescriptions()) {
            if (sd.getPlugin() == null) {
                List<ServicePlugin> matchingPluginsForSDs = new ArrayList<ServicePlugin>();
                for (ServicePlugin plugin : installedPlugins) {
                    if (plugin.getServiceType().toString().equals(sd.getServiceType())) {
                        matchingPluginsForSDs.add(plugin);
                    }
                }

                if (matchingPluginsForSDs.size() == 1) {
                    sd.setPlugin(matchingPluginsForSDs.get(0));
                    pluginsToHandleSDs.put(sd, matchingPluginsForSDs.get(0));
                } else if (matchingPluginsForSDs.size() == 0) {
                    throw new PaaSDeploymentException("Unable to find a service-provisioning-engine that handles" +
                            "service-description [" + sd.getName() + "] of type [" + sd.getServiceType() + "]");
                } else {
                    ServicePlugin plugin = orchestrator.getDefaultPlugin(matchingPluginsForSDs, sd.getServiceType());
                    if (plugin != null) {
                        sd.setPlugin(plugin);
                        pluginsToHandleSDs.put(sd, plugin);
                    } else {
                        throw new PaaSDeploymentException("Unable to resolve conflict among multiple service-provisioning-engines that handle" +
                                "service-description [" + sd.getName() + "] of type [" + sd.getServiceType() + "]");
                    }
                }
            }
        }
    }

/*
    private void addServiceDescriptionWithoutDuplicate(ServiceMetadata appServiceMetadata, ServiceDescription defSD) {
        Set<ServiceDescription> serviceDescriptions = appServiceMetadata.getServiceDescriptions();
        for (ServiceDescription sd : serviceDescriptions) {
            if (sd.getName().equals(defSD.getName())) {
                if (sd.getServiceType().equals(defSD.getServiceType())) {
                    return; //duplicate. We may also have to check whether its provided by same plugin
                    //or implement equals in service-description so as to make it easier for comparisons.
                }
            }
        }
        appServiceMetadata.addServiceDescription(defSD);
    }
*/

    private void assertMetadataComplete(Set<ServiceDescription> appSDs,
                                        Set<ServiceReference> appSRs) {
        //Assert that all SRs have their corresponding SDs
        for (ServiceReference sr : appSRs) {
            String targetSD = sr.getServiceName();
            boolean serviceDescriptionExists = false;
            for (ServiceDescription sd : appSDs) {
                if (sd.getName().equals(targetSD)) {
                    serviceDescriptionExists = true;
                }
            }
            assert serviceDescriptionExists;
        }
    }

    private boolean serviceDescriptionExistsForType(
            ServiceMetadata appServiceMetadata, ServiceType svcType) {
        for (ServiceDescription sd : appServiceMetadata.getServiceDescriptions()) {
            if (sd.getServiceType().equalsIgnoreCase(svcType.toString())) return true;
        }
        return false;
    }
}
