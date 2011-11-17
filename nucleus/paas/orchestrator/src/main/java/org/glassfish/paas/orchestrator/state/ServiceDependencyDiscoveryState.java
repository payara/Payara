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

package org.glassfish.paas.orchestrator.state;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.*;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
@Service
public class ServiceDependencyDiscoveryState implements PaaSDeploymentState {

    @Inject
    private Habitat habitat;

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    public void handle(PaaSDeploymentContext context) {
        ServiceMetadata appServiceMetadata = serviceDependencyDiscovery(context);
        final ServiceOrchestratorImpl orchestrator = context.getOrchestrator();
        String appName = context.getAppName();
        orchestrator.addServiceMetadata(appName, appServiceMetadata);
    }

    private ServiceMetadata serviceDependencyDiscovery(PaaSDeploymentContext context) {
        logger.entering(getClass().getName(), "serviceDependencyDiscovery");

        final ServiceOrchestratorImpl orchestrator = context.getOrchestrator();
        final Set<Plugin> installedPlugins = orchestrator.getPlugins();
        final DeploymentContext dc = context.getDeploymentContext();
        String appName = context.getAppName();
        final ReadableArchive archive = dc.getSource();

        return getServiceDependencyMetadata(context, installedPlugins, appName, archive);
    }

    public ServiceMetadata getServiceDependencyMetadata(PaaSDeploymentContext context, Set<Plugin> installedPlugins,
                                                         String appName, ReadableArchive archive) {
        final ServiceOrchestratorImpl orchestrator = context.getOrchestrator();
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
                            logger.log(Level.INFO, "Implicit ServiceDescription:" + sd);
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
                        logger.log(Level.INFO, "ServiceReference:" + sr);
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

                Set<ServiceDescription> matchingSDs = new HashSet<ServiceDescription>();
                if(!serviceDescriptionExists){
                    for(ServiceDescription sd : appSDs){
                        Plugin plugin = orchestrator.getPluginForServiceType(orchestrator.getPlugins(), sd.getServiceType());
                        if(plugin != null){
                            if(plugin.isReferenceTypeSupported(sr.getServiceRefType())){
                                matchingSDs.add(sd);
                            }
                        }
                    }
                    if(matchingSDs.size() == 1){
                        //we found exactly one matching service-description.
                        serviceDescriptionExists = true;
                    }
                }

                if (!serviceDescriptionExists) {
                    //create a default SD for this service ref and add to application's
                    //service metadata
                    for (Plugin svcPlugin : installedPlugins) {
                        if (svcPlugin.isReferenceTypeSupported(svcRefType)) {
                            ServiceDescription defSD = svcPlugin.getDefaultServiceDescription(appName, sr);
                            if (existingSDs.containsKey(defSD.getName())) {
                                Plugin plugin = existingSDs.get(defSD.getName());
                                if (svcPlugin.getClass().equals(plugin.getClass()) && svcPlugin.getServiceType().equals(plugin.getServiceType())) {
                                    //service description provided by same plugin, avoid adding the service-description.
                                    continue;
                                } else {
                                    existingSDs.put(defSD.getName(), svcPlugin);
                                }
                            } else {
                                existingSDs.put(defSD.getName(), svcPlugin);
                            }
                            addServiceDescriptionWithoutDuplicate(appServiceMetadata, defSD);
                            continue; //ignore the rest of the plugins
                        }
                    }
                }
            }
            assertMetadataComplete(appSDs, appSRs);

            //set virtual-cluster name
            String virtualClusterName = orchestrator.getVirtualClusterName(appServiceMetadata);
            for(ServiceDescription sd : appServiceMetadata.getServiceDescriptions()){
                sd.setVirtualClusterName(virtualClusterName);
            }

            logger.log(Level.INFO, "Final Service Metadata = " + appServiceMetadata);
            context.setAction(PaaSDeploymentContext.Action.PROCEED);
            return appServiceMetadata;
        } catch (Exception e) {
            context.setAction(PaaSDeploymentContext.Action.ROLLBACK);
        }
        return null;
    }

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

    private boolean serviceDescriptionExistsForType(
            ServiceMetadata appServiceMetadata, ServiceType svcType) {
        for (ServiceDescription sd : appServiceMetadata.getServiceDescriptions()) {
            if (sd.getServiceType().equalsIgnoreCase(svcType.toString())) return true;
        }
        return false;
    }
}
