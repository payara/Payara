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

import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentException;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
public abstract class AssociationState extends AbstractPaaSDeploymentState {

    protected void associateProvisionedServices(PaaSDeploymentContext context, boolean preDeployment)
            throws PaaSDeploymentException {
        String state="after";
        if(preDeployment){
            state="before";
        }

        logger.log(Level.FINER, localStrings.getString("associate.provisioned.services.predeployment",state));
        String appName = context.getAppName();
        final ServiceMetadata appServiceMetadata = orchestrator.getServiceMetadata(appName);
        final Set<ServicePlugin> plugins = orchestrator.getPlugins(appServiceMetadata);
        Set<org.glassfish.paas.orchestrator.service.spi.Service> servicesForAssociation =
                orchestrator.getServicesForAssociation(appName);

        List<AssociatedServiceRecord> associatedServicesList = new ArrayList<AssociatedServiceRecord>();
        try{
            for (org.glassfish.paas.orchestrator.service.spi.Service serviceProducer : servicesForAssociation) {
                for (ServicePlugin<?> svcPlugin : plugins) {
                    //associate the provisioned service only with plugins that handle other service types.
                    if (!serviceProducer.getServiceType().equals(svcPlugin.getServiceType())) {
                        Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
                        for (ServiceReference serviceRef : appSRs) {
                            if(serviceRef.getType() != null){
                                Object args[]=new Object[]{serviceProducer,serviceRef,svcPlugin};
                                logger.log(Level.INFO, "associate.provisionedservice",args);

                                Collection<org.glassfish.paas.orchestrator.service.spi.Service> serviceConsumers =
                                        orchestrator.getServicesManagedByPlugin(svcPlugin, servicesForAssociation);
                                for (org.glassfish.paas.orchestrator.service.spi.Service serviceConsumer : serviceConsumers) {
                                    try {
                                        svcPlugin.associateServices(serviceConsumer, serviceRef, serviceProducer, preDeployment, context);
                                        associatedServicesList.add(
                                                new AssociatedServiceRecord(serviceProducer, serviceConsumer, serviceRef, svcPlugin));
                                    } catch (Exception e) {
                                        Object args1[]= new Object[]{serviceConsumer.getName(),serviceProducer.getName(),serviceRef,e};
                                        logger.log(Level.WARNING,"failure.while.associating.service",args1);
                                        rollback(associatedServicesList, context, preDeployment);
                                        throw new PaaSDeploymentException(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }finally{
            associatedServicesList.clear();
        }
    }

    private void rollback(List<AssociatedServiceRecord> associatedServices, PaaSDeploymentContext context,
                          boolean preDeployment) {
        if(isAtomicDeploymentEnabled()){
            for(AssociatedServiceRecord asr : associatedServices){
                org.glassfish.paas.orchestrator.service.spi.Service serviceProvider = asr.getProvider();
                org.glassfish.paas.orchestrator.service.spi.Service serviceConsumer = asr.getConsumer();
                ServiceReference serviceRef = asr.getServiceReference();
                ServicePlugin plugin = asr.getPlugin();
                try{
                    plugin.dissociateServices(serviceConsumer, serviceRef, serviceProvider, preDeployment,
                            context);
                }catch(Exception e){
                    Object args[]= new Object[]{serviceConsumer.getName(),serviceProvider.getName(),serviceRef,e};
                    logger.log(Level.WARNING,"failure.while.associating.service",args);
                }
            }
        }
    }

    class AssociatedServiceRecord {
        private org.glassfish.paas.orchestrator.service.spi.Service provider;
        private org.glassfish.paas.orchestrator.service.spi.Service consumer;
        private ServiceReference serviceRef;
        private ServicePlugin plugin;

        public AssociatedServiceRecord(org.glassfish.paas.orchestrator.service.spi.Service serviceProvider,
                                       org.glassfish.paas.orchestrator.service.spi.Service serviceConsumer,
                                       ServiceReference serviceRef, ServicePlugin plugin){
            this.provider = serviceProvider;
            this.consumer = serviceConsumer;
            this.serviceRef = serviceRef;
            this.plugin = plugin;
        }

        public org.glassfish.paas.orchestrator.service.spi.Service getProvider(){
            return provider;
        }

        public org.glassfish.paas.orchestrator.service.spi.Service getConsumer(){
            return consumer;
        }

        public ServiceReference getServiceReference(){
            return serviceRef;
        }

        public ServicePlugin getPlugin(){
            return plugin;
        }
    }
}
