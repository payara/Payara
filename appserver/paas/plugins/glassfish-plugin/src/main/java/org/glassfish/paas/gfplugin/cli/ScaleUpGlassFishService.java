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

package org.glassfish.paas.gfplugin.cli;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.paas.gfplugin.GlassFishProvisionedService;
import org.glassfish.paas.gfplugin.GlassFishProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.AllocationConstraints;
import org.glassfish.virtualization.spi.AllocationPhase;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.glassfish.virtualization.spi.IAAS;
import org.glassfish.virtualization.spi.PhasedFuture;
import org.glassfish.virtualization.spi.SearchCriteria;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.KeyValueType;
import org.glassfish.virtualization.util.ServiceType;
import org.glassfish.virtualization.util.SimpleSearchCriteria;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
@Service
public class ScaleUpGlassFishService {

    @Inject(optional = true)
    private TemplateRepository templateRepository;

    // TODO :: remove dependency on VirtualCluster(s).
    @Inject(optional = true)  // made it optional for non-virtual scenario to work
    private VirtualClusters virtualClusters;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private IAAS iaas;

    // TODO :: Remove dependency on utility classes.
    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private ProvisionerUtil provisionerUtil;

    public static Logger logger = Logger.getLogger(ScaleUpGlassFishService.class.
            getPackage().getName());

    public ProvisionedService scaleUp(int scaleCount,
                                      ServiceDescription serviceDescription,
                                      AllocationStrategy allocationStrategy,
                                      StringBuilder errorMessages) {
        if (scaleCount <= 0) {
            errorMessages.append("\nUnable to scale up the service. Invalid scale count [" +
                    scaleCount + "]");
            return null;
        }
        ProvisionedService provisionedService = null;
        TemplateIdentifier ti = serviceDescription.getTemplateIdentifier();
        ServiceCharacteristics sc =serviceDescription.getServiceCharacteristics();
        List<Property> serviceCharacteristics = sc != null ? sc.getServiceCharacteristics() : null;
        String templateId = ti != null ? ti.getId() : null;

        if (serviceCharacteristics != null && templateRepository != null) {
            // find the right template for the service characterstics specified.
            SearchCriteria searchCriteria = new SimpleSearchCriteria();
            searchCriteria.and(new ServiceType(getProperty(serviceCharacteristics, "service-type")));
            for (Property serviceCharacteristic : serviceCharacteristics) {
                if ("service-type".equalsIgnoreCase(serviceCharacteristic.getName())) continue;
                searchCriteria.and(new KeyValueType(serviceCharacteristic.getName(),
                        serviceCharacteristic.getValue()));
            }
            Collection<TemplateInstance> matchingTemplates =
                    templateRepository.get(searchCriteria);
            if (!matchingTemplates.isEmpty()) {
                // TODO :: for now let us pick the first matching templates
                TemplateInstance matchingTemplate = matchingTemplates.iterator().next();
                if (matchingTemplates.size() > 0) {
                    errorMessages.append("\nMultiple matching templates found [" + matchingTemplates +
                            "]. Used the first one [" + matchingTemplate + "]");
                }
                templateId = matchingTemplate.getConfig().getName();
            } else {
                errorMessages.append("\nUnable to find any template matching " +
                        "service characteristics. [" + serviceCharacteristics + "]");
            }
        }
        provisionedService = scaleUp(scaleCount, serviceDescription, templateId,
                allocationStrategy, errorMessages);
        return provisionedService;
    }

    private ProvisionedService scaleUp(int scaleCount,
                                       ServiceDescription serviceDescription,
                                       String templateId,
                                       AllocationStrategy allocationStrategy,
                                       StringBuilder errorMessages) {
        ProvisionedService provisionedService = null;
        if (scaleCount <= 0) {
            errorMessages.append("\nUnable to scale up the service. " +
                    "Invalid scale count [" + scaleCount + "]");
            return provisionedService;
        }
        // TODO :: check for max.clustersize
        try {
            VirtualCluster vCluster = virtualClusters.byName(serviceDescription.getName());
            TemplateInstance template = templateRepository.byName(templateId);
            List<PhasedFuture<AllocationPhase, VirtualMachine>> futures =
                    new ArrayList<PhasedFuture<AllocationPhase, VirtualMachine>>();
            for (int i = 0; i < scaleCount; i++) {
                AllocationConstraints allocationConstraints =
                        new AllocationConstraints(template, vCluster);
                PhasedFuture<AllocationPhase, VirtualMachine> future =
                        allocationStrategy != null ?
                                iaas.allocate(allocationStrategy, allocationConstraints, null) :
                                iaas.allocate(allocationConstraints, null);
                futures.add(future);
            }
            
            for (PhasedFuture<AllocationPhase, VirtualMachine> future : futures) {
                VirtualMachine vm = future.get();

                // add app-scoped-service config for newly added node.
                ServiceInfo entry = new ServiceInfo();
                entry = new ServiceInfo();
                entry.setServiceName(serviceDescription.getName() + "." + vm.getName());
                entry.setServerType(ServiceInfo.Type.ClusterInstance.toString());
                entry.setIpAddress(vm.getAddress());
                entry.setInstanceId(vm.getName());
                entry.setState(ServiceInfo.State.Running.toString());
                entry.setAppName(serviceDescription.getAppName());
                serviceUtil.registerCloudEntry(entry);
            }

            String dasIPAddress = "localhost"; // TODO :: change it when DAS is also provisioned separately.
            Properties serviceProperties = new Properties();
            serviceProperties.setProperty("host", dasIPAddress);
            GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);
            GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
            provisionedService = new GlassFishProvisionedService(
                    serviceDescription, serviceProperties, provisionedGlassFish);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            errorMessages.append("\n" + ex.getLocalizedMessage());
        }
        return provisionedService;
    }

    // utility method.
    private String getProperty(List<Property> properties, String name) {
        for (Property p : properties) {
            if (p.getName().equals(name)) {
                return p.getValue();
            }
        }
        return null;
    }

}
