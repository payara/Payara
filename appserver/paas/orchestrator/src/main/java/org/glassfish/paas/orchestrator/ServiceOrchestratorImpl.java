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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceDefinition;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

@Service
public class ServiceOrchestratorImpl implements ServiceOrchestrator {

    @Inject
    protected Habitat habitat;

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    public Set<Plugin> getPlugins() {
        Set<Plugin> plugins = new HashSet<Plugin>();
        plugins.addAll(habitat.getAllByContract(Plugin.class));
        logger.log(Level.INFO, "Discovered plugins:" + plugins);
        return plugins;
    }

    public void deployApplication(String appName, ReadableArchive cloudArchive) {
        logger.entering(getClass().getName(), "deployApplication");
        //Get all plugins installed in this runtime
        Set<Plugin> installedPlugins = getPlugins();

        //1. Perform service dependency discovery
        ServiceMetadata appServiceMetadata = serviceDependencyDiscovery(appName, cloudArchive, installedPlugins);

        //2. Provision dependent services
        Set<ProvisionedService> appProvisionedSvcs = provisionServices(installedPlugins, appServiceMetadata);

        //3. Associate provisioned services with each other
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, true /*before deployment*/);

        //4a. Application Deployment
        deployArchive(cloudArchive, installedPlugins);

        //4b. post-deployment association
        associateProvisionedServices(installedPlugins, appServiceMetadata,
                appProvisionedSvcs, false /*before deployment*/);
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

        logger.log(Level.INFO, "Discovered declared service metadata via glassfish-services.xml = " + appServiceMetadata);

        //1.2 Get implicit service-definitions (for instance a war is deployed, and it has not
        //specified a javaee service-definition in its orchestration.xml, the PaaS runtime
        //through the GlassFish plugin that a default javaee service-definition
        //is implied
        for (Plugin svcPlugin : installedPlugins) {
            if (svcPlugin.handles(cloudArchive)) {
                //If a ServiceDefinition has not been declared explicitly in
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

    private void associateProvisionedServices(Set<Plugin> installedPlugins,
                                              ServiceMetadata appServiceMetadata,
                                              Set<ProvisionedService> appProvisionedSvcs, boolean preDeployment) {
        logger.entering(getClass().getName(), "associateProvisionedServices-beforeDeployment=" + preDeployment);
        for (ProvisionedService ps : appProvisionedSvcs) {
            for (Plugin<?> svcPlugin : installedPlugins) {
                //associate the provisioned service only with plugins that handle other service types.
                if (!ps.getServiceType().equals(svcPlugin.getServiceType())) {
                    Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                    for (ServiceReference sr : appSRs) {
                        logger.log(Level.INFO, "Associating ProvisionedService " + ps + " for ServiceReference " + sr + " through " + svcPlugin);
                        svcPlugin.associateServices(ps, sr, preDeployment);
                    }
                }
            }
        }
    }

    private Set<ProvisionedService> provisionServices(Set<Plugin> installedPlugins,
                                                      ServiceMetadata appServiceMetadata) {
        logger.entering(getClass().getName(), "provisionServices");
        Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();

        Set<ServiceDescription> appSDs = appServiceMetadata.getServiceDescriptions();
        for (ServiceDescription sd : appSDs) {
            Plugin<?> chosenPlugin = getPluginForServiceType(installedPlugins, sd.getServiceType());
            logger.log(Level.INFO, "Provisioning Service for " + sd + " through " + chosenPlugin);
            ProvisionedService ps = chosenPlugin.provisionService(sd);
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

}
