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
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Service;
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
        final ServiceMetadata appServiceMetadata = appInfoRegistry.getServiceMetadata(appName);
        Set<Service> servicesForAssociation =
                orchestrator.getServicesForAssociation(appName);
        List<ServiceAssociationRecord> associatedServicesList = new ArrayList<ServiceAssociationRecord>();
        try{
            Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
            for(ServiceReference serviceRef : appSRs){
                
                for (Service serviceProvider : servicesForAssociation) {
                    ServiceDescription sd = serviceProvider.getServiceDescription();
                    Map<ServiceReference, ServiceDescription> srToSDMap = appInfoRegistry.getSRToSDMap(appName);
                    for(Map.Entry<ServiceReference, ServiceDescription> entry : srToSDMap.entrySet()){
                        ServiceReference ref = entry.getKey();
                        if(ref.equals(serviceRef) && sd.equals(entry.getValue())){
                            ServicePlugin requestingPlugin = ref.getRequestingPlugin();
                            ServicePlugin matchingPlugin = ref.getMatchingPlugin();
                            Collection<Service> serviceConsumers =
                                        orchestrator.getServicesManagedByPlugin(requestingPlugin, servicesForAssociation);
                            for (Service serviceConsumer : serviceConsumers) {
                                try {
                                    Object args[]=new Object[]{serviceProvider,serviceRef,requestingPlugin};
                                    logger.log(Level.INFO, "associate.provisionedservice",args);
                                    requestingPlugin.associateServices(serviceConsumer, serviceRef, serviceProvider, preDeployment, context);
                                    associatedServicesList.add(
                                        new ServiceAssociationRecord(serviceProvider, serviceConsumer, serviceRef, requestingPlugin));
                                    matchingPlugin.associateServices(serviceConsumer, serviceRef, serviceProvider, preDeployment, context);
                                    associatedServicesList.add(
                                        new ServiceAssociationRecord(serviceProvider, serviceConsumer, serviceRef, matchingPlugin));
                                } catch (Exception e) {
                                    Object args1[]= new Object[]{serviceConsumer.getName(),serviceProvider.getName(),serviceRef};
                                    logger.log(Level.WARNING,localStrings.getString("failure.while.associating.service",args1),e);
                                    rollback(associatedServicesList, context, preDeployment);
                                    throw new PaaSDeploymentException(e);
                                }
                            }
                        }
                    }
                }
            }
        }finally{
            if(logger.isLoggable(Level.FINEST)){
                logger.log(Level.FINEST, "associated-services-records for application [ "+appName+" ]: " + associatedServicesList);
            }
            associatedServicesList.clear();
        }
    }

    private void rollback(List<ServiceAssociationRecord> associatedServices, PaaSDeploymentContext context,
                          boolean preDeployment) {
        if(isAtomicDeploymentEnabled()){
            for(ServiceAssociationRecord asr : associatedServices){
                Service serviceProvider = asr.getProvider();
                Service serviceConsumer = asr.getConsumer();
                ServiceReference serviceRef = asr.getServiceReference();
                ServicePlugin plugin = asr.getPlugin();
                try{
                    plugin.dissociateServices(serviceConsumer, serviceRef, serviceProvider, preDeployment,
                            context);
                }catch(Exception e){
                    Object args[]= new Object[]{serviceConsumer.getName(),serviceProvider.getName(),serviceRef};
                    logger.log(Level.WARNING,localStrings.getString("failure.while.associating.service",args),e);
                }
            }
        }
    }

    class ServiceAssociationRecord {
        private Service provider;
        private Service consumer;
        private ServiceReference serviceRef;
        private ServicePlugin plugin;

        public ServiceAssociationRecord(Service serviceProvider,
                                       Service serviceConsumer,
                                       ServiceReference serviceRef, ServicePlugin plugin){
            this.provider = serviceProvider;
            this.consumer = serviceConsumer;
            this.serviceRef = serviceRef;
            this.plugin = plugin;
        }

        public Service getProvider(){
            return provider;
        }

        public Service getConsumer(){
            return consumer;
        }

        public ServiceReference getServiceReference(){
            return serviceRef;
        }

        public ServicePlugin getPlugin(){
            return plugin;
        }

        @Override
        public String toString() {
            return "AssociatedServiceRecord{" +
                    "provider=" + provider +
                    ", consumer=" + consumer +
                    ", serviceRef=" + serviceRef +
                    ", plugin=" + plugin +
                    '}';
        }
    }
}
