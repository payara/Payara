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
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentState;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
public abstract class AssociationState implements PaaSDeploymentState {

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    protected void associateProvisionedServices(PaaSDeploymentContext context, boolean preDeployment) {
        logger.entering(getClass().getName(), "associateProvisionedServices-beforeDeployment=" + preDeployment);
        final ServiceOrchestratorImpl orchestrator = context.getOrchestrator();
        final Set<Plugin> installedPlugins = orchestrator.getPlugins();
        final DeploymentContext dc = context.getDeploymentContext();
        String appName = context.getAppName();
        final ServiceMetadata appServiceMetadata = orchestrator.getServiceMetadata(appName);
        final Set<ProvisionedService> appProvisionedSvcs = orchestrator.getProvisionedServices(appName);


        for (ProvisionedService serviceProducer : appProvisionedSvcs) {
            for (Plugin<?> svcPlugin : installedPlugins) {
                //associate the provisioned service only with plugins that handle other service types.
                if (!serviceProducer.getServiceType().equals(svcPlugin.getServiceType())) {
                    Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                    for (ServiceReference serviceRef : appSRs) {
                        logger.log(Level.INFO, "Associating ProvisionedService " + serviceProducer
                                + " for ServiceReference " + serviceRef + " through " + svcPlugin);
                        Collection<ProvisionedService> serviceConsumers =
                                orchestrator.getServicesProvisionedByPlugin(svcPlugin, appProvisionedSvcs);
                        for (ProvisionedService serviceConsumer : serviceConsumers) {
                            try {
                                svcPlugin.associateServices(serviceConsumer, serviceRef, serviceProducer, preDeployment, dc);
                            } catch (Exception e) {
                                //TODO need to dissociate the previously associated services.
                                //TODO which needs book keeping info on previously associated services.

                                context.setAction(PaaSDeploymentContext.Action.ROLLBACK);
                                return;
                            }
                        }
                    }
                }
            }
        }
        context.setAction(PaaSDeploymentContext.Action.PROCEED);
    }
}