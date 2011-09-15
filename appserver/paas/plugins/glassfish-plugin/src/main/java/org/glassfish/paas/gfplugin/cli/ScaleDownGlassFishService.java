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
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.glassfish.virtualization.spi.IAAS;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
@Service
public class ScaleDownGlassFishService {

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

    @Inject(optional=true)
    VirtualMachineLifecycle vmLifecycle;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;

    public static Logger logger = Logger.getLogger(ScaleUpGlassFishService.class.
            getPackage().getName());

    public ProvisionedService scaleDown(int scaleCount,
                                        ServiceDescription serviceDescription,
                                        StringBuilder errorMessages) {
        if (scaleCount <= 0) {
            errorMessages.append("\nUnable to scale down the service. " +
                    "Invalid scale count [-" + scaleCount + "]");
            return null;
        }
        // TODO :: check for min.clustersize
        String serviceName = serviceDescription.getName();
        String appName = serviceDescription.getAppName();
        if (serviceName != null) {
            try {
                VirtualCluster virtualCluster = virtualClusters.byName(serviceName);
                Collection<String> instances =
                        gfServiceUtil.getAllSubComponents(serviceName, appName);
                int count = 0;
                for (String instance : instances) {
                    if (count++ < scaleCount) {
                        String vmId = gfServiceUtil.getInstanceID(
                                instance, appName, ServiceType.APPLICATION_SERVER);
                        VirtualMachine vm = virtualCluster.vmByName(vmId);
                        vmLifecycle.delete(vm); // TODO :: use executor service.
                        serviceUtil.unregisterCloudEntry(instance, appName);
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                errorMessages.append("\n" + ex.getLocalizedMessage());
            }
        }

        // TODO :: Return the provisioned service with the new cluster shape.
        String dasIPAddress = "localhost"; // TODO :: change it when DAS is also provisioned separately.
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty("host", dasIPAddress);
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        return new GlassFishProvisionedService(
                serviceDescription, serviceProperties, provisionedGlassFish);
    }

}
